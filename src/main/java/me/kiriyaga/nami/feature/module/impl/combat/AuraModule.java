package me.kiriyaga.nami.feature.module.impl.combat;

import me.kiriyaga.nami.core.executable.model.ExecutableThreadType;
import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PacketReceiveEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.event.impl.Render3DEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.client.DebugModule;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.feature.module.impl.movement.SprintModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import me.kiriyaga.nami.util.EnchantmentUtils;
import me.kiriyaga.nami.util.entity.TargetUtils;
import me.kiriyaga.nami.util.render.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.EntityHitResult;

import java.awt.*;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.InteractionUtils.raycastTarget;
import static me.kiriyaga.nami.util.RotationUtils.*;
import static me.kiriyaga.nami.util.render.RenderUtil.drawBoxLines;

@RegisterModule
public class AuraModule extends Module {

    public enum TpsMode { NONE, LATEST, AVERAGE }
    public enum Rotate { NORMAL, HOLD, NONE}
    public enum Sprint { NONE, MOTION, PACKET }
    public enum Swap { NONE, REQUIRE, NORMAL, SILENT }

    public final DoubleSetting attackRange = addSetting(new DoubleSetting("Range", 3.00, 1.0, 6.0));
    public final DoubleSetting delay = addSetting(new DoubleSetting("Delay", 0.89, 0.00, 1.00));
    public final EnumSetting<Swap> swap = addSetting(new EnumSetting<>("Swap", Swap.REQUIRE));
    public final EnumSetting<TpsMode> tpsMode = addSetting(new EnumSetting<>("TPS", TpsMode.NONE));
    public final BoolSetting multiTask = addSetting(new BoolSetting("Multitask", true));
    public final EnumSetting<Sprint> stopSprinting = addSetting(new EnumSetting<>("Sprinting", Sprint.NONE));
    public final EnumSetting<Rotate> rotate = addSetting(new EnumSetting<>("Rotate", Rotate.NORMAL));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting render = addSetting(new BoolSetting("Render", true));

    private Entity currentTarget = null;

    private float attackCooldownTicks = 0f;

