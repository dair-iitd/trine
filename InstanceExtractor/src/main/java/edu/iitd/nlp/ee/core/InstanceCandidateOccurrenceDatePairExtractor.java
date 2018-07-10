package edu.iitd.nlp.ee.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.simple.JSONArray;
import edu.iitd.nlp.ee.features.Feature;
import edu.iitd.nlp.ee.freebase.FreebaseEvent;
import edu.iitd.nlp.ee.utils.CommonFunctions;
import edu.iitd.nlp.ee.utils.Global;
import edu.iitd.nlp.ee.utils.Pair;
import edu.iitd.nlp.ee.utils.WordnetWrapper;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationSerializer;
import edu.stanford.nlp.pipeline.CustomAnnotationSerializer;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

/*
 * Extract mentions from docs
 * @author dineshraghu
 *
 */

public class InstanceCandidateOccurrenceDatePairExtractor {
	
	private static class DateAnn {
		
		String normalizedDate;
		
		int beginToken;
		
		int endToken;
		
		String dateStr;

		public DateAnn(String normalizedDate, int beginToken, int endToken,
				String dateStr) {
			this.normalizedDate = normalizedDate;
			this.beginToken = beginToken;
			this.endToken = endToken;
			this.dateStr = dateStr;
		}

		@Override
		public String toString() {
			return "[" + normalizedDate + ", beginToken=" + beginToken + ", endToken=" + endToken
					+ ", dateStr=" + dateStr + "]";
		}
		
		
	}
	
	public static class EventAnn {
		
		String text;
		
		int beginToken;
		
		int endToken;
		
		String instance;
		
		

		@Override
		public String toString() {
			return "[" + text + "," + beginToken + "," + endToken + ","
					+ instance + "]";
		}



		public EventAnn(String text, String instance, int beginToken, int endToken) {
			super();
			this.text = text;
			this.instance = instance;
			this.beginToken = beginToken;
			this.endToken = endToken;
		}
	}
	
	public static int UNKNOWN_LABEL = -1;
	public static int START_LABEL 	= 1;
	public static int END_LABEL 	= 2;
	public static int HOLD_LABEL 	= 3;
	public static int OTHER_LABEL 	= 4;
	
	/*
	 * For all events, get all docs via DocGetter, then for each doc, extract and store all mentions.
	 * 
	 * Params: -docToMentionsPath Path where map is to stored. If this file
	 * exists, if will be overwritten without ever being read.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void main(String args[]) throws Exception {
		
		// ////////////////////////////////
		// Read props
		// /////////////////////////////////
		Global.props = StringUtils.argsToProperties(args);
		System.out.println("Running with the following properties...");
		for (Object key : Global.props.keySet()) {
			System.out.println(key + "\t" + Global.props.get(key));
		}
		System.out.println("-----------------------------------------");
		
		String lucenePath = Global.props.getProperty("lucenePath");
		
		Directory directory = FSDirectory.open(new File(lucenePath));
		Searcher indexSearcher = new IndexSearcher(directory);
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		QueryParser queryParser = new QueryParser(Version.LUCENE_30, "events", analyzer);
		int nResults = Integer.MAX_VALUE; //no limit on results returned
		
		String eventAliasFile = Global.props.getProperty("eventAliasFile");
		String freebaseEventsJson = Global.props.getProperty("freebaseEventsJson");
		
		String eventsWithScheduleFile = Global.props.getProperty("eventsWithScheduleFile");
		HashSet<String> eventsWithSchedule = new HashSet<String>();
		JSONArray json = CommonFunctions.readJsonArrayFromFile(eventsWithScheduleFile);
		for (int i = 0; i < json.size(); i++)
			eventsWithSchedule.add((String) json.get(i));
		
		String wordnetDir = Global.props.getProperty("wordnetDir");
		WordnetWrapper wordnetWrapper = new WordnetWrapper(wordnetDir);
		
		String feDir = Global.props.getProperty("feDir");
		
		//boolean onlyMetaEventsWithoutInstances = true;
		
		//HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents = FreebaseEvent.GetRecurrentEventsFromJson(freebaseEventsJson, eventAliasFile,allMetaEventsCsvFile, onlyMetaEventsWithoutInstances);
		HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents =FreebaseEvent. GetRecurrentEventsFromJson(freebaseEventsJson, eventAliasFile);
		HashMap<String, ArrayList<String>> metaEventToAliases = GetMetaEventAliases(metaEventsToEvents, wordnetWrapper);
		
		int totalMetaEventsInDB = 0;
		HashSet<String> totalUniqueMetaEvents = new HashSet<String>();
		int metaEventMentions = 0;
		
		int totalInstancesInDB = 0;
		HashSet<String> totalUniqueInstances = new HashSet<String>();
		int totalInstancesFound = 0;
		
		/** All **/
		
		HashMap<String, Integer> labelsToCount = new HashMap<String, Integer>();
		
		labelsToCount.put("START-TRAIN", 0);
		labelsToCount.put("END-TRAIN", 0);
		labelsToCount.put("HOLD-TRAIN", 0);
		labelsToCount.put("OTHER-TRAIN", 0);
		labelsToCount.put("UNKNOWN-TRAIN", 0);
		
		labelsToCount.put("START-TEST", 0);
		labelsToCount.put("END-TEST", 0);
		labelsToCount.put("HOLD-TEST", 0);
		labelsToCount.put("OTHER-TEST", 0);
		labelsToCount.put("UNKNOWN-TEST", 0);
		
		labelsToCount.put("UNKNOWN-NODAY", 0);
		
		labelsToCount.put("START-NOISNT-TRAIN", 0);
		labelsToCount.put("START-NOISNT-TEST", 0);
		labelsToCount.put("OTHER-NOISNT", 0);
		
		labelsToCount.put("UNKNOWN-NOISNT", 0);
		labelsToCount.put("UNKNOWN-NOISNT-NODAY", 0);
		
		HashSet<String> uniqueStartsWithInstances = new HashSet<String>();
		HashSet<String> uniqueEndsWithInstances = new HashSet<String>();
		HashSet<String> uniqueHoldsWithInstances = new HashSet<String>();
		
		HashSet<String> uniqueStartsWithInstancesTr = new HashSet<String>();
		HashSet<String> uniqueEndsWithInstancesTr = new HashSet<String>();
		HashSet<String> uniqueHoldsWithInstancesTr = new HashSet<String>();
		
		StringBuffer sbTrain = new StringBuffer();
		StringBuffer sbTrainFreebaseCompletion = new StringBuffer();
		StringBuffer sbTest = new StringBuffer();
		
		CommonFunctions.WriteToFile(feDir + File.separator + "train.txt", "");
		CommonFunctions.WriteToFile(feDir + File.separator + "freebase_completion.txt", "");
		CommonFunctions.WriteToFile(feDir + File.separator + "test.txt", "");

		int totalSize = metaEventsToEvents.size();
		
		int metaEventsInTrainSoFar = 0;
		
		HashSet<Integer> docsWithMutipleEvents = new HashSet<Integer>();
		int docsWithSingleEvent = 0;
		
		int docsWithMultipleInstances = 0;
		int docsWithSingleInstance = 0;
		
		JSONArray globalNewInstancesLog = new JSONArray();
		
