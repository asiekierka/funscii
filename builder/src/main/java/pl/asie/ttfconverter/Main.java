package pl.asie.ttfconverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: ttfconverter [in.ttf] [width] [height] [out.txt]");
            return;
        }

        int w = Integer.parseInt(args[1]);
        int h = Integer.parseInt(args[2]);

        Font font = Font.createFont(Font.TRUETYPE_FONT, new File(args[0]));
        font = font.deriveFont(Font.PLAIN,/* Integer.parseInt(args[1])*/ 8);
        BufferedImage bi = new BufferedImage(256 * w, 256 * h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = bi.createGraphics();
        graphics2D.setFont(font);

        System.out.println(
                graphics2D.getFontMetrics().charWidth('A') + " x " + graphics2D.getFontMetrics().getMaxAscent()
        );

        for (int i = 0; i < 0x10000; i++) {
            if (!font.canDisplay(i)) {
                continue;
            }

            int xOffset = (i & 0xFF) * w;
            int yOffset = (i >> 8) * h;
            String s = new StringBuilder().appendCodePoint(i).toString();

            /* LineMetrics metrics = font.getLineMetrics(s, new FontRenderContext(
                    null, false, false
            )); */

            graphics2D.drawString(s, xOffset, yOffset + graphics2D.getFontMetrics().getMaxAscent());
        }

        if (args[4] != null) {
            ImageIO.write(bi, "png", new File(args[4]));
        }

        try (
                FileOutputStream stream = new FileOutputStream(new File(args[3]));
                OutputStreamWriter osw = new OutputStreamWriter(stream);
                BufferedWriter bw = new BufferedWriter(osw);
        ) {
            String sizeString = w + "x" + h + "\n";

            for (int i = 0; i < 0x10000; i++) {
                if (font.canDisplay(i)) {
                    int xOffset = (i & 0xFF) * w;
                    int yOffset = (i >> 8) * h;

                    StringBuilder header = new StringBuilder("U+");
                    String hexStr = Integer.toHexString(i).toUpperCase(Locale.ROOT);
                    for (int hi = 0; hi < (4 - hexStr.length()); hi++) {
                        header.append("0");
                    }
                    header.append(hexStr);
                    header.append("\n");

                    bw.write(header.toString());
                    bw.write(sizeString);

                    for (int iy = 0; iy < h; iy++) {
                        StringBuilder line = new StringBuilder();
                        for (int ix = 0; ix < w; ix++) {
                            int pixel = bi.getRGB(xOffset + ix, yOffset + iy);
                            line.append(pixel != 0 ? '#' : '.');
                        }
                        line.append("\n");
                        bw.write(line.toString());
                    }

                    bw.write("\n");
                }
            }
        }
    }
}
