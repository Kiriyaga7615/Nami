package me.kiriyaga.nami.feature.module.impl.movement;

import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.*;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.impl.visuals.FreecamModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.mixin.KeyBindingAccessor;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.*;
import 	net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import 	net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import org.lwjgl.glfw.GLFW;

import static me.kiriyaga.nami.Nami.MC;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

@RegisterModule
public class GuiMoveModule extends Module {

    private boolean forwardHeld = false;
    private boolean backHeld = false;
    private boolean leftHeld = false;
    private boolean rightHeld = false;
    private boolean jumpHeld = false;

    private Screen lastScreen = null;
    private final java.util.Deque<ServerboundContainerClickPacket> clickBuffer = new java.util.ArrayDeque<>();

    public final BoolSetting _2b2t = addSetting(new BoolSetting("2b2t", true));

    public GuiMoveModule() {
        super("GuiMove", "Allows movement in most GUIs.", ModuleCategory.of("Movement"), "guimove");
    }

    @Override
    public void onDisable() {
        forwardHeld = false;
        backHeld = false;
        leftHeld = false;
        rightHeld = false;
        jumpHeld = false;
        setKeysPressed(false);
        lastScreen = null;
        clickBuffer.clear();
    }

    /*
     Author @cattyngmd
     licensed as nami:
     MIT (2025)
    */
    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent ev){
        if (!_2b2t.get())
            return;

        if (ev.getPacket() instanceof ClientboundContainerClosePacket packet && packet.getSyncId() == MC.player.playerScreenHandler.syncId)
            ev.cancel();
    }

//    @SubscribeEvent
//    public void onPacketSend(PacketSendEvent ev) {
//        if (!_2b2t.get()) return;
//
//        if (!(ev.getPacket() instanceof ClickSlotC2SPacket packet)) return;
//
//        if (!isPlayerInv()) {
//            if (!clickBuffer.isEmpty()) clickBuffer.clear();
//            return;
//        }
//
//        if (packet.syncId() != MC.player.playerScreenHandler.syncId) return;
//
//        if (packet.actionType() != SlotActionType.PICKUP) return;
//
//        ev.cancel();
//        clickBuffer.addLast(packet);
//
//        if (clickBuffer.size() > 2) {
//            clickBuffer.clear();
//        }
//    }

