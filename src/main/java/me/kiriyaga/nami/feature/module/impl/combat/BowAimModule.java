package me.kiriyaga.nami.feature.module.impl.combat;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.event.impl.Render3DEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.client.ColorModule;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.util.entity.TargetUtils;
import me.kiriyaga.nami.util.render.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.*;
import java.awt.*;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.RotationUtils.*;

@RegisterModule
public class BowAimModule extends Module {

    public final BoolSetting render = addSetting(new BoolSetting("Render", true));

    private Entity currentTarget = null;

    public BowAimModule() {
        super("BowAim", "Aims at certain targets with bow/trident.", ModuleCategory.of("Combat"), "bowbot", "aimbot", "bowaimbot");
    }

    @Override
    public void onDisable() {
        currentTarget = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.world == null) return;

        ItemStack stack = MC.player.getMainHandStack();

        Entity target = TargetUtils.getTarget();
        if (target == null || !(stack.getItem() instanceof BowItem || stack.getItem() instanceof TridentItem) || !MC.player.isUsingItem()) {
            currentTarget = null;
            return;
        }

        currentTarget = target;
        this.setDisplayInfo(target.getName().getString());

        Vec3 aimPos = getAimPosition(target);
        ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(
                BowAimModule.class.getName(),
                6,
                (float) getYawToVec(MC.player, aimPos),
                (float) getPitchToVec(MC.player, aimPos),
                RotationModule.RotationMode.MOTION
        ));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender3D(Render3DEvent event) {
        if (!render.get() || currentTarget == null) return;

        ColorModule colorModule = MODULE_MANAGER.getStorage().getByClass(ColorModule.class);
        drawBox(currentTarget, colorModule.getStyledGlobalColor(), event.getMatrices(), event.getTickDelta());
    }

    private Vec3 getAimPosition(Entity entity) {
        AABB box = entity.getBoundingBox();
        Vec3 center = getEntityCenter(entity);
        double distance = MC.player.getEyePos().distanceTo(center);

        double heightBoost = box.getLengthY() * 0.75 + distance * 0.03;
        return new Vec3(center.x, box.minY + heightBoost, center.z);
    }

    private void drawBox(Entity entity, Color color, PoseStack matrices, float partialTicks) {
        double interpX = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * partialTicks;
        double interpY = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * partialTicks;
        double interpZ = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * partialTicks;
        AABB box = entity.getBoundingBox().offset(interpX - entity.getX(), interpY - entity.getY(), interpZ - entity.getZ());
        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }
}