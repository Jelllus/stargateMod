import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the DHD textures and the glyph font from one set of 38 original "constellation" symbols.
 *
 * <p>Each symbol is drawn on a 3 × 3 grid of stars:
 * <pre>
 *   a b c
 *   d e f
 *   g h i
 * </pre>
 * A spec is a list of polylines separated by spaces ({@code "aei"} = a → e → i); a single letter is a lone star.
 * The first star of a spec is drawn bigger (the "main star"). Every shape is different and none is a rotation
 * of another nearby one, so symbols stay easy to tell apart.
 *
 * <p>Outputs:
 * <ul>
 *     <li>{@code block/dhd_keys.png} and {@code block/dhd_keys_lit.png}: 8 × 6 cells of 128 px; cells 0–37 the
 *     glyphs (symbols 1–38) on transparent, 38 key top, 39 dome, 40 key sides;</li>
 *     <li>{@code font/glyphs.png}: the same glyphs, white on transparent, 32 px cells, for the
 *     {@code stargate:glyphs} font (symbol n = U+E000 + n; the point of origin in cell 38);</li>
 *     <li>{@code block/gate_track.png}: the gate's turning symbol track, symbols 0–38 (see {@link #writeTrack}).</li>
 * </ul>
 *
 * <p>FR — Génère les textures du DHD et la police des glyphes à partir d'un même jeu de 38 symboles
 * originaux en « constellations », tracés sur une grille 3 × 3 d'étoiles. Une spécification est une liste de
 * lignes brisées séparées par des espaces ; une lettre seule est une étoile isolée ; la première étoile est
 * plus grosse. Toutes les formes sont différentes, pour que les symboles se distinguent facilement.
 *
 * <pre>java tools/DhdGlyphs.java src/main/resources/assets/stargate/textures</pre>
 */
public class DhdGlyphs {
    static final int CELL = 128, COLS = 8, ROWS = 6, SYMBOLS = 38;
    static final int TOP_CELL = 38, DOME_CELL = 39, SIDE_CELL = 40;
    static final int FONT_CELL = 32, FONT_ROWS = 5;

    /** Point of origin (symbol 0): a pyramid under a sun. / Point d'origine (symbole 0) : une pyramide sous un soleil. */
    static final String ORIGIN = "gbig e";
    /** Track cells: 39 symbols, 8 x 5 cells of 160 x 64 px. / Cases de la piste : 39 symboles, 8 x 5 cases de 160 x 64 px. */
    static final int TRACK_W = 160, TRACK_H = 64, TRACK_ROWS = 5;

    /** Symbol 1 to 38. / Symboles 1 à 38. */
    static final String[] SPECS = {
            "aei", "beh def", "gbi", "adgh", "acgi", "abcfi", "gecf", "aiga",
            "bfhdb", "aefi", "beg ei", "abc beh", "gaeci", "cbdg", "ahc", "dbf h",
            "aceh", "gac i", "dfhd", "beh a c", "abfih", "ghi b", "aeg f", "cfeg",
            "bdhb", "adg cfi", "abc ghi e", "gaec", "bcfeb", "gdfc", "aec eh", "ifbd",
            "dfi a", "ab ef gh", "gic", "gdbfi", "ceg a", "dehgd c",
    };

    public static void main(String[] args) throws Exception {
        if (SPECS.length != SYMBOLS) throw new IllegalStateException("need " + SYMBOLS + " specs");
        File textures = new File(args[0]);
        writeKeys(new File(textures, "block/dhd_keys.png"), false);
        writeKeys(new File(textures, "block/dhd_keys_lit.png"), true);
        writeFont(new File(textures, "font/glyphs.png"));
        writeTrack(new File(textures, "block/gate_track.png"));
    }

    static String spec(int id) {
        return id == 0 ? ORIGIN : SPECS[id - 1];
    }

