#!/bin/sh
CLASSPATH=/home/klotz/java/RPSK/
/usr/java/j2sdk1.4.2_03/bin/javac -d classes org/wa5znu/rpsk/*.java
status=$?
/usr/java/j2sdk1.4.2_03/bin/jar -cfm RPSK.jar MANIFEST.MF -C classes/ .
exit $status
