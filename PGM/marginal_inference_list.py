import dist

import math
import pdb
import re
import datetime
import pickle
from copy import deepcopy

from collections import OrderedDict

months = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
seasons = ['SU', 'WI', 'SP']
month_season_modifiers = ['', '_early', '_mid', '_late']
NOISE_PENALTY = 0.1
SCHEDEXT_CONF = .8
NOISE_PARAM = True
RETURN_ALL_SCHEDS_SORTED = True
RETURN_ALL_SCORES = False
SCORES_SCHEDS_TO_RETURN = 30


def marginal_inference(instance_dates, bias_schedules, alreadyLoadedDists, marginal_instance_year, instance_confs=None):
    """
    :param instance_dates: - list of lists, each inner list has multiple extractions for one event instance
                           - solution gives one event from each inner list
                           - each extraction is of the form (year, month, day)

    :param marginal_instance_year: - year for which ranked list based on marginal inference needs to be returned
    :param instance_confs: - for each extraction in instance_dates, associated confidence value

    """
    
    if not (instance_dates or bias_schedules[0]):
        return None
    
    if marginal_instance_year=='DH':
        marginal_instance_year = 2016   # doesn't matter
    else:
        marginal_instance_year = int(marginal_instance_year)
    
    if not instance_confs:
        instance_confs = [[1]*len(x) for x in instance_dates]
      
    atomic_schedule_dists = OrderedDict()
    years = set([y[0] for x in instance_dates for y in x])
    years.add(2005)
    for year in years:
        atomic_schedule_dists[year] = alreadyLoadedDists[year]
    noise = NOISE_PENALTY if NOISE_PARAM else 0
    
    phi_sch_s = load_phi_sch_s(bias_schedules, 2005, atomic_schedule_dists)
    
    skip = -1   # index of instance to skip summing out, if that is the instance in marginal_instance_year. else -1 => sum out all, no extractions for instance in marginal_instance_year
    for i in range(len(instance_dates)):
        if int(instance_dates[i][0][0])==marginal_instance_year:
            skip=i
            break
            
    phi_s = deepcopy(phi_sch_s)
    for i in range(len(instance_dates)):
        if i!= skip:
            phi_s = multiply_arrays(phi_s, sum_out_di(instance_dates[i], instance_confs[i], atomic_schedule_dists))
    
    phi_d = sum_out_s(phi_s, marginal_instance_year, atomic_schedule_dists)
    
    if skip!=-1:    # there are extractions for instance in marginal_instance_year
        phi_occ_d = [noise]*365
        for j in range(len(instance_dates[skip])):
            phi_occ_d[date_to_day_of_year(instance_dates[skip][j])-1] = instance_confs[skip][j]
        phi_d = multiply_arrays(phi_d, phi_occ_d)
    
    return sorted(range(1,366), key=lambda x: -phi_d[x-1])

def load_phi_sch_s(bias_schedules, any_year, dists):
    phi_sch_s = []
    
    for i, schedule in enumerate(dists[any_year].keys()):
        if bias_schedules[0]:
            if any([schedule == bias_schedule for bias_schedule in bias_schedules]):
                phi_sch_s.append(SCHEDEXT_CONF)
            elif any([re.search('m[0-9][0-9]', bias_schedule).group(0) in schedule for bias_schedule in bias_schedules]):  # month match
                phi_sch_s.append(SCHEDEXT_CONF - 0.1)
            else:
                phi_sch_s.append(1 - SCHEDEXT_CONF)
        else:
            phi_sch_s.append(1 - SCHEDEXT_CONF)
    
    return phi_sch_s

