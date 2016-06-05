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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HEXWriter implements Writer {
    @Override
    public void save(FontBuilder builder, File file, String[] args) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));

        List<Integer> keyList = new ArrayList<>();
        keyList.addAll(builder.getFontDataMap().keySet());
        Collections.sort(keyList);

        for (int k : keyList) {
            FontBuilder.Entry e = builder.getFontDataMap().get(k);
            String s = Integer.toHexString(k);
            while (s.length() < 4) {
                s = "0" + s;
            }
            s += ":";
            byte[] ba = e.data.toByteArray();
            for (int i = 0; i < (e.width * e.height / 8); i++) {
                int ip = i ^ (e.width == 16 ? 1 : 0);
                byte b = ip < ba.length ? ba[ip] : 0;
                String s1 = Integer.toHexString(((int) b) & 0xFF);
                switch (s1.length()) {
                    case 0:
                        s += "00";
                        break;
                    case 1:
                        s += "0" + s1;
                        break;
                    case 2:
                        s += s1;
                        break;
                }
            }
            s += "\n";
            writer.write(s.toUpperCase());
        }

        writer.close();
    }
}
