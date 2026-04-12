package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.entity.EntityPlayerSP;

final class MovementPersistentAttributeHandler {

    private MovementPersistentAttributeHandler() {
    }

    static void apply(EntityPlayerSP player) {
        player.stepHeight = MovementFeatureManager.isEnabled("auto_step")
                ? MovementFeatureManager.getConfiguredValue("auto_step", MovementFeatureManager.DEFAULT_STEP_HEIGHT)
                : MovementFeatureManager.DEFAULT_STEP_HEIGHT;
        player.entityCollisionReduction = MovementFeatureManager.DEFAULT_COLLISION_REDUCTION;
    }

    static void reset(EntityPlayerSP player) {
        if (player == null) {
            return;
        }
        player.stepHeight = MovementFeatureManager.DEFAULT_STEP_HEIGHT;
        player.entityCollisionReduction = MovementFeatureManager.DEFAULT_COLLISION_REDUCTION;
    }
}
