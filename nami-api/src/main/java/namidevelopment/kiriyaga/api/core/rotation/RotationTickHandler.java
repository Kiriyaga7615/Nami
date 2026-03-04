package namidevelopment.kiriyaga.api.core.rotation;

import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.RotationsFeatureConfig;
import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.RotationUtils.*;


public class RotationTickHandler {

    private final RotationStateHandler stateHandler;
    private final RotationRequestHandler requestHandler;

    private float currentYRotSpeed = 0f, currentXRotSpeed = 0f;
    private int ticksHolding = 0;
    private boolean returning = false;

    public RotationTickHandler(RotationStateHandler stateHandler, RotationRequestHandler requestHandler) {
        this.stateHandler = stateHandler;
        this.requestHandler = requestHandler;
    }

    public void init() {
        EVENT_SERVICE.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null) return;
        RotationsFeatureConfig rotationsFeatureConfig = FeatureContractService.get(RotationsFeatureConfig.class);

        stateHandler.updateRealRotation(MC.player.getYRot(), MC.player.getXRot());

        RotationRequest active = requestHandler.getActiveRequest();
        if (rotationsFeatureConfig.getRotationMode() == RotationsFeatureConfig.RotationMode.SILENT && stateHandler.getSilentSyncRequired()) {
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

        if (rotationsFeatureConfig.isMoveFixEnabled() && stateHandler.isRotating())
            fixMovementForSpoof();
        else
            INPUT_SERVICE.getClientHandler().clearOverride("movefix");
    }

    private void fixMovementForSpoof() {
        if (MC.player == null) return;

        float realYRot = MC.player.getYRot();
        float spoofYRot = stateHandler.getRotationYRot();
        float delta = Mth.wrapDegrees(realYRot - spoofYRot);

        // theese are tick thread and render thread
        boolean forward = INPUT_SERVICE.getInputCache().forward();
        boolean back = INPUT_SERVICE.getInputCache().back();
        boolean left = INPUT_SERVICE.getInputCache().left();
        boolean right = INPUT_SERVICE.getInputCache().right();

        float inputX = (right ? 1 : 0) - (left ? 1 : 0);
        float inputZ = (forward ? 1 : 0) - (back ? 1 : 0);

        INPUT_SERVICE.getClientHandler().overrideMovement("movefix",false, false, false, false);

        if (inputX == 0 && inputZ == 0) return;

        double moveAngle = Math.toDegrees(Math.atan2(inputX, inputZ));
        double finalAngle = moveAngle + delta;
        int sector = (int) Math.round(finalAngle / 45.0) & 7;

        // i hate myself its 02:28
        boolean f = false, b = false, l = false, r = false;

        switch (sector & 7) {
            case 0 -> {
                f = true;
            }
            case 1 -> {
                f = true;
                r = true;
            }
            case 2 -> {
                r = true;
            }
            case 3 -> {
                b = true;
                r = true;
            }
            case 4 -> {
                b = true;
            }
            case 5 -> {
                b = true;
                l = true;
            }
            case 6 -> {
                l = true;
            }
            case 7 -> {
                f = true;
                l = true;
            }
        }
        INPUT_SERVICE.getClientHandler().overrideMovement("movefix", f, b, l, r);
    }

    private void processRequest(RotationRequest request) {
        RotationsFeatureConfig rotationsFeatureConfig = FeatureContractService.get(RotationsFeatureConfig.class);

        if (!request.id.equals(requestHandler.getLastActiveId())) {
            resetRotationToReal();
            requestHandler.setLastActiveId(request.id);
        }

        boolean updated = false;
        if (request.shouldUpdate()) {
            float oldYRot = request.targetYRot;
            float oldXRot  = request.targetXRot;
            request.updateTarget();
            updated = Math.abs(oldYRot - request.targetYRot) > 0.001f || Math.abs(oldXRot - request.targetXRot) > 0.001f;
            if (updated) ticksHolding = 0;
        }

        float yRotDiff = yRotDifference(request.targetYRot, stateHandler.getRotationYRot());
        float xRotDiff = request.targetXRot - stateHandler.getRotationXRot();

        boolean reached = Math.abs(yRotDiff) <= rotationsFeatureConfig.getRotationThreshold() && Math.abs(xRotDiff) <= rotationsFeatureConfig.getRotationThreshold();

        if (reached && !updated) {
            if (++ticksHolding >= rotationsFeatureConfig.getHoldTicks()) {
                requestHandler.removeActiveRequest();
                ticksHolding = 0;
                returning = true;
            }
        } else {
            ticksHolding = 0;
            interpolateRotation(yRotDiff, xRotDiff);
        }
    }

