package com.devexperts.usages;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Config {
	private Config() {} // do not create -- static only

	public static final String USAGES_PROP = "usages";
	public static final String API_PROP = "api";
	public static final String EXCLUDES_PROP = "excludes";

	private static Matcher excludesMatcher;

	public static Pattern globToPattern(String s, boolean supportComma) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '*':
				sb.append(".*");
				break;
			case '?':
				sb.append(".");
				break;
			case ',':
				if (supportComma) {
					sb.append("|");
					break;
				}
			default:
				sb.append(Pattern.quote(String.valueOf(c)));
				break;
			}
		}
		return Pattern.compile(sb.toString());
	}

	public static String getUsages() {
		return System.getProperty(USAGES_PROP, "usages.zip");
	}

	public static String getApi() {
		return System.getProperty(API_PROP, "api.txt");
	}

	public static String getExcludes() {
		return System.getProperty(EXCLUDES_PROP,
			"java.*,javax.*,javafx.*,sun.*,sunw.*,COM.rsa.*,com.sun.*,com.oracle.*");
	}

	public static boolean excludesClassName(String excludesClassName) {
		if (excludesMatcher == null)
			excludesMatcher = globToPattern(getExcludes(), true).matcher(excludesClassName);
		else
			excludesMatcher.reset(excludesClassName);
		return excludesMatcher.matches();
	}
}
