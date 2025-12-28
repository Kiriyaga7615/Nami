package me.kiriyaga.nami.feature.module.impl.visuals.blocksearch;

import me.kiriyaga.nami.core.executable.model.ExecutableThreadType;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.*;
import me.kiriyaga.nami.feature.module.*;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.setting.impl.*;

import me.kiriyaga.nami.util.BlockUtils;
import me.kiriyaga.nami.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.minecraft.world.level.chunk.LevelChunk;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static me.kiriyaga.nami.Nami.*;

@RegisterModule
public class BlockSearchModule extends Module {

    private final WhitelistSetting blockList = addSetting(new WhitelistSetting("Whitelist", true, WhitelistSetting.Type.BLOCK));
    //private final BoolSetting tracers = addSetting(new BoolSetting("Tracers", false));
    private final BoolSetting notAtSpawn = addSetting(new BoolSetting("NotAtSpawn", false));

    private Set<Identifier> candidateIds = new HashSet<>();
    private final BlockingQueue<Chunk> snapshotQueue = new LinkedBlockingQueue<>();
    private BlockSearch workerThread;
    public static final ConcurrentMap<Long, Set<BlockPos>> chunkBlocks = new ConcurrentHashMap<>();
    private final Queue<Component> pendingMessages = new LinkedList<>();

    public BlockSearchModule() {
        super("BlockSearch", "Search for specified blocks.", ModuleCategory.of("Render"));

        blockList.setOnChanged(() -> updateCandidateBlocks());

        notAtSpawn.setOnChanged(() -> reloadChunks());
    }

    @Override
    public void onEnable() {
        updateCandidateBlocks();

        workerThread = new BlockSearch(snapshotQueue, chunkBlocks, candidateIds);
        workerThread.start();

        reloadChunks();
    }

    @Override
    public void onDisable() {
        if (workerThread != null) workerThread.stopWorker();

        chunkBlocks.clear();
        snapshotQueue.clear();
    }

    private void updateCandidateBlocks() {
        candidateIds = blockList.getWhitelist();
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onChunkLoad(ChunkDataEvent event) {
        if (MC.world == null || MC.player == null) return;

        if (notAtSpawn.get()) {
            BlockPos pos = MC.player.getBlockPos();
            if (Math.abs(pos.getX()) + Math.abs(pos.getZ()) < 12000) return;
        }

        EXECUTABLE_MANAGER.getRequestHandler().submit(() -> {
            snapshotQueue.offer(makeSnapshot(event.getChunk()));}, 0, ExecutableThreadType.PRE_TICK);
    }

    // yeah we still are forced to load them in main thread
    private Chunk makeSnapshot(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        List<Block> blocks = new ArrayList<>();

        int minY = -64;
        int maxY = 320;
        BlockPos start = pos.getStartPos();

        for (int x = 0; x < 16; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockState state = MC.world.getBlockState(new BlockPos(start.getX()+x, y, start.getZ()+z));
                    Identifier id = Registries.BLOCK.getId(state.getBlock());

                    blocks.add(new Block(x, y, z, id));
                }
            }
        }

        return new Chunk(pos, blocks);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRender(Render3DEvent event) {
        PoseStack matrices = event.getMatrices();

        synchronized (pendingMessages) {
            while (!pendingMessages.isEmpty()) CHAT_MANAGER.sendRaw(pendingMessages.poll());
        }

        if (MC.world == null || MC.player == null) return;

        for (Set<BlockPos> set : chunkBlocks.values()) {
            for (BlockPos pos : set) {

                BlockState state = MC.world.getBlockState(pos);

                RenderUtil.drawBlockPosLines(MC.world, pos, state, BlockUtils.getColorByBlockId(state),true,true, 1.5f);

            //    if (tracers.get())
                //    RenderUtil.drawLine(matrices, MC.player.getEyePos(), pos.toCenterPos(), BlockUtils.getColorByBlockId(state), 1.50);
            }
        }
    }

    private void reloadChunks() {
        if (MC.world == null || MC.player == null) return;

        snapshotQueue.clear();
        chunkBlocks.clear();

        int radius = MC.options.getViewDistance().getValue();
        ChunkPos pc = new ChunkPos(MC.player.getBlockPos());

        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                snapshotQueue.offer(makeSnapshot(
                        MC.world.getChunk(pc.x + dx, pc.z + dz)
                ));
            }
    }
}
