package dev.handyspawn.state;

import dev.handyspawn.net.PersonalSpawnKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PersonalSpawnStateTest {

    private static final ResourceKey<Level> OVERWORLD =
        ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));

    @Test
    void emptyInitially() {
        PersonalSpawnState s = new PersonalSpawnState();
        assertTrue(s.current().isEmpty());
        assertTrue(s.destroyedAt().isEmpty());
    }

    @Test
    void livePayloadPopulatesCurrentOnly() {
        PersonalSpawnState s = new PersonalSpawnState();
        BlockPos pos = new BlockPos(10, 64, -20);
        s.applyPayload(Optional.of(pos), Optional.of(OVERWORLD), PersonalSpawnKind.BED, false);

        PersonalSpawnState.Record cur = s.current().orElseThrow();
        assertEquals(pos, cur.pos());
        assertEquals(OVERWORLD, cur.dimension());
        assertEquals(PersonalSpawnKind.BED, cur.kind());
        assertTrue(s.destroyedAt().isEmpty());
    }

    @Test
    void destroyedPayloadPopulatesDestroyedOnly() {
        PersonalSpawnState s = new PersonalSpawnState();
        BlockPos pos = new BlockPos(10, 64, -20);
        s.applyPayload(Optional.of(pos), Optional.of(OVERWORLD), PersonalSpawnKind.RESPAWN_ANCHOR, true);

        PersonalSpawnState.Record dest = s.destroyedAt().orElseThrow();
        assertEquals(pos, dest.pos());
        assertEquals(OVERWORLD, dest.dimension());
        assertEquals(PersonalSpawnKind.RESPAWN_ANCHOR, dest.kind());
        assertTrue(s.current().isEmpty());
    }

    @Test
    void consecutiveLivePayloadsUpdateCurrentWithoutDestroyedMarker() {
        PersonalSpawnState s = new PersonalSpawnState();
        BlockPos a = new BlockPos(1, 64, 1);
        BlockPos b = new BlockPos(5, 64, 7);

        s.applyPayload(Optional.of(a), Optional.of(OVERWORLD), PersonalSpawnKind.BED, false);
        s.applyPayload(Optional.of(b), Optional.of(OVERWORLD), PersonalSpawnKind.BED, false);

        assertEquals(b, s.current().orElseThrow().pos());
        assertTrue(s.destroyedAt().isEmpty());
    }

    @Test
    void emptyPayloadClearsBothSlots() {
        PersonalSpawnState s = new PersonalSpawnState();
        BlockPos pos = new BlockPos(1, 64, 1);
        s.applyPayload(Optional.of(pos), Optional.of(OVERWORLD), PersonalSpawnKind.BED, false);

        s.applyPayload(Optional.empty(), Optional.empty(), PersonalSpawnKind.BED, false);

        assertTrue(s.current().isEmpty());
        assertTrue(s.destroyedAt().isEmpty());
    }

    @Test
    void livePayloadAfterDestroyedClearsDestroyed() {
        PersonalSpawnState s = new PersonalSpawnState();
        BlockPos oldPos = new BlockPos(1, 64, 1);
        BlockPos freshPos = new BlockPos(5, 64, 7);

        s.applyPayload(Optional.of(oldPos), Optional.of(OVERWORLD), PersonalSpawnKind.BED, true);
        s.applyPayload(Optional.of(freshPos), Optional.of(OVERWORLD), PersonalSpawnKind.BED, false);

        assertEquals(freshPos, s.current().orElseThrow().pos());
        assertTrue(s.destroyedAt().isEmpty());
    }

    @Test
    void clearEmptiesBothSlots() {
        PersonalSpawnState s = new PersonalSpawnState();
        BlockPos pos = new BlockPos(1, 64, 1);
        s.applyPayload(Optional.of(pos), Optional.of(OVERWORLD), PersonalSpawnKind.BED, true);

        s.clear();

        assertTrue(s.current().isEmpty());
        assertTrue(s.destroyedAt().isEmpty());
    }
}
