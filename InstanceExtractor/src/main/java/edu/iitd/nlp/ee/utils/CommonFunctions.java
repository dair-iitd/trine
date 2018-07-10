package edu.iitd.nlp.ee.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CommonFunctions {
	
	static JSONParser parser;
	
	public static JSONObject readJsonFromFile(String jsonFilePath) {
		
		if(parser == null)
			parser = new JSONParser();
		
		JSONObject json = null;
		try {
			json = (JSONObject) parser.parse(new FileReader(jsonFilePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return json;
	}
	
	public static JSONArray readJsonArrayFromFile(String jsonFilePath) {
		
		if(parser == null)
			parser = new JSONParser();
		
		JSONArray json = null;
		try {
			json = (JSONArray) parser.parse(new FileReader(jsonFilePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return json;
	}
	
	public static Object ReadObject(String FileName) {
		
		FileInputStream fis = null;
		ObjectInputStream in = null;

		Object o = new Object();
		
		try {
			fis = new FileInputStream(FileName);
			in = new ObjectInputStream(fis);
			o = in.readObject();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
			

		return o;
	}
	
	public static void WriteObject(String FileName, Object obj) 
	{
		FileOutputStream fos;
		ObjectOutputStream out;
		try {
			fos = new FileOutputStream(FileName);
			out = new ObjectOutputStream(fos);

			out.writeObject(obj);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public static void WriteToFile(String filePath, String content) {
		
		WriteToFile(filePath, content, false);
	}
	
	public static void WriteToFile(String filePath, String content, boolean append) {
		try {

			File file = new File(filePath);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile(),append);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void AppendToFile(String filePath, String content) {
		
		WriteToFile(filePath, content, true);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map SortByComparator(Map unsortMap) {
		 
		List list = new LinkedList(unsortMap.entrySet());
 
		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue())
                                       .compareTo(((Map.Entry) (o1)).getValue());
			}
		});
 
		// put sorted list into map again
                //LinkedHashMap make sure order in which keys were inserted
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static LinkedHashMap sortHashMapByValues(
	        HashMap passedMap) {
	    List mapKeys = new ArrayList(passedMap.keySet());
	    List mapValues = new ArrayList(passedMap.values());
	    Collections.sort(mapValues);
	    Collections.sort(mapKeys);
	    Collections.reverse(mapValues);
	    Collections.reverse(mapKeys);

	    LinkedHashMap sortedMap =
	        new LinkedHashMap();

	    Iterator valueIt = mapValues.iterator();
	    while (valueIt.hasNext()) {
	        Object val = valueIt.next();
	        Iterator keyIt = mapKeys.iterator();

	        while (keyIt.hasNext()) {
	            Object key = keyIt.next();
	            Object comp1 = passedMap.get(key);
	            Object comp2 = val;

	            if (comp1.equals(comp2)) {
	                keyIt.remove();
	                sortedMap.put(key, val);
	                break;
	            }
	        }
	    }
	    return sortedMap;
	}

	
	public static List<File> GetFileListing(
		    File aStartingDir
		  ) throws FileNotFoundException {
		    validateDirectory(aStartingDir);
		    List<File> result = getFileListingNoSort(aStartingDir);
		    Collections.sort(result);
		    return result;
		  }

	public static List<File> getFileListingNoSort(File aStartingDir) throws FileNotFoundException 
	{
		  List<File> result = new ArrayList<File>();
		    File[] filesAndDirs = aStartingDir.listFiles();
		    List<File> filesDirs = Arrays.asList(filesAndDirs);
		    for(File file : filesDirs) {
		      if ( ! file.isFile() ) {
		        //must be a directory
		        //recursive call!
		        List<File> deeperList = getFileListingNoSort(file);
		        result.addAll(deeperList);
		      }
		      else
		    	  result.add(file);
		    }
		    return result;
	}
	
	private static void validateDirectory (File aDirectory) throws FileNotFoundException 
	{
		  if (aDirectory == null) {
		      throw new IllegalArgumentException("Directory should not be null.");
		    }
		    if (!aDirectory.exists()) {
		      throw new FileNotFoundException("Directory does not exist: " + aDirectory);
		    }
		    if (!aDirectory.isDirectory()) {
		      throw new IllegalArgumentException("Is not a directory: " + aDirectory);
		    }
		    if (!aDirectory.canRead()) {
		      throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
		    }
	}
	
	public static boolean FileExists(String filePath) {
		
		File file = new File(filePath);
		
		if (file.exists())
			return true;
		else
			return false;
	}
	
	public static void uncompressTarGZ(File tarFile, File dest) throws IOException {
	    
		if(!dest.exists())
			dest.mkdir();
		
	    TarArchiveInputStream tarIn = new TarArchiveInputStream(
	                new GzipCompressorInputStream(
	                    new BufferedInputStream(
	                        new FileInputStream(
	                            tarFile
	                        )
	                    )
	                )
	            );

	    TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
	    // tarIn is a TarArchiveInputStream
	    while (tarEntry != null) {// create a file with the same name as the tarEntry
	        File destPath = new File(dest, tarEntry.getName());
	        //System.out.println("working: " + destPath.getCanonicalPath());
	        if (tarEntry.isDirectory()) {
	            destPath.mkdirs();
	        } else {
	            destPath.createNewFile();
	            //byte [] btoRead = new byte[(int)tarEntry.getSize()];
	            byte [] btoRead = new byte[1024];
	            //FileInputStream fin 
	            //  = new FileInputStream(destPath.getCanonicalPath());
	            BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(destPath));
	            int len = 0;

	            while((len = tarIn.read(btoRead)) != -1)
	            {
	                bout.write(btoRead,0,len);
	            }

	            bout.close();
	            btoRead = null;

	        }
	        tarEntry = tarIn.getNextTarEntry();
	    }
	    tarIn.close();
	}
	
	public static void SetProxy() {
		
		System.out.println("IITD Proxy Set");
		System.setProperty("https.proxyHost", "10.10.78.61");
        System.setProperty("https.proxyPort", "3128");
        System.setProperty("http.proxyHost", "10.10.78.61");
        System.setProperty("http.proxyPort", "3128");
        
	}
}
