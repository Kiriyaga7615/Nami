package namidevelopment.kiriyaga.nami.impl.feature.visuals;

    import namidevelopment.kiriyaga.api.event.EventPriority;
    import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
    import namidevelopment.kiriyaga.api.event.impl.PostTickEvent;
    import namidevelopment.kiriyaga.api.model.feature.Feature;
    import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
    import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
    import namidevelopment.kiriyaga.nami.mixininterface.ISimpleOption;
    import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
    import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
    import net.minecraft.world.effect.MobEffectInstance;
    import net.minecraft.world.effect.MobEffects;

    import static namidevelopment.kiriyaga.api.NamiApi.MC;

    @RegisterFeature
    public class FullbrightFeature extends Feature {

        public enum Mode {
            GAMMA, POTION
        }

        public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.GAMMA));
        public final DoubleSetting amount = addSetting(new DoubleSetting("Amount", 2, 1, 25));


        public FullbrightFeature() {
            super("Fullbright", "Modifies your game brightness", FeatureCategory.of("Render"), "autogamma", "gamma", "autogmam");
            amount.setShowCondition(() -> mode.get() == Mode.GAMMA);
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        private void onTick(PostTickEvent ev) {
            if (MC.options == null || MC.player == null) return;

            if (mode.get() == Mode.GAMMA) {
                double current = MC.options.gamma().get();
                if (current != amount.get()) {
                    ((ISimpleOption) (Object) MC.options.gamma()).setValue(amount.get());
                }
                if (MC.player.hasEffect(MobEffects.NIGHT_VISION)) {
                    MC.player.removeEffect(MobEffects.NIGHT_VISION);
                }
            }
            else if (mode.get() == Mode.POTION) {
                if (MC.options.gamma().get() > 1.0)
                    ((ISimpleOption) (Object) MC.options.gamma()).setValue(1.0);
                MC.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, false));
            }
        }

        @Override
        public void onDisable() {
            super.onDisable();
            if (MC.options != null) {
                ((ISimpleOption) (Object) MC.options.gamma()).setValue(1.0);
            }
            if (MC.player != null && MC.player.hasEffect(MobEffects.NIGHT_VISION)) {
                MC.player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
