package namidevelopment.kiriyaga.nami.impl.feature.impl.combat;

import namidevelopment.kiriyaga.nami.api.executable.model.ExecutableThreadType;
import namidevelopment.kiriyaga.nami.event.EventPriority;
import namidevelopment.kiriyaga.nami.event.SubscribeEvent;
import namidevelopment.kiriyaga.nami.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.nami.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.nami.impl.feature.FeatureCategory;
import namidevelopment.kiriyaga.nami.impl.feature.Feature;
import namidevelopment.kiriyaga.nami.impl.feature.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.setting.impl.BoolSetting;
import namidevelopment.kiriyaga.nami.impl.setting.impl.EnumSetting;
import namidevelopment.kiriyaga.nami.impl.setting.impl.IntSetting;
import namidevelopment.kiriyaga.nami.util.BlockUtils;
import namidevelopment.kiriyaga.nami.util.EnchantmentUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.nami.util.entity.PlayerUtils.isItemAWeapon;

@RegisterFeature
public class AutoTotemFeature extends Feature {

    private enum Offhand { CRYSTAL, GAPPLE, ITEMFRAME, MENDING, TOTEM}

    public final IntSetting health = addSetting(new IntSetting("Health", 12, 2, 36));
    public final IntSetting holeHealth = addSetting(new IntSetting("HoleHealth", 8, 2, 36));
    public final BoolSetting offhandOverride = addSetting(new BoolSetting("Override", false));
    public final EnumSetting<Offhand> overrideItem = addSetting(new EnumSetting<>("Item", Offhand.CRYSTAL));
    public final BoolSetting swordGap = addSetting(new BoolSetting("SwordGap", true));
    public final BoolSetting fastSwap = addSetting(new BoolSetting("Alternative", false));
    public final BoolSetting mainhand = addSetting(new BoolSetting("Mainhand", false));
    public final BoolSetting mainhandGapple = addSetting(new BoolSetting("MainhandGapple", false));
    public final IntSetting mainhandSlot = addSetting(new IntSetting("Slot", 8, 0, 8));
    public final BoolSetting deathLog = addSetting(new BoolSetting("Log", false));

    private final Map<String, String> deathReasons = new ConcurrentHashMap<>();

    private long lastAttemptTime = 0;
    private int totemCount = 0;

    public AutoTotemFeature() {
        super("AutoTotem", "Automatically places totem in your hand.", FeatureCategory.of("Combat"), "autototem");
        mainhandSlot.setShowCondition(mainhand::get);
        overrideItem.setShowCondition(offhandOverride::get);
        swordGap.setShowCondition(offhandOverride::get);
    }

