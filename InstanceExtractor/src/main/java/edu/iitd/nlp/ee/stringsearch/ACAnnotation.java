package edu.iitd.nlp.ee.stringsearch;

public class ACAnnotation {

	int begin;
	
	int end;
	
	String text;
	
	String type;
	
	String mid;
	
	int freebaseIndex;

	public ACAnnotation(int begin, int end, String text, String type, String mid, int freebaseIndex) {
		
		this.begin = begin;
		this.end = end;
		this.text = text;
		this.type = type;
		this.mid = mid;
		this.freebaseIndex = freebaseIndex;
	}

	@Override
	public String toString() {
		return text;
	}

	public int getBegin() {
		return begin;
	}

	public int getEnd() {
		return end;
	}

	public String getText() {
		return text;
	}

	public String getType() {
		return type;
	}
	
	public String getMid() {
		return mid;
	}
	
	public int getFreebaseIndex() {
		return freebaseIndex;
	}
}
