# Funscii

funscii ("forked unscii") is a personal fork of viznut's [unscii](http://pelulamu.net/unscii/), 
a public domain bitmapped Unicode font for blocky graphics.

### Changes

* Incorporation of Japanese fonts (Misaki for funscii-8, Shinonome for funscii-16)
* Bugfixes (incorrect mappings, etc.)
* (Hopefully) improving the font's Unicode coverage
* Minor personal taste changes (such as the 8x16 smiley faces)
* Complete arrows implementation and wrong arrows fixed (Supplemental arrows-b)
* make.sh rewrite to a saver and cleaner solution

### Compiling

**Linux:** Run `sh make.sh` to compile the project and assembling the glyphs.

**Windows:** Run `make.bat` to compile the project and assembling the glyphs.

### License

* The builder code under builder/ is licensed under the terms of the Apache License, version 2.0.
* The font itself is put into the public domain - licensed under the terms of CC0 1.0 Universal.
