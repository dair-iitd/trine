import os
import re
from xml.dom import minidom
from collections import Counter

import values

# TODO: change 2013 in present time of SUTime to something like 2020 Jan 1st!

labelled_dir = '/Users/surag/Desktop/BTP/Periodicity/freebase_clean_labelled_data/'
unlabelled_dir = '/Users/surag/Desktop/BTP/Periodicity/freebase_clean_data/'
sutime_labelled_dir = '/Users/surag/Desktop/BTP/Periodicity/fine_grained/SUTimeAnnotated/labelled/'
sutime_unlabelled_dir = '/Users/surag/Desktop/BTP/Periodicity/fine_grained/SUTimeAnnotated/unlabelled/'

day_weekend_prefix = '(first|1st|second|2nd|third|3rd|fourth|4th|last)'
day_week_weekend   = '(monday|tuesday|wednesday|thursday|friday|saturday|sunday|weekend|week)'
months_seasons     = '(january|february|march|april|may|june|july|august|september|october|november|december|summer|spring|winter|fall|autumn|monsoon)'
modifiers		   = '(early|mid|late|end)'
relative_extractions = '^' + day_week_weekend + '$|^(the day|this day|the present day|yesterday|today|tomorrow|sunrise|dusk|dawn|the modern day)$'

def first_occurence(text,sutime_xmls,sutime_text):
	first_trigger = r'(beg[au]n(ning)? in|etablished in|inception in|initiat(ed|ion) in|inaugurat(ed|ion) in|launched in|start(ed|ing) in|debut(ing)? in|first held in|first happened in|founded in|created in|inaugral edition in|founded in|first edition in|dating( back)? to|every year since|[^/.] since)'
	
	if re.search(first_trigger + ' [12][0-9]{3,3}',text.lower()):
		index = re.search(first_trigger + ' [12][0-9]{3,3}',text.lower()).end()
		return(text[index-4:index])
		
	if re.search('(first|started|inaugral)[^/.]*[12][0-9]{3,3}',text.lower()):
		index = re.search('(started|first|inaugral)[^/.]*[12][0-9]{3,3}',text.lower()).end()
		return(text[index-4:index])
		
	#if re.search(first_trigger + ' ' + months_seasons + ' ' ,text.lower()):
	#	index = re.search('(started|first|inaugral)[^/.]*[12][0-9]{3,3}',text.lower()).end()
	#	return(text[index-4:index])
		
	return ''

def normalize_sutime(text):
	if text.find("P1Y")!=-1: return "P1Y"
	
	for i in range(1,50):
		if text.find("P"+str(i)+"Y")!=-1: return "P"+str(i)+"Y"
		if text.find("P"+str(i)+"D")!=-1: return "P"+str(i)+"D"
		if text.find("P"+str(i)+"W")!=-1: return "P"+str(i)+"W"
	
	if text.find("P1M")!=-1: return "P1M"
	
	if text.find("P6M")!=-1: return "PY2"
	
	# keeping it last, position is important
	if text.find("XXXX")!=-1: return "P1Y"
	
	return None

def periodicity(text,sutime_xmls,sutime_text):
	
	mentions = []
	
	for timex in sutime_xmls:
		if timex.attributes['type'].value == 'SET':
			
			attr = [x[0] for x in timex.attributes.items()]
			
			if 'periodicity' in attr and normalize_sutime(timex.attributes['periodicity'].value) != None:
				#return normalize_sutime(timex.attributes['periodicity'].value)
				mentions.append(normalize_sutime(timex.attributes['periodicity'].value))
				
			elif 'value' in attr and  normalize_sutime(timex.attributes['value'].value) != None:
				#return normalize_sutime(timex.attributes['value'].value)
				mentions.append(normalize_sutime(timex.attributes['value'].value))
				
			elif 'altVal' in attr and  normalize_sutime(timex.attributes['altVal'].value) != None:
				#return normalize_sutime(timex.attributes['altVal'].value)
				mentions.append(normalize_sutime(timex.attributes['altVal'].value))
			
			elif 'alt_value' in attr and  normalize_sutime(timex.attributes['alt_value'].value) != None:
				#return normalize_sutime(timex.attributes['alt_value'].value)
				mentions.append(normalize_sutime(timex.attributes['alt_value'].value))
	#if mentions: print(mentions)		
	#return ''
	
	# returning most common
	if Counter(mentions).most_common():
		return Counter(mentions).most_common(1)[0][0]			
	else:
		return ''

