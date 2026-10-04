package com.mervyn.coolcreatestuff.jacquard.client;

import org.joml.Vector3f;

import com.mervyn.coolcreatestuff.jacquard.TapestryEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

// hangOnWall() and layRug(). Drawn in pixels in a frame where x runs along the design's width,
// y up the design (toward its top) and z out of the face it shows; the wall or floor is at z -0.5.
public class TapestryRenderer extends EntityRenderer<TapestryEntity> {
    private static final int LINEN = 0xE2DAC6;

    public TapestryRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TapestryEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(TapestryEntity entity, float yaw, float partialTick, PoseStack ms, MultiBufferSource buffer, int light) {
        Direction face = entity.getDirection();
        int Wp = entity.widthBlocks() * 16, Hp = entity.heightBlocks() * 16;
        ms.pushPose();
        if (face.getAxis().isHorizontal()) {
            ms.mulPose(Axis.YP.rotationDegrees(-face.toYRot()));
        } else {
            Direction top = entity.top();
            ms.mulPose(Axis.YP.rotationDegrees(face == Direction.UP ? 180 - top.toYRot() : -top.toYRot()));
            ms.mulPose(Axis.XP.rotationDegrees(face == Direction.UP ? -90 : 90));
        }
        ms.scale(1 / 16f, 1 / 16f, 1 / 16f);

        float x0 = -Wp / 2f, x1 = Wp / 2f, y0 = -Hp / 2f, y1 = Hp / 2f, z = -0.5f;
        boolean rug = face == Direction.UP;
        Mesh m = new Mesh(ms, buffer.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS)), light, true);
        TextureAtlasSprite linen = Mesh.sprite("createcoolstuffs:block/linen");
        float front = rug ? z + 0.55f : z + 0.6f;
        m.box(linen, x0, y0, z, x1, y1, rug ? z + 0.5f : z + 0.55f);
        if (rug) {
            for (float x = x0 + 1; x < x1; x += 1.5f) {
                m.thread(new Vector3f(x, y1, z + 0.4f), new Vector3f(x + 0.3f, y1 + 2.2f, z + 0.1f), new Vector3f(1, 0, 0), 0.4f, LINEN);
                m.thread(new Vector3f(x, y0, z + 0.4f), new Vector3f(x - 0.3f, y0 - 2.2f, z + 0.1f), new Vector3f(1, 0, 0), 0.4f, LINEN);
            }
        } else {
            TextureAtlasSprite brass = Mesh.sprite("create:block/brass_block");
            m.box(brass, x0 - 1.5f, y1 - 0.2f, z + 0.1f, x1 + 1.5f, y1 + 0.8f, z + 1.1f);
            m.box(brass, x0 - 2.5f, y1 - 0.7f, z - 0.2f, x0 - 1.2f, y1 + 1.3f, z + 1.4f);
            m.box(brass, x1 + 1.2f, y1 - 0.7f, z - 0.2f, x1 + 2.5f, y1 + 1.3f, z + 1.4f);
        }

        ResourceLocation design = DesignTextures.get(entity.designId());
        if (design != null) {
            Mesh art = new Mesh(ms, buffer.getBuffer(RenderType.entitySolid(design)), light, true);
            art.quad(null, new Vector3f(x0, y1, front), new Vector3f(x1, y1, front), new Vector3f(x1, y0, front), new Vector3f(x0, y0, front),
                    new float[] {0, 0, 1, 0, 1, 1, 0, 1}, new Vector3f(0, 0, 1), Mesh.WHITE, false);
        }
        ms.popPose();
        super.render(entity, yaw, partialTick, ms, buffer, light);
    }
}
