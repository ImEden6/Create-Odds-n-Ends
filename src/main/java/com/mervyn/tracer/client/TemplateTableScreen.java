package com.mervyn.tracer.client;

import java.util.Arrays;

import com.mervyn.tracer.Profile;
import com.mervyn.tracer.Tracer;
import com.mervyn.tracer.template.SaveProfile;
import com.mervyn.tracer.template.TemplateTableMenu;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public class TemplateTableScreen extends AbstractContainerScreen<TemplateTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "textures/gui/template_table.png");
    private static final BlockState PREVIEW_BLOCK = Blocks.OAK_PLANKS.defaultBlockState();

    // bar wells: 16 columns 7px apart, each radius step is 6px, foot layer on the left
    private static final int BARS_X = 9;
    private static final int BARS_BOTTOM = 69;
    private static final int BAR_STEP = 7;
    private static final int BAR_WIDTH = 6;
    private static final int UNIT = 6;

    private final int[] radii = new int[Profile.LAYERS];
    private ItemStack shownTemplate = ItemStack.EMPTY;
    private boolean dragging;
    private Button save;

    public TemplateTableScreen(TemplateTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 190;
        inventoryLabelY = imageHeight - 94;
        Arrays.fill(radii, Profile.MAX_RADIUS);
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + 76;
        save = addRenderableWidget(Button.builder(Component.translatable("tracer.template_table.save"), b -> save())
                .bounds(leftPos + 30, y, 46, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("tracer.template_table.clear"), b -> Arrays.fill(radii, Profile.MAX_RADIUS))
                .bounds(leftPos + 78, y, 44, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("tracer.template_table.mirror"), b -> mirror())
                .bounds(leftPos + 124, y, 46, 18).build());
    }

    private Profile profile() {
        return Profile.of(radii);
    }

    private void save() {
        PacketDistributor.sendToServer(new SaveProfile(menu.containerId, profile()));
    }

    private void mirror() {
        load(profile().mirrored());
    }

    private void load(Profile profile) {
        for (int i = 0; i < Profile.LAYERS; i++)
            radii[i] = profile.radius(i);
    }

    // a written template dropped in the slot loads into the bars
    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack template = menu.template();
        save.active = !template.isEmpty();
        if (ItemStack.isSameItemSameComponents(template, shownTemplate))
            return;
        shownTemplate = template.copy();
        Profile written = template.get(Tracer.PROFILE.get());
        if (written != null)
            load(written);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && setBar(mouseX, mouseY)) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            setBar(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean setBar(double mouseX, double mouseY) {
        double x = mouseX - leftPos - BARS_X;
        double y = topPos + BARS_BOTTOM - mouseY;
        if (!dragging && (x < 0 || x >= Profile.LAYERS * BAR_STEP || y < -2 || y > Profile.MAX_RADIUS * UNIT + 2))
            return false;
        int bar = Mth.clamp((int) (x / BAR_STEP), 0, Profile.LAYERS - 1);
        radii[bar] = Mth.clamp(Mth.ceil(y / UNIT), 1, Profile.MAX_RADIUS);
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int i = 0; i < Profile.LAYERS; i++) {
            int h = radii[i] * UNIT;
            graphics.blit(TEXTURE, leftPos + BARS_X + i * BAR_STEP, topPos + BARS_BOTTOM - h, 176, 0, BAR_WIDTH, h);
        }
        renderPreview(graphics, partialTick);
    }

    private void renderPreview(GuiGraphics graphics, float partialTick) {
        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(leftPos + 147, topPos + 44, 150);
        ms.scale(30, -30, 30);
        ms.mulPose(Axis.XP.rotationDegrees(20));
        ms.mulPose(Axis.YP.rotationDegrees((minecraft.player.tickCount + partialTick) * 2));
        ms.translate(-.5, -.5, -.5);
        Lighting.setupFor3DItems();
        VertexConsumer vc = graphics.bufferSource().getBuffer(Sheets.cutoutBlockSheet());
        for (BakedQuad quad : TurnedQuads.all(profile(), PREVIEW_BLOCK))
            vc.putBulkData(ms.last(), quad, 1, 1, 1, 1, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        graphics.flush();
        ms.popPose();
    }
}
