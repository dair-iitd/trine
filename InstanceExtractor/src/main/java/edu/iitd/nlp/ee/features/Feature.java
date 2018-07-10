package edu.iitd.nlp.ee.features;

public class Feature {

	String name;
	
	String value;
	
	String featureType;

	public Feature(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
	
	
}
