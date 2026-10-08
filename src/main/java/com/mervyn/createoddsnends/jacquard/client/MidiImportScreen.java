package com.mervyn.createoddsnends.jacquard.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

// Pick how a MIDI file lands on the grid. The summary line reruns the conversion on every change.
final class MidiImportScreen extends Screen {
    private static final int WIDTH = 300, HEIGHT = 240, ROWS = 6, ROW_H = 16;

    private final CardPunchScreen parent;
    private final MidiImport midi;
    private MidiImport.Result result;
    private int scroll;

    MidiImportScreen(CardPunchScreen parent, MidiImport midi) {
        super(Component.translatable("createoddsnends.score.import.title"));
        this.parent = parent;
        this.midi = midi;
        result = midi.convert();
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return (height - HEIGHT) / 2;
    }

    private void button(int x, int y, int w, Component text, Runnable action, boolean active) {
        Button b = Button.builder(text, p -> {
            action.run();
            result = midi.convert();
            rebuildWidgets();
        }).bounds(x, y, w, 14).build();
        b.active = active;
        addRenderableWidget(b);
    }

    private static Component literal(String s) {
        return Component.literal(s);
    }

    @Override
    protected void init() {
        int x = left() + 8, y = top() + 34;
        int gridX = x + 90;
        for (int value : new int[] {2, 4, 8}) {
            button(gridX, y, 22, CardPunchScreen.marked(Component.literal(Integer.toString(value)), midi.rowsPerBeat == value), () -> midi.rowsPerBeat = value, true);
            gridX += 24;
        }
        y += 17;
        button(x + 90, y, 16, literal("-"), () -> midi.startBar = Math.max(1, midi.startBar - 1), midi.startBar > 1);
        button(x + 136, y, 16, literal("+"), () -> midi.startBar++, true);
        y += 17;
        button(x + 90, y, 22, literal("-12"), () -> midi.transpose = Mth.clamp(midi.transpose - 12, -48, 48), true);
        button(x + 113, y, 16, literal("-1"), () -> midi.transpose = Mth.clamp(midi.transpose - 1, -48, 48), true);
        button(x + 163, y, 16, literal("+1"), () -> midi.transpose = Mth.clamp(midi.transpose + 1, -48, 48), true);
        button(x + 180, y, 22, literal("+12"), () -> midi.transpose = Mth.clamp(midi.transpose + 12, -48, 48), true);
        button(x + 206, y, 78, Component.translatable("createoddsnends.score.import.auto_fit"), () -> midi.transpose = midi.autoFit(), true);

        int rowY = top() + 100;
        for (int i = scroll; i < Math.min(scroll + ROWS, midi.groups.size()); i++) {
            MidiImport.Group g = midi.groups.get(i);
            Button b = Button.builder(voiceName(g.voice), p -> {
                g.voice = g.voice == 15 ? MidiImport.SKIP : g.voice + 1;
                result = midi.convert();
                rebuildWidgets();
            }).bounds(x + 190, rowY, 94, 14).build();
            if (g.voice != MidiImport.SKIP)
                b.setTooltip(Tooltip.create(Component.translatable(ScorePanel.timbreKey(g.voice))));
            addRenderableWidget(b);
            rowY += ROW_H;
        }

        int bottom = top() + HEIGHT - 22;
        addRenderableWidget(Button.builder(Component.translatable("createoddsnends.score.import.confirm"), p -> {
            parent.finishImport(result);
            minecraft.setScreen(parent);
        }).bounds(left() + WIDTH - 148, bottom, 68, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), p -> onClose()).bounds(left() + WIDTH - 76, bottom, 68, 16).build());
    }

    private static Component voiceName(int voice) {
        return voice == MidiImport.SKIP ? Component.translatable("createoddsnends.score.import.skip")
                : Component.translatable("color.minecraft." + DyeColor.byId(voice).getName());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, midi.groups.size() - ROWS);
        int next = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, max);
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left() - 4, top() - 4, left() + WIDTH + 4, top() + HEIGHT + 4, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = left() + 8, y = top();
        graphics.drawString(font, title, x, y + 6, 0xFFFFFF, false);
        int bpm = Math.round(60_000_000f / midi.file.microsPerBeat);
        graphics.drawString(font, Component.translatable("createoddsnends.score.import.info", bpm, midi.file.beats, midi.file.beatUnit, midi.barLength()), x, y + 20, 0xC0C0C0, false);
        graphics.drawString(font, Component.translatable("createoddsnends.score.import.grid"), x, y + 37, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("createoddsnends.score.import.start_bar"), x, y + 54, 0xFFFFFF, false);
        graphics.drawCenteredString(font, Integer.toString(midi.startBar), x + 121, y + 54, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("createoddsnends.score.import.transpose"), x, y + 71, 0xFFFFFF, false);
        graphics.drawCenteredString(font, Integer.toString(midi.transpose), x + 146, y + 71, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("createoddsnends.score.import.tracks"), x, y + 89, 0xFFFFFF, false);

        int rowY = y + 103;
        for (int i = scroll; i < Math.min(scroll + ROWS, midi.groups.size()); i++) {
            MidiImport.Group g = midi.groups.get(i);
            String name = font.plainSubstrByWidth(g.name, 74);
            Component line = Component.translatable(g.drums() ? "createoddsnends.score.import.drum_row" : "createoddsnends.score.import.row", g.track + 1, g.channel + 1, g.count);
            graphics.drawString(font, line, x, rowY, 0xE0E0E0, false);
            graphics.drawString(font, name, x + 108, rowY, 0xA0A0A0, false);
            rowY += ROW_H;
        }

        int summary = y + 100 + ROWS * ROW_H + 2;
        Component kept = Component.translatable("createoddsnends.score.import.kept", result.notes().size(), result.barsKept(), result.barsInFile());
        graphics.drawString(font, kept, x, summary, result.barsKept() < result.barsInFile() ? 0xFFC040 : 0xE0E0E0, false);
        if (result.droppedDrums() > 0 || result.overflow() > 0)
            graphics.drawString(font, Component.translatable("createoddsnends.score.import.dropped", result.droppedDrums(), result.overflow()), x, summary + 11, 0xFFC040, false);
    }
}
