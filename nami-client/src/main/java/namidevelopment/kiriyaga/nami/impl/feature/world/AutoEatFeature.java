package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;

import java.util.concurrent.atomic.AtomicBoolean;

import static namidevelopment.kiriyaga.api.NamiApi.INVENTORY_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.MC;

@RegisterFeature
public class AutoEatFeature extends Feature {

    public final IntSetting swapDelayTicksSetting = addSetting(new IntSetting("Delay", 5, 1, 20));
    public final DoubleSetting minHunger = addSetting(new DoubleSetting("Hunger", 19.0, 0.0, 19.0));
    public final DoubleSetting minHealth = addSetting(new DoubleSetting("Health", 0.0, 0.0, 19.0));
    public final BoolSetting allowGapples = addSetting(new BoolSetting("Gapples", true));
    public final BoolSetting allowPoisoned = addSetting(new BoolSetting("Poisoned", false));

    public final AtomicBoolean eating = new AtomicBoolean(false);
    private volatile int swapCooldown = 0;

    public AutoEatFeature() {
        super("AutoEat", "Automatically eats best food.", FeatureCategory.of("World"), "autoeat");
    }

    @Override
    public void onDisable() {
        eating.set(false);
        eating.set(false);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null) return;

        if (eating.get())
            MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);

        double hunger = MC.player.getFoodData().getFoodLevel();
        double health = MC.player.getHealth();
        boolean needsEat = hunger < minHunger.get() || health < minHealth.get();
        if (!needsEat) {
            eating.set(false);
            return;
        }

        int bestSlot = getBestFoodSlot();
        if (bestSlot == -1) {
            eating.set(false);
            return;
        }

        int currentSlot = MC.player.getInventory().getSelectedSlot();

        if (currentSlot == bestSlot) {
            eating.set(true);
        } else {
            if (swapCooldown > 0) {
                swapCooldown--;
            } else {
                INVENTORY_SERVICE.getSwapHandler().attemptSwitch(bestSlot, false);
                swapCooldown = swapDelayTicksSetting.get();
            }
            eating.set(false);
        }
    }

//            @SubscribeEvent(priority = EventPriority.HIGH)
//        private void onPlaceBlock(PlaceBlockEvent event) {
//            if (MC.player != null && MC.world != null && eating.get())
//                event.cancel();
//        }

    private int getBestFoodSlot() {
        int bestSlot = -1;
        float bestScore = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            float score = getFoodScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private float getFoodScore(ItemStack stack) {
        if (stack.isEmpty() || !stack.getItem().components().has(DataComponents.FOOD)) {
            return -1;
        }

        Item item = stack.getItem();

        if (!allowPoisoned.get() && isPoisonedFood(item)) {
            return -1;
        }

        if (!allowGapples.get() && isGapple(item)) {
            return -1;
        }

        FoodProperties food = item.components().get(DataComponents.FOOD);
        float nutrition = food.nutrition();
        float saturation = food.saturation();
        float totalValue = nutrition + saturation;

        if (isGapple(item)) {
            return allowGapples.get() ? totalValue - 0.5f : -1;
        }

        if (isPoisonedFood(item)) {
            return allowPoisoned.get() ? totalValue - 1.5f : -1;
        }

        return totalValue;
    }

    private boolean isGapple(Item item) {
        return item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private boolean isPoisonedFood(Item item) {
        return item == Items.ROTTEN_FLESH
                || item == Items.PUFFERFISH
                || item == Items.SPIDER_EYE
                || item == Items.CHORUS_FRUIT;
    }
}
