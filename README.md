# Funscii

funscii ("forked unscii") is a personal fork of viznut's [unscii](http://pelulamu.net/unscii/), 
a public domain bitmapped Unicode font for blocky graphics.

### Changes

* Incorporation of Japanese fonts (Misaki for funscii-8, Shinonome for funscii-16)
* Bugfixes (incorrect mappings, etc.)
* (Hopefully) improving the font's Unicode coverage
* Minor personal taste changes (such as the 8x16 smiley faces)

### Compiling

To prepare Unifont:

1. Download the source code tree from [Unifont Utilities](https://unifoundry.com/unifont/unifont-utilities.html)
2. Patch the build system to disable all `unassigned` and `noscript` font files
3. `make BUILDFONT=1`
4. Copy unifont-...-src/font/compiled/*.hex to fonts/unifont
5. Update font-width8.txt to point to the right .hex files.

To build:

    ./make.sh

### License

* The builder code under builder/ is licensed under the terms of the Apache License, version 2.0.
* The font itself is put into the public domain - licensed under the terms of CC0 1.0 Universal.