    // in resetRotationToReal() returnToRealRotation() we need to set player YRot, clamped to closest to rotation YRot
    // we need to do this between rotation requests change (highest priority appeared when old one not finished)
    // and when rotation is ended
    // this is made to prevent YRot jump
    private void resetRotationToReal() {
        float targetYRot = alignYRot(stateHandler.getRealYRot(), stateHandler.getRotationYRot());
        stateHandler.updateRealRotation(targetYRot, stateHandler.getRealXRot());
        MC.player.setYRot(targetYRot);
        stateHandler.setRotationYRot(stateHandler.getRealYRot());
        stateHandler.setRotationXRot(stateHandler.getRealXRot());
        currentYRotSpeed = 0f;
        currentXRotSpeed = 0f;
        ticksHolding = 0;
        returning = false;
    }

    private void returnToRealRotation() {
        RotationsFeatureConfig rotationsFeatureConfig = FeatureContractService.get(RotationsFeatureConfig.class);
        float targetYRot = alignYRot(stateHandler.getRealYRot(), stateHandler.getRotationYRot());
        float yRotDiff = targetYRot - stateHandler.getRotationYRot();
        float xRotDiff = stateHandler.getRealXRot() - stateHandler.getRotationXRot();

        interpolateRotation(yRotDiff, xRotDiff);

        boolean backReached = Math.abs(yRotDiff) <= rotationsFeatureConfig.getRotationThreshold() && Math.abs(xRotDiff) <= rotationsFeatureConfig.getRotationThreshold();
        if (backReached) {
            returning = false;
            stateHandler.updateRealRotation(targetYRot, stateHandler.getRealXRot());
            MC.player.setYRot(targetYRot);
            stateHandler.setRotationYRot(stateHandler.getRealYRot());
            stateHandler.setRotationXRot(stateHandler.getRealXRot());
            requestHandler.clearLastActiveId();

/*            if (Feature_SERVICE.getStorage().getByClass(RotationFeature.class).rotation.get() == RotationFeature.RotationMode.MOTION) {
                SprintFeature sm = Feature_SERVICE.getStorage().getByClass(SprintFeature.class);

                if (sm.twobtwot.get())
                    sm.stopSprinting(2);
            }*/
        }
    }

