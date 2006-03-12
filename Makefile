#
# $Id$
#
# Makefile with default rules for the Java-Algebra-System
# by Heinz kredel
#
# created 2.6.2000 hk 
# modified 1.1.2003 hk 
# todo 

# set this to your jdk binaries path
JDK=/usr/lib/jvm/java-1.5.0/bin
#JDK=/usr/java/jdk1.5.0_01/bin
#JDK=/usr/java/j2sdk1.4.1_01/bin
#JDK=/usr/java/j2sdk1.4.0_01/bin
#JDK=/opt/jdk1.4/bin
#JDK=/opt/jdk1.4.0b3/bin
#JDK=/usr/lib/jdk1.3/bin
#JDK=/opt/jdk1.2.2/bin
#JDK=/usr/lib/java/bin

LIBPATH=/home/kredel/java/lib
JUNITPATH=$(LIBPATH)/junit.jar
LOG4JPATH=$(LIBPATH)/log4j.jar
JOMPPATH=$(LIBPATH)/jomp1.0b.jar
TNJPATH=$(LIBPATH)/tnj.jar
LINTPATH=$(LIBPATH)/lint4j.jar

# --- syncing ----------
DRY=--dry-run
DELETE=
RSYNC=rsync -e ssh -avuz $(DRY) $(DELETE) --exclude=*~ --exclude=*/.jxta/ --exclude=*.log* --exclude=*.out* --exclude=*.txt* --exclude=.svn 
####--exclude=*.ps --exclude=*.pdf --exclude=spin*
PART=jas.j15

all:

home:
	$(RSYNC) krum:java/$(PART)/     .

heinz:
	$(RSYNC) ./                heinz@heinz3:$(PART)

krum:
	$(RSYNC) ./                krum:java/$(PART)

pub:
	$(RSYNC) ./                krum:htdocs/$(PART)

compute:
	$(RSYNC) ./                compute:java/$(PART)


# no need to change below this line

# command line arguments
cl=

#.EXPORT_ALL_VARIABLES :

JASPATH=/home/kredel/jas
DEFS=$(JASPATH)/arith:$(JASPATH)/poly:$(JASPATH)/vector:$(JASPATH)/ring:$(JASPATH)/module:$(JASPATH)/util:$(JASPATH)/application
DOCCLASSES=$(JUNITPATH):$(LOG4JPATH):$(JOMPPATH):$(TNJPATH)
DOCOPTS=-package
#DOCOPTS=-package -version -author
#DOCOPTS=-public -protected -package -author -version

MYCLASSPATH = .:$(DEFS):$(JUNITPATH):$(LOG4JPATH):$(JOMPPATH):$(TNJPATH)

JAVAC=$(JDK)/javac -classpath $(MYCLASSPATH) -d . -Xlint:unchecked
#-Xlint:unchecked
JAVA=$(JDK)/java -classpath $(MYCLASSPATH) -Xms300M -Xmx600M -XX:+AggressiveHeap -XX:+UseParallelGC -verbose:gc 
#JAVA=$(JDK)/java -classpath $(MYCLASSPATH) -verbose:gc -Xrunhprof:cpu=times,format=a
#JAVA=$(JDK)/java -classpath $(MYCLASSPATH) -verbose:gc -verbose:class -verbose:jni
DOC=$(JDK)/javadoc -classpath $(DOCCLASSES)

usage:
	echo; echo "usage: make <name> cl='cmd'"; echo


FIX       = fixm2j
GETC      = getc.pl
#JOPT      = -prof:gb.prof -tm
#-mx100000000


.SUFFIXES :
.SUFFIXES : .class .java 
.PRECIOUS : %.java %.class edu/jas/arith/%.class edu/jas/poly/%.class edu/jas/ring/%.class edu/jas/vector/%.class edu/jas/module/%.class edu/jas/structure/%.class edu/jas/util/%.class edu/jas/application/%.class edu/jas/%.class

.PHONY    : clean doc

