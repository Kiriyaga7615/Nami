package me.kiriyaga.nami.core.rotation;

import net.minecraft.util.Mth;

import static me.kiriyaga.nami.Nami.ROTATION_MANAGER;
import static me.kiriyaga.nami.util.RotationUtils.wrapDegrees;

public class RotationStateHandler {
    /**
     * Real degrees
     * we use it for returning
     * Also we change real yaw, mc player yaw, if rotations was very long, since client side yaw pitch is always normalized
     */
    private float realYaw, realPitch;
    /**
     * Current rotation degrees
     * If not rotating, they are the same as realDegree
     * If rotating, theese are used for spoofing degrees
     * They are not accurate, used only for rotation manager
     */
    private float rotationYaw, rotationPitch;
    /**
     * Server degrees
     * These are degrees that really got sended on a server
     * Any mc client should use mc.player.setYaw/pitch in packet send, so we, and also everyone else, know each other rotations
     * Theese can be used for checking entities in raycast (pearl check entity for example) since theese are 100% accurate, sended data
     */
    private float serverYaw, serverPitch;
    /**
     * Previus server yaw delta
     */
    private float serverDeltaYaw;

    /**
     * Is it required to restore silent rotation yaw pitch on latest pre tick
     */
    private boolean silentSyncRequired;

    public void updateRealRotation(float yaw, float pitch) {
        realYaw = yaw;
        realPitch = Mth.clamp(pitch, -90f, 90f);
    }

    public float getRealYaw() { return realYaw; }
    public float getRealPitch() { return realPitch; }

    public float getRotationYaw() { return rotationYaw; }
    public float getRotationPitch() { return rotationPitch; }

    public void setRotationYaw(float yaw) {this.rotationYaw = yaw;}

    public void setRotationPitch(float pitch) {this.rotationPitch = Mth.clamp(pitch, -90f, 90f);}

    // WE DO NOT WRAP/NORMALIZE SERVER ROTATIONS!!!
    public float getServerYaw() {return serverYaw;}
    public void setServerYaw(float yaw) {this.serverYaw = yaw;}

    public float getServerPitch() {return serverPitch;}
    public void setServerPitch(float pitch) {this.serverPitch = pitch;}

    public float getServerDeltaYaw() {return serverDeltaYaw;}
    public void setServerDeltaYaw(float deltaYaw) {this.serverDeltaYaw = deltaYaw;}

    public boolean getSilentSyncRequired() { return silentSyncRequired; }
    public void setSilentSyncRequired(boolean silentSyncRequired) {this.silentSyncRequired = silentSyncRequired;}

    public boolean isRotating() {
        return ROTATION_MANAGER.getTickHandler().isRotating();
    }
}
