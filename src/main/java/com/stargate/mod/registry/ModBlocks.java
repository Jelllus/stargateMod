package com.stargate.mod.registry;

import com.stargate.mod.StargateMod;
import com.stargate.mod.stargate.block.StargateChevronBlock;
import com.stargate.mod.stargate.block.StargateControlBlock;
import com.stargate.mod.stargate.block.StargateNaquadahBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block registrations for the mod: the three blocks of a gate ring. The DHD will be declared here too.
 *
 * <p>All three are {@code noOcclusion()}: once the ring is formed they are hidden behind the gate model,
 * and must neither hide neighbouring faces nor darken the model.
 *
 * <p><i>FR —</i> Enregistrement des blocs du mod : les trois blocs d'un anneau de porte. Le DHD sera aussi
 * déclaré ici.
 *
 * <p>Les trois sont {@code noOcclusion()} : une fois l'anneau formé, ils sont cachés derrière le modèle de la
 * porte et ne doivent ni masquer les faces voisines ni assombrir le modèle.
 */
public final class ModBlocks {
    /**
     * Block register, namespace {@value StargateMod#MODID}.
     *
     * <p><i>FR —</i> Registre des blocs, espace de noms {@value StargateMod#MODID}.
     */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StargateMod.MODID);

    /**
     * Control block of a gate, {@code stargate:stargate_control}: bottom middle of the frame, holds the gate.
     * Very blast resistant; glows while a wormhole is linked.
     *
     * <p><i>FR —</i> Bloc de contrôle d'une porte, {@code stargate:stargate_control} : en bas au milieu du
     * cadre, il porte la porte. Très résistant aux explosions ; lumineux tant qu'un trou de ver est relié.
     */
    public static final DeferredBlock<StargateControlBlock> STARGATE_CONTROL = BLOCKS.registerBlock("stargate_control",
            StargateControlBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .lightLevel(s -> s.getValue(StargateControlBlock.ACTIVE) ? 12 : 0));

    /**
     * Chevron of a gate frame, {@code stargate:stargate_chevron}. Glows while locked.
     *
     * <p><i>FR —</i> Chevron d'un cadre de porte, {@code stargate:stargate_chevron}. Lumineux tant qu'il est
     * verrouillé.
     */
    public static final DeferredBlock<StargateChevronBlock> STARGATE_CHEVRON = BLOCKS.registerBlock("stargate_chevron",
            StargateChevronBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .lightLevel(s -> s.getValue(StargateChevronBlock.LIT) ? 10 : 0));

    /**
     * Naquadah block, {@code stargate:naquadah_block}: the plain segments of a gate ring.
     *
     * <p><i>FR —</i> Bloc de naquadah, {@code stargate:naquadah_block} : les segments simples d'un anneau de
     * porte.
     */
    public static final DeferredBlock<StargateNaquadahBlock> NAQUADAH_BLOCK = BLOCKS.registerBlock("naquadah_block",
            StargateNaquadahBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion());

    private ModBlocks() {
    }

    /**
     * Hooks the register to the mod event bus.
     *
     * <p><i>FR —</i> Branche le registre sur le bus d'événements du mod.
     *
     * @param modEventBus the mod event bus / le bus d'événements du mod
     */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
