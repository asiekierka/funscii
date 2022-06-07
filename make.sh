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
java -cp $BPATH pl.asie.fontbuilder.Main font-width6.txt 6 hex out/funscii-6x8.hex
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt 8 hex out/funscii-8x8.hex
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt 16 hex out/funscii-M8x16.hex
#
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt:alt 8 hex out/funscii-8x8-alt.hex
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt:fantasy 8 hex out/funscii-8x8-fantasy.hex
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt:mcr 8 hex out/funscii-8x8-mcr.hex
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt:thin 8 hex out/funscii-8x8-thin.hex
#
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt:unifont 16 hex out/funscii-8x16-unifont.hex
#
java -cp $BPATH pl.asie.fontbuilder.Main font-width6.txt 6 bmp out/funscii-6x8-coverage.bmp 256 256
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt 8 bmp out/funscii-8x8-coverage.bmp 256 512
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt 16 bmp out/funscii-M8x16-coverage.bmp 256 512
#
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt:unifont 16 bmp out/funscii-8x16-unifont-coverage.bmp 256 512
#
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt 8 hex out/funscii-8x8-basic.hex 0 65535
java -cp $BPATH pl.asie.fontbuilder.Main font-width8.txt 16 hex out/funscii-M8x16-basic.hex 0 65535