def sum_out_di(instance_i_dates, instance_i_confs, dists):
    """
    : instance_i_dates : list of extractions for ith instance
    : instance_i_confs : list of confidences for ith instance
    : dists: distributions
    
    This function approximately sums out di that takes values from 1-365 and returns a list of |dists| 
    corresponding to \Sum_(d_i) \Phi^{occ}(d_i)\Psi^{pair}(s, d_i), a function of s
    """
    
    WINDOW = 1      # summing only WINDOW days around the mean of the schedule (since normal dist decays sharply)
    
    instance_year = int(instance_i_dates[0][0])
    noise = NOISE_PENALTY if NOISE_PARAM else 0
    ext_dict = {date_to_day_of_year(instance_i_dates[i]):instance_i_confs[i] for i in range(len(instance_i_dates))}
    
    sums = [0.]*len(dists[instance_year])
    
    for i,schedule in enumerate(dists[instance_year].values()):
        for day in range(int(schedule.mean())-WINDOW, int(schedule.mean())+WINDOW+1):
            if day in ext_dict:
                sums[i] += ext_dict[day]*schedule.pdf(day)
            else:
                sums[i] += noise*schedule.pdf(day)
    
    return sums
    

def multiply_arrays(a1, a2):
    assert(len(a1)==len(a2))
    if NOISE_PARAM:
        return [math.exp(math.log(x)+math.log(y)) for x,y in zip(a1,a2)]
    else:
        return [x*y for x,y in zip(a1,a2)]


def sum_out_s(phi_s, instance_year, dists):
    """
    : phi_s : any function of s, array of length |dists|
    
    This approximately sums out s and returns an array of length 365
    """
    if instance_year not in dists:
        dists[instance_year] = load_dist_of_a_year(instance_year)
        
    WINDOW = 1      # summing only those schedules with mean within WINDOW days of day
    day_dict = {i:set() for i in range(1,366)}      # day_dict[i] contains indices of schedules with mean in range(i-WINDOW,i+WINDOW+1)
        
    for i,sched in enumerate(dists[instance_year].values()):
        for d in range(int(sched.mean())-WINDOW,int(sched.mean())+WINDOW+1):
            if 1<=d<=365:
                day_dict[d].add(i)
    
    sums = [0.]*365
    
    for d in range(1,366):
        for sched_idx in day_dict[d]:
            sched_name = list(dists[instance_year].keys())[sched_idx]
            sums[d-1] += phi_s[sched_idx]*dists[instance_year][sched_name].pdf(d)
    
    return sums
    


def dist_ll(date, confidence, schedule, distribution):
    if len(date) == 3:  # complete date
        day_of_year = date_to_day_of_year(date)
        return math.log(confidence) + distribution.logpdf(day_of_year)

    elif len(date) == 2: # only month
        if 'm' + "%02d" % (date[1]) in schedule:  # TODO: penalty of half stddev if month matches
            if not isinstance(distribution, dist.BimodalDist):
                return math.log(confidence) + distribution.logpdf(distribution.mean() + (distribution.std() / 2))
            else:
                return math.log(confidence) + distribution.logpdf(distribution.mean()[0] + (distribution.std() / 2))
        else:  # TODO: from the middle of the month
            day_of_year = date_to_day_of_year((date[0], date[1], 15))
            return math.log(confidence) + distribution.logpdf(day_of_year)

    else:
        print('SOMETHING WRONG WITH DATE')


def load_dists(years):
    """
    :param years: list of years for which distributions are to be returned
    :return: dict of dict (all atomic distributions for each year)
    """
    dists = OrderedDict()
    for year in years:
        dists[year] = load_dist_of_a_year(year)

    return dists


