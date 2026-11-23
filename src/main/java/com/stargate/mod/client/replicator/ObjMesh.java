package com.stargate.mod.client.replicator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal Wavefront OBJ reader for entity meshes drawn as custom geometry. Each {@code o} object becomes a
 * named {@link Part}; faces must be quads (a triangle is accepted and stored as a degenerate quad), because
 * entity render types use a QUADS buffer. Texture V is flipped from the OBJ convention (up) to Minecraft's
 * (down).
 *
 * <p><i>FR —</i> Lecteur OBJ Wavefront minimal pour des maillages d'entité dessinés en géométrie
 * personnalisée. Chaque objet {@code o} devient une {@link Part} nommée ; les faces doivent être des quads
 * (un triangle est accepté et stocké en quad dégénéré), car les types de rendu d'entité utilisent un tampon
 * QUADS. Le V de texture est inversé de la convention OBJ (vers le haut) à celle de Minecraft (vers le bas).
 */
public final class ObjMesh {
    private final Map<String, Part> parts;

    private ObjMesh(Map<String, Part> parts) {
        this.parts = parts;
    }

    /**
     * Returns a part by object name.
     *
     * <p><i>FR —</i> Renvoie une partie par nom d'objet.
     *
     * @param name the {@code o} name / le nom {@code o}
     * @return the part / la partie
     * @throws IllegalArgumentException if missing / si absente
     */
    public Part part(String name) {
        Part p = this.parts.get(name);
        if (p == null) {
            throw new IllegalArgumentException("OBJ part not found: " + name);
        }
        return p;
    }

    /**
     * Loads an OBJ from the resource manager.
     *
     * <p><i>FR —</i> Charge un OBJ depuis le gestionnaire de ressources.
     *
     * @param resources the resource manager / le gestionnaire de ressources
     * @param location  the file, e.g. {@code stargate:models/entity/replicator.obj} / le fichier
     * @return the mesh / le maillage
     */
    public static ObjMesh load(ResourceManager resources, Identifier location) {
        List<float[]> v = new ArrayList<>(), vt = new ArrayList<>(), vn = new ArrayList<>();
        Map<String, List<float[]>> faces = new HashMap<>();
        List<float[]> current = faces.computeIfAbsent("default", k -> new ArrayList<>());
        try (BufferedReader in = resources.openAsReader(location)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] t = line.trim().split("\\s+");
                switch (t[0]) {
                    case "o", "g" -> current = faces.computeIfAbsent(t[1], k -> new ArrayList<>());
                    case "v" -> v.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                    case "vt" -> vt.add(new float[]{Float.parseFloat(t[1]), 1f - Float.parseFloat(t[2])});
                    case "vn" -> vn.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                    case "f" -> {
                        int n = t.length - 1;
                        if (n != 3 && n != 4) {
                            throw new IOException("Only triangles and quads are supported: " + line);
                        }
                        for (int k = 0; k < 4; k++) {
                            String[] idx = t[1 + Math.min(k, n - 1)].split("/");
                            float[] p = v.get(Integer.parseInt(idx[0]) - 1);
                            float[] uv = idx.length > 1 && !idx[1].isEmpty() ? vt.get(Integer.parseInt(idx[1]) - 1) : new float[2];
                            float[] nr = idx.length > 2 ? vn.get(Integer.parseInt(idx[2]) - 1) : new float[]{0, 1, 0};
                            current.add(new float[]{p[0], p[1], p[2], uv[0], uv[1], nr[0], nr[1], nr[2]});
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load OBJ " + location, e);
        }
        Map<String, Part> parts = new HashMap<>();
        faces.forEach((name, verts) -> {
            float[] data = new float[verts.size() * 8];
            for (int i = 0; i < verts.size(); i++) {
                System.arraycopy(verts.get(i), 0, data, i * 8, 8);
            }
            parts.put(name, new Part(data));
        });
        return new ObjMesh(parts);
    }

    /**
     * A named group of quads, stored flat as {@code x y z u v nx ny nz} per vertex.
     *
     * <p><i>FR —</i> Un groupe nommé de quads, stocké à plat en {@code x y z u v nx ny nz} par sommet.
     *
     * @param data the vertex data / les données des sommets
     */
    public record Part(float[] data) {
        /**
         * Emits every vertex into the buffer.
         *
         * <p><i>FR —</i> Envoie tous les sommets dans le tampon.
         *
         * @param pose    the current pose / la pose courante
         * @param buffer  the target buffer / le tampon cible
         * @param color   ARGB colour / couleur ARGB
         * @param light   packed light / lumière compactée
         * @param overlay packed overlay / overlay compacté
         */
        public void emit(PoseStack.Pose pose, VertexConsumer buffer, int color, int light, int overlay) {
            float[] d = this.data;
            for (int i = 0; i < d.length; i += 8) {
                buffer.addVertex(pose, d[i], d[i + 1], d[i + 2])
                        .setColor(color)
                        .setUv(d[i + 3], d[i + 4])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose, d[i + 5], d[i + 6], d[i + 7]);
            }
        }
    }
}
