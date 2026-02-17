package namidevelopment.kiriyaga.nami.impl.gui.base;

import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;

import static namidevelopment.kiriyaga.api.NamiApi.FONT_SERVICE;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public class BaseItemPanel extends BasePanel {

    private static final int HEIGHT = 13;
    private final String itemName;
    private final WhitelistSetting whitelist;

    public BaseItemPanel(String itemName, WhitelistSetting whitelist) {
        this.itemName = itemName;
        this.whitelist = whitelist;
        this.height = HEIGHT;
    }

    @Override
    public void render(GuiGraphics context, Font font, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);
        boolean enabled = whitelist.contains(itemName);

        Color textColor = enabled ? Color.GREEN : Color.RED;
        int textX = x + 3 + (hovered ? 1 : 0);
        int textY = y + (HEIGHT - 8) / 2;

        FONT_SERVICE.drawText(context, itemName, textX, textY, toRGBA(textColor), true);
    }

    @Override
    public void onLeftClick() {
        if (whitelist.contains(itemName)) whitelist.remove(itemName);
        else whitelist.add(itemName);
    }

    @Override
    protected String getName() { return itemName; }

    @Override
    protected boolean isEnabled() { return whitelist.contains(itemName); }
}
