#!/bin/sh
cd builder
javac *.java
cd ..
rm *.hex
rm *.bmp
java -cp builder/ Main font.txt 8 hex funscii-8.hex
java -cp builder/ Main font.txt:alt 8 hex funscii-8-alt.hex
java -cp builder/ Main font.txt:fantasy 8 hex funscii-8-fantasy.hex
java -cp builder/ Main font.txt:mcr 8 hex funscii-8-mcr.hex
java -cp builder/ Main font.txt:thin 8 hex funscii-8-thin.hex
java -cp builder/ Main font.txt 16 hex funscii-16.hex
java -cp builder/ Main font.txt 8 bmp funscii-8-coverage.bmp 256 256
java -cp builder/ Main font.txt 16 bmp funscii-16-coverage.bmp 256 256
