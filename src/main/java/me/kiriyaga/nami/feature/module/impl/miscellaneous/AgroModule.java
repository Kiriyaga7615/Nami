package me.kiriyaga.nami.feature.module.impl.miscellaneous;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import me.kiriyaga.nami.util.entity.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.references.Items;
import net.minecraft.world.phys.Vec3;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.RotationUtils.*;
import static me.kiriyaga.nami.util.entity.HostileUtils.isAggressiveNow;

@RegisterModule
public class AgroModule extends Module {

    public enum Mode {
        ENDERMAN,
        CREAKING
    }

    private final EnumSetting<Mode> modeSetting = new EnumSetting<>("Mode", Mode.ENDERMAN);

    public AgroModule() {
        super("AutoAgro", "Automatically looks at certain mobs.", ModuleCategory.of("Miscellaneous"));
        addSetting(modeSetting);
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;
        if (MC.player.isCreative() || MC.player.isGliding()) return;

        ItemStack helmet = MC.player.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.getItem() == Items.CARVED_PUMPKIN) return;

        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.ALL)) {
            if (entity == MC.player) continue;

            if (modeSetting.get() == Mode.ENDERMAN && !(entity instanceof EnderMan && !isAggressiveNow(entity))) continue;
            if (modeSetting.get() == Mode.CREAKING && !(entity instanceof Creaking creak && creak.isActive())) continue;

            double distance = MC.player.squaredDistanceTo(entity);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
            }
        }

        if (closest != null) {
            Vec3 eyes = closest.getEyePos();

            ROTATION_MANAGER.getRequestHandler().submit(
                    new RotationRequest(
                            AgroModule.class.getName(),
                            2,
                            (float) getYawToVec(MC.player, eyes),
                            (float) getPitchToVec(MC.player, eyes),
                            RotationModule.RotationMode.MOTION
                    )
            );
        }
    }

    public EnumSetting<Mode> getModeSetting() {
        return modeSetting;
    }
}
