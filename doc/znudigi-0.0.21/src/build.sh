#!/bin/sh
echo requires jdk 1.6
javac -encoding utf-8 -g -classpath . -d classes  org/wa5znu/znuradio/audio/*.java org/wa5znu/znuradio/dsp/*.java org/wa5znu/znuradio/modems/bpsk/*.java org/wa5znu/znuradio/network/*.java org/wa5znu/znuradio/receiver/*.java
jar cfm znudigi.jar MANIFEST.MF -C classes .

