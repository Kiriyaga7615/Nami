package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.util.ColorUtils;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.text.DecimalFormat;
import java.util.*;

import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class PlayerListFeature extends HudElementFeature {

    public enum SortMode { ASCENDING, DESCENDING }

    public final EnumSetting<SortMode> sortMode = addSetting(new EnumSetting<>("Sort", SortMode.DESCENDING));
    public final BoolSetting showDistance = addSetting(new BoolSetting("Distance", true));
    public final BoolSetting showHealth = addSetting(new BoolSetting("Health", true));
    public final BoolSetting totemPops = addSetting(new BoolSetting("TotemPops", true));
    public final BoolSetting self = addSetting(new BoolSetting("Self", true));

    private final List<TextElement> elements = new ArrayList<>();
    private final DecimalFormat dec = new DecimalFormat("0.#");

    public PlayerListFeature() {
        super("PlayerList", "Shows nearby players", 0, 0, 80, 10);
    }

    @SubscribeEvent
    public void onPreTickEvent(PreTickEvent ev) {
        elements.clear();

        List<Entity> players = EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PLAYERS);
        sort(players);

        int offset = 0;
        int w = 0;

        for (Entity player : players) {
            if (!self.get() && player instanceof LocalPlayer)
                continue;

            Component t = text(player);
            int width = FONT_SERVICE.getWidth(t);
            elements.add(new TextElement(t, 0, offset));
            w = Math.max(w, width);
            offset += FONT_SERVICE.getHeight();
        }
        this.width = w;
        this.height = offset;
    }

    @Override
    public List<TextElement> getTextElements() {

        if (MC.level == null || MC.player == null) {
            elements.clear();
            return elements;
        }

        return elements;
    }

    private Component text(Entity entity) {
        StringBuilder sb = new StringBuilder();

        if (showHealth.get() && entity instanceof Player player) {
            double hp = player.getHealth() + player.getAbsorptionAmount();
            sb.append(ColorUtils.getHealthColor(player));
            sb.append(dec.format(hp)).append(" ");
        }

        String name = entity.getName().getString();
        if (SOCIALS_SERVICE.isFriend(name)) sb.append("{friend}").append(name);
        else sb.append("{white}").append(name);

        if (showDistance.get()) {
            double dist = Math.round(Math.sqrt(MC.player.distanceToSqr(entity)));

            if (dist <= 15) sb.append(" {dark_red}");
            else if (dist <= 25) sb.append(" {red}");
            else if (dist <= 40) sb.append(" {yellow}");
            else if (dist <= 60) sb.append(" {dark_green}");
            else sb.append(" {green}");

            sb.append(dec.format(dist));
        }

        if (totemPops.get() && entity instanceof Player player) {
            int pops = TOTEMCOUNTER_SERVICE.getPoppedTotemCount(player.getId());
            if (pops > 0) {
                String popText = " -" + pops;
                sb.append(popText);
                String raw = sb.toString();
                raw = raw.replace(popText, " " + ColorUtils.getTotemColor(pops) + "-" + pops);
                return CAT_FORMAT.format(raw);
            }
        }
        return CAT_FORMAT.format(sb.toString());
    }

    private void sort(List<Entity> list) {
        switch (sortMode.get()) {
            case ASCENDING -> list.sort(Comparator.comparingInt(p -> FONT_SERVICE.getWidth(text(p))));
            case DESCENDING -> list.sort((a, b) -> Integer.compare(FONT_SERVICE.getWidth(text(b)), FONT_SERVICE.getWidth(text(a))));
        }
    }
}
