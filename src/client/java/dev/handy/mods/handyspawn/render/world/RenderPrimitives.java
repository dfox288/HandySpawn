package dev.handy.mods.handyspawn.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

/**
 * World-space rendering primitives shared by the per-feature renderers.
 *
 * <p>API target: MC 26.2-snapshot-5. All {@code RenderType} factories live on
 * {@code net.minecraft.client.renderer.rendertype.RenderTypes}. Custom geometry
 * uses {@code SubmitNodeCollector.submitCustomGeometry} — the old
 * {@code MultiBufferSource.getBuffer()} path was removed in snapshot-5.
 * Line drawing: {@link RenderTypes#lines()}; textured translucent quads:
 * {@link RenderTypes#text(Identifier)}; untextured translucent boxes:
 * {@link RenderTypes#debugFilledBox()}.
 *
 * <p>The vertex chain is
 * {@code consumer.addVertex(pose, x, y, z).setColor(r,g,b,a).setNormal(pose, nx,ny,nz)};
 * line vertices additionally require {@code .setLineWidth(4f)}.
 *
 * <p>These primitives assume the caller has already set up world-relative
 * rendering (i.e. the {@link RenderContext#poseStack()} is in the same state
 * the vanilla level renderer leaves it in partway through a frame). Each
 * method pushes and pops its own pose-stack frame.
 */
public final class RenderPrimitives {

    /**
     * Draw a camera-facing Unicode glyph at a world position.
     *
     * <p>Implementation note (MC 26.2): the vanilla LevelRenderer sets up its
     * own view-matrix state on the PoseStack and tears it back down before
     * returning. Our render hook lives at {@code COLLECT_SUBMITS} of
     * {@code LevelRenderer.renderLevel} and receives a fresh PoseStack with no
     * view matrix applied, so a naive {@code translate + billboard} on a raw
     * textured quad yields a screen-locked shape that drifts with the camera
     * and renders black (no sampler bound). We piggy-back on
     * {@link Font#drawInBatch} — the same idiom vanilla uses for entity name
     * tags — because the font renderer sets up its own matrix state internally
     * and works correctly against a camera-relative PoseStack.
     *
     * <p>{@code glyph} is the text content (typically a single Unicode
     * character). {@code size} is a multiplier on the vanilla name-tag scale
     * (0.025) — 1.0 matches vanilla entity names, larger reads as a landmark.
     * {@code rgb24} is the 0xRRGGBB tint; alpha is folded in to produce the
     * argb value Font expects. {@code throughWalls} selects
     * {@link Font.DisplayMode} between {@code NORMAL} and {@code SEE_THROUGH}.
     */