    private int healthSetting() { //me couldn't think of better name
        return BlockUtils.isInHole(MC.player, true) || BlockUtils.isInHole1x1(MC.player) ?
                holeHealth.get() : health.get();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreTick(PreTickEvent event) {
        if (MC.level == null || MC.player == null) return;
        this.clearDisplayInfo();

        int totemCount = 0;
        for (ItemStack stack : MC.player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                totemCount += stack.getCount();
            }
        }
        ItemStack offHandStack = MC.player.getOffhandItem();
        if (offHandStack.getItem() == Items.TOTEM_OF_UNDYING) {
            totemCount += offHandStack.getCount();
        }
        this.addDisplayInfo(String.valueOf(totemCount));

        attemptPlaceOffhand();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketReceiveEvent event) {
        if (MC.level == null || MC.player == null) return;

        if (event.getPacket() instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEntity(MC.level) == MC.player && packet.getEventId() == 3 && deathLog.get()) {
                EXECUTABLE_SERVICE.getRequestHandler().submit(this::logDeathData, 20, ExecutableThreadType.PRE_TICK);
            }
        }
    }

    private void attemptPlaceOffhand() {
        LocalPlayer player = MC.player;
        if (player == null) return;

        ItemStack offhandStack = player.getOffhandItem();
        ItemStack targetStack = null;
        boolean overrideActive = false;

        if (offhandOverride.get()) {
            int effectiveHealth = (int) (player.getHealth() + player.getAbsorptionAmount());
            if (effectiveHealth >= healthSetting()) {
                targetStack = getOverrideStack();
                if (targetStack != null) {
                    overrideActive = true;
                }
            }
        }

        if (targetStack == null) {
            targetStack = findTotemStack();
            if (targetStack == null) return;
        }

        if (mainhand.get()) {
            boolean useGapple = mainhandGapple.get() && MC.options.keyUse.isDown() && MC.player.getInventory().getSelectedSlot() == mainhandSlot.get();

            if (useGapple) {
                int gappleSlot = findInventorySlot(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), mainhandSlot.get());
                if (gappleSlot == -1) {
                    gappleSlot = findInventorySlot(new ItemStack(Items.GOLDEN_APPLE), mainhandSlot.get());
                }

                if (gappleSlot != -1 && MC.player.getInventory().getItem(mainhandSlot.get()).getItem() != Items.ENCHANTED_GOLDEN_APPLE
                        && MC.player.getInventory().getItem(mainhandSlot.get()).getItem() != Items.GOLDEN_APPLE) {
                    if (fastSwap.get()) {
                        INVENTORY_SERVICE.getClickHandler().swapSlot(convertSlot(gappleSlot), mainhandSlot.get());
                        lastAttemptTime = System.currentTimeMillis();
                    } else {
                        clickSlot(gappleSlot, convertSlot(mainhandSlot.get()));
                        lastAttemptTime = System.currentTimeMillis();
                    }
                }
            } else {
                if (MC.player.getInventory().getItem(mainhandSlot.get()).getItem() != Items.TOTEM_OF_UNDYING) {
                    int totem = findInventorySlot(new ItemStack(Items.TOTEM_OF_UNDYING), mainhandSlot.get());
                    if (totem != -1) {
                        if (fastSwap.get()) {
                            INVENTORY_SERVICE.getClickHandler().swapSlot(convertSlot(totem), mainhandSlot.get());
                            lastAttemptTime = System.currentTimeMillis();
                        } else {
                            clickSlot(totem, convertSlot(mainhandSlot.get()));
                            lastAttemptTime = System.currentTimeMillis();
                        }
                    }
                }
            }


            if (MC.player.getHealth() + MC.player.getAbsorptionAmount() <= healthSetting() && MC.player.getInventory().getItem(mainhandSlot.get()).getItem() == Items.TOTEM_OF_UNDYING)
                INVENTORY_SERVICE.getSlotHandler().attemptSwitch(mainhandSlot.get());
        }

        int targetSlot;

        if (mainhand.get())
            targetSlot = findInventorySlot(targetStack, mainhandSlot.get());
        else
            targetSlot = findInventorySlot(targetStack);

        if (targetSlot == -1) return;

        if (offhandStack.getItem() == targetStack.getItem()) return;

        if (fastSwap.get()) {
            if (offhandStack.getItem() != targetStack.getItem()) {
                INVENTORY_SERVICE.getClickHandler().swapSlot(convertSlot(targetSlot), 40);
                lastAttemptTime = System.currentTimeMillis();
            }
        } else {
            clickSlot(targetSlot, 45);
            lastAttemptTime = System.currentTimeMillis();
        }

        totemCount = countTotems();
        addDisplayInfo("" + totemCount);
    }

    private ItemStack findTotemStack() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) return stack;
        }
        return null;
    }

    private ItemStack getOverrideStack() {
        Offhand type = overrideItem.get();
        LocalPlayer player = MC.player;

        if (swordGap.get()
                && MC.player.getHealth() + MC.player.getAbsorptionAmount() >= healthSetting()
                && isItemAWeapon(MC.player.getInventory().getSelectedItem())
                && MC.options.keyUse.isDown())
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);

        switch (type) {
            case TOTEM:
                return new ItemStack(Items.TOTEM_OF_UNDYING);
            case CRYSTAL:
                return new ItemStack(Items.END_CRYSTAL);
            case GAPPLE:
                return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
            case ITEMFRAME:
                return new ItemStack(Items.ITEM_FRAME);
            case MENDING:
                if (hasMending(MC.player.getOffhandItem()) && !isFullyRepaired(MC.player.getOffhandItem())) return null;

                for (int i = 0; i < 36; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.isEmpty()) continue;

                    if (stack.is(ItemTags.HEAD_ARMOR)) continue;
                    if (stack.is(ItemTags.CHEST_ARMOR)) continue;
                    if (stack.is(ItemTags.LEG_ARMOR)) continue;
                    if (stack.is(ItemTags.FOOT_ARMOR)) continue;

                    if (!hasMending(stack)) continue;
                    if (isFullyRepaired(stack)) continue;

                    return stack;
                }
                break;
        }
        return null;
    }

    private void clickSlot(int invSlot, int index) {
        int realSlot = convertSlot(invSlot);

        ItemStack cursor = MC.player.containerMenu.getCarried();

        if (cursor.isEmpty()) {
            INVENTORY_SERVICE.getClickHandler().pickupSlot(realSlot);
            cursor = MC.player.containerMenu.getCarried();
        }

        if (!cursor.isEmpty()) {
            INVENTORY_SERVICE.getClickHandler().pickupSlot(index);
        }
    }

    private int countTotems() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack != null && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int convertSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    public void addDeathReason(String key, String reasonDescription) {
        deathReasons.put(key, reasonDescription);
    }

    public void removeDeathReason(String key) {
        deathReasons.remove(key);
    }

    public void clearDeathReasons() {
        deathReasons.clear();
    }

    private void logDeathData() {
        LocalPlayer player = MC.player;
        if (player == null) return;

        int ping = SERVER_SERVICE.getPing();
        boolean hasTotem = totemCount > 0;
        long timeSinceLastSwap = System.currentTimeMillis() - lastAttemptTime;

        if (!hasTotem) {
            addDeathReason("notots", "NO_TOTEMS");
        } else {
            removeDeathReason("notots");
        }

        if (ping > 125) {
            addDeathReason("highping", "HIGH_PING " + ping + " ms");
        } else {
            removeDeathReason("highping");
        }

        if (deathReasons.isEmpty()) {
            addDeathReason("unknown", "UNKNOWN_CAUSE");
        } else {
            removeDeathReason("unknown");
        }

        StringBuilder reasonsBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : deathReasons.entrySet()) {
            reasonsBuilder.append("- ").append(entry.getValue()).append("\n");
        }

        boolean pendingTotem = false;
        Component message = CAT_FORMAT.format(
                "\n=== {g}AutoTotem{reset} ===\n" +
                        "Death reasons:\n{g}" + reasonsBuilder.toString() + "{reset}\n" +
                        "Ping: {g}" + ping + " ms{reset}\n" +
                        "Totems Available: {g}" + totemCount + "{reset}\n" +
                        "Pending Totem: {g}" + pendingTotem + "{reset}\n" +
                        "Last Swap Attempt: {g}" + timeSinceLastSwap + " ms ago{reset}\n" +
                        "============================"
        );

        CHAT_SERVICE.sendPersistent(AutoTotemFeature.class.getName(), message);
    }

    private int findInventorySlot(ItemStack stack) {
        for (int i = 0; i < 36; i++) {
            if (MC.player.getInventory().getItem(i).getItem() == stack.getItem()) {
                return i;
            }
        }
        return -1;
    }

    private int findInventorySlot(ItemStack stack, int excluded) {
        for (int i = 0; i < 36; i++) {
            if (i == excluded) continue;
            if (MC.player.getInventory().getItem(i).getItem() == stack.getItem()) {
                return i;
            }
        }
        return -1;
    }


    private boolean hasMending(ItemStack stack) {
        return EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.MENDING) > 0;
    }

    private boolean isFullyRepaired(ItemStack stack) {
        if (!stack.isDamageableItem()) return true;
        return stack.getDamageValue() == 0;
    }
}