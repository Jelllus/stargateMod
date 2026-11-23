package com.stargate.mod.weapon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Goa'uld staff weapon: right-click fires a fireball where the player looks (the blaze's small fireball: it
 * burns what it hits and sets blocks alight, but never explodes), then rests for {@link #COOLDOWN_TICKS}.
 *
 * <p><i>FR —</i> Lance Goa'uld : un clic droit tire une boule de feu dans la direction du regard (la petite
 * boule de feu du blaze : elle enflamme ce qu'elle touche et met le feu aux blocs, mais n'explose jamais),
 * puis l'arme se recharge pendant {@link #COOLDOWN_TICKS}.
 */
public class StaffWeaponItem extends Item {
    /** Ticks between two shots. / <i>FR —</i> Ticks entre deux tirs. */
    public static final int COOLDOWN_TICKS = 15;
    /** Initial speed of the fireball, in blocks per tick. / <i>FR —</i> Vitesse initiale, en blocs par tick. */
    public static final double SHOT_SPEED = 1.5;
    /** How far ahead of the eyes the fireball appears. / <i>FR —</i> Distance d'apparition devant les yeux. */
    private static final double MUZZLE_DISTANCE = 1.2;

    /**
     * Creates the item. / <i>FR —</i> Crée l'objet.
     *
     * @param properties item properties / propriétés de l'objet
     */
    public StaffWeaponItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 muzzle = player.getEyePosition().add(look.scale(MUZZLE_DISTANCE));
            SmallFireball fireball = new SmallFireball(serverLevel, player, look);
            fireball.setPos(muzzle.x, muzzle.y - 0.1, muzzle.z);
            fireball.setDeltaMovement(look.scale(SHOT_SPEED));
            serverLevel.addFreshEntity(fireball);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        // CONSUME: no arm swing; the client animates a recoil instead (StaffWeaponClientExtensions).
        // CONSUME : pas de balancement du bras ; le client anime un recul à la place.
        return InteractionResult.CONSUME;
    }
}
