package me.kiriyaga.nami.core.rotation;

import me.kiriyaga.nami.core.rotation.model.RotationRequest;
import me.kiriyaga.nami.feature.module.impl.client.RotationModule;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static me.kiriyaga.nami.Nami.*;
import static me.kiriyaga.nami.util.RotationUtils.yawDifference;

/**
 * Request handler.
 * <p>
 * Stores active {@link RotationRequest}, and prioritize them,
 * Also provides as-is data about complition, current state, based on latest comparing of rotation states.
 */
public class RotationRequestHandler {

    /**
     * rotation request queue sorted by {@link RotationRequest#priority}.
     * FIRST ELEMENT OF LIST IS ALWAYS ACTIVE REQUESET
     */
    private final List<RotationRequest> requests = new ArrayList<>();

    private final RotationStateHandler stateHandler;

    /**
     * Last interpolated, and used, by TickHandler request.
     * We using it to understand, when request was not finished/returned/cancelled, but changed for other request
     */
    private String lastActiveRequestId = null;

    public RotationRequestHandler(RotationStateHandler stateHandler) {
        this.stateHandler = stateHandler;
    }

    /**
     * Submits new {@link RotationRequest}
     * <p>
     * Prioritize them
     * YOU SHOULD NEVER SUMBIT MORE THEN ONE DYNAMIC REQUEST!
     * USE STATIC REQUEST AND UPDATE DATA ON ANY PRE-TICK EVENT HIGHER THEN LOWEST
     */
    public void submit(RotationRequest request) {
//        RotationModule.RotationMode mode = MODULE_MANAGER.getStorage().getByClass(RotationModule.class).rotation.get();

        if (request.rotationMode == RotationModule.RotationMode.SILENT) {
            performSilent(request);
            stateHandler.setSilentSyncRequired(true);
            return;
        }

        requests.removeIf(r -> Objects.equals(r.id, request.id));
        requests.add(request);
        requests.sort(Comparator.comparingInt(r -> -r.priority));
    }

    public boolean hasRequest(String id) {
        return requests.stream().anyMatch(r -> r.id.equals(id));
    }

    /**
     * Cancels request by ID.
     * YOU SHOULD NEVER CANCEL STATIC REQUESTS
     * it is used only for dynamic request lifecycle
     * @param id identifier of request
     */
    public void cancel(String id) {
        requests.removeIf(r -> r.id.equals(id));
    }

    /**
     * As-Is check for complition.
     *
     * @param id identifier of request
     * @return {@code true}, yaw + pitch is close enough to target (enough = threshold)
     */
    public boolean isCompleted(String id) {
        return isCompleted(
                id,
                MODULE_MANAGER.getStorage().getByClass(RotationModule.class)
                        .rotationThreshold.get().floatValue()
        );
    }

    /**
     * Checks if request completed by threshold
     *
     * @param id identifier
     * @param threshold allowed degree loss
     * @return {@code true}, if yaw pitch is close enough to target
     */
    public boolean isCompleted(String id, float threshold) {
        if (MODULE_MANAGER.getStorage().getByClass(RotationModule.class).rotation.get() == RotationModule.RotationMode.SILENT && stateHandler.getSilentSyncRequired())
            return true; // TODO: find better solution
        return requests.stream()
                .filter(r -> r.id.equals(id))
                .findFirst()
                .map(r -> {
                    float yawDiff = yawDifference(r.targetYaw, stateHandler.getRotationYaw());
                    float pitchDiff = r.targetPitch - stateHandler.getRotationPitch();
                    return Math.abs(yawDiff) <= threshold && Math.abs(pitchDiff) <= threshold;
                }).orElse(false);
    }

    public RotationRequest getActiveRequest() {
        return requests.isEmpty() ? null : requests.get(0);
    }

    public void removeActiveRequest() {
        if (!requests.isEmpty()) {
            requests.remove(0);
        }
    }

    public void clear() {
        requests.clear();
        lastActiveRequestId = null;
    }

    public void clearLastActiveId() {
        lastActiveRequestId = null;
    }

    public String getLastActiveId() {
        return lastActiveRequestId;
    }

    public void setLastActiveId(String id) {
        lastActiveRequestId = id;
    }

    private void performSilent(RotationRequest req) {
        float targetYaw = req.targetYaw;
        float targetPitch = req.targetPitch;

//        ROTATION_MANAGER.getStateHandler().setRotationYaw(targetYaw);
//        ROTATION_MANAGER.getStateHandler().setRotationPitch(targetPitch);
        ROTATION_MANAGER.getStateHandler().setServerYaw(targetYaw);
        ROTATION_MANAGER.getStateHandler().setServerPitch(targetPitch);

        MC.getNetworkHandler().sendPacket(new ServerboundMovePlayerPacket.Full(MC.player.getX(), MC.player.getY(), MC.player.getZ(), targetYaw, targetPitch, MC.player.isOnGround(), true));
    }
}
