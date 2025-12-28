package me.kiriyaga.nami.core.rotation;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.KeyInputEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.feature.module.impl.movement.GuiMoveModule;
import me.kiriyaga.nami.feature.module.impl.movement.SprintModule;
import me.kiriyaga.nami.feature.module.impl.visuals.FreecamModule;
import me.kiriyaga.nami.util.InputCache;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.RotationUtils.alignYaw;
import static me.kiriyaga.nami.util.RotationUtils.wrapDegrees;
import static me.kiriyaga.nami.util.RotationUtils.yawDifference;

public class RotationTickHandler {

    private final RotationStateHandler stateHandler;
    private final RotationRequestHandler requestHandler;

    private float rotationSpeed;
    private float rotationEaseFactor;
    private float rotationThreshold;
    private int ticksBeforeRelease;
//    private float jitterAmount;
//    private float jitterSpeed;
    private float currentYawSpeed = 0f, currentPitchSpeed = 0f;
    private int ticksHolding = 0;
    private boolean returning = false;
    private int tickCount = 0;

    public RotationTickHandler(RotationStateHandler stateHandler, RotationRequestHandler requestHandler) {
        this.stateHandler = stateHandler;
        this.requestHandler = requestHandler;
    }

    public void init() {
        EVENT_MANAGER.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null) return;

        RotationModule module = MODULE_MANAGER.getStorage().getByClass(RotationModule.class);
        loadSettings(module);
        stateHandler.updateRealRotation(MC.player.getYaw(), MC.player.getPitch());

        RotationRequest active = requestHandler.getActiveRequest();
        if (module.rotation.get() == RotationModule.RotationMode.SILENT && stateHandler.getSilentSyncRequired()) {
            //performSilent(active); // actually this can be skipped if we somehow simulate client rotation packet sending idk
            //stateHandler.setSilentSyncRequired(false);
            //resetRotationToReal();
            requestHandler.clear();
            returning = false;
            return;
        }

        if (active != null) {
            processRequest(active);
        } else if (returning) {
            returnToRealRotation();
        } else {
            idleReset();
        }

        if (module.moveFix.get() && stateHandler.isRotating())
            fixMovementForSpoof();

