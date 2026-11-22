package com.stargate.mod.registry;

import com.stargate.mod.StargateMod;
import com.stargate.mod.stargate.block.StargateChevronBlock;
import com.stargate.mod.stargate.block.StargateControlBlock;
import com.stargate.mod.stargate.block.StargateDhdBlock;
import com.stargate.mod.stargate.block.StargateNaquadahBlock;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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
     * Naquadah block, {@code stargate:naquadah_block}: the plain segments of a gate ring, and the storage
     * block of naquadah ingots (9 ingots).
     *
     * <p><i>FR —</i> Bloc de naquadah, {@code stargate:naquadah_block} : les segments simples d'un anneau de
     * porte, et le bloc de stockage des lingots de naquadah (9 lingots).
     */
    public static final DeferredBlock<StargateNaquadahBlock> NAQUADAH_BLOCK = BLOCKS.registerBlock("naquadah_block",
            StargateNaquadahBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion());

    /**
     * The Dial Home Device, {@code stargate:stargate_dhd}: 38 keys and the dome, dials the nearest gate.
     *
     * <p><i>FR —</i> Le DHD, {@code stargate:stargate_dhd} : 38 touches et le dôme, compose la porte la plus
     * proche.
     */
    public static final DeferredBlock<StargateDhdBlock> STARGATE_DHD = BLOCKS.registerBlock("stargate_dhd",
            StargateDhdBlock::new,
            p -> p.mapColor(MapColor.STONE)
                    .strength(3.5f, 1200.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion());

    /**
     * Naquadah ore, {@code stargate:naquadah_ore}: as rare as diamond (see
     * {@code data/stargate/worldgen}), mined like iron: drops raw naquadah, needs an iron pickaxe.
     *
     * <p><i>FR —</i> Minerai de naquadah, {@code stargate:naquadah_ore} : aussi rare que le diamant (voir
     * {@code data/stargate/worldgen}), se mine comme le fer : donne du naquadah brut, demande une pioche en fer.
     */
    public static final DeferredBlock<DropExperienceBlock> NAQUADAH_ORE = BLOCKS.registerBlock("naquadah_ore",
            p -> new DropExperienceBlock(ConstantInt.of(0), p),
            p -> p.mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.0f, 3.0f));

    /**
     * Block of raw naquadah, {@code stargate:raw_naquadah_block}.
     *
     * <p><i>FR —</i> Bloc de naquadah brut, {@code stargate:raw_naquadah_block}.
     */
    public static final DeferredBlock<Block> RAW_NAQUADAH_BLOCK = BLOCKS.registerSimpleBlock("raw_naquadah_block",
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f));

    /**
     * Blue crystal cluster, {@code stargate:blue_crystal_cluster}: like an amethyst cluster, glows and drops
     * blue crystal shards; found, very rarely, on cave floors. Its shards are needed for the gate's control
     * block.
     *
     * <p><i>FR —</i> Amas de cristal bleu, {@code stargate:blue_crystal_cluster} : comme un amas d'améthyste,
     * il brille et donne des éclats de cristal bleu ; se trouve, très rarement, au sol des grottes. Ses éclats
     * servent à fabriquer le bloc de commande de la porte.
     */
    public static final DeferredBlock<AmethystClusterBlock> BLUE_CRYSTAL_CLUSTER = BLOCKS.registerBlock("blue_crystal_cluster",
            p -> new AmethystClusterBlock(7.0f, 10.0f, p),
            p -> p.mapColor(MapColor.COLOR_BLUE)
                    .forceSolidOn()
                    .noOcclusion()
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .strength(1.5f)
                    .lightLevel(state -> 7)
                    .pushReaction(PushReaction.POPPED));

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
