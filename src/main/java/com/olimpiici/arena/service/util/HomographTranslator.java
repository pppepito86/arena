package com.olimpiici.arena.service.util;

import java.util.ArrayList;
import java.util.List;

public class HomographTranslator {
	
	public static final List<Homograph> HOMOGRAPHS;
	
	static {
		HOMOGRAPHS = new ArrayList<>();
		HOMOGRAPHS.add(new Homograph('A', new char[] {'А'}));
		HOMOGRAPHS.add(new Homograph('B', new char[] {'Б','В'}));
		HOMOGRAPHS.add(new Homograph('C', new char[] {'С'}));
		HOMOGRAPHS.add(new Homograph('D', new char[] {'Д'}));
		HOMOGRAPHS.add(new Homograph('E', new char[] {'Е'}));
	}
	
	public String translate(String str) {
		String res = "";
		for(int i = 0; i < str.length(); i++) {
			res += translate(str.charAt(i));
		}
		return res;
	}
	
	public char translate(char ch) {
		for (Homograph hom : HOMOGRAPHS) {
			for (char c : hom.getHomographs()) {
				if( c == ch) {
					return hom.getCononical();
				}
			}
		}
		return ch;
	}
}