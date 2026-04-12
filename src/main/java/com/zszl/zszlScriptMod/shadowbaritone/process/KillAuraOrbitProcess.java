package com.zszl.zszlScriptMod.shadowbaritone.process;

import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.shadowbaritone.Baritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.Goal;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.GoalBlock;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.movement.ActionCosts;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.movement.IMovement;
import com.zszl.zszlScriptMod.shadowbaritone.api.process.PathingCommand;
import com.zszl.zszlScriptMod.shadowbaritone.api.process.PathingCommandType;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.BetterBlockPos;
import com.zszl.zszlScriptMod.shadowbaritone.behavior.PathingBehavior;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.CalculationContext;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.movements.MovementRouteTraverse;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.path.OrbitRoutePath;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.path.PathExecutor;
import com.zszl.zszlScriptMod.shadowbaritone.utils.BaritoneProcessHelper;
import com.zszl.zszlScriptMod.shadowbaritone.utils.PathingCommandPath;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KillAuraOrbitProcess extends BaritoneProcessHelper {

    private static final int DEFAULT_MIN_LOOP_POINTS = 12;
    private static final int MIN_GUIDE_POINTS = KillAuraHandler.MIN_HUNT_ORBIT_SAMPLE_POINTS;
    private static final int MAX_GUIDE_POINTS = 360;
    private static final int MAX_SEARCH_RADIUS = 2;
    private static final double MAX_TARGET_MOVE_BEFORE_REPLAN = 0.65D;
    private static final double MAX_RADIUS_DELTA_BEFORE_REPLAN = 0.15D;
    private static final int MAX_PLAN_AGE_TICKS = 20;
    private static final int REQUEST_STALE_TICKS = 8;
    private static final int MIN_REBUILD_INTERVAL_TICKS = 6;
    private static final int PREBUILD_NEXT_ROUTE_REMAINING_MOVEMENTS = 2;
    private static final int FAILED_REBUILD_COOLDOWN_TICKS = 30;
    private static final double FAILED_REBUILD_TARGET_MOVE_TOLERANCE = 1.25D;
    private static final double GUIDE_ROUTE_Y_OFFSET = 0.5D;
    private static final double GUIDE_RENDER_Y_OFFSET = 0.08D;
    private static final double SUSPICIOUS_ROUTE_STEP = 0.35D;
    private static final double MAX_LOOP_ENTRY_DISTANCE = 1.60D;
    private static final double ORBIT_PLAYER_HALF_WIDTH = 0.299D;
    private static final double ORBIT_CLEARANCE_MARGIN = 0.045D;
    private static final double GUIDE_RADIUS_SHRINK_STEP = 0.18D;
    private static final double GUIDE_MAX_RADIUS_SHRINK = 2.4D;
    private static final int GUIDE_ANGLE_ADJUST_STEPS = 3;
    private static final double ROUTE_SAMPLE_STEP = 0.16D;
    private static final double MAX_VERTICAL_STEP_BETWEEN_NODES = 1.25D;

    private State state = State.IDLE;
    private int targetEntityId = Integer.MIN_VALUE;
    private double desiredRadius = 4.2D;
    private double lastPlannedRadius = 4.2D;
    private double lastPlannedTargetX = 0.0D;
    private double lastPlannedTargetZ = 0.0D;
    private int lastPlannedFeetY = Integer.MIN_VALUE;
    private int lastPlanTick = Integer.MIN_VALUE;
    private int lastRequestTick = Integer.MIN_VALUE;
    private int lastRebuildTick = Integer.MIN_VALUE;
    private int lastFailedRebuildTick = Integer.MIN_VALUE;
    private int lastFailedTargetEntityId = Integer.MIN_VALUE;
    private int lastFailedFeetY = Integer.MIN_VALUE;
    private double lastFailedRadius = Double.NaN;
    private double lastFailedTargetX = Double.NaN;
    private double lastFailedTargetZ = Double.NaN;
    private long planRevision = 0L;
    private Goal currentGoal = null;
    private List<BetterBlockPos> orbitLoop = Collections.emptyList();
    private List<OrbitArcPlan> orbitArcs = Collections.emptyList();
    private List<Vec3d> renderLoop = Collections.emptyList();
    private RebuildCache rebuildCache = null;

    public KillAuraOrbitProcess(Baritone baritone) {
        super(baritone);
    }

    public boolean requestOrbit(EntityLivingBase target, double radius) {
        if (ctx.player() == null || ctx.world() == null || target == null || target.isDead) {
            orbitDebug("requestOrbit rejected state=%s target=%s radius=%.3f", this.state,
                    target == null ? "null" : Integer.toString(target.getEntityId()), radius);
            requestStop();
            return false;
        }
        if (shouldRejectAirborneOrbit()) {
            orbitDebug("requestOrbit rejected_airborne state=%s target=%s radius=%.3f playerFeet=%s", this.state,
                    Integer.toString(target.getEntityId()), radius, formatPos(ctx.playerFeet()));
            requestStop();
            return false;
        }

        int nowTick = ctx.player().ticksExisted;
        double clampedRadius = Math.max(0.5D, radius);
        boolean sameTarget = this.state == State.ACTIVE && this.targetEntityId == target.getEntityId();
        boolean sameRadius = Math.abs(this.desiredRadius - clampedRadius) <= 0.001D;
        if (sameTarget && sameRadius && hasUsableLoop()) {
            this.lastRequestTick = nowTick;
            return true;
        }
        if (isRecentFailedRebuild(target, clampedRadius, nowTick) && !hasUsableLoop()) {
            orbitDebug("requestOrbit rejected_recent_failure target=%d radius=%.3f playerFeet=%s", target.getEntityId(),
                    clampedRadius, formatPos(ctx.playerFeet()));
            this.state = State.IDLE;
            this.targetEntityId = Integer.MIN_VALUE;
            this.currentGoal = null;
            this.lastRequestTick = nowTick;
            return false;
        }

        if (sameTarget && sameRadius) {
            this.lastRequestTick = nowTick;
            return hasUsableLoop();
        }

        this.state = State.ACTIVE;
        this.targetEntityId = target.getEntityId();
        this.desiredRadius = clampedRadius;
        this.lastRequestTick = nowTick;

        String rebuildReason = getRebuildReason(target);
        orbitDebug("requestOrbit target=%d targetPos=%s radius=%.3f playerFeet=%s rebuildReason=%s",
                target.getEntityId(), formatEntityXZ(target), this.desiredRadius, formatPos(ctx.playerFeet()),
                rebuildReason == null ? "none" : rebuildReason);
        if (rebuildReason != null && shouldRebuildNow(nowTick, true)) {
            rebuildLoop(target);
        }
        return hasUsableLoop();
    }

    public void requestStop() {
        if (this.state != State.IDLE) {
            this.state = State.STOPPING;
        }
    }

    public boolean hasUsableLoop() {
        return this.orbitLoop.size() >= getRequiredLoopPointCount() && this.orbitArcs.size() == this.orbitLoop.size();
    }

    public List<BetterBlockPos> getNavigationLoopSnapshot() {
        if (this.state != State.ACTIVE || !hasUsableLoop()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(this.orbitLoop);
    }

    public List<Vec3d> getRenderedLoopSnapshot() {
        if (this.state != State.ACTIVE || !hasUsableLoop() || this.renderLoop.size() < 2) {
            return Collections.emptyList();
        }
        return new ArrayList<>(this.renderLoop);
    }

    public List<Vec3d> getRenderedLoopView() {
        if (this.state != State.ACTIVE || !hasUsableLoop() || this.renderLoop.size() < 2) {
            return Collections.emptyList();
        }
        return this.renderLoop;
    }

    @Override
    public boolean isActive() {
        return this.state != State.IDLE;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (this.state == State.STOPPING) {
            clearRuntime();
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        EntityLivingBase target = getTrackedTarget();
        if (target == null || isRequestStale()) {
            orbitDebug("tick cancel targetValid=%s requestStale=%s state=%s", target != null, isRequestStale(), this.state);
            clearRuntime();
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        boolean rebuilt = false;
        int nowTick = ctx.player() == null ? Integer.MIN_VALUE : ctx.player().ticksExisted;
        String rebuildReason = calcFailed ? "calc_failed" : getRebuildReason(target);
        boolean recentFailedRebuild = hasUsableLoop() && isRecentFailedRebuild(target, this.desiredRadius, nowTick);
        if (rebuildReason != null && !recentFailedRebuild && shouldRebuildNow(nowTick, !hasUsableLoop())) {
            orbitDebug("tick rebuild target=%d reason=%s currentGoal=%s", target.getEntityId(), rebuildReason,
                    this.currentGoal);
            rebuildLoop(target);
            rebuilt = true;
        } else if (rebuildReason != null && recentFailedRebuild && isOrbitDebugEnabled()) {
            orbitDebug("tick deferRebuildRecentFailure target=%d reason=%s currentGoal=%s", target.getEntityId(),
                    rebuildReason, this.currentGoal);
        } else if (rebuildReason != null && isOrbitDebugEnabled()) {
            orbitDebug("tick deferRebuild target=%d reason=%s currentGoal=%s", target.getEntityId(), rebuildReason,
                    this.currentGoal);
        }
        if (!hasUsableLoop()) {
            orbitDebug("tick cancel unusableLoop nodes=%d arcs=%d render=%d", this.orbitLoop.size(), this.orbitArcs.size(),
                    this.renderLoop.size());
            clearRuntime();
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        PathingCommand currentRouteCommand = tryContinueCurrentOrbitPath();
        if (currentRouteCommand != null) {
            return currentRouteCommand;
        }

        OrbitRoutePath desiredPath = determineDesiredRoutePath();
        if (desiredPath != null) {
            Goal commandGoal = getActiveOrbitCommandGoal(desiredPath);
            this.currentGoal = commandGoal;
            orbitDebug("tick route goal=%s pathGoal=%s routeKey=%s startNode=%d endNode=%d movements=%d", commandGoal,
                    desiredPath.getGoal(),
                    desiredPath.getRouteKey(), desiredPath.getStartNodeIndex(), desiredPath.getEndNodeIndex(),
                    desiredPath.movements().size());
            return new PathingCommandPath(commandGoal, PathingCommandType.SET_GOAL_AND_PATH, desiredPath);
        }

        BetterBlockPos fallbackNode = chooseNearestLoopNode(ctx.playerFeet());
        if (fallbackNode == null) {
            orbitDebug("tick cancel fallbackNode=null playerFeet=%s", formatPos(ctx.playerFeet()));
            clearRuntime();
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        Goal fallbackGoal = new GoalBlock(fallbackNode);
        PathingCommandType commandType = rebuilt || !fallbackGoal.equals(this.currentGoal)
                ? PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH
                : PathingCommandType.SET_GOAL_AND_PATH;
        this.currentGoal = fallbackGoal;
        orbitDebug("tick fallback goal=%s node=%s command=%s", fallbackGoal, formatPos(fallbackNode), commandType);
        return new PathingCommand(fallbackGoal, commandType);
    }

    @Override
    public void onLostControl() {
        this.currentGoal = null;
        if (this.state == State.STOPPING) {
            clearRuntime();
        }
    }

    @Override
    public String displayName0() {
        return "KillAura Orbit";
    }

    @Override
    public double priority() {
        return 4.2D;
    }

    private EntityLivingBase getTrackedTarget() {
        if (ctx.world() == null || this.targetEntityId == Integer.MIN_VALUE) {
            return null;
        }
        Entity entity = ctx.world().getEntityByID(this.targetEntityId);
        if (!(entity instanceof EntityLivingBase)) {
            return null;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        if (living.isDead || living.getHealth() <= 0.0F) {
            return null;
        }
        return living;
    }

    private boolean isRequestStale() {
        return ctx.player() == null || ctx.player().ticksExisted - this.lastRequestTick > REQUEST_STALE_TICKS;
    }

    private boolean shouldRebuildNow(int nowTick, boolean force) {
        if (force || nowTick == Integer.MIN_VALUE || this.lastRebuildTick == Integer.MIN_VALUE) {
            return true;
        }
        return nowTick - this.lastRebuildTick >= MIN_REBUILD_INTERVAL_TICKS;
    }

    private String getRebuildReason(EntityLivingBase target) {
        if (target == null || ctx.player() == null) {
            return "missing_target_or_player";
        }
        if (shouldRejectAirborneOrbit()) {
            return "player_airborne";
        }
        if (!hasUsableLoop()) {
            return "missing_usable_loop";
        }
        double radiusDelta = Math.abs(this.lastPlannedRadius - this.desiredRadius);
        if (radiusDelta > MAX_RADIUS_DELTA_BEFORE_REPLAN) {
            return String.format(Locale.ROOT, "radius_delta=%.3f", radiusDelta);
        }
        double dx = target.posX - this.lastPlannedTargetX;
        double dz = target.posZ - this.lastPlannedTargetZ;
        double targetMoveSq = dx * dx + dz * dz;
        if (targetMoveSq > MAX_TARGET_MOVE_BEFORE_REPLAN * MAX_TARGET_MOVE_BEFORE_REPLAN) {
            return String.format(Locale.ROOT, "target_move=%.3f", Math.sqrt(targetMoveSq));
        }
        int currentFeetY = ctx.playerFeet() == null ? Integer.MIN_VALUE : ctx.playerFeet().getY();
        if (currentFeetY != Integer.MIN_VALUE && currentFeetY > this.lastPlannedFeetY + 1) {
            return "player_feet_y_rise";
        }
        int planAge = ctx.player().ticksExisted - this.lastPlanTick;
        if (planAge > MAX_PLAN_AGE_TICKS) {
            return "plan_age=" + planAge;
        }
        return null;
    }

    private boolean shouldRejectAirborneOrbit() {
        if (ctx.player() == null) {
            return true;
        }
        if (ctx.player().capabilities != null && ctx.player().capabilities.isFlying) {
            return true;
        }
        if (ctx.player().isElytraFlying()) {
            return true;
        }
        return !ctx.player().onGround;
    }

    private boolean isRecentFailedRebuild(EntityLivingBase target, double radius, int nowTick) {
        if (target == null || ctx.player() == null || ctx.playerFeet() == null) {
            return false;
        }
        if (this.lastFailedRebuildTick == Integer.MIN_VALUE || nowTick == Integer.MIN_VALUE) {
            return false;
        }
        if (nowTick - this.lastFailedRebuildTick > FAILED_REBUILD_COOLDOWN_TICKS) {
            return false;
        }
        if (target.getEntityId() != this.lastFailedTargetEntityId) {
            return false;
        }
        if (Math.abs(radius - this.lastFailedRadius) > MAX_RADIUS_DELTA_BEFORE_REPLAN) {
            return false;
        }
        if (ctx.playerFeet().getY() != this.lastFailedFeetY) {
            return false;
        }
        double dx = target.posX - this.lastFailedTargetX;
        double dz = target.posZ - this.lastFailedTargetZ;
        return dx * dx + dz * dz <= FAILED_REBUILD_TARGET_MOVE_TOLERANCE * FAILED_REBUILD_TARGET_MOVE_TOLERANCE;
    }

    private void recordFailedRebuild(EntityLivingBase target) {
        this.lastFailedRebuildTick = ctx.player() == null ? Integer.MIN_VALUE : ctx.player().ticksExisted;
        this.lastFailedTargetEntityId = target == null ? Integer.MIN_VALUE : target.getEntityId();
        this.lastFailedFeetY = ctx.playerFeet() == null ? Integer.MIN_VALUE : ctx.playerFeet().getY();
        this.lastFailedRadius = this.desiredRadius;
        this.lastFailedTargetX = target == null ? Double.NaN : target.posX;
        this.lastFailedTargetZ = target == null ? Double.NaN : target.posZ;
    }

    private void clearFailedRebuild() {
        this.lastFailedRebuildTick = Integer.MIN_VALUE;
        this.lastFailedTargetEntityId = Integer.MIN_VALUE;
        this.lastFailedFeetY = Integer.MIN_VALUE;
        this.lastFailedRadius = Double.NaN;
        this.lastFailedTargetX = Double.NaN;
        this.lastFailedTargetZ = Double.NaN;
    }

    private void rebuildLoop(EntityLivingBase target) {
        int playerFeetY = ctx.playerFeet() == null ? MathHelper.floor(ctx.player().posY) : ctx.playerFeet().getY();
        long rebuildStartNanos = System.nanoTime();
        this.rebuildCache = createRebuildCache(target, playerFeetY);
        boolean hadUsableLoop = hasUsableLoop();
        try {
            List<Vec3d> guideLoop = buildGuideLoop(target, playerFeetY);
            List<OrbitNodePlan> nodePlans = buildOrbitLoop(target, guideLoop, playerFeetY);
            List<BetterBlockPos> rebuiltLoop = extractOrbitPositions(nodePlans);
            List<OrbitArcPlan> rebuiltArcs = buildOrbitArcs(nodePlans);
            List<Vec3d> rebuiltRenderLoop = buildRenderLoop(guideLoop);
            boolean rebuiltUsable = isUsableLoop(rebuiltLoop, rebuiltArcs);

            if (rebuiltUsable) {
                this.orbitLoop = rebuiltLoop;
                this.orbitArcs = rebuiltArcs;
                this.renderLoop = rebuiltRenderLoop;
                this.currentGoal = null;
                this.lastPlannedRadius = this.desiredRadius;
                this.lastPlannedTargetX = target.posX;
                this.lastPlannedTargetZ = target.posZ;
                this.lastPlannedFeetY = ctx.playerFeet() == null ? Integer.MIN_VALUE : ctx.playerFeet().getY();
                this.lastPlanTick = ctx.player() == null ? Integer.MIN_VALUE : ctx.player().ticksExisted;
                this.lastRebuildTick = this.lastPlanTick;
                this.planRevision++;
                clearFailedRebuild();
            } else {
                if (!hadUsableLoop) {
                    this.orbitLoop = rebuiltLoop;
                    this.orbitArcs = rebuiltArcs;
                    this.renderLoop = rebuiltRenderLoop;
                    this.currentGoal = null;
                }
                recordFailedRebuild(target);
            }
            orbitDebug(
                    "rebuildLoop target=%d targetPos=%s radius=%.3f guide=%d nodes=%d arcs=%d render=%d usable=%s retainedPrevious=%s revision=%d",
                    target.getEntityId(), formatEntityXZ(target), this.desiredRadius, guideLoop.size(),
                    rebuiltLoop.size(), rebuiltArcs.size(), rebuiltRenderLoop.size(), rebuiltUsable, hadUsableLoop && !rebuiltUsable,
                    this.planRevision);
            logOrbitNodePlans(target, nodePlans);
            logOrbitArcs(target, rebuiltArcs);
        } finally {
            logRebuildPerf(System.nanoTime() - rebuildStartNanos);
            this.rebuildCache = null;
        }
    }

    private boolean isUsableLoop(List<BetterBlockPos> loop, List<OrbitArcPlan> arcs) {
        return loop != null && arcs != null && loop.size() >= getRequiredLoopPointCount() && arcs.size() == loop.size();
    }

    private RebuildCache createRebuildCache(EntityLivingBase target, int playerFeetY) {
        if (ctx.world() == null || target == null) {
            return new RebuildCache(null, Collections.emptyList(), 0L);
        }
        double scanRadius = Math.max(1.25D, this.desiredRadius + GUIDE_MAX_RADIUS_SHRINK + MAX_SEARCH_RADIUS + 1.25D);
        AxisAlignedBB scanBounds = new AxisAlignedBB(
                target.posX - scanRadius, playerFeetY - 4.0D, target.posZ - scanRadius,
                target.posX + scanRadius, playerFeetY + 2.85D, target.posZ + scanRadius);
        long collectStartNanos = System.nanoTime();
        List<AxisAlignedBB> blockingBoxes = collectOrbitCollisionBoxes(scanBounds);
        return new RebuildCache(scanBounds, blockingBoxes, System.nanoTime() - collectStartNanos);
    }

    private List<AxisAlignedBB> collectOrbitCollisionBoxes(AxisAlignedBB scanBounds) {
        if (ctx.world() == null || scanBounds == null) {
            return Collections.emptyList();
        }
        List<AxisAlignedBB> blockingBoxes = new ArrayList<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int minX = MathHelper.floor(scanBounds.minX) - 1;
        int maxX = MathHelper.floor(scanBounds.maxX) + 1;
        int minY = MathHelper.floor(scanBounds.minY) - 1;
        int maxY = MathHelper.floor(scanBounds.maxY) + 1;
        int minZ = MathHelper.floor(scanBounds.minZ) - 1;
        int maxZ = MathHelper.floor(scanBounds.maxZ) + 1;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutablePos.setPos(x, y, z);
                    IBlockState state = ctx.world().getBlockState(mutablePos);
                    if (state == null) {
                        continue;
                    }
                    Block block = state.getBlock();
                    if (block == null
                            || block == Blocks.CARPET
                            || state.getMaterial().isReplaceable()
                            || state.getCollisionBoundingBox(ctx.world(), mutablePos) == Block.NULL_AABB) {
                        continue;
                    }
                    state.addCollisionBoxToList(ctx.world(), mutablePos, scanBounds, blockingBoxes, null, false);
                }
            }
        }
        return blockingBoxes;
    }

    private void logRebuildPerf(long rebuildDurationNanos) {
        RebuildCache cache = this.rebuildCache;
        if (!isOrbitDebugEnabled() || cache == null) {
            return;
        }
        orbitDebug(
                "rebuildPerf durationMs=%.2f collectMs=%.2f boxes=%d boxChecks=%d boxHits=%d segmentChecks=%d segmentHits=%d baseStandChecks=%d baseStandHits=%d losChecks=%d losHits=%d",
                rebuildDurationNanos / 1_000_000.0D,
                cache.collisionCollectNanos / 1_000_000.0D,
                cache.blockingBoxes.size(),
                cache.boxClearChecks,
                cache.boxClearHits,
                cache.segmentChecks,
                cache.segmentHits,
                cache.baseStandableChecks,
                cache.baseStandableHits,
                cache.lineOfSightChecks,
                cache.lineOfSightHits);
    }

    private List<Vec3d> buildGuideLoop(EntityLivingBase target, int playerFeetY) {
        if (ctx.player() == null || ctx.world() == null || target == null) {
            return Collections.emptyList();
        }

        int samples = getGuidePointCount();
        double startAngle = Math.atan2(ctx.player().posZ - target.posZ, ctx.player().posX - target.posX);
        // Keep the generated orbit loop in the same counterclockwise order as the
        // fallback orbit destination logic so the runtime never flips direction.
        double step = (Math.PI * 2.0D) / samples;
        double routeY = playerFeetY + GUIDE_ROUTE_Y_OFFSET;
        int clippedCount = 0;
        int obstacleAdjustedCount = 0;
        double minRadius = Double.POSITIVE_INFINITY;
        double maxRadius = 0.0D;
        double maxClipLoss = 0.0D;
        int maxClipLossIndex = -1;

        List<Vec3d> guide = new ArrayList<>(samples);
        Vec3d previousGuidePoint = null;
        for (int i = 0; i < samples; i++) {
            double angle = wrapRadians(startAngle + i * step);
            GuidePointPlan plan = resolveGuidePoint(target, angle, step, routeY, playerFeetY, previousGuidePoint);
            Vec3d guidePoint;
            if (plan == null) {
                double[] fallbackPoint = getClippedGuidePoint(target.posX, target.posZ, this.desiredRadius, angle);
                guidePoint = new Vec3d(fallbackPoint[0], routeY, fallbackPoint[1]);
            } else {
                guidePoint = plan.point;
            }
            double sampledRadius = Math.sqrt(horizontalDistSq(guidePoint.x, guidePoint.z, target.posX, target.posZ));
            double clipLoss = Math.max(0.0D, this.desiredRadius - sampledRadius);
            if (clipLoss > 1.0E-3D) {
                clippedCount++;
            }
            if (plan != null && plan.obstacleAdjusted) {
                obstacleAdjustedCount++;
            }
            if (clipLoss > maxClipLoss) {
                maxClipLoss = clipLoss;
                maxClipLossIndex = i;
            }
            minRadius = Math.min(minRadius, sampledRadius);
            maxRadius = Math.max(maxRadius, sampledRadius);
            guide.add(guidePoint);
            previousGuidePoint = guidePoint;
        }
        orbitDebug(
                "guideLoop samples=%d startAngleDeg=%.2f stepDeg=%.2f radius=%.3f clipped=%d obstacleAdjusted=%d minRadius=%.3f maxRadius=%.3f maxClipLoss=%.3f@%d",
                samples, Math.toDegrees(startAngle), Math.toDegrees(step), this.desiredRadius, clippedCount,
                obstacleAdjustedCount,
                minRadius == Double.POSITIVE_INFINITY ? 0.0D : minRadius, maxRadius, maxClipLoss, maxClipLossIndex);
        return guide;
    }

    private GuidePointPlan resolveGuidePoint(EntityLivingBase target, double desiredAngle, double guideAngleStep,
            double routeY, int playerFeetY, Vec3d previousGuidePoint) {
        if (target == null) {
            return null;
        }

        double bestScore = Double.MAX_VALUE;
        GuidePointPlan bestPlan = null;
        double safeAngleStep = Math.max(Math.abs(guideAngleStep), Math.toRadians(1.25D));

        for (int angleStepIndex = 0; angleStepIndex <= GUIDE_ANGLE_ADJUST_STEPS; angleStepIndex++) {
            for (int direction = -1; direction <= 1; direction++) {
                if (angleStepIndex == 0 && direction != 0) {
                    continue;
                }
                if (angleStepIndex > 0 && direction == 0) {
                    continue;
                }

                double angleOffset = angleStepIndex == 0 ? 0.0D : direction * safeAngleStep * angleStepIndex;
                double trialAngle = wrapRadians(desiredAngle + angleOffset);
                double[] clippedPoint = getClippedGuidePoint(target.posX, target.posZ, this.desiredRadius, trialAngle);
                double clippedRadius = Math.sqrt(horizontalDistSq(clippedPoint[0], clippedPoint[1], target.posX, target.posZ));
                double maxShrink = Math.min(GUIDE_MAX_RADIUS_SHRINK, Math.max(0.0D, clippedRadius - 0.85D));
                int shrinkSteps = Math.max(0, (int) Math.ceil(maxShrink / GUIDE_RADIUS_SHRINK_STEP));

                for (int shrinkIndex = 0; shrinkIndex <= shrinkSteps; shrinkIndex++) {
                    double radiusLoss = shrinkIndex * GUIDE_RADIUS_SHRINK_STEP;
                    double trialRadius = Math.max(0.85D, clippedRadius - radiusLoss);
                    double pointX = target.posX + Math.cos(trialAngle) * trialRadius;
                    double pointZ = target.posZ + Math.sin(trialAngle) * trialRadius;
                    Vec3d trialPoint = new Vec3d(pointX, routeY, pointZ);

                    if (!isGuidePointClear(trialPoint)) {
                        continue;
                    }
                    if (previousGuidePoint != null && !isRouteSegmentClear(previousGuidePoint, trialPoint)) {
                        continue;
                    }
                    if (!hasNearbyStandableGuideCandidate(trialPoint, playerFeetY)) {
                        continue;
                    }

                    double score = Math.abs(angleOffset) * 18.0D
                            + radiusLoss * 12.0D
                            + horizontalDistSq(pointX, pointZ, clippedPoint[0], clippedPoint[1]) * 4.5D;
                    if (score < bestScore) {
                        bestScore = score;
                        bestPlan = new GuidePointPlan(trialPoint,
                                Math.abs(angleOffset) > 1.0E-4D || radiusLoss > 1.0E-4D);
                    }
                    if (angleStepIndex == 0 && shrinkIndex == 0) {
                        return bestPlan;
                    }
                }
            }
        }

        if (bestPlan != null) {
            return bestPlan;
        }

        double[] fallback = getClippedGuidePoint(target.posX, target.posZ, this.desiredRadius, desiredAngle);
        return new GuidePointPlan(new Vec3d(fallback[0], routeY, fallback[1]), true);
    }

    private List<OrbitNodePlan> buildOrbitLoop(EntityLivingBase target, List<Vec3d> guideLoop, int playerFeetY) {
        if (ctx.player() == null || ctx.world() == null || target == null || guideLoop == null || guideLoop.isEmpty()) {
            return Collections.emptyList();
        }

        List<OrbitNodePlan> planned = new ArrayList<>();
        List<Vec3d> deferredLeadingGuidePoints = new ArrayList<>();
        int nullGuideCount = 0;
        int currentNullStreak = 0;
        int maxNullStreak = 0;
        int nullRunStart = -1;
        int largeNodeJumpCount = 0;
        BetterBlockPos previous = null;
        for (int guideIndex = 0; guideIndex < guideLoop.size(); guideIndex++) {
            Vec3d guidePoint = guideLoop.get(guideIndex);
            BetterBlockPos candidate = findNodeForGuidePoint(target, guidePoint, previous, playerFeetY);
            if (candidate == null) {
                nullGuideCount++;
                if (currentNullStreak == 0) {
                    nullRunStart = guideIndex;
                }
                currentNullStreak++;
                if (planned.isEmpty()) {
                    deferredLeadingGuidePoints.add(guidePoint);
                } else {
                    planned.get(planned.size() - 1).appendGuidePoint(guidePoint);
                }
                continue;
            }
            if (currentNullStreak > 0) {
                maxNullStreak = Math.max(maxNullStreak, currentNullStreak);
                orbitDebug("guideNullRun start=%d end=%d count=%d previousNode=%s resumedNode=%s", nullRunStart,
                        guideIndex - 1, currentNullStreak, formatPos(previous), formatPos(candidate));
                currentNullStreak = 0;
                nullRunStart = -1;
            }
            if (previous != null) {
                double nodeJump = Math.sqrt(horizontalDistSq(candidate.x + 0.5D, candidate.z + 0.5D, previous.x + 0.5D,
                        previous.z + 0.5D));
                if (nodeJump > 1.5D) {
                    largeNodeJumpCount++;
                    orbitDebug("guideNodeJump index=%d jump=%.3f previous=%s candidate=%s guide=%s", guideIndex, nodeJump,
                            formatPos(previous), formatPos(candidate), formatVec2(guidePoint));
                }
            }
            if (!planned.isEmpty() && candidate.equals(planned.get(planned.size() - 1).position)) {
                planned.get(planned.size() - 1).appendGuidePoint(guidePoint);
            } else {
                OrbitNodePlan nodePlan = new OrbitNodePlan(candidate);
                nodePlan.appendGuidePoint(guidePoint);
                planned.add(nodePlan);
            }
            previous = candidate;
        }
        if (currentNullStreak > 0) {
            maxNullStreak = Math.max(maxNullStreak, currentNullStreak);
            orbitDebug("guideNullRun start=%d end=%d count=%d previousNode=%s resumedNode=end_of_loop", nullRunStart,
                    guideLoop.size() - 1, currentNullStreak, formatPos(previous));
        }

        if (!planned.isEmpty() && !deferredLeadingGuidePoints.isEmpty()) {
            planned.get(planned.size() - 1).appendGuidePoints(deferredLeadingGuidePoints);
        }

        boolean mergedEndpoints = false;
        if (planned.size() > 1 && planned.get(0).position.equals(planned.get(planned.size() - 1).position)) {
            OrbitNodePlan tail = planned.get(planned.size() - 1);
            tail.appendGuidePoints(planned.get(0).guidePoints);
            planned.remove(0);
            mergedEndpoints = true;
        }
        orbitDebug(
                "orbitLoopSummary guide=%d plannedNodes=%d deferredLeading=%d nullGuides=%d maxNullStreak=%d largeNodeJumps=%d mergedEndpoints=%s",
                guideLoop.size(), planned.size(), deferredLeadingGuidePoints.size(), nullGuideCount, maxNullStreak,
                largeNodeJumpCount, mergedEndpoints);
        int requiredLoopPoints = getRequiredLoopPointCount();
        if (planned.size() < requiredLoopPoints) {
            orbitDebug("orbitLoopRejected plannedNodes=%d minRequired=%d", planned.size(), requiredLoopPoints);
            return Collections.emptyList();
        }
        return planned;
    }

    private BetterBlockPos findNodeForGuidePoint(EntityLivingBase target, Vec3d guidePoint, BetterBlockPos previous,
            int playerFeetY) {
        if (target == null || guidePoint == null) {
            return null;
        }
        double desiredX = guidePoint.x;
        double desiredZ = guidePoint.z;
        double desiredRadius = Math.sqrt(horizontalDistSq(desiredX, desiredZ, target.posX, target.posZ));
        double desiredAngle = Math.atan2(desiredZ - target.posZ, desiredX - target.posX);
        int baseX = MathHelper.floor(desiredX);
        int baseZ = MathHelper.floor(desiredZ);
        int maxFeetY = playerFeetY + 1;
        int minFeetY = playerFeetY - 4;
        BetterBlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        Vec3d previousCenter = previous == null ? null : nodeCenter(previous);

        for (int searchRadius = 0; searchRadius <= MAX_SEARCH_RADIUS; searchRadius++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    if (searchRadius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != searchRadius) {
                        continue;
                    }
                    for (int y = maxFeetY; y >= minFeetY; y--) {
                        BetterBlockPos candidate = new BetterBlockPos(baseX + dx, y, baseZ + dz);
                        if (!isStandableOrbitFeet(candidate, target, maxFeetY)) {
                            continue;
                        }

                        double centerX = candidate.x + 0.5D;
                        double centerZ = candidate.z + 0.5D;
                        if (previousCenter != null) {
                            Vec3d candidateCenter = new Vec3d(centerX, candidate.y + GUIDE_ROUTE_Y_OFFSET, centerZ);
                            if (Math.abs(candidateCenter.y - previousCenter.y) > MAX_VERTICAL_STEP_BETWEEN_NODES
                                    || !isRouteSegmentClear(previousCenter, candidateCenter)) {
                                continue;
                            }
                        }
                        double actualRadius = Math.sqrt((centerX - target.posX) * (centerX - target.posX)
                                + (centerZ - target.posZ) * (centerZ - target.posZ));
                        double actualAngle = Math.atan2(centerZ - target.posZ, centerX - target.posX);
                        double radiusPenalty = Math.abs(actualRadius - desiredRadius) * 6.0D;
                        double anglePenalty = Math.abs(wrapRadians(actualAngle - desiredAngle)) * 8.0D;
                        double desiredPenalty = horizontalDistSq(centerX, centerZ, desiredX, desiredZ) * 2.0D;
                        double continuityPenalty = 0.0D;
                        if (previous != null) {
                            double stepDistanceSq = horizontalDistSq(centerX, centerZ, previous.x + 0.5D,
                                    previous.z + 0.5D);
                            double stepDistance = Math.sqrt(stepDistanceSq);
                            continuityPenalty = stepDistanceSq * 0.45D
                                    + Math.max(0.0D, stepDistance - 1.35D) * 8.0D;
                        }
                        double heightPenalty = Math.abs(candidate.y - playerFeetY) * 0.9D;
                        double score = radiusPenalty + anglePenalty + desiredPenalty + continuityPenalty + heightPenalty;

                        if (score < bestScore) {
                            bestScore = score;
                            best = candidate;
                        }
                    }
                }
            }
        }

        return best;
    }

    private boolean hasNearbyStandableGuideCandidate(Vec3d guidePoint, int playerFeetY) {
        if (guidePoint == null) {
            return false;
        }
        int baseX = MathHelper.floor(guidePoint.x);
        int baseZ = MathHelper.floor(guidePoint.z);
        int maxFeetY = playerFeetY + 1;
        int minFeetY = playerFeetY - 2;
        for (int searchRadius = 0; searchRadius <= 1; searchRadius++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    if (searchRadius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != searchRadius) {
                        continue;
                    }
                    for (int y = maxFeetY; y >= minFeetY; y--) {
                        if (isBaseStandableOrbitFeet(new BetterBlockPos(baseX + dx, y, baseZ + dz), maxFeetY)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isStandableOrbitFeet(BetterBlockPos standPos, EntityLivingBase target, int maxFeetY) {
        if (target == null || !isBaseStandableOrbitFeet(standPos, maxFeetY)) {
            return false;
        }
        return !KillAuraHandler.requireLineOfSight || hasLineOfSightFromStandPosCached(standPos, target);
    }

    private boolean isBaseStandableOrbitFeet(BetterBlockPos standPos, int maxFeetY) {
        if (ctx.world() == null || standPos == null || standPos.y > maxFeetY) {
            return false;
        }
        RebuildCache cache = this.rebuildCache;
        long key = BetterBlockPos.longHash(standPos);
        if (cache != null) {
            Boolean cached = cache.baseStandableCache.get(key);
            if (cached != null) {
                cache.baseStandableHits++;
                return cached;
            }
            cache.baseStandableChecks++;
        }

        IBlockState feetState = ctx.world().getBlockState(standPos);
        IBlockState headState = ctx.world().getBlockState(standPos.up());
        IBlockState supportState = ctx.world().getBlockState(standPos.down());
        boolean standable = !feetState.getMaterial().blocksMovement()
                && !headState.getMaterial().blocksMovement()
                && supportState.getMaterial().blocksMovement()
                && supportState.isFullCube()
                && isOrbitPlayerBoxClear(standPos.x + 0.5D, standPos.y, standPos.z + 0.5D,
                        ORBIT_PLAYER_HALF_WIDTH, ORBIT_CLEARANCE_MARGIN);
        if (standable && AutoFollowHandler.hasActiveLockChaseRestriction()) {
            standable = AutoFollowHandler.isPositionWithinActiveLockChaseBounds(standPos.x + 0.5D, standPos.z + 0.5D);
        }
        if (cache != null) {
            cache.baseStandableCache.put(key, standable);
        }
        return standable;
    }

    private boolean hasLineOfSightFromStandPosCached(BetterBlockPos standPos, EntityLivingBase target) {
        RebuildCache cache = this.rebuildCache;
        long key = BetterBlockPos.longHash(standPos);
        if (cache != null) {
            Boolean cached = cache.lineOfSightCache.get(key);
            if (cached != null) {
                cache.lineOfSightHits++;
                return cached;
            }
            cache.lineOfSightChecks++;
        }
        boolean clear = hasLineOfSightFromStandPos(standPos, target);
        if (cache != null) {
            cache.lineOfSightCache.put(key, clear);
        }
        return clear;
    }

    private boolean hasLineOfSightFromStandPos(BetterBlockPos standPos, EntityLivingBase target) {
        if (ctx.world() == null || standPos == null || target == null) {
            return false;
        }
        Vec3d eyePos = new Vec3d(standPos.x + 0.5D, standPos.y + 1.62D, standPos.z + 0.5D);
        Vec3d targetEye = new Vec3d(target.posX, target.posY + target.getEyeHeight() * 0.85D, target.posZ);
        RayTraceResult hit = ctx.world().rayTraceBlocks(eyePos, targetEye, false, true, false);
        return hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK;
    }

    private boolean isGuidePointClear(Vec3d guidePoint) {
        if (guidePoint == null) {
            return false;
        }
        return isOrbitPlayerBoxClear(guidePoint.x, guidePoint.y - GUIDE_ROUTE_Y_OFFSET, guidePoint.z,
                ORBIT_PLAYER_HALF_WIDTH, ORBIT_CLEARANCE_MARGIN);
    }

    private boolean isRouteSegmentClear(Vec3d start, Vec3d end) {
        if (ctx.world() == null || start == null || end == null) {
            return false;
        }
        RebuildCache cache = this.rebuildCache;
        if (cache != null) {
            SegmentKey cacheKey = SegmentKey.of(start, end);
            Boolean cached = cache.segmentClearCache.get(cacheKey);
            if (cached != null) {
                cache.segmentHits++;
                return cached;
            }
            cache.segmentChecks++;
            boolean clear = isRouteSegmentClearUncached(start, end);
            cache.segmentClearCache.put(cacheKey, clear);
            return clear;
        }
        return isRouteSegmentClearUncached(start, end);
    }

    private boolean isRouteSegmentClearUncached(Vec3d start, Vec3d end) {
        double segmentLength = start.distanceTo(end);
        int sampleCount = Math.max(2, (int) Math.ceil(segmentLength / ROUTE_SAMPLE_STEP));
        for (int sample = 0; sample <= sampleCount; sample++) {
            double t = sample / (double) sampleCount;
            double centerX = start.x + (end.x - start.x) * t;
            double routeY = start.y + (end.y - start.y) * t;
            double centerZ = start.z + (end.z - start.z) * t;
            if (!isOrbitPlayerBoxClear(centerX, routeY - GUIDE_ROUTE_Y_OFFSET, centerZ,
                    ORBIT_PLAYER_HALF_WIDTH, ORBIT_CLEARANCE_MARGIN)) {
                return false;
            }
        }
        return true;
    }

    private boolean isOrbitPlayerBoxClear(double centerX, double feetY, double centerZ, double halfWidth,
            double extraMargin) {
        if (ctx.world() == null) {
            return false;
        }
        RebuildCache cache = this.rebuildCache;
        BoxKey cacheKey = cache == null ? null : new BoxKey(centerX, feetY, centerZ, halfWidth, extraMargin);
        if (cache != null) {
            Boolean cached = cache.boxClearCache.get(cacheKey);
            if (cached != null) {
                cache.boxClearHits++;
                return cached;
            }
            cache.boxClearChecks++;
        }
        AxisAlignedBB playerBox = new AxisAlignedBB(
                centerX - halfWidth, feetY, centerZ - halfWidth,
                centerX + halfWidth, feetY + 1.799D, centerZ + halfWidth)
                .grow(extraMargin, 0.0D, extraMargin);
        boolean clear;
        if (cache != null && cache.scanBounds != null
                && cache.scanBounds.minX <= playerBox.minX && cache.scanBounds.maxX >= playerBox.maxX
                && cache.scanBounds.minY <= playerBox.minY && cache.scanBounds.maxY >= playerBox.maxY
                && cache.scanBounds.minZ <= playerBox.minZ && cache.scanBounds.maxZ >= playerBox.maxZ) {
            clear = true;
            for (AxisAlignedBB blockingBox : cache.blockingBoxes) {
                if (playerBox.intersects(blockingBox)) {
                    clear = false;
                    break;
                }
            }
        } else {
            clear = ctx.world().getCollisionBoxes(null, playerBox).isEmpty();
        }
        if (cache != null) {
            cache.boxClearCache.put(cacheKey, clear);
        }
        return clear;
    }

    private List<Vec3d> buildRenderLoop(List<Vec3d> guideLoop) {
        if (guideLoop == null || guideLoop.size() < 2) {
            return Collections.emptyList();
        }
        List<Vec3d> render = new ArrayList<>(guideLoop.size() + 1);
        for (Vec3d point : guideLoop) {
            render.add(new Vec3d(point.x, point.y - GUIDE_ROUTE_Y_OFFSET + GUIDE_RENDER_Y_OFFSET, point.z));
        }
        Vec3d first = render.get(0);
        render.add(new Vec3d(first.x, first.y, first.z));
        return render;
    }

    private int getGuidePointCount() {
        return MathHelper.clamp(KillAuraHandler.getConfiguredHuntOrbitSamplePoints(), MIN_GUIDE_POINTS, MAX_GUIDE_POINTS);
    }

    private int getRequiredLoopPointCount() {
        return Math.max(KillAuraHandler.MIN_HUNT_ORBIT_SAMPLE_POINTS,
                Math.min(DEFAULT_MIN_LOOP_POINTS, getGuidePointCount()));
    }

    private double[] getClippedGuidePoint(double centerX, double centerZ, double radius, double angle) {
        double dirX = Math.cos(angle);
        double dirZ = Math.sin(angle);
        double endX = centerX + dirX * radius;
        double endZ = centerZ + dirZ * radius;

        if (!AutoFollowHandler.hasActiveLockChaseRestriction()
                || AutoFollowHandler.isPositionWithinActiveLockChaseBounds(endX, endZ)) {
            return new double[] { endX, endZ };
        }

        double low = 0.0D;
        double high = radius;
        for (int i = 0; i < 14; i++) {
            double mid = (low + high) * 0.5D;
            double testX = centerX + dirX * mid;
            double testZ = centerZ + dirZ * mid;
            if (AutoFollowHandler.isPositionWithinActiveLockChaseBounds(testX, testZ)) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return new double[] { centerX + dirX * low, centerZ + dirZ * low };
    }

    private PathingCommand tryContinueCurrentOrbitPath() {
        PathingBehavior pathingBehavior = baritone.getPathingBehavior();
        PathExecutor currentExecutor = pathingBehavior.getCurrent();
        boolean currentRouteActive = false;
        if (currentExecutor != null && currentExecutor.getPath() instanceof OrbitRoutePath) {
            OrbitRoutePath currentRoute = (OrbitRoutePath) currentExecutor.getPath();
            if (isRouteFromCurrentPlan(currentRoute)) {
                currentRouteActive = true;
                this.currentGoal = currentRoute.getGoal();
                int currentPosition = Math.max(0, currentExecutor.getPosition());
                int remainingMovements = currentRoute.movements().size() - currentPosition;
                if (remainingMovements > PREBUILD_NEXT_ROUTE_REMAINING_MOVEMENTS) {
                    return new PathingCommand(currentRoute.getGoal(), PathingCommandType.SET_GOAL_AND_PATH);
                }
            }
        }

        PathExecutor nextExecutor = pathingBehavior.getNext();
        if (nextExecutor != null && nextExecutor.getPath() instanceof OrbitRoutePath) {
            OrbitRoutePath nextRoute = (OrbitRoutePath) nextExecutor.getPath();
            if (isRouteFromCurrentPlan(nextRoute)) {
                if (currentRouteActive && this.currentGoal != null) {
                    return new PathingCommand(this.currentGoal, PathingCommandType.SET_GOAL_AND_PATH);
                }
                this.currentGoal = nextRoute.getGoal();
                return new PathingCommand(nextRoute.getGoal(), PathingCommandType.SET_GOAL_AND_PATH);
            }
        }
        return null;
    }

    private boolean isRouteFromCurrentPlan(OrbitRoutePath route) {
        if (route == null) {
            return false;
        }
        String routeKey = route.getRouteKey();
        String prefix = this.planRevision + ":";
        return routeKey != null && routeKey.startsWith(prefix);
    }

    private OrbitRoutePath determineDesiredRoutePath() {
        int startIndex = chooseNearestLoopNodeIndex(ctx.playerFeet());
        if (startIndex < 0) {
            orbitDebug("determineRoute source=nearest failed playerFeet=%s", formatPos(ctx.playerFeet()));
            return null;
        }
        double loopDistance = getPlayerDistanceToLoop();
        if (loopDistance > MAX_LOOP_ENTRY_DISTANCE) {
            orbitDebug(
                    "determineRoute source=approach startIndex=%d playerFeet=%s loopDistance=%.3f threshold=%.3f",
                    startIndex, formatPos(ctx.playerFeet()), loopDistance, MAX_LOOP_ENTRY_DISTANCE);
            return null;
        }

        PathingBehavior pathingBehavior = baritone.getPathingBehavior();
        PathExecutor currentExecutor = pathingBehavior.getCurrent();
        if (currentExecutor != null && currentExecutor.getPath() instanceof OrbitRoutePath) {
            OrbitRoutePath currentRoute = (OrbitRoutePath) currentExecutor.getPath();
            OrbitRoutePath desiredNext = buildOrbitRoutePath(currentRoute.getEndNodeIndex());
            if (desiredNext != null) {
                orbitDebug("determineRoute source=current currentKey=%s currentEndNode=%d nextKey=%s",
                        currentRoute.getRouteKey(), currentRoute.getEndNodeIndex(), desiredNext.getRouteKey());
                return desiredNext;
            }
        }

        PathExecutor nextExecutor = pathingBehavior.getNext();
        if (nextExecutor != null && nextExecutor.getPath() instanceof OrbitRoutePath) {
            OrbitRoutePath nextRoute = (OrbitRoutePath) nextExecutor.getPath();
            orbitDebug("determineRoute source=next nextKey=%s startNode=%d endNode=%d", nextRoute.getRouteKey(),
                    nextRoute.getStartNodeIndex(), nextRoute.getEndNodeIndex());
            return nextRoute;
        }

        orbitDebug("determineRoute source=nearest startIndex=%d playerFeet=%s", startIndex, formatPos(ctx.playerFeet()));
        return buildOrbitRoutePath(startIndex);
    }

    private Goal getActiveOrbitCommandGoal(OrbitRoutePath desiredPath) {
        if (desiredPath == null) {
            return this.currentGoal;
        }
        PathExecutor currentExecutor = baritone.getPathingBehavior().getCurrent();
        if (currentExecutor != null && currentExecutor.getPath() instanceof OrbitRoutePath) {
            OrbitRoutePath currentRoute = (OrbitRoutePath) currentExecutor.getPath();
            if (isRouteFromCurrentPlan(currentRoute)) {
                return currentRoute.getGoal();
            }
        }
        return desiredPath.getGoal();
    }

    private OrbitRoutePath buildOrbitRoutePath(int startIndex) {
        if (startIndex < 0 || startIndex >= this.orbitLoop.size() || this.orbitArcs.size() != this.orbitLoop.size()) {
            orbitDebug("buildRoute rejected startIndex=%d nodes=%d arcs=%d", startIndex, this.orbitLoop.size(),
                    this.orbitArcs.size());
            return null;
        }

        CalculationContext context = new CalculationContext(baritone, false);
        List<BetterBlockPos> positions = new ArrayList<>();
        List<IMovement> movements = new ArrayList<>();
        positions.add(this.orbitLoop.get(startIndex));
        String breakReason = "completed";

        int maxArcCount = Math.min(getOrbitSegmentArcCount(), this.orbitArcs.size() - 1);
        for (int offset = 0; offset < maxArcCount; offset++) {
            int arcIndex = (startIndex + offset) % this.orbitArcs.size();
            OrbitArcPlan arc = this.orbitArcs.get(arcIndex);
            if (arc == null || arc.routePoints.length < 2) {
                breakReason = "missing_arc@" + arcIndex;
                break;
            }

            BetterBlockPos currentPos = positions.get(positions.size() - 1);
            if (!currentPos.equals(arc.src) || positions.contains(arc.dest)) {
                breakReason = String.format(Locale.ROOT, "arc_mismatch@%d current=%s src=%s duplicateDest=%s", arcIndex,
                        formatPos(currentPos), formatPos(arc.src), positions.contains(arc.dest));
                break;
            }

            MovementRouteTraverse movement = new MovementRouteTraverse(baritone, arc.src, arc.dest, false,
                    arc.routePoints);
            double cost = movement.calculateCost(context);
            if (cost >= ActionCosts.COST_INF) {
                breakReason = "cost_inf@" + arcIndex;
                break;
            }
            movement.override(cost);
            movement.checkLoadedChunk(context);
            movements.add(movement);
            positions.add(arc.dest);
        }

        if (movements.isEmpty()) {
            orbitDebug("buildRoute failed startIndex=%d maxArcCount=%d breakReason=%s", startIndex, maxArcCount,
                    breakReason);
            return null;
        }

        BetterBlockPos dest = positions.get(positions.size() - 1);
        int endNodeIndex = (startIndex + movements.size()) % this.orbitLoop.size();
        String routeKey = this.planRevision + ":" + startIndex + ":" + endNodeIndex + ":" + movements.size();
        Goal goal = new GoalBlock(dest);
        orbitDebug("buildRoute startIndex=%d endNodeIndex=%d positions=%d movements=%d dest=%s routeKey=%s breakReason=%s",
                startIndex, endNodeIndex, positions.size(), movements.size(), formatPos(dest), routeKey, breakReason);
        return new OrbitRoutePath(goal, positions, movements, 0, routeKey, startIndex, endNodeIndex);
    }

    private List<BetterBlockPos> extractOrbitPositions(List<OrbitNodePlan> nodePlans) {
        if (nodePlans == null || nodePlans.isEmpty()) {
            return Collections.emptyList();
        }
        List<BetterBlockPos> positions = new ArrayList<>(nodePlans.size());
        for (OrbitNodePlan nodePlan : nodePlans) {
            positions.add(nodePlan.position);
        }
        return positions;
    }

    private List<OrbitArcPlan> buildOrbitArcs(List<OrbitNodePlan> nodePlans) {
        if (nodePlans == null || nodePlans.size() < getRequiredLoopPointCount()) {
            return Collections.emptyList();
        }
        List<OrbitArcPlan> arcs = new ArrayList<>(nodePlans.size());
        for (int i = 0; i < nodePlans.size(); i++) {
            OrbitNodePlan current = nodePlans.get(i);
            OrbitNodePlan next = nodePlans.get((i + 1) % nodePlans.size());
            if (current.position.equals(next.position)) {
                continue;
            }

            List<Vec3d> routePoints = new ArrayList<>();
            appendRoutePoints(routePoints, current.guidePoints);
            appendRoutePoint(routePoints, getFirstGuidePoint(next));
            routePoints = sanitizeArcRoutePoints(current.position, next.position, routePoints);
            if (routePoints.size() < 2) {
                orbitDebug("buildArc skipped index=%d src=%s dest=%s routePoints=%d", i, formatPos(current.position),
                        formatPos(next.position), routePoints.size());
                continue;
            }
            arcs.add(new OrbitArcPlan(current.position, next.position, routePoints.toArray(new Vec3d[0])));
        }
        if (arcs.size() != nodePlans.size()) {
            orbitDebug("buildArcs rejected arcCount=%d nodePlans=%d", arcs.size(), nodePlans.size());
        }
        return arcs.size() == nodePlans.size() ? arcs : Collections.emptyList();
    }

    private List<Vec3d> sanitizeArcRoutePoints(BetterBlockPos src, BetterBlockPos dest, List<Vec3d> rawRoutePoints) {
        if (src == null || dest == null) {
            return Collections.emptyList();
        }

        Vec3d srcCenter = nodeCenter(src);
        Vec3d destCenter = nodeCenter(dest);
        List<Vec3d> sanitized = new ArrayList<>();
        sanitized.add(srcCenter);

        if (rawRoutePoints != null) {
            Vec3d previous = srcCenter;
            for (Vec3d point : rawRoutePoints) {
                if (point == null || !isGuidePointClear(point) || !isRouteSegmentClear(previous, point)) {
                    continue;
                }
                appendRoutePoint(sanitized, point);
                previous = point;
            }
        }

        Vec3d lastPoint = sanitized.get(sanitized.size() - 1);
        if (!isRouteSegmentClear(lastPoint, destCenter)) {
            List<Vec3d> fallback = buildFallbackArcRoute(srcCenter, destCenter);
            return fallback.isEmpty() ? Collections.emptyList() : fallback;
        }

        appendRoutePoint(sanitized, destCenter);
        return sanitized;
    }

    private List<Vec3d> buildFallbackArcRoute(Vec3d srcCenter, Vec3d destCenter) {
        if (srcCenter == null || destCenter == null) {
            return Collections.emptyList();
        }
        if (isRouteSegmentClear(srcCenter, destCenter)) {
            List<Vec3d> direct = new ArrayList<>();
            direct.add(srcCenter);
            direct.add(destCenter);
            return direct;
        }

        double midY = Math.min(srcCenter.y, destCenter.y);
        Vec3d viaX = new Vec3d(destCenter.x, midY, srcCenter.z);
        if (isGuidePointClear(viaX)
                && isRouteSegmentClear(srcCenter, viaX)
                && isRouteSegmentClear(viaX, destCenter)) {
            List<Vec3d> route = new ArrayList<>();
            route.add(srcCenter);
            route.add(viaX);
            route.add(destCenter);
            return route;
        }

        Vec3d viaZ = new Vec3d(srcCenter.x, midY, destCenter.z);
        if (isGuidePointClear(viaZ)
                && isRouteSegmentClear(srcCenter, viaZ)
                && isRouteSegmentClear(viaZ, destCenter)) {
            List<Vec3d> route = new ArrayList<>();
            route.add(srcCenter);
            route.add(viaZ);
            route.add(destCenter);
            return route;
        }
        return Collections.emptyList();
    }

    private Vec3d getFirstGuidePoint(OrbitNodePlan nodePlan) {
        if (nodePlan == null || nodePlan.guidePoints.isEmpty()) {
            return null;
        }
        return nodePlan.guidePoints.get(0);
    }

    private void appendRoutePoint(List<Vec3d> routePoints, Vec3d point) {
        if (routePoints == null || point == null) {
            return;
        }
        if (routePoints.isEmpty() || routePoints.get(routePoints.size() - 1).squareDistanceTo(point) > 1.0E-4D) {
            routePoints.add(point);
        }
    }

    private void appendRoutePoints(List<Vec3d> routePoints, List<Vec3d> points) {
        if (points == null) {
            return;
        }
        for (Vec3d point : points) {
            appendRoutePoint(routePoints, point);
        }
    }

    private int chooseNearestLoopNodeIndex(BetterBlockPos playerFeet) {
        if (playerFeet == null || this.orbitLoop.isEmpty()) {
            return -1;
        }
        int nearestIndex = -1;
        double bestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < this.orbitLoop.size(); i++) {
            BetterBlockPos node = this.orbitLoop.get(i);
            double distSq = playerFeet.distanceSq(node);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearestIndex = i;
            }
        }
        if (nearestIndex >= 0) {
            orbitDebug("nearestLoopNode playerFeet=%s index=%d dist=%.3f node=%s", formatPos(playerFeet), nearestIndex,
                    Math.sqrt(bestDistSq), formatPos(this.orbitLoop.get(nearestIndex)));
        }
        return nearestIndex;
    }

    private BetterBlockPos chooseNearestLoopNode(BetterBlockPos playerFeet) {
        int index = chooseNearestLoopNodeIndex(playerFeet);
        if (index < 0 || index >= this.orbitLoop.size()) {
            return null;
        }
        return this.orbitLoop.get(index);
    }

    private int getOrbitSegmentArcCount() {
        // Keep orbit routing single-step so the active goal always stays on the
        // immediate next loop node instead of jumping far ahead around the ring.
        return this.orbitArcs.size() > 1 ? 1 : 0;
    }

    private void clearRuntime() {
        orbitDebug("clearRuntime state=%s targetEntityId=%d nodes=%d arcs=%d render=%d", this.state, this.targetEntityId,
                this.orbitLoop.size(), this.orbitArcs.size(), this.renderLoop.size());
        this.state = State.IDLE;
        this.targetEntityId = Integer.MIN_VALUE;
        this.rebuildCache = null;
        this.currentGoal = null;
        this.orbitLoop = Collections.emptyList();
        this.orbitArcs = Collections.emptyList();
        this.renderLoop = Collections.emptyList();
        this.lastPlanTick = Integer.MIN_VALUE;
        this.lastRequestTick = Integer.MIN_VALUE;
        this.lastRebuildTick = Integer.MIN_VALUE;
    }

    private double horizontalDistSq(double leftX, double leftZ, double rightX, double rightZ) {
        double dx = leftX - rightX;
        double dz = leftZ - rightZ;
        return dx * dx + dz * dz;
    }

    private double wrapRadians(double radians) {
        double wrapped = radians;
        while (wrapped <= -Math.PI) {
            wrapped += Math.PI * 2.0D;
        }
        while (wrapped > Math.PI) {
            wrapped -= Math.PI * 2.0D;
        }
        return wrapped;
    }

    private double getPlayerDistanceToLoop() {
        if (ctx.player() == null || this.renderLoop.size() < 2) {
            return Double.POSITIVE_INFINITY;
        }
        Vec3d playerPos = new Vec3d(ctx.player().posX, 0.0D, ctx.player().posZ);
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (int i = 0; i < this.renderLoop.size() - 1; i++) {
            Vec3d start = horizontalOnly(this.renderLoop.get(i));
            Vec3d end = horizontalOnly(this.renderLoop.get(i + 1));
            Vec3d nearest = nearestPointOnSegment(playerPos, start, end);
            bestDistanceSq = Math.min(bestDistanceSq, playerPos.squareDistanceTo(nearest));
        }
        return bestDistanceSq == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : Math.sqrt(bestDistanceSq);
    }

    private Vec3d nearestPointOnSegment(Vec3d point, Vec3d start, Vec3d end) {
        Vec3d segment = end.subtract(start);
        double lengthSq = segment.lengthSquared();
        if (lengthSq <= 1.0E-6D) {
            return start;
        }
        double t = point.subtract(start).dotProduct(segment) / lengthSq;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return start.add(segment.scale(t));
    }

    private Vec3d horizontalOnly(Vec3d vec) {
        return new Vec3d(vec.x, 0.0D, vec.z);
    }

    private void logOrbitNodePlans(EntityLivingBase target, List<OrbitNodePlan> nodePlans) {
        if (!isOrbitDebugEnabled() || target == null || nodePlans == null || nodePlans.isEmpty()) {
            return;
        }
        for (int i = 0; i < nodePlans.size(); i++) {
            OrbitNodePlan nodePlan = nodePlans.get(i);
            RouteStats guideStats = analyzeRoute(nodePlan.guidePoints);
            Vec3d firstGuide = nodePlan.guidePoints.isEmpty() ? null : nodePlan.guidePoints.get(0);
            Vec3d lastGuide = nodePlan.guidePoints.isEmpty() ? null : nodePlan.guidePoints.get(nodePlan.guidePoints.size() - 1);
            orbitDebug(
                    "nodePlan idx=%d node=%s nodeAngleDeg=%.2f guideCount=%d guideLength=%.3f maxGuideStep=%.3f maxGuideStepIndex=%d first=%s last=%s",
                    i, formatPos(nodePlan.position), angleDegFromTarget(target, nodeCenter(nodePlan.position)),
                    nodePlan.guidePoints.size(), guideStats.length, guideStats.maxStep, guideStats.maxStepIndex,
                    formatVec2(firstGuide), formatVec2(lastGuide));
        }
    }

    private void logOrbitArcs(EntityLivingBase target, List<OrbitArcPlan> arcs) {
        if (!isOrbitDebugEnabled() || target == null || arcs == null || arcs.isEmpty()) {
            return;
        }
        for (int i = 0; i < arcs.size(); i++) {
            OrbitArcPlan arc = arcs.get(i);
            RouteStats stats = analyzeRoute(arc.routePoints);
            orbitDebug(
                    "arcPlan idx=%d src=%s srcAngleDeg=%.2f dest=%s destAngleDeg=%.2f routePoints=%d length=%.3f maxStep=%.3f maxStepIndex=%d suspicious=%s start=%s end=%s",
                    i, formatPos(arc.src), angleDegFromTarget(target, nodeCenter(arc.src)), formatPos(arc.dest),
                    angleDegFromTarget(target, nodeCenter(arc.dest)), arc.routePoints.length, stats.length, stats.maxStep,
                    stats.maxStepIndex, stats.maxStep > SUSPICIOUS_ROUTE_STEP, formatVec2(stats.maxStepStart),
                    formatVec2(stats.maxStepEnd));
        }
    }

    private RouteStats analyzeRoute(List<Vec3d> points) {
        if (points == null || points.isEmpty()) {
            return RouteStats.EMPTY;
        }
        return analyzeRoute(points.toArray(new Vec3d[0]));
    }

    private RouteStats analyzeRoute(Vec3d[] points) {
        if (points == null || points.length < 2) {
            return RouteStats.EMPTY;
        }
        double length = 0.0D;
        double maxStep = 0.0D;
        int maxStepIndex = -1;
        Vec3d maxStepStart = null;
        Vec3d maxStepEnd = null;
        for (int i = 0; i < points.length - 1; i++) {
            double step = points[i].distanceTo(points[i + 1]);
            length += step;
            if (step > maxStep) {
                maxStep = step;
                maxStepIndex = i;
                maxStepStart = points[i];
                maxStepEnd = points[i + 1];
            }
        }
        return new RouteStats(length, maxStep, maxStepIndex, maxStepStart, maxStepEnd);
    }

    private boolean isOrbitDebugEnabled() {
        return ModConfig.isDebugFlagEnabled(DebugModule.KILL_AURA_ORBIT);
    }

    private void orbitDebug(String format, Object... args) {
        if (!isOrbitDebugEnabled()) {
            return;
        }
        ModConfig.debugLog(DebugModule.KILL_AURA_ORBIT, String.format(Locale.ROOT, format, args));
    }

    private Vec3d nodeCenter(BetterBlockPos pos) {
        if (pos == null) {
            return Vec3d.ZERO;
        }
        return new Vec3d(pos.x + 0.5D, pos.y + GUIDE_ROUTE_Y_OFFSET, pos.z + 0.5D);
    }

    private double angleDegFromTarget(EntityLivingBase target, Vec3d point) {
        if (target == null || point == null) {
            return 0.0D;
        }
        return Math.toDegrees(Math.atan2(point.z - target.posZ, point.x - target.posX));
    }

    private String formatEntityXZ(EntityLivingBase entity) {
        if (entity == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "(%.3f, %.3f)", entity.posX, entity.posZ);
    }

    private String formatPos(BetterBlockPos pos) {
        if (pos == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "(%d,%d,%d)", pos.x, pos.y, pos.z);
    }

    private String formatVec2(Vec3d vec) {
        if (vec == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "(%.3f, %.3f)", vec.x, vec.z);
    }

    private static final class RebuildCache {
        private final AxisAlignedBB scanBounds;
        private final List<AxisAlignedBB> blockingBoxes;
        private final long collisionCollectNanos;
        private final Map<Long, Boolean> baseStandableCache = new HashMap<>();
        private final Map<Long, Boolean> lineOfSightCache = new HashMap<>();
        private final Map<BoxKey, Boolean> boxClearCache = new HashMap<>();
        private final Map<SegmentKey, Boolean> segmentClearCache = new HashMap<>();
        private int baseStandableChecks;
        private int baseStandableHits;
        private int lineOfSightChecks;
        private int lineOfSightHits;
        private int boxClearChecks;
        private int boxClearHits;
        private int segmentChecks;
        private int segmentHits;

        private RebuildCache(AxisAlignedBB scanBounds, List<AxisAlignedBB> blockingBoxes, long collisionCollectNanos) {
            this.scanBounds = scanBounds;
            this.blockingBoxes = blockingBoxes == null ? Collections.emptyList() : blockingBoxes;
            this.collisionCollectNanos = collisionCollectNanos;
        }
    }

    private static final class BoxKey {
        private final long xBits;
        private final long yBits;
        private final long zBits;
        private final long halfWidthBits;
        private final long marginBits;

        private BoxKey(double centerX, double feetY, double centerZ, double halfWidth, double margin) {
            this.xBits = Double.doubleToLongBits(centerX);
            this.yBits = Double.doubleToLongBits(feetY);
            this.zBits = Double.doubleToLongBits(centerZ);
            this.halfWidthBits = Double.doubleToLongBits(halfWidth);
            this.marginBits = Double.doubleToLongBits(margin);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BoxKey)) {
                return false;
            }
            BoxKey other = (BoxKey) obj;
            return this.xBits == other.xBits
                    && this.yBits == other.yBits
                    && this.zBits == other.zBits
                    && this.halfWidthBits == other.halfWidthBits
                    && this.marginBits == other.marginBits;
        }

        @Override
        public int hashCode() {
            long hash = this.xBits;
            hash = 31L * hash + this.yBits;
            hash = 31L * hash + this.zBits;
            hash = 31L * hash + this.halfWidthBits;
            hash = 31L * hash + this.marginBits;
            return (int) (hash ^ (hash >>> 32));
        }
    }

    private static final class SegmentKey {
        private final long axBits;
        private final long ayBits;
        private final long azBits;
        private final long bxBits;
        private final long byBits;
        private final long bzBits;

        private SegmentKey(Vec3d first, Vec3d second) {
            this.axBits = Double.doubleToLongBits(first.x);
            this.ayBits = Double.doubleToLongBits(first.y);
            this.azBits = Double.doubleToLongBits(first.z);
            this.bxBits = Double.doubleToLongBits(second.x);
            this.byBits = Double.doubleToLongBits(second.y);
            this.bzBits = Double.doubleToLongBits(second.z);
        }

        private static SegmentKey of(Vec3d start, Vec3d end) {
            return compare(start, end) <= 0 ? new SegmentKey(start, end) : new SegmentKey(end, start);
        }

        private static int compare(Vec3d left, Vec3d right) {
            int cmp = Double.compare(left.x, right.x);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Double.compare(left.y, right.y);
            if (cmp != 0) {
                return cmp;
            }
            return Double.compare(left.z, right.z);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SegmentKey)) {
                return false;
            }
            SegmentKey other = (SegmentKey) obj;
            return this.axBits == other.axBits
                    && this.ayBits == other.ayBits
                    && this.azBits == other.azBits
                    && this.bxBits == other.bxBits
                    && this.byBits == other.byBits
                    && this.bzBits == other.bzBits;
        }

        @Override
        public int hashCode() {
            long hash = this.axBits;
            hash = 31L * hash + this.ayBits;
            hash = 31L * hash + this.azBits;
            hash = 31L * hash + this.bxBits;
            hash = 31L * hash + this.byBits;
            hash = 31L * hash + this.bzBits;
            return (int) (hash ^ (hash >>> 32));
        }
    }

    private enum State {
        IDLE,
        ACTIVE,
        STOPPING
    }

    private static final class GuidePointPlan {
        private final Vec3d point;
        private final boolean obstacleAdjusted;

        private GuidePointPlan(Vec3d point, boolean obstacleAdjusted) {
            this.point = point;
            this.obstacleAdjusted = obstacleAdjusted;
        }
    }

    private static final class OrbitNodePlan {
        private final BetterBlockPos position;
        private final List<Vec3d> guidePoints = new ArrayList<>();

        private OrbitNodePlan(BetterBlockPos position) {
            this.position = position;
        }

        private void appendGuidePoint(Vec3d point) {
            if (point != null) {
                this.guidePoints.add(point);
            }
        }

        private void appendGuidePoints(List<Vec3d> points) {
            if (points != null && !points.isEmpty()) {
                this.guidePoints.addAll(points);
            }
        }
    }

    private static final class OrbitArcPlan {
        private final BetterBlockPos src;
        private final BetterBlockPos dest;
        private final Vec3d[] routePoints;

        private OrbitArcPlan(BetterBlockPos src, BetterBlockPos dest, Vec3d[] routePoints) {
            this.src = src;
            this.dest = dest;
            this.routePoints = routePoints == null ? new Vec3d[0] : routePoints;
        }
    }

    private static final class RouteStats {
        private static final RouteStats EMPTY = new RouteStats(0.0D, 0.0D, -1, null, null);

        private final double length;
        private final double maxStep;
        private final int maxStepIndex;
        private final Vec3d maxStepStart;
        private final Vec3d maxStepEnd;

        private RouteStats(double length, double maxStep, int maxStepIndex, Vec3d maxStepStart, Vec3d maxStepEnd) {
            this.length = length;
            this.maxStep = maxStep;
            this.maxStepIndex = maxStepIndex;
            this.maxStepStart = maxStepStart;
            this.maxStepEnd = maxStepEnd;
        }
    }
}
