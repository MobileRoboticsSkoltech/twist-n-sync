import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy.signal import correlate as cor
from scipy.interpolate import interp1d, CubicSpline
from scipy.ndimage.filters import gaussian_filter1d
from scipy.optimize import fmin
from scipy.stats import iqr
from os import listdir

import time


class TimeSync2:
    def __init__(self, x1, x2, t1, t2, do_resample=True):
        self.x1 = x1
        self.x2 = x2
        self.t1 = t1
        self.t2 = t2
        self.do_resample = do_resample
        self.resample_complete = False
        self.t1_new = None
        self.t2_new = None
        self.x1_new = None
        self.x2_new = None
        self.time_delay = None
        
    def resample(self, accuracy=1e-3):
        # if... else... can be skipped if data has the same constant data rate
        # then just need `self.x1_new = self.x1` and `self.x2_new = self.x2`
        self.dt = np.min([accuracy,
                     np.mean(np.diff(self.t1)),
                     np.mean(np.diff(self.t2))])
        if self.do_resample:

            self.t1_new = np.arange(self.t1[0], self.t1[-1] + self.dt, self.dt)
            self.t2_new = np.arange(self.t2[0], self.t2[-1] + self.dt, self.dt)

            def interp(t_old, f_old, t_new, kind='cubic'):
                func = interp1d(t_old, f_old, kind=kind, axis=0, bounds_error=False, fill_value=(0,0))
                f_new = func(t_new)
                return f_new

            self.x1_new = interp(self.t1, self.x1, self.t1_new)
            self.x2_new = interp(self.t2, self.x2, self.t2_new)
        else:
            self.x1_new = self.x1
            self.x2_new = self.x2

        self.resample_complete = True
        
    def obtain_delay(self):
        assert self.resample_complete == True, 'resample() has not called yet'
        # Compute cross-corrrelation
        self.cor = cor(self.x2_new, self.x1_new)
        # Obtain initial index of cross-cor. Related to initial estimation of time delay
        index_init = np.argmax(self.cor)
        
        # Cross-cor. based cubic spline coefficients
        cubic_spline = CubicSpline(np.arange(self.cor.shape[0]), self.cor)
        coefs = cubic_spline.c[:,index_init]
        # Check cubic spline derivative sign...
        order = coefs.shape[0] - 1
        derivative = coefs[-2]
        # ... and rechoose initial index of cross-cor. if needed
        if derivative < 0:
            index_init -= 1
            coefs = cubic_spline.c[:,index_init]
        
        # Solve qudratic equation to obtain roots
        res = np.roots([(order-i)*coefs[i] for i in range(order)])
        # Choose solution from roots
        if sum((order-i)*coefs[i]*((res[0]+res[1])/2)**(order-i-1) for i in range(order)) < 0:
            res = np.min(res)
        else:
            res = np.max(res)
            
        # Get time delay between starts of tracks
        self.some_dat = - self.x1_new.shape[0] + 1
        self.time_delay = (index_init + self.some_dat +  res) * self.dt