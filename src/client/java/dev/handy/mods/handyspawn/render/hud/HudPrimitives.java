package dev.handy.mods.handyspawn.render.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Tiny convenience layer over {@link GuiGraphicsExtractor} so the per-feature HUD
 * classes (PhantomHud, ThunderHud, CrossDimHud, ForceloadHud) don't each have to
 * re-derive font / window / texture arguments.
 *
 * <p><b>API note (MC 26.1 / Fabric API 0.146.1):</b> the classic {@code GuiGraphics}
 * /{@code HudRenderCallback} pair that older tutorials reference no longer ships in
 * this build. Drawing during HUD rendering flows through {@code GuiGraphicsExtractor},
 * which a {@link net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement} receives
 * from the game. All helpers here take that type; upstream consumers get it from the
 * {@code HudElement.extractRenderState} callback registered in {@link HudRenderer}.
 *
 * <p>The {@code blit} overload we use — {@code blit(Identifier, int x, int y, int w,
 * int h, float u0, float u1, float v0, float v1)} — is the only non-{@code
 * RenderPipeline}-taking form available on {@code GuiGraphicsExtractor} in this
 * version, so it's the simplest thing that compiles for a full-texture icon. GUI
 * atlas sprites go through {@code blitSprite} on the extractor directly if a feature
 * needs them later.
 */
public final class HudPrimitives {

    /**
     * Draws a whole texture ({@code size x size}) at {@code (x, y)} using UV (0,0)-(1,1).
     * Texture must be the full sprite — this helper does not slice an atlas.
     *
     * <p><b>MC 26.1.2 gotcha:</b> {@link GuiGraphicsExtractor#blit(Identifier, int, int,
     * int, int, float, float, float, float)} takes the four ints as corners
     * {@code (x1, y1, x2, y2)}, not {@code (x, y, w, h)} like older versions.
     * Passing {@code (x, y, size, size)} draws from {@code (x, y)} to
     * {@code (size, size)} — a tiny/negative rectangle that disappears off-screen.
     * We compute the second corner here so callers keep the familiar
     * {@code (x, y, size)} signature.
     */
    public static void drawIconAt(GuiGraphicsExtractor ctx, Identifier sprite, int x, int y, int size) {
        ctx.blit(sprite, x, y, x + size, y + size, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    /**
     * Draws a {@link Component} with a drop shadow at {@code (x, y)} in the given ARGB colour.
     */
    public static void drawTextAt(GuiGraphicsExtractor ctx, Component text, int x, int y, int argb) {
        Font font = Minecraft.getInstance().font;
        ctx.text(font, text, x, y, argb, true);
    }

    /**
     * Draws a {@link Component} right-aligned so the last character sits at
     * {@code rightEdgeX}. Used by the top-right HUD rail so every row anchors
     * to the same vertical edge regardless of content width.
     */
    public static void drawTextRightAligned(GuiGraphicsExtractor ctx, Component text, int rightEdgeX, int y, int argb) {
        Font font = Minecraft.getInstance().font;
        int w = font.width(text);
        ctx.text(font, text, rightEdgeX - w, y, argb, true);
    }

    /** Pixel height of one text row (font line height + 1px leading). */
    public static int textRowHeight() {
        return Minecraft.getInstance().font.lineHeight + 1;
    }

    /** Scaled GUI width (what the vanilla HUD sees). */
    public static int screenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    /** Scaled GUI height (what the vanilla HUD sees). */
    public static int screenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    private HudPrimitives() {}
}
