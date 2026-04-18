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

class CompassTargetPersistenceTest {

    private static final ResourceKey<Level> OVERWORLD =
        ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));

    @Test
    void writingAndReadingRoundTrips(@TempDir Path tmp) {
        CompassTargetPersistence store = new CompassTargetPersistence(tmp);
        String worldId = "sp:TestWorld";
        CompassTargetRecord rec = CompassTargetRecord.of(new BlockPos(10, 64, -30), OVERWORLD, CompassTargetKind.BED);

        store.write(worldId, new CompassTargetPersistence.Snapshot(Optional.of(rec), Optional.empty()));
        CompassTargetPersistence.Snapshot read = store.read(worldId);

        assertEquals(Optional.of(rec), read.current());
        assertEquals(Optional.empty(), read.destroyed());
    }

    @Test
    void readingUnknownWorldReturnsEmpty(@TempDir Path tmp) {
        CompassTargetPersistence store = new CompassTargetPersistence(tmp);
        CompassTargetPersistence.Snapshot read = store.read("sp:NoSuch");
        assertTrue(read.current().isEmpty());
        assertTrue(read.destroyed().isEmpty());
    }

    @Test
    void differentWorldKeysAreIsolated(@TempDir Path tmp) {
        CompassTargetPersistence store = new CompassTargetPersistence(tmp);
        CompassTargetRecord a = CompassTargetRecord.of(new BlockPos(1, 1, 1), OVERWORLD, CompassTargetKind.BED);
        CompassTargetRecord b = CompassTargetRecord.of(new BlockPos(9, 9, 9), OVERWORLD, CompassTargetKind.RESPAWN_ANCHOR);
        store.write("sp:A", new CompassTargetPersistence.Snapshot(Optional.of(a), Optional.empty()));
        store.write("mp:server.example:25565", new CompassTargetPersistence.Snapshot(Optional.of(b), Optional.empty()));
        assertEquals(Optional.of(a), store.read("sp:A").current());
        assertEquals(Optional.of(b), store.read("mp:server.example:25565").current());
    }
}
