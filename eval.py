def to_day_of_year(date):
    # date => 'YYYY-MM-DD'
    return datetime.datetime(*[int(y) for y in date[:10].split('-')]).timetuple().tm_yday
    
def print_mrr(gold, pred):
    # gold: start-dates of form 'YYYY-MM-DD'
    # pred: array of form day-of-year (int) (ranked list, unique)

    log = open(logfile,'w')
    
    gold = [to_day_of_year(x) for x in gold]
    total_preds = 0
    mrr_agg = 0.
    ranks = []
    
    for i in range(len(gold)):
        if pred[i]:
            total_preds += 1
            for j in range(len(pred[i])):
                if pred[i][j]==gold[i]:
                    ranks.append(j+1)
                    mrr_agg += 1./float(j+1)
                    break
    
    print('Total predictions made : ' + str(total_preds) + '\n')
    print('MRR                    : ' + str(sum([1./x for x in ranks])/total_preds) + '\n')    
    print('Acc@1                  : ' + str(len([x for x in ranks if x<=1])/float(total_preds)) + '\n')
    print('Acc@3                  : ' + str(len([x for x in ranks if x<=3])/float(total_preds)) + '\n')
    print('Acc@5                  : ' + str(len([x for x in ranks if x<=5])/float(total_preds)) + '\n')
    print('Acc@10                 : ' + str(len([x for x in ranks if x<=10])/float(total_preds)) + '\n')
    print('-----------------------\n')
    
    return mrr_agg/total_preds
