package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.AddEntityEvent;
import namidevelopment.kiriyaga.api.event.impl.DissconectEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.util.entity.PlayerUtils;
import namidevelopment.kiriyaga.nami.impl.feature.miscellaneous.AutoReconnectFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.exploits.IllegalDisconnectFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class AutoLogFeature extends Feature {

    public final BoolSetting health = addSetting(new BoolSetting("Health", false));
    public final IntSetting onHealth = addSetting(new IntSetting("OnHealth", 12, 0, 36));
    public final BoolSetting totems = addSetting(new BoolSetting("Totems", false));
    public final IntSetting onTotems = addSetting(new IntSetting("OnTotems", 0, 0, 10));
    public final BoolSetting level = addSetting(new BoolSetting("Level", false));
    public final IntSetting onLevel = addSetting(new IntSetting("OnLevel", 0, 0, 15000));
    public final BoolSetting onRender = addSetting(new BoolSetting("OnRender", false));
    public final BoolSetting packet = addSetting(new BoolSetting("Packet", false));
    public final BoolSetting onPop = addSetting(new BoolSetting("OnPop", false));

    private boolean triggeredLevel = false;
    private boolean loggingOut = false;

    public AutoLogFeature() {
        super("AutoLog", "Automatically logs out in certain conditions.", FeatureCategory.of("Combat"), "autolog", "panic", "logout");
        packet.setShowCondition(() -> onRender.get());
        onHealth.setShowCondition(health::get);
        onTotems.setShowCondition(totems::get);
        onLevel.setShowCondition(level::get);
    }

    @Override
    public void onEnable() {
        loggingOut = false;
        triggeredLevel = false;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onUpdate(PreTickEvent event) {
        if (MC.player == null || MC.level == null || MC.gameMode == null)
            return;

        if (MC.gameMode.isSpectator())
            return;

        this.clearDisplayInfo();

        this.addDisplayInfo(health.get().toString());

        LocalPlayer player = MC.player;

        if (level.get()) {
            if (triggeredLevel && player.getBlockY() <= onLevel.get()) {
                logOut("Too low level: {global}" + player.getBlockY() + "{white} Blocks");
                triggeredLevel = false;
                return;
            }

            if (player.getBlockY() > onLevel.get()) {
                triggeredLevel = true;
            }
        }

        if (health.get() && player.getHealth() <= onHealth.get()) {
            logOut("Low health: {global}" + player.getHealth() + "{white} HP");
            return;
        }

        if (totems.get() && PlayerUtils.getTotemCount() <= onTotems.get()) {
            logOut("Not enough totems: {global}" + PlayerUtils.getTotemCount() + "{white} Totems left");
            return;
        }

        if (onRender.get()) {
            for (Entity other : EntityUtils.getOtherPlayers()) {
                if (!SOCIALS_SERVICE.isFriend(other.getName().getString())) {
                    double distance = player.distanceTo(other);
                    logOut("Untrusted player in range: " + player.getName().toString() + " (" + String.format("%.1f", distance) + " blocks)");
                    return;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet))
            return;

        MC.execute(() -> {
            if (MC.player == null || MC.level == null || MC.gameMode == null)
                return;

            if (MC.gameMode.isSpectator())
                return;

            if (packet.getEntity(MC.level) == MC.player
                    && packet.getEventId() == 35
                    && onPop.get()) {

                logOut("AutoLog: totem got popped.");
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntitySpawn(AddEntityEvent event) {
        if (MC.player == null || MC.level == null || !packet.get() || !onRender.get()) return;

        if (MC.level.getEntity(event.getPacket().getId()) instanceof Player player) {

            if (player == MC.player)
                return;

            if (SOCIALS_SERVICE.isFriend(player.getName().getString()))
                return;

            double distance = MC.player.distanceTo(player);
            logOut("Untrusted player in range: " + player.getName().toString() + " (" + String.format("%.1f", distance) + " blocks)");
        }
    }


    private void logOut(String reason) {
        if (loggingOut)
            return;

        loggingOut = true;
        if (FEATURE_SERVICE.getStorage().getByClass(IllegalDisconnectFeature.class).isEnabled()) {
            triggerToggle();
            EVENT_SERVICE.post(new DissconectEvent());} else {
            if (MC.getConnection() != null) {
                triggerToggle();
                MC.getConnection().handleDisconnect(new ClientboundDisconnectPacket(CAT_FORMAT.format("AutoLog: " + reason)));
            }
        }
    }


    private void triggerToggle(){
        this.toggle();

        if (FEATURE_SERVICE.getStorage().getByClass(AutoReconnectFeature.class).isEnabled())
            FEATURE_SERVICE.getStorage().getByClass(AutoReconnectFeature.class).toggle();
    }
}
