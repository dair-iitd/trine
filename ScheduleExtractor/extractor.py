"""
Extracts the schedule of an event given raw text that discusses the event.
Use the schedule function as follows: schedule(text, sutime_xmls, sutime_text)

text is the raw text, sutime_text is the annotated file returned after passing
raw text through SUTime, and sutime_xmls are the xmls from the sutime_text. An
example of inputs would be as follows:

text:
-----
Scientific and Technical Academy Award Ceremony is an yearly event held to present Academy awards for scientific and technical achievements in motion pictures. 

sutime_xmls:
------------
<?xml version="1.0" encoding="UTF-8"?>
<DOC>
<TIMEX3 freq="P1X" periodicity="P1Y" quant="EVERY" tid="t1" type="SET" value="P1Y">yearly</TIMEX3>
<TIMEX3 tid="t2" type="DATE" value="PRESENT_REF">present</TIMEX3>
</DOC>

sutime_text:
------------
<?xml version="1.0" encoding="UTF-8"?>
<DOC>
  <DATE>2013-07-14</DATE>
  <TEXT>Scientific and Technical Academy Award Ceremony is an <TIMEX3 tid="t1" type="SET" value="P1Y">yearly</TIMEX3> event held to <TIMEX3 tid="t2" type="DATE" value="PRESENT_REF">present</TIMEX3> Academy awards for scientific and technical achievements in motion pictures. 
</TEXT>
</DOC>
"""

import os
import re
from xml.dom import minidom
from collections import Counter

import values


day_weekend_prefix = '(first|1st|second|2nd|third|3rd|fourth|4th|last)'
day_week_weekend   = '(monday|tuesday|wednesday|thursday|friday|saturday|sunday|weekend|week)'
months_seasons     = '(january|february|march|april|may|june|july|august|september|october|november|december|summer|spring|winter|fall|autumn|monsoon)'
modifiers		   = '(early|mid|late|end)'
relative_extractions = '^' + day_week_weekend + '$|^(the day|this day|the present day|yesterday|today|tomorrow|sunrise|dusk|dawn|the modern day)$'


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
	
	if mentions:
		return resolve_schedule(mentions)
	else:
		return ''

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
