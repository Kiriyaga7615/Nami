package me.kiriyaga.nami.feature.module.impl.visuals;

    import me.kiriyaga.nami.event.EventPriority;
    import me.kiriyaga.nami.event.SubscribeEvent;
    import me.kiriyaga.nami.event.impl.PostTickEvent;
    import me.kiriyaga.nami.feature.module.Module;
    import me.kiriyaga.nami.feature.module.ModuleCategory;
    import me.kiriyaga.nami.feature.module.RegisterModule;
    import me.kiriyaga.nami.mixininterface.ISimpleOption;
    import me.kiriyaga.nami.feature.setting.impl.DoubleSetting;
    import me.kiriyaga.nami.feature.setting.impl.EnumSetting;
    import net.minecraft.world.effect.MobEffectInstance;
    import net.minecraft.world.effect.MobEffects;

    import static me.kiriyaga.nami.Nami.MC;

    @RegisterModule
    public class FullbrightModule extends Module {

        public enum Mode {
            GAMMA, POTION
        }

        public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.GAMMA));
        public final DoubleSetting amount = addSetting(new DoubleSetting("Amount", 2, 1, 25));


        public FullbrightModule() {
            super("Fullbright", "Modifies your game brightness", ModuleCategory.of("Render"), "autogamma", "gamma", "autogmam");
            amount.setShowCondition(() -> mode.get() == Mode.GAMMA);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        private void onTick(PostTickEvent ev) {
            if (MC.options == null || MC.player == null) return;

            if (mode.get() == Mode.GAMMA) {
                double current = MC.options.getGamma().getValue();
                if (current != amount.get()) {
                    ((ISimpleOption) (Object) MC.options.getGamma()).setValue(amount.get());
                }
                if (MC.player.hasStatusEffect(MobEffects.NIGHT_VISION)) {
                    MC.player.removeStatusEffect(MobEffects.NIGHT_VISION);
                }
            }
            else if (mode.get() == Mode.POTION) {
                if (MC.options.getGamma().getValue() > 1.0)
                    ((ISimpleOption) (Object) MC.options.getGamma()).setValue(1.0);
                MC.player.addStatusEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, false));
            }
        }

        @Override
        public void onDisable() {
            super.onDisable();
            if (MC.options != null) {
                ((ISimpleOption) (Object) MC.options.getGamma()).setValue(1.0);
            }
            if (MC.player != null && MC.player.hasStatusEffect(MobEffects.NIGHT_VISION)) {
                MC.player.removeStatusEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
