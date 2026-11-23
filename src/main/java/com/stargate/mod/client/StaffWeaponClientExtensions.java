package com.stargate.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stargate.mod.weapon.StaffWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * First-person hold of the staff weapon: the staff stays level (see {@code firstperson_righthand} in
 * {@code models/item/staff_weapon.json}) and, instead of the arm swing, kicks back a little when it fires, then
 * slides forward again. The kick follows the shot cooldown, so it needs no state of its own.
 *
 * <p><i>FR —</i> Tenue de la lance à la première personne : la lance reste horizontale (voir
 * {@code firstperson_righthand} dans le modèle) et, au lieu du balancement du bras, recule un peu quand elle
 * tire, puis revient en place. Le recul suit le temps de recharge du tir : aucun état propre n'est nécessaire.
 */
public final class StaffWeaponClientExtensions implements IClientItemExtensions {
    /** How far the staff kicks back, in blocks. / <i>FR —</i> Distance du recul, en blocs. */
    private static final float RECOIL_DISTANCE = 0.14F;
    /** Ticks to reach full recoil. / <i>FR —</i> Ticks pour atteindre le recul maximal. */
    private static final float KICK_TICKS = 1.0F;
    /** Ticks to slide back into place. / <i>FR —</i> Ticks pour revenir en place. */
    private static final float RETURN_TICKS = 6.0F;

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, PlayerRenderState playerRenderState, HumanoidArm arm,
                                           ItemStack itemInHand, float partialTick, float equipProcess,
                                           float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // Same as vanilla's item arm transform, without the swing. / Comme la pose vanilla, sans balancement.
        poseStack.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F + recoil(itemInHand, partialTick));
        return true;
    }

    /** Backward offset of the staff (+Z, towards the camera). / Décalage arrière de la lance (+Z, vers la caméra). */
    private static float recoil(ItemStack stack, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0.0F;
        }
        float remaining = player.getCooldowns().getCooldownPercent(stack, partialTick);
        if (remaining <= 0.0F) {
            return 0.0F;
        }
        float elapsed = (1.0F - remaining) * StaffWeaponItem.COOLDOWN_TICKS;
        float amount;
        if (elapsed < KICK_TICKS) {
            amount = elapsed / KICK_TICKS;
        } else {
            float back = Math.min(1.0F, (elapsed - KICK_TICKS) / RETURN_TICKS);
            amount = 1.0F - back * back * (3.0F - 2.0F * back);
        }
        return amount * RECOIL_DISTANCE;
    }
}