		for(String metaEvent : metaEventsToEvents.keySet()) {
			
			boolean trainMode = true;
			
			if(metaEvent != null && !metaEvent.equals("")) {
				
				if(eventsWithSchedule.contains(metaEvent))
					trainMode = false;
				else {
					if(metaEventsInTrainSoFar > (totalSize/2))
						trainMode = false;
				}
				
				if(trainMode == true)
					metaEventsInTrainSoFar++;
				
				totalMetaEventsInDB++;
				
				trainMode = !trainMode;
				
				System.out.println(totalMetaEventsInDB + " : " + trainMode + " : " + metaEvent );
				
				ArrayList<FreebaseEvent> instances = metaEventsToEvents.get(metaEvent);
				totalInstancesInDB += instances.size();
				
				HashMap<String, String> aliasToInstance = new HashMap<String, String>();
				ArrayList<String> metaEventAliases = metaEventToAliases.get(metaEvent);
				
				HashMap<String, Pair<String, String>> instanceToStartAndEndDate = new HashMap<String, Pair<String, String>>();
				HashMap<Integer, String> yearToInstance = new HashMap<Integer, String>();
				
				JSONArray instancesWithNoStartDates = new JSONArray();
				JSONArray instancesAlreadyPresentLog = new JSONArray();
				HashMap<Integer, Integer> instancesNotInDBCountMap = new HashMap<Integer, Integer>();
				
				
				for(FreebaseEvent f : instances) {
					for(String alias : f.aliases)
						aliasToInstance.put(alias.replaceAll("[\\s]+", " ").toLowerCase().trim(), f.name);
					
					if(f.startDateStr != null && f.startDateStr.length() > 9 && f.endDateStr == null)
						if(f.name.toLowerCase().contains("award") || f.name.toLowerCase().contains("miss") || f.name.toLowerCase().contains("bowl")
								|| f.name.toLowerCase().contains("final") || f.name.toLowerCase().contains("prize") || f.name.toLowerCase().contains("prix"))
						f.endDateStr = f.startDateStr;
					
					instanceToStartAndEndDate.put(f.name, new Pair<String, String>(f.startDateStr, f.endDateStr));
					
					if(f.startDateStr == null) {
						instancesWithNoStartDates.add(f.name);
						instancesAlreadyPresentLog.add(f.name);
					} else {
						int year = Integer.parseInt(f.startDateStr.substring(0, 4));
						yearToInstance.put(year, f.name);
						instancesAlreadyPresentLog.add(year);
						if(f.startDateStr.length() <= 5)
							instancesWithNoStartDates.add(year);
					}
				}
				
				String queryString = QueryParser.escape(metaEvent.toLowerCase().trim().replaceAll("[ ]+", "_"));
				TopDocs topDocs;
				
		        try{
		        
		          Query query = queryParser.parse(queryString);
		          topDocs = indexSearcher.search(query,nResults);
		          HashSet<String> alreadyAdded = new HashSet<String>();
		          metaEventMentions += topDocs.scoreDocs.length;
		          
		          for(ScoreDoc scoreDoc : topDocs.scoreDocs){
		        	  
		        	  totalUniqueMetaEvents.add(metaEvent);
		        	  
		              Document luceneDoc = indexSearcher.doc(scoreDoc.doc);
		              //System.out.println("\n\nID : " + scoreDoc.doc);
		              String text = luceneDoc.get("text");
		              String events = luceneDoc.get("events");
		              if(events.split(" ").length > 1) {
		            	  //System.out.println("\tMore than one event : " + events);
		            	  docsWithMutipleEvents.add(scoreDoc.doc);
		            	  continue;
		              }
		              
		              docsWithSingleEvent++;
		              
		              String normalizedText = text.replaceAll("[^A-Za-z0-9 ]", "").replaceAll("[\\s]+", " ").trim();
		              if(alreadyAdded.contains(normalizedText)) {
		            	  //System.out.println("\tText already present");
		            	  //System.out.println("\t"+ normalizedText);
		            	  continue;
		              }
		              
		              alreadyAdded.add(normalizedText);
		              
		              String annStr = luceneDoc.get("annDoc");
		              alreadyAdded.add(text);
		              List<CoreMap> sentences = idToSentences(annStr);
		              
		              HashSet<String> alreadyAddedInstance = new HashSet<String>();
		              HashSet<String> instancesPresent = new HashSet<String>();
		              
		              ArrayList<EventAnn> allInstanceAnnotations = new ArrayList<EventAnn>();
		              ArrayList<EventAnn> allNonSubsumedMetaAnnotations = new ArrayList<EventAnn>();
		              
		              ArrayList<HashSet<Integer>> eventTokensList = new ArrayList<HashSet<Integer>>();
		              ArrayList<DateAnn> allDatesInDoc = new ArrayList<DateAnn>();
		              ArrayList<DateAnn> allDatesWithAtleastMonthInDoc = new ArrayList<DateAnn>();
		              
    				  boolean atleastMonthPresent = false;
    					 
		              if(sentences != null) {
		            	  
		            	  int tokenSoFar = 0;
		            	  for (int i = 0; i < sentences.size(); i++) {
				      			
		            		HashSet<Integer> eventTokens = new HashSet<Integer>();
		            				  
			            	CoreMap sentence = sentences.get(i);
			      			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			      			
			      			ArrayList<DateAnn> dates = extractDates(tokens);
				      		if(dates.size() > 0) {
				      			for (int j = 0; j < dates.size(); j++) {
				      				
				      				String normalizedDate = dates.get(j).normalizedDate;
			    					DATETYPE dt = GetDateType(normalizedDate);
			    					
			    					DateAnn d = dates.get(j);
			    					
			    					if(dt.equals(DATETYPE.DMY) || dt.equals(DATETYPE.DM)
			    							|| dt.equals(DATETYPE.M) || dt.equals(DATETYPE.MY)) {
			    						atleastMonthPresent = true;
			    						allDatesWithAtleastMonthInDoc.add(new DateAnn(d.normalizedDate, d.beginToken+tokenSoFar, d.endToken+tokenSoFar, d.dateStr));
			    					}
			    					
				      				allDatesInDoc.add(new DateAnn(d.normalizedDate, d.beginToken+tokenSoFar, d.endToken+tokenSoFar, d.dateStr));
								}
				      		}
				      			
			      			String sentenceText = GetSentenceText(tokens, true);
			      			ArrayList<EventAnn> instanceAnnotations = GetEventMentions(aliasToInstance.keySet(), sentenceText, tokens, aliasToInstance);
			      			allInstanceAnnotations.addAll(instanceAnnotations);
			      			for (EventAnn eventAnn : instanceAnnotations) {
			    				
			      				for (int j = eventAnn.beginToken; j < eventAnn.endToken; j++)
			      					eventTokens.add(j);
								
			    				if(alreadyAddedInstance.contains(eventAnn.instance))
			    					continue;
			    				
			    				totalInstancesFound++;
			    				String key = eventAnn.instance;
			    				alreadyAddedInstance.add(key);
				    			totalUniqueInstances.add(key);
				    			instancesPresent.add(key);
			    			}
			    			
			    			ArrayList<EventAnn> metaAnnotations = GetEventMentions(new HashSet<String>(metaEventAliases), sentenceText, tokens, null);
			    			for(EventAnn metaAnn : metaAnnotations) {
		    					
		    					boolean isSubsumed = false;
		    					for (EventAnn instAnn : instanceAnnotations) {
		    						if(metaAnn.beginToken >= instAnn.beginToken && metaAnn.endToken <= instAnn.endToken) {
		    							isSubsumed = true;
		    							break;
		    						}
		    					}
		    					
		    					if(!isSubsumed) {
		    						allNonSubsumedMetaAnnotations.add(new EventAnn(metaAnn.text, metaAnn.instance, metaAnn.beginToken + tokenSoFar, metaAnn.endToken + tokenSoFar));
		    						for (int j = metaAnn.beginToken; j < metaAnn.endToken; j++)
					      				eventTokens.add(j);
		    						
		    					}
		    				}
		    				
		    				eventTokensList.add(eventTokens);
		    				tokenSoFar += tokens.size();
			              }
		            	  
		            	  String paraText = GetParaText(sentences).replaceAll("[\\s;]+", " ").trim();
		            	  
		            	  HashSet<Integer> instanceYearsInDB = new  HashSet<Integer>();
		            	  HashSet<Integer> instanceYearsNotInDB = new  HashSet<Integer>();
	            		  
		            	  if(instancesPresent.size() + allNonSubsumedMetaAnnotations.size() == 0) {
		            		  //System.out.println("\t");
		            		  continue;
		            	  }
		            		  
		            		  
	            		  if(instancesPresent.size() > 0) {
	            			  for(String str : instancesPresent) {
	            				  int year = Integer.parseInt(instanceToStartAndEndDate.get(str).getFirst().substring(0, 4));
	            				  instanceYearsInDB.add(year);
	            			  }
	            		  }
	            		  
	            		  int minDistance = Integer.MAX_VALUE;
	            		  int year = -1;
	            		  if(allNonSubsumedMetaAnnotations.size() > 0) {
	            			  
	            			  for (int i = 0; i < allNonSubsumedMetaAnnotations.size(); i++) {
								for (int j = 0; j < allDatesInDoc.size(); j++) {
									DateAnn d = allDatesInDoc.get(j);
									EventAnn e = allNonSubsumedMetaAnnotations.get(i);
									int distance = 0;
									if(d.endToken < e.beginToken)
										distance = e.beginToken - d.endToken;
									else if(e.endToken < d.beginToken)
										distance = d.beginToken - e.endToken;
									
									if(distance < minDistance) {
										try{
											if(d.normalizedDate.length() < 4) {
												//System.err.println("Date Length Error : " + d.normalizedDate + " " + d.dateStr);
											} else {
												year = Integer.parseInt(d.normalizedDate.substring(0, 4));
												minDistance = distance;
											}
										} catch (NumberFormatException eNum) {
											//System.err.println("Normalization Error : " + d.normalizedDate + " " + d.dateStr);
										}
									}
								}
								if(year != -1) {
									if(yearToInstance.containsKey(year))
										instanceYearsInDB.add(year);
									else
										instanceYearsNotInDB.add(year);
								}
	            			  }
	            		  }
	            		  
	            		  int noOfUniqueInstances = instanceYearsInDB.size() + instanceYearsNotInDB.size();
	            		  for(int instanceYear : instanceYearsNotInDB) {
	            			 	if(instancesNotInDBCountMap.containsKey(instanceYear))
	            			 		instancesNotInDBCountMap.put(instanceYear, instancesNotInDBCountMap.get(instanceYear)+1);
	            			 	else
	            			 		instancesNotInDBCountMap.put(instanceYear,1);
	            		  }
	            		  
	            		  
	            		  //System.out.println(instanceYears);
	            		  
	            		  if(noOfUniqueInstances > 1) {
		            		  docsWithMultipleInstances++;
		            	  } else {
		            		  docsWithSingleInstance++;
		            	  }
		            	  
		            	  if(!atleastMonthPresent) {
		            		  //System.out.println("\tNo dates with month present");
		            		  continue;
		            	  }
		            	  
		            	  // instance less + year less is useless for train mode
		            	  if(noOfUniqueInstances == 0 && trainMode) {
		            		  //System.out.println("\tTrain Mode and YLIL");
		            		  continue;
		            	  }
		            	  
		            	  /*if((instanceYearsInDB.size() + instanceYearsNotInDB.size()) > 1) {
		            		  System.out.println(paraText);
			            	  System.out.println("Instances : " + allInstanceAnnotations);
			            	  System.out.println("Meta      : " + allNonSubsumedMetaAnnotations);
			            	  System.out.println("Dates     : " + allDatesWithAtleastMonthInDoc);
		            		  System.out.println(instanceYearsInDB);
		            		  System.out.println(instanceYearsNotInDB);
		            		  System.out.println("");
		            	  }*/
		            	
		            	  if(noOfUniqueInstances > 0) {
		            		  
		            		  for (int i = 0; i < sentences.size(); i++) {
				            	  
				            	  CoreMap sentence = sentences.get(i);
					      		  List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
					      		
						      		ArrayList<DateAnn> dates = extractDates(tokens);
					      			
						      		if(dates.size() > 0) {
						      			
						      			for (int j = 0; j < dates.size(); j++) {
					      					
					      					String normalizedDate = dates.get(j).normalizedDate;
					      					DATETYPE dt = GetDateType(normalizedDate);
					      					
					      					if(dt.equals(DATETYPE.DMY) || dt.equals(DATETYPE.MY)) {
					      						
					      						int dateYear = Integer.parseInt(normalizedDate.substring(0, 4));
					      						
					      						if(instanceYearsInDB.contains(dateYear) && dt.equals(DATETYPE.DMY)) {
					      							
					      							String instance = yearToInstance.get(dateYear);
					      							String instancesPresentStr = ";INSTANCE#" + instance + ";" + paraText;
					      							
					      							String gtStartDate = instanceToStartAndEndDate.get(instance).getFirst();
					      							DateCompareType startDateMatch = CompareDate(gtStartDate, normalizedDate);
					      							
					      							String gtEndDate = instanceToStartAndEndDate.get(instance).getSecond();
					      							DateCompareType endDateMatch = CompareDate(gtEndDate, normalizedDate);
					      							
					      							String classType = "UNKNOWN";
					      							int classId = UNKNOWN_LABEL;
					      							
					      							if(startDateMatch == DateCompareType.EQUAL && endDateMatch == DateCompareType.BEFORE) {
					      								classId = START_LABEL;
					      								classType = "START";
					      								uniqueStartsWithInstances.add(instance);
					      								if(trainMode)
					      									uniqueStartsWithInstancesTr.add(instance);
					      							
					      							} else if(startDateMatch == DateCompareType.AFTER && endDateMatch == DateCompareType.EQUAL) {
					      								classId = END_LABEL;
					      								classType = "END";
					      								uniqueEndsWithInstances.add(instance);
					      								if(trainMode)
					      									uniqueEndsWithInstancesTr.add(instance);
					      							
					      							} else if(startDateMatch == DateCompareType.EQUAL && endDateMatch == DateCompareType.EQUAL) {
					      								classId = START_LABEL;
					      								classType = "START";
					      								uniqueStartsWithInstances.add(instance);
					      								if(trainMode)
					      									uniqueStartsWithInstancesTr.add(instance);
					      							
					      							} else if(startDateMatch == DateCompareType.AFTER && endDateMatch == DateCompareType.BEFORE) {
					      								classId = HOLD_LABEL;
					      								classType = "HOLD";
					      								uniqueHoldsWithInstances.add(instance);
					      								if(trainMode)
					      									uniqueHoldsWithInstancesTr.add(instance);
					      								
					      							} else if(startDateMatch == DateCompareType.BEFORE || endDateMatch == DateCompareType.AFTER) {
					      								classId = OTHER_LABEL;
					      								classType = "OTHER";
					      							}
					      							
						      						/*System.out.println("---------------------------");
					      							System.out.println("\tANNOTATION TYPE:" + classType + "\t" + instancesPresent);
				      								System.out.println(normalizedDate + ", " + gtStartDate + ", " + startDateMatch.toString());
				      								System.out.println(normalizedDate + ", " + gtEndDate + ", " + endDateMatch.toString());
				      								System.out.println(paraText);
				      								System.out.println("");*/
						      						
						      						if(trainMode) {
					      								if(classId != UNKNOWN_LABEL) { 
					      									sbTrain.append(GetFeatures(dates.get(j), sentence, classId+"", instancesPresentStr, eventTokensList.get(i)));
					      								} else {
					      									if(gtStartDate == null || gtStartDate.length() < 10)
					      										sbTrainFreebaseCompletion.append(GetFeatures(dates.get(j), sentence, classId+"", instancesPresentStr, eventTokensList.get(i)));
					      								}
					      								labelsToCount.put(classType+"-TRAIN", labelsToCount.get(classType+"-TRAIN")+1);
				      								} else {
				      									
				      									sbTest.append(GetFeatures(dates.get(j), sentence, classId+"", instancesPresentStr, eventTokensList.get(i)));
				      									labelsToCount.put(classType+"-TEST", labelsToCount.get(classType+"-TEST")+1);
				      								}
					      							
					      						} else if(instanceYearsNotInDB.contains(dateYear)) {
					      							// initially skipped in train mode as we do know the start and end dates of the instance
					      							// but we now use it for Freebase completion
					      							
					      							if(!trainMode) {
					      								String instancesPresentStr = ";INSTANCE-NOT-IN-DB#" + dateYear + " " + metaEvent;
					      								
					      								if(dt.equals(DATETYPE.MY)) {
						      								instancesPresentStr += ";NODAY";
						      								labelsToCount.put("UNKNOWN-NODAY", labelsToCount.get("UNKNOWN-NODAY")+1);
						      							} else {
						      								labelsToCount.put("UNKNOWN-TEST", labelsToCount.get("UNKNOWN-TEST")+1);
						      							}
					      								
					      								instancesPresentStr += ";" + paraText;
						      							sbTest.append(GetFeatures(dates.get(j), sentence, UNKNOWN_LABEL+"", instancesPresentStr, eventTokensList.get(i)));
					      							} else { 
					      								// add so we can perform freebase completion
					      								
					      								String instancesPresentStr = ";INSTANCE-NOT-IN-DB#" + dateYear + " " + metaEvent + ";" + paraText;
					      								
					      								if(dt.equals(DATETYPE.MY))
						      								instancesPresentStr += ";NODAY";
					      								
					      								sbTrainFreebaseCompletion.append(GetFeatures(dates.get(j), sentence, UNKNOWN_LABEL+"", instancesPresentStr, eventTokensList.get(i)));
					      							}
					      						}
					      						
					      					} else if(dt.equals(DATETYPE.DM) || dt.equals(DATETYPE.M)) {
					      						
					      						if(trainMode && dt.equals(DATETYPE.DM)) {
					      							
						      						for(int instanceYear : instanceYearsInDB) {
						      							
						      							String instance = yearToInstance.get(instanceYear);
						      							String instancesPresentStr = ";INSTANCE#" + instance + ";" + paraText;
						      							
						      							String gtStartDate = instanceToStartAndEndDate.get(instance).getFirst();
						      							DateCompareType startDateMatch = CompareDate(gtStartDate, normalizedDate);
						      							
						      							String gtEndDate = instanceToStartAndEndDate.get(instance).getSecond();
						      							DateCompareType endDateMatch = CompareDate(gtEndDate, normalizedDate);
						      							
						      							String classType = "UNKNOWN";
						      							int classId = UNKNOWN_LABEL;
						      							
						      							if(startDateMatch == DateCompareType.EQUAL && endDateMatch == DateCompareType.BEFORE) {
						      								classId = START_LABEL;
						      								classType = "START";
						      								uniqueStartsWithInstances.add(instance);
						      								if(trainMode)
						      									uniqueStartsWithInstancesTr.add(instance);
						      							
						      							} else if(startDateMatch == DateCompareType.AFTER && endDateMatch == DateCompareType.EQUAL) {
						      								classId = END_LABEL;
						      								classType = "END";
						      								uniqueEndsWithInstances.add(instance);
						      								if(trainMode)
						      									uniqueEndsWithInstancesTr.add(instance);
						      							
						      							} else if(startDateMatch == DateCompareType.EQUAL && endDateMatch == DateCompareType.EQUAL) {
						      								classId = START_LABEL;
						      								classType = "START";
						      								uniqueStartsWithInstances.add(instance);
						      								if(trainMode)
						      									uniqueStartsWithInstancesTr.add(instance);
						      							
						      							} else if(startDateMatch == DateCompareType.AFTER && endDateMatch == DateCompareType.BEFORE) {
						      								classId = HOLD_LABEL;
						      								classType = "HOLD";
						      								uniqueHoldsWithInstances.add(instance);
						      								if(trainMode)
						      									uniqueHoldsWithInstancesTr.add(instance);
						      							}
						      							
						      							/*System.out.println("---------------------------");
						      							System.out.println("\tANNOTATION TYPE:" + classType + "\t" + instancesPresent);
					      								System.out.println(normalizedDate + ", " + gtStartDate + ", " + startDateMatch.toString());
					      								System.out.println(normalizedDate + ", " + gtEndDate + ", " + endDateMatch.toString());
					      								System.out.println(paraText);
					      								System.out.println("");*/
							      						
							      						if(classId != UNKNOWN_LABEL) {
						      								sbTrain.append(GetFeatures(dates.get(j), sentence, classId+"", instancesPresentStr, eventTokensList.get(i)));
						      								labelsToCount.put(classType+"-TRAIN", labelsToCount.get(classType+"-TRAIN")+1);
						      								break;
							      						}
						      							
						      						}
						      						
					      						} else { // test mode
					      							
					      							String instancesPresentStr = "";
					      							for(int instanceYear : instanceYearsNotInDB) 
					      								instancesPresentStr += ";INSTANCE-NOT-IN-DB#" + instanceYear + " " + metaEvent;
					      							for(int instanceYear : instanceYearsInDB) 
					      								instancesPresentStr += ";INSTANCE#" + yearToInstance.get(instanceYear);
					      							if(dt.equals(DATETYPE.M)) {
					      								instancesPresentStr += ";NODAY";
					      								labelsToCount.put("UNKNOWN-NODAY", labelsToCount.get("UNKNOWN-NODAY")+1);
					      							} else {
					      								labelsToCount.put("UNKNOWN-TEST", labelsToCount.get("UNKNOWN-TEST")+1);
					      							}
					      							instancesPresentStr += ";" + paraText;
					      							
					      							sbTest.append(GetFeatures(dates.get(j), sentence, UNKNOWN_LABEL+"", instancesPresentStr, eventTokensList.get(i)));
					      							
					      						}
					      					}
						      			}
						      		}
		            		  }
		            	  }
		            	  
		            	  
		            	  // year less + instance less
		            	  if(noOfUniqueInstances == 0) {
		            		  String instancesPresentStr = ";META#" + metaEvent;
			    			  instancesPresentStr += ";" + paraText;
			    			  
			    			  for (int i = 0; i < sentences.size(); i++) {
				            	  
				            	  	CoreMap sentence = sentences.get(i);
				            	  	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
					      		
						      		ArrayList<DateAnn> dates = extractDates(tokens);
						      		for (int j = 0; j < dates.size(); j++) {
				      					
						      			String normalizedDate = dates.get(j).normalizedDate;
				      					DATETYPE dt = GetDateType(normalizedDate);
				      					
				      					if(dt.equals(DATETYPE.DM)) {
				      						labelsToCount.put("UNKNOWN-NOISNT", labelsToCount.get("UNKNOWN-NOISNT")+1);
				      						sbTest.append(GetFeatures(dates.get(j), sentence, UNKNOWN_LABEL+"", instancesPresentStr, eventTokensList.get(i)));
				      					} else if(dt.equals(DATETYPE.M)) {
				      						labelsToCount.put("UNKNOWN-NOISNT-NODAY", labelsToCount.get("UNKNOWN-NOISNT-NODAY")+1);
				      						sbTest.append(GetFeatures(dates.get(j), sentence, UNKNOWN_LABEL+"", instancesPresentStr, eventTokensList.get(i)));
				      					} else {
				      						/*System.out.println(scoreDoc.doc);
				      						System.out.println(paraText);
							            	  System.out.println("Instances : " + allInstanceAnnotations);
							            	  System.out.println("Meta      : " + allNonSubsumedMetaAnnotations);
							            	  System.out.println("Dates     : " + allDatesWithAtleastMonthInDoc);
						            		  System.out.println(instanceYearsInDB);
						            		  System.out.println(instanceYearsNotInDB);
						            		  System.out.println("");
				      						System.out.println("");*/
				      					}
					      			}
			    			  }
		            	  }
		            	  
		              }		
		          
			          if(sbTrain.length() > 100000) {
			        	  CommonFunctions.AppendToFile(feDir + File.separator + "train.txt", sbTrain.toString());
			        	  sbTrain = new StringBuffer();
			          }
			          
			          if(sbTrainFreebaseCompletion.length() > 100000) {
			        	  CommonFunctions.AppendToFile(feDir + File.separator + "freebase_completion.txt", sbTrainFreebaseCompletion.toString());
			        	  sbTrainFreebaseCompletion = new StringBuffer();
			          }
			          
			          if(sbTest.length() > 100000) {
			        	  CommonFunctions.AppendToFile(feDir + File.separator + "test.txt", sbTest.toString());
			        	  sbTest = new StringBuffer();
			          }
			          
		          }
		          
		        } catch(Exception e) {
		          e.printStackTrace();
		          System.out.println("Exception doing lucene search (searchString="+queryString+") in getDocList.");
		        }
		        
		       // if(!trainMode) {
		        	JSONArray localNewInstancesLog = new JSONArray();
		        	for(Entry<Integer, Integer> e : instancesNotInDBCountMap.entrySet()) {
		        		org.json.simple.JSONObject newInstanceEntry = new org.json.simple.JSONObject();
		        		newInstanceEntry.put("year", e.getKey());
		        		newInstanceEntry.put("count", e.getValue());
		        		localNewInstancesLog.add(newInstanceEntry);
		        	}
		        	
		        	if(localNewInstancesLog.size() > 0) {
		        		org.json.simple.JSONObject metaEventEntryToLog = new org.json.simple.JSONObject();
				        
		        		metaEventEntryToLog.put("meta-event", metaEvent);
		        		metaEventEntryToLog.put("in-db", instancesAlreadyPresentLog);
		        		metaEventEntryToLog.put("in-db-but-no-start-dates", instancesWithNoStartDates);
		        		metaEventEntryToLog.put("not-in-db", localNewInstancesLog);
		        		
		        		globalNewInstancesLog.add(metaEventEntryToLog);
		        	}
		       // }
			}
		}