    /**
     * The gate's symbol track: symbol n in cell n, glyph upright with its top towards the ring's outer edge,
     * engraved in brushed metal; a groove separates two symbols.
     * La piste des symboles de la porte : symbole n dans la case n, glyphe droit, le haut vers l'extérieur de
     * l'anneau, gravé dans du métal brossé ; une rainure sépare deux symboles.
     */
    static void writeTrack(File out) throws Exception {
        BufferedImage img = new BufferedImage(TRACK_W * COLS, TRACK_H * TRACK_ROWS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.util.Random noise = new java.util.Random(39);
        for (int id = 0; id <= SYMBOLS; id++) {
            Graphics2D c = (Graphics2D) g.create(id % COLS * TRACK_W, id / COLS * TRACK_H, TRACK_W, TRACK_H);
            c.setPaint(new GradientPaint(0, 0, new Color(176, 186, 198), 0, TRACK_H, new Color(132, 142, 156)));
            c.fillRect(0, 0, TRACK_W, TRACK_H);
            for (int k = 0; k < 90; k++) { // brushed streaks / stries de brossage
                c.setColor(new Color(255, 255, 255, 10 + noise.nextInt(22)));
                int y = noise.nextInt(TRACK_H);
                int x = noise.nextInt(TRACK_W);
                c.drawLine(x, y, x + 20 + noise.nextInt(60), y);
            }
            c.setColor(new Color(58, 62, 70));
            c.fillRect(0, 0, 3, TRACK_H);
            c.fillRect(TRACK_W - 3, 0, 3, TRACK_H);
            c.setColor(new Color(210, 218, 228));
            c.fillRect(3, 0, 1, TRACK_H);
            c.translate(TRACK_W / 2.0 - CELL * 0.45 / 2, TRACK_H / 2.0 - CELL * 0.45 / 2);
            c.scale(0.45, 0.45);
            draw(c, spec(id), new Color(235, 240, 246), 0); // highlight below the groove / reflet sous la gravure
            c.translate(0, -5);
            draw(c, spec(id), id == 0 ? new Color(120, 36, 24) : new Color(40, 44, 52), 0);
            c.dispose();
        }
        g.dispose();
        ImageIO.write(img, "png", out);
        System.out.println(out);
    }

    static void writeKeys(File out, boolean lit) throws Exception {
        BufferedImage img = new BufferedImage(CELL * COLS, CELL * ROWS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Color glyph = lit ? new Color(255, 251, 236) : new Color(240, 226, 190);
        Color outline = lit ? new Color(110, 34, 0) : new Color(20, 18, 16);
        for (int id = 1; id <= SYMBOLS; id++) {
            Graphics2D c = cell(g, id - 1, CELL);
            draw(c, spec(id), outline, 7);
            draw(c, spec(id), glyph, 0);
            c.dispose();
        }
        fill(g, TOP_CELL, lit ? new Color(230, 118, 26) : new Color(66, 61, 56));
        fill(g, SIDE_CELL, lit ? new Color(176, 80, 16) : new Color(44, 41, 38));
        Graphics2D dome = cell(g, DOME_CELL, CELL);
        dome.setPaint(new RadialGradientPaint(new Point(CELL / 3, CELL / 3), CELL * 0.8f, new float[]{0f, 1f},
                lit ? new Color[]{new Color(255, 200, 150), new Color(255, 60, 20)}
                        : new Color[]{new Color(200, 70, 60), new Color(110, 15, 10)}));
        dome.fillRect(0, 0, CELL, CELL);
        dome.dispose();
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println(out);
    }

    /** White glyphs on transparent for the chat font. / Glyphes blancs sur transparent pour la police du chat. */
    static void writeFont(File out) throws Exception {
        BufferedImage img = new BufferedImage(FONT_CELL * COLS, FONT_CELL * FONT_ROWS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int id = 0; id <= SYMBOLS; id++) {
            // Symbol n in cell n - 1; the point of origin in the spare cell 38. / Symbole n en case n - 1 ; origine en case 38.
            Graphics2D c = cell(g, id == 0 ? SYMBOLS : id - 1, FONT_CELL);
            c.scale(FONT_CELL / (double) CELL, FONT_CELL / (double) CELL);
            draw(c, spec(id), Color.WHITE, 2);
            c.dispose();
        }
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println(out);
    }

    static Graphics2D cell(Graphics2D g, int index, int size) {
        return (Graphics2D) g.create(index % COLS * size, index / COLS * size, size, size);
    }

    static void fill(Graphics2D g, int index, Color color) {
        Graphics2D c = cell(g, index, CELL);
        c.setColor(color);
        c.fillRect(0, 0, CELL, CELL);
        c.dispose();
    }

    /** Grid star position in a 128 px cell. / Position d'une étoile de la grille dans une case de 128 px. */
    static double[] at(char p) {
        int k = p - 'a';
        double step = 30; // corner star + outline stays within the inscribed circle / étoile de coin + contour dans le cercle inscrit
        return new double[]{CELL / 2.0 + (k % 3 - 1) * step, CELL / 2.0 + (k / 3 - 1) * step};
    }

    /**
     * Draws a spec; {@code grow} thickens everything (used for the outline pass).
     * Trace une spécification ; {@code grow} épaissit le tout (pour le contour).
     */
    static void draw(Graphics2D c, String spec, Color color, double grow) {
        c.setColor(color);
        c.setStroke(new BasicStroke((float) (11 + 2 * grow), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        boolean first = true;
        for (String line : spec.split(" ")) {
            if (line.length() > 1) {
                Path2D path = new Path2D.Double();
                for (int i = 0; i < line.length(); i++) {
                    double[] p = at(line.charAt(i));
                    if (i == 0) path.moveTo(p[0], p[1]);
                    else path.lineTo(p[0], p[1]);
                }
                c.draw(path);
            }
            for (int i = 0; i < line.length(); i++) {
                double[] p = at(line.charAt(i));
                double r = (first && i == 0 ? 14 : line.length() == 1 ? 11 : 9) + grow;
                c.fill(new Ellipse2D.Double(p[0] - r, p[1] - r, 2 * r, 2 * r));
            }
            first = false;
        }
    }
}
