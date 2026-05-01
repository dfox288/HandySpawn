package dev.handy.mods.handyspawn.mixin;

import dev.handy.mods.handyspawn.HandySpawnConstants;
import dev.handy.mods.handyspawn.net.PersonalSpawnKind;
import dev.handy.mods.handyspawn.server.SpawnBroadcaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects bed destruction at its canonical vanilla entry point.
 *
 * <p>This replaces our earlier {@code PlayerBlockBreakEvents.AFTER} listener in
 * {@code HandySpawnServer}. That Fabric event turned out to be unreliable for
 * FOOT-half bed breaks in MC 26.1.2 — player-initiated FOOT breaks in survival
 * apparently did not trigger the AFTER dispatch in our test environment, so
 * clients never received the {@code destroyed=true} payload and the HOME marker
 * stuck around over the gone bed. {@link BedBlock#playerWillDestroy} is the
 * method vanilla itself uses to hook bed-break behaviour, so injecting HEAD
 * here is fire-once, guaranteed to run for both HEAD and FOOT breaks from any
 * caller that goes through {@code ServerPlayerGameMode.destroyBlock}.
 *
 * <p>Firing at {@code HEAD} (before vanilla's logic runs) also lets us read the
 * original {@link BlockState} cleanly — by {@code TAIL} the FOOT-break path has
 * already silently removed the HEAD partner (in creative mode) and the state
 * at the partner position is AIR.
 */
@Mixin(BedBlock.class)
public abstract class BedBlockMixin {

    @Inject(
        method = "playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("HEAD")
    )
    private void handyspawn$onBedDestroyed(Level level, BlockPos pos, BlockState state, Player player,
                                           CallbackInfoReturnable<BlockState> cir) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos partnerPos = pos.relative(BedBlock.getConnectedDirection(state));
        ResourceKey<Level> worldKey = serverLevel.dimension();

        for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
            ServerPlayer.RespawnConfig rc = p.getRespawnConfig();
            if (rc == null) continue;
            LevelData.RespawnData data = rc.respawnData();
            if (data == null) continue;
            BlockPos spawnPos = data.pos();
            if (!worldKey.equals(data.dimension())) continue;

            boolean hit = spawnPos.equals(pos) || spawnPos.equals(partnerPos);
            if (!hit) continue;

            SpawnBroadcaster.broadcastDestroyed(p, spawnPos, data.dimension(), PersonalSpawnKind.BED);
            // Clear the respawn; our ServerPlayerMixin's null-guard skips the
            // redundant empty broadcast that would otherwise race with the
            // destroyed payload above.
            p.setRespawnPosition(null, false);
        }
    }
}
