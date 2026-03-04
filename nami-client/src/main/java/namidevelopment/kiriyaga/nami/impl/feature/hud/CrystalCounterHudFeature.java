package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.AddEntityEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayDeque;
import java.util.Deque;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.NamiApi.*;


@RegisterFeature
public class CrystalCounterHudFeature extends HudElementFeature {

    public enum Mode {
        SPAWN, EXPLOSION
    }

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));
    public final BoolSetting precise = addSetting(new BoolSetting("Precise", false));
    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.SPAWN));

    private final Deque<Long> marked = new ArrayDeque<>();

    public CrystalCounterHudFeature() {
        super("CPS", "Displays current crystals per second(WARNING: IT WORKS FOR ALL ENTITIES IN RANGE).", 0, 0, 100, 30);
    }

    @SubscribeEvent
    private void onAddEntityEvent(AddEntityEvent event) {
        if (MC.player == null || MC.level == null) return;
        if (mode.get() != Mode.SPAWN) return;

        if (event.getPacket() instanceof ClientboundAddEntityPacket packet) {
            if (packet.getType() != EntityType.END_CRYSTAL) return;
            addMarked();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onPacketReceive(PacketReceiveEvent event) {
        if (MC.player == null || MC.level == null) return;
        if (mode.get() != Mode.EXPLOSION) return;

        if (event.getPacket() instanceof ClientboundExplodePacket) {
            addMarked();
        }
    }

    @Override
    public Component getDisplayText() {
        double cps = getCps();

        String formatted = "";

        if (displayLabel.get()) {
            formatted += "{global}CPS: ";
        }

        formatted += "{white}" + formatNumber(cps);

        width = FONT_SERVICE.getWidth(formatted.replaceAll("\\{.*?}", ""));
        height = FONT_SERVICE.getHeight();

        return CAT_FORMAT.format(formatted);
    }


    private void addMarked() {
        long now = System.currentTimeMillis();
        marked.addLast(now);
        while (!marked.isEmpty() && now - marked.peekFirst() > 1000) {
            marked.removeFirst();
        }
    }

    private double getCps() {
        long now = System.currentTimeMillis();
        while (!marked.isEmpty() && now - marked.peekFirst() > 1000) {
            marked.removeFirst();
        }
        return marked.size();
    }

    private String formatNumber(double val) {
        return String.format(precise.get() ? "%.2f" : "%.0f", val).replace(',', '.');
    }
}
