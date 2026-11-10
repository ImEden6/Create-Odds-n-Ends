package com.mervyn.createoddsnends.jacquard.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.jacquard.CardPunchMenu;
import com.mervyn.createoddsnends.jacquard.Design;
import com.mervyn.createoddsnends.jacquard.DesignGen;
import com.mervyn.createoddsnends.jacquard.DesignSync;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.mervyn.createoddsnends.jacquard.SaveDesign;
import com.mervyn.createoddsnends.organ.Score;
import com.mervyn.createoddsnends.organ.ScoreSync;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

// The card punch's menu doubles as the design editor: canvas on the left, tools and dyes in the
// middle, the woven preview and the punch's slots on the right.
public class CardPunchScreen extends AbstractContainerScreen<CardPunchMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "textures/gui/card_punch.png");
    private static final ResourceLocation SCORE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "textures/gui/card_punch_score.png");
    private static final long MAX_MIDI_BYTES = 4 * 1024 * 1024;
    private static final int TEX_W = 292, TEX_H = 249;
    private static final WidgetSprites BUTTON = new WidgetSprites(ResourceLocation.withDefaultNamespace("widget/button"),
            ResourceLocation.withDefaultNamespace("widget/button_disabled"), ResourceLocation.withDefaultNamespace("widget/button_highlighted"));

    private static final int CANVAS_X = 9, CANVAS_Y = 19, CANVAS_SIZE = 128;
    private static final int TOOLS_X = 143, TOOLS_Y = 17;
    private static final int PALETTE_X = 143, PALETTE_Y = 94, SWATCH = 13;
    private static final int PREVIEW_X = 221, PREVIEW_Y = 18, PREVIEW_SIZE = 64;

    private enum Tool {
        PENCIL, LINE, RECT, RECT_FILLED, FILL, PICKER
    }

    private final DesignCanvas canvas = new DesignCanvas();
    private Tool tool = Tool.PENCIL;
    private int primary = DyeColor.BLACK.getId(), secondary = DyeColor.WHITE.getId();
    // the button held down on the canvas (0 or 1, -1 for none), where it went down and where it was last
    private int drawing = -1, startX, startY, lastX, lastY;

    @Nullable
    private DynamicTexture preview;
    @Nullable
    private ResourceLocation previewLocation;
    private boolean previewDirty = true;
    private IconButton load;
    private final List<AbstractWidget> designWidgets = new ArrayList<>();
    private final ScorePanel score = new ScorePanel(this);
    private boolean scoreMode;
    private Button designTab, scoreTab;

    public CardPunchScreen(CardPunchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 292;
        imageHeight = 249;
        inventoryLabelX = CardPunchMenu.INVENTORY_X;
        inventoryLabelY = CardPunchMenu.INVENTORY_Y - 11;
        int pending = menu.punch.pending();
        Design design = pending >= 0 && !menu.punch.pendingIsScore() ? DesignSync.client(pending) : null;
        if (design != null)
            canvas.load(design);
        else
            canvas.clear(secondary);
        Score saved = menu.punch.pendingIsScore() ? ScoreSync.client(pending) : null;
        if (saved != null)
            score.load(saved);
    }

    @Override
    protected void init() {
        super.init();
        designWidgets.clear();
        designTab = addRenderableWidget(Button.builder(Component.translatable("createoddsnends.editor.mode.design"), b -> setScoreMode(false))
                .bounds(leftPos + 80, topPos + 3, 44, 12).build());
        scoreTab = addRenderableWidget(Button.builder(Component.translatable("createoddsnends.editor.mode.score"), b -> setScoreMode(true))
                .bounds(leftPos + 126, topPos + 3, 44, 12).build());
        int i = 0;
        for (Tool t : Tool.values())
            tile(i++, t.name().toLowerCase(), () -> tool = t, () -> tool == t);
        tile(i++, "mirror_x", () -> canvas.mirrorX = !canvas.mirrorX, () -> canvas.mirrorX);
        tile(i++, "mirror_y", () -> canvas.mirrorY = !canvas.mirrorY, () -> canvas.mirrorY);
        tile(i++, "flip_x", () -> edit(canvas::flipX), null);
        tile(i++, "flip_y", () -> edit(canvas::flipY), null);
        tile(i++, "undo", () -> changed(drawing < 0 && canvas.undo()), null);
        tile(i++, "redo", () -> changed(drawing < 0 && canvas.redo()), null);
        tile(i++, "clear", () -> edit(() -> canvas.clear(secondary)), null);
        load = tile(i++, "load", this::loadSource, null);
        tile(i, "save", this::save, null);

        sizeButtons(88, true);
        sizeButtons(101, false);
        score.init(leftPos, topPos, this::addRenderableWidget);
        previewDirty = true;
        applyMode();
    }

    private void setScoreMode(boolean on) {
        if (drawing >= 0 || score.busy())
            return;
        scoreMode = on;
        if (!on)
            score.stopPreview();
        applyMode();
    }

    private void applyMode() {
        for (AbstractWidget w : designWidgets)
            w.visible = !scoreMode;
        score.setShown(scoreMode);
        designTab.setMessage(marked(Component.translatable("createoddsnends.editor.mode.design"), !scoreMode));
        scoreTab.setMessage(marked(Component.translatable("createoddsnends.editor.mode.score"), scoreMode));
    }

    static Component marked(MutableComponent text, boolean current) {
        return current ? text.withStyle(ChatFormatting.YELLOW) : text;
    }

    void finishImport(MidiImport.Result result) {
        score.imported(result);
    }

    void openMidi(Path path) {
        try {
            if (Files.size(path) > MAX_MIDI_BYTES)
                throw new IOException("the file is too big");
            minecraft.setScreen(new MidiImportScreen(this, new MidiImport(MidiFile.parse(Files.readAllBytes(path)))));
        } catch (IOException | RuntimeException e) {
            score.failed(Component.translatable("createoddsnends.score.import.failed", e.getMessage()));
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        for (Path path : paths) {
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".mid") || name.endsWith(".midi")) {
                setScoreMode(true);
                openMidi(path);
                return;
            }
        }
    }

    private IconButton tile(int index, String name, Runnable action, @Nullable BooleanSupplier selected) {
        IconButton button = addRenderableWidget(new IconButton(leftPos + TOOLS_X + index % 4 * 19, topPos + TOOLS_Y + index / 4 * 19, name, action, selected));
        designWidgets.add(button);
        return button;
    }

    private void sizeButtons(int y, boolean width) {
        for (int step : new int[] {-1, 1}) {
            Button b = Button.builder(Component.literal(step < 0 ? "-" : "+"), button -> {
                int w = canvas.width + (width ? step : 0), h = canvas.height + (width ? 0 : step);
                if (w >= 1 && w <= Design.MAX_WIDTH && h >= 1 && h <= Design.MAX_HEIGHT)
                    edit(() -> canvas.resize(w, h, secondary));
            }).bounds(leftPos + (step < 0 ? 256 : 270), topPos + y, 12, 12).build();
            b.setTooltip(Tooltip.create(Component.translatable("createoddsnends.editor." + (width ? "width" : "height"))));
            addRenderableWidget(b);
            designWidgets.add(b);
        }
    }

    private void edit(Runnable change) {
        if (drawing >= 0)
            return;
        canvas.checkpoint();
        change.run();
        previewDirty = true;
    }

    private void changed(boolean did) {
        if (did)
            previewDirty = true;
    }

    private int sourceId() {
        ItemStack source = menu.slots.get(0).getItem();
        return source.getOrDefault(Jacquard.DESIGN.get(), -1);
    }

    private void loadSource() {
        int id = sourceId();
        Design design = id < 0 ? null : DesignSync.client(id);
        if (design != null)
            edit(() -> canvas.load(design));
    }

    private void save() {
        Design design = canvas.toDesign();
        PacketDistributor.sendToServer(new SaveDesign(menu.containerId, design.width(), design.height(), design.pixels()));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        score.tick();
        int id = sourceId();
        load.active = id >= 0 && DesignSync.client(id) != null;
        if (previewDirty) {
            previewDirty = false;
            NativeImage painted = DesignTextures.paint(canvas.toDesign());
            if (preview != null && preview.getPixels().getWidth() == painted.getWidth() && preview.getPixels().getHeight() == painted.getHeight()) {
                preview.getPixels().copyFrom(painted);
                painted.close();
                preview.upload();
            } else {
                releasePreview();
                preview = new DynamicTexture(painted);
                previewLocation = Minecraft.getInstance().getTextureManager().register("createoddsnends_editor", preview);
            }
        }
    }

    private void releasePreview() {
        if (previewLocation != null)
            Minecraft.getInstance().getTextureManager().release(previewLocation);
        previewLocation = null;
        preview = null;
    }

    @Override
    public void removed() {
        super.removed();
        score.stopPreview();
        releasePreview();
    }

    private int scale() {
        return Math.max(1, Math.min(CANVAS_SIZE / canvas.pw(), CANVAS_SIZE / canvas.ph()));
    }

    private int canvasLeft() {
        return leftPos + CANVAS_X + (CANVAS_SIZE - canvas.pw() * scale()) / 2;
    }

    private int canvasTop() {
        return topPos + CANVAS_Y + (CANVAS_SIZE - canvas.ph() * scale()) / 2;
    }

    private int pixelX(double mouseX) {
        return Mth.floor((mouseX - canvasLeft()) / scale());
    }

    private int pixelY(double mouseY) {
        return Mth.floor((mouseY - canvasTop()) / scale());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scoreMode)
            return score.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        int x = pixelX(mouseX), y = pixelY(mouseY);
        if ((button == 0 || button == 1) && drawing < 0 && canvas.inside(x, y)) {
            press(x, y, button);
            return true;
        }
        int swatch = swatchAt(mouseX, mouseY);
        if (swatch >= 0 && (button == 0 || button == 1)) {
            if (button == 0)
                primary = swatch;
            else
                secondary = swatch;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void press(int x, int y, int button) {
        int dye = button == 0 ? primary : secondary;
        if (tool == Tool.PICKER) {
            if (button == 0)
                primary = canvas.get(x, y);
            else
                secondary = canvas.get(x, y);
            return;
        }
        canvas.checkpoint();
        previewDirty = true;
        if (tool == Tool.FILL) {
            canvas.fill(x, y, dye);
            return;
        }
        drawing = button;
        startX = lastX = x;
        startY = lastY = y;
        if (tool == Tool.PENCIL)
            canvas.plot(x, y, dye);
        else
            canvas.beginShape();
        shape(x, y);
    }

    private void shape(int x, int y) {
        int dye = drawing == 0 ? primary : secondary;
        switch (tool) {
            case PENCIL -> canvas.stroke(lastX, lastY, x, y, dye);
            case LINE -> canvas.line(startX, startY, x, y, dye);
            case RECT -> canvas.rect(startX, startY, x, y, dye, false);
            case RECT_FILLED -> canvas.rect(startX, startY, x, y, dye, true);
            default -> {}
        }
        lastX = x;
        lastY = y;
        previewDirty = true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scoreMode && score.mouseDragged(mouseX, mouseY))
            return true;
        if (drawing >= 0) {
            int x = Mth.clamp(pixelX(mouseX), 0, canvas.pw() - 1), y = Mth.clamp(pixelY(mouseY), 0, canvas.ph() - 1);
            if (x != lastX || y != lastY)
                shape(x, y);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scoreMode && score.mouseReleased())
            return true;
        if (drawing >= 0 && button == drawing) {
            drawing = -1;
            canvas.endShape();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scoreMode && score.mouseScrolled(mouseX, mouseY, scrollY))
            return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (scoreMode)
            return score.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
        if (hasControlDown() && drawing < 0) {
            if (keyCode == GLFW.GLFW_KEY_Z) {
                changed(hasShiftDown() ? canvas.redo() : canvas.undo());
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_Y) {
                changed(canvas.redo());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int swatchAt(double mouseX, double mouseY) {
        int col = Mth.floor((mouseX - leftPos - PALETTE_X) / SWATCH), row = Mth.floor((mouseY - topPos - PALETTE_Y) / SWATCH);
        return col >= 0 && col < 4 && row >= 0 && row < 4 ? row * 4 + col : -1;
    }

    static int argb(int dye) {
        int[] rgb = DesignGen.DYE_RGB[dye];
        return 0xFF000000 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 3) {
            String key = switch (hoveredSlot.index) {
                case 0 -> "source";
                case 1 -> "blanks";
                default -> "output";
            };
            graphics.renderTooltip(font, Component.translatable("createoddsnends.editor.slot." + key), x, y);
            return;
        }
        Component scoreTip = scoreMode ? score.tooltip(x, y) : null;
        if (scoreTip != null) {
            graphics.renderTooltip(font, scoreTip, x, y);
            return;
        }
        int swatch = scoreMode ? -1 : swatchAt(x, y);
        if (swatch >= 0) {
            graphics.renderTooltip(font, Component.translatable("color.minecraft." + DyeColor.byId(swatch).getName()), x, y);
            return;
        }
        super.renderTooltip(graphics, x, y);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (scoreMode) {
            graphics.blit(SCORE_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
            score.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        renderCanvas(graphics, mouseX, mouseY);

        for (int i = 0; i < 16; i++) {
            int x = leftPos + PALETTE_X + i % 4 * SWATCH, y = topPos + PALETTE_Y + i / 4 * SWATCH;
            graphics.fill(x + 1, y + 1, x + SWATCH - 1, y + SWATCH - 1, argb(i));
            if (i == primary)
                outline(graphics, x, y, SWATCH, SWATCH, 0xFFFFFFFF);
            else if (i == secondary)
                outline(graphics, x, y, SWATCH, SWATCH, 0xFF404040);
        }
        int cx = leftPos + PALETTE_X + 4 * SWATCH + 4;
        graphics.fill(cx + 6, topPos + PALETTE_Y + 6, cx + 18, topPos + PALETTE_Y + 18, argb(secondary));
        outline(graphics, cx + 5, topPos + PALETTE_Y + 5, 14, 14, 0xFF404040);
        graphics.fill(cx, topPos + PALETTE_Y, cx + 12, topPos + PALETTE_Y + 12, argb(primary));
        outline(graphics, cx - 1, topPos + PALETTE_Y - 1, 14, 14, 0xFFFFFFFF);

        if (previewLocation != null) {
            int tw = canvas.pw(), th = canvas.ph();
            float s = Math.min((float) PREVIEW_SIZE / canvas.pw(), (float) PREVIEW_SIZE / canvas.ph());
            int w = Math.round(canvas.pw() * s), h = Math.round(canvas.ph() * s);
            graphics.blit(previewLocation, leftPos + PREVIEW_X + (PREVIEW_SIZE - w) / 2, topPos + PREVIEW_Y + (PREVIEW_SIZE - h) / 2, w, h, 0, 0, tw, th, tw, th);
        }
    }

    private void renderCanvas(GuiGraphics graphics, int mouseX, int mouseY) {
        int s = scale(), left = canvasLeft(), top = canvasTop(), pw = canvas.pw(), ph = canvas.ph();
        for (int y = 0; y < ph; y++)
            for (int x = 0; x < pw; x++)
                graphics.fill(left + x * s, top + y * s, left + x * s + s, top + y * s + s, argb(canvas.get(x, y)));
        if (s >= 6) {
            for (int x = 1; x < pw; x++)
                graphics.fill(left + x * s, top, left + x * s + 1, top + ph * s, 0x18000000);
            for (int y = 1; y < ph; y++)
                graphics.fill(left, top + y * s, left + pw * s, top + y * s + 1, 0x18000000);
        }
        for (int x = 16; x < pw; x += 16)
            graphics.fill(left + x * s, top, left + x * s + 1, top + ph * s, 0x90000000);
        for (int y = 16; y < ph; y += 16)
            graphics.fill(left, top + y * s, left + pw * s, top + y * s + 1, 0x90000000);
        int hx = pixelX(mouseX), hy = pixelY(mouseY);
        if (canvas.inside(hx, hy) && drawing < 0)
            outline(graphics, left + hx * s, top + hy * s, s, s, 0xC0FFFFFF);
    }

    static void outline(GuiGraphics graphics, int x, int y, int w, int h, int argb) {
        graphics.fill(x, y, x + w, y + 1, argb);
        graphics.fill(x, y + h - 1, x + w, y + h, argb);
        graphics.fill(x, y, x + 1, y + h, argb);
        graphics.fill(x + w - 1, y, x + w, y + h, argb);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (scoreMode) {
            score.labels(graphics, font);
        } else {
            graphics.drawString(font, Component.translatable("createoddsnends.editor.size_w", canvas.width), 221, 90, 0x404040, false);
            graphics.drawString(font, Component.translatable("createoddsnends.editor.size_h", canvas.height), 221, 103, 0x404040, false);
        }
        int pending = menu.punch.pending();
        if (pending >= 0) {
            Component text = Component.translatable(menu.punch.pendingIsScore() ? "createoddsnends.score.pending" : "createoddsnends.tooltip.design", pending);
            graphics.drawString(font, text, imageWidth - 8 - font.width(text), titleLabelY, 0x404040, false);
        }
    }

    static class IconButton extends AbstractButton {
        private final ResourceLocation icon;
        private final Runnable action;
        @Nullable
        private final BooleanSupplier selected;

        IconButton(int x, int y, String name, Runnable action, @Nullable BooleanSupplier selected) {
            super(x, y, 18, 18, Component.translatable("createoddsnends.editor." + name));
            icon = ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "textures/gui/punch/" + name + ".png");
            this.action = action;
            this.selected = selected;
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean on = selected != null && selected.getAsBoolean();
            graphics.blitSprite(BUTTON.get(active, on || isHoveredOrFocused()), getX(), getY(), width, height);
            if (on)
                outline(graphics, getX(), getY(), width, height, 0xFFFFFFFF);
            graphics.setColor(1, 1, 1, active ? 1 : 0.4f);
            graphics.blit(icon, getX() + 1, getY() + 1, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
