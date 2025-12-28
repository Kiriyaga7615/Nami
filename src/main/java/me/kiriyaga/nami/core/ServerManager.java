package me.kiriyaga.nami.core;

import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.PacketReceiveEvent;
import me.kiriyaga.nami.event.impl.PreTickEvent;
import me.kiriyaga.nami.feature.module.impl.client.DebugModule;
import me.kiriyaga.nami.feature.module.impl.client.FastLatencyModule;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.debugchart.LocalSampleLogger;

import java.util.Arrays;

import static me.kiriyaga.nami.Nami.*;

public class ServerManager {

    private final float[] tickRates = new float[20];
    private int nextIndex = 0;
    private int countTick = 0;
    private long lastTimeUpdate = -1;

    private Vec3 lastSetbackPosition;
    private long lastSetbackTime;
    private int lastTeleportId;
    private final int[] pendingTransactions = new int[4];
    private int transactionIndex;
    private int ping = -1;

    private volatile long lastReceiveTime = -1;
    private volatile int lastPing = -1;
    private int[] pingHistory = new int[10];
    private int index = 0;
    private int countPing = 0;
    private volatile long lastUpdated = -1;

    public void init() {
        EVENT_MANAGER.register(this);
        LOGGER.info("Server Manager loaded");
        Arrays.fill(pendingTransactions, -1);
    }

