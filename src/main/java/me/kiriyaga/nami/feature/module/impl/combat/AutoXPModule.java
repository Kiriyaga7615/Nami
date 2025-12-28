package me.kiriyaga.nami.feature.module.impl.combat;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import me.kiriyaga.nami.util.EnchantmentUtils;
import me.kiriyaga.nami.util.entity.TargetUtils;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.references.Items;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;


import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.PacketUtils.sendSequencedPacket;
import static me.kiriyaga.nami.util.entity.PlayerUtils.isPhased;

@RegisterModule
public class AutoXPModule extends Module {

    public enum SwapMode {NORMAL, SILENT }

    private final IntSetting durability = addSetting(new IntSetting("Durability", 80, 70, 99));
    private final BoolSetting packet = addSetting(new BoolSetting("Packet", false));
    private final IntSetting packetShift = addSetting(new IntSetting("ShiftTicks", 3, 1, 6));
    private final BoolSetting whenNoTarget = addSetting(new BoolSetting("NoTarget", false));
    private final BoolSetting onlyPhased = addSetting(new BoolSetting("OnlyPhased", true));
    private final BoolSetting selfToggle = addSetting(new BoolSetting("SelfToggle", true));
    private final EnumSetting<SwapMode> swapMode = addSetting(new EnumSetting<>("Swap", SwapMode.NORMAL));
    private final BoolSetting is1_12 = addSetting(new BoolSetting("1.12", false));

    public AutoXPModule() {
        super("AutoXP", "Automatically repair armor with XP bottles.", ModuleCategory.of("Combat"), "autoxp");
        packetShift.setShowCondition(packet::get);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private void onPreTickEvent(PreTickEvent ev) {
        if (!isEnabled() || MC.player == null || MC.world == null) return;

        if (is1_12.get() && anyAboveThreshold()) { // old minecraft versions does not have mending bugfix
            if (selfToggle.get())
                toggle();
            return;
        }

        if (whenNoTarget.get() && TargetUtils.getTarget() != null) {
            if (selfToggle.get())
                toggle();
            return;
        }

        if (onlyPhased.get() && !isPhased(MC.player)) {
            if (selfToggle.get())
                toggle();

            return;
        }

        int xpSlot = getSlotInHotbar(Items.EXPERIENCE_BOTTLE);
        if (xpSlot == -1) {
            if (selfToggle.get())
                toggle();
            return;
        }

        if (!shouldRepair()) {
            if (selfToggle.get())
                toggle();
            return;
        }

        ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(
                this.name,
                6,
                MC.player.getYaw(),
                90.0f,
                RotationModule.RotationMode.MOTION // only motion here sorry
                )
        );

        if (!ROTATION_MANAGER.getRequestHandler().isCompleted(this.name)) return;

        int prevSlot = MC.player.getInventory().getSelectedSlot();

        switch (swapMode.get()) {
            case NORMAL -> {
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(xpSlot);
                MC.interactionManager.interactItem(MC.player, InteractionHand.MAIN_HAND);

                if (packet.get()) {
                    for (int l = 0; l < packetShift.get(); l++) {
                      sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, ROTATION_MANAGER.getStateHandler().getServerYaw(), ROTATION_MANAGER.getStateHandler().getServerPitch()));
                        }
                }

            }
            case SILENT -> {
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(xpSlot);
                MC.interactionManager.interactItem(MC.player, InteractionHand.MAIN_HAND);

                if (packet.get()) {
                    for (int l = 0; l < packetShift.get(); l++) {
                        sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, ROTATION_MANAGER.getStateHandler().getServerYaw(), ROTATION_MANAGER.getStateHandler().getServerPitch()));
                    }
                }

                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(prevSlot);
            }
        }
    }

    private boolean shouldRepair() {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = MC.player.getEquippedStack(slot);
            if (!hasMending(stack))
                continue;

            if (isBelow(stack)) return true;
        }
        return false;
    }

    private boolean anyAboveThreshold() {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = MC.player.getEquippedStack(slot);
            if (isAbove(stack)) return true;
        }
        return false;
    }


    private boolean isBelow(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageable()) return false;
        int max = stack.getMaxDamage();
        int damage = stack.getDamage();
        int percentRemaining = (int) (((max - damage) / (float) max) * 100);
        return percentRemaining <= durability.get();
    }

    private boolean isAbove(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageable()) return false;
        int max = stack.getMaxDamage();
        int damage = stack.getDamage();
        int percentRemaining = (int) (((max - damage) / (float) max) * 100);
        return percentRemaining > durability.get();
    }

    private int getSlotInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) return i;
        }
        return -1;
    }

    private boolean hasMending(ItemStack stack) {
        return EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.MENDING) > 0;
    }
}