def duration(text,sutime_xmls,sutime_text):
	for timex in sutime_xmls:
		
		if timex.attributes['type'].value == 'DURATION' or timex.attributes['type'].value == 'DATE':
			
			attr = [x[0] for x in timex.attributes.items()]
				
			if 'value' in attr and  normalize_sutime(timex.attributes['value'].value) != None:
				if not re.search('Y',normalize_sutime(timex.attributes['value'].value)):
					return normalize_sutime(timex.attributes['value'].value)
					#print(normalize_sutime(timex.attributes['value'].value))

			elif 'altVal' in attr and  normalize_sutime(timex.attributes['altVal'].value) != None:
				if not re.search('Y',normalize_sutime(timex.attributes['altVal'].value)):
					return normalize_sutime(timex.attributes['altVal'].value)
					#print(normalize_sutime(timex.attributes['altVal'].value))

			elif 'alt_value' in attr and  normalize_sutime(timex.attributes['alt_value'].value) != None:
				if not re.search('Y',normalize_sutime(timex.attributes['alt_value'].value)):
					return normalize_sutime(timex.attributes['alt_value'].value)
					#print(normalize_sutime(timex.attributes['alt_value'].value))
	
	# add more
	if bool(re.search('(Marathon)',text)):
		return 'P1D'
	
	return ''

def non_relative_extraction(timex_text):
	# a relative extraction would be something like 'today', 'wednesday', etc.
	# TODO: add more!

	if not re.search(relative_extractions, timex_text.lower()):	return True
	else: return False
	
def normalize_sutime_schedule(text,timex_text):
	# timex_text is stuff within the <TIMEX3>
	# change 2013 to something like 2020 to not interfere (better idea?)
	
	if re.search('(the)? ?'+ day_weekend_prefix + ' ' + day_week_weekend + ' (of|in) ' + months_seasons,timex_text.lower() ):
		pf  = re.search(day_weekend_prefix,timex_text.lower() )
		pfx = getattr(values, 'pf_'+timex_text.lower()[pf.start():pf.end()])
		dw  = re.search(day_week_weekend,timex_text.lower() )
		dww = getattr(values, timex_text.lower()[dw.start():dw.end()])
		m   = re.search(months_seasons,timex_text.lower() )
		ms  = getattr(values, timex_text.lower()[m.start():m.end()])
		
		return (pfx + '_' + dww + '_of_' + ms)
	
	if re.search('(XXXX|201[34])-[01][0-9]-[0-3][0-9]',text):
		index = re.search('(XXXX|201[34])-[01][0-9]-[0-3][0-9]',text).end()
		
		# things like just 'wednesday' should not pass, right?		
		if non_relative_extraction(timex_text):					
			return 'm'+text[index-5:index]
		
	if re.search('(XXXX|201[34])-[01][0-9]',text):
		index = re.search('(XXXX|201[34])-[01][0-9]',text).end()

		# things like just 'wednesday' should not pass, right?
		if non_relative_extraction(timex_text):			
			return 'm'+text[index-2:index]
			
	if re.search('(XXXX|201[34])-[A-Z][A-Z]',text):
		index = re.search('(XXXX|201[34])-[A-Z][A-Z]',text).end()

		# things like just 'wednesday' should not pass, right?
		if non_relative_extraction(timex_text):
			return 's'+text[index-2:index]
	
	# for modifiers (EARLY, BEGINNING, MID)
	if re.search('(^EARLY$|^MID$|^LATE$)',text):
		return text
	
	return None
		
def next_text_timex(tid,sutime_text):
	sutime_annotated = sutime_text.childNodes
	
	i = 0
	while(True):
		if sutime_annotated[i].nodeName == 'TIMEX3' and sutime_annotated[i].getAttribute('tid')==tid:
			break
		i+=1
	
	# if not the last element, then next is text
	if i!=len(sutime_annotated)-1:
		return sutime_annotated[i+1].nodeValue
	
	
def schedule(text,sutime_xmls,sutime_text):
	
	# distinguish b/w annual and others?
	
	mentions = []
	
	for i,timex in enumerate(sutime_xmls):
		if timex.attributes['type'].value == 'SET' or timex.attributes['type'].value == 'DATE':
			
			recently_added = False
			attr = [x[0] for x in timex.attributes.items()]
			
			if 'value' in attr and  normalize_sutime_schedule(timex.attributes['value'].value,timex.firstChild.nodeValue) != None:
				#return normalize_sutime(timex.attributes['value'].value)
				mentions.append(normalize_sutime_schedule(timex.attributes['value'].value,timex.firstChild.nodeValue))
				recently_added = True
				
			elif 'altVal' in attr and  normalize_sutime_schedule(timex.attributes['altVal'].value,timex.firstChild.nodeValue) != None:
				#return normalize_sutime(timex.attributes['altVal'].value)
				mentions.append(normalize_sutime_schedule(timex.attributes['altVal'].value,timex.firstChild.nodeValue))
				recently_added = True
			
			elif 'alt_value' in attr and  normalize_sutime_schedule(timex.attributes['alt_value'].value,timex.firstChild.nodeValue) != None:
				#return normalize_sutime(timex.attributes['alt_value'].value)
				mentions.append(normalize_sutime_schedule(timex.attributes['alt_value'].value,timex.firstChild.nodeValue))
				recently_added = True
				
			if 'mod' in attr:
				if mentions:
					# add only if month or season before it
					if re.search('(^[01][0-9]$|' + months_seasons[1:-1] + ')' , mentions[-1].lower()):
						mentions[-1] = mentions[-1] + '-' + normalize_sutime_schedule(timex.attributes['mod'].value,'')
			
			# see if it is followed by 'or'			
			if recently_added and next_text_timex(timex.getAttribute('tid'),sutime_text)==' or ':
					mentions.append('or')
			if recently_added and next_text_timex(timex.getAttribute('tid'),sutime_text)==' and ':				
					mentions.append('and')
					
	# covering up for SUTime (it misses mid-Month and such expressions)
	if re.search(modifiers + '-' + months_seasons, text.lower()):
		index = re.search(modifiers + '-' + months_seasons, text.lower()).start()
		end   = re.search(modifiers + '-' + months_seasons, text.lower()).end()
		intermediate = text.lower()[index:end]
		dash = re.search('-',intermediate).start()
		md = getattr(values, intermediate[:dash])
		ms = getattr(values, intermediate[dash+1:])
		mentions.append(ms + md)
				
	#if mentions: print(mentions)		
	#return ''
	
	if mentions:
		return resolve_schedule(mentions)
	else:
		return ''
	# returning most common
	#if Counter(mentions).most_common():
	#	return Counter(mentions).most_common(1)[0][0]			
	#else:
	#	return ''	

