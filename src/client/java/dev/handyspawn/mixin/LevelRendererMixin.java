package dev.handyspawn.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.handyspawn.render.world.RenderContext;
import dev.handyspawn.render.world.WorldRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the end of {@link LevelRenderer#renderLevel} once per frame and invokes
 * {@link WorldRenderer#tick(RenderContext)} so the five per-feature world renderers
 * (house icon, bed tint, ghost marker, world-spawn marker, forceload border) get a
 * chance to draw.
 *
 * <p><b>Injection point:</b> {@code @At("RETURN")} on {@code renderLevel}. This fires
 * after vanilla has finished entity rendering, translucent passes, and particles.
 * Z-order: our draws may therefore sit on top of translucent geometry — acceptable
 * for a first pass (all five features are deliberately opaque or HUD-like). A later
 * polish task can move to a more precise {@code INVOKE} target if we need to slot
 * between entity and translucent passes.
 *
 * <p><b>Signature shape (MC 26.1):</b>
 * {@code renderLevel(GraphicsResourceAllocator, DeltaTracker, boolean, CameraRenderState,
 *                    Matrix4fc, GpuBufferSlice, Vector4f, boolean, ChunkSectionsToRender)}.
 * We only consume {@code DeltaTracker} (for the partial tick) and pull the live
 * {@link Camera} from {@link Minecraft#gameRenderer} — the {@code CameraRenderState}
 * is a snapshot record, but {@code Camera.position()} / {@code Camera.rotation()}
 * (which {@code RenderPrimitives} needs) live on the live instance.
 *
 * <p><b>PoseStack:</b> we construct a fresh one rather than trying to capture the
 * vanilla stack via {@code @Local}. The render primitives translate everything by
 * {@code (pos - cameraPos)} and billboard via {@code camera.rotation()}, so they
 * are camera-relative and do not rely on inheriting world-space matrix state.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void handyspawn$renderAfterLevel(
            GraphicsResourceAllocator allocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            CameraRenderState cameraRenderState,
            Matrix4fc frustumMatrix,
            GpuBufferSlice fogBuffer,
            Vector4f fogColor,
            boolean flag,
            ChunkSectionsToRender chunkSectionsToRender,
            CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) {
            return;
        }
        PoseStack pose = new PoseStack();
        MultiBufferSource buffers = mc.renderBuffers().bufferSource();
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
        RenderContext ctx = new RenderContext(pose, camera, buffers, level, tickDelta);
        WorldRenderer.tick(ctx);
    }
}
