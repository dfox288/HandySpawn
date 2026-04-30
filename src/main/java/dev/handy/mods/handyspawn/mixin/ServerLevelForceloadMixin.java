package dev.handy.mods.handyspawn.mixin;

import dev.handy.mods.handyspawn.HandySpawnServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side hook that rebroadcasts the dimension's full force-loaded chunk
 * set whenever vanilla mutates it via
 * {@code ServerLevel.setChunkForced(int, int, boolean)}.
 *
 * <p>There is no Fabric event for forceload mutations, so we inject at
 * {@code TAIL} of the sole setter to capture both {@code /forceload add} and
 * {@code /forceload remove} plus any datapack / mod driven changes that go
 * through the same path. The broadcast fires once per mutation and sends a
 * fresh snapshot rather than diffing, which keeps the protocol stateless and
 * resilient to reordering.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelForceloadMixin {

    @Inject(
        method = "setChunkForced(IIZ)Z",
        at = @At("TAIL")
    )
    private void handyspawn$broadcastForceloadChange(int x, int z, boolean force,
                                                     CallbackInfoReturnable<Boolean> cir) {
        HandySpawnServer.onForceloadChanged((ServerLevel) (Object) this);
    }
}
