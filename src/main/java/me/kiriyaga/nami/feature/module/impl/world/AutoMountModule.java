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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;


import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.InteractionUtils.interactWithEntity;
import static me.kiriyaga.nami.util.RotationUtils.*;

@RegisterModule
public class AutoMountModule extends Module {

    private final DoubleSetting range = addSetting(new DoubleSetting("Range", 2, 1.0, 10.0));
    private final IntSetting delay = addSetting(new IntSetting("Delay", 10, 1, 20));
    private final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    private final BoolSetting rotate = addSetting(new BoolSetting("Rotate", false));

    private int actionCooldown = 0;

    public AutoMountModule() {
        super("AutoMount", "Automatically mounts nearby entities.", ModuleCategory.of("World"), "mount", "automount");
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;

        if (MC.player.hasVehicle()) return;

        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }

        for (Entity entity : MC.world.getEntities()) {
            if (entity == null || entity == MC.player || !entity.isAlive() || entity.hasPassengers()) continue;

            if (!(entity instanceof Horse || entity instanceof Pig || entity instanceof Strider ||
                    entity instanceof Llama || entity instanceof Donkey ||
                    entity instanceof Boat || entity instanceof Minecart)) continue;

            interactWithEntity(entity, range.get(), swing.get(), rotate.get(), this.name);

            actionCooldown = delay.get();
            break;
        }
    }
}
