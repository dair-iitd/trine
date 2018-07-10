package edu.iitd.nlp.ee.corpus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;

import edu.iitd.nlp.ee.freebase.FreebaseEvent;
import edu.iitd.nlp.ee.stringsearch.ACAnnotation;
import edu.iitd.nlp.ee.stringsearch.ACTrie;
import edu.iitd.nlp.ee.utils.CommonFunctions;
import edu.iitd.nlp.ee.utils.WordnetWrapper;
import edu.iitd.nlp.ee.utils.Global;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.AnnotationSerializer;
import edu.stanford.nlp.pipeline.CustomAnnotationSerializer;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.TimeAnnotator;

public class ParallelNYTIndexBuilderForMetaEvents {

	 private static AtomicInteger atomicCount = new AtomicInteger(0);
	
	 private static AtomicInteger at;
	 
	 private static List<File> tgzFiles;
	 
	 private static String tmpFolder;
	 
	 private static int totalDocs;
	 
	 private static int nextCommitDeadline;
	 
	 private static int nextCommitDeadlineIncrement = 5000;
	 
	 private static AtomicInteger indexWriterCloseCriteria;
	 
	 // private static long startTime;
	 
	 public static void main(String[] args) throws IOException, InterruptedException {
	    
		//startTime = System.currentTimeMillis();
    	// ////////////////////////////////
		// Read props
		// /////////////////////////////////
		Global.props = edu.stanford.nlp.util.StringUtils.argsToProperties(args);
		System.err.println("Running with the following properties...");
		for (Object key : Global.props.keySet()) {
			System.err.println(key + "\t" + Global.props.get(key));
		}
		System.err.println("-----------------------------------------");
		
    	String lucenePath  = Global.props.getProperty("lucenePath");
    	File nytFolder = new File(Global.props.getProperty("nytFolder"));
    	tmpFolder = Global.props.getProperty("tmpFolder");

		String wordnetDir = Global.props.getProperty("wordnetDir");
		WordnetWrapper wordnetWrapper = new WordnetWrapper(wordnetDir);

		String eventAliasFile = Global.props.getProperty("eventAliasFile");
		String eventDetailsJson = Global.props.getProperty("freebaseEventsJson");
		String allMetaEventsCsvFile = Global.props.getProperty("allMetaEventsCsvFile");
		
		String threadCount = Global.props.getProperty("threads");
		int iThreadCount = Integer.parseInt(threadCount);
		indexWriterCloseCriteria = new AtomicInteger(-1*iThreadCount);
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		Directory index = new SimpleFSDirectory(new File(lucenePath));
		IndexWriter indexWriter = new IndexWriter(index, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
		
		List<File> files = CommonFunctions.getFileListingNoSort(nytFolder); 
    	
		tgzFiles = new ArrayList<File>();
    	for(File f : files) {
    		if(f.getName().endsWith(".tgz")) {
    			tgzFiles.add(f);
    		}
    	}
    	
    	nextCommitDeadline = nextCommitDeadlineIncrement;
    	
    	at = new AtomicInteger(tgzFiles.size());
    	totalDocs = tgzFiles.size();
    	
    	Properties pipelineProps = new Properties();
        
        pipelineProps.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
        pipelineProps.setProperty("parse.maxlen", "80");
        pipelineProps.setProperty("pos.maxlen", "80");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(pipelineProps);
        pipeline.addAnnotator(new TimeAnnotator(true));
    	
    	
		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < iThreadCount; i++) {

			HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents = FreebaseEvent.GetRecurrentEventsFromJson(eventDetailsJson, eventAliasFile, allMetaEventsCsvFile, true);
			ACTrie acTrie = FreebaseEvent.BuildACTrieForRecurrentEvent(metaEventsToEvents, wordnetWrapper);
			RunnableIndexer r = new RunnableIndexer( "thread_" + i, acTrie, pipeline, indexWriter);
		    threads.add(r);
		    r.start();   	
		}
		    
		for(Thread thread : threads) {
			thread.join();
		}
		
		System.out.println("Exiting Main...");
	 }
	 
	 public static class RunnableIndexer extends Thread {

		  private Thread t;
		  String threadName;
		  ACTrie acTrie;
		  IndexWriter indexWriter;
		  AnnotationPipeline pipeline;
		  
		  public RunnableIndexer(String threadName, ACTrie acTrie, AnnotationPipeline pipeline, IndexWriter indexWriter) {
			  this.threadName = threadName;
			  this.indexWriter = indexWriter;
			  this.acTrie = acTrie;
			  this.pipeline = pipeline;
		  }
		  
