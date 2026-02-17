package namidevelopment.kiriyaga.nami.impl.gui.component.panel;

import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.setting.*;
import namidevelopment.kiriyaga.nami.impl.gui.base.BasePanel;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.settings.*;

import java.util.*;

public class FeaturePanel extends BasePanel {

    public static final int HEIGHT = 13;

    private final Feature feature;
    private final List<Setting<?>> allSettings = new ArrayList<>();
    private final Map<Setting<?>, BasePanel> settingPanels = new HashMap<>();

    public FeaturePanel(Feature feature) {
        this.feature = feature;
        this.height = HEIGHT;

        for (Setting<?> setting : feature.getSettings()) {
            if (setting == null) continue;
            allSettings.add(setting);
        }
        refreshSettings();
    }

    @Override
    protected String getName() {
        return feature.getName();
    }

    @Override
    protected boolean isEnabled() {
        return feature.isEnabled();
    }

    @Override
    public void onLeftClick() {
        feature.toggle();
    }

    @Override
    public void onMiddleClick() {
        feature.setDrawn(!feature.isDrawn());
    }

    @Override
    public void onRightClick() {
        expanded = !expanded;
    }

    public Feature getFeature() {
        return feature;
    }

    public void refreshSettings() {
        Iterator<Map.Entry<Setting<?>, BasePanel>> it = settingPanels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Setting<?>, BasePanel> entry = it.next();
            Setting<?> setting = entry.getKey();
            BasePanel panel = entry.getValue();

            if (!setting.isShow()) {
                getSubPanels().remove(panel);
                it.remove();
            }
        }

        getSubPanels().clear();
        for (Setting<?> setting : allSettings) {
            if (setting == null || !setting.isShow()) continue;

            BasePanel panel = settingPanels.get(setting);
            if (panel == null) {
                if (setting instanceof WhitelistSetting whitelistSetting)
                    panel = new WhitelistSettingPanel(whitelistSetting);
                else if (setting instanceof BoolSetting boolSetting)
                    panel = new BoolSettingPanel(boolSetting);
                else if (setting instanceof IntSetting intSetting)
                    panel = new IntSettingPanel(intSetting);
                else if (setting instanceof DoubleSetting doubleSetting)
                    panel = new DoubleSettingPanel(doubleSetting);
                else if (setting instanceof KeyBindSetting keyBindSetting)
                    panel = new KeyBindSettingPanel(keyBindSetting);
                else if (setting instanceof EnumSetting enumSetting)
                    panel = new EnumSettingPanel(enumSetting);
                else if (setting instanceof ColorSetting colorSetting)
                    panel = new ColorSettingPanel(colorSetting);

                if (panel != null)
                    settingPanels.put(setting, panel);
            }
            if (panel != null)
                addSubPanel(panel);
        }
    }
}
