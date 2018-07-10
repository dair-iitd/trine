package edu.iitd.nlp.ee.corpus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleSentencesExtractor;
import edu.iitd.nlp.ee.freebase.FreebaseEvent;
import edu.iitd.nlp.ee.stringsearch.ACAnnotation;
import edu.iitd.nlp.ee.stringsearch.ACTrie;
import edu.iitd.nlp.ee.utils.CommonFunctions;
import edu.iitd.nlp.ee.utils.WordnetWrapper;
import edu.iitd.nlp.ee.utils.Global;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.AnnotationSerializer;
import edu.stanford.nlp.pipeline.CustomAnnotationSerializer;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.util.StringUtils;

public class ParallelWarcIndexBuilderForMetaEvents {

	 private static AtomicInteger atomicCount = new AtomicInteger(0);
	
	 private static AtomicInteger at;
	 
	 private static List<File> warcFiles;
	 
	 private static int totalDocs;
	 
	 private static int nextCommitDeadline;
	 
	 private static int nextCommitDeadlineIncrement = 5000;
	 
	 private static AtomicInteger indexWriterCloseCriteria;
	 
	 // private static long startTime;
	 
	 public static void main(String[] args) throws BoilerpipeProcessingException, IOException, InterruptedException {
	    
		//startTime = System.currentTimeMillis();
    	// ////////////////////////////////
		// Read props
		// /////////////////////////////////
		Global.props = StringUtils.argsToProperties(args);
		System.err.println("Running with the following properties...");
		for (Object key : Global.props.keySet()) {
			System.err.println(key + "\t" + Global.props.get(key));
		}
		System.err.println("-----------------------------------------");
		
    	String lucenePath  = Global.props.getProperty("lucenePath");
    	File cluewebFolder = new File(Global.props.getProperty("cluewebFolder"));

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
		
		List<File> files = CommonFunctions.getFileListingNoSort(cluewebFolder);
    	
		warcFiles = new ArrayList<File>();
    	for(File f : files) {
    		if(f.getName().endsWith(".warc.gz") || f.getName().endsWith(".warc")) {
    			warcFiles.add(f);
    		}
    	}
    	
    	nextCommitDeadline = nextCommitDeadlineIncrement;
    	
    	at = new AtomicInteger(warcFiles.size());
    	totalDocs = warcFiles.size();
    	
    	Properties pipelineProps = new Properties();
        
        pipelineProps.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
        pipelineProps.setProperty("parse.maxlen", "80");
        pipelineProps.setProperty("pos.maxlen", "80");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(pipelineProps);
        pipeline.addAnnotator(new TimeAnnotator(true));
    	
    	
		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < iThreadCount; i++) {

			HashMap<String, ArrayList<FreebaseEvent>> metaEventsToEvents = FreebaseEvent.GetRecurrentEventsFromJson(eventDetailsJson, eventAliasFile, allMetaEventsCsvFile, false);
			ACTrie acTrie = FreebaseEvent.BuildACTrieForRecurrentEvent(metaEventsToEvents, wordnetWrapper);
			RunnableIndexer r = new RunnableIndexer( "Thread-" + i, acTrie, pipeline, indexWriter);
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
		      
			  int warcIndexToProcess = at.decrementAndGet();
	    	  while(warcIndexToProcess >= 0) {
	    		 
	    		  File file = warcFiles.get(warcIndexToProcess);
	    		  System.out.println( threadName + " processing " + (warcIndexToProcess+1) + "/" + totalDocs + "\t" + file.getName());
	    		  
	    		  InputStream in = null;
	    		  WarcReader reader = null;
	    		  
	    		  try {
	    	        	
	    			  if(file.getName().endsWith(".gz"))
	    	        	in = new GZIPInputStream(new FileInputStream(file));
	    			  else
	    				  in = new FileInputStream(file);
	    	            
	    	            reader = WarcReaderFactory.getReader( in );
	    	            WarcRecord record;
	    	            
	    	            AnnotationSerializer annSer = new CustomAnnotationSerializer(false, false);
	    	            
	    	            int recordId = 0;
	    	            while ( (record = reader.getNextRecord()) != null ) {
	    	                
	    	            	String payLoad = IOUtils.toString(record.getPayloadContent(), "UTF-8");
	    	            	
	    	            	recordId++;
	    	            	if(recordId == 1) {
	    	            		continue;
	    	            	}
	    	            	
	    	            	String tagStrippedDocument = ArticleSentencesExtractor.INSTANCE.getText(payLoad);
	    	        		
	    	            	String[] paras = tagStrippedDocument.split("[\\r\\n]+");
	    	            	
	    	            	for (int i = 0; i < paras.length; i++) {
	    	            		String rawText = paras[i];
	    	            		String para = paras[i].replaceAll("[^\\x20-\\x7E]+", " ");
	    	            		
	    	            		if(para.length() > 60  && para.length() <= 2500) {
	    		            		
	    	            			ArrayList<ACAnnotation> annotations = acTrie.ProcessInput(para.toLowerCase());
	    		            		
	    							if (annotations.size() > 0) {
	    								
	    								atomicCount.incrementAndGet();
	    								
	    								Annotation annDoc = new Annotation(para);
		    			    	  	    pipeline.annotate(annDoc);
		    			    	  	    
		    			    	  	    ByteArrayOutputStream os = new ByteArrayOutputStream();
		    			    	  	    annSer.write(annDoc, os);
		    			    	  	    os.close();
		    			    	  	    
		    			    	  	    String annDocString = new String(os.toByteArray(),"UTF-8");
	    								
		    			    	  	    String eventsStr = "";
		    			    	  	    HashSet<String> eventsAdded = new HashSet<String>();
		    			    	  	    for(ACAnnotation ac : annotations) {
		    			    	  	    	System.out.println(ac.getMid());
		    			    	  	    	String annStr = ac.getMid().replaceAll("[ ]+", "_");
		    			    	  	    	if(eventsAdded.contains(annStr))
		    			    	  	    		continue;
		    			    	  	    	eventsStr += annStr + " ";
		    			    	  	    	eventsAdded.add(annStr);
		    			    	  	    }
		    			    	  	    
		    			    	  	    eventsStr = eventsStr.trim();
	    								
	    								Document doc = new Document();
	    								String fullPath =  file.getAbsolutePath() + "_" + recordId;
	    								doc.add(new Field("filename", fullPath, Store.YES, Index.NO));
	    								doc.add(new Field("docid", fullPath + "_" + i, Store.YES, Index.NO));
	    								doc.add(new Field("text", para, Store.YES, Index.NO));
	    								doc.add(new Field("rawtext", rawText, Store.YES, Index.NO));
	    								doc.add(new Field("events", eventsStr, Store.YES, Index.ANALYZED));
	    								doc.add(new Field("annDoc", annDocString, Store.YES, Index.NO));
	    								
	    								indexWriter.addDocument(doc);
	    								
	    							}
	    	            		}
	    					}
	    	            }
	    	            
	    	            System.out.println(threadName + " paragraphs extracted so far : " + atomicCount.get());

	    	            reader.close();
	    	            in.close();
	    	            
	    	        } catch (OutOfMemoryError e) {
	    	        	
	    	        	e.printStackTrace();
	    	        	System.out.println(threadName + " died due to file " + file.getAbsolutePath() + " :(");
	    	        	indexWriterCloseCriteria.decrementAndGet();
	    	        	
	    	        	if(reader != null)
	    	        		reader.close();
	    	        	if(in != null) {
							try {
								in.close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
	    	        	}
	    	        
	    	        	return;
	    	        	
	    	        } catch (StackOverflowError e) {
	    	        	
	    	        	e.printStackTrace();
	    	        	System.out.println(threadName + " died due to file " + file.getAbsolutePath() + " :(");
	    	        	indexWriterCloseCriteria.decrementAndGet();
	    	        	
	    	        	if(reader != null)
	    	        		reader.close();
	    	        	if(in != null) {
							try {
								in.close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
	    	        	}
	    	        
	    	        	return;
	    	        	
	    	        } catch (IOException e) {
						e.printStackTrace();
					} catch (BoilerpipeProcessingException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
	    	        	if(reader != null)
	    	        		reader.close();
	    	        	if(in != null) {
							try {
								in.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
	    	        	}
	    	        	
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
	    	        	warcIndexToProcess = at.decrementAndGet(); 
		    	    }
	    		  
	    		  System.out.println( threadName + " processed " + (warcIndexToProcess+1) + "/" + totalDocs + "\t" + file.getName());
	    	  }
	    	  
	    	  if(warcIndexToProcess == indexWriterCloseCriteria.get()) {
				try {
					indexWriter.close();
					System.out.println("Total Paragraphs extracted : " + atomicCount.get());
					System.out.println("Index Writer Closed by " + threadName + " ("+ warcIndexToProcess + ")");
				} catch (CorruptIndexException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	  } else {
	    		  System.out.println("Index Writer left open by " + threadName + " ("+ warcIndexToProcess + ")");
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
