package edu.iitd.nlp.ee.classify;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.simple.JSONObject;

import com.aliasi.io.LogLevel;
import com.aliasi.io.Reporter;
import com.aliasi.io.Reporters;
import com.aliasi.matrix.SparseFloatVector;
import com.aliasi.matrix.Vector;
import com.aliasi.stats.AnnealingSchedule;
import com.aliasi.stats.LogisticRegression;
import com.aliasi.stats.RegressionPrior;

import edu.iitd.nlp.ee.freebase.FreebaseEvent;
import edu.iitd.nlp.ee.utils.CommonFunctions;
import edu.iitd.nlp.ee.utils.Global;
import edu.iitd.nlp.ee.utils.Pair;
import edu.stanford.nlp.util.StringUtils;

public class LingpipeClassifier {
	
	enum FeaturesUsed {
		ALL, ALL_5, ALL_3, ALL_3_NO_SU, ALL_3_NO_SU_NO_UNI
	}
	
	enum ClassLabelStrategy {
		OTHER_0_START_HOLD_END_1, OTHER_HOLD_END_0_START_1, ALL_DIFF
	}
	
	static boolean DEBUG = false;

	public static HashMap<String, String> GetMetaEventsToMid(String allMetaEventsWithMidFile) throws IOException {
		 
		HashMap<String, String> metaEventsToMid = new HashMap<String, String>();
		 
		 BufferedReader br = new BufferedReader(new FileReader(allMetaEventsWithMidFile));
		 String line = br.readLine();

	     while (line != null) {
	    	 String[] split = line.split("\t");
	    	 metaEventsToMid.put(split[0].toLowerCase().replace("&amp;", "&").trim(), split[1].replace("https://www.freebase.com", ""));
	    	 line = br.readLine();
	     }
	     br.close();
		    
		 return metaEventsToMid;
	 }
	
	public static void main(String[] args) throws IOException {
		
		Global.props = StringUtils.argsToProperties(args);
		System.out.println("Running with the following properties...");
		for (Object key : Global.props.keySet()) {
			System.out.println(key + "\t" + Global.props.get(key));
		}
		
		String metaEventsJson = Global.props.getProperty("metaEventsJson");
		
		String eventsWithScheduleFile = Global.props.getProperty("eventsWithScheduleFile");
		HashSet<String> eventsWithSchedule = new HashSet<String>();
		org.json.simple.JSONArray json = CommonFunctions.readJsonArrayFromFile(eventsWithScheduleFile);
		for (int i = 0; i < json.size(); i++)
			eventsWithSchedule.add((String) json.get(i));
		
		String eventAliasFile = Global.props.getProperty("eventAliasFile");
		String freebaseEventsJson = Global.props.getProperty("freebaseEventsJson");
		Global.props.getProperty("allMetaEventsCsvFile");
		String allMetaEventsWithMidFile = Global.props.getProperty("allMetaEventsWithMidFile");
		HashMap<String, String> metaEventsToMid =  GetMetaEventsToMid(allMetaEventsWithMidFile);
		
		//boolean onlyMetaEventsWithoutInstances = true;
		//HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents = FreebaseEvent.GetRecurrentEventsFromJson(freebaseEventsJson, eventAliasFile,allMetaEventsCsvFile, onlyMetaEventsWithoutInstances);
		
		HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents = FreebaseEvent.GetRecurrentEventsFromJson(freebaseEventsJson, eventAliasFile);
		HashMap<String, String> instanceToGTStartDate = GetInstanceToGTStartDate(metaEventsToEvents, eventsWithSchedule);
		
		String trainFile = Global.props.getProperty("trainFile");
		String testFile = Global.props.getProperty("testFile");
		String featureFile = Global.props.getProperty("featureFile");
		String newInstanceFile = Global.props.getProperty("newInstanceFile");
		
		HashMap<String,HashMap<Integer, Integer>> metaEventToinstancesNotInDBCountMap = GetNewInstancesDetails(newInstanceFile);
		
		FeaturesUsed featuresUsed = FeaturesUsed.ALL_3_NO_SU;
		ClassLabelStrategy classLabelStrategy = ClassLabelStrategy.OTHER_0_START_HOLD_END_1;
		
		double prior = 0.5;
		
		String modelFilePath 	= "model" + File.separator + featuresUsed.toString() + "_" + classLabelStrategy.toString() + "_" + prior + ".bin";
		String logFilePrefix 	= "model" + File.separator + featuresUsed.toString() + "_" + classLabelStrategy.toString() + "_" + prior;
		String resultFilePath 	= "result" + File.separator + featuresUsed.toString() + "_" + classLabelStrategy.toString() + "_" + prior + ".log";
		
		
		HashMap<Integer, String> featureIdToStringMap = GetFeatureIdToStringMap(featureFile);
		HashMap<Integer, Integer> oldToNewFeatureID = FeatureSelection(trainFile, featureIdToStringMap, featuresUsed);
		
		LogisticRegression regression = null;
		File modelFile = new File(modelFilePath);
		if(modelFile.exists())
			regression = (LogisticRegression) CommonFunctions.ReadObject(modelFilePath);
		else {
			regression = Train(trainFile, featureIdToStringMap, oldToNewFeatureID, classLabelStrategy, logFilePrefix, prior);
			CommonFunctions.WriteObject(modelFilePath, regression);
		}
		
		HashMap<String, ArrayList<Pair<String, Double>>> instanceToStartDates 
				= Test(regression, testFile, featureIdToStringMap, oldToNewFeatureID, classLabelStrategy, resultFilePath, instanceToGTStartDate);
		
		WriteILPInput(instanceToGTStartDate, metaEventsToEvents, metaEventsJson, eventsWithSchedule, instanceToStartDates, metaEventToinstancesNotInDBCountMap, metaEventsToMid);
		
	}
	
