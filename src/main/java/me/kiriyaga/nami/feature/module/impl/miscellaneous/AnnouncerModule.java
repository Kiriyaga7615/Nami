package me.kiriyaga.nami.feature.module.impl.miscellaneous;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.EntitySpawnEvent;
import me.kiriyaga.nami.event.impl.PacketReceiveEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;

import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class AnnouncerModule extends Module {
    public enum VisualRangeMode {
        NONE, BELL, EXP
    }

    public final BoolSetting everyone = addSetting(new BoolSetting("Everyone", false));
    public final BoolSetting friends = addSetting(new BoolSetting("Friends", true));
    public final BoolSetting joinAnnounce = addSetting(new BoolSetting("JoinAnnounce", false));
    public final BoolSetting visualRange = addSetting(new BoolSetting("VisualRange", false));
    private final EnumSetting<VisualRangeMode> soundMode = addSetting(new EnumSetting<>("Sound", VisualRangeMode.NONE));

    public AnnouncerModule() {
        super("Announcer", "Announces in chat when a certain action happened.", ModuleCategory.of("Miscellaneous"), "joinannounce", "joins", "announce", "visualrange");
    soundMode.setShowCondition(visualRange::get);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!joinAnnounce.get()) return;

        if (event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket joinPacket) {
            if (joinPacket.getActions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                for (var entry : joinPacket.getEntries()) {
                    String playerName = entry.profile().name();
                    if (playerName == null) continue;

                    boolean isFriend = FRIEND_MANAGER.isFriend(playerName);

                    if ((everyone.get() && !isFriend) || (friends.get() && isFriend)) {
                        Component message = CAT_FORMAT.format("{g}" + playerName + " {reset}joined the game.");
                        LOG.addEntry(this.name + ": " + message.getString());
                        CHAT_MANAGER.sendPersistent(playerName, message);
                    }
                }
            }
        } else if (event.getPacket() instanceof ClientboundPlayerInfoRemovePacket leavePacket) {
            for (var playerInfo : leavePacket.comp_1105()) {
                var info = MC.getNetworkHandler().getPlayerListEntry(playerInfo);
                if (info == null) continue;

                String playerName = info.getProfile().name();
                if (playerName == null) continue;

                boolean isFriend = FRIEND_MANAGER.isFriend(playerName);

                if ((everyone.get() && !isFriend) || (friends.get() && isFriend)) {
                    Component message = CAT_FORMAT.format("{g}" + playerName + " {reset}has left the game.");
                    LOG.addEntry(this.name + ": " + message.getString());
                    CHAT_MANAGER.sendPersistent(playerName, message);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (MC.player == null || MC.world == null || !visualRange.get()) return;

        if (event.getEntity() instanceof Player player) {

            if (player == MC.player)
                return;

            boolean isFriend = FRIEND_MANAGER.isFriend(player.getName().getString());

            if (friends.get() && everyone.get()) {
            } else if (friends.get() && isFriend) {
            } else if (everyone.get() && !isFriend) {
            } else {
                return;
            }

            Component message = CAT_FORMAT.format("{g}" + player.getName().getString() + " {reset}has entered visual range.");

            LOG.addEntry(this.name + ": " + message.getString());
            CHAT_MANAGER.sendPersistent(player.getUuidAsString(), message);

            switch (soundMode.get()) {
                case BELL -> MC.player.playSound(SoundEvents.BLOCK_BELL_USE, 1.0f, 1.0f);
                case EXP -> MC.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                default -> {}
            }
        }
    }
}
