package namidevelopment.kiriyaga.nami.impl.feature.visuals;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import net.minecraft.client.CameraType;

import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class NoWeatherFeature extends Feature {
    public NoWeatherFeature() {
        super("NoWeather", "Disables rendering of weather.", FeatureCategory.of("Render"), "noweather", "nowether", "nowather");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    private void onUpdate (PreTickEvent ev){
        if (MC == null || MC.level == null || MC.player == null)
            return;
        this.clearDisplayInfo();

        String weather;

        if (MC.level.isRaining()) {
            if (MC.level.isThundering()) {
                weather = "thunder";
            } else {
                weather = "rain";
            }
        } else {
            weather = "clear";
        }

        this.addDisplayInfo(weather);
    }
}
