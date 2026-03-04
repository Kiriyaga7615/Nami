package namidevelopment.kiriyaga.nami.impl.feature.client;


import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.contract.feature.RotationsFeatureConfig;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;

import static namidevelopment.kiriyaga.nami.Nami.FUTURE;

@RegisterFeature
public class RotationsFeature extends Feature implements RotationsFeatureConfig {

    public final EnumSetting<RotationMode> rotation = addSetting(new EnumSetting<>("Rotation",RotationMode.MOTION));
    public final DoubleSetting rotationSpeed = addSetting(new DoubleSetting("Speed", 360, 25, 360));
    public final DoubleSetting rotationEaseFactor = addSetting(new DoubleSetting("Ease", 1, 0.5, 1));
    public final DoubleSetting rotationThreshold = addSetting(new DoubleSetting("Threshold", 5, 3, 15));
    public final IntSetting ticksBeforeRelease = addSetting(new IntSetting("Hold", 0, 00, 30));
    public final EnumSetting<JitterMode> jitter = addSetting(new EnumSetting<>("Jitter",JitterMode.NORMAL));
    public final BoolSetting moveFix = addSetting(new BoolSetting("MoveFix", true));
    public final BoolSetting render = addSetting(new BoolSetting("Render", false));
    public final BoolSetting futureRotations = addSetting(new BoolSetting("FutureRotations", false));

    public RotationsFeature() {
        super("Rotations", "Client rotations configuration.", FeatureCategory.of("Client"), "rotate", "rotationSERVICE", "roate", "toationSERVICE");
        if (!this.isEnabled())
            this.toggle();
//        jitterSpeed.setShowCondition(() -> jitterAmount.get()>0);
//        jitterMaxYaw.setShowCondition(() -> jitterAmount.get() > 0);
//        jitterMaxPitch.setShowCondition(() -> jitterAmount.get() > 0);
        rotationSpeed.setShow(false);
        rotationEaseFactor.setShow(false);
        rotationThreshold.setShow(false);
        ticksBeforeRelease.setShow(false);
        render.setShow(false);

//        jitterAmount.setShow(false);
//        jitterSpeed.setShow(false);
//        jitterMaxYaw.setShow(false);
//        jitterMaxPitch.setShow(false);
    }

    @Override
    public void onDisable(){
        if (!this.isEnabled())
            this.toggle();
    }

    @Override
    public RotationsFeatureConfig.RotationMode getRotationMode() {
        return rotation.get();
    }

    @Override
    public double getRotationSpeed() {
        return rotationSpeed.get();
    }

    @Override
    public double getRotationEase() {
        return rotationEaseFactor.get();
    }

    @Override
    public double getRotationThreshold() {
        return rotationThreshold.get();
    }

    @Override
    public JitterMode getJitterMode() {
        return jitter.get();
    }

    @Override
    public boolean isMoveFixEnabled() {
        return moveFix.get();
    }

    @Override
    public boolean isRenderEnabled() {
        return render.get();
    }

    @Override
    public int getHoldTicks() {
        return ticksBeforeRelease.get();
    }

    @Override
    public boolean isFutureRotations() {
        if (FUTURE)
            return futureRotations.get();

        return false;
    }
}
