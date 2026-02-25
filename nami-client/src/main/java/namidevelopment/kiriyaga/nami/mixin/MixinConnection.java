package namidevelopment.kiriyaga.nami.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.nami.impl.feature.miscellaneous.NoPacketKick;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@Mixin(Connection.class)
public abstract class MixinConnection {
    private boolean b = false;

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    public void onPacketReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (b) return;
        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        EVENT_SERVICE.post(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        if (event.getPacket() != packet) {
            ci.cancel();

            b = true;
            try {
                ctx.fireChannelRead(event.getPacket());
            } finally {
                b = false;
            }
        }
    }

    @Inject(method = "doSendPacket", at = @At("HEAD"), cancellable = true)
    private void onDoSendPacket(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        PacketSendEvent event = new PacketSendEvent(packet);
        EVENT_SERVICE.post(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        if (event.getPacket() != packet) { // uhhh
            ci.cancel();
            ((DuckConnection) this).doSendPacket(event.getPacket(), listener, flush);
        }
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void exceptionCaught(ChannelHandlerContext context, Throwable exception, CallbackInfo ci) {
        NoPacketKick Feature = FEATURE_SERVICE.getStorage().getByClass(NoPacketKick.class);
        if (Feature != null && Feature.isEnabled()) {
            LOGGER.error("Packet exception caught: ", exception);
            ci.cancel();
        }
    }
}