		CommonFunctions.AppendToFile(feDir + File.separator + "train.txt", sbTrain.toString());
		CommonFunctions.AppendToFile(feDir + File.separator + "freebase_completion.txt", sbTrainFreebaseCompletion.toString());
		CommonFunctions.AppendToFile(feDir + File.separator + "test.txt", sbTest.toString());
		printFeatureToIdMap(feDir + File.separator + "features.txt");
		
		org.json.simple.JSONObject finalJson = new org.json.simple.JSONObject();
		finalJson.put("meta-events", globalNewInstancesLog);
		
		CommonFunctions.WriteToFile(feDir + File.separator + "new.log", finalJson.toJSONString());
		
		System.out.println("Total Meta Events in DB            : " + totalMetaEventsInDB);
		System.out.println("Total Unique Meta Events in Corpus : " + totalUniqueMetaEvents.size());
		System.out.println("Total Event Mentions in Corpus     : " + metaEventMentions);
		
		System.out.println("\nTotal Instances in DB              : " + totalInstancesInDB);
		System.out.println("Total Unique Instances in Corpus   : " + totalUniqueInstances.size());
		System.out.println("Total Instance Mentions in Corpus  : " + totalInstancesFound);
		
		System.out.println("\nTotal Unique Instances With Start Dates in Corpus       : " +  uniqueStartsWithInstancesTr.size() + ", " 
																						  + (uniqueStartsWithInstances.size() - uniqueStartsWithInstancesTr.size()) + ", "+ uniqueStartsWithInstances.size());
		
