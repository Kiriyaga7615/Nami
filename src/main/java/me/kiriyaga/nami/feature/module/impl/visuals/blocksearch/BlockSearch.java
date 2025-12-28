package me.kiriyaga.nami.feature.module.impl.visuals.blocksearch;

import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;

public class BlockSearch extends Thread {

    private final BlockingQueue<Chunk> queue;
    private final ConcurrentMap<Long, Set<BlockPos>> resultMap;
    private volatile boolean running = true;
    private final Set<Identifier> targets;

    public BlockSearch(BlockingQueue<Chunk> queue, ConcurrentMap<Long, Set<BlockPos>> resultMap, Set<Identifier> targetIds) {
        this.queue = queue;
        this.resultMap = resultMap;
        this.targets = targetIds;
        setName("BlockSearchWorker");
        setDaemon(true);
    }

    @Override
    public void run() {
        while (running) {
            try {
                Chunk snapshot = queue.take();

                Set<BlockPos> found = new HashSet<>();

                for (Block bs : snapshot.blocks) {
                    if (bs.id == Identifier.of("air") || bs.id == Identifier.of("void_air")|| bs.id == Identifier.of("cave_air")) {
                        continue;
                    }

                    if (targets.contains(bs.id)) {
                        found.add(new BlockPos(
                                snapshot.pos.getStartX() + bs.x,
                                bs.y,
                                snapshot.pos.getStartZ() + bs.z
                        ));
                    }
                }

                if (found.isEmpty()) {
                    resultMap.remove(snapshot.getKey());
                } else {
                    resultMap.put(snapshot.getKey(), found);
                }

            } catch (InterruptedException ignored) {}
        }
    }

    public void stopWorker() {
        running = false;
        interrupt();
    }
}