# refactored by dinesh
def load_dist_of_a_year(year):
    distyear = OrderedDict()

    # x_week_of_y distributions
    for month in months:
        for ordinal in list(range(1, 5)) + [-1]:  # 1, 2, 3, 4, -1 <- last week
            distyear[str(ordinal) + '_week_of_m' + str(month)] = dist.x_week_of_y_dist(year, int(month), ordinal)

    # month_season distributions
    for ms in months:
        for mod in month_season_modifiers:
            distyear['m' + ms + mod] = dist.month_season_dist(year, int(ms), mod)

    #for ms in seasons:
    #    distyear['s' + ms] = dist.month_season_dist(year, ms)

    # x_day_of_y distributions
    for month in months:
        for day in range(7):  # {0 for Monday, 6 for Sunday}
            for ordinal in list(range(1, 5)) + [-1]:  # 1, 2, 3, 4, -1 <- last
                distyear[str(ordinal) + '_' + str(day) + '_day_of_m' + str(month)] = dist.x_day_of_m_dist(year,
                                                                                                          int(month),
                                                                                                          day, ordinal)
    # x_day_of_y_or_x+1_day_of_y distributions
    #for month in months:
    #    for day in range(7):  # {0 for Monday, 6 for Sunday}
    #        for ordinal in range(1, 4):  # 1st_or_2nd 2nd_or_3rd 3rd_or_4th
    #            distyear[str(ordinal) + '_' + str(day) + '_day_of_m' + str(month) + '_or_' +
    #                     str(ordinal + 1) + '_' + str(day) + '_day_of_m' + str(
    #                month)] = dist.x1_day1_of_m1_or_x2_day2_of_m2_dist(year, int(month), day, ordinal, int(month),
    #                                                                   day,
    #                                                                   ordinal + 1)
    #
    ## -1_day_of_y_or_1_day_of_y+1 distributions
    #for month in months[:-1]:  # Jan to November
    #    for day in range(7):  # {0 for Monday, 6 for Sunday}
    #        distyear['-1_' + str(day) + '_day_of_m' + str(month) + '_or_1_' + str(day) + '_day_of_m' + "%02d"%(
    #            int(month)+1)] = dist.x1_day1_of_m1_or_x2_day2_of_m2_dist(year, int(month), day, -1, int(month) + 1, day,
    #                                                               1)

    first_day = datetime.date(year, 1, 1)
    last_day = datetime.date(year, 12, 31)
    diff = last_day - first_day
    for i in range(diff.days + 1):
        fixed_day = first_day + datetime.timedelta(i)
        if not (fixed_day.month==2 and fixed_day.day==29):  # corner case
            distyear['m' + "%02d"%(fixed_day.month) + "_d" + "%02d"%(fixed_day.day)] = dist.day_dist(fixed_day.year,
                                                                                         fixed_day.month, fixed_day.day)
            #distyear['m' + "%02d"%(fixed_day.month) + "_d" + "%02d"%(fixed_day.day) + "_wider"] = dist.day_wider_dist(fixed_day.year,
            #                                                                             fixed_day.month, fixed_day.day)                                                                            

    return distyear


def date_to_day_of_year(date_tuple):
    try:
        doy = datetime.datetime(*date_tuple).timetuple().tm_yday
        return doy
    except:
        # assuming Feb 29 issue
        doy = datetime.datetime(date_tuple[0], date_tuple[1], 28).timetuple().tm_yday
        return doy

def marginal_inference_parallel(input):
    instance_dates_batch, bias_schedules_batch, marginal_instance_year_batch, instance_confs_batch, batch_id = input

    dists = OrderedDict()
    inst_years = range(2005,2017)
    for inst_year in inst_years:
        if inst_year not in dists:
            dists[inst_year] = load_dist_of_a_year(inst_year)
    
    ranked_lists = []
    for i in range(len(instance_dates_batch)):
        ranked_lists.append(marginal_inference(instance_dates_batch[i], bias_schedules_batch[i], dists, marginal_instance_year_batch[i], instance_confs_batch[i]))
    
    print('finished batch : ' + str(batch_id))
    return ranked_lists

def get_dists(event_input, instance_year):
    dists = OrderedDict()
    inst_years = set([y[0] for x in event_input for y in x])
    inst_years = inst_years.union(set(range(2005,2018)))
    for inst_year in inst_years.union(set([instance_year])):
        if inst_year not in dists:
            dists[inst_year] = load_dist_of_a_year(inst_year)
    return dists
    

if __name__ == '__main__':

    cur_instance_ilp_input = [
        [(2011, 3, 21)]
        ]
    query_instance = 2016
    dists = get_dists(cur_instance_ilp_input, query_instance)
    ranked_list = marginal_inference(cur_instance_ilp_input, [''], dists, query_instance, [[0.52]])

    pdb.set_trace()