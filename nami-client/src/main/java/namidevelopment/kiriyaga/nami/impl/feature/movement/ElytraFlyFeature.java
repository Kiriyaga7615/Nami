package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.GlidingEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.nami.mixin.DuckKeyMapping;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.Timer;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class ElytraFlyFeature extends Feature {

    public enum FlyMode {
        BOUNCE, ROTATION, GLIDE
    }

    public final EnumSetting<FlyMode> mode = addSetting(new EnumSetting<>("Mode", FlyMode.BOUNCE));

    // GLIDE
    public final IntSetting targetY = addSetting(new IntSetting("TargetY", 180, 60, 600));
    public final IntSetting vLow = addSetting(new IntSetting("MinSpeed", 14, 6, 40));
    public final IntSetting vHigh = addSetting(new IntSetting("MaxSpeed", 27, 10, 60));
    public final IntSetting climbPitch = addSetting(new IntSetting("ClimbPitch", 40, 0, 60));
    public final IntSetting divePitch = addSetting(new IntSetting("DivePitch", 38, 20, 60));
    public final IntSetting cruiseMin = addSetting(new IntSetting("CruiseMin", 4, 0, 20));
    public final IntSetting cruiseMax = addSetting(new IntSetting("CruiseMax", 12, 2, 25));
    public final BoolSetting allowRockets = addSetting(new BoolSetting("AllowRockets", true));
    @SuppressWarnings("FieldCanBeLocal")
    public final IntSetting rocketSpeed = addSetting(new IntSetting("RocketBelow", 9, 0, 30));
    public final BoolSetting setbackStop = addSetting(new BoolSetting("SetbackStop", true));

    //
    // CONTROL
    //public final BoolSetting midAirFreeze = addSetting(new BoolSetting("mid air freeze", false));
    public final BoolSetting lockPitch = addSetting(new BoolSetting("LockPitch", true));
    public final BoolSetting hover = addSetting(new BoolSetting("Hover", true));
    public final IntSetting hoverSpeed = addSetting(new IntSetting("HoverSpeed", 10, 2, 20)); // тики 2-20

    // BOOST
    //public final BoolSetting boost = addSetting(new BoolSetting("Boost", false));
    //public final BoolSetting newBoost = addSetting(new BoolSetting("NewBoost", false));
    public final BoolSetting pitch = addSetting(new BoolSetting("Pitch", true));
    public final IntSetting pitchDegree = addSetting(new IntSetting("Pitch", 75, 0, 90));

    private enum GlideState { DIVE, CRUISE, CLIMB }
    private GlideState glideState = GlideState.CRUISE;
    private double speed = 0;
    private double[] speedSamples = new double[25];
    private int speedSampleIndex = 0;
    private boolean speedBufferFilled = false;
    private double baseY = 0;
    private double lastX = 0;
    private double lastZ = 0;
    private double cruisePhase = 0;
    private long rocket = 0;
    private boolean climbingToTarget = false;

    private final Timer hoverTimer = new Timer();
    private boolean hoverB = true;

    public ElytraFlyFeature() {
        super("ElytraFly", "Improves elytra flying.", FeatureCategory.of("Movement"), "elytrafly");
        //boost.setShowCondition(() -> mode.get() == FlyMode.BOUNCE);
        //newBoost.setShowCondition(() -> mode.get() == FlyMode.BOUNCE);
        pitch.setShowCondition(() -> mode.get() == FlyMode.BOUNCE);
        pitchDegree.setShowCondition(() -> mode.get() == FlyMode.BOUNCE && pitch.get());
        lockPitch.setShowCondition(() -> mode.get() == FlyMode.ROTATION);
        hover.setShowCondition(() -> mode.get() == FlyMode.ROTATION);
        hoverSpeed.setShowCondition(() -> mode.get() == FlyMode.ROTATION && hover.get());
        vLow.setShowCondition(() -> mode.get() == FlyMode.GLIDE);
        vHigh.setShowCondition(() -> mode.get() == FlyMode.GLIDE);
        climbPitch.setShowCondition(() -> mode.get() == FlyMode.GLIDE);
        divePitch.setShowCondition(() -> mode.get() == FlyMode.GLIDE);
        cruiseMin.setShowCondition(() -> mode.get() == FlyMode.GLIDE);
        cruiseMax.setShowCondition(() -> mode.get() == FlyMode.GLIDE);
        allowRockets.setShowCondition(() -> mode.get() == FlyMode.GLIDE);
        rocketSpeed.setShowCondition(() -> mode.get() == FlyMode.GLIDE && allowRockets.get());
        targetY.setShowCondition(() -> mode.get() == FlyMode.GLIDE);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (MC.player != null) {
            baseY = MC.player.getY();
            glideState = GlideState.CLIMB;
            cruisePhase = 0;
        }
    }

    @Override
    public void onDisable() {
        if (mode.get() == FlyMode.BOUNCE) setJumpHeld(false);
        baseY = 0;
    }

/*    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onMove(MoveEvent event) {
        if (MC.player == null || mode.get() != FlyMode.BOUNCE) return;

        if (!MC.player.isOnGround()) return;

        if (MC.player.isSprinting() && speed > 12.00 && newBoost.get()) {

            Vec3d velocity = MC.player.getVelocity();
            event.setMovement(new Vec3d(velocity.x, 0, velocity.z));
        }
    }*/

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onGliding(GlidingEvent event) {
        if (MC.player == null)
            return;

        if (MC.player.getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA)
            return;

        if (setbackStop.get() && !SERVER_SERVICE.hasElapsedSinceSetback(5000))
            return;

        if (mode.get() == FlyMode.BOUNCE)
            event.cancel();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onPreTick(PreTickEvent event) {
        if (MC.player == null)
            return;

        this.clearDisplayInfo();

        if (MC.player.getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA)
            return;

        if (setbackStop.get() && !SERVER_SERVICE.hasElapsedSinceSetback(5000))
            return;

        this.addDisplayInfo(mode.get().toString());

        if (mode.get() == FlyMode.BOUNCE) {
            setJumpHeld(true);
/*
            if (boost.get()) {
                MC.player.setVelocity(MC.player.getVelocity().x, 0.0, MC.player.getVelocity().z);
            }*/

            if (pitch.get())
                ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(this.getName(), 1, MC.player.getYRot(), pitchDegree.get().floatValue(), RotationsFeature.RotationMode.MOTION));

            MC.player.connection.send(
                    new ServerboundPlayerCommandPacket(MC.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING)
            );
        } else
        if (mode.get() == FlyMode.ROTATION) {
            if (!MC.player.isFallFlying()) return;
            Vec3 dir = getControlDirection();
            if (dir != null) {
                float finalYRot;
                float finalXRot;

                if (Math.abs(dir.y) > 0.5) {
                    finalYRot = MC.player.getYRot();
                    finalXRot = dir.y > 0 ? -90f : 90f;
                } else {
                    finalYRot = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90f;
                    finalXRot = MC.player.getXRot();
                    if (lockPitch.get()) finalXRot = -3f;
                }

                ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(this.getName(), 1, finalYRot, finalXRot, RotationsFeature.RotationMode.MOTION));
            } else if (hover.get()) {
                int hoverMs = hoverSpeed.get() * 50;
                if (hoverTimer.hasElapsed(hoverMs)) {
                    hoverB = !hoverB;
                    hoverTimer.reset();
                }

                float targetYaw = MC.player.getYRot() + (hoverB ? 0f : 180f);
                float targetPitch = -3f;

                ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(this.getName(), 1, targetYaw, targetPitch, RotationsFeature.RotationMode.MOTION));
            }
        } else if (mode.get() == FlyMode.GLIDE) {
            if (!MC.player.isFallFlying())
                return;

            double playerY = MC.player.getY();
            double target = targetY.get();
            long currentTime = System.currentTimeMillis();

            if (!climbingToTarget && playerY < target - 80) {
                climbingToTarget = true;
            }

            GlideState effectiveState;

            if (climbingToTarget) {
                effectiveState = GlideState.CLIMB;

                if (allowRockets.get() && currentTime - rocket >= 3500) {
                    useItemAnywhere(Items.FIREWORK_ROCKET);
                    rocket = currentTime;
                }

                if (playerY >= target) {
                    climbingToTarget = false;
                }
            } else {
                final double v = speed;
                final int vLowVal = vLow.get();
                final int vHighVal = Math.max(vHigh.get(), vLowVal + 2);

                switch (glideState) {
                    case DIVE:
                        if (v >= vHighVal) glideState = GlideState.CRUISE;
                        break;
                    case CLIMB:
                        if (v <= vLowVal) glideState = GlideState.DIVE;
                        break;
                    case CRUISE:
                        if (v <= vLowVal - 1) glideState = GlideState.DIVE;
                        else if (v >= vHighVal + 2) glideState = GlideState.CLIMB;
                        break;
                }
                effectiveState = glideState;
            }

            float targetPitch;
            if (effectiveState == GlideState.DIVE) {
                targetPitch = clampPitch(+divePitch.get());
            } else if (effectiveState == GlideState.CLIMB) {
                targetPitch = clampPitch(-climbPitch.get());
            } else {
                targetPitch = cruisePitch();
            }

            float currentPitch = ROTATION_SERVICE.getStateHandler().getRotationXRot();
            float smoothPitch = approach(currentPitch, targetPitch, 10);

            //TODO yaw smooth n
            ROTATION_SERVICE.getRequestHandler().submit(
                    new RotationRequest(this.getName(), 1, MC.player.getYRot(), smoothPitch, RotationsFeature.RotationMode.MOTION)
            );
        }
    }


    private float cruisePitch() {
        double dt = 1.0 / 20.0;
        double period = 2.6;
        cruisePhase += dt / period;
        if (cruisePhase > 1.0) cruisePhase -= 1.0;
        double tri = 2.0 * Math.abs(2.0 * (cruisePhase - Math.floor(cruisePhase + 0.5))) - 1.0;
        tri = Math.copySign(tri * tri, tri);
        double min = cruiseMin.get();
        double max = Math.max(cruiseMax.get(), cruiseMin.get() + 1);
        double ampPitch = min + (max - min) * (0.5 * (tri + 1.0));
        return clampPitch((float) -ampPitch);
    }


    private float approach(float current, float target, float maxDelta) {
        float delta = Mth.clamp(target - current, -maxDelta, +maxDelta);
        return current + delta;
    }


    private float clampPitch(float pitchDeg) {
        return Mth.clamp(pitchDeg, -89f, 89f);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    private void onPreTick2(PreTickEvent event) {
        if (MC.player == null) return;

        double dx = MC.player.getX() - lastX;
        double dz = MC.player.getZ() - lastZ;

        double instantSpeed = Math.sqrt(dx * dx + dz * dz) * 20;

        speedSamples[speedSampleIndex] = instantSpeed;
        speedSampleIndex = (speedSampleIndex + 1) % speedSamples.length;

        if (speedSampleIndex == 0) speedBufferFilled = true;

        int count = speedBufferFilled ? speedSamples.length : speedSampleIndex;
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += speedSamples[i];
        }

        speed = count > 0 ? sum / count : 0;

        lastX = MC.player.getX();
        lastZ = MC.player.getZ();
    }

    private void setJumpHeld(boolean held) {
        KeyMapping jumpKey = MC.options.keyJump;
        InputConstants.Key boundKey = ((DuckKeyMapping) jumpKey).getKey();
        int keyCode = boundKey.getValue();
        boolean physicallyPressed = InputConstants.isKeyDown(MC.getWindow(), keyCode);
        jumpKey.setDown(physicallyPressed || held);
    }

    private Vec3 getControlDirection() {
        boolean forward = InputConstants.isKeyDown(MC.getWindow(), ((DuckKeyMapping) MC.options.keyUp).getKey().getValue());
        boolean back    = InputConstants.isKeyDown(MC.getWindow(), ((DuckKeyMapping) MC.options.keyDown).getKey().getValue());
        boolean left    = InputConstants.isKeyDown(MC.getWindow(), ((DuckKeyMapping) MC.options.keyLeft).getKey().getValue());
        boolean right   = InputConstants.isKeyDown(MC.getWindow(), ((DuckKeyMapping) MC.options.keyRight).getKey().getValue());
        boolean up      = InputConstants.isKeyDown(MC.getWindow(), ((DuckKeyMapping) MC.options.keyJump).getKey().getValue());
        boolean down    = InputConstants.isKeyDown(MC.getWindow(), ((DuckKeyMapping) MC.options.keyShift).getKey().getValue());

        if (!(forward || back || left || right || up || down)) return null;

        if (up && !down) {
            return new Vec3(0, 1, 0);
        } else if (down && !up) {
            return new Vec3(0, -1, 0);
        }

        double forwardVal = (forward ? 1.0 : 0.0) - (back ? 1.0 : 0.0);
        double strafeVal  = (right ? 1.0 : 0.0) - (left ? 1.0 : 0.0);

        if (forwardVal == 0.0 && strafeVal == 0.0) return null;

        double lx = strafeVal;
        double lz = forwardVal;

        double yawRad = Math.toRadians(MC.player.getYRot());
        double fx = -Math.sin(yawRad);
        double fz =  Math.cos(yawRad);
        double rx = -Math.sin(yawRad + Math.PI / 2.0);
        double rz =  Math.cos(yawRad + Math.PI / 2.0);

        double wx = fx * lz + rx * lx;
        double wz = fz * lz + rz * lx;

        Vec3 worldDir = new Vec3(wx, 0.0, wz);
        if (worldDir.lengthSqr() == 0.0) return null;
        return worldDir.normalize();
    }

    private boolean useItemAnywhere(Item item) {
        int hotbarSlot = getSlotInHotbar(item);

        if (hotbarSlot != -1) {
            INVENTORY_SERVICE.getSwapHandler().attemptSwitch(hotbarSlot, true);
            MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
            return true;
        }

        int invSlot = getSlotInInventory(item);
        if (invSlot != -1) {
            int selectedHotbarIndex = MC.player.getInventory().getSelectedSlot();
            int containerInvSlot = convertSlot(invSlot);

            INVENTORY_SERVICE.getClickHandler().swapSlot(containerInvSlot, selectedHotbarIndex);
            MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
            INVENTORY_SERVICE.getClickHandler().swapSlot(containerInvSlot, selectedHotbarIndex);
            return true;
        }

        return false;
    }

    private int getSlotInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }

    private int getSlotInInventory(Item item) {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }

    private int convertSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }
}
