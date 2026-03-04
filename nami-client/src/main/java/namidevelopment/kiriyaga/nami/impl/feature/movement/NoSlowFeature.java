package namidevelopment.kiriyaga.nami.impl.feature.movement;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.ItemUseSlowEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.PacketUtils.sendSequencedPacket;

@RegisterFeature
public class NoSlowFeature extends Feature {
    public enum Mode {NONE, VANILLA, GRIMV3, GRIM}

    public enum InvMove {NONE, WAIT, STOP}

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>("Mode", Mode.VANILLA));
    public final BoolSetting items = addSetting(new BoolSetting("Items", true));
    //public final EnumSetting<InvMove> invMove = addSetting(new EnumSetting<>("MultiAction", InvMove.NONE));
    public final BoolSetting fastCrawl = addSetting(new BoolSetting("FastCrawl", false));
    //public final BoolSetting fastWeb = addSetting(new BoolSetting("fast web", false));
    public final BoolSetting onlyOnGround = addSetting(new BoolSetting("OnlyOnGround", true));


    public NoSlowFeature() {
        super("NoSlow", "Reduces slowdown effect caused on player.", FeatureCategory.of("Movement"), "noslow");
        items.setShowCondition(()-> mode.get() != Mode.NONE);
        onlyOnGround.setShowCondition(()-> mode.get() != Mode.NONE);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onItemUseSlowEvent(ItemUseSlowEvent ev){
        if (!items.get() || MC.player == null || MC.level == null || !MC.player.isUsingItem() || MC.player.isFallFlying() || MC.player.isHandsBusy() || MC.player.isShiftKeyDown())
            return;

        if (onlyOnGround.get() && !MC.player.onGround())
            return;

        if (mode.get() == Mode.VANILLA || mode.get() == Mode.GRIM){
            ev.cancel();
            return;
        }

        boolean boost = true; //cattyngmd
        if (mode.get() == Mode.GRIMV3){
            boost = MC.player.tickCount % 3 == 0 || MC.player.tickCount % 4 == 0;
            //if (MC.player.age % 12 == 0) boost = false;
            if (boost){
                ev.cancel();
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private void onPreTick(PreTickEvent event) {
        if (mode.get() == Mode.GRIM && MC.player.isUsingItem() && !MC.player.isShiftKeyDown() && items.get()) {

            if (isFood(MC.player.getActiveItem())) {
                float yaw = ROTATION_SERVICE.getStateHandler().getServerYRot();
                float pitch = ROTATION_SERVICE.getStateHandler().getServerXRot();

                if (MC.player.getUsedItemHand() == InteractionHand.MAIN_HAND)
                    sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, yaw, pitch));
                else
                    sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, yaw, pitch));

            }
        }
    }

//    @SubscribeEvent(priority = EventPriority.LOW)
//    private void onPreTick(PreTickEvent event) {
//        if (!fastWeb.get()) return;
//
//        BlockPos webPos = getPhasedWebBlock();
//        if (webPos != null) {
//            //CHAT_SERVICE.sendRaw("c");
//            MC.world.setBlockState(webPos, Blocks.AIR.getDefaultState(), 3);
//        }
//    }

    private BlockPos getPhasedWebBlock() {
        if (MC.player == null || MC.level == null) return null;

        AABB bb = MC.player.getBoundingBox();

        int minX = Mth.floor(bb.minX);
        int maxX = Mth.ceil(bb.maxX);
        int minY = Mth.floor(bb.minY);
        int maxY = Mth.ceil(bb.maxY);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.ceil(bb.maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (MC.level.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private boolean isFood(ItemStack stack) {
        if (stack.isEmpty())
            return false;

        Item item = stack.getItem();

        return item.components().has(DataComponents.FOOD);
    }
}
