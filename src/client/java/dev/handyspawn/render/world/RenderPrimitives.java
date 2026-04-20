package dev.handyspawn.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
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
     * Draw a camera-facing Unicode glyph at a world position.
     *
     * <p>Implementation note (MC 26.1.2): the vanilla LevelRenderer sets up its
     * own view-matrix state on the PoseStack and tears it back down before
     * returning. Our render hook lives at {@code @At("RETURN")} of
     * {@code LevelRenderer.renderLevel} and receives a fresh PoseStack with no
     * view matrix applied, so a naive {@code translate + billboard} on a raw
     * textured quad yields a screen-locked shape that drifts with the camera
     * and renders black (no sampler bound). We piggy-back on
     * {@link Font#drawInBatch} — the same idiom vanilla uses for entity name
     * tags — because the font renderer sets up its own matrix state internally
     * and works correctly against a camera-relative PoseStack. A sprite-based
     * path is tracked as a polish task.
     *
     * <p>{@code glyph} is the text content (typically a single Unicode
     * character). {@code size} is a multiplier on the vanilla name-tag scale
     * (0.025) — 1.0 matches vanilla entity names, larger reads as a landmark.
     * {@code rgb24} is the 0xRRGGBB tint; alpha is folded in to produce the
     * argb value Font expects. {@code throughWalls} selects
     * {@link Font.DisplayMode} between {@code NORMAL} and {@code SEE_THROUGH}.
     */
    // DEBUG (kept commented for future diagnosis): per-glyph call-rate log used
    // while hunting the world-space text silent-render bug in v0.2.0.
    // private static int debugCallCounter = 0;
    // private static final int DEBUG_LOG_EVERY_N = 120;

    public static void billboardGlyph(RenderContext ctx,
                                      Vec3 pos,
                                      String glyph,
                                      float size,
                                      int rgb24,
                                      float alpha,
                                      boolean throughWalls) {
        PoseStack ps = ctx.poseStack();
        Vec3 cam = ctx.camera().position();

        // if ((debugCallCounter++ % DEBUG_LOG_EVERY_N) == 0) {
        //     dev.handyspawn.HandySpawnConstants.LOG.info(
        //         "[billboard] glyph='{}' worldPos={} camPos={} mode={}",
        //         glyph, pos, cam, throughWalls ? "SEE_THROUGH" : "NORMAL");
        // }

        // Rendering idiom borrowed from sibling mod HandyBookshelf's
        // ChiseledBookshelfRenderer.submit() (name-tag branch): translate to
        // camera-relative world offset, apply camera rotation for billboard,
        // scale to vanilla name-tag sizing, and queue via SubmitNodeCollector.
        // In MC 26.1 direct Font.drawInBatch against a level-render BufferSource
        // does not land on screen at any renderLevel injection point we tried —
        // the collector-based path is the supported one.
        ps.pushPose();
        try {
            ps.translate(
                (float) (pos.x - cam.x),
                (float) (pos.y - cam.y),
                (float) (pos.z - cam.z));
            // Match vanilla NameTagFeatureRenderer$Storage.add exactly:
            // billboard via CameraRenderState.orientation and scale(+s, -s, +s).
            // Our previous scale of (-s, -s, s) X-mirrored the text quad, which
            // made its triangles face away from the camera — the font render
            // pipeline then silently culled them (no crash, no pixels).
            ps.mulPose(ctx.cameraOrientation());
            float s = 0.025f * size;
            ps.scale(s, -s, s);

            FormattedCharSequence fcs = Component.literal(glyph).getVisualOrderText();
            // submitText expects the text origin and takes the width-centred x
            // offset as the second arg. No Font instance needed — the collector
            // owns its Font reference.
            int a = clamp8(Math.round(alpha * 255.0f));
            int argb = (a << 24) | (rgb24 & 0x00FFFFFF);
            // POLYGON_OFFSET matches vanilla text_display entities — renders on
            // top of translucent geometry without depth-test disable.
            // SEE_THROUGH for through-walls ghost markers.
            Font.DisplayMode mode = throughWalls
                ? Font.DisplayMode.SEE_THROUGH
                : Font.DisplayMode.POLYGON_OFFSET;
            float textX = -net.minecraft.client.Minecraft.getInstance().font.width(fcs) / 2f;
            final int LIGHT_FULL = 0xF000F0;
            // order(2) mirrors HandyBookshelf's name-tag draw — a stable
            // "after geometry, before HUD" layer in the collector's order list.
            ctx.collector().order(2).submitText(
                ps,
                textX,
                0f,
                fcs,
                false,        // shadow
                mode,
                LIGHT_FULL,
                argb,
                0,            // background colour (0 = no bg)
                0             // outline colour (0 = no outline)
            );
        } finally {
            ps.popPose();
        }
    }

    /**
     * Draw four translucent vertical walls around a force-loaded chunk.
     *
     * <p>Each chunk gets a full-height column (minY → maxY) whose four side
     * faces are rendered as low-alpha quads. Reads as a glass box framing the
     * chunk — the same footprint as the old wireframe beams, but with filled
     * faces so the loaded area is visible through terrain at a distance.
     *
     * <p>Each wall is emitted twice (both windings) so the player sees it from
     * outside AND inside the loaded region. Backface-culling is on for
     * {@link RenderTypes#debugFilledBox()}, so a single-winding quad would
     * vanish from one side.
     *
     * <p>Using {@link RenderTypes#debugFilledBox()} — the proven untextured
     * translucent layer. {@code RenderTypes.translucent()} is the block-terrain
     * layer and silently drops vertices missing UV/lightmap.
     */
    public static void forceLoadSurface(RenderContext ctx, int chunkX, int chunkZ,
                                        int fillArgb, int edgeArgb,
                                        boolean drawNorth, boolean drawSouth,
                                        boolean drawWest,  boolean drawEast) {
        // Any wall shared with an adjacent force-loaded chunk is suppressed by
        // the caller (drawN/S/W/E = false). That serves two purposes: (1)
        // contiguous regions render as one continuous hollow column rather
        // than a stack of boxes, and (2) the double-opacity artifact at shared
        // seams — two back-to-back quads with alpha-blending — is eliminated.
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

        ps.pushPose();
        try {
            ps.translate(
                (float) (chunkX * 16 - cam.x),
                (float) (-cam.y),
                (float) (chunkZ * 16 - cam.z));

            PoseStack.Pose pose = ps.last();

            float y0 = minY, y1 = maxY;

            // Buffer-source gotcha (MC 26.1.2): MultiBufferSource.getBuffer()
            // finalises the previously-open BufferBuilder when the caller asks
            // for a *different* render type. Holding two VertexConsumers
            // simultaneously and writing to them interleaved crashes with
            // "BufferBuilder: Not building!". So we do all fill writes first
            // in one block, then all line writes in a second block, and never
            // interleave them.
            // Minecraft axis convention: -Z = north, +Z = south, -X = west, +X = east.
            {
                VertexConsumer fillVc = ctx.buffers().getBuffer(RenderTypes.debugFilledBox());
                if (drawNorth) wall(fillVc, pose, 0f, y0, 0f,   16f, y0, 0f,   16f, y1, 0f,   0f, y1, 0f,   fr, fg, fb, fa, 0f, 0f, -1f);
                if (drawSouth) wall(fillVc, pose, 0f, y0, 16f,  0f, y1, 16f,   16f, y1, 16f,  16f, y0, 16f, fr, fg, fb, fa, 0f, 0f,  1f);
                if (drawWest)  wall(fillVc, pose, 0f, y0, 0f,   0f, y1, 0f,    0f, y1, 16f,   0f, y0, 16f,  fr, fg, fb, fa, -1f, 0f, 0f);
                if (drawEast)  wall(fillVc, pose, 16f, y0, 0f,  16f, y0, 16f,  16f, y1, 16f,  16f, y1, 0f,  fr, fg, fb, fa,  1f, 0f, 0f);
            }
            // Each drawn wall gets a perimeter outline (4 edges) so the chunk
            // boundary stays readable even when the translucent fill washes out
            // against distant terrain. Shared vertical corner edges double-draw
            // at region corners — harmless, reads as a brighter post.
            {
                VertexConsumer lineVc = ctx.buffers().getBuffer(RenderTypes.lines());
                if (drawNorth) {
                    line(lineVc, pose, 0f,  y0, 0f,  16f, y0, 0f,  er, eg, eb, ea, 1f, 0f, 0f);
                    line(lineVc, pose, 0f,  y1, 0f,  16f, y1, 0f,  er, eg, eb, ea, 1f, 0f, 0f);
                    line(lineVc, pose, 0f,  y0, 0f,  0f,  y1, 0f,  er, eg, eb, ea, 0f, 1f, 0f);
                    line(lineVc, pose, 16f, y0, 0f,  16f, y1, 0f,  er, eg, eb, ea, 0f, 1f, 0f);
                }
                if (drawSouth) {
                    line(lineVc, pose, 0f,  y0, 16f,  16f, y0, 16f,  er, eg, eb, ea, 1f, 0f, 0f);
                    line(lineVc, pose, 0f,  y1, 16f,  16f, y1, 16f,  er, eg, eb, ea, 1f, 0f, 0f);
                    line(lineVc, pose, 0f,  y0, 16f,  0f,  y1, 16f,  er, eg, eb, ea, 0f, 1f, 0f);
                    line(lineVc, pose, 16f, y0, 16f,  16f, y1, 16f,  er, eg, eb, ea, 0f, 1f, 0f);
                }
                if (drawWest) {
                    line(lineVc, pose, 0f, y0, 0f,   0f, y0, 16f,   er, eg, eb, ea, 0f, 0f, 1f);
                    line(lineVc, pose, 0f, y1, 0f,   0f, y1, 16f,   er, eg, eb, ea, 0f, 0f, 1f);
                    line(lineVc, pose, 0f, y0, 0f,   0f, y1, 0f,    er, eg, eb, ea, 0f, 1f, 0f);
                    line(lineVc, pose, 0f, y0, 16f,  0f, y1, 16f,   er, eg, eb, ea, 0f, 1f, 0f);
                }
                if (drawEast) {
                    line(lineVc, pose, 16f, y0, 0f,  16f, y0, 16f,  er, eg, eb, ea, 0f, 0f, 1f);
                    line(lineVc, pose, 16f, y1, 0f,  16f, y1, 16f,  er, eg, eb, ea, 0f, 0f, 1f);
                    line(lineVc, pose, 16f, y0, 0f,  16f, y1, 0f,   er, eg, eb, ea, 0f, 1f, 0f);
                    line(lineVc, pose, 16f, y0, 16f, 16f, y1, 16f,  er, eg, eb, ea, 0f, 1f, 0f);
                }
            }
        } finally {
            ps.popPose();
        }
    }

    /**
     * Emit one filled wall as two quads with opposite winding so it renders
     * from either side under backface culling. {@code debugFilledBox} is the
     * target render layer; the reversed-winding quad uses a negated normal to
     * match its lighting expectation.
     */
    private static void wall(VertexConsumer vc, PoseStack.Pose pose,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float r, float g, float b, float alpha,
                             float nx, float ny, float nz) {
        // Outward-facing quad.
        vc.addVertex(pose, ax, ay, az).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, bx, by, bz).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, cx, cy, cz).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, dx, dy, dz).setColor(r, g, b, alpha).setNormal(pose, nx, ny, nz);
        // Reversed winding (for view from behind) with negated normal.
        vc.addVertex(pose, dx, dy, dz).setColor(r, g, b, alpha).setNormal(pose, -nx, -ny, -nz);
        vc.addVertex(pose, cx, cy, cz).setColor(r, g, b, alpha).setNormal(pose, -nx, -ny, -nz);
        vc.addVertex(pose, bx, by, bz).setColor(r, g, b, alpha).setNormal(pose, -nx, -ny, -nz);
        vc.addVertex(pose, ax, ay, az).setColor(r, g, b, alpha).setNormal(pose, -nx, -ny, -nz);
    }

    /**
     * Draw a translucent tint quad over a bed mattress, with a gentle alpha
     * pulse so it reads as a highlight rather than a static ghost rectangle.
     *
     * <p>Height: the vanilla bed mattress top sits at {@code y = 9/16}; we
     * render at {@code 9/16 + 0.003} so the quad hugs the mattress surface
     * instead of floating a full block above it (the previous {@code +1.001}
     * looked like a detached sign). The tiny offset avoids z-fighting with
     * the mattress texture without needing a dedicated polygon-offset layer.
     *
     * <p>Pulse: alpha multiplied by a sin-driven factor that oscillates
     * between {@link #PULSE_MIN} and {@link #PULSE_MAX} on a {@link #PULSE_PERIOD_MS}
     * cycle. Time source is {@link System#currentTimeMillis()} — frame-rate
     * independent, no state kept.
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
        // MC 26.1 lines vertex format requires setLineWidth; omitting it
        // crashes at end-of-vertex with "Missing elements in vertex: LineWidth".
        vc.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(4f);
        vc.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(4f);
    }

    private static int clamp8(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private RenderPrimitives() {}
}
