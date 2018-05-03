import scipy
import numpy as np
from scipy import stats
import math
from datetime import datetime
import matplotlib.pyplot as plt
import parameters
import calendar

# TODO: season + modifier, x_day_of_y_dist, day dist
# fig, ax = plt.subplots(1, 1)


class BimodalDist:
    def __init__(self,dist1, dist2):
        self.dist1 = dist1
        self.dist2 = dist2

    def logpdf(self, val):
        # TODO: better arithmetic
        if self.dist1.pdf(val) + self.dist2.pdf(val) == 0.0:
            return (max(self.dist1.logpdf(val),self.dist2.logpdf(val))- math.log(2))
        #print(self.dist1.pdf(val) + self.dist2.pdf(val))
        return math.log((self.dist1.pdf(val)+self.dist2.pdf(val))/2)

    def mean(self):
        return [self.dist1.mean(), self.dist2.mean()]

    def std(self):
        assert(self.dist1.std()==self.dist2.std())
        return self.dist1.std()


def x_day_of_y(year, month, day, ordinal):
    # e.g. 2nd Monday of August => (2016,8,0,2) {0 for Monday, 6 for Sunday}
    # ordinal = -1 for last!
    # returns day number

    count = 0
    last = 0

    if ordinal > 0:
        for i in range(1, calendar.monthrange(year, month)[1]+1):
            if datetime(year, month, i).weekday() == day:
                count += 1

            if count == ordinal:
                return datetime(year, month, i).timetuple().tm_yday

            last = i

    elif ordinal == -1:
        for i in range(calendar.monthrange(year, month)[1], 0, -1):
            if datetime(year, month, i).weekday() == day:
                return datetime(year, month, i).timetuple().tm_yday


def x_week_of_y_dist(year, month, ordinal):
    that_week_wednesday = x_day_of_y(year, month, 2, ordinal)
    distr = scipy.stats.norm(that_week_wednesday, parameters.week_std)
    return distr

def x_day_of_m_dist(year, month, day, ordinal):
    that_day = x_day_of_y(year, month, day, ordinal)
    distr = scipy.stats.norm(that_day, parameters.day_std)
    return distr

def x1_day1_of_m1_or_x2_day2_of_m2_dist(year, month1, day1, ordinal1, month2, day2, ordinal2):
    x1_day1_of_m1_dist = x_day_of_m_dist(year, month1, day1, ordinal1)
    x2_day2_of_m2_dist = x_day_of_m_dist(year, month2, day2, ordinal2)
    return BimodalDist(x1_day1_of_m1_dist, x2_day2_of_m2_dist)

def day_dist(year, month, day):
    mean = datetime(year, month, day).timetuple().tm_yday
    distr = scipy.stats.norm(mean, parameters.day_std)
    return distr
    
def day_wider_dist(year, month, day):
    mean = datetime(year, month, day).timetuple().tm_yday
    distr = scipy.stats.norm(mean, 3*parameters.day_std)
    return distr
    
def month_season_dist(year, month_season, modifier=''):
    if type(month_season) == int:
        if not modifier:
            mean = datetime(year, month_season, parameters.month_mean).timetuple().tm_yday
            distr = scipy.stats.norm(mean, parameters.month_std)
            # show_pdf(distr)
            return distr
        elif modifier == '_early':
            mean = datetime(year, month_season, parameters.month_early_mean).timetuple().tm_yday
            distr = scipy.stats.norm(mean, parameters.month_early_std)
            # show_pdf(distr)
            return distr
        elif modifier == '_mid':
            mean = datetime(year, month_season, parameters.month_mid_mean).timetuple().tm_yday
            distr = scipy.stats.norm(mean, parameters.month_mid_std)
            # show_pdf(distr)
            return distr
        elif modifier == '_late':
            mean = datetime(year, month_season, parameters.month_late_mean).timetuple().tm_yday
            distr = scipy.stats.norm(mean, parameters.month_late_std)
            # show_pdf(distr)
            return distr
    else:
        if month_season == 'SU':
            if not modifier:
                distr = scipy.stats.norm(parameters.summer_mean, parameters.summer_std)
                return distr
        elif month_season == 'WI':
            if not modifier:
                distr = scipy.stats.norm(parameters.winter_mean, parameters.winter_std)
                return distr
        elif month_season == 'SP':
            if not modifier:
                distr = scipy.stats.norm(parameters.spring_mean, parameters.spring_std)
                return distr


def show_pdf(distr):
    fig, ax = plt.subplots(1,1)
    x = np.linspace(distr.ppf(0.01), distr.ppf(0.99), 100)
    ax.plot(x, distr.pdf(x), 'k-', lw=2, label='frozen pdf')
    plt.show()


def show_pdf_comp(distr1, distr2):
    fig, ax = plt.subplots(1, 1)
    x = np.linspace(distr1.ppf(0.000000000001), distr1.ppf(0.99999), 100)
    ax.plot(x, distr1.pdf(x), 'k-', lw=2, label='frozen pdf')
    ax.plot(x, distr2.pdf(x), 'r-', lw=2, label='frozen pdf')
    plt.show()
