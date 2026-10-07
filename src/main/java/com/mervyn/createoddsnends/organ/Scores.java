package com.mervyn.createoddsnends.organ;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class Scores extends SavedData {
    private static final String NAME = "createoddsnends_scores";

    private final Int2ObjectMap<Score> scores = new Int2ObjectOpenHashMap<>();
    private int nextId;

    public static Scores get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Scores::new, Scores::load), NAME);
    }

    @Nullable
    public Score get(int id) {
        return scores.get(id);
    }

    public int add(Score score) {
        for (Int2ObjectMap.Entry<Score> entry : scores.int2ObjectEntrySet())
            if (entry.getValue().equals(score))
                return entry.getIntKey();
        int id = nextId++;
        scores.put(id, score);
        setDirty();
        return id;
    }

    private static Scores load(CompoundTag tag, HolderLookup.Provider registries) {
        Scores out = new Scores();
        out.nextId = tag.getInt("NextId");
        for (Tag entry : tag.getList("Scores", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) entry;
            Score.CODEC.parse(NbtOps.INSTANCE, c.get("Score")).ifSuccess(s -> out.scores.put(c.getInt("Id"), s));
        }
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Int2ObjectMap.Entry<Score> e : scores.int2ObjectEntrySet()) {
            CompoundTag c = new CompoundTag();
            c.putInt("Id", e.getIntKey());
            Score.CODEC.encodeStart(NbtOps.INSTANCE, e.getValue()).ifSuccess(t -> c.put("Score", t));
            list.add(c);
        }
        tag.put("Scores", list);
        tag.putInt("NextId", nextId);
        return tag;
    }
}