//    @SubscribeEvent
//    public void onPreTick(PreTickEvent ev) {
//        if (!_2b2t.get()) return;
//
//        if (!isPlayerInv()) {
//            if (!clickBuffer.isEmpty()) clickBuffer.clear();
//            return;
//        }
//
//        if (clickBuffer.size() < 2) return;
//
//        ClickSlotC2SPacket first = clickBuffer.pollFirst();
//        ClickSlotC2SPacket second = clickBuffer.pollFirst();
//
//        if (first == null || second == null) {
//            clickBuffer.clear();
//            return;
//        }
//
//        MC.getNetworkHandler().sendPacket(first);
//        MC.getNetworkHandler().sendPacket(second);
//        MC.getNetworkHandler().sendPacket(first);
//
//        clickBuffer.clear();
//    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (!canMove()) return;

        updateHeld(MC.options.forwardKey, event.key, event.scancode, event.action, event.modifiers, false, v -> forwardHeld = v);
        updateHeld(MC.options.backKey, event.key, event.scancode, event.action, event.modifiers, false, v -> backHeld = v);
        updateHeld(MC.options.leftKey, event.key, event.scancode, event.action, event.modifiers, false, v -> leftHeld = v);
        updateHeld(MC.options.rightKey, event.key, event.scancode, event.action, event.modifiers, false, v -> rightHeld = v);
        updateHeld(MC.options.jumpKey, event.key, event.scancode, event.action, event.modifiers, false, v -> jumpHeld = v);
    }

    private void updateHeld(KeyMapping bind, int key, int scancode, int action, int modifiers, boolean mouse, java.util.function.Consumer<Boolean> setter) {
        if (!mouse) {
            KeyEvent input = new KeyEvent(key, scancode, modifiers);
            if (!bind.matchesKey(input)) return;
        } else {
            MouseButtonInfo mouseInput = new MouseButtonInfo(key, 0);
            MouseButtonEvent click = new MouseButtonEvent(0, 0, mouseInput);
            if (!bind.matchesMouse(click)) return;
        }

        setter.accept(action == GLFW.GLFW_PRESS);
    }



    @SubscribeEvent
    public void onRender3D(Render3DEvent event) {
        if (MODULE_MANAGER.getStorage().getByClass(FreecamModule.class).isEnabled()) return;

        Screen currentScreen = MC.currentScreen;

        if (currentScreen != null) {
            if (lastScreen != currentScreen) {
                resetHeldKeys();
                if (!isPlayerInv()) clickBuffer.clear();
            }
            lastScreen = currentScreen;
        } else {
            lastScreen = null;
            clickBuffer.clear();
        }

        if (!canMove()) {
            setKeysPressed(false);
            return;
        }

        updateKeyWithHold(MC.options.forwardKey, forwardHeld);
        updateKeyWithHold(MC.options.backKey, backHeld);
        updateKeyWithHold(MC.options.leftKey, leftHeld);
        updateKeyWithHold(MC.options.rightKey, rightHeld);
        updateKeyWithHold(MC.options.jumpKey, jumpHeld);
    }

    private void resetHeldKeys() {
        forwardHeld = false;
        backHeld = false;
        leftHeld = false;
        rightHeld = false;
        jumpHeld = false;
        setKeysPressed(false);
    }

    private void updateKeyWithHold(KeyMapping bind, boolean held) {
        InputConstants.Key boundKey = ((KeyBindingAccessor) bind).getBoundKey();
        int keyCode = boundKey.getCode();
        boolean physicallyPressed = InputConstants.isKeyPressed(MC.getWindow(), keyCode);
        bind.setPressed(physicallyPressed || held);
    }

    private boolean canMove() {
        if (MC.currentScreen == null) return true;

        if (MC.currentScreen instanceof ChatScreen
                || MC.currentScreen instanceof SignEditScreen
                || MC.currentScreen instanceof AnvilScreen
                || MC.currentScreen instanceof AbstractCommandBlockScreen
                || MC.currentScreen instanceof StructureBlockScreen
                || MC.currentScreen instanceof CreativeInventoryScreen) {
            return false;
        }

//        if (_2b2t.get() && ( // theese containers doesnt work on 2b, or they do but i dont care
//                MC.currentScreen instanceof ShulkerBoxScreen
//                        || MC.currentScreen instanceof AnvilScreen
//                        || MC.currentScreen instanceof BrewingStandScreen
//                        || MC.currentScreen instanceof CartographyTableScreen
//                        || MC.currentScreen instanceof CrafterScreen
//                        || MC.currentScreen instanceof EnchantmentScreen
//                        || MC.currentScreen instanceof FurnaceScreen
//                        || MC.currentScreen instanceof GrindstoneScreen
//                        || MC.currentScreen instanceof HopperScreen
//                        || MC.currentScreen instanceof HorseScreen
//                        || MC.currentScreen instanceof MerchantScreen
//                        || MC.currentScreen instanceof SmithingScreen
//                        || MC.currentScreen instanceof SmokerScreen
//                        || MC.currentScreen instanceof StonecutterScreen
//                        || MC.currentScreen instanceof GenericContainerScreen
//                        || MC.currentScreen instanceof CreativeInventoryScreen)) {
//            return false;
//        }

        return true;
    }

    private boolean isPlayerInv() {
        if (MC.player == null) return false;
        if (!(MC.currentScreen instanceof InventoryScreen)) return false;
        return MC.player.currentScreenHandler == MC.player.playerScreenHandler;
    }

    private void setKeysPressed(boolean pressed) {
        MC.options.forwardKey.setPressed(pressed);
        MC.options.backKey.setPressed(pressed);
        MC.options.leftKey.setPressed(pressed);
        MC.options.rightKey.setPressed(pressed);
        MC.options.jumpKey.setPressed(pressed);
        MC.options.sneakKey.setPressed(pressed);
        MC.options.sprintKey.setPressed(pressed);
    }
}
