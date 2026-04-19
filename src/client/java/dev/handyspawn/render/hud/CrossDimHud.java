package dev.handyspawn.render.hud;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.config.HandySpawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class CrossDimHud {

    /**
     * Renders one line right-aligned at {@code (rightEdgeX, y)} and returns the
     * new Y (advanced by one text row) if anything was drawn, or the original Y
     * otherwise. Used by the top-right HUD rail in {@link HudRenderer}.
     */
    public static int renderRight(GuiGraphicsExtractor ctx, int rightEdgeX, int y) {
        if (!HandySpawnConfig.get().crossDimensionReadout) return y;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return y;

        boolean hasCurrent;
        ResourceKey<Level> dim;
        net.minecraft.core.BlockPos pos;
        if (HandySpawn.session().isServerSynced()) {
            var current = HandySpawn.personal().current().orElse(null);
            var destroyed = HandySpawn.personal().destroyedAt().orElse(null);
            if (current == null && destroyed == null) return y;
            hasCurrent = current != null;
            dim = hasCurrent ? current.dimension() : destroyed.dimension();
            pos = hasCurrent ? current.pos() : destroyed.pos();
        } else {
            var current = HandySpawn.compassTarget().state().current().orElse(null);
            var destroyed = HandySpawn.compassTarget().state().destroyedAt().orElse(null);
            if (current == null && destroyed == null) return y;
            hasCurrent = current != null;
            dim = hasCurrent ? current.dimension() : destroyed.dimension();
            pos = hasCurrent ? current.pos() : destroyed.pos();
        }

        // Render in every dimension now: in-dimension players want the coords
        // just as much as cross-dim players want the reminder that their bed
        // is elsewhere. F1/F3 (HOME / GONE world-space markers) handle the
        // spatial cue separately and don't conflict with this textual readout.
        boolean crossDim = !dim.equals(mc.level.dimension());
        StringBuilder sb = new StringBuilder("\u2302 "); // ⌂ prefix
        if (crossDim) {
            sb.append(simpleDimensionName(dim.identifier().toString())).append(" \u00b7 ");
        }
        sb.append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ());
        if (!hasCurrent) {
            sb.append(" \u00b7 not set");
        }

        int colour = hasCurrent ? 0xFFFFFFFF : 0xFFFF5050;
        HudPrimitives.drawTextRightAligned(ctx, Component.literal(sb.toString()), rightEdgeX, y, colour);
        return y + HudPrimitives.textRowHeight();
    }

    private static String simpleDimensionName(String id) {
        return switch (id) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> id;
        };
    }

    private CrossDimHud() {}
}
