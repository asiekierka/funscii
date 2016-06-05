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

    private final Map<String, Integer> customGlyphPointers = new HashMap<>();
    private final Map<String, Entry> fontDataMapString = new HashMap<>();
    private final Map<Integer, Entry> fontDataMap = new HashMap<>();
    private final Set<Integer> allowedWidths = new HashSet<>();
    private final int height, maxWidth;
    private final Set<String> args = new HashSet<>();

    public FontBuilder(int[] allowedWidths, int height) {
        this.height = height;
        int mWidth = 0;
        for (int i : allowedWidths) {
            if (i > mWidth) mWidth = i;
            this.allowedWidths.add(i);
        }
        this.maxWidth = mWidth;
        customGlyphPointers.put("XTND", 0xE100);
        customGlyphPointers.put("XTND2", 0xE800);
        customGlyphPointers.put("XTND3", 0xEC00);
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

    public Map<Integer, Entry> getFontDataMap() {
        return Collections.unmodifiableMap(fontDataMap);
    }

    private void afterKey(String key) {
        if (customGlyphPointers.containsKey(key)) {
            int k = customGlyphPointers.get(key);
            customGlyphPointers.put(key, k + 1);
        }
    }

    private int key(String key) throws NumberFormatException {
        if (customGlyphPointers.containsKey(key)) {
            int k = customGlyphPointers.get(key);
            return k;
        } else if (key.equals("IGNORE")) {
            return -1;
        } else {
            return Integer.parseInt(key, 16);
        }
    }

    private Entry get(String key) {
        if (key.startsWith("'")) {
            return fontDataMap.get(key.codePointAt(1));
        } else {
            return fontDataMapString.get(key);
        }
    }

    private void error(String key, String font) {
        //System.err.println(String.format("Error at U+%s: %s!", key, font));
    }

    public void read(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
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
                if (line.startsWith("IFDEF")) {
                    String[] defArgs = line.split(" ", 3);
                    if (defArgs.length < 3) {
                        continue;
                    } else {
                        if (!args.contains(defArgs[1].toLowerCase())) {
                            if (defArgs[2].startsWith("U+")) {
                                drawingFont = false;
                            }
                            continue;
                        } else {
                            line = defArgs[2];
                        }
                    }
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
                            if (fontKey.equals("IGNORE")) {
                                continue;
                            }
                            Entry oldEntry = fontDataMapString.get(fontKey);
                            if (oldEntry == null || !(!oldEntry.upscaled && entry.upscaled)) {
                                fontDataMapString.put(fontKey, entry);
                                if (fontKey.matches("[0-9a-fA-F]+") || customGlyphPointers.containsKey(fontKey)) {
                                    fontDataMap.put(key(fontKey), entry);
                                }
                            }
                        }

                        drawingFont = false;
                        fontData = null;
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
                    String includeFilename = line.substring(2).split("\\s+")[1];
                    File includeFile = new File(includeFilename);
                    if (includeFile.exists()) {
                        read(includeFile);
                    } else {
                        System.err.println(String.format("File %s does not exist!", includeFilename));
                    }
                } else if (line.startsWith("SET")) {
                    // TODO
                } else if (line.indexOf("x") > 0 && line.indexOf("x") < 3 && fontKeys.size() > 0) {
                    drawingFont = true;
                    fontWidth = -1;
                    fontLines = new ArrayList<>();
                }
            }
        }

        for (String key : fontKeys) {
            afterKey(key);
        }
    }
}
