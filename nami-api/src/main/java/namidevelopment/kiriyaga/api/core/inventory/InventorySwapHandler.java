package namidevelopment.kiriyaga.api.core.inventory;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.mixininterface.IClientPlayerInteractionManager;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

import static namidevelopment.kiriyaga.api.NamiApi.EVENT_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

public class InventorySwapHandler {

    public int lastSlot = -1;

    public void init() {
        EVENT_SERVICE.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreTickFirst(PreTickEvent event) {
        if (MC.player == null || MC.level == null || MC.gameMode == null || MC.player.getInventory() == null)
            return;

        lastSlot = MC.player.getInventory().getSelectedSlot();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreTickLast(PreTickEvent event) {
        if (MC.player == null || MC.level == null || MC.gameMode == null || MC.player.getInventory() == null)
            return;

        attemptSwitch(lastSlot);
        lastSlot = -1;
    }

    public void attemptSwitch(int targetSlot, boolean silent) {
        if (MC.player == null || MC.level == null || MC.gameMode == null || targetSlot < 0 || targetSlot > 8)
            return;

        if (!silent) {
            lastSlot = targetSlot;
        }

        MC.player.getInventory().setSelectedSlot(targetSlot);
        syncSelectedSlot();
    }

    private void attemptSwitch(int targetSlot) {
        if (MC.player == null || MC.level == null || MC.gameMode == null || targetSlot < 0 || targetSlot > 8 || MC.player.getInventory().getSelectedSlot() == targetSlot)
            return;

        MC.player.getInventory().setSelectedSlot(targetSlot);
        syncSelectedSlot();
    }

    private void syncSelectedSlot(){
        ((IClientPlayerInteractionManager) MC.gameMode).updateSlot(); // this one is the same as mc default one
    }

    public int findHotbarItem(Predicate<ItemStack> predicate) {
        if (MC.player == null || MC.level == null || MC.gameMode == null)
            return -1;

/*        if (predicate.test(MC.player.getInventory().getSelectedItem()))
            return MC.player.getInventory().getSelectedSlot();*/

        ItemStack selected = MC.player.getInventory().getSelectedItem();
        if (!selected.isEmpty() && predicate.test(selected))
            return MC.player.getInventory().getSelectedSlot();

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = MC.player.getInventory().getItem(slot);
            if (stack.isEmpty())
                continue;

            if (predicate.test(stack))
                return slot;
        }
        return -1;
    }
}
