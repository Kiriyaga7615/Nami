package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.ColorUtils;
import namidevelopment.kiriyaga.nami.impl.feature.combat.AutoPotFeature;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterFeature
public class PotCounterHudFeature extends HudElementFeature {

    public final EnumSetting<AutoPotFeature.Pot> potEffect = addSetting(new EnumSetting<>("Effect", AutoPotFeature.Pot.RESISTANCE));
    public final IntSetting amplifier = addSetting(new IntSetting("Amplifier", 1, 0, 4));

    public PotCounterHudFeature() {super("PotCounter", "Displays potion count and active effect time.", 0, 0, 100, 20);
    }

    @Override
    public Component getDisplayText() {
        if (MC.player == null || MC.level == null) {
            return Component.empty();
        }
        int count = getPots();
        int seconds = getDuration();

        String countColor = getCountColor(count);
        String timeColor = getTimeColor(seconds);

        String formatted = countColor + (count == 0 ? " " : count) + "  " + timeColor + (seconds == 0 ? " " : seconds);

        width = FONT_SERVICE.getWidth(formatted.replaceAll("\\{.*?}", ""));
        height = FONT_SERVICE.getHeight();

        return CAT_FORMAT.format(formatted);
    }

    private int getPots() {
        int requiredAmp = amplifier.get();
        Holder<MobEffect> target = potEffect.get().getEffect();
        int p = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.getItem() != Items.SPLASH_POTION) continue;
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) continue;

            for (MobEffectInstance inst : contents.getAllEffects()) {
                if (inst.getEffect() == target && inst.getAmplifier() >= requiredAmp) {
                    p += stack.getCount();
                    break;
                }
            }
        }
        return p;
    }

    private int getDuration() {
        Holder<MobEffect> target = potEffect.get().getEffect();
        MobEffectInstance instance = MC.player.getEffect(target);
        if (instance == null) return 0;
        return instance.getDuration() / 20;
    }

    private String getCountColor(int count) {
        if (count >= 8) return "{green}";
        if (count >= 5) return "{yellow}";
        if (count >= 3) return "{gold}";
        if (count >= 1) return "{red}";
        return "{dark_red}";
    }

    private String getTimeColor(int seconds) {
        int clamped = Math.min(seconds, 60);

        if (clamped >= 50) return "{green}";
        if (clamped >= 35) return "{yellow}";
        if (clamped >= 20) return "{gold}";
        if (clamped >= 10) return "{red}";
        return "{dark_red}";
    }
}