	static class Data {
		
		boolean isInstance;
		boolean isNotInDB;
		boolean noDay;
		
		String date;
		String text;
		String event;
		
		public Data(boolean isInstance, boolean isNotInDB, boolean noDay, String date, String text, String event) {
			super();
			this.isInstance = isInstance;
			this.isNotInDB = isNotInDB;
			this.noDay = noDay;
			this.date = date;
			this.text = text;
			this.event = event;
		}
	}
	
	static class CleanData {
		
		Vector[] INPUTS;
		int[] OUTPUTS;
		
		Data[] DATA;
		
		String LOG;
		
		public CleanData(Vector[] iNPUTS, int[] oUTPUTS, Data[] dATA, String lOG) {
			super();
			INPUTS = iNPUTS;
			OUTPUTS = oUTPUTS;
			DATA = dATA;
			LOG = lOG;
		}
	}
	
	static class DataPoint {
		
		Vector INPUT;
		int OUTPUT;
		
		Data DATA;

		public DataPoint(Vector iNPUT, int oUTPUT, Data dATA) {
			super();
			INPUT = iNPUT;
			OUTPUT = oUTPUT;
			DATA = dATA;
		}
	}
	
	public static CleanData GetCleanData(String file, HashMap<Integer, String> featureIdToStringMap, 
			HashMap<Integer, Integer> oldToNewFeatureID, ClassLabelStrategy classLabelStrategy, boolean test) {
		
		BufferedReader br = null;
		
		int totalFeatures = oldToNewFeatureID.size();
		
		int totalLines = 0;
		int dataPointsAdded = 0;
		HashMap<Integer, Integer> labelDistribution = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> newLabelDistribution = new HashMap<Integer, Integer>();
		
		ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
		
		String log = "";
		
		try {

			String sCurrentLine = null;
			br = new BufferedReader(new FileReader(file));
			while ((sCurrentLine = br.readLine()) != null) {
				
				totalLines++;
				
				int indexOfHash = sCurrentLine.indexOf('#');
				
				String[] features = sCurrentLine.substring(0, indexOfHash).split(" ");
				
				int label = Integer.parseInt(features[0]);
				if(labelDistribution.containsKey(label))
					labelDistribution.put(label, labelDistribution.get(label)+1);
				else
					labelDistribution.put(label, 1);
				
				switch (classLabelStrategy) {
						
					case OTHER_HOLD_END_0_START_1:
						if(label == 1)
							label = 1;
						else if(label >= 2 && label  <= 4)
							label = 0;
						break;
						
					default:
					case OTHER_0_START_HOLD_END_1:
						if(label == 4)
							label = 0;
						else if(label >= 1 && label  <= 3)
							label = 1;
						break;
				}
				
				if(!test && label == -1)
					continue;
				
				String[] metaData = sCurrentLine.substring(indexOfHash+1).replace("&amp;", "&").split(";");
				
				String date = metaData[0];
				
				boolean noDay = false;
				
				int endIndex = metaData.length-2;
				if(metaData[metaData.length-2].equals("NODAY")) {
					noDay = true;
					endIndex = metaData.length-3;
				}
				
				for (int k = 1; k <= endIndex; k++) {
					
					boolean isInstance = false;
					boolean notInDB = false;
					String event = null;
					
					if(metaData[k].startsWith("META#")) {
						event = metaData[k].replaceFirst("META#", "");
					} else if(metaData[k].startsWith("INSTANCE#")) {
						isInstance = true;
						event = metaData[k].replaceFirst("INSTANCE#", "").replace("&", "&amp;");
					} else {
						isInstance = true;
						notInDB = true;
						event = metaData[k].replaceFirst("INSTANCE-NOT-IN-DB#", "").replace("&", "&amp;");
					}
					
					if(!test) {
						if(isInstance == false || notInDB == true || noDay)
							continue;
					}
					
					String text = metaData[metaData.length-1];
					
					Data data = new Data(isInstance, notInDB, noDay, date, text, event);
					
					if(newLabelDistribution.containsKey(label))
						newLabelDistribution.put(label, newLabelDistribution.get(label)+1);
					else
						newLabelDistribution.put(label, 1);
					
					Map<Integer, Double> featureMap = new HashMap<Integer, Double>();
					featureMap.put(0, 1.0);
					for (int i = 1; i < features.length; i++) {
						int featureIndex = Integer.parseInt(features[i].substring(0, features[i].indexOf(':')));
						if(oldToNewFeatureID.containsKey(featureIndex)) {
							featureMap.put(oldToNewFeatureID.get(featureIndex), 1.0);
						}
					}
					
					DataPoint dataPoint = new DataPoint(new SparseFloatVector(featureMap, totalFeatures+1), label, data);
					dataPoints.add(dataPoint);
					
					dataPointsAdded++;
					
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		log += "\n" + dataPointsAdded + " / " + totalLines + "\t" + file + "\n";
		log += labelDistribution + "\n";
		log += newLabelDistribution + "\n";
		
		Vector[] INPUTS = new Vector[dataPoints.size()];
		int[] OUTPUTS = new int[dataPoints.size()];
		Data[] DATA = new Data[dataPoints.size()];
		for (int i = 0; i < dataPoints.size(); i++) {
			DataPoint dp = dataPoints.get(i);
			INPUTS[i] = dp.INPUT;
			OUTPUTS[i] = dp.OUTPUT;
			DATA[i] = dp.DATA;
		}
		
		return new CleanData(INPUTS, OUTPUTS, DATA, log);
	}
	
	public static LogisticRegression Train(String trainFile, HashMap<Integer, String> featureIdToStringMap, 
			HashMap<Integer, Integer> oldToNewFeatureID, ClassLabelStrategy classLabelStrategy, String logFilePrefix, double prior) throws IOException {
		
		HashMap<Integer, Integer> newToOldFeatureID = new HashMap<Integer, Integer>();
		for(Entry<Integer, Integer> e : oldToNewFeatureID.entrySet())
			newToOldFeatureID.put(e.getValue(), e.getKey());
		
		CleanData cleanData = GetCleanData(trainFile, featureIdToStringMap, oldToNewFeatureID, classLabelStrategy, false);
		
		Reporter reporter =  Reporters.file(new File(logFilePrefix + ".report"), "UTF-8");
		reporter.setLevel(LogLevel.DEBUG);
		
		System.out.println("\nTraining Started...");
		LogisticRegression regression
        	= LogisticRegression.estimate(cleanData.INPUTS,
        							  cleanData.OUTPUTS,
                                      RegressionPrior.laplace(prior, true),
                                      AnnealingSchedule.inverse(.05,100),
                                      reporter,        
                                      0.000000001, // min improve
                                      1, // min epochs
                                      10000); // max epochs
		
		StringBuffer sb = new StringBuffer();
		sb.append("No of datapoints " + cleanData.OUTPUTS.length + "\n");
		sb.append("Feature size reduced from " + featureIdToStringMap.size() + " to "+ oldToNewFeatureID.size() + "\n\n");
		
		sb.append(cleanData.LOG);
		
		Vector[] weightVectors = regression.weightVectors();
		
		for (int i = 0; i < weightVectors.length; i++) {
			HashMap<String, Double> featureWeightMap = new HashMap<String, Double>();
			Vector weightVector = weightVectors[i];
			sb.append("\nindex = " + i + "\n");
			sb.append("-------------------\n");
			
			for (int j = 1; j < weightVector.length(); j++)
				featureWeightMap.put(featureIdToStringMap.get(newToOldFeatureID.get(j)), weightVector.value(j));
			
			@SuppressWarnings("unchecked")
			HashMap<String, Double> sortedFeatureWeightMap = CommonFunctions.sortHashMapByValues(featureWeightMap);
			
			int count = 0;
			for (Entry<String, Double> e : sortedFeatureWeightMap.entrySet()) {
				if(count < 100)
					sb.append("P : " + e.getKey() + "\t" + e.getValue() + "\n");
				if(count == 100)
					sb.append("\n");
				if(sortedFeatureWeightMap.size() - count <= 100)
					sb.append("N : " + e.getKey() + "\t" + e.getValue() + "\n");
				count++;
			}
		}
		
		CommonFunctions.WriteToFile(logFilePrefix + ".log", sb.toString());
		
		return regression;
	}
	
	public static HashMap<String, String> GetInstanceToGTStartDate(HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents,
			HashSet<String> eventsWithSchedule) {
		
		HashMap<String, String> instanceToGTStartDate = new HashMap<String, String>();
		int instanceWithGt=0;
		metaEventsToEvents.size();
		
		for(Entry<String, ArrayList<FreebaseEvent>> e : metaEventsToEvents.entrySet()) {
			
			/*boolean trainMode = true;
			
			if(eventsWithSchedule.contains(e.getKey()))
				trainMode = false;
			else {
				if(metaEventsInTrainSoFar > (totalSize/2))
					trainMode = false;
			}
			
			if(trainMode == true) {
				metaEventsInTrainSoFar++;
			}
			*/
			
			ArrayList<FreebaseEvent> instances = e.getValue();
			for(FreebaseEvent instance :instances) {
				if(instance.startDateStr.length() >= 10) {
					instanceToGTStartDate.put(instance.name, instance.startDateStr);
					
				}
			}
		}
		
		System.out.println("instanceWithGt in Train : " + instanceWithGt);
		return instanceToGTStartDate;
	}
	
	@SuppressWarnings("unchecked")
	public static void WriteILPInput(HashMap<String, String> instanceToGTStartDate, HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents, 
			String metaEventsJson, HashSet<String> eventsWithSchedule, HashMap<String, ArrayList<Pair<String, Double>>> instanceToStartDates,
			HashMap<String,HashMap<Integer, Integer>> metaEventToinstancesNotInDBCountMap, HashMap<String, String> metaEventsToMid) {
		
		int noOfExtractions = 0;
		int metaEventsInTrainSoFar = 0;
		int totalSize = metaEventsToEvents.size();
		
		int numberOfInstancesWithStartDatesWithJustYear = 0;
		int numberOfInstancesWithNoStartDates = 0;
		
		JSONArray metaEventsArray = new JSONArray();
		int totalWithStartDate = 0; 
		int newCount = 0;
		for(Entry<String, ArrayList<FreebaseEvent>> e : metaEventsToEvents.entrySet()) {
			
			boolean trainMode = true;
			
			if(eventsWithSchedule.contains(e.getKey()))
				trainMode = false;
			else {
				if(metaEventsInTrainSoFar > (totalSize/2))
					trainMode = false;
			}
			
			if(trainMode == true) {
				metaEventsInTrainSoFar++;
			}
			
			String metaEvent = e.getKey();
			
			HashMap<Integer, Integer> instancesNotInDBCountMap = metaEventToinstancesNotInDBCountMap.get(e.getKey());
				
			JSONObject metaEventObj = new JSONObject();
        	metaEventObj.put("event-name", e.getKey());
        	
			ArrayList<FreebaseEvent> instances = e.getValue();
			
			String metaEventMid = "";
			for(FreebaseEvent f : instances) {
				if(f.recurrentEventMid != null && !f.recurrentEventMid.trim().equals(""))
					metaEventMid = f.recurrentEventMid;
			}
			
			if(metaEventMid.equals("")) {
				if(metaEventsToMid.containsKey(e.getKey()))
					metaEventMid = metaEventsToMid.get(e.getKey());
			}
			metaEventObj.put("mid", metaEventMid);
			
			JSONArray instancesArray = new JSONArray();
			int numberOfInstancesWithAtleastOneStartDate = 0;
			
			HashSet<String> alreadyAdded = new HashSet<String>();
			for(FreebaseEvent instance :instances) { 
				
				JSONObject instanceObj = new JSONObject();
        		instanceObj.put("name", instance.name);
        		
				if(instanceToGTStartDate.containsKey(instance.name))
					instanceObj.put("start-date", instance.startDateStr);
				else {
					if(instance.startDateStr.length()>=4) {
						instanceObj.put("instance-year", instance.startDateStr.substring(0, 4));
						numberOfInstancesWithStartDatesWithJustYear++;
					} else {
						numberOfInstancesWithNoStartDates++;
						continue;
					}
				}
				
				JSONArray instanceDatesArray = new JSONArray();
        		if(instanceToStartDates.containsKey(instance.name)) {
        			alreadyAdded.add(instance.name);
        			if(instanceToStartDates.get(instance.name).size() > 0) {
        				if(DEBUG)
        					System.out.println(instance.name);
                		
        				numberOfInstancesWithAtleastOneStartDate++;
        			}
        			
        			ArrayList<Pair<String, Double>> startDateEntries = instanceToStartDates.get(instance.name);
        			// Added to print statistics
        			if(trainMode && !instanceToGTStartDate.containsKey(instance.name) && startDateEntries.size() >0)
        				noOfExtractions++;
        			
        			for (int i = 0; i < startDateEntries.size(); i++) {
        				if(DEBUG)
        					System.out.println("\t" + startDateEntries.get(i).getFirst());
        				JSONObject startDateObject = new JSONObject();
        				startDateObject.put("date", "XXXX-" + startDateEntries.get(i).getFirst());
        				startDateObject.put("confidence", startDateEntries.get(i).getSecond());
        				instanceDatesArray.put(startDateObject);
					}
        			
        			if(DEBUG && startDateEntries.size()>0)
        				System.out.println("");
        		}
        		
        		instanceObj.put("extracted-start-dates", instanceDatesArray);
        		instancesArray.put(instanceObj);
			}
			
			HashSet<Integer> newInstancesAdded = new HashSet<Integer>();
			
			for(Entry<String, ArrayList<Pair<String, Double>>> instanceEntry : instanceToStartDates.entrySet()) {
				
				if(alreadyAdded.contains(instanceEntry.getKey()))
					continue;
				
				if(instanceEntry.getKey().endsWith(metaEvent) && metaEvent.length() + 5 == instanceEntry.getKey().length()) {
					
					int instanceYear = Integer.parseInt(instanceEntry.getKey().substring(0, 4));
					
					if(instancesNotInDBCountMap ==null || !instancesNotInDBCountMap.containsKey(instanceYear))
						continue;
					
					if(instancesNotInDBCountMap.get(instanceYear) < 4)
						continue;
					
					newInstancesAdded.add(instanceYear);
					
					JSONObject instanceObj = new JSONObject();
	        		instanceObj.put("name", instanceEntry.getKey());
	        		instanceObj.put("instance-year", instanceYear);
	        		
	        		JSONArray instanceDatesArray = new JSONArray();
	        		if(instanceToStartDates.get(instanceEntry.getKey()).size() > 0)
	    				numberOfInstancesWithAtleastOneStartDate++;
	        		ArrayList<Pair<String, Double>> startDateEntries = instanceToStartDates.get(instanceEntry.getKey());
        			for (int i = 0; i < startDateEntries.size(); i++) {
	    				JSONObject startDateObject = new JSONObject();
	    				startDateObject.put("date", "XXXX-" + startDateEntries.get(i).getFirst());
	    				startDateObject.put("confidence", startDateEntries.get(i).getSecond());
	    				instanceDatesArray.put(startDateObject);
					}
	    			
	    			instanceObj.put("extracted-start-dates", instanceDatesArray);
	        		instancesArray.put(instanceObj);
				}
			}
			
			if(instancesNotInDBCountMap != null) {
				for(Entry<Integer, Integer> notInDb : instancesNotInDBCountMap.entrySet()) {
					
					int instanceYear = notInDb.getKey();
					
					if(newInstancesAdded.contains(instanceYear))
						continue;
					
					if(notInDb.getValue() < 1) {
						continue;
					}
					
					if(instanceYear < 1900 || instanceYear > 2020) {
						continue;
					}
					
					newCount++;
					
					JSONObject instanceObj = new JSONObject();
	        		instanceObj.put("name", instanceYear + " " + metaEvent);
	        		instanceObj.put("instance-year", instanceYear);
	        		JSONArray instanceDatesArray = new JSONArray();
	        		instanceObj.put("extracted-start-dates", instanceDatesArray);
	        		instancesArray.put(instanceObj);
				}
			}
			
			metaEventObj.put("instances", instancesArray);
			if(metaEventToYiliDates.containsKey(metaEvent)) {
				JSONArray yiliDatesArray = new JSONArray();
				ArrayList<Pair<String, Double>> yiliDates = metaEventToYiliDates.get(metaEvent);
				for (int i = 0; i < yiliDates.size(); i++) {
					JSONObject startDateObject = new JSONObject();
    				startDateObject.put("date", "XXXX-" + yiliDates.get(i).getFirst());
    				startDateObject.put("confidence", yiliDates.get(i).getSecond());
    				yiliDatesArray.put(startDateObject);
				}
				//metaEventObj.put("ylil-dates", yiliDatesArray);
			}
			
			metaEventsArray.put(metaEventObj);
			
			if(numberOfInstancesWithAtleastOneStartDate > 1) {
				totalWithStartDate+=numberOfInstancesWithAtleastOneStartDate;
			}
		}
		
		System.out.println("Extractions : " + noOfExtractions);
		
		JSONObject finalJson = new JSONObject();
		finalJson.put("meta-events", metaEventsArray);
		
		CommonFunctions.WriteToFile(metaEventsJson, finalJson.toJSONString());
		System.out.println("MetaEvents with atleast one extraction : " + totalWithStartDate);
		System.out.println("Instances with only start year : " + numberOfInstancesWithStartDatesWithJustYear);
		System.out.println("Instances with No start year : " + numberOfInstancesWithNoStartDates);
		System.out.println("New Instances : " + newCount);
	}
	
	static HashMap<String, ArrayList<Pair<String, Double>>> metaEventToYiliDates = new HashMap<String, ArrayList<Pair<String, Double>>>();
	
	public static HashMap<String, ArrayList<Pair<String, Double>>> Test(LogisticRegression regression, String testFile, HashMap<Integer, String> featureIdToStringMap, 
			HashMap<Integer, Integer> oldToNewFeatureID, ClassLabelStrategy classLabelStrategy, String resultFilePath, 
			HashMap<String, String> instanceToGTStartDate) {
		
		HashMap<String, ArrayList<Pair<String, Double>>> instanceToStartDates = new HashMap<String, ArrayList<Pair<String, Double>>>();
		
		CleanData cleanData = GetCleanData(testFile, featureIdToStringMap, oldToNewFeatureID, classLabelStrategy, true);
		
		HashMap<Integer, HashMap<Integer, Integer>> confusionMatrix = null;
		HashMap<Integer, Integer> gtLabelDistribution = null;
		HashMap<Integer, Integer> predictedLabelDistribution = null;
		
		metaEventToYiliDates = new HashMap<String, ArrayList<Pair<String, Double>>>();
		
		StringBuffer sb = new StringBuffer();
		sb.append(cleanData.LOG);
		
		for (int i = 0; i < cleanData.INPUTS.length; i++) {
			
			double[] conditionalProbs = regression.classify(cleanData.INPUTS[i]);
	        
	        if(confusionMatrix == null) {
	        	confusionMatrix = new HashMap<Integer, HashMap<Integer, Integer>>();
	        	predictedLabelDistribution = new HashMap<Integer, Integer>();
	        	gtLabelDistribution = new HashMap<Integer, Integer>();
	        	for (int j = 0; j < conditionalProbs.length; j++) {
	        		HashMap<Integer, Integer> value = new HashMap<Integer, Integer>();
	        		for (int j2 = 0; j2 < conditionalProbs.length; j2++)
	        			value.put(j2, 0);
	        		confusionMatrix.put(j, value);
	        		gtLabelDistribution.put(j, 0);
	        		predictedLabelDistribution.put(j, 0);
				}
	        }
	        
	        int prediction = -1;
	        double maxProb = 0;
	        for (int k = 0; k < conditionalProbs.length; ++k) {
	        	if(conditionalProbs[k] > maxProb) {
	        		prediction = k;
	        		maxProb = conditionalProbs[k];
	        	}
	        }
	        
	        if(prediction == 0 && maxProb < 0.6)
	        	prediction = 1;
	        
	        int gt = cleanData.OUTPUTS[i];
	        if(gt != -1) {
	        	predictedLabelDistribution.put(prediction, predictedLabelDistribution.get(prediction)+1);
	        	gtLabelDistribution.put(gt, gtLabelDistribution.get(gt)+1);
	        	confusionMatrix.get(gt).put(prediction, confusionMatrix.get(gt).get(prediction)+1);
	        }
	        
	        if(cleanData.DATA[i].isInstance == true || cleanData.DATA[i].isNotInDB == true) {
	        	
	        	String event = cleanData.DATA[i].event;
	        	String date = cleanData.DATA[i].date;
	        	
	        	//if(cleanData.DATA[i].event.contains("independence bowl"))
	        	//	System.out.println(prediction + "\t" + event + "\t" + date);
	        	
	        	if(prediction==1 && date.length() >= 10) {
					String key = date.substring(5);
					double value = maxProb;
					if(instanceToStartDates.containsKey(event)) {
						ArrayList<Pair<String, Double>> startDates = instanceToStartDates.get(event);
						startDates.add(new Pair<String, Double>(key, value));
						instanceToStartDates.put(event, startDates);
					} else {
						
						ArrayList<Pair<String, Double>> startDates = new ArrayList<Pair<String, Double>>();
						startDates.add(new Pair<String, Double>(key, value));	
						instanceToStartDates.put(event, startDates);
					}
				}
	        } else {
	        	
	        	String metaEvent = cleanData.DATA[i].event;
	        	String date = cleanData.DATA[i].date;
	        	String key = date.substring(5);
	        	
	        	if(metaEventToYiliDates.containsKey(metaEvent)) {
					ArrayList<Pair<String, Double>> yiliDates = metaEventToYiliDates.get(metaEvent);
					yiliDates.add(new Pair<String, Double>(key, maxProb));
					metaEventToYiliDates.put(metaEvent, yiliDates);
				} else {
					
					ArrayList<Pair<String, Double>> yiliDates = new ArrayList<Pair<String, Double>>();
					yiliDates.add(new Pair<String, Double>(key, maxProb));	
					metaEventToYiliDates.put(metaEvent, yiliDates);
				}
	        	
	        	/*if(prediction==1) {
	        		int day = -1;
	        		if(date.length() >= 10)
	        			day = Integer.parseInt(date.substring(8, 10));
	        		
	        		String onlyM = "m" + date.substring(5, 7);
	        		scheduleDist = AddToMap(scheduleDist, metaEvent, onlyM, 0.033);
	        		
	        		if(date.length() >= 10){
		        		String monthMod = "m" + date.substring(5, 7);
		        		if(day <= 10)
		        			monthMod += "_early";
		        		else if(day <= 20)
		        			monthMod += "_mid";
		        		else
		        			monthMod += "_late";
		        		scheduleDist = AddToMap(scheduleDist, metaEvent, monthMod, 0.1);
		        		
		        		String weekOfM = "_week_of_m" + date.substring(5, 7);
		        		if(day <= 7)
		        			weekOfM = "1" + weekOfM;
		        		else if(day <= 14)
		        			weekOfM = "2" + weekOfM;
		        		else if(day <= 21)
		        			weekOfM = "3" + weekOfM;
		        		else if(day <= 28)
		        			weekOfM = "4" + weekOfM;
		        		else
		        			weekOfM = null;
		        		
		        		if(weekOfM != null)
		        			scheduleDist = AddToMap(scheduleDist, metaEvent, weekOfM, 0.142);
		        		
		        		if(day > 25)
		        			scheduleDist = AddToMap(scheduleDist, metaEvent, "-1_week_of_m" + date.substring(5, 7), 0.142);
	        		}
	        	}*/
	        }
	    }
		
		sb.append("GT Label Distribution : " + gtLabelDistribution+"\n");
		sb.append("Confusion Matrix : " + confusionMatrix + "\n");
		
		for(Entry<Integer, HashMap<Integer, Integer>> e : confusionMatrix.entrySet()) {
			sb.append("\n" + e.getKey() + "\n");
			sb.append("------------" + "\n");
			
			int tp = e.getValue().get(e.getKey());
			double precision = ((double) tp)/predictedLabelDistribution.get(e.getKey());
			double recall = ((double) tp)/gtLabelDistribution.get(e.getKey());
			double fscore = (2*precision*recall)/(precision+recall);
			
			sb.append("Precision : " + precision + "\n");
			sb.append("Recall    : " + recall + "\n");
			sb.append("F-score   : " + fscore + "\n");
		}
		
		CommonFunctions.WriteToFile(resultFilePath, sb.toString());
		
		return instanceToStartDates;
	}
	
	public static HashMap<String, HashMap<String, Double>> AddToMap(HashMap<String, HashMap<String, Double>> scheduleDist, 
			String event, String schedule, double score) {
		
		if(!scheduleDist.containsKey(event))
			scheduleDist.put(event, new HashMap<String, Double>());
		
		HashMap<String, Double> scheduleMapEntry = scheduleDist.get(event);
		if(scheduleMapEntry.containsKey(schedule)) 
			scheduleMapEntry.put(schedule, scheduleMapEntry.get(schedule)+score);
		else
			scheduleMapEntry.put(schedule, score);
		
		scheduleDist.put(event, scheduleMapEntry);
		
		return scheduleDist;
	}
	
	public static HashMap<Integer, Integer> FeatureSelection(String file, 
			HashMap<Integer, String> featureIdToStringMap, FeaturesUsed featuresUsed) {
		
		BufferedReader br = null;
		
		HashMap<Integer, Integer> oldToNewFeatureID = new HashMap<Integer, Integer>();
		
		int minCount = 0;
		if(featuresUsed == FeaturesUsed.ALL_5)
			minCount = 5;
		if(featuresUsed == FeaturesUsed.ALL_3 
				|| featuresUsed == FeaturesUsed.ALL_3_NO_SU
				|| featuresUsed == FeaturesUsed.ALL_3_NO_SU_NO_UNI)
			minCount = 3;
		
		try {

			String sCurrentLine = null;
			br = new BufferedReader(new FileReader(file));
			
			HashMap<Integer, Integer> featureCountMap = new HashMap<Integer, Integer>();
			
			while ((sCurrentLine = br.readLine()) != null) {
				
				int indexOfHash = sCurrentLine.indexOf('#');
				String[] features = sCurrentLine.substring(0, indexOfHash).split(" ");
				
				for (int i = 1; i < features.length; i++) {
					int featureIndex = Integer.parseInt(features[i].substring(0, features[i].indexOf(':')));
					if(featureCountMap.containsKey(featureIndex))
						featureCountMap.put(featureIndex, featureCountMap.get(featureIndex)+1);
					else
						featureCountMap.put(featureIndex, 1);
				}
				
			}
			
			for(Entry<Integer, Integer> e : featureCountMap.entrySet()) {
				
				String featureString = featureIdToStringMap.get(e.getKey());
				if((featuresUsed == FeaturesUsed.ALL_3_NO_SU || featuresUsed == FeaturesUsed.ALL_3_NO_SU_NO_UNI) 
						&& featureString.startsWith("SU-"))
					continue;
				
				if(featuresUsed == FeaturesUsed.ALL_3_NO_SU_NO_UNI 
						&& (featureString.startsWith("1AU-") || featureString.startsWith("1BU-") ||
								featureString.startsWith("2AU-") || featureString.startsWith("2BU-") || 
								featureString.startsWith("3AU-") || featureString.startsWith("3BU-")))
					continue;
				
				if(e.getValue() >= minCount) {
					int newIndex = oldToNewFeatureID.size()+1;
					oldToNewFeatureID.put(e.getKey(), newIndex);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return oldToNewFeatureID;
	}
	
	public static HashMap<Integer, String> GetFeatureIdToStringMap(String featureFile) {
		
		HashMap<Integer, String> featureIdToStringMap = new HashMap<Integer, String>();
		
		BufferedReader br = null;

		try {

			String sCurrentLine = null;
			br = new BufferedReader(new FileReader(featureFile));
			
			while ((sCurrentLine = br.readLine()) != null) {
				
				String[] strSplit = sCurrentLine.split("\t");
				
				int id = Integer.parseInt(strSplit[0]);
				String string = strSplit[1];
				featureIdToStringMap.put(id, string);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return featureIdToStringMap;
	}
	
	public static HashMap<String,HashMap<Integer, Integer>> GetNewInstancesDetails(String jsonFile) {
		
		HashMap<String,HashMap<Integer, Integer>> metaEventToinstancesNotInDBCountMap = new HashMap<String,HashMap<Integer, Integer>>();
		
		if(jsonFile == null)
			return metaEventToinstancesNotInDBCountMap;
		
		org.json.simple.JSONObject baseObject = CommonFunctions.readJsonFromFile(jsonFile);
		org.json.simple.JSONArray metaEvents = (org.json.simple.JSONArray) baseObject.get("meta-events");
		for (int i = 0; i < metaEvents.size(); i++) {
			org.json.simple.JSONObject me = (org.json.simple.JSONObject) metaEvents.get(i);
			String key = (String) me.get("meta-event");
			
			HashMap<Integer, Integer> instancesNotInDBCountMap = new HashMap<Integer, Integer>();
			
			org.json.simple.JSONArray notInDBInstances = (org.json.simple.JSONArray) me.get("not-in-db");
			for (int j = 0; j < notInDBInstances.size(); j++) {
				Long lNotInDBKey = (Long) ((org.json.simple.JSONObject)notInDBInstances.get(j)).get("year");
				Long lCount = (Long) ((org.json.simple.JSONObject)notInDBInstances.get(j)).get("count");
				instancesNotInDBCountMap.put(new Integer(lNotInDBKey.intValue()), new Integer(lCount.intValue()));
			}
			metaEventToinstancesNotInDBCountMap.put(key, instancesNotInDBCountMap);
		}
		
		return metaEventToinstancesNotInDBCountMap;
	}
}
