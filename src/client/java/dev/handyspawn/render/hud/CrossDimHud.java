package dev.handyspawn.render.hud;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.config.HandySpawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class CrossDimHud {

    public static void render(GuiGraphicsExtractor ctx) {
        if (!HandySpawnConfig.get().crossDimensionReadout) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        boolean hasCurrent;
        ResourceKey<Level> dim;
        if (HandySpawn.session().isServerSynced()) {
            var current = HandySpawn.personal().current().orElse(null);
            var destroyed = HandySpawn.personal().destroyedAt().orElse(null);
            if (current == null && destroyed == null) return;
            hasCurrent = current != null;
            dim = hasCurrent ? current.dimension() : destroyed.dimension();
        } else {
            var current = HandySpawn.compassTarget().state().current().orElse(null);
            var destroyed = HandySpawn.compassTarget().state().destroyedAt().orElse(null);
            if (current == null && destroyed == null) return;
            hasCurrent = current != null;
            dim = hasCurrent ? current.dimension() : destroyed.dimension();
        }

        // Hide if the player is in the same dimension as the referenced spawn — F1/F3 handle that case
        if (dim.equals(mc.level.dimension())) return;

        String dimName = simpleDimensionName(dim.identifier().toString());
        String status = hasCurrent ? "intact" : "DESTROYED";
        int colour = hasCurrent ? 0xFFFFFFFF : 0xFFFF5050;
        String line = "\u2302 " + dimName + " \u00b7 " + status; // ⌂ prefix, · separator

        int x = 4;
        int y = 4;
        HudPrimitives.drawTextAt(ctx, Component.literal(line), x, y, colour);
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
