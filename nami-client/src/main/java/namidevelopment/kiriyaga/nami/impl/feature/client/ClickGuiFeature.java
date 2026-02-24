package namidevelopment.kiriyaga.nami.impl.feature.client;

import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class ClickGuiFeature extends Feature {

    public final DoubleSetting scale = addSetting(new DoubleSetting("Scale", 1.00, 0.50, 1.50));
    public final BoolSetting lines = addSetting(new BoolSetting("Lines", true));
    //public final BoolSetting descriptions = addSetting(new BoolSetting("Descriptions", true));
    public final IntSetting guiAlpha = addSetting(new IntSetting("UIAlpha", 70, 0, 255));
    public final BoolSetting blur = addSetting(new BoolSetting("Blur", false));
    public final BoolSetting background = addSetting(new BoolSetting("Background", true));
    public final BoolSetting gradientFill = addSetting(new BoolSetting("Gradient", true));
    public final BoolSetting gear = addSetting(new BoolSetting("Gear", true));

    public ClickGuiFeature() {
        super("ClickGui", "Opens client UI.", FeatureCategory.of("Client"), "clickgui","click", "gui", "menu", "clckgui");
        this.keyBind.set(80);
    }

    @Override
    public void onEnable(){
        if (MC == null || MC.mouseHandler == null)
            return;

        NAVIGATE_PANEL.resetActive();
        CLICK_GUI_SCREEN.scale = this.scale.get().floatValue(); // bad
        HUD_EDITOR_SCREEN.scale = this.scale.get().floatValue(); // bad
        //CLICK_GUI_SCREEN.setPreviousScreen(MC.screen);

        CLICK_GUI_SCREEN.refreshPanels();
        HUD_EDITOR_SCREEN.refreshPanels();

        MC.setScreen(CLICK_GUI_SCREEN);
        this.toggle();
    }
}
