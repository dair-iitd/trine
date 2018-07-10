package edu.iitd.nlp.ee.stringsearch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;

public class ACTrie {

	ACTrieNode root;
	
	String SEPERATOR = "@@#@@";
	
	public ACTrie() {
		
		root = new ACTrieNode();
		root.label = "";
		root.value = null;
		root.parent = null;
	}
	
	public void addMention(String mention, String entityName, String mid, int freebaseListIndex) {
		
		if(mention.contains("edition")) {
			if(mention.split("[\\s]+").length < 3) {
				return;
			}
		}
		
		if(mention == null || mention.trim().equals("")) {
			return;
		}
		
		mention = mention.trim();
		
		ACTrieNode currNode = root;
		for (int i = 0; i < mention.length(); i++) {
			Character c = mention.charAt(i);
			if(i == mention.length()-1) {
				currNode = currNode.addChild(c, currNode.label, entityName+SEPERATOR+mid, freebaseListIndex);
			} else
				currNode = currNode.addChild(c, currNode.label, null, -1);
		}
	}
	
	public void InsertSuffixEdges() {
		
		root.suffix = null;
		Queue<ACTrieNode> nodeQueue = new LinkedList<ACTrieNode>();
		if(root.children == null)
			return;
		
		if(root.entityTypes != null)
			root.entityTypes.trimToSize();
		nodeQueue.addAll(root.children.values());
		
		
		while(nodeQueue.peek() != null) {
			ACTrieNode currNode = nodeQueue.poll();
			
			if(currNode.entityTypes != null)
				currNode.entityTypes.trimToSize();
			ACTrieNode parent = currNode.parent;
			if(parent == root) {
				currNode.suffix = root;
			} else {
				while(parent != null && parent.suffix != null) {
					if(parent.suffix.hasChild(currNode.value)) {
						if(currNode.label.endsWith(parent.suffix.children.get(currNode.value).label)) {
							currNode.suffix = parent.suffix.children.get(currNode.value);
							break;
						}
					}
					parent = parent.parent;
				}
			}
			
			if(currNode.suffix == null)
				currNode.suffix = root;
			
			if(currNode.children != null)
				nodeQueue.addAll(currNode.children.values());
		}
		

		InsertDictionarySuffixEdges();
	}
	
	public void InsertDictionarySuffixEdges() {
		
		root.suffix = null;
		Queue<ACTrieNode> nodeQueue = new LinkedList<ACTrieNode>();
		if(root.children == null)
			return;
		
		nodeQueue.addAll(root.children.values());
		
		while(nodeQueue.peek() != null) {
			ACTrieNode currNode = nodeQueue.poll();
			
			ACTrieNode suffixParent = currNode.suffix;
			while(suffixParent != null  && suffixParent != root && (suffixParent.entityTypes == null || suffixParent.entityTypes.size() == 0)) {
				suffixParent = suffixParent.suffix;
			}
			
			if(suffixParent != null && suffixParent != root && suffixParent.entityTypes != null && suffixParent.entityTypes.size() > 0)
				currNode.dictionarySuffix = suffixParent;
			
			if(currNode.children != null)
				nodeQueue.addAll(currNode.children.values());
		}
	}
	
	public ArrayList<ACAnnotation> ProcessInput(String input) {
		
		ArrayList<ACAnnotation> annotations = new ArrayList<ACAnnotation>();
		ACTrieNode currNode = root;
		
		for (int j = 0; j < input.length(); j++) {
			Character c = input.charAt(j);
			if(currNode.hasChild(c)) {
				currNode = currNode.children.get(c);
			} else {
				ACTrieNode suffixNode = currNode.suffix;
				while(suffixNode != null && !suffixNode.hasChild(c)) {
					suffixNode = suffixNode.suffix;
				}
				
				if(suffixNode != null && suffixNode.hasChild(c))
					currNode = suffixNode.children.get(c);
				else
					currNode = root;
			}
			
			ACTrieNode dictionaryEntry = currNode.dictionarySuffix;
			while(dictionaryEntry != null) {
				int start = j+1-dictionaryEntry.label.length();
				if(dictionaryEntry.entityTypes != null && (j+1 == input.length() || input.charAt(j+1) == ' ') && (start == 0 || input.charAt(start-1) == ' ')) {
					for (int i = 0; i < dictionaryEntry.entityTypes.size(); i++) {
						String[] split = dictionaryEntry.entityTypes.get(i).split(Pattern.quote(SEPERATOR));
						String type = split[0];
						String mid = split[1];
						ACAnnotation ann = new ACAnnotation(start, j+1, input.substring(start, j+1), type, mid, dictionaryEntry.freebaseListIndices.get(i));
						annotations.add(ann);
					}
				}
				dictionaryEntry = dictionaryEntry.dictionarySuffix;
			}
			
			int start = j+1-currNode.label.length();
			if(currNode.entityTypes != null && (j+1 == input.length() || input.charAt(j+1) == ' ') && (start == 0 || input.charAt(start-1) == ' ')) {
				for (int i = 0; i < currNode.entityTypes.size(); i++) {
					String[] split = currNode.entityTypes.get(i).split(Pattern.quote(SEPERATOR));
					String type = split[0];
					String mid = split[1];
					ACAnnotation ann = new ACAnnotation(start, j+1, input.substring(start, j+1), type, mid, currNode.freebaseListIndices.get(i));
					annotations.add(ann);
				}
			}
		}
		
		return annotations;
	}
}
