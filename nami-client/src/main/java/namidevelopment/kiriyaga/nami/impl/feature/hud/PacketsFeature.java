package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterFeature
public class PacketsFeature extends HudElementFeature {

    public enum LayoutMode { HORIZONTAL, VERTICAL}

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));
    public final EnumSetting<LayoutMode> layout = addSetting(new EnumSetting<>("Layout", LayoutMode.HORIZONTAL));

    private final List<MsPacket> packets = new ArrayList<>();

    public PacketsFeature() {
        super("Packets", "Displays sended packets.", 0, 0, 100, 30);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPacketSend(PacketSendEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet == null) return;
        long now = System.currentTimeMillis();
        MC.execute(() -> {
            packets.add(new MsPacket(packet, now));
            cleanup(now);
        });
    }

    private void cleanup(long now) {
        long cutoff = now - 1000L;
        Iterator<MsPacket> it = packets.iterator();
        while (it.hasNext()) {
            if (it.next().time < cutoff) {
                it.remove();
            }
        }
    }

    private int countGlobal() {
        return packets.size();
    }

    private int countMove() {
        return (int) packets.stream().filter(p -> p.packet instanceof ServerboundMovePlayerPacket).count();
    }

    private int countClick() {
        return (int) packets.stream().filter(p -> p.packet instanceof ServerboundContainerClickPacket).count();
    }

    private int countSwaps() {
        return (int) packets.stream().filter(p -> p.packet instanceof ServerboundSetCarriedItemPacket).count();
    }

    private int countAction() {
        return (int) packets.stream().filter(p -> p.packet instanceof ServerboundPlayerActionPacket).count();
    }

    private int countInteractEntity() {
        return (int) packets.stream().filter(p -> p.packet instanceof ServerboundInteractPacket).count();
    }


    @Override
    public net.minecraft.network.chat.Component getDisplayText() {
        if (layout.get() == LayoutMode.VERTICAL) return null;

        long now = System.currentTimeMillis();
        cleanup(now);
        int move = countMove();
        int click = countClick();
        int swaps = countSwaps();
        int action = countAction();
        int interact = countInteractEntity();
        int global = countGlobal();


        String text = "";

        if (displayLabel.get()) {
            text += "{global}Packets ";
        }

        text += "{secondary}({white}M:" + move +
                "{secondary}, {white}C:" + click +
                "{secondary}, {white}S:" + swaps +
                "{secondary}, {white}A:" + action +
                "{secondary}, {white}I:" + interact +
                "{secondary}, {white}G:" + global +
                "{secondary})";

        width = FONT_SERVICE.getWidth(text.replaceAll("\\{.*?}", ""));
        height = FONT_SERVICE.getHeight();

        return CAT_FORMAT.format(text);
    }

    @Override
    public List<TextElement> getTextElements() {
        if (layout.get() != LayoutMode.VERTICAL) {
            return super.getTextElements();
        }
        long now = System.currentTimeMillis();
        cleanup(now);
        int move = countMove();
        int click = countClick();
        int swaps = countSwaps();
        int action = countAction();
        int interact = countInteractEntity();
        int global = countGlobal();

        List<TextElement> lines = new ArrayList<>();
        int lineHeight = FONT_SERVICE.getHeight() + 1;
        int offsetY = 0;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Move: {white}" + move), 0, offsetY));
        offsetY += lineHeight;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Click: {white}" + click), 0, offsetY));
        offsetY += lineHeight;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Swap: {white}" + swaps), 0, offsetY));
        offsetY += lineHeight;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Action: {white}" + action), 0, offsetY));
        offsetY += lineHeight;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Interact: {white}" + interact), 0, offsetY));
        offsetY += lineHeight;

        lines.add(new TextElement(CAT_FORMAT.format("{global}Global: {white}" + global), 0, offsetY));
        offsetY += lineHeight;

        int maxWidth = lines.stream().mapToInt(te -> FONT_SERVICE.getWidth(te.text().getString().replaceAll("\\{.*?}", ""))).max().orElse(0);
        width = maxWidth;
        height = offsetY;

        return lines;
    }

    private static class MsPacket {
        public final Packet<?> packet;
        public final long time;
        public MsPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }

    private Counts count() {
        int move = 0;
        int click = 0;
        int swaps = 0;
        int action = 0;
        int interact = 0;

        for (MsPacket p : packets) {
            Packet<?> packet = p.packet;

            if (packet instanceof ServerboundMovePlayerPacket) move++;
            else if (packet instanceof ServerboundContainerClickPacket) click++;
            else if (packet instanceof ServerboundSetCarriedItemPacket) swaps++;
            else if (packet instanceof ServerboundPlayerActionPacket) action++;
            else if (packet instanceof ServerboundInteractPacket) interact++;
        }

        return new Counts(move, click, swaps, action, interact, packets.size());
    }

    private record Counts(int move, int click, int swaps, int action, int interact, int global) {}
}
