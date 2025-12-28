package me.kiriyaga.nami.feature.module.impl.movement;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.*;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.feature.setting.impl.*;
import me.kiriyaga.nami.mixin.*;

import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.network.protocol.Packet;


import java.util.*;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.entity.PlayerUtils.isPhased;

@RegisterModule
public class VelocityModule extends Module {

    private enum Mode { VANILLA, WALLS, GRIM }

    private final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.WALLS));
    private final DoubleSetting horizontalPercent = addSetting(new DoubleSetting("Horizontal", 100.0, 0.0, 100.0));
    private final DoubleSetting verticalPercent = addSetting(new DoubleSetting("Vertical", 100.0, 0.0, 100.0));
    private final BoolSetting handleKnockback = addSetting(new BoolSetting("Knockback", true));
    private final BoolSetting handleExplosions = addSetting(new BoolSetting("Explosion", true));
    private final BoolSetting concealMotion = addSetting(new BoolSetting("Conceal", false));
    private final BoolSetting requireGround = addSetting(new BoolSetting("GroundOnly", false));
    private final BoolSetting cancelEntityPush = addSetting(new BoolSetting("EntityPush", true));
    private final BoolSetting cancelBlockPush = addSetting(new BoolSetting("BlockPush", true));
    private final BoolSetting cancelLiquidPush = addSetting(new BoolSetting("LiquidPush", true));
    private final BoolSetting cancelFishHook = addSetting(new BoolSetting("RodPush", false));

    private boolean pendingConcealment = false;
    private boolean pendingVelocity = false;

    public VelocityModule() {super("Velocity", "Reduces incoming velocity effects.", ModuleCategory.of("Movement"), "antiknockback");}

    @Override
    public void onEnable() {
        pendingVelocity = false;
    }

    @Override
    public void onDisable() {
        flushPendingVelocity();
        pendingConcealment = false;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTick(PreTickEvent event) {
        flushPendingVelocity();
        pendingConcealment = false;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (MC.player == null || MC.world == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundPlayerPositionPacket && concealMotion.get()) {
            pendingConcealment = true;
        }

        if (packet instanceof ClientboundSetEntityMotionPacket vel && handleKnockback.get()) {
            handleVelocityPacket(event, vel);
        } else if (packet instanceof ClientboundExplodePacket explosion && handleExplosions.get()) {
            handleExplosionPacket(event, explosion);
        } else if (packet instanceof ClientboundBundlePacket bundle) {
            handleBundlePacket(event, bundle);
        } else if (packet instanceof ClientboundEntityEventPacket status
                && status.getStatus() == EntityEvent.FISHING_ROD_REEL_IN
                && cancelFishHook.get()) {
            handleFishHookPacket(event, status);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onEntityPush(EntityPushEvent event) {
        if (cancelEntityPush.get() && event.getTarget().equals(MC.player)) {
            event.cancel();
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onBlockPush(BlockPushEvent event) {
        if (cancelBlockPush.get()) {
            event.cancel();
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onFluidPush(LiquidPushEvent event) {
        if (cancelLiquidPush.get()) {
            event.cancel();
        }
    }

    private void handleVelocityPacket(PacketReceiveEvent event, ClientboundSetEntityMotionPacket packet) {
        if (packet.getEntityId() != MC.player.getId()) return;

        if (pendingConcealment && isZeroVelocity(packet)) {
            pendingConcealment = false;
            return;
        }

        switch (mode.get()) {
            case VANILLA -> processVelocityVanilla(event, packet);
            case WALLS -> processVelocityWalls(event, packet);
            case GRIM -> processVelocityGrim(event);
        }
    }

    private void handleExplosionPacket(PacketReceiveEvent event, ClientboundExplodePacket packet) {
        switch (mode.get()) {
            case VANILLA -> processExplosionVanilla(event, packet);
            case WALLS -> processExplosionWalls(event, packet);
            case GRIM -> processExplosionGrim(event);
        }
    }

    private void handleBundlePacket(PacketReceiveEvent event, ClientboundBundlePacket bundle) {
        List<Packet<?>> filtered = new ArrayList<>();

        for (Packet<?> packet : bundle.getPackets()) {
            if (packet instanceof ClientboundExplodePacket exp && handleExplosions.get()) {
                processBundleExplosion(filtered, exp);
            } else if (packet instanceof ClientboundSetEntityMotionPacket vel && handleKnockback.get()) {
                processBundleVelocity(filtered, vel, event);
            } else {
                filtered.add(packet);
            }
        }

        ((BundlePacketAccessor) bundle).setIterable(filtered);
    }

    private void handleFishHookPacket(PacketReceiveEvent event, ClientboundEntityEventPacket status) {
        Entity entity = status.getEntity(MC.world);
        if (entity instanceof FishingHook hook && hook.getHookedEntity() == MC.player) {
            event.cancel();
        }
    }

    private void processVelocityVanilla(PacketReceiveEvent event, ClientboundSetEntityMotionPacket packet) {
        if (isNoVelocityConfigured()) {
            event.cancel();
        } else {

            event.packet = scaleVelocityPacket(packet);
        }
    }

    private void processVelocityWalls(PacketReceiveEvent event, ClientboundSetEntityMotionPacket packet) {
        if (!isPhased(MC.player) || (requireGround.get() && !MC.player.isOnGround())) return;
        processVelocityVanilla(event, packet);
    }

    private void processVelocityGrim(PacketReceiveEvent event) {
        if (!SERVER_MANAGER.hasElapsedSinceSetback(100)) return;
        event.cancel();
        pendingVelocity = true;
    }

    private void processExplosionVanilla(PacketReceiveEvent event, ClientboundExplodePacket packet) {
        if (isNoVelocityConfigured()) {
            event.cancel();
        } else {
            scaleExplosionPacket(packet);
        }
    }

    private void processExplosionWalls(PacketReceiveEvent event, ClientboundExplodePacket packet) {
        if (!isPhased(MC.player)) return;
        processExplosionVanilla(event, packet);
    }

    private void processExplosionGrim(PacketReceiveEvent event) {
        if (!SERVER_MANAGER.hasElapsedSinceSetback(100)) return;
        event.cancel();
        pendingVelocity = true;
    }

    private void processBundleExplosion(List<Packet<?>> filtered, ClientboundExplodePacket packet) {
        switch (mode.get()) {
            case VANILLA -> {
                if (!isNoVelocityConfigured()) scaleExplosionPacket(packet);
                else return;
            }
            case WALLS -> {
                if (!isPhased(MC.player)) { filtered.add(packet); return; }
                if (!isNoVelocityConfigured()) scaleExplosionPacket(packet);
                else return;
            }
            case GRIM -> {
                if (!SERVER_MANAGER.hasElapsedSinceSetback(100)) { filtered.add(packet); return; }
                pendingVelocity = true;
                return;
            }
        }
        filtered.add(packet);
    }

    private void processBundleVelocity(List<Packet<?>> filtered, ClientboundSetEntityMotionPacket packet, PacketReceiveEvent event) {
        if (packet.getEntityId() != MC.player.getId()) {
            filtered.add(packet);
            return;
        }

        switch (mode.get()) {
            case VANILLA -> {
                if (!isNoVelocityConfigured()) event.packet = scaleVelocityPacket(packet);
                else return;
            }
            case WALLS -> {
                if (!isPhased(MC.player) || (requireGround.get() && !MC.player.isOnGround())) {
                    filtered.add(packet);
                    return;
                }
                if (!isNoVelocityConfigured()) event.packet = scaleVelocityPacket(packet);
                else return;
            }
            case GRIM -> {
                if (!SERVER_MANAGER.hasElapsedSinceSetback(100)) { filtered.add(packet); return; }
                pendingVelocity = true;
                return;
            }
        }

        filtered.add(packet);
    }

    private void flushPendingVelocity() {
        if (!pendingVelocity) return;
        if (mode.get() == Mode.GRIM) {
            sendRotationFix();
        }
        pendingVelocity = false;
    }

    private void sendRotationFix() { // somehow it happens, needs tests on grim v2 asap
        float yaw = ROTATION_MANAGER.getStateHandler().getServerYaw();
        float pitch = ROTATION_MANAGER.getStateHandler().getServerPitch();

        ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(this.name, 0, yaw, pitch, RotationModule.RotationMode.SILENT));
        MC.getNetworkHandler().sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                MC.player.isCrawling() ? MC.player.getBlockPos() : MC.player.getBlockPos().up(),
                Direction.DOWN
        ));
    }

    private boolean isZeroVelocity(ClientboundSetEntityMotionPacket packet) {
        return packet.getVelocity().x == 0 && packet.getVelocity().y == 0 && packet.getVelocity().z == 0;
    }

    private boolean isNoVelocityConfigured() {
        return horizontalPercent.get() == 0 && verticalPercent.get() == 0;
    }

    private ClientboundSetEntityMotionPacket scaleVelocityPacket(ClientboundSetEntityMotionPacket packet) {
        Vec3d v = packet.getVelocity();

        Vec3d scaled = new Vec3d(
                v.x * (horizontalPercent.get() / 100.0),
                v.y * (verticalPercent.get() / 100.0),
                v.z * (horizontalPercent.get() / 100.0)
        );

        ClientboundSetEntityMotionPacket newPacket = EntityVelocityUpdateS2CPacketAccessor.create(packet.getEntityId(), scaled);
        return newPacket;
    }


    private void scaleExplosionPacket(ClientboundExplodePacket packet) {
        ExplosionS2CPacketAccessor accessor = (ExplosionS2CPacketAccessor) (Object) packet;
        accessor.getPlayerKnockback().ifPresent(original -> {
            Vec3d scaled = new Vec3d(
                    original.x * (horizontalPercent.get() / 100.0),
                    original.y * (verticalPercent.get() / 100.0),
                    original.z * (horizontalPercent.get() / 100.0)
            );
            accessor.setPlayerKnockback(Optional.of(scaled));
        });
    }
}