package namidevelopment.kiriyaga.api.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.BlockUtils.isPlaceable;


public class BlockUtils {
    private static final Block AIR_BLOCK = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", "air"));
    private static final Block VOID_AIR_BLOCK = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", "void_air"));


    private static final Set<String> NON_VANILLA_GENERATED_IDS = Set.of(
            "minecraft:crafting_table",
            "minecraft:enchanting_table",
            "minecraft:stonecutter",
            "minecraft:white_bed",
            "minecraft:orange_bed",
            "minecraft:magenta_bed",
            "minecraft:light_blue_bed",
            "minecraft:yellow_bed",
            "minecraft:lime_bed",
            "minecraft:pink_bed",
            "minecraft:gray_bed",
            "minecraft:light_gray_bed",
            "minecraft:cyan_bed",
            "minecraft:purple_bed",
            "minecraft:blue_bed",
            "minecraft:brown_bed",
            "minecraft:green_bed",
            "minecraft:red_bed",
            "minecraft:black_bed",
            "minecraft:loom",
            "minecraft:fletching_table",
            "minecraft:cauldron",
            "minecraft:composter",
            "minecraft:jukebox",
            "minecraft:bell",
            "minecraft:respawn_anchor",
            "minecraft:lodestone",
            "minecraft:conduit",
            "minecraft:beacon",
            "minecraft:end_portal_frame",
            "minecraft:end_gateway",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:jigsaw",
            "minecraft:barrier",
            "minecraft:light",
            "minecraft:spawner",
            "minecraft:armor_stand",
            "minecraft:painting",
            "minecraft:daylight_detector",
            "minecraft:note_block",
            "minecraft:redstone_lamp",
            "minecraft:tnt",
            "minecraft:piston",
            "minecraft:sticky_piston",
            "minecraft:observer",
            "minecraft:target",
            "minecraft:tripwire_hook",
            "minecraft:lever",
            "minecraft:redstone_torch",
            "minecraft:repeater",
            "minecraft:comparator",
            "minecraft:crafter",
            "minecraft:chiseled_bookshelf",
            "minecraft:decorated_pot",
            "minecraft:oak_sign",
            "minecraft:spruce_sign",
            "minecraft:birch_sign",
            "minecraft:jungle_sign",
            "minecraft:acacia_sign",
            "minecraft:dark_oak_sign",
            "minecraft:mangrove_sign",
            "minecraft:cherry_sign",
            "minecraft:bamboo_sign",
            "minecraft:crimson_sign",
            "minecraft:warped_sign",
            "minecraft:oak_hanging_sign",
            "minecraft:spruce_hanging_sign",
            "minecraft:birch_hanging_sign",
            "minecraft:jungle_hanging_sign",
            "minecraft:acacia_hanging_sign",
            "minecraft:dark_oak_hanging_sign",
            "minecraft:mangrove_hanging_sign",
            "minecraft:cherry_hanging_sign",
            "minecraft:bamboo_hanging_sign",
            "minecraft:crimson_hanging_sign",
            "minecraft:warped_hanging_sign",
            "minecraft:sculk_catalyst",
            "minecraft:sculk_sensor",
            "minecraft:calibrated_sculk_sensor",
            "minecraft:sculk_shrieker"
    );


    private static final Set<Identifier> NON_VANILLA_GENERATED_IDENTIFIERS = new HashSet<>();

