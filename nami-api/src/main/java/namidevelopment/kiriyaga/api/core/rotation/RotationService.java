package namidevelopment.kiriyaga.api.core.rotation;

/**
 * Rotation SERVICE doc.
 * <p>
 * Manages motion rotations:
 * <ul>
 *     <li>{@link RotationStateHandler} — Stores current state of rotations, read docs;</li>
 *     <li>{@link RotationRequestHandler} — as-is request handler;</li>
 *     <li>{@link RotationTickHandler} — rotation intorpolation, movement fix.</li>
 * </ul>
 *
 * How it works:
 * <ul>
 *   <li><b>PreTick (Priority: Low or higher)</b>
 *   Rotation requests are submitted and prioritized</li>
 *
 *   <li><b>PreTick (Priority: Lowest)</b>
 *   Rotations are interpolated and final values are applied.</li>
 *
 *   <li><b>Tick</b>
 *   The rotation/movement packet is sent
 *   (see {@code net.minecraft.client.network.ClientPlayerEntity:224}).</li>
 *
 *   <li><b>Next tick</b>
 *   During PreTick the rotation is confirmed.
 *   Note: interactions (attack/place/interact) are only possible during PreTick,
 *   which always introduces at least a 1-tick delay.</li>
 * </ul>
 */
public class RotationService {

    private final RotationStateHandler stateHandler = new RotationStateHandler();
    private final RotationRequestHandler requestHandler = new RotationRequestHandler(stateHandler);
    private final RotationTickHandler tickHandler = new RotationTickHandler(stateHandler, requestHandler);

    public void init() {
        tickHandler.init();
    }

    public RotationStateHandler getStateHandler() {
        return stateHandler;
    }

    public RotationRequestHandler getRequestHandler() {
        return requestHandler;
    }

    public RotationTickHandler getTickHandler() {
        return tickHandler;
    }
}
