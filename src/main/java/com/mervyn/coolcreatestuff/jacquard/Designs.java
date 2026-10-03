package com.mervyn.coolcreatestuff.jacquard;

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

// Every design in the world by id, kept on the overworld the way vanilla keeps map data.
// Items and entities only ever hold the id.
public class Designs extends SavedData {
    private static final String NAME = "createcoolstuffs_designs";

    private final Int2ObjectMap<Design> designs = new Int2ObjectOpenHashMap<>();
    private int nextId;

    public static Designs get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Designs::new, Designs::load), NAME);
    }

    @Nullable
    public Design get(int id) {
        return designs.get(id);
    }

    public int add(Design design) {
        int id = nextId++;
        designs.put(id, design);
        setDirty();
        return id;
    }

    private static Designs load(CompoundTag tag, HolderLookup.Provider registries) {
        Designs out = new Designs();
        out.nextId = tag.getInt("NextId");
        for (Tag entry : tag.getList("Designs", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) entry;
            Design.CODEC.parse(NbtOps.INSTANCE, c.get("Design")).ifSuccess(d -> out.designs.put(c.getInt("Id"), d));
        }
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Int2ObjectMap.Entry<Design> e : designs.int2ObjectEntrySet()) {
            CompoundTag c = new CompoundTag();
            c.putInt("Id", e.getIntKey());
            Design.CODEC.encodeStart(NbtOps.INSTANCE, e.getValue()).ifSuccess(t -> c.put("Design", t));
            list.add(c);
        }
        tag.put("Designs", list);
        tag.putInt("NextId", nextId);
        return tag;
    }
}
