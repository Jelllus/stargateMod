import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Turns a black-on-white drawing into a square item texture: dark pixels become the icon colour, white
 * becomes transparent, with box-filtered (anti-aliased) downscaling. Used for the creative tab icon.
 *
 * <p>FR — Transforme un dessin noir sur blanc en texture d'objet carrée : les pixels sombres prennent la
 * couleur de l'icône, le blanc devient transparent, avec une réduction lissée (filtre boîte). Sert pour l'icône
 * de l'onglet créatif.
 *
 * <pre>java tools/TabIcon.java source.png src/main/resources/assets/stargate/textures/item/tab_icon.png 32</pre>
 */
public class TabIcon {
    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        int size = Integer.parseInt(args[2]);
        int w = src.getWidth(), h = src.getHeight();
        // Bounding box of the drawing. / Boîte englobante du dessin.
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (ink(src, x, y) > 0.5) {
                    x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y);
                }
            }
        }
        // Square around it, 1 px margin at the target size. / Carré autour, marge d'1 px à la taille finale.
        double side = Math.max(x1 - x0 + 1, y1 - y0 + 1) * size / (size - 2.0);
        double cx = (x0 + x1 + 1) / 2.0, cy = (y0 + y1 + 1) / 2.0;
        double left = cx - side / 2, top = cy - side / 2, step = side / size;

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int sub = 8;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double sum = 0;
                for (int sy = 0; sy < sub; sy++) {
                    for (int sx = 0; sx < sub; sx++) {
                        int x = (int) Math.floor(left + (px + (sx + 0.5) / sub) * step);
                        int y = (int) Math.floor(top + (py + (sy + 0.5) / sub) * step);
                        sum += x >= 0 && y >= 0 && x < w && y < h ? ink(src, x, y) : 0;
                    }
                }
                int alpha = (int) Math.round(Math.min(1, sum / (sub * sub) * 1.25) * 255); // slightly bolder / un peu plus gras
                out.setRGB(px, py, (alpha << 24) | 0x1E1B18);
            }
        }
        File file = new File(args[1]);
        file.getParentFile().mkdirs();
        ImageIO.write(out, "png", file);
        System.out.println(file);
    }

    /** 1 for black, 0 for white. / 1 pour le noir, 0 pour le blanc. */
    static double ink(BufferedImage img, int x, int y) {
        int rgb = img.getRGB(x, y);
        double lum = (((rgb >> 16) & 255) * 0.299 + ((rgb >> 8) & 255) * 0.587 + (rgb & 255) * 0.114) / 255;
        return 1 - lum;
    }
}
