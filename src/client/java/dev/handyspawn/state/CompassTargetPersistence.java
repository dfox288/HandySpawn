package dev.handyspawn.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Per-world persistence of the tracked compass-target state. Files are keyed by
 * a SHA-1 hash of a caller-supplied world identifier so singleplayer save names
 * and multiplayer server addresses don't collide on disk.
 */
public final class CompassTargetPersistence {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record Dto(Entry current, Entry destroyed) {}
    public record Entry(int x, int y, int z, String dimension, String kind) {}
    public record Snapshot(Optional<CompassTargetRecord> current, Optional<CompassTargetRecord> destroyed) {}

    private final Path baseDir;

    public CompassTargetPersistence(Path baseDir) {
        this.baseDir = baseDir;
    }

    public Snapshot read(String worldId) {
        Path file = pathFor(worldId);
        if (!Files.exists(file)) return new Snapshot(Optional.empty(), Optional.empty());
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Dto dto = GSON.fromJson(json, Dto.class);
            if (dto == null) return new Snapshot(Optional.empty(), Optional.empty());
            return new Snapshot(
                Optional.ofNullable(dto.current()).map(CompassTargetPersistence::toRecord),
                Optional.ofNullable(dto.destroyed()).map(CompassTargetPersistence::toRecord)
            );
        } catch (IOException | RuntimeException e) {
            return new Snapshot(Optional.empty(), Optional.empty());
        }
    }

    public void write(String worldId, Snapshot snap) {
        Path file = pathFor(worldId);
        try {
            Files.createDirectories(file.getParent());
            Dto dto = new Dto(
                snap.current().map(CompassTargetPersistence::toEntry).orElse(null),
                snap.destroyed().map(CompassTargetPersistence::toEntry).orElse(null)
            );
            Files.writeString(file, GSON.toJson(dto), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Persistence failure is non-fatal; state remains in memory.
        }
    }

    private Path pathFor(String worldId) {
        return baseDir.resolve(hash(worldId) + ".json");
    }

    private static String hash(String worldId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] out = md.digest(worldId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Entry toEntry(CompassTargetRecord r) {
        return new Entry(
            r.pos().getX(), r.pos().getY(), r.pos().getZ(),
            r.dimension().identifier().toString(),
            r.kind().name()
        );
    }

    private static CompassTargetRecord toRecord(Entry e) {
        Identifier id = Identifier.parse(e.dimension());
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, id);
        return CompassTargetRecord.of(new BlockPos(e.x(), e.y(), e.z()), dim, CompassTargetKind.valueOf(e.kind()));
    }
}
