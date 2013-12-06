package com.devexperts.usages;

import java.io.IOException;
import java.io.InputStream;

interface Processor {
	public void process(String className, InputStream in) throws IOException;
}