def resolve_schedule(mentions):
	
	# if prefixed available, probably correct
	if 'or' in mentions:
		index = mentions.index('or')
		# return (mentions[index-1] + ' or ' + mentions[index+1])
		return [mentions[index-1],mentions[index+1]]
	
	if 'and' in mentions:
		index = mentions.index('and')
		# return (mentions[index-1] + ' and ' + mentions[index+1])
		return [mentions[index-1],mentions[index+1]]
	
	for mention in mentions:
		if re.search('(LATE|EARLY|MID)',mention):
			return [mention] #,mentions
		
	# take the finer one if SU and 06 are both present => written for north hemisphere only!
	if 'SU' in mentions:
		finer = [bool(re.search('0[4-8]',x)) for x in mentions]
		if any(finer):
			index = finer.index(True)
			return [mentions[index]]
	if 'WI' in mentions:
		finer = [bool(re.search('(10|11|12|01|02|03)',x)) for x in mentions]
		if any(finer):
			index = finer.index(True)
			return [mentions[index]]
	
	return [mentions[0]]

def duration_post_schedule(schedule):
	if re.search('WEEKEND',schedule):
		return 'P2D'
	elif re.search('[01][0-9]-[0-3][0-9]',schedule):
		return 'P1D'		
	return ''	

def extract(pred,header):
	for i,event in enumerate(pred):
		if os.path.isfile(labelled_dir + event['file']):
			f = open(labelled_dir + event['file'],'rb')
		else:
			f = open(unlabelled_dir + event['file'],'rb')
			
		text = f.read()
		f.close()
		
		if os.path.isfile(sutime_labelled_dir + event['file'] + '_all'):
			sutime_xmls = minidom.parse(sutime_labelled_dir + event['file'] + '_all').getElementsByTagName('TIMEX3')
		else:
			sutime_xmls = minidom.parse(sutime_unlabelled_dir + event['file'] + '_all').getElementsByTagName('TIMEX3')
		
		if os.path.isfile(sutime_labelled_dir + event['file'] ):
			sutime_text = minidom.parse(sutime_labelled_dir + event['file']).getElementsByTagName('TEXT')[0]
		else:
			sutime_text = minidom.parse(sutime_unlabelled_dir + event['file']).getElementsByTagName('TEXT')[0]
		
		if header == 'duration_post_schedule':
			if not pred[i]['duration']:
				pred[i]['duration'] = globals()[header](pred[i]['schedule'])
		else:
			pred[i][header] = globals()[header](text,sutime_xmls,sutime_text)
			
	return pred

def extract_sched(freebase_id):
	if os.path.isfile(labelled_dir + freebase_id):
		f = open(labelled_dir + freebase_id,'rb')
	else:
		f = open(unlabelled_dir + freebase_id,'rb')
			
	text = f.read()
	f.close()
		
	if os.path.isfile(sutime_labelled_dir + freebase_id + '_all'):
		sutime_xmls = minidom.parse(sutime_labelled_dir + freebase_id + '_all').getElementsByTagName('TIMEX3')
	else:
		sutime_xmls = minidom.parse(sutime_unlabelled_dir + freebase_id + '_all').getElementsByTagName('TIMEX3')
		
	if os.path.isfile(sutime_labelled_dir + freebase_id ):
		sutime_text = minidom.parse(sutime_labelled_dir + freebase_id).getElementsByTagName('TEXT')[0]
	else:
		sutime_text = minidom.parse(sutime_unlabelled_dir + freebase_id).getElementsByTagName('TEXT')[0]
				
	return globals()['schedule'](text,sutime_xmls,sutime_text)

			
