package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import static namidevelopment.kiriyaga.api.NamiApi.ROTATION_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.SERVER_SERVICE;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.PacketUtils.sendSequencedPacket;

@RegisterFeature
public class AutoBowReleaseFeature extends Feature {
    public enum TpsMode {NONE, LATEST, AVERAGE}

    public final IntSetting ticks = addSetting(new IntSetting("Delay", 3, 0, 25));
    public final EnumSetting<TpsMode> tpsMode = addSetting(new EnumSetting<>("TPS", TpsMode.NONE));

    private float ticker = 0f;

    public AutoBowReleaseFeature() {
        super("AutoBowRelease", "Automatically releases bow after holding for a set time.", FeatureCategory.of("Combat"), "autbowrelease");
    }

    @Override
    public void onEnable() {
        ticker = 0f;
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.player == null || MC.level == null || !MC.player.isUsingItem()) return;

        Item usedItem = MC.player.getUseItem().getItem();
        if (usedItem != Items.BOW && usedItem != Items.TRIDENT) return;

        float tps = switch (tpsMode.get()) {
            case LATEST -> SERVER_SERVICE.getLatestTPS();
            case AVERAGE -> SERVER_SERVICE.getAverageTPS();
            default -> 20.0f;
        };

        ticker += tps / 20.0f;

        if (ticker >= ticks.get()) {
            ticker = 0f;

            MC.getConnection().send(
                    new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN)
            );
            MC.player.releaseUsingItem();
            sendSequencedPacket(id -> new ServerboundUseItemPacket(MC.player.getUsedItemHand(), id, ROTATION_SERVICE.getStateHandler().getServerYRot(), ROTATION_SERVICE.getStateHandler().getServerXRot()));

        }
    }
}