		System.out.println("Total Unique Instances With End Dates in Corpus           : " + uniqueEndsWithInstancesTr.size()+ ", " 
				  																	    + (uniqueEndsWithInstances.size() - uniqueEndsWithInstancesTr.size()) + ", "+ uniqueEndsWithInstances.size());
		
		System.out.println("Total Unique Instances With Holds Dates in Corpus         : " + uniqueHoldsWithInstancesTr.size() + ", "
																						  + (uniqueHoldsWithInstances.size() - uniqueHoldsWithInstancesTr.size()) + ", "+ uniqueHoldsWithInstances.size());
		
		System.out.println("\nTotal Instance Mentions With Start Dates      : " + labelsToCount.get("START-TRAIN") + ", " + labelsToCount.get("START-TEST") +", " +
																				(labelsToCount.get("START-TRAIN") + labelsToCount.get("START-TEST")));
		
		System.out.println("Total Instance Mentions With End Dates        : " + labelsToCount.get("END-TRAIN") + ", " + labelsToCount.get("END-TEST") +", " +
																				(labelsToCount.get("END-TRAIN") + labelsToCount.get("END-TEST")));
		
		System.out.println("Total Instance Mentions With Holds Date       : " + labelsToCount.get("HOLD-TRAIN") + ", " + labelsToCount.get("HOLD-TEST") +", " +
																				(labelsToCount.get("HOLD-TRAIN") + labelsToCount.get("HOLD-TEST")));
		
