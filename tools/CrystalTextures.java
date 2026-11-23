import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

/**
 * Generates the original 16 × 16 blue crystal textures: {@code block/blue_crystal_cluster.png} (spikes, drawn
 * on a cross model like the amethyst cluster) and {@code item/blue_crystal_shard.png}.
 *
 * <p>FR — Génère les textures originales 16 × 16 du cristal bleu : l'amas (pointes, sur un modèle en croix
 * comme l'amas d'améthyste) et l'éclat.
 *
 * <pre>java tools/CrystalTextures.java src/main/resources/assets/stargate/textures</pre>
 */
public class CrystalTextures {
    /** Deep, dark, mid, light, highlight, white. / Profond, sombre, moyen, clair, reflet, blanc. */
    static final Map<Character, Integer> PALETTE = Map.of(
            'p', 0x14286E, 'd', 0x1E4AA8, 'm', 0x3C7EE0, 'l', 0x72B4FA, 'h', 0xBEE6FF, 'w', 0xF0FAFF);

    static final String[] CLUSTER = {
            "................",
            ".......w........",
            "......hlm.......",
            "......lmd.......",
            "..w...lmd...w...",
            "..hm..lmd..hl...",
            "..lmd.lmd..lm...",
            "...md.lmd.lmd...",
            "...lm.lmd.lmd...",
            "w..lmdlmdhlmd..w",
            "lm.lmdlmdlmd..hm",
            "lmdlmdlmdlmd.lmd",
            ".lmdlmdlmdmd.lmd",
            ".lmdlmdpmdlmdlmd",
            "..mdpmdlmdpmdlm.",
            "..pppppppppppp..",
    };

    static final String[] SHARD = {
            "................",
            "................",
            "..........ww....",
            ".........hlm....",
            "........hlmd....",
            ".......hlmdp....",
            "......hlmdp.....",
            ".....hlmdp......",
            "....hlmdp.......",
            "...hlmdp........",
            "..hlmdp.........",
            "..lmdp..........",
            "..mdp...........",
            "...p............",
            "................",
            "................",
    };

    public static void main(String[] args) throws Exception {
        File t = new File(args[0]);
        write(CLUSTER, new File(t, "block/blue_crystal_cluster.png"));
        write(SHARD, new File(t, "item/blue_crystal_shard.png"));
    }

    static void write(String[] rows, File out) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Integer c = PALETTE.get(rows[y].charAt(x));
                if (c != null) img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println(out);
    }
}
