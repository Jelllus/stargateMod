import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Random;

/**
 * Generates the original 16 × 16 naquadah textures: dark green-black metal with green glints.
 * <ul>
 *     <li>{@code block/naquadah_ore_overlay.png}: specks drawn over the vanilla stone texture by the ore model (transparent elsewhere, so no vanilla texture is copied);</li>
 *     <li>{@code block/raw_naquadah_block.png}; {@code block/naquadah_block.png}, in the gate's metal colour;</li>
 *     <li>{@code item/raw_naquadah.png}, {@code item/naquadah_ingot.png}, {@code item/naquadah_nugget.png}.</li>
 * </ul>
 *
 * <p>FR — Génère les textures originales 16 × 16 du naquadah : métal noir-vert aux reflets verts. Le minerai
 * n'est qu'un calque de taches posé par le modèle sur la texture vanilla de pierre
 * (transparent ailleurs : aucune texture vanilla n'est copiée).
 *
 * <pre>java tools/NaquadahTextures.java src/main/resources/assets/stargate/textures</pre>
 */
public class NaquadahTextures {
    /** Palette: outline, dark, mid, light, highlight, green glint. / Palette : contour, sombre, moyen, clair, reflet, éclat vert. */
    static final Map<Character, Integer> PALETTE = Map.of(
            'o', 0x0E1211, 'd', 0x222E2A, 'm', 0x384C44, 'l', 0x567064, 'h', 0x84A092, 'g', 0x6EE696);

    /**
     * Gate metal (average colour of the gate ring texture, 134 143 150), for the naquadah block the ring is
     * built from. / Métal de la porte (couleur moyenne de la texture de l'anneau), pour le bloc de naquadah dont
     * l'anneau est fait.
     */
    static final Map<Character, Integer> GATE = Map.of(
            'o', 0x3A4046, 'd', 0x60686F, 'm', 0x868F96, 'n', 0x7A838B, 'l', 0xA8B1BA, 'h', 0xCDD4DC);

    static final String[] ORE = {
            "................",
            "..od............",
            ".ohmd.......om..",
            ".odmd......ohld.",
            "..dd.......omd..",
            "......om........",
            ".....ohgd.......",
            ".....omdd...od..",
            "......dd...ohmd.",
            "............dd..",
            "..om............",
            ".ohld.....oml...",
            ".omdd....ohgmd..",
            "..dd.....omdd...",
            "..........dd....",
            "................",
    };

    static final String[] RAW = {
            "................",
            "................",
            "................",
            ".....oooo.......",
            "....ohhlmo......",
            "...ohlglmmooo...",
            "..ohllmmmlhmmo..",
            "..olmmdmllmgdo..",
            ".ohlmmddmmmddo..",
            ".olmgdddmmdddo..",
            ".ommddddmdddo...",
            "..oddddddddo....",
            "...oooooooo.....",
            "................",
            "................",
            "................",
    };

    static final String[] INGOT = {
            "................",
            "................",
            "................",
            "................",
            "......oooooooo..",
            "....oohhhhhhlo..",
            "..oohhlglllmmo..",
            ".ohllllllllmmdo.",
            ".ollllllllmmddo.",
            ".ommmmmmmmmdddo.",
            ".odmmmmmmmmddoo.",
            ".oddddddddddo...",
            "..oooooooooo....",
            "................",
            "................",
            "................",
    };

    static final String[] NUGGET = {
            "................",
            "................",
            "................",
            "................",
            "................",
            ".......ooo......",
            ".....oohhlo.....",
            "....ohlglmmo....",
            "....olllmmdo....",
            "....odmmmddo....",
            ".....oddddo.....",
            "......oooo......",
            "................",
            "................",
            "................",
            "................",
    };

    public static void main(String[] args) throws Exception {
        File t = new File(args[0]);
        write(fromMap(ORE), new File(t, "block/naquadah_ore_overlay.png"));
        write(fromMap(RAW), new File(t, "item/raw_naquadah.png"));
        write(fromMap(INGOT), new File(t, "item/naquadah_ingot.png"));
        write(fromMap(NUGGET), new File(t, "item/naquadah_nugget.png"));
        write(rawBlock(), new File(t, "block/raw_naquadah_block.png"));
        write(storageBlock(), new File(t, "block/naquadah_block.png"));
        write(chevron(false), new File(t, "block/stargate_chevron.png"));
        write(chevron(true), new File(t, "block/stargate_chevron_lit.png"));
        write(control(false), new File(t, "block/stargate_control.png"));
        write(chevronItem(), new File(t, "item/chevron.png"));
        write(control(true), new File(t, "block/stargate_control_active.png"));
    }

