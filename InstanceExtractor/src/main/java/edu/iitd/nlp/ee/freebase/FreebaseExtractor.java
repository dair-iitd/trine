package edu.iitd.nlp.ee.freebase;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

public class FreebaseExtractor {
	
	static String FREEBASE_KEY = "Enter your FB key here";

	static String freeBaseURL = "https://www.freebase.com";
	
	public static JSONObject QueryFreebase(String query, boolean cursor, String cursorValue) {
		
		JSONObject result = null;
		try {
			HttpTransport httpTransport = new NetHttpTransport();
	         HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
	         JSONParser parser = new JSONParser();
	         GenericUrl url = new GenericUrl("https://www.googleapis.com/freebase/v1/mqlread");
	         
	         url.put("lang", "/lang/en");
	         if(cursor) {
			     if(cursorValue == null)
			    	 url.put("cursor", "");
			     else
			    	 url.put("cursor", cursorValue);
		     }
	         url.put("query", query);
		     url.put("key", FREEBASE_KEY);
		     
		     HttpRequest request = requestFactory.buildGetRequest(url);
	         HttpResponse httpResponse = request.execute();
	         
	         result = (JSONObject)parser.parse(httpResponse.parseAsString());
	         
	    } catch (Exception ex) {
	    	ex.printStackTrace();
	    	return result;
	    }
		return result;
	}
}
