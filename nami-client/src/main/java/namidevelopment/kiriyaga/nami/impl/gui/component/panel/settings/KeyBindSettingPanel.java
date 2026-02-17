package namidevelopment.kiriyaga.nami.impl.gui.component.panel.settings;

import namidevelopment.kiriyaga.api.model.setting.KeyBindSetting;
import namidevelopment.kiriyaga.api.util.KeyUtils;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static namidevelopment.kiriyaga.api.NamiApi.FONT_SERVICE;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public class KeyBindSettingPanel extends BasePanel {

    public static final int HEIGHT = 13;
    private static final int PADDING = 3;

    private final KeyBindSetting setting;
    private static KeyBindSetting waitingForKeyBind = null;

    public KeyBindSettingPanel(KeyBindSetting setting) {
        this.setting = setting;
        this.height = HEIGHT;
    }

    @Override
    public void render(GuiGraphics context, Font font, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        int textY = y + 2;
        int textX = x + PADDING + (hovered ? 1 : 0);

        String nameStr = hovered ? (setting.isHoldMode() ? "Hold" : "Toggle") : setting.getName();
        FONT_SERVICE.drawText(context, nameStr, textX, textY, toRGBA(getTextColor()), true);
        String valueStr = waitingForKeyBind == setting ? "Listening..." : KeyUtils.getKeyName(setting.get());

        int valueX = x + width - PADDING - FONT_SERVICE.getWidth(valueStr);
        FONT_SERVICE.drawText(context, valueStr, valueX, textY, toRGBA(getTextColor()), true);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return false;

        if (waitingForKeyBind == setting) {
            waitingForKeyBind.set(button);
            waitingForKeyBind = null;
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            setting.setHoldMode(!setting.isHoldMode());
            return true;
        }

        if (waitingForKeyBind == null) {
            waitingForKeyBind = setting;
        }

        return true;
    }

    @Override
    public void keyPressed(int keyCode) {
        if (waitingForKeyBind != null) {
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKeyBind.set(-1);
            } else {
                waitingForKeyBind.set(keyCode);
            }
            waitingForKeyBind = null;
        }
    }

    @Override
    protected String getName() {
        return setting.getName();
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected Color getTextColor() {
        return getColorFeature().getStyledTextColor(255);
    }
}
