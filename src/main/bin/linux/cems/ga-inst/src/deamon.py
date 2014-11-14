# !/usr/bin/env python
''' 
#
# YapDi - Yet another python Daemon implementation <https://github.com/kasun/YapDi>
# Author Kasun Herath <kasunh01@gmail.com> 
#
'''

from signal import SIGTERM
import sys, atexit, os, pwd
import time


COMMAND_START = 'start'
COMMAND_STOP = 'stop'
COMMAND_RESTART = 'restart'
COMMAND_STATUS = 'status'


class Daemon(object):
  
    OPERATION_SUCCESSFUL = 0
    OPERATION_FAILED = 1
    INSTANCE_ALREADY_RUNNING = 2
    INSTANCE_NOT_RUNNING = 3
    SET_USER_FAILED = 4

    """
    A generic daemon class.

    Usage: subclass the Daemon class and override the run() method
    """
    def __init__(self, pidfile, stdin=os.devnull, stdout=os.devnull, stderr=os.devnull, home_dir='.', umask=022):
        self.pidfile = pidfile
        self.stdin = stdin
        self.stdout = stdout
        self.stderr = stderr
        self.home_dir = home_dir
        self.umask = umask
        self.daemon_alive = True

    def daemonize(self):
        ''' Daemonize the current process and return '''
        if self.status():
            return Daemon.INSTANCE_ALREADY_RUNNING
        try: 
            pid = os.fork() 
            if pid > 0:
                # exit first parent
                sys.exit(0) 
        except OSError, e: 
            return Daemon.OPERATION_FAILED

        # decouple from parent environment
        os.chdir(self.home_dir)
        os.setsid()
        os.umask(self.umask)

        # do second fork
        try: 
            pid = os.fork() 
            if pid > 0:
                # exit from second parent
                sys.exit(0) 
        except OSError, e: 
            return Daemon.OPERATION_FAILED

        # redirect standard file descriptors
        sys.stdout.flush()
        sys.stderr.flush()
        si = file(self.stdin, 'r')
        so = file(self.stdout, 'a+', 0)
        se = file(self.stderr, 'a+', 0)
        os.dup2(si.fileno(), sys.stdin.fileno())
        os.dup2(so.fileno(), sys.stdout.fileno())
        os.dup2(se.fileno(), sys.stderr.fileno())

        # write pidfile
        atexit.register(self.delpid)
        pid = str(os.getpid())
        file(self.pidfile,'w+').write("%s\n" % pid)
    
        return Daemon.OPERATION_SUCCESSFUL

    def delpid(self):
        os.remove(self.pidfile)

    def kill(self):
        ''' kill any running instance '''
        # check if an instance is not running
        pid = self.status()
        if not pid:
            return Daemon.INSTANCE_NOT_RUNNING

        # Try killing the daemon process	
        try:
            while 1:
                os.kill(pid, SIGTERM)
                time.sleep(0.1)
        except OSError, err:
            err = str(err)
            if err.find("No such process") > 0:
                if os.path.exists(self.pidfile):
                    os.remove(self.pidfile)
            else:
                return Daemon.OPERATION_FAILED
        return Daemon.OPERATION_SUCCESSFUL

    def restart(self):
        ''' Restart an instance; If an instance is already running kill it and start else just start '''
        kill_status = self.kill()
        if kill_status == Daemon.OPERATION_FAILED:
            return kill_status
        return self.daemonize()

    def status(self):
        ''' check whether an instance is already running. If running return pid or else False '''
        try:
            pf = file(self.pidfile,'r')
            pid = int(pf.read().strip())
            pf.close()
            
            # See if the pidfile matches the calling script name
            cmdlinepath = '/proc/%d/cmdline' % pid
            if os.path.exists(cmdlinepath):
                cmdlinefile = open(cmdlinepath, 'r')
                cmdline = cmdlinefile.read()
                cmdlinefile.close()

                # The pid in our file isn't this script
                if not sys.argv[0].split('/')[-1] in cmdline:
                    self.delpid()
                    pid = None

            # The pid from our pidfile isn't running      
            else:
                self.delpid()
                pid = None
        except IOError:
            pid = None
        return pid

    def get_pidfile(self, scriptname):
        ''' Return file name to save pid given original script name '''
        pidpath_components = scriptname.split('/')[0:-1]
        pidpath_components.append('.yapdi.pid')
        return '/'.join(pidpath_components)

    def run(self):
      """
      You should override this method when you subclass Daemon. It will be called after the process has been
      daemonized by start() or restart().
      """
  
    def usage(self, command):
        print("USAGE: python %s %s|%s|%s|%s" % (command, COMMAND_START, COMMAND_STOP, COMMAND_RESTART, COMMAND_STATUS))
 
    @classmethod
    def setup(cls, argv):
        name = os.path.splitext(argv[0])[0]
        deamon = cls(pidfile=name+'.pid', stdout=name+'.out', stderr=name+'.err')

        # Invalid executions
        if len(argv) < 2 or argv[1] not in [COMMAND_START, COMMAND_STOP, COMMAND_RESTART, COMMAND_STATUS]:
            deamon.usage(argv[0])
            sys.exit(0) 

        if argv[1] == COMMAND_START:
            # Check whether an instance is already running
            if deamon.status():
                print("An instance is already running.")
                sys.exit(0) 
            # Execute if daemonization was successful else exit
            if deamon.daemonize() == Daemon.OPERATION_SUCCESSFUL:
                deamon.run()
            else:
                print('Daemonization failed')

        elif argv[1] == COMMAND_STOP:
            # Check whether no instance is running
            if not deamon.status():
                print("No instance running.")
                sys.exit(0)
            if deamon.kill() == Daemon.OPERATION_FAILED:
                print('Trying to stop running instance failed')

        elif argv[1] == COMMAND_RESTART:
            # Execute if daemonization was successful else exit
            if deamon.restart() == Daemon.OPERATION_SUCCESSFUL:
                deamon.run()
            else:
                print('Daemonization failed')
                
        elif argv[1] == COMMAND_STATUS:
            if deamon.status():
                print("A instance is running.")
            else:
                print("No instance running.")