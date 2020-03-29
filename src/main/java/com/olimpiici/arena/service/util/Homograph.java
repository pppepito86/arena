package com.olimpiici.arena.service.util;


public class Homograph {
	private char cononical;
	private char[] homographs;
	
	public Homograph(char cononical, char[] homographs) {
		super();
		this.cononical = cononical;
		this.homographs = homographs;
	}

	public char getCononical() {
		return cononical;
	}

	public char[] getHomographs() {
		return homographs;
	}
}