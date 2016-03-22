__author__ = 'daniel'

import numpy as np
import scipy.linalg as linalg

class WarpDeriver(object):
    pass

    def __init__(self):
        self.bucket_size_limit = 512
        self.trial_confidence = 0.05

    def _n_trials(self):
        while ((self.n_trials >= 100) | (self.n_trials < 0)):
            self.bucket_size = 2**self.bucket_count
            self.bucket_count += 1
            if self.bucket_size == self.bucket_size_limit:
                return  # if bucket is too big, get out
            self._define_buckets()
            self._compute_trials()
        self.converged = True

    def _define_buckets(self):
        x_max = np.round(np.max(self.locs_a[:, 0]))
        x_min = np.round(np.min(self.locs_a[:, 0]))
        y_max = np.round(np.max(self.locs_a[:, 1]))
        y_min = np.round(np.min(self.locs_a[:, 1]))

        x_diff = x_max - x_min
        y_diff = y_max - y_min
        x_remain = x_diff % self.bucket_size
        y_remain = y_diff % self.bucket_size
        self.count_x_buckets = (x_diff - x_remain)/self.bucket_size + 1
        self.count_y_buckets = (y_diff - y_remain)/self.bucket_size + 1
        self.total_buckets = self.count_x_buckets * self.count_y_buckets

        #get the number of sampled buckets by rounding x and y bucket to its nearest bucket
        self.sampled_x_buckets = self.round_to_base(np.round(self.locs_a[:,0]) - x_min)
        self.sampled_y_buckets = self.round_to_base(np.round(self.locs_a[:,1]) - y_min)
        self.sampled_buckets = np.unique(self.sampled_x_buckets + self.sampled_y_buckets*100000).shape[0]

    def round_to_base(self, values_to_round):
        return self.bucket_size*np.round(values_to_round/self.bucket_size)

    def _compute_trials(self):
        n = np.log(self.trial_confidence)
        x = 1 - (self.sampled_buckets/self.total_buckets)**(self.total_buckets/self.sampled_buckets)
        if x <= 0:
            d = 1
        else:
            d = np.log(x)
        trials = np.round(n / d)
        if np.isnan(trials) == True:
            self.n_trials = -1
        if np.isinf(trials) == True:
            self.n_trials = -1  # is this necessary?
        self.n_trials = trials

    def _evaluate_transformations(self):
        # first get an idea of the error characteristics unwarped
        self._assess_error_prewarp()

        # now find a transformation to improve the warp
        for trial in xrange(int(self.n_trials)):
            try:
                self._select_tiepoints()

                for run in [1,2]:
                    transform = self._lsfit()
                    cp_rmse, tp_rmse = self._assess_warp(run, transform)
                    if cp_rmse < self.best_rmse:
                        self.transform = transform
                        self.best_rmse = cp_rmse
                        self.cp_rmse_warped = cp_rmse
                        self.tp_rmse_warped = tp_rmse
            except Exception, e:
                print "Deriving transformation failed with the error:", e, "... Continuing with next iteration"
                continue

    def _assess_error_prewarp(self):
        diffX = self.locs_a[:,0] - self.locs_b[:,0]
        diffY = self.locs_a[:,1] - self.locs_b[:,1]
        residuals = np.sqrt(diffY**2 + diffX**2)
        sumSquareResiduals = np.sum(residuals**2)
        self.rmse_prewarp = np.sqrt(sumSquareResiduals/diffY.shape[0])


    def _select_tiepoints(self):
        self._weight_buckets()
        self._tiepoint_picker()

    def _weight_buckets(self):
        bucket_index = np.zeros(self.locs_a.shape[0])  #holds the bucket location for the given tiepoint
        bucket_weights = np.zeros(self.sampled_buckets) #holds the number of tiepoints within a given bucket
        bucket_id = 0
        for i in np.arange(0, (self.count_y_buckets + 1)*self.bucket_size, self.bucket_size):
            for j in np.arange(0, (self.count_x_buckets + 1)*self.bucket_size, self.bucket_size):
                which_buckets = (self.sampled_y_buckets == i) & (self.sampled_x_buckets == j)
                if np.max(which_buckets) != 0:  #if there are samples
                    if bucket_id >= self.sampled_buckets:
                        print "There are more buckets (", bucket_id, ") than have been sampled", self.sampled_buckets
                        print "This should not have happened, continuing"
                        bucket_id += 1
                        continue
                    bucket_index[which_buckets] = bucket_id
                    bucket_weights[bucket_id] = np.sum(which_buckets) #this sums the total number of tie-points within the given bucket
                    bucket_id += 1

        #scale the bucket weights from 0,1 and cumulatively sum
        self.bucket_weights = np.cumsum(bucket_weights * (1.0/np.sum(bucket_weights)))
        self.bucket_index = bucket_index

    def _tiepoint_picker(self):
        tiepoint_indexes = list()
        checkpoint_indexes = list()
        while np.min(self.bucket_weights) < 99999:
            chosen_bucket = np.argmin(np.abs(self.bucket_weights -  np.random.random()))
            potential_tiepoints = np.where(self.bucket_index == chosen_bucket)[0]

            if potential_tiepoints.size > 1:
                # put one into tiepoints and rest in to checkopints
                chosen_tiepoint = np.argmin(np.random.random(potential_tiepoints.size))
                tiepoint_indexes.append(potential_tiepoints[chosen_tiepoint])
                for i, cp in enumerate(potential_tiepoints):
                    if i == chosen_tiepoint:
                        continue
                    checkpoint_indexes.append(cp)
            else:
                tiepoint_indexes.append(potential_tiepoints)

            self.bucket_weights[chosen_bucket] = 99999

        self.tiepoint_indexes = np.array(tiepoint_indexes)
        self.checkpoint_indexes = np.array(checkpoint_indexes)

    def _lsfit(self):
        #regressand -  the 'map' coordinates, which are dependent upon the 'image'
        R = np.zeros([self.tiepoint_indexes.size*2])
        R[0:self.tiepoint_indexes.shape[0]] = self.locs_b[self.tiepoint_indexes, 1] #map indexes (y)
        R[self.tiepoint_indexes.shape[0]:] = self.locs_b[self.tiepoint_indexes, 0] #map indexes (x)

        #design - the 'image' coordinates
        inputD = np.zeros([self.tiepoint_indexes.size, 3])
        inputD[:, 0] = 1
        inputD[:, 1] = self.locs_a[self.tiepoint_indexes, 1] #image indexes (r)
        inputD[:, 2] = self.locs_a[self.tiepoint_indexes, 0] #image indexes (c)

        D = np.zeros([self.tiepoint_indexes.size*2, 6])
        D[0:self.tiepoint_indexes.size, 0:3] = inputD
        D[self.tiepoint_indexes.size:,3:] = inputD

        #derive the function to transform the image coordinates (X) onto the map (Y), so that Y=f(X)
        DT = D.T
        DTD = np.dot(DT, D)
        DTR = np.dot(DT, R)
        L,lower = linalg.cho_factor(DTD, lower=True)
        return linalg.cho_solve((L,lower),DTR)

    def _assess_warp(self, run, transform):
        #apply warp to the 'image'
        warpedY, warpedX = self.warp_points(self.tiepoint_indexes, transform)

        #compute distances of the warped 'image' coordinates from the 'map' coordinates
        diffX = warpedX - self.locs_b[self.tiepoint_indexes,0]
        diffY = warpedY - self.locs_b[self.tiepoint_indexes,1]

        #furst run to remove out-lying tie-points
        if run == 1:
            meanDiffY = np.mean(diffY)
            meanDiffX = np.mean(diffX)
            stdDiffY = np.std(diffY)
            stdDiffX = np.std(diffX)
            inliers = (diffY > (meanDiffY - stdDiffY)) & (diffY < (meanDiffY + stdDiffY)) & (diffX > (meanDiffX - stdDiffX)) & (diffX < (meanDiffX + stdDiffX))
            self.tiepoint_indexes = self.tiepoint_indexes[inliers]  # update tiepoints
            return 99999, 99999
        else:
            # now with outlier gone, do the evaluation on the checkpoints
            residuals = np.sqrt(diffY**2 + diffX**2)
            sumSquareResiduals  = np.sum(residuals**2)
            tiepointRMSE = np.sqrt(sumSquareResiduals/diffY.shape[0])

            #warp checkpoints
            warpedY, warpedX = self.warp_points(self.checkpoint_indexes, transform)

            #evaluate checkpoints
            diffX = warpedX - self.locs_b[self.checkpoint_indexes,0]
            diffY = warpedY - self.locs_b[self.checkpoint_indexes,1]
            residuals = np.sqrt(diffY**2 + diffX**2)
            sumSquareResiduals  = np.sum(residuals**2)
            checkpointRMSE = np.sqrt(sumSquareResiduals/diffY.shape[0])
            for i in np.arange(0, residuals.shape[0]):
                print('residuals[' + str(i) + '] = ' + str(residuals[i]))
            print "checkpointRMSE, tiepointRMSE = ", checkpointRMSE , ', ' , tiepointRMSE   
            return checkpointRMSE, tiepointRMSE

    def warp_points(self, index, transform):
        '''
        Apply the warp to the 'image', to transform in into the 'map'
        '''
        warped_y = transform[0] + self.locs_a[index,1]*transform[1] + self.locs_a[index,0]*transform[2]
        warped_x = transform[3] + self.locs_a[index,1]*transform[4] + self.locs_a[index,0]*transform[5]

        return warped_y, warped_x

    def get_warp(self, locs_a, locs_b):

        self.locs_a = locs_a
        self.locs_b = locs_b

        self.n_trials = 99999
        self.best_rmse = 99999
        self.bucket_count = 4
        self.converged = False
        self.transform = False

        self._n_trials()
        if self.converged:
            print "Evaluating transformations with", self.n_trials, "trials"
            self._evaluate_transformations()
        else:
            print "Could not find n_trials which will lead to an optimal transformation"

