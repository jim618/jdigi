echo requires jdk 1.6
"c:\Program Files\Java\jdk1.6.0_03\bin\javac" -encoding utf-8 -g -classpath . -d classes  org/wa5znu/znuradio/audio/*.java org/wa5znu/znuradio/dsp/*.java org/wa5znu/znuradio/modems/bpsk/*.java org/wa5znu/znuradio/network/*.java org/wa5znu/znuradio/receiver/*.java
"c:\Program Files\Java\jdk1.6.0_03\bin\jar" cfm znudigi.jar MANIFEST.MF -C classes .
