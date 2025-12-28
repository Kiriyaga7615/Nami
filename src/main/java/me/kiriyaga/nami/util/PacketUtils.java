package me.kiriyaga.nami.util;

import me.kiriyaga.nami.mixin.ClientWorldAccessor;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;

import static me.kiriyaga.nami.Nami.MC;

public class PacketUtils {

    public static void sendSequencedPacket(PredictiveAction packetCreator) {
        if (MC.world == null || MC.getNetworkHandler() == null) {
            return;
        }

        BlockStatePredictionHandler p = ((ClientWorldAccessor) MC.world).getPendingUpdateManager().incrementSequence();

        try (p) {
            int sequence = p.getSequence();
            MC.getNetworkHandler().sendPacket(packetCreator.predict(sequence));
        }
    }
}
