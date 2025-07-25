package me.kiriyaga.nami.feature.module.impl.misc;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.DissconectEvent;
import me.kiriyaga.nami.event.impl.PacketReceiveEvent;
import me.kiriyaga.nami.event.impl.PacketSendEvent;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.RegisterModule;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.message.LastSeenMessageList;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.QueryEntityNbtC2SPacket;
import net.minecraft.util.Identifier;

import java.time.Instant;
import java.util.BitSet;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class IllegalDisconnectModule extends Module {

    public IllegalDisconnectModule() {
        super("illegal disconnect", "Cancels disconnects and sends invalid packets to force close connection.", ModuleCategory.of("misc"));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private void onDissconect(DissconectEvent event) {
        if (MC == null || MC.getNetworkHandler() == null) return;

        try {
            BitSet invalidBits = new BitSet(10);
            invalidBits.set(0);
            invalidBits.set(9);

            ChatMessageC2SPacket illegalPacket = new ChatMessageC2SPacket(
                    "§",
                    Instant.now(),
                    NetworkEncryptionUtils.SecureRandomUtil.nextLong(),
                    null,
                    new LastSeenMessageList.Acknowledgment(1, invalidBits, (byte) 0)
            );

            MC.getNetworkHandler().sendPacket(illegalPacket);
            event.cancel();
        } catch (Exception e) {
            e.printStackTrace();
            MC.getNetworkHandler().onDisconnect(new net.minecraft.network.packet.s2c.common.DisconnectS2CPacket(
                    net.minecraft.text.Text.of("Illegal dissconect failed")
            ));
        }
    }
}