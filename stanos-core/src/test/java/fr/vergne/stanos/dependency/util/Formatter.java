package fr.vergne.stanos.dependency.util;

import java.util.regex.Pattern;

public class Formatter {

	public static String removeClassPrefixes(String content, Class<?> prefixClass) {
		return content.replaceAll(Pattern.quote(prefixClass.getName() + "$"), "");
	}
}
