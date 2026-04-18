package dev.handyspawn.render.hud;

import dev.handyspawn.HandySpawn;
import dev.handyspawn.HandySpawnConstants;
import dev.handyspawn.config.HandySpawnConfig;
import dev.handyspawn.state.PhantomTimer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PhantomHud {

    private static final Identifier WARN =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/phantom_warn.png");
    private static final Identifier ACTIVE =
        Identifier.fromNamespaceAndPath(HandySpawnConstants.MOD_ID, "textures/gui/icons/phantom_active.png");

    public static void render(GuiGraphicsExtractor ctx) {
        HandySpawnConfig cfg = HandySpawnConfig.get();
        if (!cfg.phantomTimer) return;

        int t = HandySpawn.phantom().timeSinceRest();
        PhantomTimer.Phase phase = PhantomTimer.phaseFor(t, cfg.phantomWarningStartTick);
        if (phase == PhantomTimer.Phase.SILENT) return;

        int x = 4;
        int y = HudPrimitives.screenHeight() - 40;
        if (phase == PhantomTimer.Phase.WARNING) {
            HudPrimitives.drawIconAt(ctx, WARN, x, y, 16);
            int remaining = PhantomTimer.ticksRemainingUntilActive(t);
            String label = String.format("%d:%02d", remaining / 1200, (remaining % 1200) / 20);
            HudPrimitives.drawTextAt(ctx, Component.literal(label), x + 20, y + 4, 0xFFFFD040);
        } else {
            HudPrimitives.drawIconAt(ctx, ACTIVE, x, y, 16);
            HudPrimitives.drawTextAt(ctx, Component.literal("phantoms"), x + 20, y + 4, 0xFFFF4040);
        }
    }

    private PhantomHud() {}
}
