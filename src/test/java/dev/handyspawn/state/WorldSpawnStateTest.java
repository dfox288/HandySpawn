package dev.handyspawn.state;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldSpawnStateTest {

    private static final ResourceKey<Level> OVERWORLD =
        ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));

    @Test
    void currentIsEmptyInitially() {
        WorldSpawnState s = new WorldSpawnState();
        assertTrue(s.current().isEmpty());
        assertTrue(s.destroyedAt().isEmpty());
    }

    @Test
    void setCurrentStoresRecord() {
        WorldSpawnState s = new WorldSpawnState();
        WorldSpawnRecord r = WorldSpawnRecord.of(new BlockPos(1, 64, 2), OVERWORLD, WorldSpawnKind.BED);
        s.setCurrent(r);
        assertEquals(r, s.current().orElseThrow());
    }

    @Test
    void clearingCurrentWhilePreviousExistedRecordsDestroyed() {
        WorldSpawnState s = new WorldSpawnState();
        WorldSpawnRecord old = WorldSpawnRecord.of(new BlockPos(1, 64, 2), OVERWORLD, WorldSpawnKind.BED);
        s.setCurrent(old);
        s.clearCurrent();
        assertTrue(s.current().isEmpty());
        assertEquals(old, s.destroyedAt().orElseThrow());
    }

    @Test
    void newSpawnClearsDestroyed() {
        WorldSpawnState s = new WorldSpawnState();
        WorldSpawnRecord old = WorldSpawnRecord.of(new BlockPos(1, 64, 2), OVERWORLD, WorldSpawnKind.BED);
        WorldSpawnRecord fresh = WorldSpawnRecord.of(new BlockPos(5, 64, 7), OVERWORLD, WorldSpawnKind.BED);
        s.setCurrent(old);
        s.clearCurrent();
        s.setCurrent(fresh);
        assertEquals(fresh, s.current().orElseThrow());
        assertTrue(s.destroyedAt().isEmpty());
    }

    @Test
    void settingIdenticalRecordDoesNotCreateDestroyedMarker() {
        WorldSpawnState s = new WorldSpawnState();
        WorldSpawnRecord r = WorldSpawnRecord.of(new BlockPos(1, 64, 2), OVERWORLD, WorldSpawnKind.BED);
        s.setCurrent(r);
        s.setCurrent(r);
        assertTrue(s.destroyedAt().isEmpty());
    }
}
