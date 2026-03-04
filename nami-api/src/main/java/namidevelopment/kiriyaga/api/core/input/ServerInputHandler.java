package namidevelopment.kiriyaga.api.core.input;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class ServerInputHandler {
    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean jumping;
    private boolean sneaking;
    private boolean sprinting;

    public void init() {
        EVENT_SERVICE.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof ServerboundPlayerInputPacket packet) {

            Input input = packet.input();
            forward = input.forward();
            backward = input.backward();
            left = input.left();
            right = input.right();
            jumping = input.jump();
            sneaking = input.shift();
            sprinting = input.sprint();
        }
    }

    public boolean isForward() {
        return forward;
    }
    public boolean isBackward() {
        return backward;
    }
    public boolean isLeft() {
        return left;
    }
    public boolean isRight() {
        return right;
    }

    public boolean isJumping() {
        return jumping;
    }
    public boolean isSneaking() {
        return sneaking;
    }
    public boolean isSprinting() {
        return sprinting;
    }

    public boolean isMoving() {
        return forward || backward || left || right;
    }

    public boolean hasAnyInput() {
        return forward || backward || left || right || jumping || sneaking || sprinting;
    }
}