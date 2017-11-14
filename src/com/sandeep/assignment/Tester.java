package com.sandeep.assignment;

public class Tester {

	public static void main(String[] args) {
		String string = "~D(x,y) | ~H(y)";
		String[] substrings = string.split("\\|");
		for (String string2 : substrings) {
			System.out.println(string2.trim());
		}

		for (String string2 : substrings) {
			System.out.println(string2.trim().substring(1, string2.length()-1));
		}
	}
}
