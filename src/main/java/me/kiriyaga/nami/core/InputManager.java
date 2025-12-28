package me.kiriyaga.nami.core;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.KeyInputEvent;
import me.kiriyaga.nami.event.impl.PacketSendEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.impl.movement.GuiMoveModule;
import me.kiriyaga.nami.feature.module.impl.visuals.FreecamModule;
import me.kiriyaga.nami.util.InputCache;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import static me.kiriyaga.nami.Nami.*; // TODO: 1.20.6 viafabric flags sprinting, since packet does not exists. The grim check, does not apply for input on theese versions, but do apply for sprinting

public class InputManager {

    private boolean forward, backward, left, right, jumping, sneaking, sprinting;
    private boolean forwardPressed, backPressed, leftPressed, rightPressed;
    private boolean frozen = false;
    private int freezeTicks = 0;
    private boolean savedForward, savedBack, savedLeft, savedRight;
    private boolean savedJump, savedSneak, savedSprint;

    public void init() {
        EVENT_MANAGER.register(this);
        LOGGER.info("Input Manager loaded");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyInput(KeyInputEvent event) {
        if (!canMove()) return;

        int key = event.key;
        int scancode = event.scancode;
        int action = event.action;

        updateHeld(MC.options.forwardKey, key, scancode, action, v -> forwardPressed = v);
        updateHeld(MC.options.leftKey,    key, scancode, action, v -> leftPressed = v);
        updateHeld(MC.options.backKey,    key, scancode, action, v -> backPressed = v);
        updateHeld(MC.options.rightKey,   key, scancode, action, v -> rightPressed = v);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof ServerboundPlayerInputPacket packet) {
            Input input = packet.comp_3139();

            this.forward = input.comp_3159();
            this.backward = input.comp_3160();
            this.left = input.comp_3161();
            this.right = input.comp_3162();
            this.jumping = input.comp_3163();
            this.sneaking = input.sneak();
            this.sprinting = input.comp_3165();
        } else if (event.getPacket() instanceof ServerboundMoveVehiclePacket) {
            // TODO: finish this
        } else if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreTick(PreTickEvent event) {
        if (!frozen) return;

        freezeTicks--;

        if (freezeTicks <= 0) {
            restoreKeys();
            frozen = false;
        } else {
            disableAllKeys();
        }
    }

    public void freezeInput(int i) {
        if (frozen) return;
        frozen = true;
        freezeTicks = i;

        saveKeys();
        disableAllKeys();
    }


    public boolean isFrozen() {
        return frozen;
    }

    public int getFrozenTicks() {
        return freezeTicks;
    }

    public boolean isForwardPressed() { return forwardPressed; }
    public boolean isBackPressed() { return backPressed; }
    public boolean isLeftPressed() { return leftPressed; }
    public boolean isRightPressed() { return rightPressed; }

    public boolean hasAnyInput() {
        return forward || backward || left || right || jumping || sneaking || sprinting;
    }

    public boolean isMoving() {
        return forward || backward || left || right;
    }

    private void saveKeys() {
        Options opt = MC.options;
        savedForward = opt.forwardKey.isPressed();
        savedBack = opt.backKey.isPressed();
        savedLeft = opt.leftKey.isPressed();
        savedRight = opt.rightKey.isPressed();
        savedJump = opt.jumpKey.isPressed();
        savedSneak = opt.sneakKey.isPressed();
        savedSprint = opt.sprintKey.isPressed();
    }

    private void disableAllKeys() {
        Options opt = MC.options;
        setPressed(opt.forwardKey, false);
        setPressed(opt.backKey, false);
        setPressed(opt.leftKey, false);
        setPressed(opt.rightKey, false);
        setPressed(opt.jumpKey, false);
        setPressed(opt.sneakKey, false);
        setPressed(opt.sprintKey, false);
    }

    private void restoreKeys() {
        Options opt = MC.options;
        setPressed(opt.forwardKey, savedForward);
        setPressed(opt.backKey, savedBack);
        setPressed(opt.leftKey, savedLeft);
        setPressed(opt.rightKey, savedRight);
        setPressed(opt.jumpKey, savedJump);
        setPressed(opt.sneakKey, savedSneak);
        setPressed(opt.sprintKey, savedSprint);
    }

    private void setPressed(KeyMapping key, boolean pressed) {
        key.setPressed(pressed);
    }

    private void updateHeld(KeyMapping bind, int key, int scancode, int action, java.util.function.Consumer<Boolean> setter) {
        KeyEvent input = new KeyEvent(key, scancode, 0); // 0 = нет модификаторов, если нужны, передайте их сюда
        if (!bind.matchesKey(input)) return;
        boolean pressed = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
        setter.accept(pressed);
        if (action == GLFW.GLFW_RELEASE) {
            setter.accept(false);
        }
    }


    public float getDirection() {
        float realYaw = MC.player.getYaw();

        boolean forward = InputCache.forward;
        boolean back = InputCache.back;
        boolean left = InputCache.left;
        boolean right = InputCache.right;

        int inputX = (right ? 1 : 0) - (left ? 1 : 0);
        int inputZ = (forward ? 1 : 0) - (back ? 1 : 0);

        if (inputX == 0 && inputZ == 0) return realYaw;

        if (inputZ > 0) return realYaw;

        if (inputZ < 0) return Mth.wrapDegrees(realYaw + 180);

        if (inputX != 0 && inputZ == 0) return Mth.wrapDegrees(realYaw + (inputX > 0 ? 90 : -90));

        if (inputZ > 0 && inputX != 0) return realYaw;

        if (inputZ < 0 && inputX != 0) return Mth.wrapDegrees(realYaw + 180);

        return realYaw;
    }

    private boolean canMove() {
        if (MODULE_MANAGER.getStorage().getByClass(FreecamModule.class).isEnabled()) return false;
        if (MC.currentScreen == null) return true;
        if (MC.currentScreen != null && !MODULE_MANAGER.getStorage().getByClass(GuiMoveModule.class).isEnabled()) return false;
        if (MC.currentScreen instanceof ChatScreen
                || MC.currentScreen instanceof SignEditScreen
                || MC.currentScreen instanceof AnvilScreen
                || MC.currentScreen instanceof AbstractCommandBlockScreen
                || MC.currentScreen instanceof StructureBlockScreen
                || MC.currentScreen instanceof CreativeInventoryScreen) {
            return false;
        }
        return true;
    }
}