    @SubscribeEvent
    public void onTick(PreTickEvent event) {
        if (MC.world == null ||  MC.getNetworkHandler() == null)
            return;

        updatePing();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundSetTimePacket) {
            long now = System.currentTimeMillis();

            if (lastTimeUpdate != -1) {
                float elapsed = (now - lastTimeUpdate) / 1000.0f;
                float tps = 20.0f / elapsed;
                tickRates[nextIndex % tickRates.length] = Math.min(Math.max(tps, 0.0f), 20.0f);
                nextIndex++;
                countTick = Math.min(countTick + 1, tickRates.length);
            }

            lastTimeUpdate = now;
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPacketReceive2(PacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundPingPacket packet) {
            if (transactionIndex > 3) return;

            pendingTransactions[transactionIndex] = packet.getParameter();
            transactionIndex++;
        } else if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
            lastSetbackPosition = packet.comp_3228().comp_3148();
            lastSetbackTime = System.currentTimeMillis();
            lastTeleportId = packet.teleportId();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPacketReceive3(PacketReceiveEvent packet) {
        FastLatencyModule config = MODULE_MANAGER.getStorage().getByClass(FastLatencyModule.class);

        if (config.fastLatencyMode.get() != FastLatencyModule.FastLatencyMode.OLD)
            return;

        if (packet.getPacket() instanceof ClientboundKeepAlivePacket) {
            long now = System.currentTimeMillis();
            int keepAliveInterval = config != null ? config.keepAliveInterval.get() : 1000;

            if (lastReceiveTime != -1) {
                long interval = now - lastReceiveTime;
                int ping = (int) Math.max(0, interval - keepAliveInterval);

                int smoothingStrength = config != null ? config.smoothingStrength.get() : 10;
                if (pingHistory.length != smoothingStrength) {
                    int[] newHistory = new int[smoothingStrength];
                    for (int i = 0; i < Math.min(countPing, smoothingStrength); i++) {
                        newHistory[i] = pingHistory[(index - countPing + i + pingHistory.length) % pingHistory.length];
                    }
                    pingHistory = newHistory;
                    countPing = Math.min(countPing, smoothingStrength);
                    index = countPing % smoothingStrength;
                }

                pingHistory[index++ % smoothingStrength] = ping;
                countPing = Math.min(countPing + 1, smoothingStrength);

                updatePing(averagePing());
                DebugModule debugModule = MODULE_MANAGER.getStorage().getByClass(DebugModule.class);

                debugModule.debugPing(Component.of("Interval=" + interval + "ms, Ping=" + ping + "ms, Average=" + lastPing + "ms"));
            }

            lastReceiveTime = now;
        }
    }

    public float getAverageTPS() {
        if (countTick == 0) return 20.0f;

        float sum = 0.0f;
        int valid = 0;
        for (int i = 0; i < countTick; i++) {
            float t = tickRates[i];
            if (t > 0.0f) {
                sum += t;
                valid++;
            }
        }
        return valid == 0 ? 20.0f : Math.min(Math.max(sum / valid, 0.0f), 20.0f);
    }

    public float getMinTPS() {
        if (countTick == 0) return 20.0f;

        float min = 20.0f;
        for (int i = 0; i < countTick; i++) {
            float t = tickRates[i];
            if (t > 0.0f && t < min) {
                min = t;
            }
        }
        return Math.min(Math.max(min, 0.0f), 20.0f);
    }

    public float getLatestTPS() {
        if (countTick == 0) return 20.0f;

        int last = (nextIndex - 1 + tickRates.length) % tickRates.length;
        return Math.min(Math.max(tickRates[last], 0.0f), 20.0f);
    }

    public boolean hasElapsedSinceSetback(long milliseconds) {
        return lastSetbackPosition != null && (System.currentTimeMillis() - lastSetbackTime) >= milliseconds;
    }

    public Vec3 getLastSetbackPosition() {
        return lastSetbackPosition;
    }

    public long getLastSetbackTime() {
        return lastSetbackTime;
    }

    public int getLastTeleportId() {
        return lastTeleportId;
    }

    private int averagePing() {
        int sum = 0;
        for (int i = 0; i < countPing; i++) {
            sum += pingHistory[i];
        }
        return countPing == 0 ? -1 : sum / countPing;
    }

    public int getPing() {
        return ping;
    }

    public void updatePing() {
        FastLatencyModule config = MODULE_MANAGER.getStorage().getByClass(FastLatencyModule.class);
        if (config == null) ping = lastPing;

        switch (config.fastLatencyMode.get()) {
            case OLD:
                ping = lastPing;
                break;
            case OFF:
                if (MC.getNetworkHandler() != null && MC.player != null) {
                    ping = MC.getNetworkHandler().getPlayerListEntry(MC.player.getUuid()).getLatency();
                } else {
                    ping = -1;
                }
                break;
            case NEW:
                try {
                    if (MC.getDebugHud() != null && MC.getDebugHud().getPingLog() != null) {
                        LocalSampleLogger pingLog = MC.getDebugHud().getPingLog();
                        int count = pingLog.getLength();
                        if (count == 0) ping = -1;

                        updatePing((int) pingLog.get(count - 1, 0));

                        ping = (int) pingLog.get(count - 1, 0);
                    }
                } catch (Exception ignored) {
                }
                break;
            default: ping = -1;
        }
    }

    public boolean isConnectionUnstable() {
        FastLatencyModule config = MODULE_MANAGER.getStorage().getByClass(FastLatencyModule.class);
        if (config == null) return false;

        DebugModule debugModule = MODULE_MANAGER.getStorage().getByClass(DebugModule.class);
        int timeoutMillis = config.unstableConnectionTimeout.get() * 1000;

        if (lastUpdated == -1) {
            debugModule.debugPing(Component.of("Connection unstable: no ping data yet"));
            return true;
        }

        boolean unstable = (System.currentTimeMillis() - lastUpdated) > timeoutMillis;
        debugModule.debugPing(Component.of("Connection unstable: last ping updated " + (System.currentTimeMillis() - lastUpdated) + "ms ago"));
        return unstable;
    }

    public void updatePing(int ping) {
        if (ping != lastPing) {
            lastPing = ping;
            lastUpdated = System.currentTimeMillis();
        }
    }

    public float getUnstableTime() {
        if (lastUpdated == -1) return Float.POSITIVE_INFINITY;
        long deltaMillis = System.currentTimeMillis() - lastUpdated;
        return deltaMillis / 1000.0f;
    }
}