		  public void run() {
			  
			  System.out.println("Starting " +  threadName );
		      
			  NYTCorpusDocumentParser parser = new NYTCorpusDocumentParser();
			  
			  int indexToProcess = at.decrementAndGet();
			  File localTempFolder = new File(tmpFolder + threadName);
			  
			  AnnotationSerializer annSer = new CustomAnnotationSerializer(false, false);
					  
			  while(indexToProcess >= 0) {
	    		  
				  File file = tgzFiles.get(indexToProcess);
				  
				  try {
					  
		    		  FileUtils.deleteDirectory(localTempFolder);
		    		  CommonFunctions.uncompressTarGZ(file, localTempFolder);
		    		  
		    		  System.out.println( threadName + " processing " + (indexToProcess+1) + "/" + totalDocs + "\t" + file.getName());
		    		  
		    		  List<File> xmlfiles = CommonFunctions.getFileListingNoSort(localTempFolder); 
		    		  int xmlCount = 0;
			  			for(File xmlfile : xmlfiles) {
			  				
			  				xmlCount++;
			  				if(xmlCount %100 == 0)
			  					System.out.println((indexToProcess+1) + " : " + xmlCount + "/" + xmlfiles.size());
			  				
			  				if(!xmlfile.getName().endsWith(".xml"))
			  					continue;
			  				
			  				NYTCorpusDocument nytDoc = parser.parseNYTCorpusDocumentFromFile(xmlfile, false);
			  				if(nytDoc.getBody() == null)
			  					continue;
			  				
			  				String refDate = nytDoc.getPublicationYear()+ "-" 
									+ nytDoc.getPublicationMonth() + "-"
									+ nytDoc.getPublicationDayOfMonth() ;
			  				
			  				String[] paragraphs = nytDoc.getBody().split("\\n");
			  				
			  				for (int i=0; i< paragraphs.length; i++) {
			  					
			  					String paragraph = paragraphs[i].replaceAll("[^\\x20-\\x7E]+", " ");
			  					paragraph = paragraph.replaceAll("[\\s]+", " ");
			  					
			  					int words = StringUtils.countMatches( paragraph, " " );
			  					if(words >= 15) {
			  						
			  						ArrayList<ACAnnotation> annotations = acTrie.ProcessInput(paragraph.toLowerCase());
	    		            		
	    							if (annotations.size() > 0) {
	    								
	    								atomicCount.incrementAndGet();
	    								
	    								Annotation annDoc = new Annotation(paragraph);
	    								annDoc.set(CoreAnnotations.DocDateAnnotation.class, refDate);
		    			    	  	    pipeline.annotate(annDoc);
		    			    	  	    
		    			    	  	    ByteArrayOutputStream os = new ByteArrayOutputStream();
		    			    	  	    annSer.write(annDoc, os);
		    			    	  	    os.close();
		    			    	  	    
		    			    	  	    String annDocString = new String(os.toByteArray(),"UTF-8");
	    								
		    			    	  	    String eventsStr = "";
		    			    	  	    HashSet<String> eventsAdded = new HashSet<String>();
		    			    	  	    for(ACAnnotation ac : annotations) {
		    			    	  	    	String annStr = ac.getMid().replaceAll("[ ]+", "_");
		    			    	  	    	if(eventsAdded.contains(annStr))
		    			    	  	    		continue;
		    			    	  	    	eventsStr += annStr + " ";
		    			    	  	    	eventsAdded.add(annStr);
		    			    	  	    }
		    			    	  	    
		    			    	  	    eventsStr = eventsStr.trim();
	    								
	    								Document doc = new Document();
	    								String fullPath =  xmlfile.getAbsolutePath();
	    								doc.add(new Field("filename", fullPath, Store.YES, Index.NO));
	    								doc.add(new Field("docid", fullPath + "_" + i, Store.YES, Index.NO));
	    								doc.add(new Field("text", paragraph, Store.YES, Index.NO));
	    								doc.add(new Field("events", eventsStr, Store.YES, Index.ANALYZED));
	    								doc.add(new Field("annDoc", annDocString, Store.YES, Index.NO));
	    								
	    								indexWriter.addDocument(doc);
	    								
	    							}
			  					}
			  				}
			  			}
			  			
				  } catch (OutOfMemoryError e) {
	    	        	
	    	        	e.printStackTrace();
	    	        	System.out.println(threadName + " died due to file " + file.getAbsolutePath() + " :(");
	    	        	indexWriterCloseCriteria.decrementAndGet();
	    	        	return;
	    	        	
	    	        } catch (StackOverflowError e) {
	    	        	
	    	        	e.printStackTrace();
	    	        	System.out.println(threadName + " died due to file " + file.getAbsolutePath() + " :(");
	    	        	indexWriterCloseCriteria.decrementAndGet();
	    	        	return;
	    	        	
	    	        } catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
	    	        	
	    	        	if(atomicCount.get() > nextCommitDeadline) {
	    	        		System.out.println(threadName + " commiting the IndexWriter...");
							try {
								indexWriter.commit();
							} catch (CorruptIndexException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
							nextCommitDeadline += nextCommitDeadlineIncrement;
	    	        	}
	    	        	indexToProcess = at.decrementAndGet(); 
		    	    }
	    	  }
	    	  
	    	  if(indexToProcess == indexWriterCloseCriteria.get()) {
				try {
					indexWriter.close();
					System.out.println("Total Paragraphs extracted : " + atomicCount.get());
					System.out.println("Index Writer Closed by " + threadName + " ("+ indexToProcess + ")");
				} catch (CorruptIndexException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	  } else {
	    		  System.out.println("Index Writer left open by " + threadName + " ("+ indexToProcess + ")");
	    	  }
		  }
		  
		  public void start() {
			  if (t == null)
		      {
		         t = new Thread (this, threadName);
		         t.start ();
		      }
		  }
	 }
}
