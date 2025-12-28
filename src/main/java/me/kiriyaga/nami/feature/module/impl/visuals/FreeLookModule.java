package me.kiriyaga.nami.feature.module.impl.visuals;

import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import net.minecraft.client.CameraType;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class FreeLookModule extends Module { // todo this shit broke
    public float cameraYaw;
    public float cameraPitch;

    private CameraType previousPerspective;

    public DoubleSetting sensivity = addSetting(new DoubleSetting("Sensivity", 5, 2, 15));


    public FreeLookModule() {
        super("FreeLook", "Look around freely without moving your real yaw/pitch.", ModuleCategory.of("Render"), "freelook", "freelok", "third");
    }

    @Override
    public void onEnable() {
        if (MC.player == null || MC.world == null) {
            toggle();
            return;
        }

        cameraYaw = MC.player.getYaw();
        cameraPitch = MC.player.getPitch();

        previousPerspective = MC.options.getPerspective();
        if (previousPerspective != CameraType.THIRD_PERSON_BACK) {
            MC.options.setPerspective(CameraType.THIRD_PERSON_BACK);
        }
    }

    @Override
    public void onDisable() {
        if (previousPerspective != null && MC.options.getPerspective() != previousPerspective) {
            MC.options.setPerspective(previousPerspective);
        }
        if (previousPerspective == null) {
            MC.options.setPerspective(CameraType.FIRST_PERSON);
        }
    }
}
