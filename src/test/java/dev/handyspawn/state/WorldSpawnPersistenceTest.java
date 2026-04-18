package dev.handyspawn.state;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WorldSpawnPersistenceTest {

    private static final ResourceKey<Level> OVERWORLD =
        ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));

    @Test
    void writingAndReadingRoundTrips(@TempDir Path tmp) {
        WorldSpawnPersistence store = new WorldSpawnPersistence(tmp);
        String worldId = "sp:TestWorld";
        WorldSpawnRecord rec = WorldSpawnRecord.of(new BlockPos(10, 64, -30), OVERWORLD, WorldSpawnKind.BED);

        store.write(worldId, new WorldSpawnPersistence.Snapshot(Optional.of(rec), Optional.empty()));
        WorldSpawnPersistence.Snapshot read = store.read(worldId);

        assertEquals(Optional.of(rec), read.current());
        assertEquals(Optional.empty(), read.destroyed());
    }

    @Test
    void readingUnknownWorldReturnsEmpty(@TempDir Path tmp) {
        WorldSpawnPersistence store = new WorldSpawnPersistence(tmp);
        WorldSpawnPersistence.Snapshot read = store.read("sp:NoSuch");
        assertTrue(read.current().isEmpty());
        assertTrue(read.destroyed().isEmpty());
    }

    @Test
    void differentWorldKeysAreIsolated(@TempDir Path tmp) {
        WorldSpawnPersistence store = new WorldSpawnPersistence(tmp);
        WorldSpawnRecord a = WorldSpawnRecord.of(new BlockPos(1, 1, 1), OVERWORLD, WorldSpawnKind.BED);
        WorldSpawnRecord b = WorldSpawnRecord.of(new BlockPos(9, 9, 9), OVERWORLD, WorldSpawnKind.RESPAWN_ANCHOR);
        store.write("sp:A", new WorldSpawnPersistence.Snapshot(Optional.of(a), Optional.empty()));
        store.write("mp:server.example:25565", new WorldSpawnPersistence.Snapshot(Optional.of(b), Optional.empty()));
        assertEquals(Optional.of(a), store.read("sp:A").current());
        assertEquals(Optional.of(b), store.read("mp:server.example:25565").current());
    }
}
