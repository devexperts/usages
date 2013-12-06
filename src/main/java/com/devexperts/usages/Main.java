package com.devexperts.usages;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Main {
	public static final String VERSION = "2.2";
	public static final String SYSTEM_PROPERTY_PREFIX = "-D";
	public static final String API_OPTION = "--api";

	public static final String CLASS_SUFFIX = ".class";
	public static final String USAGES_SUFFIX = ".usages";

	private final List<String> allJarFiles = new ArrayList<String>();
	private final List<String> apiJarFiles = new ArrayList<String>();

	private final Cache cache = new Cache();
	private final Usages usages = new Usages(cache);
	private final PublicApi api = new PublicApi(cache);

	private final byte[] tempBuf = new byte[65536];

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			help(System.err);
			return;
		}
		Main main = new Main();
		try {
			try {
				main.parse(args);
			} catch (InvalidParam invalidParam) {
				helpError(invalidParam.getMessage());
				return;
			}
			main.go();
		} catch (Throwable t) {
			t.printStackTrace();
			System.err.println();
			helpError("Got exception: " + t);
		}
	}

	private void parse(String[] args) throws InvalidParam {
		boolean deprecatedOption = false;
		for (String s : args) {
			if (s.startsWith(SYSTEM_PROPERTY_PREFIX)) {
				String rest = s.substring(SYSTEM_PROPERTY_PREFIX.length());
				int i = rest.indexOf('=');
				String key = i < 0 ? rest : rest.substring(0, i);
				String value = i < 0 ? "" : rest.substring(i + 1);
				System.setProperty(key, value);
				continue;
			}
			if (s.equals(API_OPTION)) {
				if (deprecatedOption)
					throw new InvalidParam("Duplicate option \"" + API_OPTION + "\".");
				deprecatedOption = true;
				continue;
			}
			if (s.startsWith("-"))
				throw new InvalidParam("Unsupported option \"" + s + "\".");
			allJarFiles.add(s);
			if (deprecatedOption)
				apiJarFiles.add(s);
		}
		if (deprecatedOption && apiJarFiles.isEmpty())
			throw new InvalidParam("Missing <api-jar-files> after \"" + API_OPTION + "\".");
		if (allJarFiles.isEmpty())
			throw new InvalidParam("Missing <usage-jar-files>.");
	}

	private void go() throws IOException {
		Map<String, Processor> usageProcessors = new HashMap<String, Processor>();
		usageProcessors.put(CLASS_SUFFIX, new Processor() {
			public void process(String className, InputStream in) throws IOException {
				usages.parseClass(className, in);
			}
		});
		usageProcessors.put(USAGES_SUFFIX, new Processor() {
			public void process(String className, InputStream in) throws IOException {
				usages.getUsagesForClass(className).readFromStream(in);
			}
		});
		System.out.println("Processing usages");
		for (String arg : allJarFiles)
			process(arg, usageProcessors);
		usages.analyze();
		// write report of process deprecation
		if (apiJarFiles.isEmpty()) {
			usages.writeToZipFile(new File(Config.getUsages()));
		} else {
			Map<String, Processor> apiProcessors = new HashMap<String, Processor>();
			apiProcessors.put(CLASS_SUFFIX, new Processor() {
				public void process(String className, InputStream in) throws IOException {
					api.parseClass(className, in);
				}
			});
			System.out.println("Processing api");
			for (String arg : apiJarFiles)
				process(arg, apiProcessors);
			System.out.println("Removing uses from api classes");
			usages.removeUsesFromClasses(api.getImplClasses());
			api.writeReportToFile(new File(Config.getApi()), usages);
		}
	}

	private static void helpError(String message) {
		System.err.println(message);
		System.err.println("Run without arguments for help.");
	}

	private static void help(PrintStream out) {
		out.println("== Usage Analysis Tool v" + VERSION + " == (C) Devexperts, 2012-2013 ==");
		out.println("   This tool analyzes dependencies between Java classes.");
		out.println("   Use this tool in one of the following ways:");
		out.println();
		out.println("   1) java -jar usages-" + VERSION + ".jar <usage-jar-files>");
		out.println("      Analyzes jar files looking for all members of other classes that use used from there.");
		out.println("      The results of this analysis are written to \"" + Config.getUsages() + "\" file.");
		out.println("      This archive contains human-readable \".usages\" files that capture detailed information");
		out.println("      about usages.");
		out.println();
		out.println("   2) java -jar usages-" + VERSION + ".jar <usage-jar-files> " + API_OPTION + " <api-jar-files>");
		out.println("      Analyzes all jar files looking for deprecated members of api jar files that are used from outside of them.");
		out.println("      The results of this analysis are written to \"" + Config.getApi() + "\" file.");
		out.println();
		out.println("   Here <usage-jar-files> and <api-jar-files> can use wildcard like \"lib" + File.separator + "*.jar\".");
		out.println("   Use \"**\" at last level to scan subdirectories, like \"lib" + File.separator + "**.jar\".");
		out.println("   Zip files with nested zip and jar files are supported and are recursively analyzed.");
		out.println("   The \"" + Config.getUsages() + "\" file that is produced by the tool can be used in <usage-jar-files>");
		out.println("   as a compact source of information about usages.");
		out.println();
		out.println("   Usages for the classes mentioned in \"" + Config.EXCLUDES_PROP + "\" property are excluded from analysis.");
		out.println();
		out.println("   The following JVM system properties are supported by this tool (their defaults are given):");
		helpOption(out, Config.USAGES_PROP, Config.getUsages());
		helpOption(out, Config.API_PROP, Config.getApi());
		helpOption(out, Config.EXCLUDES_PROP, Config.getExcludes());
	}

	private static void helpOption(PrintStream out, String prop, String value) {
		out.println("      " + SYSTEM_PROPERTY_PREFIX + prop + "=" + value);
	}

	private String pathToClassName(String path) {
		return cache.resolveString(path.replace('/', '.'));
	}

	private void process(String arg, Map<String, Processor> processors) throws IOException {
		System.out.println("Processing " + arg);
		File file = new File(arg);
		String name = file.getName();
		if (!name.contains("*") && !name.contains("?")) {
			processZipFile(arg, file, processors);
			return;
		}
		File parentFile = file.getParentFile();
		if (name.contains("**")) {
			File[] dirs = parentFile.listFiles();
			if (dirs != null) {
				for (File dir : dirs) {
					if (dir.isDirectory())
						process(dir + File.separator + name, processors);
				}
			}
			name = name.replace("**", "*");
		}
		final Pattern pattern = Config.globToPattern(name, false);
		String[] fileNames = parentFile.list(new FilenameFilter() {
			public boolean accept(File dir, String fileName) {
				return pattern.matcher(fileName).matches();
			}
		});
		if (fileNames != null)
			for (String fileName : fileNames) {
				File f = new File(parentFile, fileName);
				System.out.println("Processing " + f);
				processZipFile(f.getPath(), f, processors);
			}
	}

	public void processZipFile(String zipPath, File zipFile, Map<String, Processor> processors) throws IOException {
		ZipFile zip = new ZipFile(zipFile);
		try {
			for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
				ZipEntry ze = en.nextElement();
				if (ze.isDirectory())
					continue;
				String entryName = ze.getName();
				String entryPath = zipPath + "!" + entryName;
				if (entryName.endsWith(".zip") || entryName.endsWith(".jar")) {
					File temp = new File("temp" + entryPath.replaceAll("[\\\\!/]", "~") + ".zip");
					System.out.println("Extracting " + entryPath + " to " + temp);
					InputStream in = zip.getInputStream(ze);
					try {
						extract(in, temp);
						processZipFile(entryPath, temp, processors);
					} finally {
						temp.delete();
						in.close();
					}
					continue;
				}
				for (Map.Entry<String, Processor> processorEntry : processors.entrySet()) {
					String suffix = processorEntry.getKey();
					if (entryName.endsWith(suffix)) {
						String className = pathToClassName(entryName.substring(0, entryName.length() - suffix.length()));
						if (Config.excludesClassName(className))
							continue;
						System.out.println("Processing " + entryPath);
						InputStream in = zip.getInputStream(ze);
						try {
							processorEntry.getValue().process(className, in);
						} finally {
							in.close();
						}
					}
				}
			}
		} finally {
			zip.close();
		}
	}

	private void extract(InputStream in, File temp) throws IOException {
		OutputStream out = new FileOutputStream(temp);
		try {
			int n;
			while ((n = in.read(tempBuf)) > 0)
				out.write(tempBuf, 0, n);
		} finally {
			out.close();
		}
	}
}
