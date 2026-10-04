package com.mervyn.coolcreatestuff.jacquard.client;

import java.util.Iterator;

import org.jetbrains.annotations.Nullable;

import com.mervyn.coolcreatestuff.jacquard.Design;
import com.mervyn.coolcreatestuff.jacquard.DesignGen;
import com.mervyn.coolcreatestuff.jacquard.DesignSync;
import com.mojang.blaze3d.platform.NativeImage;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

// One texture per design, made the first time something draws it, the way MapRenderer keeps a
// texture per map. Unused ones are dropped after a minute and everything goes on disconnect.
public final class DesignTextures {
    // key for the loom's built-in debug design, which has no id
    public static final int DEBUG = -1;
    private static final long UNUSED_MS = 60_000;

    private static final class Entry {
        final DynamicTexture texture;
        final ResourceLocation location;
        long lastUsed;

        Entry(Design design, int key) {
            texture = new DynamicTexture(paint(design));
            location = Minecraft.getInstance().getTextureManager().register("createcoolstuffs_design_" + (key < 0 ? "debug" : key), texture);
        }
    }

    private static final Int2ObjectMap<Entry> TEXTURES = new Int2ObjectOpenHashMap<>();

    private DesignTextures() {}

    @Nullable
    public static ResourceLocation get(int id) {
        Design design = DesignSync.client(id);
        return design == null ? null : get(id, design);
    }

    public static ResourceLocation get(int key, Design design) {
        Entry entry = TEXTURES.get(key);
        if (entry == null) {
            entry = new Entry(design, key);
            TEXTURES.put(key, entry);
        }
        entry.lastUsed = Util.getMillis();
        return entry.location;
    }

    static void releaseUnused() {
        long now = Util.getMillis();
        Iterator<Entry> it = TEXTURES.values().iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (now - entry.lastUsed > UNUSED_MS) {
                Minecraft.getInstance().getTextureManager().release(entry.location);
                it.remove();
            }
        }
    }

    static void clear() {
        for (Entry entry : TEXTURES.values())
            Minecraft.getInstance().getTextureManager().release(entry.location);
        TEXTURES.clear();
    }

    // paintDesign(): 8 texels per design pixel with a woven shading pattern. The preview packs
    // every design into one 512 square; here each design gets a texture of its own size.
    static NativeImage paint(Design d) {
        int Wp = d.pixelWidth(), Hp = d.pixelHeight();
        NativeImage img = new NativeImage(Wp * 8, Hp * 8, false);
        for (int py = 0; py < Hp; py++)
            for (int px = 0; px < Wp; px++) {
                int[] rgb = DesignGen.DYE_RGB[d.dye(px, py)];
                for (int ty = 0; ty < 8; ty++)
                    for (int tx = 0; tx < 8; tx++) {
                        int w = ((tx + (ty >> 1)) & 3) < 2 ? 9 : -9;
                        if (ty == 7)
                            w -= 7;
                        if (tx == 7)
                            w -= 3;
                        w += ((px * 31 + py * 17 + tx * 7 + ty * 13) % 5) - 2;
                        img.setPixelRGBA(px * 8 + tx, py * 8 + ty, abgr(rgb[0] + w, rgb[1] + w, rgb[2] + w));
                    }
            }
        return img;
    }

    static int abgr(int r, int g, int b) {
        return 0xFF000000 | clamp(b) << 16 | clamp(g) << 8 | clamp(r);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
