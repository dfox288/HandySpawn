package dev.handyspawn.render.hud;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.HandySpawnConstants;
import dev.handyspawn.state.CompassTargetRecord;
import dev.handyspawn.state.PersonalSpawnState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.network.chat.Component;
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
                // DEBUG (kept commented for future diagnosis): server→client
                // state banner used while world-space text rendering was silent
                // during v0.2.0 development.
                // renderDebugStatus(ctx);
            });
    }

    // DEBUG (kept commented for future diagnosis):
    // private static void renderDebugStatus(net.minecraft.client.gui.GuiGraphicsExtractor ctx) {
    //     StringBuilder sb = new StringBuilder("HS ");
    //     sb.append(HandySpawn.session().isServerSynced() ? "SYNCED " : "LOCAL ");
    //
    //     PersonalSpawnState ps = HandySpawn.personal();
    //     if (ps != null) {
    //         ps.current().ifPresent(r ->
    //             sb.append("| bed@").append(r.pos().getX()).append(",").append(r.pos().getY()).append(",").append(r.pos().getZ()).append(" "));
    //         ps.destroyedAt().ifPresent(r ->
    //             sb.append("| GONE@").append(r.pos().getX()).append(",").append(r.pos().getY()).append(",").append(r.pos().getZ()).append(" "));
    //     }
    //     CompassTargetRecord compass = HandySpawn.compassTarget() != null
    //         ? HandySpawn.compassTarget().state().current().orElse(null) : null;
    //     if (compass != null) {
    //         sb.append("| world@").append(compass.pos().getX()).append(",").append(compass.pos().getY()).append(",").append(compass.pos().getZ());
    //     }
    //     HudPrimitives.drawTextAt(ctx, Component.literal(sb.toString()), 4, 4, 0xFFFFFF00);
    // }

    private HudRenderer() {}
}