    public static void billboardGlyph(RenderContext ctx,
                                      Vec3 pos,
                                      String glyph,
                                      float size,
                                      int rgb24,
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
            // Match vanilla NameTagFeatureRenderer$Storage.add exactly:
            // billboard via CameraRenderState.orientation and scale(+s, -s, +s).
            ps.mulPose(ctx.cameraOrientation());
            float s = 0.025f * size;
            ps.scale(s, -s, s);

            FormattedCharSequence fcs = Component.literal(glyph).getVisualOrderText();
            int a = clamp8(Math.round(alpha * 255.0f));
            int argb = (a << 24) | (rgb24 & 0x00FFFFFF);
            Font.DisplayMode mode = throughWalls
                ? Font.DisplayMode.SEE_THROUGH
                : Font.DisplayMode.POLYGON_OFFSET;
            float textX = -net.minecraft.client.Minecraft.getInstance().font.width(fcs) / 2f;
            final int LIGHT_FULL = 0xF000F0;
            ctx.collector().order(2).submitText(
                ps,
                textX,
                0f,
                fcs,
                false,
                mode,
                LIGHT_FULL,
                argb,
                0,
                0
            );
        } finally {
            ps.popPose();
        }
    }

    /**
     * Draw four translucent vertical walls around a force-loaded chunk.
     *
     * <p>Each chunk gets a full-height column (minY -> maxY) whose four side
     * faces are rendered as low-alpha quads. Reads as a glass box framing the
     * chunk.
     *
     * <p>MC 26.2: uses {@code SubmitNodeCollector.submitCustomGeometry} for
     * both fill quads and outline lines — two separate submit passes, one per
     * render type.
     */
    public static void forceLoadSurface(RenderContext ctx, int chunkX, int chunkZ,
                                        int fillArgb, int edgeArgb,
                                        boolean drawNorth, boolean drawSouth,
                                        boolean drawWest,  boolean drawEast) {
        if (!drawNorth && !drawSouth && !drawWest && !drawEast) return;

        PoseStack ps = ctx.poseStack();
        Vec3 cam = ctx.camera().position();
        float fa = ((fillArgb >>> 24) & 0xff) / 255f;
        float fr = ((fillArgb >>> 16) & 0xff) / 255f;
        float fg = ((fillArgb >>>  8) & 0xff) / 255f;
        float fb = ( fillArgb         & 0xff) / 255f;
        float ea = ((edgeArgb >>> 24) & 0xff) / 255f;
        float er = ((edgeArgb >>> 16) & 0xff) / 255f;
        float eg = ((edgeArgb >>>  8) & 0xff) / 255f;
        float eb = ( edgeArgb         & 0xff) / 255f;

        int minY = ctx.level() != null ? ctx.level().getMinY() : -64;
        int maxY = ctx.level() != null ? ctx.level().getMaxY() : 320;

        float y0 = minY, y1 = maxY;

        ps.pushPose();
        ps.translate(
            (float) (chunkX * 16 - cam.x),
            (float) (-cam.y),
            (float) (chunkZ * 16 - cam.z));

        // Filled walls — one submit pass.
        RenderType fillType = RenderTypes.debugFilledBox();
        ctx.collector().submitCustomGeometry(ps, fillType, (p, vc) -> {
            if (drawNorth) wall(vc, p, 0f, y0, 0f,   16f, y0, 0f,   16f, y1, 0f,   0f, y1, 0f,   fr, fg, fb, fa,  0f, 0f, -1f);
            if (drawSouth) wall(vc, p, 0f, y0, 16f,  16f, y0, 16f,  16f, y1, 16f,  0f, y1, 16f,  fr, fg, fb, fa,  0f, 0f,  1f);
            if (drawWest)  wall(vc, p, 0f, y0, 0f,   0f, y0, 16f,  0f, y1, 16f,  0f, y1, 0f,   fr, fg, fb, fa, -1f, 0f, 0f);
            if (drawEast)  wall(vc, p, 16f, y0, 0f,  16f, y0, 16f,  16f, y1, 16f,  16f, y1, 0f, fr, fg, fb, fa,  1f, 0f, 0f);
        });
        // Per-wall outlines — second submit pass.
        RenderType lineType = RenderTypes.lines();
        ctx.collector().submitCustomGeometry(ps, lineType, (p, vc) -> {
            if (drawNorth) {
                line(vc, p, 0f,  y0, 0f,  16f, y0, 0f,  er, eg, eb, ea, 1f, 0f, 0f);
                line(vc, p, 0f,  y1, 0f,  16f, y1, 0f,  er, eg, eb, ea, 1f, 0f, 0f);
                line(vc, p, 0f,  y0, 0f,  0f,  y1, 0f,  er, eg, eb, ea, 0f, 1f, 0f);
                line(vc, p, 16f, y0, 0f,  16f, y1, 0f,  er, eg, eb, ea, 0f, 1f, 0f);
            }
            if (drawSouth) {
                line(vc, p, 0f,  y0, 16f,  16f, y0, 16f, er, eg, eb, ea, 1f, 0f, 0f);
                line(vc, p, 0f,  y1, 16f,  16f, y1, 16f, er, eg, eb, ea, 1f, 0f, 0f);
                line(vc, p, 0f,  y0, 16f,  0f,  y1, 16f, er, eg, eb, ea, 0f, 1f, 0f);
                line(vc, p, 16f, y0, 16f,  16f, y1, 16f, er, eg, eb, ea, 0f, 1f, 0f);
            }
            if (drawWest) {
                line(vc, p, 0f,  y0, 0f,   0f,  y0, 16f, er, eg, eb, ea, 0f, 0f, 1f);
                line(vc, p, 0f,  y1, 0f,   0f,  y1, 16f, er, eg, eb, ea, 0f, 0f, 1f);
                line(vc, p, 0f,  y0, 0f,   0f,  y1, 0f,  er, eg, eb, ea, 0f, 1f, 0f);
                line(vc, p, 0f,  y0, 16f,  0f,  y1, 16f, er, eg, eb, ea, 0f, 1f, 0f);
            }
            if (drawEast) {
                line(vc, p, 16f, y0, 0f,  16f, y0, 16f, er, eg, eb, ea, 0f, 0f, 1f);
                line(vc, p, 16f, y1, 0f,  16f, y1, 16f, er, eg, eb, ea, 0f, 0f, 1f);
                line(vc, p, 16f, y0, 0f,  16f, y1, 0f,  er, eg, eb, ea, 0f, 1f, 0f);
                line(vc, p, 16f, y0, 16f, 16f, y1, 16f, er, eg, eb, ea, 0f, 1f, 0f);
            }
        });

        ps.popPose();
    }

    /**
     * Emit one filled wall as two quads with opposite winding so it renders
     * from either side under backface culling.
     */
    private static void wall(VertexConsumer vc, PoseStack.Pose pose,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float r, float g, float b, float alpha,
                             float nx, float ny, float nz) {
        vc.addVertex(pose, ax, ay, az).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, bx, by, bz).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, cx, cy, cz).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, dx, dy, dz).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, dx, dy, dz).setColor(r, g, b, alpha).setNormal(pose, -nx, -ny, -nz);
        vc.addVertex(pose, cx, cy, cz).setColor(r, g, b, alpha).setNormal(pose, -nx, -ny, -nz);
        vc.addVertex(pose, bx, by, bz).setColor(r, g, b, alpha).setNormal(pose, -nx, -ny, -nz);
        vc.addVertex(pose, ax, ay, az).setColor(r, g, b, alpha).setNormal(pose, -nx, -ny, -nz);
    }

    /**
     * Draw a translucent tint quad over a bed mattress, with a gentle alpha
     * pulse so it reads as a highlight rather than a static ghost rectangle.
     *
     * <p>MC 26.2: uses {@code SubmitNodeCollector.submitCustomGeometry} instead
     * of the old {@code MultiBufferSource.getBuffer()} path.
     */
    private static final float PULSE_MIN = 0.55f;
    private static final float PULSE_MAX = 1.00f;
    private static final long  PULSE_PERIOD_MS = 1800L;

    public static void topFaceTint(RenderContext ctx, BlockPos pos, int argb) {
        PoseStack ps = ctx.poseStack();
        Vec3 cam = ctx.camera().position();

        double phase = (System.currentTimeMillis() % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        float pulse = PULSE_MIN
            + (PULSE_MAX - PULSE_MIN) * (0.5f + 0.5f * (float) Math.sin(phase * 2 * Math.PI));
        int a = clamp8(Math.round(((argb >>> 24) & 0xff) * pulse));
        int r = (argb >>> 16) & 0xff;
        int g = (argb >>>  8) & 0xff;
        int b =  argb         & 0xff;

        ps.pushPose();
        try {
            ps.translate(
                (float) (pos.getX() - cam.x),
                (float) (pos.getY() - cam.y + 9f / 16f + 0.003f),
                (float) (pos.getZ() - cam.z));

            RenderType type = RenderTypes.debugFilledBox();
            ctx.collector().submitCustomGeometry(ps, type, (p, vc) -> {
                vc.addVertex(p, 0f, 0f, 0f).setColor(r, g, b, a).setNormal(p, 0f, 1f, 0f);
                vc.addVertex(p, 0f, 0f, 1f).setColor(r, g, b, a).setNormal(p, 0f, 1f, 0f);
                vc.addVertex(p, 1f, 0f, 1f).setColor(r, g, b, a).setNormal(p, 0f, 1f, 0f);
                vc.addVertex(p, 1f, 0f, 0f).setColor(r, g, b, a).setNormal(p, 0f, 1f, 0f);
            });
        } finally {
            ps.popPose();
        }
    }

    private static void line(VertexConsumer vc, PoseStack.Pose pose,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a,
                             float nx, float ny, float nz) {
        vc.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(4f);
        vc.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(4f);
    }

    private static int clamp8(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private RenderPrimitives() {}
}
