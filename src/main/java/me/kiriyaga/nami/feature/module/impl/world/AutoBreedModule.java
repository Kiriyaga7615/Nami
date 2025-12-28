package me.kiriyaga.nami.feature.module.impl.world;

import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import me.kiriyaga.nami.util.entity.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.InteractionUtils.interactWithEntity;
import static me.kiriyaga.nami.util.entity.EntityUtils.canBreed;

@RegisterModule
public class AutoBreedModule extends Module {

    private final DoubleSetting range = addSetting(new DoubleSetting("Range", 2, 1.0, 5.0));
    private final IntSetting delay = addSetting(new IntSetting("Delay", 10, 1, 20));
    private final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    private final BoolSetting rotate = addSetting(new BoolSetting("Rotate", false));

    private final Set<Integer> animalsFed = new HashSet<>();
    private int breedCooldown = 0;

    public AutoBreedModule() {
        super("AutoBreed", "Automatically breeds nearby animals.", ModuleCategory.of("World"), "autobreed");
    }

    @Override
    public void onDisable() {
        animalsFed.clear();
        breedCooldown = 0;
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;

        animalsFed.removeIf(id -> {
            Entity e = MC.world.getEntityById(id);
            return e == null
                    || !e.isAlive()
                    || e instanceof Animal an && !canBreed(an);
        });

        if (breedCooldown > 0) {
            breedCooldown--;
            return;
        }

        for (Entity entity : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PASSIVE, 10, true)) {
            if (!(entity instanceof Animal animal)) continue;
            if (animalsFed.contains(animal.getId())) continue;

            if (!canBreed(animal)) continue;

            int foodSlot = getSlot(animal);
            if (foodSlot == -1) continue;

            int currentSlot = MC.player.getInventory().getSelectedSlot();
            if (currentSlot != foodSlot) {
                INVENTORY_MANAGER.getSlotHandler().attemptSwitch(foodSlot);
                breedCooldown = delay.get();
                return;
            }


            interactWithEntity(animal, range.get(), swing.get(), rotate.get(), this.name);

            animalsFed.add(animal.getId());
            breedCooldown = delay.get();
            break;
        }
    }

    private int getSlot(Animal animal) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getStack(i);
            if (!stack.isEmpty() && animal.isBreedingItem(stack)) {
                return i;
            }
        }
        return -1;
    }
}