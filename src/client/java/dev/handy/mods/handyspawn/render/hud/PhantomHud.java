package dev.handy.mods.handyspawn.render.hud;

import dev.handy.mods.handyspawn.HandySpawnClient;
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

    public static int renderRight(GuiGraphicsExtractor ctx, int rightEdgeX, int y) {
        HandySpawnConfig cfg = HandySpawnConfig.get();
        if (!cfg.phantomTimer) return y;

        int t = HandySpawnClient.phantom().timeSinceRest();
        PhantomTimer.Phase phase = PhantomTimer.phaseFor(t, cfg.phantomWarningStartTick);
        if (phase == PhantomTimer.Phase.SILENT) return y;

        // Row layout: [text] [4px] [16px icon]   where the icon anchors to rightEdgeX.
        // Text baseline sits at y + 4 so it vertically centres against the icon.
        String label;
        int textColour;
        Identifier icon;
        if (phase == PhantomTimer.Phase.WARNING) {
            int remaining = PhantomTimer.ticksRemainingUntilActive(t);
            int minutes = remaining / PhantomTimer.TICKS_PER_MINUTE;
            int seconds = (remaining % PhantomTimer.TICKS_PER_MINUTE) / PhantomTimer.TICKS_PER_SECOND;
            label = String.format("%d:%02d", minutes, seconds);
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
