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

class SpawnPersistenceTest {

    private static final ResourceKey<Level> OVERWORLD =
        ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));

    @Test
    void writingAndReadingRoundTrips(@TempDir Path tmp) {
        SpawnPersistence store = new SpawnPersistence(tmp);
        String worldId = "sp:TestWorld";
        SpawnRecord rec = SpawnRecord.of(new BlockPos(10, 64, -30), OVERWORLD, SpawnKind.BED);

        store.write(worldId, new SpawnPersistence.Snapshot(Optional.of(rec), Optional.empty()));
        SpawnPersistence.Snapshot read = store.read(worldId);

        assertEquals(Optional.of(rec), read.current());
        assertEquals(Optional.empty(), read.destroyed());
    }

    @Test
    void readingUnknownWorldReturnsEmpty(@TempDir Path tmp) {
        SpawnPersistence store = new SpawnPersistence(tmp);
        SpawnPersistence.Snapshot read = store.read("sp:NoSuch");
        assertTrue(read.current().isEmpty());
        assertTrue(read.destroyed().isEmpty());
    }

    @Test
    void differentWorldKeysAreIsolated(@TempDir Path tmp) {
        SpawnPersistence store = new SpawnPersistence(tmp);
        SpawnRecord a = SpawnRecord.of(new BlockPos(1, 1, 1), OVERWORLD, SpawnKind.BED);
        SpawnRecord b = SpawnRecord.of(new BlockPos(9, 9, 9), OVERWORLD, SpawnKind.RESPAWN_ANCHOR);
        store.write("sp:A", new SpawnPersistence.Snapshot(Optional.of(a), Optional.empty()));
        store.write("mp:server.example:25565", new SpawnPersistence.Snapshot(Optional.of(b), Optional.empty()));
        assertEquals(Optional.of(a), store.read("sp:A").current());
        assertEquals(Optional.of(b), store.read("mp:server.example:25565").current());
    }
}
