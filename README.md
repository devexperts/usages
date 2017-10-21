Usages Analysis Tool
======

This tool finds code usages in the specified Maven repositories. It indexes repositories, downloads required artifacts and scans ".class" files in them. The tool analyzes all kinds of dependencies: usages of fields, usages of methods, extensions of classes and implementation of interfaces, usages of annotations, overrides of methods. 

The tool is separated into 2 parts: server application, which collects all information and analyzes classes, and a client one, which is implemented as IntelliJ Plugin. Both of them are currently in development, but alpha versions are available on our portal.
[https://code.devexperts.com/display/USAGES/About+Usages]()

Server
------
Server part of the tool is a Java application and could be run simply:

`java -jar server-${version}.jar`
 

### Configuration properties
All parameters are passed as system variables (`-D<name>=<value>`) 

* **server.port** - defines server port, which is used for find usages requests, *8080* by default;
* **usages.workDir** - defines working directory for *settings.xml* (see section below) and work files (e.g. caches, database), *~/usages/* by default.

### Repositories indexing configuration
You need to provide information about your repositories in `${usages.workDir/settings.xml}` file. See the example below.

```xml
<settings>
    <!-- Repositories to be indexed>
    <repositories> 
        <repository>
            <id>qd</id>
            <url>https://maven.in.devexperts.com/content/repositories/qd/</url>
            <!-- repository system, "nexus" or "artifactory" -->
            <type>nexus</type> 
            <user>username</user>
            <password>password</password>
            <!-- scan repository for new artifacts every 3 hours ->
            <scanTimePeriod>3h</scanTimePeriod>
        </repository>
    <!-- Type of artifacts to be analyzed -->
    <artifactTypes>
        <type>jar</type>
        <type>war</type>
    </artifactTypes>
</settings>
```

IntelliJ Plugin
---------------
Plugin for IntelliJ IDEA should be installed from disk using `idea-plugin-${version}.jar`. After the installation, set URL to usages server in plugin configuration (**Tools --> Configure Usages plugin**). To perform find usages action use **CTRL + F9** (for simple search) or **ALT + CTRL + SHIFT + F9** (with configuration before the search) shortcut when you are on the element to be searched for.