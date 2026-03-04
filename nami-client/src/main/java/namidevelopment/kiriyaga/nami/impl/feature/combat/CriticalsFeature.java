package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.nami.mixininterface.IPlayerInteractEntityC2SPacket;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.atomic.AtomicBoolean;

import static namidevelopment.kiriyaga.api.NamiApi.INPUT_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.ROTATION_SERVICE;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.entity.PlayerUtils.isPhased;

@RegisterFeature
public class CriticalsFeature extends Feature {

    public enum Mode { PACKET, GRIM, GRIMNEW}

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.PACKET));
    public final BoolSetting onlyPhased = addSetting(new BoolSetting("OnlyPhased", true));
    public final BoolSetting onlyStandingStill = addSetting(new BoolSetting("OnlyStandingStill", true));
    public final BoolSetting onlyWhenHeadCovered = addSetting(new BoolSetting("HeadCovered", true));

    public CriticalsFeature() {
        super("Criticals", "Changes player movement for always critting.", FeatureCategory.of("Combat"));
        onlyPhased.setShowCondition(() -> mode.get() != Mode.PACKET);
        onlyStandingStill.setShowCondition(() -> mode.get() != Mode.PACKET);
        onlyWhenHeadCovered.setShowCondition(() -> mode.get() != Mode.PACKET);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPacket() instanceof IPlayerInteractEntityC2SPacket packet))
            return;
        if (packet.getType() != ServerboundInteractPacket.ActionType.ATTACK)
            return;

        if (MC.player == null || MC.level == null || MC.player.isHandsBusy() || MC.player.isFallFlying() || MC.player.isInWater() || MC.player.isInLava() || MC.player.isSuppressingSlidingDownLadder() || MC.player.hasEffect(MobEffects.BLINDNESS)) {
            return;
        }

        Entity target = packet.getEntity();

        if (!(target instanceof LivingEntity living) || !living.isAlive() || target instanceof EndCrystal || target instanceof ItemFrame) return;

        if (MC.player.isHandsBusy()) {
            ridingAttack(target);
            return;
        }

        onGroundAttack();
    }

    private void ridingAttack(Entity target) {
        if (mode.get() == Mode.PACKET) {
            for (int i = 0; i < 5; i++) {
                MC.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, MC.player.isShiftKeyDown()));
                MC.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
    }

    private void onGroundAttack() {
        double x = MC.player.getX();
        double y = MC.player.getY();
        double z = MC.player.getZ();

        switch (mode.get()) {
            case PACKET -> packetCrit(x, y, z);
            case GRIM -> grimCrit(x, y, z);
            case GRIMNEW -> grimNewCrit(x, y,z);
        }
    }

    private void packetCrit(double x, double y, double z) {
        if (!MC.player.onGround())
            return;

        MC.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, false));
        MC.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
    }

    private void grimCrit(double x, double y, double z) {
        if (!MC.player.onGround())
            return;

        if (onlyPhased.get() && !isPhased(MC.player))
            return;

        if (onlyStandingStill.get() && MC.player.getDeltaMovement().x > 0.01 || MC.player.getDeltaMovement().y > 0.01)
            return;

        if (onlyWhenHeadCovered.get()) {
            BlockPos pos = MC.player.blockPosition();
            BlockPos target = pos.above(2);

            if (MC.player.isVisuallyCrawling()) {
                target = pos.above(1);
            }

            if (MC.level.getBlockState(target).isAir())
                return;
        }

        float yaw = ROTATION_SERVICE.getStateHandler().getServerYRot();
        float pitch = ROTATION_SERVICE.getStateHandler().getServerXRot();

        MC.getConnection().send(new ServerboundMovePlayerPacket.PosRot(x, y + 0.0625, z, yaw, pitch, false, false));
        MC.getConnection().send(new ServerboundMovePlayerPacket.PosRot(x, y + 0.0625013579, z, yaw, pitch, false, false));
        MC.getConnection().send(new ServerboundMovePlayerPacket.PosRot(x, y + 1.3579e-6, z, yaw, pitch, false,false));
    }

    private void grimNewCrit(double x, double y, double z) {
        if (!MC.player.onGround())
            return;

        if (onlyPhased.get() && (!isPhased(MC.player) || !eyesPhased(MC.player)))
            return;

        if (onlyStandingStill.get() && MC.player.getDeltaMovement().x > 0.01 || MC.player.getDeltaMovement().y > 0.01)
            return;

        if (onlyWhenHeadCovered.get()) {
            BlockPos pos = MC.player.blockPosition();
            BlockPos target = pos.above(2);

            if (MC.player.isVisuallyCrawling()) {
                target = pos.above(1);
            }

            if (MC.level.getBlockState(target).isAir())
                return;
        }

        float yaw = ROTATION_SERVICE.getStateHandler().getServerYRot();
        float pitch = ROTATION_SERVICE.getStateHandler().getServerXRot();

        float f = (float)((Math.random() * 2.0 - 1.0) * 0.001f);
        float f2 = Mth.clamp(pitch + f, -90.0F, 90.0F);

        // Author: cattyngmd
        MC.getConnection().send(new ServerboundMovePlayerPacket.PosRot(x, y + .0626, z, yaw, f2, false, false));
        MC.getConnection().send(new ServerboundMovePlayerPacket.PosRot(x, y + .0455, z, yaw, f2, false, false));
    }

    private boolean eyesPhased(Player player) {
        Vec3 eyePos = player.getEyePosition();
        BlockPos pos = BlockPos.containing(eyePos);
        BlockState state = MC.level.getBlockState(pos);
        if (state.isAir()) return false;
        VoxelShape shape = state.getCollisionShape(MC.level, pos);
        if (shape.isEmpty()) return false;

        for (AABB box : shape.toAabbs()) {
            if (box.move(pos).contains(eyePos)) {
                return true;
            }
        }
        return false;
    }

}