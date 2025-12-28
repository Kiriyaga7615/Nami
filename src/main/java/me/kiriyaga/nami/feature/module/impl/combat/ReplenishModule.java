package me.kiriyaga.nami.feature.module.impl.combat;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PostTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.player.LocalPlayer;

import java.util.HashMap;
import java.util.Map;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class ReplenishModule extends Module {

    private final IntSetting percentage = addSetting(new IntSetting("Percentage", 20, 10, 50));
    private final BoolSetting inScreen = addSetting(new BoolSetting("InScreen", false));

    private final Map<Integer, Integer> hotbarTicks = new HashMap<>();
    private final Map<Integer, Item> lastHotbarItems = new HashMap<>();

    public ReplenishModule() {
        super("Replenish", "Automatically refills items in hotbar.", ModuleCategory.of("Combat"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTick(PostTickEvent event) {
        if (MC.world == null || MC.player == null) return;
        if (!inScreen.get() && MC.currentScreen != null) return;
        LocalPlayer player = MC.player;
        ItemStack cursor = player.currentScreenHandler.getCursorStack();

        if (!cursor.isEmpty()) return;

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack stack = player.getInventory().getStack(hotbarSlot);
            Item currentItem = stack.isEmpty() ? null : stack.getItem();

            if (lastHotbarItems.getOrDefault(hotbarSlot, null) != currentItem) {
                hotbarTicks.put(hotbarSlot, 0);
                lastHotbarItems.put(hotbarSlot, currentItem);
            } else {
                hotbarTicks.put(hotbarSlot, hotbarTicks.getOrDefault(hotbarSlot, 0) + 1);
            }

            if (hotbarTicks.getOrDefault(hotbarSlot, 0) < 10) continue;

            if (stack.isEmpty()) continue;

            int maxCount = stack.getMaxCount();
            int minCount = Math.max(1, (int) (maxCount * (percentage.get() / 100f)));

            if (stack.getCount() < minCount) {
                int invSlot = findInventorySlotToReplenish(stack);
                if (invSlot != -1) {
                    swap(hotbarSlot, invSlot);
                    return;
                }
            }
        }
    }

    private int findInventorySlotToReplenish(ItemStack target) {
        LocalPlayer player = MC.player;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (!ItemStack.areItemsAndComponentsEqual(stack, target)) continue; // this is component not nbt, but anyway it works the same since components are just wrapper for nbt
            return i;
        }
        return -1;
    }

    private void swap(int hotbarSlot, int invSlot) {
        int realInvSlot = invSlot;
        if (invSlot < 9) {
            realInvSlot += 36;
        }

        int realHotbarSlot = hotbarSlot + 36;

        //boolean inventoryOpen = MC.currentScreen instanceof InventoryScreen || MC.currentScreen instanceof HudEditorScreen || MC.currentScreen instanceof ClickGuiScreen;

        MC.interactionManager.clickSlot(MC.player.playerScreenHandler.syncId, realInvSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, MC.player);
        MC.interactionManager.clickSlot(MC.player.playerScreenHandler.syncId, realHotbarSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, MC.player);
        MC.interactionManager.clickSlot(MC.player.playerScreenHandler.syncId, realInvSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, MC.player);
    }

}