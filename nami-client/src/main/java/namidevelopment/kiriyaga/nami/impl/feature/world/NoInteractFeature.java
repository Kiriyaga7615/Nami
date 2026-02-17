package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PacketSendEvent;
import namidevelopment.kiriyaga.api.event.impl.PlaceBlockEvent;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;

import java.util.Map;

import static namidevelopment.kiriyaga.api.NamiApi.MC;
import static namidevelopment.kiriyaga.api.util.BlockUtils.isBed;

@RegisterFeature
public class NoInteractFeature extends Feature {

    public final WhitelistSetting whitelist = addSetting(new WhitelistSetting("WhiteList", false, WhitelistSetting.Type.BLOCK));
    public final BoolSetting spawnPoint = addSetting(new BoolSetting("SpawnPoint", true));
    public final BoolSetting strip = addSetting(new BoolSetting("Strip", false));
    public final BoolSetting packet = addSetting(new BoolSetting("Packet", false));

    public NoInteractFeature() {
        super("NoInteract", "Prevents you from interacting with certain blocks.", FeatureCategory.of("World"), "antiinteract");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private void onPlaceBlock(PlaceBlockEvent event) {
        LocalPlayer player = event.getPlayer();
        BlockHitResult hitResult = event.getHitResult();

        if (player.level() == null) return;

        Block block = player.level().getBlockState(hitResult.getBlockPos()).getBlock();
        String dimension = player.level().dimensionType().toString();

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);

        if (whitelist.get() && whitelist.contains(blockId.toString())) {
            event.cancel();
            return;
        }

        if (spawnPoint.get()) {
            if (player.level().dimensionType().hasSkyLight() && isBed(block)) {
                event.cancel();
                return;
            }

            if (block == Blocks.RESPAWN_ANCHOR && player.level().dimensionType().hasCeiling()) {
                event.cancel();
                return;
            }
        }

        if (strip.get()) {
            boolean isMain = MC.player.getMainHandItem().is(ItemTags.AXES) && event.getHand() == InteractionHand.MAIN_HAND;
            boolean isOff = MC.player.getOffhandItem().is(ItemTags.AXES) && event.getHand() == InteractionHand.OFF_HAND;
            if (isMain || isOff) {

                Map<Block, Block> strippables = net.fabricmc.fabric.impl.content.registry.util.ImmutableCollectionUtils.getAsMutableMap(
                        () -> net.fabricmc.fabric.mixin.content.registry.AxeItemAccessor.getStrippedBlocks(),
                        map -> net.fabricmc.fabric.mixin.content.registry.AxeItemAccessor.setStrippedBlocks(map)
                );

                if (strippables.containsKey(block)) {
                    event.cancel();
                    return;
                }
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private void onPacketSendRespawn(PacketSendEvent ev) {
        if (!packet.get()) return;

        if (!(ev.getPacket() instanceof ServerboundUseItemOnPacket interactPacket)) return;
        if (MC.level == null) return;

        BlockPos pos = interactPacket.getHitResult().getBlockPos();
        Block block = MC.level.getBlockState(pos).getBlock();
        var dimension = MC.level.dimensionTypeRegistration().is(BuiltinDimensionTypes.OVERWORLD) ? "overworld"
                : MC.level.dimensionTypeRegistration().is(BuiltinDimensionTypes.NETHER) ? "nether"
                : MC.level.dimensionTypeRegistration().is(BuiltinDimensionTypes.END) ? "end"
                : "unknown";

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);

        if (whitelist.get() && whitelist.contains(blockId.toString())) {
            ev.cancel();
            return;
        }

        if (spawnPoint.get()) {
            if (isBed(block) && dimension.equals("overworld")) {
                ev.cancel();
                return;
            }

            if (block == Blocks.RESPAWN_ANCHOR && dimension.equals("nether")) {
                ev.cancel();
                return;
            }
        }

        if (strip.get()) {
            boolean isMain = MC.player.getMainHandItem().is(ItemTags.AXES) && interactPacket.getHand() == InteractionHand.MAIN_HAND;
            boolean isOff = MC.player.getOffhandItem().is(ItemTags.AXES) && interactPacket.getHand() == InteractionHand.OFF_HAND;

            if (isMain || isOff) { // this is fucking shizo
                Map<Block, Block> strippables = net.fabricmc.fabric.impl.content.registry.util.ImmutableCollectionUtils.getAsMutableMap(
                        () -> net.fabricmc.fabric.mixin.content.registry.AxeItemAccessor.getStrippedBlocks(),
                        map -> net.fabricmc.fabric.mixin.content.registry.AxeItemAccessor.setStrippedBlocks(map)
                );

                if (strippables.containsKey(block)) {
                    ev.cancel();
                    return;
                }
            }
        }

    }
}
