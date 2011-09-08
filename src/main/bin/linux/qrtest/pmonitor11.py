import glob
import os
import sys
import threading
import threadpool
import subprocess

__author__ = 'boe'

class PMonitor:
    """
    Handles tasks and their dependencies (as formal inputs and outputs) and executes them on a thread pool.
    Maintains
      a thread pool with a task queue of mature tasks, those with inputs available
      a backlog of tasks not yet mature
      a report file that records all completed calls and the paths of output product (set) names
      a set of commands executed in previous runs listed in the initial report, not updated
      a map of bound product (set) names, mapped to None (same path) or a set of paths, same as in report file
      a map of product set names to the number of yet missing outputs of non-collating tasks
      a list of host resources with capacity and current load, ordered by capacity-load
      a mutex to protect access to backlog, report, paths, counts
    Usage:
        ...
        pm = PMonitor(allInputs, request=year+'-'+month, hosts=[('localhost',2),('phost2',4)])
        for day in days:
            ...
            pm.execute('bin/meris-l2.sh', l2Inputs, [l2Output], priority=1, collating=False)
            ...
            pm.execute('bin/meris-l3.sh', [l2Output], [dailyOutput], priority=2)
        ...
        pm.execute('bin/meris-aggregation.sh', dailyOutputs, [monthlyOutput], priority=3)
        pm.wait_for_completion()
    """

    class HostConfig:
        """
        Triple of host name, capacity, and load
        """
        name = None
        capacity = None
        load = None
        def __init__(self,name,capacity):
            self.name = name
            self.capacity = capacity
            self.load = 0
        def __cmp__(self, other):
            if self.load > other.load:
                return 1
            elif self.load < other.load:
                return -1
            else:
                return 0

    _pool = None
    _backlog = []
    _created = 0
    _processed = 0
    _failed = []
    _report = None
    _status = None
    _commands = set([])
    _paths = dict([])
    _counts = dict([])
    _hosts = []
    _swd = None
    _cache = None
    _mutex = threading.Lock()
    _request = None

    def __init__(self, inputs, request='', hosts=[('localhost',4)], swd=os.getcwd(), cache=None):
        """
        Initiates monitor, marks inputs, reads report, creates thread pool
        """
        try:
            os.system('mkdir -p logs; mkdir -p traces')     
            self._mutex.acquire()
            self._init_hosts(hosts)
            self._swd = swd
            self._cache = cache
            self._mark_inputs(inputs)
            self._maybe_read_report(request + '.report')
            self._status = open(request + '.status', 'w')
            concurrency = sum(map(lambda host:host[1], hosts))
            self._pool = threadpool.ThreadPool(concurrency)
            self._request = request
        finally:
            self._mutex.release()

    def execute(self, call, inputs, outputs, priority=1, collating=True):
        """
        Schedules task `call inputs outputs`, either a single collating call or one call per input
        """
        try:
            self._mutex.acquire()
            if self._all_inputs_available(inputs):
                inputPaths = self._paths_of(inputs)
                if collating:
                    self._created += 1
                    request = threadpool.WorkRequest(self._process_step, [call, self._created, inputPaths, outputs], priority=priority)
                    self._pool.putRequest(request)
                else:
                    self._counts[outputs[0]] = len(inputPaths)
                    for input in inputPaths:
                        self._created += 1
                        request = threadpool.WorkRequest(self._process_noncollating_step, [call, self._created, [input], outputs], priority=priority)
                        self._pool.putRequest(request)
            else:
                if collating:
                    handler = self._process_step
                else:
                    handler = self._process_noncollating_step
                self._created += 1
                request = threadpool.WorkRequest(handler, [call, self._created, inputs, outputs], priority=priority)
                self._backlog.append(request)
        finally:
            self._mutex.release()

    def wait_for_completion(self):
        """
        Waits until all scheduled tasks are run, then returns
        """
        self._pool.wait()
        self._write_status()
        self._status.close()
        return int(bool(self._failed or self._backlog))

    def _init_hosts(self, hosts):
        """
        Sets host resources
        """
        for config in hosts:
            host,cpus = config
            self._hosts.append(PMonitor.HostConfig(host,cpus))
        self._hosts.sort()

    def _maybe_read_report(self, reportName):
        """
        Reads report containing lines with command line calls and lines with an output path of a product name, e.g.
          bin/meris-l3.sh /home/boe/eodata/MER_WV__2P/v01/2010/01/25 /home/boe/eodata/MER_WV__3P/v01/2010/01/25/meris-l3-daily-25.dat
          #output /home/boe/eodata/MER_WV__3P/v01/2010/01/25/meris-l3-daily-25.dat /home/boe/eodata/MER_WV__3P/v01/2010/01/25/meris-l3-daily-25.dat.real
        """
        if glob.glob(reportName):
            self._report = open('logs/' + reportName, 'r+')
            for line in self._report.readlines():
                if line.startswith('#output '):
                    w = line.split()
                    name = w[1]
                    paths = w[2:]
                    self._bind_output(name, paths)
                else:
                    self._commands.add(line)
        else:
            self._report = open('logs/' + reportName, 'w')

    def _write_status(self):
        try:
            self._mutex.acquire()
            self._status.seek(0)
            workers = len(self._pool.workers)
            requests = len(self._pool.workRequests)
            if (requests >= workers):
                running = workers
            else:
                running = requests
            pending = requests - running
            self._status.write('{0} created, {1} running, {2} pending, {3} backlog, {4} processed, {5} failed\n'.\
                    format(self._created, running, pending, len(self._backlog), self._processed, len(self._failed)))
            for l in self._failed:
                self._status.write('{0}\n'.format(l))
            self._status.flush()
        finally:
            self._mutex.release()

    def _process_noncollating_step(self, call, taskId, inputs, outputs):
        """
        Callable for tasks, marker for non-collating tasks, same implementation as _process_step
        """
        self._process_step(call, taskId, inputs, outputs)

    def _process_step(self, call, taskId, inputs, outputs):
        """
        Callable for tasks, marker for simple or collating tasks.
        looks up call in commands from report, maybe skips execution
        executes call by os call, scans process stdout for `output=...`
        updates report, maintains products, and schedule mature tasks
        """
        if call[0] != '/':
            command = '{0} {1} {2}\n'.format(self._swd + '/' + call, ' '.join(inputs), ' '.join(outputs))
        else:
            command = '{0} {1} {2}\n'.format(call, ' '.join(inputs), ' '.join(outputs))
        if command in self._commands:
            sys.__stdout__.write('skipping {0}'.format(command))
            self._propagate_result(outputs)
            self._processed += 1
        else:
            self._write_status()
            wd = self._prepare_working_dir(taskId)
            outputPaths = []
            host = self._select_host()
            process = self._start_processor(command, host, wd)
            self._trace_processor_output(outputPaths, process, taskId, wd)
            process.stdout.close()
            code = process.wait()
            self._release_host(host)
            if code == 0:
                self._report_and_propagate_result(command, outputs, outputPaths)
                self._processed += 1
                if self._cache != None and 'cache' in wd:
                    subprocess.call(['rm', '-rf', wd])
            else:
                self._failed.append(command[:-1])
                sys.__stderr__.write('failed {0}'.format(command))

    def _select_host(self):
        """returns host with idle capacity and lowest load"""
        try:
            self._mutex.acquire()
            for host in self._hosts:
                if host.load < host.capacity:
                    host.load += 1
                    return host.name
            self._hosts[0].load += 1
            return self._hosts[0].name
        finally:
            self._hosts.sort()
            self._mutex.release()

    def _release_host(self, usedHost):
        """decrements load of host just released"""
        try:
            self._mutex.acquire()
            for host in self._hosts:
                if host.name == usedHost:
                    host.load -= 1
                    return
            raise Exception('cannot find ' + usedHost + ' in ' + str(self._hosts))
        finally:
            self._hosts.sort()
            self._mutex.release()

    def _prepare_working_dir(self, taskId):
        """Creates working directory in .../cache/request-id/task-id"""
        if self._cache == None:
            wd = '.'
        else:
            wd = self._cache + '/' + self._request + '/' + '{0:04d}'.format(taskId)
        if not os.path.exists(wd):
            os.makedirs(wd)
        return wd

    def _start_processor(self, command, host, wd):
        """starts subprocess either locally or via ssh, returns process handle"""
        if host == 'localhost':
            cmd = command[:-1]
            sys.__stdout__.write('executing {0}'.format(cmd + '\n'))
            process = subprocess.Popen(cmd, shell=True, bufsize=1, cwd=wd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        else:
            cmd = 'ssh ' + host + " 'mkdir -p " + wd + ';' + " cd " + wd + ';' + command[:-1] + "'"
            sys.__stdout__.write('executing {0}'.format(cmd + '\n'))
            process = subprocess.Popen(cmd, shell=True, bufsize=1, cwd=wd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        return process

    def _trace_processor_output(self, outputPaths, process, taskId, wd):
        """traces processor output, recognises 'output=' lines, writes all lines to trace file in working dir"""
        if wd == '.':
            if not os.path.exists(wd + 'traces'):
                os.makedirs(wd + 'traces')
            trace = open(wd + '/traces/' + self._request + '-' + '{0:04d}'.format(taskId) + '.stdout', 'w')
        else:    
            trace = open(wd + '/' + self._request + '-' + '{0:04d}'.format(taskId) + '.stdout', 'w')
            
        for line in process.stdout:
            if line.startswith('output='):
                outputPaths.append(line[7:].strip())
            trace.write(line)
            trace.flush()
        trace.close()

    def _propagate_result(self, outputs):
        """
        Maintains products and schedules mature tasks, used if command is in former report and skipped
        """
        try:
            self._mutex.acquire()
            self._mark_outputs(outputs)
            self._check_for_mature_tasks()
        finally:
            self._mutex.release()

    def _report_and_propagate_result(self, command, outputs, outputPaths):
        """
        Maintains report, products, and schedules mature tasks, used if command execution has been completed
        """
        try:
            self._mutex.acquire()
            self._report.write(command)
            self._report_and_bind_outputs(outputs, outputPaths)
            self._report.flush()
            self._check_for_mature_tasks()
        finally:
            self._mutex.release()


    def _check_for_mature_tasks(self):
        """
        Checks tasks in backlog whether inputs are (now) all bound in products map
        adds these mature tasks to thread pool, removes them from backlog
        distinguishes collocating and non-collocating asks by the callable used
        generates one task per input for non-collocating tasks
        """
        matureTasks = []
        for task in self._backlog:
            if self._all_inputs_available(task.args[2]):
                inputPaths = self._paths_of(task.args[2])
                if task.callable == self._process_step:
                    task.args[2] = inputPaths
                    self._pool.putRequest(task)
                else:
                    self._counts[task.args[3][0]] = len(inputPaths)
                    for input in inputPaths:
                        self._created += 1
                        request = threadpool.WorkRequest(self._process_noncollating_step, \
                                                         [task.args[0], self._created, [input], task.args[3]], \
                                                         priority=task.priority)
                        self._pool.putRequest(request)
                matureTasks.append(task)
        for task in matureTasks:
            self._backlog.remove(task)

    def _all_inputs_available(self, inputs):
        """
        Returns whether all inputs are bound in products map and no one waits for more non-collating tasks to contribute
        """
        for i in inputs:
            if not i in self._paths:
                return False
            if i in self._counts:
                return False
        return True

    def _mark_inputs(self, inputs):
        """
        Marks initial inputs as being bound, with the path being the same as the name
        """
        for i in inputs:
            self._paths[i] = None

    def _mark_outputs(self, outputs):
        """
        Marks task outputs as being bound, with the paths preliminarily being the same as the names
        count down the count for non-collating outputs
        """
        for o in outputs:
            if o not in self._paths:
                self._paths[o] = None
            if o in self._counts:
                n = self._counts[o]
                if n <= 1:
                    self._counts.pop(o)
                else:
                    self._counts[o] = n - 1

    def _report_and_bind_outputs(self, outputs, outputPaths):
        """
        Binds outputs in products map to None, a path or a set of paths, maintains report
        """
        self._mark_outputs(outputs)
        if len(outputPaths) == 0:
            pass
        elif len(outputs) == len(outputPaths):
            for i in range(len(outputs)):
                if outputPaths[i] != outputs[i]:
                    self._report_and_bind_output(outputs[i], [outputPaths[i]])
        elif len(outputs) == 1:
            self._report_and_bind_output(outputs[0], outputPaths)
        else:
            raise RuntimeError('no output expected: found {1}'.format(' '.join(outputPaths)))

    def _report_and_bind_output(self, output, outputPaths):
        """
        Binds output in products map to set of paths, maintains report
        """
        self._bind_output(output, outputPaths)
        self._report.write('#output {0} {1}\n'.format(output, ' '.join(outputPaths)))

    def _bind_output(self, name, paths):
        """
        Bind output name to paths or extend existing output name by paths
        """
        if name not in self._paths or self._paths[name] == None:
            self._paths[name] = paths
        else:
            self._paths[name].extend(paths)

    def _paths_of(self, inputs):
        """
        Returns flat list of paths of the inputs, looked up in products map
        """
        if len(inputs) == 0:
            return inputs
        else:   
            return reduce(lambda x, y: x + y, map(lambda p: self._path_of(p), inputs))

    def _path_of(self, product):
        """
        Returns paths a product is bound to, or a single entry list with the product name if the product bound to its own name
        """
        paths = self._paths[product]
        if paths == None:
            return [product]
        else:
            return paths
