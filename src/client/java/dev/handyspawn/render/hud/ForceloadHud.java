package dev.handyspawn.render.hud;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.HandySpawnConstants;
import dev.handyspawn.config.HandySpawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

/**
 * HUD overlay that places a chunk-grid glyph in the top-right whenever the player is
 * standing inside a {@code /forceload}-loaded chunk. Singleplayer only — the integrated
 * server is queried directly, so on a dedicated server {@link
 * dev.handyspawn.state.ForceloadState#isAvailable()} returns false and the badge hides.
 */
public final class ForceloadHud {

    private static final Identifier SPRITE =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/forceload.png");

    public static void render(GuiGraphicsExtractor ctx) {
        if (!HandySpawnConfig.get().forceloadBadge) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        if (!HandySpawn.forceload().isAvailable()) return;

        ChunkPos here = ChunkPos.containing(mc.player.blockPosition());
        if (!HandySpawn.forceload().containsChunk(mc.level.dimension(), here)) return;

        int x = HudPrimitives.screenWidth() - 24;
        int y = 4;
        HudPrimitives.drawIconAt(ctx, SPRITE, x, y, 16);
    }

    private ForceloadHud() {}
}
