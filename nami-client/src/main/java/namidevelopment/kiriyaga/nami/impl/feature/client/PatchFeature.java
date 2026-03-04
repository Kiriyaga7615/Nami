package namidevelopment.kiriyaga.nami.impl.feature.client;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketReceiveEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.event.impl.SprintResetEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.ItemStack;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.NamiApi.CHAT_SERVICE;


@RegisterFeature
public class PatchFeature extends Feature {

    public enum TPSCooldownSync {DISABLED, LAST, AVERAGE}

    public final BoolSetting grimAttackVelocity = addSetting(new BoolSetting("GrimAttackVelocity", false));
    //public final BoolSetting grimNoSlowDisabler = addSetting(new BoolSetting("NoSlowDisabler", false));
    public final BoolSetting slotDragDesync = addSetting(new BoolSetting("SlotDragDesync", false));
    public final BoolSetting silentSwapFix = addSetting(new BoolSetting("SilentSwapFix", false));
    public final BoolSetting setSlotDebug = addSetting(new BoolSetting("SetSlotDebug", false));
    public final EnumSetting<TPSCooldownSync> tpsCooldownSync = addSetting(new EnumSetting<>("TPSCooldownSync", TPSCooldownSync.DISABLED));

    public PatchFeature() {
        super("Patch", "Any kind of hotfixes you should apply based on what server and ac u on.", FeatureCategory.of("Client"));
        if (!this.isEnabled())
            this.toggle();
        setSlotDebug.setShow(false);
    }

    @Override
    public void onDisable(){
        if (!this.isEnabled())
            this.toggle();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onSprintResetEvent(SprintResetEvent event) {
        if (grimAttackVelocity.get() && !event.isCancelled())
                event.cancel();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        Packet<?> p = event.getPacket();

        if (silentSwapFix.get() && p instanceof ClientboundContainerSetSlotPacket packet) {
            if (MC.player != null) {
                if (setSlotDebug.get()) {
                MC.execute(() -> {
                    CHAT_SERVICE.sendPersistent("ContainerID: ", "ContainerID: " +packet.getContainerId()+"");
                    CHAT_SERVICE.sendPersistent("StateID: ", "StateID: " +packet.getStateId()+"");
                    CHAT_SERVICE.sendPersistent("Item: ", "Item: " +packet.getItem()+"");
                    CHAT_SERVICE.sendPersistent("Slot: ", "Slot: " +packet.getSlot()+"");
                    CHAT_SERVICE.sendPersistent("Type: ", "Type: " +packet.type()+"");

                });

                }
                if (packet.getContainerId() == 0) { // only player inventory, syncid of player inventory is always 0
                    int slot = packet.getSlot();

                    if (slot >= 36 && slot <= 44) { // onlu hotbar
                        ItemStack packetStack = packet.getItem();
                        ItemStack handStack = MC.player.getMainHandItem();

                        if (!packetStack.isEmpty() && !handStack.isEmpty()) {
                            if (ItemStack.isSameItem(packetStack, handStack) && packetStack.getCount() == handStack.getCount()) {
                                if (setSlotDebug.get()) {

                                    MC.execute(() -> {
                                        CHAT_SERVICE.sendPersistent("1", "canceled yo");
                                    });
                                }
                                event.cancel(); // TODO: maybe delay it to 2 ticks instead of canceling, like in mio
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