    static {
        for (String id : NON_VANILLA_GENERATED_IDS) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier != null) {
                NON_VANILLA_GENERATED_IDENTIFIERS.add(identifier);
            }
        }
    }

    public static boolean isNonVanillaGenerated(Block block) {
        if (block == null) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return NON_VANILLA_GENERATED_IDENTIFIERS.contains(id);
    }

    public static Set<Block> getNonVanillaGeneratedBlocks() {
        Set<Block> blocks = new HashSet<>();
        for (Identifier id : NON_VANILLA_GENERATED_IDENTIFIERS) {
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null && block != AIR_BLOCK && block != VOID_AIR_BLOCK) {
                blocks.add(block);
            }
        }
        return blocks;
    }


    private static final Set<String> STORAGE_IDS = Set.of(
            "minecraft:furnace",
            "minecraft:blast_furnace",
            "minecraft:smoker",
            "minecraft:anvil",
            "minecraft:chipped_anvil",
            "minecraft:damaged_anvil",
            "minecraft:brewing_stand",
            "minecraft:grindstone",
            "minecraft:smithing_table",
            "minecraft:cartography_table",
            "minecraft:lectern",
            "minecraft:chest",
            "minecraft:trapped_chest",
            "minecraft:ender_chest",
            "minecraft:barrel",
            "minecraft:shulker_box",
            "minecraft:white_shulker_box",
            "minecraft:orange_shulker_box",
            "minecraft:magenta_shulker_box",
            "minecraft:light_blue_shulker_box",
            "minecraft:yellow_shulker_box",
            "minecraft:lime_shulker_box",
            "minecraft:pink_shulker_box",
            "minecraft:gray_shulker_box",
            "minecraft:light_gray_shulker_box",
            "minecraft:cyan_shulker_box",
            "minecraft:purple_shulker_box",
            "minecraft:blue_shulker_box",
            "minecraft:brown_shulker_box",
            "minecraft:green_shulker_box",
            "minecraft:red_shulker_box",
            "minecraft:black_shulker_box",
            "minecraft:hopper",
            "minecraft:dispenser",
            "minecraft:dropper"
    );


    private static final Set<Identifier> STORAGE_IDENTIFIERS = new HashSet<>();

    static {
        for (String id : STORAGE_IDS) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier != null) {
                STORAGE_IDENTIFIERS.add(identifier);
            }
        }
    }

    public static boolean isStorage(Block block) {
        if (block == null) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return STORAGE_IDENTIFIERS.contains(id);
    }

    public static Set<Block> getStorage() {
        Set<Block> blocks = new HashSet<>();
        for (Identifier id : STORAGE_IDENTIFIERS) {
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null && block != AIR_BLOCK && block != VOID_AIR_BLOCK) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    public static List<BlockPos> getSurround(Player player, int offset, boolean extension) {
        Set<BlockPos> positions = new HashSet<>();

        AABB bb = player.getBoundingBox();
        int yLegs = (int) Math.floor(player.getY());
        List<BlockPos> inside = new ArrayList<>();
        for (int x = (int) Math.floor(bb.minX); x < Math.ceil(bb.maxX); x++) {
            for (int z = (int) Math.floor(bb.minZ); z < Math.ceil(bb.maxZ); z++) {
                inside.add(new BlockPos(x, yLegs, z));
            }
        }

        for (BlockPos base : inside)
            addSurroundForBase(base, positions);

        int yOffseted = yLegs + offset;
        List<BlockPos> faceLevel = new ArrayList<>();
        for (int x = (int) Math.floor(bb.minX); x < Math.ceil(bb.maxX); x++) {
            for (int z = (int) Math.floor(bb.minZ); z < Math.ceil(bb.maxZ); z++) {
                faceLevel.add(new BlockPos(x, yOffseted, z));
            }
        }
        for (BlockPos base : faceLevel)
            addSurroundForBase(base, positions);

        expand(positions, player, extension);

        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : positions)
            if (!isPlaceable(pos))
                result.add(pos);

        return result;
    }

    public static  void addSurroundForBase(BlockPos base, Set<BlockPos> positions) {
        BlockPos below = base.below();
        positions.add(below);

        BlockPos north = base.north();
        BlockPos south = base.south();
        BlockPos east  = base.east();
        BlockPos west  = base.west();

        positions.add(north);
        positions.add(south);
        positions.add(east);
        positions.add(west);
    }

    public static  void expand(Set<BlockPos> positions, Player player, boolean extension) {
        if (!extension)
            return;

        Set<BlockPos> extra = new HashSet<>();
        for (BlockPos pos : positions) {
            AABB blockBox = new AABB(pos);
            for (Entity entity : MC.level.entitiesForRendering()) {
                if (entity.distanceToSqr(player) > 10) continue;
                if (entity instanceof EndCrystal) continue;
                if (entity instanceof ItemEntity) continue;
                if (entity instanceof Arrow) continue;

                if (entity.getBoundingBox().intersects(blockBox)) {
                    int entY = (int) Math.floor(entity.getY());
                    AABB entBox = entity.getBoundingBox();
                    for (int x = (int) Math.floor(entBox.minX); x < Math.ceil(entBox.maxX); x++) {
                        for (int z = (int) Math.floor(entBox.minZ); z < Math.ceil(entBox.maxZ); z++) {
                            BlockPos entBase = new BlockPos(x, entY, z);
                            addSurroundForBase(entBase, extra);
                        }
                    }
                }
            }
        }
        positions.addAll(extra);
    }

    public static Color getColorByBlockId(BlockState state) { // ai made
            Block block = state.getBlock();
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);

            if (id == null) {
                return Color.WHITE;
            }

            switch (id.toString()) {
                // Печи и кузнечные блоки
                case "minecraft:furnace":
                    return new Color(0x6A6A6A); // серый (камень)
                case "minecraft:blast_furnace":
                    return new Color(0x4A4A4A); // темно-серый
                case "minecraft:smoker":
                    return new Color(0x8B8B5A); // коричневато-серый
                case "minecraft:anvil":
                case "minecraft:chipped_anvil":
                case "minecraft:damaged_anvil":
                    return new Color(0x808080); // металлический серый
                case "minecraft:brewing_stand":
                    return new Color(0x5A5A5A); // темно-серый
                case "minecraft:grindstone":
                    return new Color(0xC2B280); // песочный (дерево + камень)
                case "minecraft:smithing_table":
                    return new Color(0x8B4513); // коричневый (дерево + железо)
                case "minecraft:cartography_table":
                    return new Color(0x8B5A2B); // светло-коричневый (дерево + бумага)
                case "minecraft:lectern":
                    return new Color(0x8B4513); // темно-коричневый (дуб)

                // Контейнеры
                case "minecraft:chest":
                case "minecraft:trapped_chest":
                    return new Color(0xC2A000); // желто-коричневый (дерево)
                case "minecraft:ender_chest":
                    return new Color(0x2D1B5B); // темно-фиолетовый (эндер)
                case "minecraft:barrel":
                    return new Color(0x8B4513); // коричневый (дерево)
                case "minecraft:shulker_box":
                    return new Color(0x946B8E); // пурпурный (шалкер)
                case "minecraft:white_shulker_box":
                    return Color.WHITE;
                case "minecraft:orange_shulker_box":
                    return new Color(0xFF8C00); // оранжевый
                case "minecraft:magenta_shulker_box":
                    return new Color(0xFF00FF); // пурпурный
                case "minecraft:light_blue_shulker_box":
                    return new Color(0xADD8E6); // голубой
                case "minecraft:yellow_shulker_box":
                    return new Color(0xFFFF00); // желтый
                case "minecraft:lime_shulker_box":
                    return new Color(0x00FF00); // лаймовый
                case "minecraft:pink_shulker_box":
                    return new Color(0xFFC0CB); // розовый
                case "minecraft:gray_shulker_box":
                    return new Color(0x808080); // серый
                case "minecraft:light_gray_shulker_box":
                    return new Color(0xD3D3D3); // светло-серый
                case "minecraft:cyan_shulker_box":
                    return new Color(0x00FFFF); // бирюзовый
                case "minecraft:purple_shulker_box":
                    return new Color(0x800080); // фиолетовый
                case "minecraft:blue_shulker_box":
                    return new Color(0x0000FF); // синий
                case "minecraft:brown_shulker_box":
                    return new Color(0x8B4513); // коричневый
                case "minecraft:green_shulker_box":
                    return new Color(0x008000); // зеленый
                case "minecraft:red_shulker_box":
                    return new Color(0xFF0000); // красный
                case "minecraft:black_shulker_box":
                    return Color.BLACK;

                // Механизмы
                case "minecraft:hopper":
                    return new Color(0x808080); // серый (железо)
                case "minecraft:dispenser":
                case "minecraft:dropper":
                    return new Color(0x6A6A6A); // серый (камень + механизмы)

                // Рабочие столы
                case "minecraft:crafting_table":
                    return new Color(0x8B4513); // коричневый (дуб)
                case "minecraft:enchanting_table":
                    return new Color(0x4B0082); // индиго (магия)
                case "minecraft:stonecutter":
                    return new Color(0x808080); // серый (камень)
                case "minecraft:loom":
                    return new Color(0xD2B48C); // бежевый (ткань)
                case "minecraft:fletching_table":
                    return new Color(0x8B5A2B); // светло-коричневый (дерево)
                case "minecraft:cauldron":
                    return new Color(0x404040); // темно-серый (железо)
                case "minecraft:composter":
                    return new Color(0x556B2F); // темно-зеленый (компост)
                case "minecraft:jukebox":
                    return new Color(0x8B0000); // темно-красный (дерево + алмазы)

                // Декоративные и особые блоки
                case "minecraft:bell":
                    return new Color(0xD4AF37); // золотой
                case "minecraft:respawn_anchor":
                    return new Color(0x4B0082); // индиго (кричащий обсидиан)
                case "minecraft:lodestone":
                    return new Color(0x708090); // серо-голубой (магнетит)
                case "minecraft:conduit":
                    return new Color(0x4682B4); // стальной синий (активированный)
                case "minecraft:beacon":
                    return new Color(0x7FFFD4); // аквамарин (стекло + незерит)
                case "minecraft:end_portal_frame":
                    return new Color(0x006400); // темно-зеленый (эндстоун)
                case "minecraft:end_gateway":
                    return new Color(0x4B0082); // фиолетовый (портал)
                case "minecraft:command_block":
                    return new Color(0x8B4513); // коричневый (по умолчанию)
                case "minecraft:chain_command_block":
                    return new Color(0x008000); // зеленый
                case "minecraft:repeating_command_block":
                    return new Color(0xFF0000); // красный
                case "minecraft:structure_block":
                    return new Color(0x00008B); // темно-синий
                case "minecraft:jigsaw":
                    return new Color(0x8B008B); // темно-пурпурный
                case "minecraft:barrier":
                    return new Color(0xFF0000); // полупрозрачный красный
                case "minecraft:light":
                    return new Color(0xFFFF00); // желтый (свет)
                case "minecraft:spawner":
                    return new Color(0x1E1E1E); // почти черный (темный)
                case "minecraft:armor_stand":
                    return new Color(0xC0C0C0); // светло-серый (металл)
                case "minecraft:painting":
                    return new Color(0x8B4513); // коричневый (дерево + холст)
                case "minecraft:daylight_detector":
                    return new Color(0x8B5A2B); // светло-коричневый (дерево + кварц)
                case "minecraft:note_block":
                    return new Color(0x8B0000); // темно-красный (дерево + редстоун)
                case "minecraft:redstone_lamp":
                    return new Color(0xFFD700); // золотой (включенный)
                case "minecraft:tnt":
                    return new Color(0xFF0000); // красный
                case "minecraft:piston":
                case "minecraft:sticky_piston":
                    return new Color(0x808080); // серый (камень + механизмы)
                case "minecraft:observer":
                    return new Color(0x4A4A4A); // темно-серый
                case "minecraft:target":
                    return new Color(0xFF9999); // светло-красный (мишень)
                case "minecraft:tripwire_hook":
                    return new Color(0x8B4513); // коричневый (дерево)
                case "minecraft:lever":
                    return new Color(0x8B5A2B); // светло-коричневый (дерево + редстоун)
                case "minecraft:redstone_torch":
                    return new Color(0xFF0000); // красный (редстоун)
                case "minecraft:repeater":
                case "minecraft:comparator":
                    return new Color(0x8B0000); // темно-красный (редстоун + камень)
                case "minecraft:crafter":
                    return new Color(0x8B4513); // коричневый (дерево + механизмы)
                case "minecraft:chiseled_bookshelf":
                    return new Color(0x8B5A2B); // светло-коричневый (дерево + книги)
                case "minecraft:decorated_pot":
                    return new Color(0xA0522D); // терракотовый

                // Кровати (цвета соответствуют их типам)
                case "minecraft:white_bed":
                    return Color.WHITE;
                case "minecraft:orange_bed":
                    return new Color(0xFF8C00);
                case "minecraft:magenta_bed":
                    return new Color(0xFF00FF);
                case "minecraft:light_blue_bed":
                    return new Color(0xADD8E6);
                case "minecraft:yellow_bed":
                    return new Color(0xFFFF00);
                case "minecraft:lime_bed":
                    return new Color(0x00FF00);
                case "minecraft:pink_bed":
                    return new Color(0xFFC0CB);
                case "minecraft:gray_bed":
                    return new Color(0x808080);
                case "minecraft:light_gray_bed":
                    return new Color(0xD3D3D3);
                case "minecraft:cyan_bed":
                    return new Color(0x00FFFF);
                case "minecraft:purple_bed":
                    return new Color(0x800080);
                case "minecraft:blue_bed":
                    return new Color(0x0000FF);
                case "minecraft:brown_bed":
                    return new Color(0x8B4513);
                case "minecraft:green_bed":
                    return new Color(0x008000);
                case "minecraft:red_bed":
                    return new Color(0xFF0000);
                case "minecraft:black_bed":
                    return Color.BLACK;

                // Знаки (цвета дерева)
                case "minecraft:oak_sign":
                case "minecraft:oak_hanging_sign":
                    return new Color(0x8B4513); // дуб
                case "minecraft:spruce_sign":
                case "minecraft:spruce_hanging_sign":
                    return new Color(0x654321); // ель
                case "minecraft:birch_sign":
                case "minecraft:birch_hanging_sign":
                    return new Color(0xF0E68C); // береза
                case "minecraft:jungle_sign":
                case "minecraft:jungle_hanging_sign":
                    return new Color(0xB8860B); // тропическое дерево
                case "minecraft:acacia_sign":
                case "minecraft:acacia_hanging_sign":
                    return new Color(0xCD853F); // акация
                case "minecraft:dark_oak_sign":
                case "minecraft:dark_oak_hanging_sign":
                    return new Color(0x5D4037); // темный дуб
                case "minecraft:mangrove_sign":
                case "minecraft:mangrove_hanging_sign":
                    return new Color(0x8B5A2B); // мангровое дерево
                case "minecraft:cherry_sign":
                case "minecraft:cherry_hanging_sign":
                    return new Color(0xFFB6C1); // вишня
                case "minecraft:bamboo_sign":
                case "minecraft:bamboo_hanging_sign":
                    return new Color(0x9ACD32); // бамбук
                case "minecraft:crimson_sign":
                case "minecraft:crimson_hanging_sign":
                    return new Color(0x800000); // малиновый (нетерракотовый)
                case "minecraft:warped_sign":
                case "minecraft:warped_hanging_sign":
                    return new Color(0x008080); // искаженный (сине-зеленый)

                // Скульк-блоки
                case "minecraft:sculk_catalyst":
                    return new Color(0x006400); // темно-зеленый
                case "minecraft:sculk_sensor":
                    return new Color(0x00AA00); // ярко-зеленый
                case "minecraft:calibrated_sculk_sensor":
                    return new Color(0x00FF00); // лаймовый (активированный)
                case "minecraft:sculk_shrieker":
                    return new Color(0x003300); // очень темно-зеленый

                default:
                    return Color.WHITE;
            }
        }

    static boolean isBlockAirOrFluid(BlockPos pos) {
        if (MC.level.getBlockState(pos).isAir()) {
            return true;
        }
        FluidState fluidState = MC.level.getFluidState(pos);
        return !fluidState.isEmpty();
    }

    public static boolean isPlaceable(BlockPos pos) {
        return isPlaceable(pos, 10);
    }

    public static boolean isPlaceable(BlockPos pos, int distance) {
        AABB blockBox = new AABB(pos);
        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity.distanceToSqr(MC.player) > distance) continue;
            if (entity instanceof EndCrystal) continue;
            if (entity instanceof ItemEntity) continue;
            if (entity instanceof Arrow) continue;

            if (entity.getBoundingBox().intersects(blockBox)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isReplaceable(BlockPos pos) {
        return MC.level.getBlockState(pos).canBeReplaced();
    }

    public static boolean isBed(Block block) {
        return block instanceof BedBlock;
    }

    public static boolean withinLevelHeight(BlockPos pos) {
        if (MC.level == null) return false;
        int minY = MC.level.getMinY();
        int maxY = MC.level.getMaxY();
        return pos.getY() >= minY && pos.getY() <= maxY;
    }
}