        tickCount++;
    }

    private void fixMovementForSpoof() {
        if (MC.player == null || INPUT_MANAGER.isFrozen()) return;

        float realYaw = MC.player.getYaw();
        float spoofYaw = stateHandler.getRotationYaw();
        float delta = Mth.wrapDegrees(realYaw - spoofYaw);

        // theese are tick thread and render thread
        boolean forward = INPUT_MANAGER.isForwardPressed();
        boolean back = INPUT_MANAGER.isBackPressed();
        boolean left = INPUT_MANAGER.isLeftPressed();
        boolean right = INPUT_MANAGER.isRightPressed();

        InputCache.update(
                forward,
                back,
                left,
                right
        );

        float inputX = (right ? 1 : 0) - (left ? 1 : 0);
        float inputZ = (forward ? 1 : 0) - (back ? 1 : 0);

        MC.options.forwardKey.setPressed(false);
        MC.options.backKey.setPressed(false);
        MC.options.leftKey.setPressed(false);
        MC.options.rightKey.setPressed(false);

        if (inputX == 0 && inputZ == 0) return;

        double moveAngle = Math.toDegrees(Math.atan2(inputX, inputZ));
        double finalAngle = moveAngle + delta;
        int sector = (int) Math.round(finalAngle / 45.0) & 7;

        // i hate myself its 02:28
        switch (sector) {
            case 0: MC.options.forwardKey.setPressed(true); break;
            case 1: MC.options.forwardKey.setPressed(true); MC.options.rightKey.setPressed(true); break;
            case 2: MC.options.rightKey.setPressed(true); break;
            case 3: MC.options.backKey.setPressed(true); MC.options.rightKey.setPressed(true); break;
            case 4: MC.options.backKey.setPressed(true); break;
            case 5: MC.options.backKey.setPressed(true); MC.options.leftKey.setPressed(true); break;
            case 6: MC.options.leftKey.setPressed(true); break;
            case 7: MC.options.forwardKey.setPressed(true); MC.options.leftKey.setPressed(true); break;
        }
    }

    private void loadSettings(RotationModule module) {
        rotationSpeed = module.rotationSpeed.get().floatValue();
        rotationEaseFactor = module.rotationEaseFactor.get().floatValue();
        rotationThreshold = module.rotationThreshold.get().floatValue();
        ticksBeforeRelease = module.ticksBeforeRelease.get();
    }

    private void processRequest(RotationRequest request) {
        if (!request.id.equals(requestHandler.getLastActiveId())) {
            resetRotationToReal();
            requestHandler.setLastActiveId(request.id);
        }

        boolean updated = false;
        if (request.shouldUpdate()) {
            float oldYaw = request.targetYaw;
            float oldPitch = request.targetPitch;
            request.updateTarget();
            updated = Math.abs(oldYaw - request.targetYaw) > 0.001f || Math.abs(oldPitch - request.targetPitch) > 0.001f;
            if (updated) ticksHolding = 0;
        }

        float yawDiff = yawDifference(request.targetYaw, stateHandler.getRotationYaw());
        float pitchDiff = request.targetPitch - stateHandler.getRotationPitch();

        boolean reached = Math.abs(yawDiff) <= rotationThreshold && Math.abs(pitchDiff) <= rotationThreshold;

        if (reached && !updated) {
            if (++ticksHolding >= ticksBeforeRelease) {
                requestHandler.removeActiveRequest();
                ticksHolding = 0;
                returning = true;
            }
        } else {
            ticksHolding = 0;
            interpolateRotation(yawDiff, pitchDiff);
        }
    }

    // in resetRotationToReal() returnToRealRotation() we need to set player yaw, clamped to closest to rotation yaw
    // we need to do this between rotation requests change (highest priority appeared when old one not finished)
    // and when rotation is ended
    // this is made to prevent yaw jump
    private void resetRotationToReal() {
        float targetYaw = alignYaw(stateHandler.getRealYaw(), stateHandler.getRotationYaw());
        stateHandler.updateRealRotation(targetYaw, stateHandler.getRealPitch());
        MC.player.setYaw(targetYaw);
        stateHandler.setRotationYaw(stateHandler.getRealYaw());
        stateHandler.setRotationPitch(stateHandler.getRealPitch());
        currentYawSpeed = 0f;
        currentPitchSpeed = 0f;
        ticksHolding = 0;
        returning = false;
    }

    private void returnToRealRotation() {
        float targetYaw = alignYaw(stateHandler.getRealYaw(), stateHandler.getRotationYaw());
        float yawDiff = targetYaw - stateHandler.getRotationYaw();
        float pitchDiff = stateHandler.getRealPitch() - stateHandler.getRotationPitch();

        interpolateRotation(yawDiff, pitchDiff);

        boolean backReached = Math.abs(yawDiff) <= rotationThreshold && Math.abs(pitchDiff) <= rotationThreshold;
        if (backReached) {
            returning = false;
            stateHandler.updateRealRotation(targetYaw, stateHandler.getRealPitch());
            MC.player.setYaw(targetYaw);
            stateHandler.setRotationYaw(stateHandler.getRealYaw());
            stateHandler.setRotationPitch(stateHandler.getRealPitch());
            requestHandler.clearLastActiveId();

/*            if (MODULE_MANAGER.getStorage().getByClass(RotationModule.class).rotation.get() == RotationModule.RotationMode.MOTION) {
                SprintModule sm = MODULE_MANAGER.getStorage().getByClass(SprintModule.class);

                if (sm.twobtwot.get())
                    sm.stopSprinting(2);
            }*/
        }
    }

    private void interpolateRotation(float yawDiff, float pitchDiff) {
        currentYawSpeed = lerp(currentYawSpeed, yawDiff, rotationEaseFactor);
        currentPitchSpeed = lerp(currentPitchSpeed, pitchDiff, rotationEaseFactor);

        float yawSpeed = Mth.clamp(currentYawSpeed, -rotationSpeed, rotationSpeed);
        float pitchSpeed = Mth.clamp(currentPitchSpeed, -rotationSpeed, rotationSpeed);

        float newYaw = stateHandler.getRotationYaw() + yawSpeed;
        float newPitch = stateHandler.getRotationPitch() + pitchSpeed;

//        RotationManagerModule module = MODULE_MANAGER.getStorage().getByClass(RotationManagerModule.class);
//        if (module.mouseDeltaFix.get()) { // https://github.com/GrimAnticheat/Grim/blob/57a9f8f432800382d43c28df9e8409b4d7d80813/common/src/main/java/ac/grim/grimac/checks/impl/aim/AimModulo360.java#L31
//            float lastServerYaw = stateHandler.getServerYaw();
//            float lastServerDeltaYaw = stateHandler.getServerDeltaYaw();
//
//            float rawDiff = yawDifference(newYaw, lastServerYaw);
//
//            MODULE_MANAGER.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                    "before wrap: newYaw=" + newYaw +
//                            ", lastServerYaw=" + lastServerYaw +
//                            ", lastServerDeltaYaw=" + lastServerDeltaYaw +
//                            ", rawDiff=" + rawDiff
//            ));
//
//            while (rawDiff > 360f) rawDiff -= 360f;
//            while (rawDiff < -360f) rawDiff += 360f;
//
//            MODULE_MANAGER.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                    "After wrap: rawDiff=" + rawDiff
//            ));
//
//            if (Math.abs(rawDiff) > 320f && Math.abs(lastServerDeltaYaw) < 30f) {
//                newYaw = lastServerYaw + Math.copySign(319f, rawDiff);
//                MODULE_MANAGER.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                        "clipping applied: newYaw before=" + newYaw +
//                                ", newYaw after=" + newYaw +
//                                ", clipSign=" + Math.copySign(1f, rawDiff)
//                ));
//            }
//
//
//            MODULE_MANAGER.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                    "Final newYaw=" + newYaw
//            ));
//        }

        stateHandler.setRotationYaw(newYaw);
        stateHandler.setRotationPitch(newPitch);
    }

    private float lerpAngle(float start, float end, float factor) {
        float delta = wrapDegrees(end - start);
        return start + delta * factor;
    }

    private void idleReset() {
        requestHandler.clearLastActiveId();
        stateHandler.setRotationYaw(stateHandler.getRealYaw());
        stateHandler.setRotationPitch(stateHandler.getRealPitch());
        currentYawSpeed = 0f;
        currentPitchSpeed = 0f;
    }

    private void performSilent(RotationRequest req) {
        float targetYaw = MC.player.getYaw();
        float targetPitch = MC.player.getPitch();
        // AimModulo360 seems fixable here but due to race condition it fucks a little bit screen, maybe ill fix it someday but now we just left it with flag
//        ROTATION_MANAGER.getStateHandler().setRotationYaw(targetYaw);
//        ROTATION_MANAGER.getStateHandler().setRotationPitch(targetPitch);
        ROTATION_MANAGER.getStateHandler().setServerYaw(targetYaw);
        ROTATION_MANAGER.getStateHandler().setServerPitch(targetPitch);
//
//        returning = false;
//        stateHandler.updateRealRotation(targetYaw, stateHandler.getRealPitch());
//        MC.player.setYaw(targetYaw);
//        requestHandler.clearLastActiveId();
//        requestHandler.removeActiveRequest();

        MC.getNetworkHandler().sendPacket(new ServerboundMovePlayerPacket.Full(MC.player.getX(), MC.player.getY(), MC.player.getZ(), targetYaw, targetPitch, MC.player.isOnGround(), true));
    }

    private float lerp(float from, float to, float factor) {
        return from + (to - from) * factor;
    }

    public boolean isRotating() {
        return requestHandler.getActiveRequest() != null || returning;
    }
}
