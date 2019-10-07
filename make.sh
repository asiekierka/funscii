#!/bin/sh
cd builder
gradle build
cd ..
rm *.hex
rm *.bmp

BPATH=builder/build/libs/builder-0.1.0.jar
java -cp $BPATH pl.asie.fontbuilder.Main font8.txt 8 hex funscii-8.hex
java -cp $BPATH pl.asie.fontbuilder.Main font8.txt:alt 8 hex funscii-8-alt.hex
java -cp $BPATH pl.asie.fontbuilder.Main font8.txt:fantasy 8 hex funscii-8-fantasy.hex
java -cp $BPATH pl.asie.fontbuilder.Main font8.txt:mcr 8 hex funscii-8-mcr.hex
java -cp $BPATH pl.asie.fontbuilder.Main font8.txt:thin 8 hex funscii-8-thin.hex
java -cp $BPATH pl.asie.fontbuilder.Main font8.txt 16 hex funscii-16.hex
java -cp $BPATH pl.asie.fontbuilder.Main font8.txt 8 bmp funscii-8-coverage.bmp 256 256
java -cp $BPATH pl.asie.fontbuilder.Main font8.txt 16 bmp funscii-16-coverage.bmp 256 256
