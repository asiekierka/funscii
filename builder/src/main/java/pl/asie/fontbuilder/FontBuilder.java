/*
 * Copyright (c) 2016 Adrian Siekierka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.asie.fontbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FontBuilder {
    public static class Entry {
        public final BitSet data;
        public final int width, height;
        public final boolean upscaled;

        public Entry(int width, int height, boolean upscaled, BitSet data) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.upscaled = upscaled;
        }

        @Override
        public String toString() {
            return String.format("{%dx%d = %s}", width, height, data);
        }
    }

    public static class Pointer {
        private final boolean increment;
        private int location;

        public Pointer(int location, boolean increment) {
            this.increment = increment;
            this.location = location;
        }

        public int get() {
            return location;
        }

        public void finish() {
            if (increment) {
                location++;
            }
        }
    }

    private final Map<String, Pointer> pointerMap;
    private final Map<Object, Entry> fontDataMap = new HashMap<>();
    private final Set<Integer> allowedWidths;
    private final int height, maxWidth;
    private final Set<String> args = new HashSet<>();
    private final FontBuilder parent;

    private FontBuilder(FontBuilder parent, Set<Integer> allowedWidths, int height, Map<String, Pointer> pointerMap) {
        this.parent = parent;
        this.height = height;
        this.allowedWidths = allowedWidths;
        int mWidth = 0;
        for (int i : allowedWidths) {
            if (i > mWidth) mWidth = i;
        }
        this.maxWidth = mWidth;
        this.pointerMap = pointerMap;
    }

    public FontBuilder(Set<Integer> allowedWidths, int height) {
        this(null, allowedWidths, height, new HashMap<>());
    }

    public FontBuilder(FontBuilder parent) {
        this(parent, parent.allowedWidths, parent.height, parent.pointerMap);
        args.addAll(parent.args);
    }

    public void setArgs(String[] args) {
        this.args.clear();
        for (String arg : args) {
            this.args.add(arg.toLowerCase());
        }
    }

    public int getHeight() {
        return height;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public Map<Object, Entry> getFontDataMap() {
        return Collections.unmodifiableMap(fontDataMap);
    }

    private void afterKey(String key) {
        if (pointerMap.containsKey(key)) {
            pointerMap.get(key).finish();
        }
    }

    private Entry get(String key) {
        Entry result = null;
        if (key.startsWith("'")) {
            result = fontDataMap.get(key.codePointAt(1));
        } else if (fontDataMap.containsKey(key)) {
            result = fontDataMap.get(key);
        } else if (key.matches("[0-9a-fA-F]+")) {
            try {
                result = fontDataMap.get(Integer.parseInt(key, 16));
            } catch (NumberFormatException e) {
            }
        }

        if (result == null && parent != null) {
            return parent.get(key);
        } else {
            return result;
        }
    }

    private void putObject(Object key, Entry entry) {
        if (!key.equals("IGNORE")) {
            Entry oldEntry = fontDataMap.get(key);
            if (oldEntry == null || !(!oldEntry.upscaled && entry.upscaled)) {
                fontDataMap.put(key, entry);
            }
        }
    }

    private void putString(String key, Entry entry) {
        if (pointerMap.containsKey(key)) {
            putObject(pointerMap.get(key).get(), entry);
        } else if (key.matches("[0-9a-fA-F]+")) {
            try {
                putObject(Integer.parseInt(key, 16), entry);
            } catch (NumberFormatException e) {
                putObject(key, entry);
            }
        } else {
            putObject(key, entry);
        }
    }

    private void error(String key, String font) {
        //System.err.println(String.format("Error at U+%s: %s!", key, font));
    }

    private void readHex(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file); InputStreamReader isr = new InputStreamReader(fis); BufferedReader reader = new BufferedReader(isr)) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] lineParts = line.split(":", 2);
                if (lineParts.length >= 2) {
                    int fontIndex = Integer.parseInt(lineParts[0], 16);
                    int height = getHeight();
                    int width = lineParts[1].length() * 4 / height;
                    BitSet fontData = new BitSet(width * height);
                    for (int i = 0; i < lineParts[1].length(); i += 2) {
                        int value = Integer.parseInt(lineParts[1].substring(i, i + 2), 16);
                        for (int j = 0; j < 8; j++) {
                            int posMirrored = i * 4 + j;
                            int pos = ((posMirrored) - (posMirrored % width)) + width - 1 - (posMirrored % width);
                            fontData.set(pos, (value & (1 << (7 - j))) != 0);
                        }
                    }
                    if (!allowedWidths.contains(width)) {
                        error("" + fontIndex, String.format("Invalid font width: %d", width));
                    } else {
                        Entry entry = new Entry(width, height, false, fontData);
                        putObject(fontIndex, entry);
                    }
                }
            }
        }

    }

    public void read(File file) throws IOException {
        if (file.getName().toLowerCase(Locale.ROOT).endsWith(".hex")) {
            readHex(file);
            return;
        }
        try (FileInputStream fis = new FileInputStream(file); InputStreamReader isr = new InputStreamReader(fis); BufferedReader reader = new BufferedReader(isr)) {
            String line;

            List<String> fontLines = new ArrayList<>();
            List<String> fontKeys = new ArrayList<>();
            int fontWidth = -1;
            BitSet fontData = null;
            boolean drawingFont = false;

            while ((line = reader.readLine()) != null || drawingFont) {
                if (line == null) {
                    line = "";
                } else {
                    boolean defMet = true;
                    while (line.startsWith("IFDEF") || line.startsWith("IFNDEF")) {
                        boolean invert = line.startsWith("IFNDEF");
                        String[] defArgs = line.split(" ", 3);
                        if (defArgs.length < 3) {
                            defMet = false;
                            break;
                        } else {
                            boolean contains = args.contains(defArgs[1].toLowerCase());
                            line = defArgs[2];
                            if ((!invert && !contains) || (invert && contains)) {
                                if (defArgs[2].startsWith("U+")) {
                                    drawingFont = false;
                                }
                                defMet = false;
                                break;
                            }
                        }
                    }
                    if (!defMet) {
                        continue;
                    }
                }

                if (drawingFont) {
                    if (line.startsWith(".") || line.startsWith("#")) {
                        int lWidth = 0;
                        while (lWidth < line.length() && (line.charAt(lWidth) == '.' || line.charAt(lWidth) == '#')) {
                            lWidth++;
                        }
                        if (fontWidth == -1) {
                            fontWidth = lWidth;
                        }

                        if (lWidth == fontWidth) {
                            fontLines.add(line.substring(0, lWidth));
                        }
                    } else if (line.trim().length() == 0 || line.startsWith("U+")) {
                        boolean upscaled = false;

                        if (!allowedWidths.contains(fontWidth)) {
                            error(fontKeys.get(0), String.format("Invalid font width: %d", fontWidth));
                            drawingFont = false;
                        } else if (fontData == null) {
                            fontData = new BitSet(fontWidth * height);
                            if (fontLines.size() == height / 2) {
                                List<String> oldFontLines = fontLines;
                                fontLines = new ArrayList<>();
                                for (String s : oldFontLines) {
                                    fontLines.add(s);
                                    fontLines.add(s);
                                }
                                upscaled = true;
                            } else if (args.contains("upscaleOnly")) {
                                fontData = null;
                                drawingFont = false;
                            }

                            if (drawingFont && fontLines.size() != height) {
                                error(fontKeys.get(0), String.format("Invalid font height: %d", fontLines.size()));
                                fontData = null;
                                drawingFont = false;
                            }
                        }

                        if (drawingFont) {
                            if (fontLines.size() == height) {
                                for (int iy = 0; iy < height; iy++) {
                                    String s = fontLines.get(iy);
                                    for (int ix = 0; ix < fontWidth; ix++) {
                                        boolean c = s.charAt(ix) == '#';
                                        fontData.set(iy * fontWidth + (fontWidth - 1 - ix), c);
                                    }
                                }
                            }
                            Entry entry = new Entry(fontWidth, height, upscaled, fontData);
                            for (String fontKey : fontKeys) {
                                putString(fontKey, entry);
                            }

                            drawingFont = false;
                        }
                    }
                }

                if (!drawingFont) {
                    if (line.startsWith("U+")) {
                        String[] fontKeyArr = line.substring(2).split("\\s+");
                        drawingFont = true;
                        for (String key : fontKeys) {
                            afterKey(key);
                        }
                        fontKeys = new ArrayList<>();
                        fontKeys.add(fontKeyArr[0]);
                        fontData = null;
                        fontWidth = -1;
                        fontLines = new ArrayList<>();
                        if (fontKeyArr.length > 2) {
                            if (fontKeyArr[1].equals("FLIPX")) {
                                Entry entry = get(fontKeyArr[2]);
                                if (entry != null) {
                                    fontData = new BitSet(entry.width * entry.height);
                                    fontWidth = entry.width;
                                    for (int iy = 0; iy < height; iy++) {
                                        for (int ix = 0; ix < fontWidth; ix++) {
                                            fontData.set(iy * fontWidth + (fontWidth - 1 - ix), entry.data.get(iy * fontWidth + ix));
                                        }
                                    }
                                }
                            } else if (fontKeyArr[1].equals("FLIPY")) {
                                Entry entry = get(fontKeyArr[2]);
                                if (entry != null) {
                                    fontData = new BitSet(entry.width * entry.height);
                                    fontWidth = entry.width;
                                    for (int iy = 0; iy < height; iy++) {
                                        for (int ix = 0; ix < fontWidth; ix++) {
                                            fontData.set(iy * fontWidth + ix, entry.data.get((height - 1 - iy) * fontWidth + ix));
                                        }
                                    }
                                }
                            } else if (fontKeyArr[1].equals("DBLW")) {
                                Entry entry = get(fontKeyArr[2]);
                                if (entry != null && (entry.height == height || entry.height * 2 == height)) {
                                    fontWidth = allowedWidths.contains(entry.width * 2) ? entry.width * 2 : entry.width;
                                    fontData = new BitSet(fontWidth * height);
                                    for (int iy = 0; iy < height; iy++) {
                                        for (int z = 0; z < ((entry.height * 2 == height) ? 2 : 1); z++) {
                                            for (int ix = 0; ix < entry.width; ix++) {
                                                int pOrig = iy * entry.width + ix;
                                                int p = iy * fontWidth;
                                                if (fontWidth == entry.width) {
                                                    p = p + ix;
                                                    fontData.set(p, entry.data.get(pOrig));
                                                } else {
                                                    p = p + ix * 2;
                                                    fontData.set(p, entry.data.get(pOrig));
                                                    fontData.set(p + 1, entry.data.get(pOrig));
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (fontKeyArr[1].equals("INV")) {
                                Entry entry = get(fontKeyArr[2]);
                                if (entry != null) {
                                    fontWidth = entry.width;
                                    fontData = new BitSet(entry.width * entry.height);
                                    for (int iy = 0; iy < height; iy++) {
                                        for (int ix = 0; ix < entry.width; ix++) {
                                            int p = iy * fontWidth + ix;
                                            fontData.set(p, !entry.data.get(p));
                                        }
                                    }
                                }
                            } else if (fontKeyArr[1].equals("=")) {
                                String[] fontParamArr = line.split("=", 2)[1].split("\\+");
                                for (String fontParam : fontParamArr) {
                                    String param = fontParam.trim().split("\\s+")[0];
                                    Entry entry = get(param);
                                    if (entry != null) {
                                        if (fontData == null) {
                                            fontData = new BitSet(entry.width * entry.height);
                                            fontWidth = entry.width;
                                        }

                                        fontData.or(entry.data);
                                    }
                                }
                            }
                        }
                    } else if (line.startsWith("INCLUDE")) {
                        String[] args = line.substring(2).split("\\s+");
                        String includeFilename = args[1];
                        File includeFile = new File(includeFilename);
                        if (includeFile.exists()) {
                            FontBuilder builder = new FontBuilder(this);
                            builder.read(includeFile);

                            int rangeMin = -1;
                            int rangeMax = -1;
                            boolean override = true;
                            if (args.length >= 5 && "RANGE".equals(args[2])) {
                                rangeMin = Integer.parseInt(args[3], 16);
                                rangeMax = Integer.parseInt(args[4], 16);
                                if (args.length >= 6 && "NOOVERRIDE".equals(args[5]))
                                    override = false;
                            } else {
                                if (args.length >= 3 && "NOOVERRIDE".equals(args[2]))
                                    override = false;
                            }

                            Map<Object, Entry> entryMap = builder.getFontDataMap();
                            for (Object o : entryMap.keySet()) {
                                if (rangeMin >= 0 && o instanceof Number) {
                                    int oVal = ((Number) o).intValue();
                                    if (oVal < rangeMin || oVal > rangeMax) {
                                        continue;
                                    }
                                }
                                if (!override && getFontDataMap().containsKey(o)) {
                                    continue;
                                }
                                putObject(o, entryMap.get(o));
                            }
                        } else {
                            System.err.println(String.format("File %s does not exist!", includeFilename));
                        }
                    } else if (line.startsWith("SETPOINTER")) {
                        String[] args = line.split("\\s+");
                        if (args.length >= 3) {
                            pointerMap.put(args[1], new Pointer(Integer.parseInt(args[2], 16), args.length >= 4 && args[3].equalsIgnoreCase("INCREMENT")));
                        }
                    } else if (line.startsWith("SET")) {
                        // TODO
                    } else if (line.indexOf("x") > 0 && line.indexOf("x") < 3 && fontKeys.size() > 0) {
                        String[] widthDefArr = line.substring(2).split("\\s+");
                        BitSet oldFontData = fontData;
                        int oldFontWidth = fontWidth;
                        drawingFont = true;
                        fontLines = new ArrayList<>();
                        fontData = null;
                        fontWidth = -1;
                        if (widthDefArr.length > 1) {
                            if (widthDefArr[1].equals("SHIFT") && widthDefArr.length > 2 && height == 16) {
                                int yOffset = Integer.parseInt(widthDefArr[2]);
                                fontWidth = oldFontWidth;
                                fontData = new BitSet(fontWidth * height);
                                for (int iy = 0; iy < 8; iy++) { // TODO: Make work for non-8x8/8x16 cases
                                    for (int ix = 0; ix < fontWidth; ix++) {
                                        int pOld = iy * fontWidth * 2 + ix; // * 2 because the old is prescaled
                                        int pNew = (iy + yOffset) * fontWidth + ix;
                                        if (pNew >= 0 && pNew < (fontWidth * height)) {
                                            fontData.set(pNew, oldFontData.get(pOld));
                                        }
                                    }
                                }
                            } else if (widthDefArr[1].equals("DBLW")) {
                                // TODO
                            }
                        }
                    }
                }
            }

            for (String key : fontKeys) {
                afterKey(key);
            }
        }
    }
}
