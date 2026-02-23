package namidevelopment.kiriyaga.api.core.input;

import net.minecraft.client.Options;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

public class InputCache {

    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean shift;

    public void updateFromOptions() {
        if (MC.player == null) return;

        Options o = MC.options;

        forward = o.keyUp.isDown();
        back = o.keyDown.isDown();
        left = o.keyLeft.isDown();
        right = o.keyRight.isDown();
        jump = o.keyJump.isDown();
        shift = o.keyShift.isDown();
    }

    public boolean forward() {
        return forward;
    }
    public boolean back() {
        return back;
    }
    public boolean left() {
        return left;
    }
    public boolean right() {
        return right;
    }
    public boolean jump() {
        return jump;
    }
    public boolean shift() {
        return shift;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public void setBack(boolean back) {
        this.back = back;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public void setShift(boolean shift) {
        this.shift = shift;
    }

    public boolean isMoving() {
        return forward || back || left || right;
    }

    public boolean hasAnyInput() {
        return isMoving() || jump || shift;
    }
}