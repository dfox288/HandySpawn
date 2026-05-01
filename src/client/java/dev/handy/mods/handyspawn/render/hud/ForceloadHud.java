package dev.handy.mods.handyspawn.render.hud;

import dev.handy.mods.handyspawn.HandySpawnClient;
import dev.handy.mods.handyspawn.HandySpawnConstants;
import dev.handy.mods.handyspawn.config.HandySpawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

/**
 * HUD overlay that places a chunk-grid glyph in the top-right whenever the player is
 * standing inside a {@code /forceload}-loaded chunk. Works against
 * {@link dev.handy.mods.handyspawn.state.SharedForceloadState}, which is populated from
 * server broadcasts, so the badge also shows up on dedicated servers provided
 * they have HandySpawn installed. Dimensions with no sync data simply report an
 * empty set, so {@code containsChunk} short-circuits to false.
 */
public final class ForceloadHud {

    private static final Identifier SPRITE =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/forceload.png");

    private static final int LABEL_ARGB = 0xFF40E0FF; // matches the in-world INSIDE tint.

    public static int renderRight(GuiGraphicsExtractor ctx, int rightEdgeX, int y) {
        if (!HandySpawnConfig.get().forceloadBadge) return y;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return y;

        ChunkPos here = ChunkPos.containing(mc.player.blockPosition());
        if (!HandySpawnClient.forceload().containsChunk(mc.level.dimension(), here)) return y;

        // Icon on the right edge, text label to its left so the whole row
        // reads as "FORCELOADED [icon]" aligned to the rail's right edge.
        int iconSize = 16;
        int iconX = rightEdgeX - iconSize;
        HudPrimitives.drawIconAt(ctx, SPRITE, iconX, y, iconSize);
        Component label = Component.translatable("hud.handyspawn.forceload_inside");
        int textY = y + (iconSize - mc.font.lineHeight) / 2;
        HudPrimitives.drawTextRightAligned(ctx, label, iconX - 3, textY, LABEL_ARGB);
        return y + iconSize + 2;
    }

    private ForceloadHud() {}
}
