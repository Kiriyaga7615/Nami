package namidevelopment.kiriyaga.api.core.rotation.model;

import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.RotationsFeatureConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

import static namidevelopment.kiriyaga.api.util.RotationUtils.*;

/**
 * Rotation request. Used for prioritizing, and controlling motion rotations
 * This is never used for silent (1.20.4-6 exploit) rotations, because they are made as-is and they are very simple
 */
public class RotationRequest {
    /**
     * Identifier of request, it is not unique, we use it as task scoped identifier
     * For example, kill aura id can be KillAuraFeature.getNane().getString()
     * Any new request with the same id will replace old one
     * Do not replace supplier requests! You should control their life cycle by yourself!
     */
    public final String id;

    /**
     * Priority of rotation
     * Theesde are also recalculate:
     * New request appeared
     * Reequest finished
     */
    public final int priority;

    /**
     * Float supplier:
     * <ul>
     *     <li>{@code false} — angles are declared one time, static;</li>
     *     <li>{@code true} — (YOU SHOULD CONTROL THEIR LIFE CYCLE BY YOURSELF!)Angles dynamic updated by {@link Supplier}.</li>
     * </ul>
     */
    private final boolean dynamic;

    /**
     * Supplir of YRot param, dynamic
     */
    private final Supplier<Float> yRotSupplier;

    /**
     * Supplier of XRot param, dynamic.
     */
    private final Supplier<Float> xRotSupplier;

    /**
     * Static YRot param.
     */
    public float targetYRot;

    /**
     * Static XRot param.
     */
    public float targetXRot;

    /**
     * Rotation mode (default = settinga)
     */
    public final RotationsFeatureConfig.RotationMode rotationMode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RotationRequest)) return false;
        RotationRequest other = (RotationRequest) o;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Static rotation request constructor.
     *
     * @param id        identifier
     * @param priority  priority
     * @param yRot       static yRot value
     * @param XRot     static XRot value
     */
    public RotationRequest(String id, int priority, float yRot, float XRot) {
        this(id, priority, yRot, XRot, getDefaultRotationMode());
    }

    /**
     * Static rotation request constructor with override.
     *
     * @param id            identifier
     * @param priority      priority
     * @param yRot           static yRot value
     * @param XRot         static XRot value
     * @param rotationMode  override rotation mode
     */
    public RotationRequest(String id, int priority, float yRot, float XRot, RotationsFeatureConfig.RotationMode rotationMode) {
        this.id = id;
        this.priority = priority;
        this.dynamic = false;
        this.targetYRot = yRot;
        this.targetXRot = XRot;
        this.yRotSupplier = null;
        this.xRotSupplier = null;
        this.rotationMode = rotationMode;
    }

    /**
     * Dynamic rotation request constructor.
     *
     * @param id             identifier
     * @param priority       priority
     * @param yRotSupplier    dynamic yRot supplier
     * @param xRotSupplier  dynamic XRot supplier
     */
    public RotationRequest(String id, int priority, Supplier<Float> yRotSupplier, Supplier<Float> xRotSupplier) {
        this(id, priority, yRotSupplier, xRotSupplier, getDefaultRotationMode());
    }

    /**
     * Dynamic rotation request constructor with override.
     *
     * @param id             identifier
     * @param priority       priority
     * @param yRotSupplier    dynamic yRot supplier
     * @param xRotSupplier  dynamic XRot supplier
     * @param rotationMode   override rotation mode
     */
    public RotationRequest(String id, int priority, Supplier<Float> yRotSupplier, Supplier<Float> xRotSupplier, RotationsFeatureConfig.RotationMode rotationMode) {
        this.id = id;
        this.priority = priority;
        this.dynamic = true;
        this.yRotSupplier = yRotSupplier;
        this.xRotSupplier = xRotSupplier;
        this.rotationMode = rotationMode;
        updateTarget();
    }

    /**
     * 1 tick predicted from player eye pos, for motion rotations
     *
     * @param id  Identifier
     * @param priority   Priority
     * @param player Player BEFORE motion predict
     * @param pos  Pos to look at
     */
    public RotationRequest(String id, int priority, LivingEntity player, Vec3 pos) {
        this.id = id;
        this.priority = priority;
        this.rotationMode = RotationsFeatureConfig.RotationMode.MOTION;

        Vec3 predictedEye = predictMotion(player);

        this.targetYRot = getYRotToVec(predictedEye, pos);
        this.targetXRot = getXRotToVec(predictedEye, pos);

        this.yRotSupplier = null;
        this.xRotSupplier = null;
        dynamic = false;
    }

    public boolean shouldUpdate() {
        return dynamic;
    }

    /**
     * Updates {@link #targetYRot} and {@link #targetXRot},
     * if request is dynamic and has valid {@link Supplier}.
     */
    public void updateTarget() {
        if (dynamic && yRotSupplier != null && xRotSupplier != null) {
            targetYRot = yRotSupplier.get();
            targetXRot = xRotSupplier.get();
        }
    }

    private static RotationsFeatureConfig.RotationMode getDefaultRotationMode() {
        RotationsFeatureConfig rotationsFeatureConfig = FeatureContractService.get(RotationsFeatureConfig.class);
        return rotationsFeatureConfig != null ? rotationsFeatureConfig.getRotationMode() : RotationsFeatureConfig.RotationMode.MOTION;
    }
}