    public AuraModule() {
        super("Aura", "Attacks certain targets automatically.", ModuleCategory.of("Combat"), "killaura", "ara", "killara");
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        //ROTATION_MANAGER.cancelRequest(AuraModule.class.getName()); //no
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;

        float tps;
        switch (tpsMode.get()) {
            case LATEST -> tps = SERVER_MANAGER.getLatestTPS();
            case AVERAGE -> tps = SERVER_MANAGER.getAverageTPS();
            default -> tps = 20f;
        }

        attackCooldownTicks -= 1f * (tps / 20f);
        if (attackCooldownTicks < 0f) attackCooldownTicks = 0f;

        MODULE_MANAGER.getStorage().getByClass(DebugModule.class).debugAura(Component.of("cooldown ticks is : "+attackCooldownTicks));

        if (!multiTask.get() && MC.player.isUsingItem()) {
            currentTarget = null;
            return;
        }

        long startTime = System.nanoTime();

        ItemStack stack = MC.player.getMainHandStack();
        Entity target = TargetUtils.getTarget();
        DebugModule debugModule = MODULE_MANAGER.getStorage().getByClass(DebugModule.class);

        if (target == null || (swap.get() == Swap.REQUIRE && !(stack.getItem() instanceof AxeItem
                || stack.isIn(ItemTags.SWORDS)
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MaceItem))) {
            currentTarget = null;
            this.setDisplayInfo("");
            return;
        }

        currentTarget = target;
        this.setDisplayInfo(target.getName().getString());
        long auraLogicStart = System.nanoTime();

//        if (!ItemStack.areEqual(stack, lastHeldStack)) {
//            lastHeldStack = stack;
//            attackCooldownTicks = getBaseCooldownTicks(stack, tps);
//        }

        boolean skipCooldown = false;

        if (target instanceof ShulkerBullet) {
            skipCooldown = true;
        } else {
            ItemStack held = stack;
            float attackDamage = 1.0f;

            if (MC.player.hasStatusEffect(MobEffects.STRENGTH)) {
                var strength = MC.player.getStatusEffect(MobEffects.STRENGTH);
                attackDamage += 3.0f * (strength.getAmplifier() + 1);
            }

            if (MC.player.hasStatusEffect(MobEffects.WEAKNESS)) {
                var weakness = MC.player.getStatusEffect(MobEffects.WEAKNESS);
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

        if (MODULE_MANAGER.getStorage().getByClass(RotationModule.class).rotation.get() == RotationModule.RotationMode.SILENT)
            preRotate = 0.00; // rotation silent are instant and do not require pre rotate to reduce attack delay

        // RayCast as main distance check
        if ((skipCooldown || attackCooldownTicks <= preRotate * tps)) {
            Vec3 eyePos = MC.player.getCameraPosVec(1.0f);
            Vec3 closestPoint = getClosestPointToEye(eyePos, target.getBoundingBox());
            float idealYaw = (float) getYawToVec(MC.player, closestPoint);
            float idealPitch = (float) getPitchToVec(MC.player, closestPoint);

            EntityHitResult distanceCheck = raycastTarget(
                    MC.player,
                    target,
                    attackRange.get() + (MODULE_MANAGER.getStorage().getByClass(RotationModule.class).rotation.get() == RotationModule.RotationMode.MOTION ? 0.10 : 0.00),
                    idealYaw,
                    idealPitch
            );

            boolean insideBox = target.getBoundingBox().contains(MC.player.getEyePos());

            if (!insideBox && distanceCheck == null) {
                currentTarget = null;
                return;
            }
            boolean canAttack = rotate.get() == Rotate.NONE;

            if (rotate.get() != Rotate.NONE) {
                ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(
                        AuraModule.class.getName(),
                        5,
                        idealYaw,
                        idealPitch
                ));

                EntityHitResult serverCheck = raycastTarget(
                        MC.player,
                        target,
                        attackRange.get(),
                        ROTATION_MANAGER.getStateHandler().getServerYaw(),
                        ROTATION_MANAGER.getStateHandler().getServerPitch()
                );

                canAttack = serverCheck != null;

                if (rotate.get() == Rotate.NONE)
                    canAttack = true;
            }

            if (insideBox)
                canAttack = true;

            if (!canAttack) {
                //currentTarget = null;
                return;
            }

            SprintModule m = MODULE_MANAGER.getStorage().getByClass(SprintModule.class);
            if (stopSprinting.get() == Sprint.MOTION && m != null && m.isEnabled())
                m.stopSprinting(3);
        }

        if (!skipCooldown && attackCooldownTicks > 0f) {
            //currentTarget = null;
            return;
        }

        boolean b = false;

        if (stopSprinting.get() == Sprint.PACKET && !MC.player.isSneaking())
            if (MC.player.isSprinting()){
                MC.getNetworkHandler().sendPacket(new ServerboundPlayerCommandPacket(MC.player, ServerboundPlayerCommandPacket.Mode.STOP_SPRINTING));
                b = true;
            }

        int prev = -1;

        if (swap.get() == Swap.NORMAL || swap.get() == Swap.SILENT) {
            int slot = getWeapon();
            if (slot != -1) {
                prev = MC.player.getInventory().getSelectedSlot();
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(slot);
            }
        }

        MC.interactionManager.attackEntity(MC.player, target);

        if (swing.get())
            MC.player.swingHand(InteractionHand.MAIN_HAND);

        if (swap.get() == Swap.SILENT) {
            if (prev != -1) {
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(prev);
            }
        }

        if (stopSprinting.get() == Sprint.PACKET)
            if (b)
                MC.getNetworkHandler().sendPacket(new ServerboundPlayerCommandPacket(MC.player, ServerboundPlayerCommandPacket.Mode.START_SPRINTING));

        if (!skipCooldown) attackCooldownTicks = getBaseCooldownTicks(stack, tps);

        long auraLogicDuration = System.nanoTime() - auraLogicStart;
        debugModule.debugAura(Component.of(String.format("logic time: %.3f ms", auraLogicDuration / 1_000_000.0)));
        long totalDuration = System.nanoTime() - startTime;
        debugModule.debugAura(Component.of(String.format("total %.3f ms", totalDuration / 1_000_000.0)));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPacketReceive(PacketReceiveEvent ev) {
        if (!(ev.getPacket() instanceof ServerboundSetCarriedItemPacket)) return;
        if (MC.player == null || MC.world == null) return;

        EXECUTABLE_MANAGER.getRequestHandler().submit(() -> {
            ItemStack stack = MC.player.getMainHandStack();
            if (stack == null || stack.isEmpty()) return;

            float tps;
            switch (tpsMode.get()) {
                case LATEST -> tps = SERVER_MANAGER.getLatestTPS();
                case AVERAGE -> tps = SERVER_MANAGER.getAverageTPS();
                default -> tps = 20f;
            }

            attackCooldownTicks = getBaseCooldownTicks(stack, tps);
        }, 0, ExecutableThreadType.PRE_TICK);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender3D(Render3DEvent event) {
        if (!render.get() || currentTarget == null) return;

        ColorModule colorModule = MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
        drawBox(currentTarget, colorModule.getStyledGlobalColor(), event.getMatrices(), event.getTickDelta());
    }

    private void drawBox(Entity entity, Color color, PoseStack matrices, float partialTicks) {
        double interpX = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * partialTicks;
        double interpY = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * partialTicks;
        double interpZ = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * partialTicks;
        AABB box = entity.getBoundingBox().offset(interpX - entity.getX(), interpY - entity.getY(), interpZ - entity.getZ());

        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);

    }

    private float getBaseCooldownTicks(ItemStack stack, float tps) {
        float baseTicks;

        ItemStack currentStack = ((swap.get() == Swap.SILENT || swap.get() == Swap.NORMAL) && getWeapon() != -1) ? MC.player.getInventory().getStack(getWeapon()) : stack;
        if (currentStack.isIn(ItemTags.SWORDS)) baseTicks = 13f;
        else if (currentStack.isIn(ItemTags.AXES)) baseTicks = 21f;
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

        boolean prioritizeMace = !MC.player.getAbilities().flying && !MC.player.isOnGround();

        for (int slot = 0; slot < 9; slot++) {
            ItemStack held = MC.player.getInventory().getStack(slot);
            if (held.isEmpty()) continue;

            boolean isSword = held.isIn(ItemTags.SWORDS);
            boolean isAxe = held.isIn(ItemTags.AXES);
            boolean isTrident = held.getItem() instanceof TridentItem;
            boolean isMace = held.getItem() instanceof MaceItem;

            if (!isSword && !isAxe && !isTrident && !isMace) continue;

            if (isMace && prioritizeMace) return slot;

            float attackDamage = 0f;

            if (held.contains(DataComponents.ATTRIBUTE_MODIFIERS)) {
                ItemAttributeModifiers modifiers = held.get(DataComponents.ATTRIBUTE_MODIFIERS);
                for (var entry : modifiers.comp_2393()) {
                    if (entry.comp_2395().matches(Attributes.ATTACK_DAMAGE)) {
                        attackDamage += (float) entry.comp_2396().value();
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
