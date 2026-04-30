package dev.handy.mods.handyspawn.render.hud;

import dev.handy.mods.handyspawn.HandySpawn;
import dev.handy.mods.handyspawn.HandySpawnConstants;
import dev.handy.mods.handyspawn.config.HandySpawnConfig;
import dev.handy.mods.handyspawn.state.PhantomTimer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PhantomHud {

    private static final Identifier WARN =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/phantom_warn.png");
    private static final Identifier ACTIVE =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/phantom_active.png");

    /**
     * DEBUG: when true, ignore the real {@code time_since_rest} stat and cycle
     * SILENT → WARNING → ACTIVE every 5 seconds so the HUD states can be eyeballed
     * without waiting an hour of real time. Set back to {@code false} before release.
     */
    private static final boolean DEBUG_CYCLE_PHASES = false;
    private static final long DEBUG_CYCLE_PERIOD_MS = 5000L;

    public static int renderRight(GuiGraphicsExtractor ctx, int rightEdgeX, int y) {
        HandySpawnConfig cfg = HandySpawnConfig.get();
        if (!cfg.phantomTimer) return y;

        int t;
        if (DEBUG_CYCLE_PHASES) {
            long cycle = (System.currentTimeMillis() / DEBUG_CYCLE_PERIOD_MS) % 3L;
            // 0 → SILENT, 1 → mid-way into WARNING, 2 → ACTIVE
            t = cycle == 0
                ? 0
                : cycle == 1
                    ? cfg.phantomWarningStartTick + (PhantomTimer.PHANTOM_THRESHOLD_TICKS - cfg.phantomWarningStartTick) / 2
                    : PhantomTimer.PHANTOM_THRESHOLD_TICKS + 100;
        } else {
            t = HandySpawn.phantom().timeSinceRest();
        }
        PhantomTimer.Phase phase = PhantomTimer.phaseFor(t, cfg.phantomWarningStartTick);
        if (phase == PhantomTimer.Phase.SILENT) return y;

        // Row layout: [text] [4px] [16px icon]   where the icon anchors to rightEdgeX.
        // Text baseline sits at y + 4 so it vertically centres against the icon.
        String label;
        int textColour;
        Identifier icon;
        if (phase == PhantomTimer.Phase.WARNING) {
            int remaining = PhantomTimer.ticksRemainingUntilActive(t);
            label = String.format("%d:%02d", remaining / 1200, (remaining % 1200) / 20);
            textColour = 0xFFFFD040;
            icon = WARN;
        } else {
            label = "phantoms";
            textColour = 0xFFFF4040;
            icon = ACTIVE;
        }

        int iconSize = 16;
        int gap = 4;
        int iconX = rightEdgeX - iconSize;
        int textW = net.minecraft.client.Minecraft.getInstance().font.width(label);
        int textX = iconX - gap - textW;
        HudPrimitives.drawIconAt(ctx, icon, iconX, y, iconSize);
        HudPrimitives.drawTextAt(ctx, Component.literal(label), textX, y + 4, textColour);
        return y + iconSize + 2;
    }

    private PhantomHud() {}
}
