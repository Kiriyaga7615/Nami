package namidevelopment.kiriyaga.api.core.rotation;

import net.minecraft.util.Mth;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class RotationStateHandler {
    /**
     * Real degrees
     * we use it for returning
     * Also we change real yaw, mc player yaw, if rotations was very long, since client side yaw pitch is always normalized
     */
    private float realYRot, realXRot;
    /**
     * Current rotation degrees
     * If not rotating, they are the same as realDegree
     * If rotating, theese are used for spoofing degrees
     * They are not accurate, used only for rotation SERVICE
     */
    private float rotationYRot, rotationXRot;
    /**
     * Server degrees
     * These are degrees that really got sended on a server
     * Any mc client should use mc.player.setYaw/pitch in packet send, so we, and also everyone else, know each other rotations
     * Theese can be used for checking entities in raycast (pearl check entity for example) since theese are 100% accurate, sended data
     */
    private float serverYRot, serverXRot;

    /**
     * Last Server degrees
     * These are degrees last we sent on server
     */
    private float serverYRot0, serverXRot0;

    /**
     * Previus server yaw delta
     */
    private float serverDeltaYRot;

    /**
     * Is it required to restore silent rotation yRot xRot on latest pre tick
     */
    private boolean silentSyncRequired;

    public void updateRealRotation(float yRot, float xRot) {
        realYRot = yRot;
        realXRot = Mth.clamp(xRot, -90f, 90f);
    }

    public float getRealYRot() {
        return realYRot;
    }
    public float getRealXRot() {
        return realXRot;
    }

    public float getRotationYRot() {
        return rotationYRot;
    }
    public float getRotationXRot() {
        return rotationXRot;
    }

    public void setRotationYRot(float yRot) {
        this.rotationYRot = yRot;
    }
    public void setRotationXRot(float xRot) {
        this.rotationXRot = Mth.clamp(xRot, -90f, 90f);
    }

    // WE DO NOT WRAP/NORMALIZE SERVER ROTATIONS!!!
    public float getServerYRot() {
        return serverYRot;
    }

    public void setServerYRot(float yRot) {
        this.serverYRot0 = serverYRot;
        this.serverYRot = yRot;
    }

    public float getServerXRot() {
        return serverXRot;
    }
    public void setServerXRot(float pitch) {
        this.serverXRot0 = serverXRot;
        this.serverXRot = pitch;
    }

    public float getServerYRot0() {
        return serverYRot0;
    }


    public float getServerXRot0() {
        return serverXRot0;
    }

    public float getServerDeltaYRot() {
        return serverDeltaYRot;
    }
    public void setServerDeltaYRot(float deltaYRot) {
        this.serverDeltaYRot = deltaYRot;
    }

    public boolean getSilentSyncRequired() { 
        return silentSyncRequired; 
    }
    public void setSilentSyncRequired(boolean silentSyncRequired) {
        this.silentSyncRequired = silentSyncRequired;
    }

    public boolean isRotating() {
        return ROTATION_SERVICE.getTickHandler().isRotating();
    }
}
