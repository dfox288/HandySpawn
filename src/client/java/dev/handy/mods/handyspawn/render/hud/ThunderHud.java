package dev.handy.mods.handyspawn.render.hud;

import dev.handy.mods.handyspawn.HandySpawnConstants;
import dev.handy.mods.handyspawn.config.HandySpawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class ThunderHud {

    private static final Identifier SPRITE =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/thunder_sleep.png");

    public static int renderRight(GuiGraphicsExtractor ctx, int rightEdgeX, int y) {
        if (!HandySpawnConfig.get().daytimeThunderstormHint) return y;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return y;
        Level w = mc.level;

        // Only surface when daytime AND thundering — the non-obvious sleep window.
        if (w.isDarkOutside()) return y;     // night: user already knows they can sleep
        if (!w.isThundering()) return y;     // no thunder: can't sleep at all

        int iconSize = 16;
        HudPrimitives.drawIconAt(ctx, SPRITE, rightEdgeX - iconSize, y, iconSize);
        return y + iconSize + 2;
    }

    private ThunderHud() {}
}
