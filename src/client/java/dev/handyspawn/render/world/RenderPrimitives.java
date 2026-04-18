package dev.handyspawn.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Three world-space rendering primitives shared by the per-feature renderers.
 *
 * <p>API target: MC 26.1.2. All {@code RenderType} factories live on the new
 * {@code net.minecraft.client.renderer.rendertype.RenderTypes} utility class —
 * Mojang split the old monolithic {@code RenderType} into data + factory classes
 * sometime during the 26.1 cycle. Line drawing: {@link RenderTypes#lines()};
 * textured translucent quads: {@link RenderTypes#text(Identifier)}; untextured
 * translucent boxes: {@link RenderTypes#debugFilledBox()}.
 *
 * <p>The vertex chain for this version is
 * {@code consumer.addVertex(pose, x, y, z).setColor(r,g,b,a).setUv(u,v)};
 * line vertices additionally require {@code .setNormal(pose, nx, ny, nz)}.
 *
 * <p>These primitives assume the caller has already set up world-relative
 * rendering (i.e. the {@link RenderContext#poseStack()} is in the same state
 * the vanilla level renderer leaves it in partway through a frame). Each
 * method pushes and pops its own pose-stack frame.
 */
public final class RenderPrimitives {

    /**
     * Draw a camera-facing textured quad at a world position.
     *
     * <p>First-pass quality: the quad billboards correctly and samples the sprite
     * texture, but {@code throughWalls} is currently ignored — we always use
     * depth-tested {@link RenderTypes#text(Identifier)}. Proper through-walls
     * support needs a custom {@code RenderType} variant with depth testing
     * disabled, which is deferred to a polish task.
     */
    public static void billboardIcon(RenderContext ctx,
                                     Vec3 pos,
                                     Identifier sprite,
                                     float size,
                                     float alpha,
                                     boolean throughWalls) {
        PoseStack ps = ctx.poseStack();
        Vec3 cam = ctx.camera().position();
        ps.pushPose();
        try {
            ps.translate(
                (float) (pos.x - cam.x),
                (float) (pos.y - cam.y),
                (float) (pos.z - cam.z));
            // Billboard: rotate the quad to face the camera.
            ps.mulPose(ctx.camera().rotation());

            // TODO(polish): real through-walls support needs a depth-test-off variant.
            RenderType layer = RenderTypes.text(sprite);
            VertexConsumer vc = ctx.buffers().getBuffer(layer);

            int a = clamp8(Math.round(alpha * 255.0f));
            int r = 255, g = 255, b = 255;
            float h = size * 0.5f;
            PoseStack.Pose pose = ps.last();

            // Quad on XY plane at z=0, corners:
            //   (-h,-h) -- ( h,-h)
            //   (-h, h) -- ( h, h)
            // Wound CCW when viewed from +Z (post-billboard: toward the camera).
            // Full-bright lightmap (15/15 block/sky) so the icon renders at consistent
            // brightness regardless of ambient lighting. NO_OVERLAY disables entity
            // damage/hurt tint.
            final int LIGHT_FULL = 0xF000F0;
            final int NO_OVERLAY = 0x00F000F0;
            vc.addVertex(pose, -h, -h, 0f).setColor(r, g, b, a).setUv(0f, 1f).setOverlay(NO_OVERLAY).setLight(LIGHT_FULL);
            vc.addVertex(pose,  h, -h, 0f).setColor(r, g, b, a).setUv(1f, 1f).setOverlay(NO_OVERLAY).setLight(LIGHT_FULL);
            vc.addVertex(pose,  h,  h, 0f).setColor(r, g, b, a).setUv(1f, 0f).setOverlay(NO_OVERLAY).setLight(LIGHT_FULL);
            vc.addVertex(pose, -h,  h, 0f).setColor(r, g, b, a).setUv(0f, 0f).setOverlay(NO_OVERLAY).setLight(LIGHT_FULL);
        } finally {
            ps.popPose();
        }
    }

    /**
     * Draw a chunk-aligned wireframe box spanning the world's full build height.
     *
     * <p>First-pass quality: emits all 12 edges (4 vertical + 4 top + 4 bottom).
     */
    public static void chunkWireframe(RenderContext ctx, int chunkX, int chunkZ, int argb) {
        PoseStack ps = ctx.poseStack();
        Vec3 cam = ctx.camera().position();
        float a = ((argb >>> 24) & 0xff) / 255f;
        float r = ((argb >>> 16) & 0xff) / 255f;
        float g = ((argb >>>  8) & 0xff) / 255f;
        float b = ( argb         & 0xff) / 255f;

        int minY = ctx.level() != null ? ctx.level().getMinY() : -64;
        int maxY = ctx.level() != null ? ctx.level().getMaxY() : 320;

        ps.pushPose();
        try {
            ps.translate(
                (float) (chunkX * 16 - cam.x),
                (float) (-cam.y),
                (float) (chunkZ * 16 - cam.z));
            VertexConsumer vc = ctx.buffers().getBuffer(RenderTypes.lines());
            PoseStack.Pose pose = ps.last();

            float x0 = 0f, x1 = 16f;
            float z0 = 0f, z1 = 16f;
            float y0 = minY, y1 = maxY;

            // 4 vertical edges (normal = up).
            line(vc, pose, x0, y0, z0, x0, y1, z0, r, g, b, a, 0f, 1f, 0f);
            line(vc, pose, x1, y0, z0, x1, y1, z0, r, g, b, a, 0f, 1f, 0f);
            line(vc, pose, x1, y0, z1, x1, y1, z1, r, g, b, a, 0f, 1f, 0f);
            line(vc, pose, x0, y0, z1, x0, y1, z1, r, g, b, a, 0f, 1f, 0f);

            // 4 bottom edges (normals on the XZ plane).
            line(vc, pose, x0, y0, z0, x1, y0, z0, r, g, b, a, 1f, 0f, 0f);
            line(vc, pose, x1, y0, z0, x1, y0, z1, r, g, b, a, 0f, 0f, 1f);
            line(vc, pose, x1, y0, z1, x0, y0, z1, r, g, b, a, 1f, 0f, 0f);
            line(vc, pose, x0, y0, z1, x0, y0, z0, r, g, b, a, 0f, 0f, 1f);

            // 4 top edges.
            line(vc, pose, x0, y1, z0, x1, y1, z0, r, g, b, a, 1f, 0f, 0f);
            line(vc, pose, x1, y1, z0, x1, y1, z1, r, g, b, a, 0f, 0f, 1f);
            line(vc, pose, x1, y1, z1, x0, y1, z1, r, g, b, a, 1f, 0f, 0f);
            line(vc, pose, x0, y1, z1, x0, y1, z0, r, g, b, a, 0f, 0f, 1f);
        } finally {
            ps.popPose();
        }
    }

    /**
     * Draw a translucent tint quad on the top face of a block.
     *
     * <p>First-pass quality: offsets the quad by +0.001 y to try to avoid
     * z-fighting with the block surface. This may still flicker slightly on
     * some GPUs / viewing angles — a polish task can add polygon-offset or
     * a dedicated render layer.
     */
    public static void topFaceTint(RenderContext ctx, BlockPos pos, int argb) {
        PoseStack ps = ctx.poseStack();
        Vec3 cam = ctx.camera().position();
        int a = (argb >>> 24) & 0xff;
        int r = (argb >>> 16) & 0xff;
        int g = (argb >>>  8) & 0xff;
        int b =  argb         & 0xff;

        ps.pushPose();
        try {
            ps.translate(
                (float) (pos.getX() - cam.x),
                (float) (pos.getY() - cam.y + 1.001),
                (float) (pos.getZ() - cam.z));

            VertexConsumer vc = ctx.buffers().getBuffer(RenderTypes.debugFilledBox());
            PoseStack.Pose pose = ps.last();

            // 1x1 quad on XZ plane at y=0, CCW from above.
            // Normal points up (+Y) — debugFilledBox needs a normal vector.
            vc.addVertex(pose, 0f, 0f, 0f).setColor(r, g, b, a).setNormal(pose, 0f, 1f, 0f);
            vc.addVertex(pose, 0f, 0f, 1f).setColor(r, g, b, a).setNormal(pose, 0f, 1f, 0f);
            vc.addVertex(pose, 1f, 0f, 1f).setColor(r, g, b, a).setNormal(pose, 0f, 1f, 0f);
            vc.addVertex(pose, 1f, 0f, 0f).setColor(r, g, b, a).setNormal(pose, 0f, 1f, 0f);
        } finally {
            ps.popPose();
        }
    }

    private static void line(VertexConsumer vc, PoseStack.Pose pose,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a,
                             float nx, float ny, float nz) {
        vc.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }

    private static int clamp8(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private RenderPrimitives() {}
}
