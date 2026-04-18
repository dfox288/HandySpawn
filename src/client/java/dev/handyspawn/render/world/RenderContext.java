package dev.handyspawn.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Per-frame bundle passed to each per-feature renderer and to {@link RenderPrimitives}.
 *
 * <p>Populated once per frame from a Fabric {@code LevelRenderContext} inside
 * {@code WorldRenderer.register()}. In MC 26.1 the correct path for level-overlay
 * rendering is to queue draws via the {@link SubmitNodeCollector} (see sibling
 * HandyBookshelf's {@code ChiseledBookshelfRenderer} for the proven pattern),
 * not to write vertices directly to the buffer source. The buffer source is
 * retained for the edge cases where direct vertex submission is unavoidable.
 */
public record RenderContext(PoseStack poseStack,
                            Camera camera,
                            MultiBufferSource buffers,
                            SubmitNodeCollector collector,
                            ClientLevel level,
                            float tickDelta) {
}
