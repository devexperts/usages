package com.devexperts.usages;

import java.nio.charset.Charset;

class Fmt {
	public static final Charset CHARSET = Charset.forName("UTF-8");

	public static final String COMMENT_PREFIX = "#";
	public static final String CLASS_PREFIX = "\t";
	public static final String MEMBER_PREFIX = "\t\t";
	public static final String USE_KINDS_PREFIX = " -- ";
	public static final String USE_KINDS_SEPARATOR = ",";
}
