package me.kiriyaga.nami.feature.module.impl.client;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.Render3DEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.IntSetting;
import me.kiriyaga.nami.util.entity.EntityUtils;
import me.kiriyaga.nami.util.PredictMovementUtils;
import me.kiriyaga.nami.util.render.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class PredictTestModule extends Module {

    public final IntSetting ticks = addSetting(new IntSetting("Ticks", 3, 1, 20));
    public final BoolSetting predictSelf = addSetting(new BoolSetting("Self", true));
    public final BoolSetting predictOthers = addSetting(new BoolSetting("Others", true));
    public final BoolSetting showBox = addSetting(new BoolSetting("ShowBox", true));
    public final BoolSetting showEye = addSetting(new BoolSetting("ShowEye", true));

    public PredictTestModule() {
        super("PredictTest", ".", ModuleCategory.of("Client"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender3D(Render3DEvent event) {
        if (MC.world == null || MC.player == null) return;

        PoseStack matrices = event.getMatrices();

        if (predictSelf.get())
            renderPredictionForEntity(MC.player, matrices);

        if (predictOthers.get()) {
            List<Player> others = EntityUtils.getOtherPlayers();
            for (Player other : others) {
                if (other.isRemoved()) continue;
                renderPredictionForEntity(other, matrices);
            }
        }
    }

    private void renderPredictionForEntity(Entity entity, PoseStack matrices) {
        PredictMovementUtils.PredictedEntity initial = new PredictMovementUtils.PredictedEntity(
                entity.getEntityPos(),
                entity.getVelocity(),
                entity.getYaw(),
                entity.getPitch(),
                entity.isOnGround(),
                entity.getStandingEyeHeight()
        );

        PredictMovementUtils.PredictedEntity predicted = PredictMovementUtils.predict(initial, ticks.get(), t -> Vec3.ZERO);

        if (predicted == null) return;

        if (showBox.get()) {
            AABB box = entity.getBoundingBox().offset(predicted.pos.subtract(entity.getEntityPos()));
            RenderUtil.drawBoxLines(box, new Color(0, 255, 0, 200), true, true, 1.5f);

        }

        if (showEye.get()) {
            Vec3 eye = predicted.getEyePos();
            double size = 0.1;
            AABB eyeBox = new AABB(eye.x - size, eye.y - size, eye.z - size, eye.x + size, eye.y + size, eye.z + size);
            RenderUtil.drawBoxLines(eyeBox, new Color(255, 0, 0, 255), true, true, 1.5f);
        }
    }
}
