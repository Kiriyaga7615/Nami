package namidevelopment.kiriyaga.nami.impl.feature.miscellaneous;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static namidevelopment.kiriyaga.api.util.RotationUtils.getXRotToVec;
import static namidevelopment.kiriyaga.api.util.RotationUtils.getYRotToVec;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.entity.HostileUtils.isAggressiveNow;

@RegisterFeature
public class AgroFeature extends Feature {

    public enum Mode {
        ENDERMAN,
        CREAKING
    }

    public final EnumSetting<Mode> modeSetting = new EnumSetting<>("Mode", Mode.ENDERMAN);

    public AgroFeature() {
        super("Agro", "Automatically looks at certain mobs.", FeatureCategory.of("Miscellaneous"));
        addSetting(modeSetting);
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;
        if (MC.player.isCreative() || MC.player.isFallFlying()) return;

        ItemStack helmet = MC.player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() == Items.CARVED_PUMPKIN) return;

        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.ALL)) {
            if (entity == MC.player) continue;

            if (modeSetting.get() == Mode.ENDERMAN && !(entity instanceof EnderMan && !isAggressiveNow(entity))) continue;
            if (modeSetting.get() == Mode.CREAKING && !(entity instanceof Creaking creak && creak.isActive())) continue;

            double distance = MC.player.distanceToSqr(entity);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
            }
        }

        if (closest != null) {
            Vec3 eyes = closest.getEyePosition();

            ROTATION_SERVICE.getRequestHandler().submit(
                    new RotationRequest(
                            AgroFeature.class.getName(),
                            2,
                            (float) getYRotToVec(MC.player, eyes),
                            (float) getXRotToVec(MC.player, eyes),
                            RotationsFeature.RotationMode.MOTION
                    )
            );
        }
    }

    public EnumSetting<Mode> getModeSetting() {
        return modeSetting;
    }
}
