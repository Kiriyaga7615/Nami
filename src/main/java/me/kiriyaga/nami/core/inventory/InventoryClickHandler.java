package me.kiriyaga.nami.core.inventory;

import me.kiriyaga.nami.feature.module.impl.combat.AutoTotemModule;
import me.kiriyaga.nami.feature.module.impl.movement.NoSlowModule;
import net.minecraft.client.gui.screens.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.core.NonNullList;

import com.google.common.collect.Lists;

import java.util.List;

import static me.kiriyaga.nami.Nami.*;

public class InventoryClickHandler {

    public boolean pickupSlot(int slotIndex, boolean skipGeneric) {
        return click(slotIndex, 0, ClickType.PICKUP, skipGeneric);
    }

    public boolean quickMoveSlot(int slotIndex, boolean skipGeneric) {
        return click(slotIndex, 0, ClickType.QUICK_MOVE, skipGeneric);
    }

    public boolean throwSlot(int slotIndex, boolean skipGeneric) {
        return click(slotIndex, 0, ClickType.THROW, skipGeneric);
    }

    public boolean swapSlot(int targetSlot, int hotbarSlotIndex, boolean skipGeneric) {
        return click(targetSlot, hotbarSlotIndex, ClickType.SWAP, skipGeneric);
    }


    public boolean pickupSlot(int slotIndex) {
        return click(slotIndex, 0, ClickType.PICKUP);
    }

    public boolean quickMoveSlot(int slotIndex) {
        return click(slotIndex, 0, ClickType.QUICK_MOVE);
    }

    public boolean throwSlot(int slotIndex) {
        return click(slotIndex, 0, ClickType.THROW);
    }

    public boolean swapSlot(int targetSlot, int hotbarSlotIndex) {
        return click(targetSlot, hotbarSlotIndex, ClickType.SWAP);
    }

    private boolean click(int slot, int button, ClickType type) {
        return click(slot, button, type, false);
    }

    private boolean click(int slot, int button, ClickType type, boolean skipGeneric) {
        if (slot < 0) return false;

        NoSlowModule noSlow = MODULE_MANAGER.getStorage().getByClass(NoSlowModule.class);

        if (noSlow != null && noSlow.isEnabled()){
            if (noSlow.invMove.get() == NoSlowModule.InvMove.WAIT && INPUT_MANAGER.hasAnyInput()) {
                MODULE_MANAGER.getStorage().getByClass(AutoTotemModule.class).addDeathReason("invmove", "Inventory Move not allowed by configuration");
                return false;
            }

            if (noSlow.invMove.get() == NoSlowModule.InvMove.STOP) {
                if (INPUT_MANAGER.getFrozenTicks() == 2) return false; // if one stop call existed this tick, we cannot use any other click actions, since it will apply net tick only

                if (!INPUT_MANAGER.isFrozen() && INPUT_MANAGER.hasAnyInput()) {
                    INPUT_MANAGER.freezeInput(2);
                    return false;
                }
            }
        }

        if (MC.currentScreen instanceof ShulkerBoxScreen
                || MC.currentScreen instanceof AnvilScreen
                || MC.currentScreen instanceof BrewingStandScreen
                || MC.currentScreen instanceof CartographyTableScreen
                || MC.currentScreen instanceof CrafterScreen
                || MC.currentScreen instanceof EnchantmentScreen
                || MC.currentScreen instanceof FurnaceScreen
                || MC.currentScreen instanceof GrindstoneScreen
                || MC.currentScreen instanceof HopperScreen
                || MC.currentScreen instanceof HorseScreen
                || MC.currentScreen instanceof MerchantScreen
                || MC.currentScreen instanceof SmithingScreen
                || MC.currentScreen instanceof SmokerScreen
                || MC.currentScreen instanceof StonecutterScreen
                || (MC.currentScreen instanceof GenericContainerScreen && !skipGeneric)
                || MC.currentScreen instanceof CreativeInventoryScreen) {
            MODULE_MANAGER.getStorage().getByClass(AutoTotemModule.class).addDeathReason("invfail", "Inventory Fail");
            return false;
        }

        AbstractContainerMenu handler = MC.player.currentScreenHandler;

        NonNullList<Slot> slots = handler.slots;
        List<ItemStack> before = Lists.newArrayListWithCapacity(slots.size());
        for (Slot s : slots) before.add(s.getStack().copy());

        MC.interactionManager.clickSlot(handler.syncId, slot, button, type, MC.player);
        return true;
    }
}