package edu.iitd.nlp.ee.stringsearch;

import java.util.ArrayList;
import java.util.HashMap;

public class ACTrieNode {

	String label;
	
	Character value;
	
	HashMap<Character, ACTrieNode> children;
	
	ArrayList<String> entityTypes;
	
	ArrayList<Integer> freebaseListIndices;
	
	ACTrieNode parent;
	
	ACTrieNode suffix;
	
	ACTrieNode dictionarySuffix;
	
	public ACTrieNode addChild(Character c, String parentLabel, String entityType, int freebaseListIndex) {
		
		if(children == null)
			children = new HashMap<Character, ACTrieNode>();
		
		ACTrieNode acTrieNode = null;
		if(children.containsKey(c))
			acTrieNode = children.get(c);
		else {
			acTrieNode = new ACTrieNode();
			acTrieNode.value = c;
			acTrieNode.label = parentLabel + c;
			acTrieNode.parent = this;
			children.put(c, acTrieNode);
		}
		
		if(entityType != null) {
			if(acTrieNode.entityTypes == null)
				acTrieNode.entityTypes = new ArrayList<String>();
			if(acTrieNode.freebaseListIndices == null)
				acTrieNode.freebaseListIndices = new ArrayList<Integer>();
			if(!acTrieNode.entityTypes.contains(entityType)) {
				acTrieNode.entityTypes.add(entityType);
				acTrieNode.freebaseListIndices.add(freebaseListIndex);
			}
		}
		
		return acTrieNode;
	}

	public boolean hasChild(Character c) {
		
		if(this.children != null && children.containsKey(c))
			return true;
		else
			return false;
	}
}
