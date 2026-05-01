package dev.handy.mods.handyspawn.state;

import dev.handy.mods.handyspawn.spawn.SpawnBlockKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompassTargetStateTest {

    private static final ResourceKey<Level> OVERWORLD =
        ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));

    @Test
    void currentIsEmptyInitially() {
        CompassTargetState s = new CompassTargetState();
        assertTrue(s.current().isEmpty());
        assertTrue(s.destroyedAt().isEmpty());
    }

    @Test
    void setCurrentStoresRecord() {
        CompassTargetState s = new CompassTargetState();
        CompassTargetRecord r = CompassTargetRecord.of(new BlockPos(1, 64, 2), OVERWORLD, SpawnBlockKind.BED);
        s.setCurrent(r);
        assertEquals(r, s.current().orElseThrow());
    }

    @Test
    void clearingCurrentWhilePreviousExistedRecordsDestroyed() {
        CompassTargetState s = new CompassTargetState();
        CompassTargetRecord old = CompassTargetRecord.of(new BlockPos(1, 64, 2), OVERWORLD, SpawnBlockKind.BED);
        s.setCurrent(old);
        s.clearCurrent();
        assertTrue(s.current().isEmpty());
        assertEquals(old, s.destroyedAt().orElseThrow());
    }

    @Test
    void newSpawnClearsDestroyed() {
        CompassTargetState s = new CompassTargetState();
        CompassTargetRecord old = CompassTargetRecord.of(new BlockPos(1, 64, 2), OVERWORLD, SpawnBlockKind.BED);
        CompassTargetRecord fresh = CompassTargetRecord.of(new BlockPos(5, 64, 7), OVERWORLD, SpawnBlockKind.BED);
        s.setCurrent(old);
        s.clearCurrent();
        s.setCurrent(fresh);
        assertEquals(fresh, s.current().orElseThrow());
        assertTrue(s.destroyedAt().isEmpty());
    }

    @Test
    void settingIdenticalRecordDoesNotCreateDestroyedMarker() {
        CompassTargetState s = new CompassTargetState();
        CompassTargetRecord r = CompassTargetRecord.of(new BlockPos(1, 64, 2), OVERWORLD, SpawnBlockKind.BED);
        s.setCurrent(r);
        s.setCurrent(r);
        assertTrue(s.destroyedAt().isEmpty());
    }
}
