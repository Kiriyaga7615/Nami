package me.kiriyaga.nami.feature.module.impl.visuals;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import net.minecraft.client.CameraType;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class NoWeatherModule extends Module {
    public float cameraYaw;
    public float cameraPitch;

    private CameraType previousPerspective; // todo this shit broke

    public NoWeatherModule() {
        super("NoWeather", "Disables rendering of weather.", ModuleCategory.of("Render"), "noweather", "nowether", "nowather");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private void onUpdate (PreTickEvent ev){
        if (MC == null || MC.world == null || MC.player == null)
            return;

        String weather;

        if (MC.world.isRaining()) {
            if (MC.world.isThundering()) {
                weather = "thunder";
            } else {
                weather = "rain";
            }
        } else {
            weather = "clear";
        }

        this.setDisplayInfo(weather);
    }
}
