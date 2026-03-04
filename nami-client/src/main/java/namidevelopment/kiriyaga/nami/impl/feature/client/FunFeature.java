package namidevelopment.kiriyaga.nami.impl.feature.client;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import namidevelopment.kiriyaga.api.util.PredictMovementUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.nami.Nami.*;

//@RegisterFeature
public class FunFeature extends Feature {

    public final BoolSetting interactDebug = addSetting(new BoolSetting("ShowInteract", false));
    public final IntSetting ticks = addSetting(new IntSetting("PredictTicksTicks", 3, 1, 20));
    public final BoolSetting predictSelf = addSetting(new BoolSetting("PredictSelfSelf", false));
    public final BoolSetting predictOthers = addSetting(new BoolSetting("PredictOthers", false));
    public final BoolSetting showBox = addSetting(new BoolSetting("PredictShowBox", false));
    public final BoolSetting showEye = addSetting(new BoolSetting("PredictShowEye", false));

    public FunFeature() {
        super("Fun", ".", FeatureCategory.of("Client"));
    }

    private final Map<AABB, Integer> interactTargets = new ConcurrentHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPacketSend(PacketSendEvent event) {
        if (!interactDebug.get()) return;
        if (MC.level == null || MC.player == null) return;

        if (event.getPacket() instanceof ServerboundUseItemOnPacket usePacket) {
            BlockHitResult hit = usePacket.getHitResult();
            if (hit == null) return;
            addMarker(hit.getLocation());
        }
    }


    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRender3D(Render3DEvent event) {
        if (MC.level == null || MC.player == null) return;

        PoseStack matrices = event.getMatrices();

        if (interactDebug.get()) {
            Iterator<Map.Entry<AABB, Integer>> it = interactTargets.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<AABB, Integer> entry = it.next();

                AABB box = entry.getKey();
                int renderTicks = entry.getValue();

                RenderUtil.drawBoxLines(
                        box,
                        new Color(0, 200, 255, 255),
                        true,
                        true,
                        1.5f
                );

                renderTicks++;

                if (renderTicks >= 240) {
                    it.remove();
                } else {
                    entry.setValue(renderTicks);
                }
            }
        }

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
                entity.position(),
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot(),
                entity.onGround(),
                entity.getEyeHeight()
        );

        PredictMovementUtils.PredictedEntity predicted = PredictMovementUtils.predict(initial, ticks.get(), t -> Vec3.ZERO);

        if (predicted == null) return;

        if (showBox.get()) {
            AABB box = entity.getBoundingBox().move(predicted.pos.subtract(entity.position()));
            RenderUtil.drawBoxLines(box, new Color(0, 255, 0, 200), true, true, 1.5f);

        }

        if (showEye.get()) {
            Vec3 eye = predicted.getEyePos();
            double size = 0.1;
            AABB eyeBox = new AABB(eye.x - size, eye.y - size, eye.z - size, eye.x + size, eye.y + size, eye.z + size);
            RenderUtil.drawBoxLines(eyeBox, new Color(255, 0, 0, 255), true, true, 1.5f);
        }
    }

    private void addMarker(Vec3 pos) {
        double size = 0.08;
        AABB box = new AABB(
                pos.x - size, pos.y - size, pos.z - size,
                pos.x + size, pos.y + size, pos.z + size
        );
        interactTargets.put(box, 0);
    }
}
