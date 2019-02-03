package pl.asie.fontbuilder;/*
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Main {
    private static final Map<String, Writer> WRITERS = new HashMap<>();

    static {
        WRITERS.put("bmp", new ImageWriter("BMP"));
        WRITERS.put("png", new ImageWriter("PNG"));
        WRITERS.put("hex", new HexWriter());
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: pl.asie.fontbuilder.Main <buildfile>[:arg1,[arg2,[...]]] <height (8 or 16)> <output format> <output file> [additional arguments]");
            System.out.println("Available writers: " + Arrays.toString(WRITERS.keySet().toArray(new String[WRITERS.size()])));
            return;
        }
        String[] writerArgs = new String[args.length - 4];
        System.arraycopy(args, 4, writerArgs, 0, writerArgs.length);
        FontBuilder builder;

        try {
            int height = Integer.parseInt(args[1]);
            Set<Integer> allowedWidths = new HashSet<>();
            if (height == 8) {
                allowedWidths.add(8);
                builder = new FontBuilder(allowedWidths, 8);
            } else if (height == 16) {
                allowedWidths.add(8);
                allowedWidths.add(16);
                builder = new FontBuilder(allowedWidths, 16);
            } else {
                System.err.println("Invalid height specified - must be 8 or 16!");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid number specified!");
            return;
        }

        File input = new File(args[0].split(":")[0]);
        File output = new File(args[3]);
        Writer writer = WRITERS.get(args[2].toLowerCase());
        if (args[0].indexOf(":") > 0) {
            String[] builderArgs = args[0].split(":")[1].split(",");
            builder.setArgs(builderArgs);
        }

        if (writer == null) {
            System.err.println("Invalid writer specified!");
            return;
        }
        if (!input.exists()) {
            System.out.println(String.format("File %s does not exist!", args[0]));
        }

        try {
            builder.read(input);
            writer.save(builder, output, writerArgs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
