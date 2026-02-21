package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.util.InteractionUtils.interactWithEntity;

@RegisterFeature
public class AutoNametagFeature extends Feature {

    public final BoolSetting nametagged = addSetting(new BoolSetting("Nametagged", false));
    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 3.0, 1.0, 10.0));
    public final IntSetting delay = addSetting(new IntSetting("Delay", 1, 0, 20));
    public final BoolSetting swapSilent = addSetting(new BoolSetting("SwapSilent", true));
    public final BoolSetting multitask = addSetting(new BoolSetting("Multitask", false));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));

    private int cooldown = 0;

    public AutoNametagFeature() {
        super("AutoNametag", "Automatically renames nearby entities with nametags.", FeatureCategory.of("World"), "nametag", "autoname", "autonametag");
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity == null || entity == MC.player) continue;
            if (entity.getCustomName() != null && !nametagged.get()) continue;
            if (entity instanceof Villager || entity instanceof ThrownEnderpearl || entity instanceof EnderDragon) continue;


            if (interactWithEntity(entity, Items.NAME_TAG, swapSilent.get(), multitask.get(), range.get(), swing.get(), rotate.get(), this.name)) {
                cooldown = delay.get();
                break;
            }
        }
    }
}