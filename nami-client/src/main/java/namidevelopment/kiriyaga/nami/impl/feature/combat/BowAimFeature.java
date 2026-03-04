package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.util.entity.TargetUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.ROTATION_SERVICE;
import static namidevelopment.kiriyaga.api.util.RotationUtils.*;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class BowAimFeature extends Feature {

    public final BoolSetting render = addSetting(new BoolSetting("Render", true));

    private Entity currentTarget = null;

    public BowAimFeature() {
        super("BowAim", "Aims at certain targets with bow/trident.", FeatureCategory.of("Combat"), "bowbot", "aimbot", "bowaimbot");
    }

    @Override
    public void onDisable() {
        currentTarget = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;
        this.clearDisplayInfo();

        ItemStack stack = MC.player.getMainHandItem();

        Entity target = TargetUtils.getTarget();
        if (target == null || !(stack.getItem() instanceof BowItem || stack.getItem() instanceof TridentItem) || !MC.player.isUsingItem()) {
            currentTarget = null;
            return;
        }

        currentTarget = target;
        this.addDisplayInfo(target.getName().getString());

        Vec3 aimPos = getAimPosition(target);
        ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(
                BowAimFeature.class.getName(),
                6,
                (float) getYRotToVec(MC.player, aimPos),
                (float) getXRotToVec(MC.player, aimPos),
                RotationsFeature.RotationMode.MOTION
        ));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRender3D(Render3DEvent event) {
        if (!render.get() || currentTarget == null) return;

        ColorFeature colorFeature = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        drawBox(currentTarget, colorFeature.getStyledGlobalColor(), event.getMatrices(), event.getTickDelta());
    }

    private Vec3 getAimPosition(Entity entity) {
        AABB box = entity.getBoundingBox();
        Vec3 center = getEntityCenter(entity);
        double distance = MC.player.getEyePosition().distanceTo(center);

        double heightBoost = box.getYsize() * 0.75 + distance * 0.03;
        return new Vec3(center.x, box.minY + heightBoost, center.z);
    }

    private void drawBox(Entity entity, Color color, PoseStack matrices, float partialTicks) {
        double interpX = entity.xOld + (entity.getX() - entity.xOld) * partialTicks;
        double interpY = entity.yOld + (entity.getY() - entity.yOld) * partialTicks;
        double interpZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks;
        AABB box = entity.getBoundingBox().move(interpX - entity.getX(), interpY - entity.getY(), interpZ - entity.getZ());
        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }
}