class Warper(object):

    def __init__(self,
                 null_value=0):
        self.null_value = null_value

    def _image_warper(self):
        x_grid, y_grid = np.meshgrid(np.arange(self.image_shape[1]), np.arange(self.image_shape[0]))
        warped_y_grid = np.round(self.transform[0] + y_grid*self.transform[1] + x_grid*self.transform[2]).astype('int')
        warped_x_grid = np.round(self.transform[3] + y_grid*self.transform[4] + x_grid*self.transform[5]).astype('int')
        indicies = (warped_y_grid >= 0) & (warped_y_grid < self.map_shape[0]) & \
                   (warped_x_grid >= 0) & (warped_x_grid < self.map_shape[1])

        y = np.squeeze(warped_y_grid[indicies])
        x = np.squeeze(warped_x_grid[indicies])
        r = np.squeeze(y_grid[indicies])
        c = np.squeeze(x_grid[indicies])

        self.resampled_image = np.zeros(self.map_shape) + self.null_value
        self.resampled_image[y, x] = self.image_to_warp[r, c]

        return self.resampled_image

    def apply_warp(self, shape, image_to_warp, transform):

        self.map_shape = shape
        self.image_to_warp = image_to_warp
        self.image_shape = image_to_warp.shape
        self.transform = transform
        return self._image_warper()
