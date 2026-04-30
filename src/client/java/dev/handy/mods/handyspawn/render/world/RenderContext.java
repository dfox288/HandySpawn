package dev.handy.mods.handyspawn.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Quaternionf;

/**
 * Per-frame bundle passed to each per-feature renderer and to {@link RenderPrimitives}.
 *
 * <p>Populated once per frame from a Fabric {@code LevelRenderContext} inside
 * {@code WorldRenderer.register()}. All custom geometry is queued via the
 * {@link SubmitNodeCollector} (see sibling HandyBookshelf's
 * {@code ChiseledBookshelfRenderer} for the proven pattern).
 */
public record RenderContext(PoseStack poseStack,
                            Camera camera,
                            Quaternionf cameraOrientation,
                            SubmitNodeCollector collector,
                            ClientLevel level,
                            float tickDelta) {
}
