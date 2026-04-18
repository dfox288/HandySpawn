package dev.handyspawn.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Per-frame bundle passed to each per-feature renderer and to {@link RenderPrimitives}.
 *
 * <p>In earlier Fabric API versions this role was served by
 * {@code net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext}, but that
 * class was removed in {@code fabric-rendering-v1 23.0.5+08c26e364c} (MC 26.1.2).
 * We supply our own minimal record so Tasks 12–16 have a stable signature to call
 * against; the runtime wiring that actually produces a {@code RenderContext} each
 * frame (via a {@code LevelRenderer.renderLevel} mixin or equivalent) is the job
 * of a follow-up task.
 */
public record RenderContext(PoseStack poseStack,
                            Camera camera,
                            MultiBufferSource buffers,
                            ClientLevel level,
                            float tickDelta) {
}
