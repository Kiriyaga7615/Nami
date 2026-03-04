package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.util.GrimUtils;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.nami.impl.feature.combat.autocrystal.AutoCrystalFeature;
import namidevelopment.kiriyaga.nami.impl.feature.movement.SprintFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.util.EnchantmentUtils;
import namidevelopment.kiriyaga.api.util.entity.TargetUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.RotationUtils.*;
import static namidevelopment.kiriyaga.api.util.entity.PlayerUtils.isItemAWeapon;

@RegisterFeature
public class AuraFeature extends Feature {

    public enum TpsMode { NONE, LATEST, AVERAGE }
    public enum Rotate { NORMAL, HOLD, NONE}
    public enum Sprint { NONE, MOTION, PACKET }
    public enum Swap { NONE, REQUIRE, NORMAL, SILENT }

    public final DoubleSetting attackRange = addSetting(new DoubleSetting("Range", 3.00, 1.0, 6.0));
    public final BoolSetting stanceAbuse = addSetting(new BoolSetting("StanceAbuse", false));
    public final DoubleSetting delay = addSetting(new DoubleSetting("Delay", 0.92, 0.00, 1.00));
    public final EnumSetting<Swap> swap = addSetting(new EnumSetting<>("Swap", Swap.REQUIRE));
    public final EnumSetting<TpsMode> tpsMode = addSetting(new EnumSetting<>("TPS", TpsMode.AVERAGE));
    public final BoolSetting multiTask = addSetting(new BoolSetting("Multitask", true));
    public final EnumSetting<Sprint> stopSprinting = addSetting(new EnumSetting<>("Sprinting", Sprint.NONE));
    public final EnumSetting<Rotate> rotate = addSetting(new EnumSetting<>("Rotate", Rotate.NORMAL));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting render = addSetting(new BoolSetting("Render", true));

    private Entity currentTarget = null;
    private float attackCooldownTicks = 0f;

