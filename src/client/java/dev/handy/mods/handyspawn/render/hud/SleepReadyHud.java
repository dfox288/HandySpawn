package dev.handy.mods.handyspawn.render.hud;

import dev.handy.mods.handyspawn.config.HandySpawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Top-left textual readout that tells the player when sleep would succeed
 * right now — night, thunderstorm, or both. Renders one line below the {@code ⌂}
 * line produced by {@link CrossDimHud}.
 *
 * <p>Sleep conditions in MC 26.1.2 (same as {@link dev.handy.mods.handyspawn.render.world.BedTintRenderer}):
 * {@link Level#isDarkOutside()} (night or dimension-specific equivalent) OR
 * {@link Level#isThundering()}. Plain rain does not qualify.
 *
 * <p>Overlaps conceptually with {@code ThunderHud} (a bottom-left icon shown
 * only for daytime thunderstorms); we keep both because a text line is more
 * discoverable next to the spawn info, while the bottom icon stays glanceable
 * at the hotbar.
 */
public final class SleepReadyHud {

    public static int renderRight(GuiGraphicsExtractor ctx, int rightEdgeX, int y) {
        if (!HandySpawnConfig.get().sleepReadinessTint) return y;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return y;
        Level w = mc.level;

        boolean night = w.isDarkOutside();
        boolean thunder = w.isThundering();
        if (!night && !thunder) return y;

        String label;
        if (night && thunder) {
            label = "\u263E sleep ready \u00b7 night + thunder";
        } else if (thunder) {
            label = "\u263E sleep ready \u00b7 thunder";
        } else {
            label = "\u263E sleep ready";
        }

        HudPrimitives.drawTextRightAligned(ctx, Component.literal(label), rightEdgeX, y, 0xFFB0E0A0);
        return y + HudPrimitives.textRowHeight();
    }

    private SleepReadyHud() {}
}
