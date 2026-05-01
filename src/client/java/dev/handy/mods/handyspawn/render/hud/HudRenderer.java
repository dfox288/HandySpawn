package dev.handy.mods.handyspawn.render.hud;

import dev.handy.mods.handyspawn.HandySpawnConstants;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/**
 * Single entry point that plugs HandySpawn's HUD overlay into the vanilla HUD pipeline.
 *
 * <p>Fabric API 0.146.1 (MC 26.1.2) exposes {@code HudElementRegistry} — the old
 * {@code HudRenderCallback} / {@code HudLayerRegistrationCallback} do not exist in
 * this version. We attach a single element right after {@link VanillaHudElements#HOTBAR}
 * so our draws land above the hotbar/bars layer but below boss bars, chat, debug,
 * title, and the tab list. Each per-feature class ({@link PhantomHud},
 * {@link ThunderHud}, {@link CrossDimHud}, {@link ForceloadHud}) gets the same
 * {@code GuiGraphicsExtractor} and decides for itself whether it has anything to
 * draw this frame — Tasks 18–21 fill those in.
 */
public final class HudRenderer {

    private static final Identifier OVERLAY_ID =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "overlay");

    public static void register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.HOTBAR,
            OVERLAY_ID,
            (ctx, deltaTracker) -> {
                // Top-right "rail": every element is right-aligned to the same
                // edge and stacks vertically. Each renderRight(..) returns the
                // next Y to use, so elements that don't render this frame pass
                // the cursor through unchanged and leave no gap.
                final int rightEdge = HudPrimitives.screenWidth() - 4;
                int y = 4;
                y = CrossDimHud.renderRight(ctx, rightEdge, y);
                y = SleepReadyHud.renderRight(ctx, rightEdge, y);
                y = PhantomHud.renderRight(ctx, rightEdge, y);
                y = ThunderHud.renderRight(ctx, rightEdge, y);
                y = ForceloadHud.renderRight(ctx, rightEdge, y);
            });
    }

    private HudRenderer() {}
}
