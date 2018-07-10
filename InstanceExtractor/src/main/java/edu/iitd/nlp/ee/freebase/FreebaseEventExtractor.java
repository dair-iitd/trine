package edu.iitd.nlp.ee.freebase;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.iitd.nlp.ee.utils.CommonFunctions;

public class FreebaseEventExtractor {

	public void GetEventDetails() {

		String query = "[{"
							+ "\"mid\":null,"
							+ "\"name\":null,"
							+ "\"type\":\"/time/event\","
							+ "\"/common/topic/alias\":[],"
							+ "\"/time/event/start_date\":null,"
							+ "\"/time/event/end_date\":null,"
							+ "\"/time/event/locations\":[{\"mid\":null,\"name\":null,\"optional\":true}],"
							+ "\"/time/event/included_in_event\":[{\"mid\":null,\"name\":null,\"optional\":true}],"
							+ "\"/time/event/includes_event\":[{\"mid\":null,\"name\":null,\"optional\":true}], "
							+ "\"/time/event/instance_of_recurring_event\":{\"mid\":null,\"name\":null,\"optional\":true},"
							+ "\"/wikipedia/topic/en_id\":[]"
						+ "}]";

		boolean cursor = true;
		String cursorValue = null;

		StringBuffer sb = new StringBuffer();
		sb.append("{\"result\": [");

		while (cursor) {

			JSONObject result = FreebaseExtractor.QueryFreebase(query, true, cursorValue);
			if (result == null)
				break;

			JSONArray resultList = (JSONArray) result.get("result");

			if (result.get("cursor") == null
					|| result.get("cursor").toString().equalsIgnoreCase("false"))
				cursor = false;
			else
				cursorValue = result.get("cursor").toString();
			
			for (int i = 0; i < resultList.size(); i++) {

				try {

					JSONObject resultJson = (JSONObject) resultList.get(i);

					String startDate = (String) resultJson.get("/time/event/start_date");
					if (startDate == null || startDate.length() < 4)
						continue;

					String yearOfStartDateStr = startDate.substring(0, 4);
					try {
						int year = Integer.parseInt(yearOfStartDateStr);
						if (year < 1900) {
							continue;
						}
					} catch (Exception e) {
						continue;
					}

					String eventName = (String) resultJson.get("name");
					System.out.println(eventName);
					sb.append(resultJson.toString() + ",");

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		sb.delete(sb.length()-1, sb.length());
		sb.append("]}");
		
		
		CommonFunctions.WriteToFile("data//events-details.json", sb.toString());
	}
	
	public void GetEventAliases() {

		String query = "[{" 
							+ "\"mid\": null," 
							+ "\"name\": null,"
							+ "\"type\": \"/time/event\"," 
							+ "\"/common/topic/alias\": [],"
							+ "\"/time/event/start_date\": null," 
							+ "\"ns0:type\": []" +
						"}]";

		boolean cursor = true;
		String cursorValue = null;

		StringBuffer sb = new StringBuffer();

		while (cursor) {

			JSONObject result = FreebaseExtractor.QueryFreebase(query, true, cursorValue);
			if (result == null)
				break;

			JSONArray resultList = (JSONArray) result.get("result");

			if (result.get("cursor") == null
					|| result.get("cursor").toString().equalsIgnoreCase("false"))
				cursor = false;
			else
				cursorValue = result.get("cursor").toString();

			for (int i = 0; i < resultList.size(); i++) {

				try {

					JSONObject resultJson = (JSONObject) resultList.get(i);

					String startDate = (String) resultJson.get("/time/event/start_date");
					if (startDate == null || startDate.length() < 4)
						continue;

					String yearOfStartDateStr = startDate.substring(0, 4);
					try {
						int year = Integer.parseInt(yearOfStartDateStr);
						if (year < 1900) {
							continue;
						}
					} catch (Exception e) {
						continue;
					}

					String eventName = (String) resultJson.get("name");
					String eventMid = (String) resultJson.get("mid");

					System.out.println(eventName);

					JSONArray aliasArray = (JSONArray) resultJson
							.get("/common/topic/alias");
					String aliases = eventName;

					if (aliasArray != null) {
						for (int k = 0; k < aliasArray.size(); k++) {
							aliases += "@#@" + (String) aliasArray.get(k);
						}
					}

					JSONArray notableTypes = (JSONArray) resultJson
							.get("ns0:type");
					String types = "";

					if (notableTypes != null) {
						for (int k = 0; k < notableTypes.size(); k++) {
							if (k == 0)
								types += (String) notableTypes.get(k);
							else
								types += "@#@" + (String) notableTypes.get(k);
						}
					}

					sb.append(eventName + "\t" + eventMid + "\t" + aliases
							+ "\t" + types + "\n");

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		CommonFunctions.WriteToFile("data//events-aliases.txt", sb.toString());
	}
	
	public void GetEventWikiIds() {

		String query = "[{" + "\"mid\":null," 
							+ "\"name\":null,"
							+ "\"type\":\"/time/event\"," 
							+ "\"/time/event/start_date\":null," 
							+ "\"/wikipedia/topic/en_id\":[]"
						+ "}]";

		boolean cursor = true;
		String cursorValue = null;

		StringBuffer sb = new StringBuffer();

		while (cursor) {

			JSONObject result = FreebaseExtractor.QueryFreebase(query, true, cursorValue);
			if (result == null)
				break;

			JSONArray resultList = (JSONArray) result.get("result");

			if (result.get("cursor") == null
					|| result.get("cursor").toString().equalsIgnoreCase("false"))
				cursor = false;
			else
				cursorValue = result.get("cursor").toString();

			for (int i = 0; i < resultList.size(); i++) {

				try {

					JSONObject resultJson = (JSONObject) resultList.get(i);

					String startDate = (String) resultJson.get("/time/event/start_date");
					if (startDate == null || startDate.length() < 4)
						continue;

					String yearOfStartDateStr = startDate.substring(0, 4);
					try {
						int year = Integer.parseInt(yearOfStartDateStr);
						if (year < 1900) {
							continue;
						}
					} catch (Exception e) {
						continue;
					}

					String eventName = (String) resultJson.get("name");
					
					JSONArray eventWikiIdArray = (JSONArray)resultJson.get("/wikipedia/topic/en_id"); 
					String eventWikiId = ""; 
					if (eventWikiIdArray.size() > 0) {
						System.out.println(eventName);
						eventWikiId = (String)eventWikiIdArray.get(0);
						sb.append(eventWikiId + "\n");
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		CommonFunctions.WriteToFile("data//pageids.txt", sb.toString());
	}
		
	public static void main(String[] args) {
		
		CommonFunctions.SetProxy();
		
		FreebaseEventExtractor freebaseEventExtractor = new FreebaseEventExtractor();
		
		freebaseEventExtractor.GetEventWikiIds();
		
		// eventAliasFile
		freebaseEventExtractor.GetEventAliases();
		
		//eventsDetailsJson
		freebaseEventExtractor.GetEventDetails();
	}
}
