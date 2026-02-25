package namidevelopment.kiriyaga.nami.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(Connection.class)
public interface DuckConnection {
    @Invoker("doSendPacket")
    void doSendPacket(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush);
}