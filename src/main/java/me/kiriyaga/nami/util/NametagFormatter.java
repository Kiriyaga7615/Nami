package me.kiriyaga.nami.util;

import me.kiriyaga.nami.feature.module.impl.visuals.NametagsModule;
import me.kiriyaga.nami.util.entity.HostileUtils;
import net.minecraft.client.telemetry.TelemetryProperty;
import 	net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.awt.*;
import java.text.DecimalFormat;

import static me.kiriyaga.nami.Nami.*;
import static net.minecraft.world.level.GameType.*;

public class NametagFormatter {

    private final NametagsModule module;
    private final DecimalFormat df = new DecimalFormat("##");

    public static final Color COLOR_PASSIVE = new Color(211, 211, 211, 255);
    public static final Color COLOR_NEUTRAL = new Color(255, 255, 0, 255);
    public static final Color COLOR_HOSTILE = new Color(255, 0, 0, 255);
    public static final Color COLOR_ITEM = new Color(211, 211, 211, 255);
    public static final Color COLOR_FRIEND = new Color(0, 170, 170, 255);
    public static final Color COLOR_SNEAK = new Color(255, 165, 0, 255);

    public NametagFormatter(NametagsModule module) {
        this.module = module;
    }

    public Component formatPlayer(Player player) {
        return Component.literal(player.getName().getString()).setStyle(getStyle());
    }

    public Component getHealthText(Player player) {
        double hp = player.getHealth() + player.getAbsorptionAmount();
        double health = Math.round(hp * 2.0) / 2.0;

        int rgbColor;
        if (health >= 19) {
            rgbColor = 0x00FF00;
        } else if (health >= 13) {
            rgbColor = 0xFFFF00;
        } else if (health >= 8) {
            rgbColor = 0xFFA500;
        } else if (health >= 6) {
            rgbColor = 0xFF0000;
        } else {
            rgbColor = 0x8B0000;
        }

        Style style = getStyle().withColor(TextColor.fromRgb(rgbColor));
        return Component.literal(df.format(hp)).setStyle(style);
    }

    public Component formatGameMode(Player player) {
        TelemetryProperty.GameMode gm = TelemetryProperty.GameMode.SURVIVAL;
        try {
            var entry = MC.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) gm = entry.getGameMode();
        } catch (Exception ignored) {}

        String gmShort;
        switch (gm) {
            case CREATIVE -> gmShort = "[C]";
            case SURVIVAL -> gmShort = "[S]";
            case ADVENTURE -> gmShort = "[A]";
            case SPECTATOR -> gmShort = "[Sp]";
            default -> gmShort = "[?]";
        }

        return Component.literal(gmShort).setStyle(getStyle().withColor(TextColor.fromRgb(Color.WHITE.getRGB())));
    }

    public Component formatPing(Player player) {
        int ping = 0;
        try {
            var entry = MC.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) ping = entry.getLatency();
        } catch (Exception ignored) {}

        int rgbColor;
        if (ping <= 50) {
            rgbColor = 0x00FF00;
        } else if (ping <= 70) {
            rgbColor = 0x006400;
        } else if (ping <= 100) {
            rgbColor = 0xFFFF00;
        } else if (ping <= 150) {
            rgbColor = 0xFFA500;
        } else {
            rgbColor = 0xFF0000;
        }

        Style style = getStyle().withColor(TextColor.fromRgb(rgbColor));
        return Component.literal(ping + "ms").setStyle(style);
    }

    public Component formatEntityId(Entity entity) {
        return Component.literal(String.valueOf(entity.getId())).setStyle(getStyle().withColor(TextColor.fromRgb(Color.WHITE.getRGB())));
    }

    public Component formatEntity(Entity entity) {
        return Component.literal(entity.getName().getString()).setStyle(getStyle());
    }

    public Component formatItem(ItemEntity item) {
        String name = item.getStack().getName().getString();
        int count = item.getStack().getCount();
        String text = count > 1 ? name + " x" + count : name;
        return Component.literal(text).setStyle(getStyle());
    }

    public Component formatWithColor(Component baseText, Color forcedColor, Entity entity) {
        Color color = forcedColor;

        if (color == null && entity != null) {
            if (entity instanceof Player player) {
                if (player.isSneaking()) {
                    color = COLOR_SNEAK;
                } else if (FRIEND_MANAGER.isFriend(player.getName().getString())) {
                    color = COLOR_FRIEND;
                } else {
                    color = Color.WHITE;
                }
            } else if (entity instanceof ItemEntity) {
                color = COLOR_ITEM;
            } else if (HostileUtils.isHostile(entity)) {
                color = COLOR_HOSTILE;
            } else if (HostileUtils.isNeutral(entity)) {
                color = COLOR_NEUTRAL;
            } else if (HostileUtils.isPassive(entity)) {
                color = COLOR_PASSIVE;
            } else {
                color = Color.WHITE;
            }
        }

        Style style = getStyle().withColor(TextColor.fromRgb(color.getRGB()));
        return baseText.copy().setStyle(style);
    }

    private Style getStyle() {
        boolean bold = module.formatting.get() == NametagsModule.TextFormat.BOLD || module.formatting.get() == NametagsModule.TextFormat.BOTH;
        boolean italic = module.formatting.get() == NametagsModule.TextFormat.ITALIC || module.formatting.get() == NametagsModule.TextFormat.BOTH;
        return Style.EMPTY.withBold(bold).withItalic(italic);
    }
}