    private void interpolateRotation(float yRotdiff, float xRotDiff) {
        RotationsFeatureConfig rotationsFeatureConfig = FeatureContractService.get(RotationsFeatureConfig.class);

        currentYRotSpeed = lerp(currentYRotSpeed, yRotdiff, (float) rotationsFeatureConfig.getRotationEase());
        currentXRotSpeed = lerp(currentXRotSpeed, xRotDiff, (float) rotationsFeatureConfig.getRotationEase());

        float yRotSpeed = (float) Mth.clamp(currentYRotSpeed, -rotationsFeatureConfig.getRotationSpeed(), rotationsFeatureConfig.getRotationSpeed());
        float xRotSpeed = (float) Mth.clamp(currentXRotSpeed, -rotationsFeatureConfig.getRotationSpeed(), rotationsFeatureConfig.getRotationSpeed());

        float newYRot = stateHandler.getRotationYRot() + yRotSpeed;
        float newXRot = stateHandler.getRotationXRot() + xRotSpeed;

        if (rotationsFeatureConfig.getJitterMode() == RotationsFeatureConfig.JitterMode.NORMAL) {
            float minJitter = (float) (rotationsFeatureConfig.getRotationThreshold() / 4f);
            float maxJitter = (float) (rotationsFeatureConfig.getRotationThreshold() / 2);
            float jitterYRot = minJitter + (float) (Math.random() * (maxJitter - minJitter));
            float jitterXRot = minJitter + (float) (Math.random() * (maxJitter - minJitter));
            jitterYRot *= Math.random() < 0.5 ? -1 : 1;
            jitterXRot *= Math.random() < 0.5 ? -1 : 1;

            newYRot += jitterYRot;
            newXRot += jitterXRot;

            newXRot = Mth.clamp(newXRot, -90f, 90f);
        } else if (rotationsFeatureConfig.getJitterMode() == RotationsFeatureConfig.JitterMode.GRIM) {
            float f = (float)((Math.random() * 2.0 - 1.0) * 0.001f);
            newXRot = Mth.clamp(newXRot + f, -90.0F, 90.0F);
        }

//        RotationSERVICEFeature Feature = Feature_SERVICE.getStorage().getByClass(RotationSERVICEFeature.class);
//        if (Feature.mouseDeltaFix.get()) { // https://github.com/GrimAnticheat/Grim/blob/57a9f8f432800382d43c28df9e8409b4d7d80813/common/src/main/java/ac/grim/grimac/checks/impl/aim/AimModulo360.java#L31
//            float lastServerYaw = stateHandler.getServerYaw();
//            float lastServerDeltaYaw = stateHandler.getServerDeltaYaw();
//
//            float rawDiff = yawDifference(newYaw, lastServerYaw);
//
//            Feature_SERVICE.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                    "before wrap: newYaw=" + newYaw +
//                            ", lastServerYaw=" + lastServerYaw +
//                            ", lastServerDeltaYaw=" + lastServerDeltaYaw +
//                            ", rawDiff=" + rawDiff
//            ));
//
//            while (rawDiff > 360f) rawDiff -= 360f;
//            while (rawDiff < -360f) rawDiff += 360f;
//
//            Feature_SERVICE.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                    "After wrap: rawDiff=" + rawDiff
//            ));
//
//            if (Math.abs(rawDiff) > 320f && Math.abs(lastServerDeltaYaw) < 30f) {
//                newYaw = lastServerYaw + Math.copySign(319f, rawDiff);
//                Feature_SERVICE.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                        "clipping applied: newYaw before=" + newYaw +
//                                ", newYaw after=" + newYaw +
//                                ", clipSign=" + Math.copySign(1f, rawDiff)
//                ));
//            }
//
//
//            Feature_SERVICE.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                    "Final newYaw=" + newYaw
//            ));
//        }

        stateHandler.setRotationYRot(newYRot);
        stateHandler.setRotationXRot(newXRot);
    }

    private float lerpAngle(float start, float end, float factor) {
        float delta = wrapDegrees(end - start);
        return start + delta * factor;
    }

    private void idleReset() {
        requestHandler.clearLastActiveId();
        stateHandler.setRotationYRot(stateHandler.getRealYRot());
        stateHandler.setRotationXRot(stateHandler.getRealXRot());
        currentYRotSpeed = 0f;
        currentXRotSpeed = 0f;
    }

    private void performSilent(RotationRequest req) {
        float targetYRot = MC.player.getYRot();
        float targetXRot = MC.player.getXRot();
        // AimModulo360 seems fixable here but due to race condition it fucks a little bit screen, maybe ill fix it someday but now we just left it with flag
//        ROTATION_SERVICE.getStateHandler().setRotationYaw(targetYaw);
//        ROTATION_SERVICE.getStateHandler().setRotationPitch(targetPitch);
        ROTATION_SERVICE.getStateHandler().setServerYRot(targetYRot);
        ROTATION_SERVICE.getStateHandler().setServerXRot(targetXRot);
//
//        returning = false;
//        stateHandler.updateRealRotation(targetYaw, stateHandler.getRealPitch());
//        MC.player.setYaw(targetYaw);
//        requestHandler.clearLastActiveId();
//        requestHandler.removeActiveRequest();

        MC.getConnection().send(new ServerboundMovePlayerPacket.PosRot(MC.player.getX(), MC.player.getY(), MC.player.getZ(), targetYRot, targetXRot, MC.player.onGround(), true));
    }

    private float lerp(float from, float to, float factor) {
        return from + (to - from) * factor;
    }

    public boolean isRotating() {
        return requestHandler.getActiveRequest() != null || returning;
    }
}
