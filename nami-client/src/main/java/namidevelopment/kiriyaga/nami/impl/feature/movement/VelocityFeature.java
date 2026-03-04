package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.*;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;

import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.nami.mixin.DuckBundlePacket;
import namidevelopment.kiriyaga.nami.mixin.DuckClientboundExplodePacket;
import namidevelopment.kiriyaga.nami.mixininterface.IClientboundSetEntityMotionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.network.protocol.Packet;

import net.minecraft.world.phys.Vec3;

import java.util.*;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.entity.PlayerUtils.isPhased;

@RegisterFeature
public class VelocityFeature extends Feature {

    private enum Mode { VANILLA, WALLS, GRIM }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.WALLS));
    public final DoubleSetting horizontalPercent = addSetting(new DoubleSetting("Horizontal", 0.00, 0.00, 100.00));
    public final DoubleSetting verticalPercent = addSetting(new DoubleSetting("Vertical", 0.00, 0.00, 100.00));
    public final BoolSetting concealMotion = addSetting(new BoolSetting("Conceal", false));
    public final BoolSetting cancel = addSetting(new BoolSetting("Cancel", false));
    public final BoolSetting onlyOnGround = addSetting(new BoolSetting("OnlyOnGround", false));
    public final BoolSetting entityPush = addSetting(new BoolSetting("Entity", true));
    public final BoolSetting blockPush = addSetting(new BoolSetting("Block", true));
    public final BoolSetting liquidPush = addSetting(new BoolSetting("Liquid", true));
    public final BoolSetting fishingRod = addSetting(new BoolSetting("FishingRod", false));
    public final BoolSetting onlyPhased = addSetting(new BoolSetting("OnlyPhased", false));
    public final BoolSetting onlyWhenHeadCovered = addSetting(new BoolSetting("OnlyCoveredHead", false));

    private boolean pendingConcealment = false;
    private boolean pendingVelocity = false;

    public VelocityFeature() {super("Velocity", "Reduces incoming velocity effects.", FeatureCategory.of("Movement"), "antiknockback");
        horizontalPercent.setShowCondition(()-> !cancel.get());
        verticalPercent.setShowCondition(()-> !cancel.get());
        onlyPhased.setShowCondition(()-> mode.get() == Mode.GRIM);
        onlyWhenHeadCovered.setShowCondition(()-> mode.get() == Mode.GRIM);
    }

    @Override
    public void onEnable() {
        pendingVelocity = false;
    }

    @Override
    public void onDisable() {
        if (!pendingVelocity) return;
        if (mode.get() == Mode.GRIM) {
            sendRotationFix();
        }
        pendingVelocity = false;
        pendingConcealment = false;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTick(PreTickEvent event) {
        if (!pendingVelocity) return;
        if (mode.get() == Mode.GRIM) {
            sendRotationFix();
        }
        pendingVelocity = false;
        pendingConcealment = false;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (MC.player == null || MC.level == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundPlayerPositionPacket && concealMotion.get()) {
            pendingConcealment = true;
        }

        if (packet instanceof ClientboundSetEntityMotionPacket vel) {
            handleVelocityPacket(event, vel);
        } else if (packet instanceof ClientboundExplodePacket explosion) {
            handleExplosionPacket(event, explosion);
        } else if (packet instanceof ClientboundBundlePacket bundle) {
            handleBundlePacket(event, bundle);
        } else if (packet instanceof ClientboundEntityEventPacket status
                && status.getEventId() == EntityEvent.FISHING_ROD_REEL_IN
                && fishingRod.get()) {
            handleFishHookPacket(event, status);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onEntityPush(EntityPushEvent event) {
        if (entityPush.get() && event.getTarget().equals(MC.player)) {
            event.cancel();
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onBlockPush(BlockPushEvent event) {
        if (blockPush.get()) {
            event.cancel();
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onFluidPush(LiquidPushEvent event) {
        if (liquidPush.get()) {
            event.cancel();
        }
    }

    private void handleVelocityPacket(PacketReceiveEvent event, ClientboundSetEntityMotionPacket packet) {
        if (packet.getId() != MC.player.getId()) return;

        if (pendingConcealment && packet.getMovement().x == 0 && packet.getMovement().y == 0 && packet.getMovement().z == 0) {
            pendingConcealment = false;
            return;
        }

        switch (mode.get()) {
            case VANILLA -> processVelocityVanilla(event, packet);
            case WALLS -> processVelocityWalls(event, packet);
            case GRIM -> processVelocityGrim(event, packet);
        }
    }

    private void handleExplosionPacket(PacketReceiveEvent event, ClientboundExplodePacket packet) {
        switch (mode.get()) {
            case VANILLA -> processExplosionVanilla(event, packet);
            case WALLS -> processExplosionWalls(event, packet);
            case GRIM -> processExplosionGrim(event, packet);
        }
    }

    private void handleBundlePacket(PacketReceiveEvent event, ClientboundBundlePacket bundle) {
        List<Packet<?>> filtered = new ArrayList<>();

        for (Packet<?> packet : bundle.subPackets()) {
            if (packet instanceof ClientboundExplodePacket exp) {
                processBundleExplosion(filtered, exp, event);
            } else if (packet instanceof ClientboundSetEntityMotionPacket vel) {
                processBundleVelocity(filtered, vel, event);
            } else {
                filtered.add(packet);
            }
        }

        ((DuckBundlePacket) bundle).setIterable(filtered);
    }

    private void handleFishHookPacket(PacketReceiveEvent event, ClientboundEntityEventPacket status) {
        Entity entity = status.getEntity(MC.level);
        if (entity instanceof FishingHook hook && hook.getHookedIn() == MC.player) {
            event.cancel();
        }
    }

    private void processVelocityVanilla(PacketReceiveEvent event, ClientboundSetEntityMotionPacket packet) {
        if(cancel.get()) {
            event.cancel();
            return;
        }

        scaleVelocityPacket(packet);
    }

    private void processVelocityWalls(PacketReceiveEvent event, ClientboundSetEntityMotionPacket packet) {
        if (!isPhased(MC.player) || (onlyOnGround.get() && !MC.player.onGround()))
            return;

        if(cancel.get()) {
            event.cancel();
            return;
        }

        processVelocityVanilla(event, packet);
    }

    private void processVelocityGrim(PacketReceiveEvent event, ClientboundSetEntityMotionPacket packet) {
        if (!SERVER_SERVICE.hasElapsedSinceSetback(100))
            return;
        if (onlyPhased.get() && !isPhased(MC.player))
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

        processVelocityVanilla(event, packet);
        pendingVelocity = true;
    }

    private void processExplosionVanilla(PacketReceiveEvent event, ClientboundExplodePacket packet) {
        if(cancel.get()) {
            event.cancel();
            return;
        }

        scaleExplosionPacket(packet);
    }

    private void processExplosionWalls(PacketReceiveEvent event, ClientboundExplodePacket packet) {
        if (!isPhased(MC.player))
            return;
        if(cancel.get()) {
            event.cancel();
            return;
        }

        processExplosionVanilla(event, packet);
    }

    private void processExplosionGrim(PacketReceiveEvent event, ClientboundExplodePacket packet) {
        if (!SERVER_SERVICE.hasElapsedSinceSetback(100))
            return;
        if (onlyPhased.get() && !isPhased(MC.player))
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
        processExplosionVanilla(event, packet);
        pendingVelocity = true;
    }

    private void processBundleExplosion(List<Packet<?>> filtered, ClientboundExplodePacket packet, PacketReceiveEvent event) {
        switch (mode.get()) {
            case VANILLA -> {
                if(cancel.get()) {
                    event.cancel();
                    return;
                }
                 scaleExplosionPacket(packet);
            }
            case WALLS -> {
                if (!isPhased(MC.player)) {

                    filtered.add(packet);
                    return;
                }

                if(cancel.get()) {
                    event.cancel();
                    return;
                }
                scaleExplosionPacket(packet);
            }
            case GRIM -> {
                if (!SERVER_SERVICE.hasElapsedSinceSetback(100)) {
                    filtered.add(packet);
                    return;
                }

                if (onlyPhased.get() && !isPhased(MC.player)) {
                    filtered.add(packet);
                    return;
                }

                if (onlyWhenHeadCovered.get()) {
                    BlockPos pos = MC.player.blockPosition();
                    BlockPos target = pos.above(2);

                    if (MC.player.isVisuallyCrawling()) {
                        target = pos.above(1);
                    }

                    if (MC.level.getBlockState(target).isAir()) {
                        filtered.add(packet);
                        return;
                    }
                }

                if(cancel.get()) {
                    event.cancel();
                    return;
                }

                pendingVelocity = true;
                return;
            }
        }
        filtered.add(packet);
    }

    private void processBundleVelocity(List<Packet<?>> filtered, ClientboundSetEntityMotionPacket packet, PacketReceiveEvent event) {
        if (packet.getId() != MC.player.getId()) {
            filtered.add(packet);
            return;
        }

        switch (mode.get()) {
            case VANILLA -> {
                if(cancel.get()) {
                    event.cancel();
                    return;
                }

                scaleVelocityPacket(packet);
            }
            case WALLS -> {
                if (!isPhased(MC.player) || (onlyOnGround.get() && !MC.player.onGround())) {
                    filtered.add(packet);
                    return;
                }

                if(cancel.get()) {
                    event.cancel();
                    return;
                }

                scaleVelocityPacket(packet);
            }
            case GRIM -> {
                if (!SERVER_SERVICE.hasElapsedSinceSetback(100)) {
                    filtered.add(packet);
                    return;
                }

                if (onlyPhased.get() && !isPhased(MC.player)) {
                    filtered.add(packet);
                    return;
                }

                if (onlyWhenHeadCovered.get()) {
                    BlockPos pos = MC.player.blockPosition();
                    BlockPos target = pos.above(2);

                    if (MC.player.isVisuallyCrawling()) {
                        target = pos.above(1);
                    }

                    if (MC.level.getBlockState(target).isAir()) {
                        filtered.add(packet);
                        return;
                    }
                }

                pendingVelocity = true;
                return;
            }
        }
        filtered.add(packet);
    }

    private void sendRotationFix() { // somehow it happens, needs tests on grim v2 asap
        float yaw = ROTATION_SERVICE.getStateHandler().getServerYRot();
        float pitch = ROTATION_SERVICE.getStateHandler().getServerXRot();

        float f = (float)((Math.random() * 2.0 - 1.0) * 0.001f);
        float f2 = Mth.clamp(pitch + f, -90.0F, 90.0F);

        MC.getConnection().send(new ServerboundMovePlayerPacket.PosRot(MC.player.getX(), MC.player.getY(), MC.player.getZ(), yaw, f2, MC.player.onGround(), MC.player.horizontalCollision));

       // ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(this.name, 0, yaw, pitch, RotationsFeature.RotationMode.SILENT));
    }

    private void scaleVelocityPacket(ClientboundSetEntityMotionPacket packet) {
        Vec3 v = packet.getMovement();

        Vec3 scaled = new Vec3(
                v.x * (horizontalPercent.get() / 100.0),
                v.y * (verticalPercent.get() / 100.0),
                v.z * (horizontalPercent.get() / 100.0)
        );

        ((IClientboundSetEntityMotionPacket) packet).setMovement(scaled);
    }

    private void scaleExplosionPacket(ClientboundExplodePacket packet) {
        DuckClientboundExplodePacket accessor = (DuckClientboundExplodePacket) (Object) packet;
        accessor.getPlayerKnockback().ifPresent(original -> {
            Vec3 scaled = new Vec3(
                    original.x * (horizontalPercent.get() / 100.0),
                    original.y * (verticalPercent.get() / 100.0),
                    original.z * (horizontalPercent.get() / 100.0)
            );
            accessor.setPlayerKnockback(Optional.of(scaled));
        });
    }
}