all:
	$(JAVAC) *.java

%.class: %.java
	$(JAVAC) $<


edu/jas/%.class: %.java
	$(JAVAC) $<

edu/jas/arith/%.class: %.java
	$(JAVAC) $<

edu/jas/poly/%.class: %.java
	$(JAVAC) $<

edu/jas/ring/%.class: %.java
	$(JAVAC) $<

edu/jas/vector/%.class: %.java
	$(JAVAC) $<

edu/jas/module/%.class: %.java
	$(JAVAC) $<

edu/jas/structure/%.class: %.java
	$(JAVAC) $<

edu/jas/util/%.class: %.java
	$(JAVAC) $<

edu/jas/application/%.class: %.java
	$(JAVAC) $<


edu.jas.%: edu/jas/%.class
	$(JAVA) $@ $(cl)

edu.jas.arith.%: edu/jas/arith/%.class
	$(JAVA) $@ $(cl)

edu.jas.poly.%: edu/jas/poly/%.class
	$(JAVA) $@ $(cl)

edu.jas.ring.%: edu/jas/ring/%.class
	$(JAVA) $@ $(cl)

edu.jas.vector.%: edu/jas/vector/%.class
	$(JAVA) $@ $(cl)

edu.jas.module.%: edu/jas/module/%.class
	$(JAVA) $@ $(cl)

edu.jas.structure.%: edu/jas/structure/%.class
	$(JAVA) $@ $(cl)

edu.jas.util.%: edu/jas/util/%.class
	$(JAVA) $@ $(cl)

edu.jas.application.%: edu/jas/application/%.class
	$(JAVA) $@ $(cl)




FILES=$(wildcard *.java structure/*.java arith/*.java poly/*.java ring/*.java application/*.java vector/*.java module/*.java util/*.java)
LIBS=$(JUNITPATH) $(LOG4JPATH) $(JOMPPATH) $(TNJPATH)
#CLASSES=$(wildcard *.class structure/*.java arith/*.class poly/*.class ring/*.class application/*.class vector/*.class module/*.class util/*.class)
CLASSES=edu/jas
PYS=$(wildcard *.py examples/*.py)
DOCU=$(wildcard jas-log.html index.html problems.html design.html README COPYING COPYING.lgpl sample.jythonrc overview.html */package.html)

doc: $(FILES)
	$(DOC) $(DOCOPTS) -d doc $(FILES) 

jas.jar: $(FILES) $(DOCU) Makefile build.xml log4j.properties $(PYS)
	$(JDK)/jar -cvf jas.jar $(FILES) edu/ $(DOCU) Makefile build.xml log4j.properties $(PYS)
	cp jas.jar /tmp/jas-`date +%Y%j`.jar
#	cp jas.jar /mnt/i/e/kredel/jas-`date +%Y%j`.jar


TOJAR=$(FILES) $(CLASSES) Makefile build.xml log4j.properties
jas-run.jar: GBManifest.MF $(TOJAR)
	$(JDK)/jar -cvfm jas-run.jar GBManifest.MF $(TOJAR)

jas-doc.jar: doc/
	$(JDK)/jar -cvf jas-doc.jar doc/ $(DOCU)


dist: jas.jar jas-run.jar jas-doc.jar $(LIBS)
	tar -cvzf jas-dist.tgz jas.jar jas-run.jar jas-doc.jar $(LIBS)

#links: arith/build.xml module/build.xml vector/build.xml ring/build.xml application/build.xml poly/build.xml structure/build.xml util/build.xml
#

jars: jas.jar jas-run.jar jas-doc.jar


lint:
	$(JDK)/java -jar $(LINTPATH) -v 3 -classpath $(DOCCLASSES):jas.jar -sourcepath src edu.jas.*
#	$(JDK)/java -jar $(LINTPATH) -v 5 -exact -classpath $(DOCCLASSES) -sourcepath src edu.jas.*


clean:
	find . -name "*~" -follow -print -exec rm {} \;

# -eof-