		System.out.println("Total Instance Mentions With Other Date       : " + labelsToCount.get("OTHER-TRAIN") + ", " + labelsToCount.get("OTHER-TEST") +", " +
																				(labelsToCount.get("OTHER-TRAIN") + labelsToCount.get("OTHER-TEST")));
		
		
		System.out.println("\nTotal Instance With No Day  : " + labelsToCount.get("UNKNOWN-NODAY"));
		System.out.println("\nTotal Non Instance Mentions With Day Month  : " + labelsToCount.get("UNKNOWN-NOISNT"));
		System.out.println("Total Non Instance Mentions With Just Month  : " + labelsToCount.get("UNKNOWN-NOISNT-NODAY"));
		
		System.out.println("\n Docs Ignored (more than one meta event) : " + docsWithMutipleEvents.size());
		System.out.println("\n Docs Considered   (just one meta event) : " + docsWithSingleEvent);
		
		System.out.println("\n Docs with more than one instance : " + docsWithMultipleInstances);
		System.out.println("\n Docs with just one instance      : " + docsWithSingleInstance);
		
		indexSearcher.close();
	}
	
	public static String GetInstanceIdentifier(List<CoreLabel> tokens, int begin, int end) {
		for (int j = begin-1; j < end+1; j++) {
			if(j<0 || j >  tokens.size()-1)
				continue;
			if(j>= begin && j < end)
				continue;
			CoreLabel t = tokens.get(j);
			String posTag = t.get(PartOfSpeechAnnotation.class);
			String ner = t.ner();
			
			if( (posTag.equals("CD") && ner.equals("DATE"))
					|| (posTag.equals("JJ") && ner.equals("ORDINAL")) )
				return t.getString(TextAnnotation.class);
		}
		
		return null;
	}
	private static String GetParaText(List<CoreMap> sentences) {
		String paraText = "";
		for (int i = 0; i < sentences.size(); i++) {
      	  
      	  	CoreMap sentence = sentences.get(i);
    		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			paraText += GetSentenceText(tokens, false)+ " ";
		}
		paraText = paraText.trim();
		
		return paraText;
	}
	
	public static String GetSentenceText(List<CoreLabel> tokens, boolean toLowercase) {
		String sentenceText = "";
		for (int j = 0; j < tokens.size(); j++)
			sentenceText += tokens.get(j).getString(TextAnnotation.class)+ " ";
		if(toLowercase)
			sentenceText = sentenceText.toLowerCase().trim();
		else
			sentenceText = sentenceText.trim();
		
		return sentenceText;
	}
	
	public static List<CoreMap> idToSentences(String annStr) {
		
		List<CoreMap> rtn = null; 
		Annotation docAnn = null;
		
		try {
			InputStream is = new ByteArrayInputStream(annStr.getBytes("UTF-8"));
			AnnotationSerializer annSer = new CustomAnnotationSerializer(false, false);
			docAnn = annSer.read(is).first();
			
		} catch (Exception e) {
			//e.printStackTrace();
			return rtn;
		}
		
		if(docAnn != null)
			rtn = docAnn.get(SentencesAnnotation.class);
		
		return rtn;
	}

	enum DATETYPE {
		DMY, MY, M, DM, O
	}
	
	private static DATETYPE GetDateType(String date) {
		
		if(date.length() > 10)
			date = date.substring(0, 10);
		
		if(date.length() == 10 && date.replaceAll("\\d{4}-\\d{2}-\\d{2}", "").equals("")) {
			return DATETYPE.DMY;
		} else if(date.length() == 7 && date.replaceAll("\\d{4}-\\d{2}", "").equals("")) {
			return DATETYPE.MY;
		} else if(date.length() == 7 && date.replaceAll("XXXX-\\d{2}", "").equals("")) {
			return DATETYPE.M;
		} else if(date.length() == 10 && date.replaceAll("XXXX-\\d{2}-\\d{2}", "").equals("")) {
			return DATETYPE.DM;
		} else {
			return DATETYPE.O;
		}
	}
	
	private static ArrayList<DateAnn> extractDates(List<CoreLabel> tokens) {
		
		ArrayList<DateAnn> dates = new ArrayList<DateAnn>();
		
		try {
			String prevNER = "";
			String dateStr = "";
	  	    String date = null;
	  	    int begin = -1;
	  	    
	  	    for (int k = 0; k < tokens.size(); k++) {
	  	    	
	  	    	CoreLabel t = tokens.get(k);
	  	    	
	  	    	if(t.ner().equals("DATE") || t.ner().equals("TIME")) {
	  	    		if(!(prevNER.equals("DATE")) && !(prevNER.equals("TIME"))) {
	  	    			dateStr = t.getString(TextAnnotation.class);
	  	    			date = t.get(NormalizedNamedEntityTagAnnotation.class);
	  	    			begin = k;
	  	    		} else {
	  	    			if(!date.equals(t.get(NormalizedNamedEntityTagAnnotation.class))) {
	  	    				if(!date.replaceAll("[a-zA-Z\\-_]+", "").trim().equals("")) {
	  	    					DateAnn dateAnn = new DateAnn(date, begin, k, dateStr);
	  	    					dates.add(dateAnn);
	  	  	    			}
	  	    				dateStr = t.getString(TextAnnotation.class);
		  	    			date = t.get(NormalizedNamedEntityTagAnnotation.class);
		  	    			begin = k;
	  	    			} else {
	  	    				dateStr = dateStr + " " + t.getString(TextAnnotation.class);
	  	    			}
	  	    		}
	  	    	}
	  	    	
	  	    	if(!t.ner().equals("DATE") && !t.ner().equals("TIME")) {
	  	    		if(date != null) {
	  	    			if(!date.replaceAll("[a-zA-Z\\-_]+", "").trim().equals("")) {
	  	    				DateAnn dateAnn = new DateAnn(date, begin, k, dateStr);
  	    					dates.add(dateAnn);
	  	    			}
	  	    			date = null;
	  	    			dateStr = null;
	  	    		}
	  	    	}
	  	    				  	    	
	  	    	prevNER = t.ner();
	  	    }
	  	    
	  	    if(date != null) {
	  	    	if(!date.replaceAll("[a-zA-Z\\-_]+", "").trim().equals("")) {
	  	    		DateAnn dateAnn = new DateAnn(date, begin, tokens.size(), dateStr);
  					dates.add(dateAnn);
	  			}
	  		}
		} catch (NullPointerException e) {
			//e.printStackTrace();
		}
  	    
  	    return dates;
	}
	
	public static HashSet<String> GetNounPhrases(HashSet<String> npPositions, List<CoreLabel> tokens) {
		
		HashSet<String> nounPhrases = new HashSet<String>();
		
		for(String position : npPositions) {
			String[] splitData = position.split("#");
			int begin = Integer.parseInt(splitData[0]);
			int end = Integer.parseInt(splitData[1]);
			String nounPhrase = "";
			if(begin-1 < 0) {
				System.out.println("Begin id is " + (begin-1));
				continue;
			}
			
			if(end-1 > tokens.size()) {
				System.out.println("End id is " + (end-1));
				continue;
			}
			
			for (int j = begin-1; j < end-1; j++)
				nounPhrase += tokens.get(j).getString(TextAnnotation.class)+ " ";
			nounPhrases.add(nounPhrase.replaceAll("[\\s]+", " ").toLowerCase().trim());
		}
		
		return nounPhrases;
	}

	public static HashSet<String> GetNounPhrases(Tree ParseTree) {
		
		HashSet<String> nounPhrases = new HashSet<String>();
		
		try {
			ParseTree.indexLeaves();
			Queue<Tree> nodesToTraverse = new LinkedList<Tree>();
			nodesToTraverse.add(ParseTree);
			while(!nodesToTraverse.isEmpty()) {
				Tree tempNode = nodesToTraverse.poll();
				if(tempNode.isPrePreTerminal()) {
					if(tempNode.label().value().equals("NP")) {
						List<Tree> children = tempNode.getChildrenAsList();
						
						int begin = -1;
						String[] splitData = children.get(0).children()[0].label().toString().split("\\-");
						String position = splitData[splitData.length-1];
						if(children.get(0).label().value().equals("DT")
								|| children.get(0).label().value().equals("PRP$")
								|| children.get(0).label().value().equals("PRP"))
							begin = Integer.parseInt(position)+1;
						else
							begin = Integer.parseInt(position);
						
						splitData = children.get(children.size()-1).children()[0].label().toString().split("\\-");
						position = splitData[splitData.length-1];
						int end = Integer.parseInt(position)+1;
						if(begin < end) {
							String key = begin + "#" + end;
							nounPhrases.add(key);
						}
					}
				} else {
					List<Tree> children = tempNode.getChildrenAsList();
					for (int i = 0; i < children.size(); i++)
						nodesToTraverse.add(children.get(i));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return nounPhrases;
		}
		return nounPhrases;
	}
	
	enum DateCompareType {EQUAL, BEFORE, AFTER, UNKNOWN};
	
	public static DateCompareType CompareDate(String reference, String extracted) {
		
		if(reference == null || reference.length() < 10
				|| extracted == null)
			return DateCompareType.UNKNOWN;
		
		int rYear  = Integer.parseInt(reference.substring(0, 4));
		int rMonth = Integer.parseInt(reference.substring(5, 7));
		int rDay   = Integer.parseInt(reference.substring(8, 10));
		
		int eYear = -1;
		if(!extracted.startsWith("XXXX"))
			eYear  = Integer.parseInt(extracted.substring(0, 4));
		
		int eMonth = -1;
		// as we expect the month and date to be present
		if(extracted.length() >= 7)
			eMonth  = Integer.parseInt(extracted.substring(5, 7));
		int eDay   = -1;
		if(extracted.length() >= 10)
			eDay   = Integer.parseInt(extracted.substring(8, 10));
		
		if(extracted.length() >= 10) {
			
			if(eYear == -1 || eYear == rYear) {
				if(eMonth == rMonth) {
					if(eDay == rDay)
						return DateCompareType.EQUAL;
					else if(eDay < rDay)
						return DateCompareType.BEFORE;
					else
						return DateCompareType.AFTER;
				} else if(eMonth < rMonth) {
					return DateCompareType.BEFORE;
				} else
					return DateCompareType.AFTER;
				
			} else if(eYear < rYear)
				return DateCompareType.BEFORE;
			else
				return DateCompareType.AFTER;
				
		} 
		
		return DateCompareType.UNKNOWN;
	}
	
	public static ArrayList<EventAnn> GetEventMentions(Set<String> aliases, String sentenceText, List<CoreLabel> tokens, HashMap<String, String> aliasToInstance) {
		
		ArrayList<EventAnn> eventMentions = new ArrayList<EventAnn>();
		for (String alias : aliases) {
			
			try {
				int indexOf = sentenceText.indexOf(alias, 0);
				while(indexOf != -1) {
					int begin = indexOf;
					int end   = indexOf + alias.length();
					
					indexOf = sentenceText.indexOf(alias, begin+1);
					
					int beginToken = -1;
					int endToken   = -1;
					
					int k = 0;
					int currChar = 0;
					
					for (; k < tokens.size() && currChar <= begin; k++) {
						if(currChar == begin) {
							beginToken = k;
							break;
						}
						currChar += tokens.get(k).getString(TextAnnotation.class).length()+1;
					}
					
					currChar += tokens.get(k).getString(TextAnnotation.class).length();
					k++;
					if(currChar == end)
						endToken = k;
					else {
						for (;k < tokens.size(); k++) {
							if(currChar == end) {
								endToken = k;
								break;
							}
							currChar += tokens.get(k).getString(TextAnnotation.class).length()+1;
						}
						if(currChar == end)
							endToken = k;
					}
					
					if(beginToken != -1 && endToken != -1) {
						
						EventAnn eventAnn = null;
						if(aliasToInstance == null)
							eventAnn = new EventAnn(alias, null, beginToken, endToken);
						else
							eventAnn = new EventAnn(alias, aliasToInstance.get(alias), beginToken, endToken);
						eventMentions.add(eventAnn);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return eventMentions;
	}
	
	public static HashMap<String, ArrayList<String>> GetMetaEventAliases(HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents,
			WordnetWrapper wordnetWrapper) throws IOException {

		HashMap<String, ArrayList<String>> metaEventToAliases = new HashMap<String, ArrayList<String>>();
		
		HashMap<String, String> fbEventAliasesToBeAdded = new HashMap<String, String>();
		HashMap<String, ArrayList<String>> duplicateAliases = new HashMap<String, ArrayList<String>>();
		
		for (Entry<String, ArrayList<FreebaseEvent>> e : metaEventsToEvents.entrySet()) {

			if(e.getKey().equalsIgnoreCase("generation"))
				continue;
			
			HashSet<String> metaNames = FreebaseEvent.GetMetaEventNames(e.getKey(), e.getValue());
			
			for (String alias : metaNames) {
				
				String cleanedAlias = alias.replaceAll("[\\s]+", " ").toLowerCase().trim();

				if (FreebaseEvent.IsAliasInvalid(cleanedAlias)) {
					continue;
				}

				// to remove commonly used English words like Bad (MJ Conecert Name) and Rose
				if (!cleanedAlias.contains(" ")) {
					if (wordnetWrapper.isWord(cleanedAlias) 
							&& !cleanedAlias.equals("oktoberfest")) {
						continue;
					}
				}
				
				if(duplicateAliases.containsKey(cleanedAlias)) {
					ArrayList<String> metaEvents = duplicateAliases.get(cleanedAlias);
					metaEvents.add(e.getKey());
					duplicateAliases.put(cleanedAlias, metaEvents);
					
				} else if(fbEventAliasesToBeAdded.containsKey(cleanedAlias)) {
					ArrayList<String> metaEvents = new ArrayList<String>();
					metaEvents.add(fbEventAliasesToBeAdded.get(cleanedAlias));
					metaEvents.add(e.getKey());
					duplicateAliases.put(cleanedAlias, metaEvents);
					
					fbEventAliasesToBeAdded.remove(cleanedAlias);
				} else {
					fbEventAliasesToBeAdded.put(cleanedAlias, e.getKey());
				}
			}
		}
		
		HashMap<String, String> newNames = new HashMap<String, String>();
		for(String duplicate : duplicateAliases.keySet()) {
			
			ArrayList<String> values = duplicateAliases.get(duplicate);
			String newName = values.get(0);
			
			fbEventAliasesToBeAdded.put(duplicate, newName);
			
			for(int i=1; i< values.size(); i++)
				newNames.put(values.get(i), newName);
		}
		
		HashSet<String> recurrentEvents = new HashSet<String>();
		for(Entry<String, String> e : fbEventAliasesToBeAdded.entrySet()) {
			String metaEventName = e.getValue();
			
			if(newNames.containsKey(e.getValue()))
				metaEventName = newNames.get(e.getValue());
			
			recurrentEvents.add(metaEventName);
			if(e.getKey().contains(",")) {
				if(metaEventToAliases.containsKey(metaEventName)) {
					ArrayList<String> aliases = metaEventToAliases.get(metaEventName);
					aliases.add(e.getKey().replace(",", " ").replaceAll("[ ]+", " "));
					metaEventToAliases.put(metaEventName, aliases);
				} else {
					ArrayList<String> aliases = new ArrayList<String>();
					aliases.add(e.getKey().replace(",", " ").replaceAll("[ ]+", " "));
					metaEventToAliases.put(metaEventName, aliases);
				}
			}
			
			if(metaEventToAliases.containsKey(metaEventName)) {
				ArrayList<String> aliases = metaEventToAliases.get(metaEventName);
				aliases.add(e.getKey());
				metaEventToAliases.put(metaEventName, aliases);
			} else {
				ArrayList<String> aliases = new ArrayList<String>();
				aliases.add(e.getKey());
				metaEventToAliases.put(metaEventName, aliases);
			}
		}
		
		return metaEventToAliases;
	}
	
	public static final String START = "<S>";
	public static final String END = "</S>";
	
	public static HashMap<String, Integer> featureToIDMap = new HashMap<String, Integer>();
	
	public static void printFeatureToIdMap(String filepath) {
		
		StringBuffer sb = new StringBuffer();
		for(Entry<String, Integer> e : featureToIDMap.entrySet()) {
			sb.append(e.getValue() + "\t" + e.getKey() + "\n");
		}
		CommonFunctions.WriteToFile(filepath, sb.toString());
	}
	
	public static String GetFeatures(DateAnn dateAnn, CoreMap sentAnn, String label, String instance, HashSet<Integer> eventTokens) {
  		 
		ArrayList<Feature> features = GetFeatureList(dateAnn, sentAnn, eventTokens);
		
  		  String featureRow = label;
  		  for(Feature f : features) {
  			  String key = f.getValue();
  			  if(!featureToIDMap.containsKey(key)) {
  				  int id = featureToIDMap.size()+1;
  				  featureToIDMap.put(key, id);
  			  }
  			  featureRow += " " + featureToIDMap.get(key)+":1";
  		  }
  		  
  		String date = dateAnn.normalizedDate;
  		if(date.length() > 10)
  			date = date.substring(0, 10);
  		featureRow += " #" + date  + ((instance == null) ? "" : instance ) + "\n";
  		
  		return featureRow;
	}
	
	public static ArrayList<Feature> GetFeatureList(DateAnn dateAnn, CoreMap sentAnn, HashSet<Integer> eventTokens) {
		
		SemanticGraph graph =
				sentAnn.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
		
		List<CoreLabel> tokens = 
		  	      sentAnn.get(TokensAnnotation.class);
		 
  	    
		int begin = dateAnn.beginToken;
		int end = dateAnn.endToken-1;
		
		ArrayList<Feature> features = new ArrayList<Feature>();
  	   
		//TODO: 
		// 1. Correct Feature Names in the Feature constructor
		// 2. Make a loop to read before and after
		
		
  	    //add 1before unigram
  		if(begin > 0){
  			features.add(new Feature("1 Before Unigram", "1BU-"+tokens.get(begin-1).getString(TextAnnotation.class).toLowerCase()));
  			features.add(new Feature("1 Before Unigram", "1BL-"+tokens.get(begin-1).lemma().toLowerCase()));
  			features.add(new Feature("1 Before Unigram", "1BPOS-"+tokens.get(begin-1).get(PartOfSpeechAnnotation.class)));
  			features.add(new Feature("1 Before Unigram", "1BNER-"+tokens.get(begin-1).ner()));
  		}
  		else
  			features.add(new Feature("1 Before Unigram", "1BU-"+START));
  		
  		//add 1after unigram
  		if(end+1 < tokens.size()){
  			features.add(new Feature("1 After Unigram", "1AU-"+tokens.get(end+1).getString(TextAnnotation.class).toLowerCase()));
  		    features.add(new Feature("1 After Unigram", "1AL-"+tokens.get(end+1).lemma().toLowerCase()));
  		    features.add(new Feature("1 After Unigram", "1APOS-"+tokens.get(end+1).get(PartOfSpeechAnnotation.class)));
  		    features.add(new Feature("1 After Unigram", "1ANER-"+tokens.get(end+1).ner()));
  		}else
  		    features.add(new Feature("1 After Unigram", "1AU-"+END));
  		    
  		//add 2before unigram
  		if(begin > 1){
  			features.add(new Feature("2 Before Unigram", "2BU-"+tokens.get(begin-2).getString(TextAnnotation.class).toLowerCase()));
  			features.add(new Feature("2 Before Unigram", "2BL-"+tokens.get(begin-2).lemma().toLowerCase()));
  			features.add(new Feature("2 Before Unigram", "2BPOS-"+tokens.get(begin-2).get(PartOfSpeechAnnotation.class)));
  			features.add(new Feature("2 Before Unigram", "2BNER-"+tokens.get(begin-2).ner()));
  		}
  		else
  		    features.add(new Feature("2 Before Unigram", "2BU-"+START));
  		    
  		//add 2after unigram
  		if(end < tokens.size() - 2){
  		    	features.add(new Feature("2 After Unigram", "2AU-"+tokens.get(end+2).getString(TextAnnotation.class).toLowerCase()));
  		    	features.add(new Feature("2 After Unigram", "2AL-"+tokens.get(end+2).lemma().toLowerCase()));
  		    	features.add(new Feature("2 After Unigram", "2APOS-"+tokens.get(end+2).get(PartOfSpeechAnnotation.class)));
  		    	features.add(new Feature("2 After Unigram", "2ANER-"+tokens.get(end+2).ner()));
  		}else
  		    	features.add(new Feature("2 After Unigram", "2AU-"+END));
  		    
  		    
	    //add 3before unigram
	    if(begin > 2){
	    	features.add(new Feature("3 Before Unigram", "3BU-"+tokens.get(begin-3).getString(TextAnnotation.class).toLowerCase()));
	    	features.add(new Feature("3 Before Unigram", "3BL-"+tokens.get(begin-3).lemma().toLowerCase()));
	    	features.add(new Feature("3 Before Unigram", "3BPOS-"+tokens.get(begin-3).get(PartOfSpeechAnnotation.class)));
	    	features.add(new Feature("3 Before Unigram", "3BNER-"+tokens.get(begin-3).ner()));
	    }
	    else{
	    	features.add(new Feature("3 Before Unigram", "3BU-"+START));
	    }
	    
	    //add 3after unigram
	    if(end < tokens.size() - 3){
	    	features.add(new Feature("3 After Unigram", "3AU-"+tokens.get(end+3).getString(TextAnnotation.class).toLowerCase()));
	    	features.add(new Feature("3 After Unigram", "3AL-"+tokens.get(end+3).lemma().toLowerCase()));
	    	features.add(new Feature("3 After Unigram", "3APOS-"+tokens.get(end+3).get(PartOfSpeechAnnotation.class)));
	    	features.add(new Feature("3 After Unigram", "3ANER-"+tokens.get(end+3).ner()));
	    }
	    else{
	    	features.add(new Feature("3 After Unigram", "3AU-"+END));
	    }
	    
	    for (int i = 0; i < tokens.size(); i++) {
	    	features.add(new Feature("Sentence Unigram", "SU-"+tokens.get(i).lemma().toLowerCase()));
		}
  		    
	  for(SemanticGraphEdge edge : graph.edgeIterable()){
	      
		  int depIdx = edge.getDependent().index()-1;
	      int govIdx = edge.getGovernor().index()-1;
	      
	      if(depIdx >= begin && depIdx <= end && (govIdx < begin || govIdx > end)){
  		        
	    	  //Incoming edge
  		        String govString = edge.getGovernor().toString();
  		        int govStringHyphIdx = govString.lastIndexOf('/');
  		        if(govStringHyphIdx < 1 || govStringHyphIdx > govString.length() - 1) continue;
  		        String govStringText = govString.substring(0,govStringHyphIdx).toLowerCase();
  		        String govStringTag = govString.substring(govStringHyphIdx + 1, govString.length());
  		        String relString = edge.getRelation().toString();
  		        
  		        //lexicalized feature
  		        features.add(new Feature("Dependency Parse Incoming Edge - Text", "LexIE-"+relString+"-"+govStringText));
  		        //unlexicalized feature
  		        features.add(new Feature("Dependency Parse Incoming Edge - Tag", "UnLexIE-"+relString+"-"+govStringTag));
  		        
  		        if(eventTokens.contains(govIdx))
	  		        features.add(new Feature("Dependency Parse Incoming Edge", "EvUnLexIE-"+relString));
	      }
	      
	      if(govIdx >= begin && govIdx <= end && (depIdx < begin || depIdx > end) ){
  		        
	    	  //outgoing edge
  		        String depString = edge.getDependent().toString();
  		        int depStringHyphIdx = depString.lastIndexOf('/');
  		        if(depStringHyphIdx < 1 || depStringHyphIdx > depString.length() - 1) continue;
  		        String depStringText = depString.substring(0,depStringHyphIdx).toLowerCase();
  		        String depStringTag = depString.substring(depStringHyphIdx + 1, depString.length());
  		        String relString = edge.getRelation().toString();
  		        
  		        //lexicalized feature
  		        features.add(new Feature("Dependency Parse Outgoing Edge - Text+Tag", "LexOE-"+relString+"-"+depStringText));
  		        //unlexicalized feature
  		        features.add(new Feature("Dependency Parse Outgoing Edge - Tag", "UnLexOE-"+relString+"-"+depStringTag));
  		        
  		      if(eventTokens.contains(depIdx))
  		    	   features.add(new Feature("Dependency Parse Outgoing Edge - Tag", "EvUnLexOE-"+relString));
  		      
	      }
	    }
  		  
  		return features;
	}
		
}
