package me.kiriyaga.nami.feature.module.impl.world;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import 	net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.InteractionUtils.interactWithEntity;
import static me.kiriyaga.nami.util.RotationUtils.*;

@RegisterModule
public class AutoFrameModule extends Module {

    private final DoubleSetting range = addSetting(new DoubleSetting("Range", 4, 1.0, 6.0));
    private final IntSetting delay = addSetting(new IntSetting("Delay", 10, 0, 20));
    private final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    private final BoolSetting rotate = addSetting(new BoolSetting("Rotate", false));

    private int cooldown = 0;


    public AutoFrameModule() {
        super("AutoFrame", "Automatically puts a map in nearby item frames.", ModuleCategory.of("World"), "autoframe");
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        for (Entity entity : MC.world.getEntities()) {
            if (!(entity instanceof ItemFrame frame))
                continue;

            if (frame.getHeldItemStack() != null)
                continue;

            int mapSlot = getMapSlot();
            if (mapSlot == -1)
                continue;

            int currentSlot = MC.player.getInventory().getSelectedSlot();
            if (currentSlot != mapSlot) {
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(mapSlot);
                cooldown = delay.get();
                return;
            }

            interactWithEntity(frame, range.get(), swing.get(), rotate.get(), this.name);

            cooldown = delay.get();
            break;
        }
    }

    private int getMapSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MapItem) {
                return i;
            }
        }
        return -1;
    }
}