    /**
     * The naquadah block with a small stargate in the middle: dark ring, 7 chevrons (none at the bottom), and
     * an empty opening, or a blue event horizon with lit chevrons when active.
     * Le bloc de naquadah avec une petite porte des étoiles au milieu : anneau sombre, 7 chevrons (aucun en
     * bas), ouverture vide, ou horizon des événements bleu et chevrons allumés quand elle est active.
     */
    static BufferedImage control(boolean active) {
        BufferedImage img = storageBlock();
        double cx = 7.5, cy = 7.5;
        for (int y = 1; y < 15; y++) {
            for (int x = 1; x < 15; x++) {
                double r = Math.hypot(x - cx, y - cy);
                int c;
                if (r > 6.6) continue;
                else if (r > 5.9) c = 0x22262A;                    // outer rim / bord extérieur
                else if (r > 4.6) c = (x + y) % 3 == 0 ? 0x4A5258 : 0x5A636A; // symbol track / piste des symboles
                else if (r > 3.9) c = 0x22262A;                    // inner rim / bord intérieur
                else if (!active) c = 0x101316;                    // empty opening / ouverture vide
                else c = r < 1.5 ? 0xBEE6FF : r < 2.8 ? 0x72B4FA : 0x3C7EE0; // event horizon / horizon
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        // 7 chevrons, 40° apart from the top, skipping the two bottom slots. / 7 chevrons tous les 40°, sauf les deux du bas.
        for (int slot = 0; slot < 9; slot++) {
            if (slot == 4 || slot == 5) continue;
            double a = Math.toRadians(slot * 40);
            int x = (int) Math.round(cx + 6.0 * Math.sin(a)), y = (int) Math.round(cy - 6.0 * Math.cos(a));
            img.setRGB(x, y, 0xFF000000 | (active ? 0xFF8A20 : 0x8C3A16));
        }
        return img;
    }

    /**
     * A gate chevron (V pointing down, metal frame, lamp inside). k frame shadow, f frame, r lamp, y lamp core.
     * Un chevron de porte (V pointant vers le bas, cadre métallique, lampe à l'intérieur).
     */
    static final String[] CHEVRON = {
            "................",
            "................",
            "................",
            ".kkkk......kkkk.",
            ".kffkk....kkffk.",
            ".kfyrk....kryfk.",
            ".kkfyrk..kryfkk.",
            "..kkfyrkkryfkk..",
            "...kkfyrryfkk...",
            "....kkfyyfkk....",
            ".....kkffkk.....",
            "......kkkk......",
            "................",
            "................",
            "................",
            "................",
    };

    /** The chevron alone, on transparent: the crafting item. / Le chevron seul, sur fond transparent : l'objet d'artisanat. */
    static BufferedImage chevronItem() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Map<Character, Integer> colours = Map.of('k', 0x22262A, 'f', 0x9AA3AB, 'r', 0x6E2A12, 'y', 0x9C4420);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Integer c = colours.get(CHEVRON[y].charAt(x));
                if (c != null) img.setRGB(x, y + 1, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** The naquadah block with a chevron on top; the lamp glows when lit. / Le bloc de naquadah surmonté d'un chevron ; la lampe brille quand il est allumé. */
    static BufferedImage chevron(boolean lit) {
        BufferedImage img = storageBlock();
        Map<Character, Integer> colours = Map.of(
                'k', 0x22262A, 'f', 0x9AA3AB,
                'r', lit ? 0xFF7A18 : 0x6E2A12, 'y', lit ? 0xFFD47A : 0x9C4420);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Integer c = colours.get(CHEVRON[y].charAt(x));
                if (c != null) img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    static BufferedImage fromMap(String[] rows) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Integer c = PALETTE.get(rows[y].charAt(x));
                if (c != null) img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** Lumpy raw ore. / Minerai brut bosselé. */
    static BufferedImage rawBlock() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(42);
        double[][] lumps = new double[9][3];
        for (double[] l : lumps) { l[0] = r.nextDouble() * 16; l[1] = r.nextDouble() * 16; l[2] = 2.5 + r.nextDouble() * 2.5; }
        char[] ramp = {'o', 'd', 'm', 'l', 'h'};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double v = 0.15 + r.nextDouble() * 0.15;
                for (double[] l : lumps) {
                    for (int dx = -16; dx <= 16; dx += 16) for (int dy = -16; dy <= 16; dy += 16) { // tile seamlessly / sans raccord
                        double d = Math.hypot(x + 0.5 - l[0] - dx, y + 0.5 - l[1] - dy) / l[2];
                        if (d < 1) v = Math.max(v, (1 - d) * 0.8 + 0.25 - Math.max(0, (x - l[0] - dx) + (y - l[1] - dy)) * 0.05);
                    }
                }
                char c = ramp[(int) Math.min(4, Math.max(0, v * 5))];
                if (c == 'l' && r.nextInt(14) == 0) c = 'g';
                img.setRGB(x, y, 0xFF000000 | PALETTE.get(c));
            }
        }
        return img;
    }

    /** Gate-metal block, bevelled, with an engraved square. / Bloc en métal de porte, biseauté, avec un carré gravé. */
    static BufferedImage storageBlock() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(7);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                char c = r.nextInt(4) == 0 ? 'n' : 'm';
                if (x == 0 || y == 0) c = 'h';
                else if (x == 15 || y == 15) c = 'o';
                else if (x == 1 || y == 1) c = 'l';
                else if (x == 14 || y == 14) c = 'd';
                else if ((x == 4 || x == 11) && y >= 4 && y <= 11 || (y == 4 || y == 11) && x >= 4 && x <= 11) c = 'o';
                else if ((x == 5 || x == 12) && y >= 5 && y <= 12 || (y == 5 || y == 12) && x >= 5 && x <= 12) c = 'l'; // groove highlight / reflet de la gravure
                img.setRGB(x, y, 0xFF000000 | GATE.get(c));
            }
        }
        return img;
    }

    static void write(BufferedImage img, File out) throws Exception {
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println(out);
    }
}
