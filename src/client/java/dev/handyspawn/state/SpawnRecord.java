package dev.handyspawn.state;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record SpawnRecord(BlockPos pos, ResourceKey<Level> dimension, SpawnKind kind) {
    public static SpawnRecord of(BlockPos pos, ResourceKey<Level> dim, SpawnKind kind) {
        return new SpawnRecord(pos.immutable(), dim, kind);
    }
}
