package me.kiriyaga.nami.feature.module.impl.visuals;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.KeyInputEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import net.minecraft.client.CameraType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import static me.kiriyaga.nami.Nami.MC;
import static me.kiriyaga.nami.Nami.ROTATION_MANAGER;

@RegisterModule
public class FreecamModule extends Module { // todo this shit broke
    private final DoubleSetting speed = addSetting(new DoubleSetting("Speed", 0.5, 0.1, 5.0));
    private final DoubleSetting accelerate = addSetting(new DoubleSetting("Accelerate", 2.3, 1.0, 3.0));
    private final BoolSetting look = addSetting(new BoolSetting("Look", true));

    private double currentFactor = 1.0;
    private long accelStartTime = -1;
    private final double accelDuration = 0.8;
    private CameraType previousPerspective;
    private Vec3 cameraPos;
    public Vec3 pos = Vec3.ZERO;
    public Vec3 prevPos = Vec3.ZERO;
    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private double camX, camY, camZ;

    private boolean forward, back, left, right, up, down;

    public FreecamModule() {
        super("Freecam", "Fly around freely without moving your player.", ModuleCategory.of("Render"), "freecum");
    }

    @Override
    public void onEnable() {
        if (MC.player == null || MC.world == null) {
            toggle();
            return;
        }

        previousPerspective = MC.options.getPerspective();
        MC.options.setPerspective(CameraType.THIRD_PERSON_BACK);

        cameraPos = MC.player.getCameraPosVec(1.0f);
        camX = cameraPos.x;
        camY = cameraPos.y;
        camZ = cameraPos.z;

        yaw = MC.player.getYaw();
        pitch = MC.player.getPitch();
    }

    @Override
    public void onDisable() {
        if (MC.player == null)
            return;
        if (MC.options.getPerspective() != previousPerspective && previousPerspective != null) {
            MC.options.setPerspective(previousPerspective);
        }
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        if (cameraPos == null || MC.player == null || MC.world == null) {
            this.toggle();
            return;
        }

        if (MC.currentScreen instanceof ChatScreen)
            return;

        boolean moving = forward || back || left || right || up || down;

        if (moving) {
            if (accelStartTime < 0) accelStartTime = System.currentTimeMillis();
            double elapsed = (System.currentTimeMillis() - accelStartTime) / 1000.0;
            double t = Mth.clamp(elapsed / accelDuration, 0, 1);
            currentFactor = 1.0 + t * (accelerate.get() - 1.0);
        } else {
            currentFactor = 1.0;
            accelStartTime = -1;
        }

        double spd = speed.get() * currentFactor;

        double dx = 0, dy = 0, dz = 0;
        Vec3 forwardVec = Vec3.fromPolar(0, yaw);
        Vec3 rightVec = Vec3.fromPolar(0, yaw + 90);

        if (forward) { dx += forwardVec.x * spd; dz += forwardVec.z * spd; }
        if (back)    { dx -= forwardVec.x * spd; dz -= forwardVec.z * spd; }
        if (left)    { dx -= rightVec.x * spd; dz -= rightVec.z * spd; }
        if (right)   { dx += rightVec.x * spd; dz += rightVec.z * spd; }
        if (up) dy += spd;
        if (down) dy -= spd;

        if ((forward || back) && (left || right)) {
            dx *= 0.7071;
            dz *= 0.7071;
        }

        prevPos = pos;
        pos = cameraPos;
        cameraPos = cameraPos.add(dx, dy, dz);

        camX = cameraPos.x;
        camY = cameraPos.y;
        camZ = cameraPos.z;

        if (look.get()) {
            var hit = MC.crosshairTarget;
            if (hit != null && hit.getType() != net.minecraft.util.hit.HitResult.Type.MISS) {
                Vec3 target = hit.getPos();
                Vec3 from = MC.player.getEntityPos().add(0, MC.player.getStandingEyeHeight(), 0);

                double diffX = target.x - from.x;
                double diffY = target.y - from.y;
                double diffZ = target.z - from.z;

                double yawToTarget = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90;
                double pitchToTarget = -Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)));

                ROTATION_MANAGER.getRequestHandler().submit(new RotationRequest(FreecamModule.class.getName() ,3, (float)yawToTarget, (float)pitchToTarget, RotationModule.RotationMode.MOTION));
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {

        int forwardKey = InputConstants.fromTranslationKey(MC.options.forwardKey.getBoundKeyTranslationKey()).getCode();
        int backKey    = InputConstants.fromTranslationKey(MC.options.backKey.getBoundKeyTranslationKey()).getCode();
        int leftKey    = InputConstants.fromTranslationKey(MC.options.leftKey.getBoundKeyTranslationKey()).getCode();
        int rightKey   = InputConstants.fromTranslationKey(MC.options.rightKey.getBoundKeyTranslationKey()).getCode();
        int jumpKey    = InputConstants.fromTranslationKey(MC.options.jumpKey.getBoundKeyTranslationKey()).getCode();
        int sneakKey   = InputConstants.fromTranslationKey(MC.options.sneakKey.getBoundKeyTranslationKey()).getCode();


        boolean pressed = event.action != GLFW.GLFW_RELEASE;

        if (event.key == forwardKey) forward = pressed;
        else if (event.key == backKey) back = pressed;
        else if (event.key == leftKey) left = pressed;
        else if (event.key == rightKey) right = pressed;
        else if (event.key == jumpKey) up = pressed;
        else if (event.key == sneakKey) down = pressed;

        if (forward || back || left || right || up || down) {
            event.cancel();
        }
    }



    public double getX() { return camX; }
    public double getY() { return camY; }
    public double getZ() { return camZ; }

    public Vec3 getCameraPos() {
        return cameraPos;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;

        yaw += (float) deltaX;
        pitch += (float) deltaY;

        pitch = Mth.clamp(pitch, -90, 90);
    }
}
