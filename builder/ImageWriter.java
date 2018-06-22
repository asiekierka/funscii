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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.FileReader;
import java.io.LineNumberReader;

public class ImageWriter implements Writer {
    private final String format;

    public ImageWriter(String format) {
        this.format = format.toUpperCase();
    }

    public void save(FontBuilder builder, File file, String[] args) throws IOException {
        int caw = -1;
        int cah = -1;
		Map<Integer,Integer> translationMap = new HashMap<Integer,Integer>();
        if (args.length >= 2) {
            try {
                caw = Integer.parseInt(args[0]);
                cah = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid width/height values!");
            }
			if (args.length >= 3) {
				LineNumberReader transMappingReader = new LineNumberReader(new FileReader(args[2]));
				int nextchar = 0;
				for(String line; (line = transMappingReader.readLine()) != null; ) {
					if (line.length() == 0 || line.startsWith("//")) continue;
					String[] parts = line.split(":");
					switch (parts.length) {
					default:
						System.err.println("Syntax error in translation mapping file, line "+transMappingReader.getLineNumber());
						return;
					case 2:
						try {
							nextchar = Integer.parseInt(parts[1], 16);
						} catch (NumberFormatException e) {
							System.err.println("Second number format error in translation mapping file, line "+transMappingReader.getLineNumber());
							return;
						}
					// Drop through
					case 1:
						try {
							int mapchar = Integer.parseInt(parts[0], 16);
							translationMap.put(nextchar,mapchar);
							nextchar++;
						} catch (NumberFormatException e) {
							System.err.println("First number format error in translation mapping file, line "+transMappingReader.getLineNumber());
							return;
						}
					}
				}
			}	
        } else {
            caw = 256;
            int maxPos = 0;
            for (Object o : builder.getFontDataMap().keySet()) {
                if (o instanceof Integer) {
                    int i = ((Integer) o).intValue();
                    if (i > maxPos) {
                        maxPos = i;
                    }
                }
            }
            cah = (int) Math.ceil((maxPos + 1) / 256.0);
        }
        int cw = builder.getMaxWidth();
        int ch = builder.getHeight();
        BufferedImage image = new BufferedImage(caw * cw, cah * ch, BufferedImage.TYPE_BYTE_BINARY);
        for (int i = 0; i < caw * cah; i++) {
			Integer remap = translationMap.get(i);
            FontBuilder.Entry e = builder.getFontDataMap().get(remap==null?i:remap);
            if (e != null) {
                int cx = (i % caw) * cw;
                int cy = (i / caw) * ch;
                for (int y = 0; y < e.height; y++) {
                    for (int x = 0; x < e.width; x++) {
                        if (e.data.get(y * e.width + (e.width - 1 - x))) {
                            image.setRGB(cx + x, cy + y, 0xFFFFFFFF);
                        }
                    }
                }
            }
        }

        ImageIO.write(image, format, file);
    }
}
