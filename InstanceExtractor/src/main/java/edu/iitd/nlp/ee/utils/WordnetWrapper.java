package edu.iitd.nlp.ee.utils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;

public class WordnetWrapper {

	IDictionary dict;
	
	public WordnetWrapper(String wordnetDir) throws IOException {
		URL url = new URL ( "file" , null , wordnetDir) ;
		dict = new Dictionary ( url ) ;
		dict.open();
	}
	
	public boolean isWord(String word) {
	
		IIndexWord idxWord = dict.getIndexWord(word,POS.NOUN) ;
		if(idxWord == null)
			return false;
		else
			return true;
	}
	
	public boolean isEvent(String str) {
		
		IIndexWord idxWord = dict.getIndexWord(str,POS.NOUN) ;
		
		if(idxWord == null)
			return false;
		
		Queue<IWord> hypernymQueue = new LinkedList<IWord>();
		for (int i = 0; i < idxWord.getWordIDs().size() && i < 3; i++) {
			IWordID wordID = idxWord.getWordIDs().get(i) ;
			IWord word = dict.getWord(wordID) ;
			hypernymQueue.add(word);
		}
		
		//HashSet<String> allHypernyms = new HashSet<String>();
		
		while(!hypernymQueue.isEmpty()) {
			IWord currWord = hypernymQueue.poll();
			ArrayList<IWord> hypernymList = GetHypernym(currWord);
			for (int i = 0; i < hypernymList.size(); i++) {
				hypernymQueue.add(hypernymList.get(i));
				if(hypernymList.get(i).getLemma().equals("event"))
					return true;
				if(hypernymList.get(i).getLemma().equals("meeting"))
					return true;
			}
		}
		
		return false;
	}
	
	private ArrayList<IWord> GetHypernym(IWord word) {
	
		ArrayList<IWord> hypernymList = new ArrayList<IWord>();
		ISynset synset = word.getSynset();
		List < ISynsetID > hypernyms = synset.getRelatedSynsets ( Pointer.HYPERNYM );
		for ( ISynsetID sid : hypernyms ) {
			List <IWord> words = dict. getSynset ( sid ) . getWords () ;
			for (Iterator<IWord> i = words.iterator() ; i.hasNext () ;) {
				IWord w = i . next ();
				hypernymList.add(w) ;
			}
		}
		
		return hypernymList;
	}
	
	public static void main(String[] args) throws IOException {
		
		String wordnetDir = "/Users/dineshraghu/IIT/Project/Data/EventExtraction/dict" ;
		WordnetWrapper wordnetWrapper = new WordnetWrapper(wordnetDir);
		System.out.println(wordnetWrapper.isEvent("amendment"));
	}
}
