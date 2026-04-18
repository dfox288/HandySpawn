package dev.handyspawn.render.hud;

import dev.handyspawn.HandySpawnConstants;
import dev.handyspawn.config.HandySpawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class ThunderHud {

    private static final Identifier SPRITE =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/thunder_sleep.png");

    public static void render(GuiGraphicsExtractor ctx) {
        if (!HandySpawnConfig.get().daytimeThunderstormHint) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        Level w = mc.level;

        // Only surface when daytime AND thundering — the non-obvious sleep window.
        if (w.isDarkOutside()) return;     // night: user already knows they can sleep
        if (!w.isThundering()) return;     // no thunder: can't sleep at all

        int x = 4;
        int y = HudPrimitives.screenHeight() - 60;
        HudPrimitives.drawIconAt(ctx, SPRITE, x, y, 16);
    }

    private ThunderHud() {}
}
