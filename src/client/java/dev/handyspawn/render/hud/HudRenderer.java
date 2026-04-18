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
                PhantomHud.render(ctx);
                ThunderHud.render(ctx);
                CrossDimHud.render(ctx);
                ForceloadHud.render(ctx);
                // DEBUG: big status banner so we can verify server→client state
                // even if world-space rendering stays silent. Remove once world
                // rendering is proven visible.
                renderDebugStatus(ctx);
            });
    }

    private static void renderDebugStatus(net.minecraft.client.gui.GuiGraphicsExtractor ctx) {
        StringBuilder sb = new StringBuilder("HS ");
        sb.append(HandySpawn.session().isServerSynced() ? "SYNCED " : "LOCAL ");

        PersonalSpawnState ps = HandySpawn.personal();
        if (ps != null) {
            ps.current().ifPresent(r ->
                sb.append("| bed@").append(r.pos().getX()).append(",").append(r.pos().getY()).append(",").append(r.pos().getZ()).append(" "));
            ps.destroyedAt().ifPresent(r ->
                sb.append("| GONE@").append(r.pos().getX()).append(",").append(r.pos().getY()).append(",").append(r.pos().getZ()).append(" "));
        }
        CompassTargetRecord compass = HandySpawn.compassTarget() != null
            ? HandySpawn.compassTarget().state().current().orElse(null) : null;
        if (compass != null) {
            sb.append("| world@").append(compass.pos().getX()).append(",").append(compass.pos().getY()).append(",").append(compass.pos().getZ());
        }
        HudPrimitives.drawTextAt(ctx, Component.literal(sb.toString()), 4, 4, 0xFFFFFF00);
    }

    private HudRenderer() {}
}
