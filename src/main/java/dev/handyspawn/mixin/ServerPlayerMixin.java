package dev.handyspawn.mixin;

import dev.handyspawn.server.SpawnBroadcaster;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side hook that pushes a fresh {@link dev.handyspawn.net.PersonalSpawnPayload}
 * to the affected player whenever vanilla updates their respawn config.
 *
 * <p>Targets {@code ServerPlayer.setRespawnPosition(RespawnConfig, boolean)} at
 * {@code TAIL}, so the broadcast sees the post-mutation state regardless of
 * whether the caller is {@code Player.startSleepInBed},
 * {@code RespawnAnchorBlock.trySetSpawn}, {@code /spawnpoint}, or any other
 * entry point that routes through this single setter.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(
        method = "setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V",
        at = @At("TAIL")
    )
    private void handyspawn$broadcastAfterSpawnChange(ServerPlayer.RespawnConfig config,
                                                      boolean notifyClient,
                                                      CallbackInfo ci) {
        // Skip the broadcast when vanilla clears the respawn config to null —
        // that always follows a bed break, and the PlayerBlockBreakEvents.AFTER
        // path already sent an explicit destroyed=true payload via
        // SpawnBroadcaster.broadcastDestroyed. Broadcasting an empty payload
        // here would race with (and clobber) the destroyed record the client
        // just latched, preventing the ghost marker from rendering.
        if (config == null) return;
        SpawnBroadcaster.broadcastPersonalSpawn((ServerPlayer) (Object) this);
    }
}
