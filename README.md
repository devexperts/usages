Usages Analysis Tool
====================

This tool analyzes dependencies between Java classes.
It scans ".class" files and analyzes all kinds of dependencies: usages of fields, usages of methods, extension of classes 
and implementation of interfaces, usages of annotations, overrides of methods. It is designed to gather and combine usages 
information from a number of projects and then use this collected usages information while refactoring libraries that are 
being used in those projects. 
Deprecated members that are safe to remove from the library can be automatically identified by the tool. 

Use this tool in one of the following ways:

    java -jar usages.jar <usage-jar-files>

Analyzes jar files looking for all members of other classes that use used from there.
The results of this analysis are written to "usages.zip" file.
This archive contains human-readable ".usages" files that capture detailed information
about usages.

    java -jar usages.jar <usage-jar-files> --api <api-jar-files>

Analyzes all jar files looking for deprecated members of api jar files that are used from outside of them.
The results of this analysis are written to "api.txt" file.

Here <usage-jar-files> and <api-jar-files> can use wildcard like "lib\*.jar".
Use "**" at the last level to scan subdirectories, like "lib\**.jar".
Zip files with nested zip and jar files are supported and are recursively analyzed.
The "usages.zip" file that is produced by the tool can be used in <usage-jar-files>
as a compact source of information about usages.
Usages for the classes mentioned in "excludes" property are excluded from analysis.

The following JVM system properties are supported by this tool (their defaults are given):

 * -Dusages=usages.zip
 * -Dapi=api.txt
 * -Dexcludes=java.*,javax.*,javafx.*,sun.*,sunw.*,COM.rsa.*,com.sun.*,com.oracle.*
