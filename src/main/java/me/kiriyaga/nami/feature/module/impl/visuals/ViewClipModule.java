package me.kiriyaga.nami.feature.module.impl.visuals;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.Render2DEvent;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.RegisterModule;
import me.kiriyaga.nami.feature.setting.impl.BoolSetting;
import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
import net.minecraft.client.CameraType;

import static me.kiriyaga.nami.Nami.MC;

@RegisterModule
public class ViewClipModule extends Module {

    public final DoubleSetting distance = addSetting(new DoubleSetting("Distance", 3.5, 1, 9));
    public final BoolSetting animate = addSetting(new BoolSetting("Animation", true));

    private float currentDistance = 3.5f;

    public ViewClipModule() {
        super("ViewClip", "Disables block clipping and extends camera distance.", ModuleCategory.of("Render"), "viewclip");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender(Render2DEvent ev) {
        CameraType perspective = MC.options.getPerspective();

        if (perspective == CameraType.FIRST_PERSON) {
            currentDistance = 1f;
        } else {
            if (animate.get()) {
                currentDistance += (float) (distance.get() - currentDistance) * 0.12f;
            } else {
                currentDistance = distance.get().floatValue();
            }
        }
    }

    public float getAnimatedDistance() {
        return currentDistance;
    }
}