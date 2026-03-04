package namidevelopment.kiriyaga.api.core.input;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.PostTickEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import net.minecraft.util.Mth;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class ClientInputHandler {

    private InputCache cache;
    private boolean overriding;
    private String overrideOwner;
    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean shift;

    public void init(InputCache cache) {
        EVENT_SERVICE.register(this);
        this.cache = cache;
    }


    public void overrideMovement(String owner, boolean forward, boolean back, boolean left, boolean right) {
        if (overrideOwner != null && !overrideOwner.equals(owner))
            return;

        overrideOwner = owner;
        overriding = true;

        this.forward = forward;
        this.back = back;
        this.left = left;
        this.right = right;
        this.jump = cache.jump();
        this.shift = cache.shift();
    }

    public void overrideEverything(String owner, boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean shift) {
        if (overrideOwner != null && !overrideOwner.equals(owner))
            return;
        overrideOwner = owner;
        overriding = true;
        this.forward = forward;
        this.back = back;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.shift = shift;
    }

    public void clearOverride(String owner) {
        if (overrideOwner != null && !overrideOwner.equals(owner))
            return;
        overriding = false;
        overrideOwner = null;
        forward = back = left = right = false;
        jump = shift = false;
        cache.updateFromOptions();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null)
            return;

        cache.updateFromOptions();
        if (overriding)
            applyOverride();
    }

    private void applyOverride() {
        MC.options.keyUp.setDown(forward);
        MC.options.keyDown.setDown(back);
        MC.options.keyLeft.setDown(left);
        MC.options.keyRight.setDown(right);
        MC.options.keyJump.setDown(jump);
        MC.options.keyShift.setDown(shift);
    }

    public boolean isOverriding() {
        return overriding;
    }

    public InputCache getCache() {
        return cache;
    }

    public float getDirection() {
        float realYaw = MC.player.getYRot();
        boolean forward = INPUT_SERVICE.getInputCache().forward();
        boolean back = INPUT_SERVICE.getInputCache().back();
        boolean left = INPUT_SERVICE.getInputCache().left();
        boolean right = INPUT_SERVICE.getInputCache().right();
        int inputX = (right ? 1 : 0) - (left ? 1 : 0);
        int inputZ = (forward ? 1 : 0) - (back ? 1 : 0);
        if (inputX == 0 && inputZ == 0) return realYaw;
        if (inputZ > 0) return realYaw; if (inputZ < 0) return Mth.wrapDegrees(realYaw + 180);
        if (inputX != 0 && inputZ == 0)
            return Mth.wrapDegrees(realYaw + (inputX > 0 ? 90 : -90));

        if (inputZ > 0 && inputX != 0) return realYaw;
        if (inputZ < 0 && inputX != 0) return Mth.wrapDegrees(realYaw + 180);

        return realYaw;
    }
}