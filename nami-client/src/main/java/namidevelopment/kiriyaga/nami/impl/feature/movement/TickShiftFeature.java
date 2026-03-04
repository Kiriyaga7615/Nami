/*
The idea how it should work is from https://github.com/NamiDevelopment/mint/blob/master/src/main/java/net/melbourne/modules/impl/movement/TickShiftFeature.java
licensed under MIT(2025)
 */

package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;

import namidevelopment.kiriyaga.nami.impl.feature.exploits.TimerFeature;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterFeature
public class TickShiftFeature extends Feature {

    public enum Mode {PHYSICS, FORCE_TIMER}

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.PHYSICS));
    public final IntSetting packets = addSetting(new IntSetting("Packets", 15, 1, 50));
    public final DoubleSetting intensity = addSetting(new DoubleSetting("Intensity", 3.0, 1.0, 10.0));
    public final DoubleSetting speed = addSetting(new DoubleSetting("Speed", 0.25, 0.01, 2.0));
    public final IntSetting amount = addSetting(new IntSetting("Amount", 5, 1, 20));
    public final BoolSetting selfToggle = addSetting(new BoolSetting("SelfToggle", true));

    private int left = 0;
    private float progress = 0f;
    private boolean physics = true;

    public TickShiftFeature() {super("TickShift", "Tick acceleration to speed up.", FeatureCategory.of("Movement"), "tickshift");
    }

    @Override
    public void onEnable() {
        left = packets.get().intValue();
        progress = 0f;
        physics = true;
    }

    @Override
    public void onDisable() {
        left = 0;
        progress = 0f;
        physics = true;
    }

    @SubscribeEvent
    public void onPreTick(PreTickEvent event) {
        this.clearDisplayInfo();
        if (MC.player == null || MC.level == null)
            return;
        TimerFeature timer = FEATURE_SERVICE.getStorage().getByClass(TimerFeature.class);

        boolean b = INPUT_SERVICE.getClientHandler().getCache().isMoving();
        
        if (b && left > 0) {
            switch (mode.get()) {
                case PHYSICS -> {
                    if (physics) {
                        physics = false;

                        for (int i = 0; i < this.intensity.get().intValue(); i++) {
                            MC.player.tick();
                        }
                    }
                }

                case FORCE_TIMER -> {
                    if (timer != null && timer.isEnabled()) {
                        timer.forceTimer(this.intensity.get().floatValue());
                    }
                }
                default -> {
                }
            }

            left--;
            progress = 0f;

            if (left <= 0 && selfToggle.get())
                toggle();

            if (left <= 0 && selfToggle.get() && mode.get() == Mode.FORCE_TIMER && timer != null && timer.isEnabled())
                timer.forceTimer(1);

        } else {
            if (timer != null) {
                timer.releaseTimer();
            }

            if (!b)
                recharge();
        }

        if (!b)
            physics = true;

        this.addDisplayInfo(left +"");
    }

    private void recharge() {
        progress += speed.get().floatValue();
        while (progress >= 1.0f) {
            progress -= 1.0f;
            left = Math.min(left + amount.get().intValue(), packets.get().intValue());
        }
    }
}