    public AuraFeature() {
        super("Aura", "Attacks certain targets automatically.", FeatureCategory.of("Combat"), "killaura", "ara", "killara");
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        //ROTATION_SERVICE.cancelRequest(AuraFeature.class.getName()); //no
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPreTickEvent(PreTickEvent event) {
        if (MC.player == null || MC.level == null || MC.player.isDeadOrDying()) return;
        this.clearDisplayInfo();

        AutoCrystalFeature ac = FEATURE_SERVICE.getStorage().getByClass(AutoCrystalFeature.class);
        if (ac.isEnabled() && ac.lastTotalDamage > 0.00f)
            return;

        float tps;
        switch (tpsMode.get()) {
            case LATEST -> tps = SERVER_SERVICE.getLatestTPS();
            case AVERAGE -> tps = SERVER_SERVICE.getAverageTPS();
            default -> tps = 20f;
        }

        attackCooldownTicks -= 1f * (tps / 20f);
        if (attackCooldownTicks < 0f) attackCooldownTicks = 0f;

        if (MC.player.isUsingItem() && MC.player.getUseItem() == MC.player.getInventory().getSelectedItem()) {
            currentTarget = null;
            return;
        }


        if (!multiTask.get() && MC.player.getUseItemRemainingTicks() > 0) {
            currentTarget = null;
            return;
        }

        ItemStack stack = MC.player.getMainHandItem();
        Entity target = TargetUtils.getTarget();

        if (target == null || swap.get() == Swap.REQUIRE && !isItemAWeapon(stack)) {
            currentTarget = null;
            this.clearDisplayInfo();
            return;
        }

        currentTarget = target;
        this.addDisplayInfo(target.getName());

//        if (!ItemStack.areEqual(stack, lastHeldStack)) {
//            lastHeldStack = stack;
//            attackCooldownTicks = getBaseCooldownTicks(stack, tps);
//        }

        boolean skipCooldown = false;

        if (target instanceof ShulkerBullet) {
            skipCooldown = true;
        } else {
            float attackDamage = 1.0f;

            if (MC.player.hasEffect(MobEffects.STRENGTH)) {
                var strength = MC.player.getEffect(MobEffects.STRENGTH);
                attackDamage += 3.0f * (strength.getAmplifier() + 1);
            }

            if (MC.player.hasEffect(MobEffects.WEAKNESS)) {
                var weakness = MC.player.getEffect(MobEffects.WEAKNESS);
                attackDamage -= 4.0f * (weakness.getAmplifier() + 1); // im not sure is it 4 or 3 btw
            }

            if (target instanceof LivingEntity living) {
                if (living.getMaxHealth() <= attackDamage) {
                    skipCooldown = true;
                }
            }
        }

        //        if (MC.player.isGliding() && eyeDist >= 2.601) // yes
        //            return;

        double preRotate;

        switch (rotate.get()) { // yes
            case NORMAL -> preRotate = 0.10;
            case HOLD -> preRotate = 1.00;
            default -> preRotate = 0.10;
        }

        if (FEATURE_SERVICE.getStorage().getByClass(RotationsFeature.class).rotation.get() == RotationsFeature.RotationMode.SILENT)
            preRotate = 0.00; // rotation silent are instant and do not require pre rotate to reduce attack delay

        if ((skipCooldown || attackCooldownTicks <= preRotate * tps)) {
            Vec3 eyePos;

            if (stanceAbuse.get()) {
                double foundDist = Double.MAX_VALUE;
                Vec3 foundEye = MC.player.getEyePosition();
                for (Vec3 v : GrimUtils.getPossibleEyePositions(MC.player)) {
                    Vec3 closest = getClampClosestPoint(v, target.getBoundingBox());
                    double dist = v.distanceTo(closest);
                    if (dist < foundDist) {
                        foundDist = dist;
                        foundEye = v;
                    }
                }
                eyePos = foundEye;
            } else {
                eyePos = MC.player.getEyePosition();
            }

            Vec3 closestPoint = getClosestPointToEye(eyePos, target.getBoundingBox());
            float pYRot = getYRotToVec(eyePos, closestPoint);
            float pXRot = getXRotToVec(eyePos, closestPoint);
            boolean insideBox = target.getBoundingBox().contains(eyePos);

            if (eyePos.distanceTo(getClampClosestPoint(eyePos, target.getBoundingBox())) > attackRange.get()) {
                currentTarget = null;
                return;
            }

            boolean canAttack = rotate.get() == Rotate.NONE;

            if (rotate.get() != Rotate.NONE) {
                ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(AuraFeature.class.getName(), 6, pYRot, pXRot));
                EntityHitResult serverCheck = raycastTarget(eyePos, target, attackRange.get(), ROTATION_SERVICE.getStateHandler().getServerYRot(), ROTATION_SERVICE.getStateHandler().getServerXRot());

                canAttack = serverCheck != null;
            }

            if (insideBox)
                canAttack = true;

            if (!canAttack) {
                //currentTarget = null;
                return;
            }

            SprintFeature m = FEATURE_SERVICE.getStorage().getByClass(SprintFeature.class);
            if (stopSprinting.get() == Sprint.MOTION && m != null && m.isEnabled())
                m.stopSprinting(3);
        }

        if (!skipCooldown && attackCooldownTicks > 0f) {
            //currentTarget = null;
            return;
        }

        boolean b = false;

        if (stopSprinting.get() == Sprint.PACKET && !MC.player.isShiftKeyDown())
            if (MC.player.isSprinting()){
                MC.getConnection().send(new ServerboundPlayerCommandPacket(MC.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                b = true;
            }

        if (swap.get() == Swap.NORMAL || swap.get() == Swap.SILENT) {
            int slot = getWeapon();
            if (slot != -1) {
                INVENTORY_SERVICE.getSwapHandler().attemptSwitch(slot, swap.get() == Swap.SILENT);
            }
        }

        MC.gameMode.attack(MC.player, target);

        if (swing.get())
            MC.player.swing(InteractionHand.MAIN_HAND);

        if (stopSprinting.get() == Sprint.PACKET)
            if (b)
                MC.getConnection().send(new ServerboundPlayerCommandPacket(MC.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));

        if (!skipCooldown) attackCooldownTicks = getBaseCooldownTicks(stack, tps);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPacketReceive(PacketReceiveEvent ev) {
        if (!(ev.getPacket() instanceof ServerboundSetCarriedItemPacket)) return;
        if (MC.player == null || MC.level == null) return;

        MC.execute(() -> {
            ItemStack stack = MC.player.getMainHandItem();
            if (stack == null || stack.isEmpty()) return;

            float tps;
            switch (tpsMode.get()) {
                case LATEST -> tps = SERVER_SERVICE.getLatestTPS();
                case AVERAGE -> tps = SERVER_SERVICE.getAverageTPS();
                default -> tps = 20f;
            }

            attackCooldownTicks = getBaseCooldownTicks(stack, tps);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRender3D(Render3DEvent event) {
        if (!render.get() || currentTarget == null || MC.player == null || MC.player.isDeadOrDying()) return;

        AutoCrystalFeature ac = FEATURE_SERVICE.getStorage().getByClass(AutoCrystalFeature.class);
        if (ac.isEnabled() && ac.lastTotalDamage >= ac.minDamage.get())
            return;

        ColorFeature colorFeature = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        drawBox(currentTarget, colorFeature.getStyledGlobalColor(), event.getMatrices(), event.getTickDelta());
    }

    private void drawBox(Entity entity, Color color, PoseStack matrices, float partialTicks) {
        double interpX = entity.xOld + (entity.getX() - entity.xOld) * partialTicks;
        double interpY = entity.yOld + (entity.getY() - entity.yOld) * partialTicks;
        double interpZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks;
        AABB box = entity.getBoundingBox().move(interpX - entity.getX(), interpY - entity.getY(), interpZ - entity.getZ());

        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }

    private float getBaseCooldownTicks(ItemStack stack, float tps) {
        float baseTicks;

        ItemStack currentStack = ((swap.get() == Swap.SILENT || swap.get() == Swap.NORMAL) && getWeapon() != -1) ? MC.player.getInventory().getItem(getWeapon()) : stack;
        if (currentStack.is(ItemTags.SWORDS)) baseTicks = 13f;
        else if (currentStack.is(ItemTags.AXES)) baseTicks = 21f;
        else if (currentStack.getItem() instanceof TridentItem) baseTicks = 19f;
        else if (currentStack.getItem() instanceof MaceItem) baseTicks = 34f;
        else {
            float attackSpeed = 6f; // 2b2t allows from 4 to 6
            baseTicks = 20f / attackSpeed;
        }

        return (baseTicks * (20f / tps)) * delay.get().floatValue();
    }

    public boolean multitask(){
        if (!multiTask.get())
            return false;

        if (TargetUtils.getTarget() == null)
            return false;

        return false;
    }

    public static int getWeapon() {
        int bestSlot = -1;
        float bestDamage = -1f;

        boolean prioritizeMace = !MC.player.getAbilities().flying && !MC.player.onGround();

        for (int slot = 0; slot < 9; slot++) {
            ItemStack held = MC.player.getInventory().getItem(slot);
            if (held.isEmpty()) continue;

            boolean isSword = held.is(ItemTags.SWORDS);
            boolean isAxe = held.is(ItemTags.AXES);
            boolean isTrident = held.getItem() instanceof TridentItem;
            boolean isMace = held.getItem() instanceof MaceItem;

            if (!isSword && !isAxe && !isTrident && !isMace) continue;

            if (isMace && prioritizeMace) return slot;

            float attackDamage = 0f;

            if (held.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
                ItemAttributeModifiers modifiers = held.get(DataComponents.ATTRIBUTE_MODIFIERS);
                for (var entry : modifiers.modifiers()) {
                    if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                        attackDamage += (float) entry.modifier().amount();
                    }
                }
            }

            if (isSword) attackDamage += 5f;

            int sharpness = EnchantmentUtils.getEnchantmentLevel(held, Enchantments.SHARPNESS);
            int smite = EnchantmentUtils.getEnchantmentLevel(held, Enchantments.SMITE);
            int bane = EnchantmentUtils.getEnchantmentLevel(held, Enchantments.BANE_OF_ARTHROPODS);

            attackDamage += sharpness * 1.25f + smite * 2.5f + bane * 2.5f;

            if (attackDamage > bestDamage) {
                bestDamage = attackDamage;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }
}
