#!/bin/sh
cd builder
./gradlew build
cd ..
if [ ! -d out ]; then
	mkdir out
fi
cd out
rm *.hex
rm *.bmp
cd ..

BPATH=builder/build/libs/builder-0.1.0.jar
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt:unifont 16 hex out/funscii-8x16-unifont.hex
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt:unifont 16 bmp out/funscii-8x16-unifont-coverage.bmp 256 512
