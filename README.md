# MavenBeautifier

This is a quick extensions for the Maven, which displays Maven execution progress in one line with easily recognizable colors. It also logs the actual Maven log to a file.

#### Requirements

 - Java 8 or newer (didn't really test later versions)
 - Maven 3.5 or newer
 - `mvn` in the path environment variable
 - Unix-like terminal (Git Bash, Cygwin, Linux/OSX Terminal)

#### Installation

To install, just run the jar like this:

    java -jar MavenBeautifier.jar

This should put the jar and mvnb script inside Maven installation folder.

#### Usage

Substitute `mvn` with `mvnb`, when running Maven.

#### Options

Right now, there is only one option, you can add:

 - `-Dcom.stirante.maven.logFileKey=<path>` this will change location of the log file. You can disable logging to file at all by setting this to `none`