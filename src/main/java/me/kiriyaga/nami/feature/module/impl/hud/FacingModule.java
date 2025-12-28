package me.kiriyaga.nami.feature.module.impl.hud;

import me.kiriyaga.nami.feature.module.HudElementModule;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class FacingModule extends HudElementModule {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));

    public FacingModule() {
        super("Facing", "Displays player facing direction.", 0, 0, 50, 9);
    }

    @Override
    public Component getDisplayText() {
        Minecraft mc = MC;
        if (mc.player == null) {
            width = FONT_MANAGER.getWidth("NaN");
            height = FONT_MANAGER.getHeight();
            return CAT_FORMAT.format("{bg}NaN");
        }

        float yaw = mc.player.getYaw() % 360;
        if (yaw < 0) yaw += 360;

        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        double absDx = Math.abs(dx);
        double absDz = Math.abs(dz);

        String dir = switch ((int) Math.floor((yaw + 45) / 90) % 4) {
            case 0 -> "South";
            case 1 -> "West";
            case 2 -> "North";
            case 3 -> "East";
            default -> "Invalid";
        };

        String axisPart;

        if (absDx > 0.2 && absDz > 0.2 && Math.abs(absDx - absDz) < 0.4) {
            String axisX = dx > 0 ? "+X" : "-X";
            String axisZ = dz > 0 ? "+Z" : "-Z";
            axisPart = formatAxis(axisX) + "{bg}, {bg} " + formatAxis(axisZ);
        } else {
            if (absDz > absDx) {
                String axis = dz > 0 ? "+Z" : "-Z";
                axisPart = formatAxis(axis);
            } else {
                String axis = dx > 0 ? "+X" : "-X";
                axisPart = formatAxis(axis);
            }
        }

        String labelPart = displayLabel.get() ? "{bg}" + dir + " {bg}[" + axisPart + "{bg}]" : axisPart;

        width = FONT_MANAGER.getWidth(labelPart.replace("{bg}", "").replace("{bw}", "").replace("{gray}", ""));
        height = FONT_MANAGER.getHeight();

        return CAT_FORMAT.format(labelPart);
    }

    private String formatAxis(String axis) {
        char sign = axis.charAt(0);
        String letter = axis.substring(1);
        return "{bw}" + sign + letter;
    }
}