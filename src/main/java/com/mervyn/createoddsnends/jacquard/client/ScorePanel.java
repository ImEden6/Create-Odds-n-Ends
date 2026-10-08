package com.mervyn.createoddsnends.jacquard.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.mervyn.createoddsnends.organ.SaveScore;
import com.mervyn.createoddsnends.organ.Score;
import com.mervyn.createoddsnends.organ.Score.Note;
import com.mervyn.createoddsnends.organ.ScoreSync;
import com.mervyn.createoddsnends.organ.Timbre;
import com.mervyn.createoddsnends.organ.client.OrganNoteSound;
import com.mervyn.createoddsnends.organ.client.OrganPlayback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;

// The card punch's Score mode: the piano roll and everything around it. CardPunchScreen shows this
// instead of the design tools and hands it the mouse and keyboard.
final class ScorePanel {
    static final int MIN_RPM = 16, MAX_RPM = 256;
    private static final int LABEL_X = 8, ROLL_X = 32, ROLL_Y = 18, ROLL_W = 181, ROLL_H = 127;
    private static final int VBAR_X = 214, VBAR_W = 4, HBAR_Y = 148, HBAR_H = 4;
    private static final int PALETTE_X = 233, PALETTE_Y = 167, SWATCH = 13;
    private static final int RIGHT_X = 221, TOOLS_X = 8, TOOLS_Y = 166;
    private static final int[] ROW_WIDTHS = {1, 2, 3, 4, 6, 8, 12, 16}, LANE_HEIGHTS = {3, 4, 6, 8};
    private static final String[] NAMES = {"C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"};
    private static final int STATUS_TICKS = 80, SAVE_TICKS = 100;

    private enum Tool {
        DRAW, ERASE, SELECT
    }

    private enum Drag {
        NONE, DRAW, ERASE, BOX, MOVE, HBAR, VBAR
    }

    private final CardPunchScreen screen;
    final ScoreCanvas canvas = new ScoreCanvas();
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private Consumer<AbstractWidget> sink;
    private int left, top;
    private boolean shown;

    private Tool tool = Tool.DRAW;
    private int voice = DyeColor.RED.getId();
    private int rowZoom = 3, laneZoom = 2;
    private int scrollX, scrollY = 100;
    private int cursorRow;

    private Drag drag = Drag.NONE;
    private int startRow, startCol, nowRow, nowCol, lastRow, lastCol;
    private int minRow, maxEnd, minCol, maxCol;

    @Nullable
    private Score playing;
    private float playRow;
    private int playLast;
    private int rpm = 96;
    private final List<OrganNoteSound> held = new ArrayList<>();

    @Nullable
    private Score sent;
    private int confirming;

    private boolean picking;
    @Nullable
    private Component status;
    private int statusTicks;

    private CardPunchScreen.IconButton play, stop;
    private Button rowsPerBar12, rowsPerBar16;

    ScorePanel(CardPunchScreen screen) {
        this.screen = screen;
    }

    static String timbreKey(int voice) {
        return "createoddsnends.organ.timbre." + switch (Timbre.of(voice, 2)) {
            case MELODY -> "melody";
            case BASS -> "bass";
            case CHORD -> "chord";
            default -> "drums";
        };
    }

    private int rowW() {
        return ROW_WIDTHS[rowZoom];
    }

    private int laneH() {
        return LANE_HEIGHTS[laneZoom];
    }

    private <T extends AbstractWidget> T add(T widget) {
        widgets.add(widget);
        sink.accept(widget);
        return widget;
    }

    private CardPunchScreen.IconButton tile(int x, int y, String name, Runnable action, @Nullable java.util.function.BooleanSupplier selected) {
        return add(new CardPunchScreen.IconButton(left + x, top + y, name, action, selected));
    }

    private Button text(int x, int y, int w, String label, Runnable action) {
        return add(Button.builder(Component.literal(label), b -> action.run()).bounds(left + x, top + y, w, 12).build());
    }

    private void stepper(int y, String tip, Runnable down, Runnable up) {
        Button minus = text(256, y, 12, "-", down), plus = text(270, y, 12, "+", up);
        minus.setTooltip(Tooltip.create(Component.translatable(tip)));
        plus.setTooltip(Tooltip.create(Component.translatable(tip)));
    }

    void init(int left, int top, Consumer<AbstractWidget> sink) {
        this.left = left;
        this.top = top;
        this.sink = sink;
        widgets.clear();

        play = tile(RIGHT_X, 18, "play", this::startPreview, null);
        stop = tile(RIGHT_X, 18, "stop", this::stopPreview, null);
        tile(RIGHT_X + 20, 18, "import", this::pickFile, null);
        tile(RIGHT_X + 40, 18, "save", this::save, null);

        stepper(40, "createoddsnends.score.rpm", () -> rpm = Math.max(MIN_RPM, rpm - 16), () -> rpm = Math.min(MAX_RPM, rpm + 16));
        stepper(54, "createoddsnends.score.bars", () -> setBars(canvas.bars - 1), () -> setBars(canvas.bars + 1));
        rowsPerBar12 = text(254, 68, 15, "12", () -> setBarLength(12));
        rowsPerBar16 = text(270, 68, 15, "16", () -> setBarLength(16));
        stepper(82, "createoddsnends.score.zoom", () -> zoomTime(-1, left + ROLL_X + ROLL_W / 2.0), () -> zoomTime(1, left + ROLL_X + ROLL_W / 2.0));
        stepper(96, "createoddsnends.score.lanes", () -> zoomLanes(-1), () -> zoomLanes(1));

        tile(TOOLS_X, TOOLS_Y, "pencil", () -> tool = Tool.DRAW, () -> tool == Tool.DRAW);
        tile(TOOLS_X + 18, TOOLS_Y, "erase", () -> tool = Tool.ERASE, () -> tool == Tool.ERASE);
        tile(TOOLS_X + 36, TOOLS_Y, "select", () -> tool = Tool.SELECT, () -> tool == Tool.SELECT);
        tile(TOOLS_X, TOOLS_Y + 18, "copy", this::copy, null);
        tile(TOOLS_X + 18, TOOLS_Y + 18, "paste", this::paste, null);
        tile(TOOLS_X + 36, TOOLS_Y + 18, "delete", this::delete, null);
        tile(TOOLS_X, TOOLS_Y + 36, "undo", this::undo, null);
        tile(TOOLS_X + 18, TOOLS_Y + 36, "redo", this::redo, null);
        tile(TOOLS_X + 36, TOOLS_Y + 36, "clear", this::clear, null);
        text(TOOLS_X, TOOLS_Y + 55, 27, "-1", () -> transpose(-1));
        text(TOOLS_X + 27, TOOLS_Y + 55, 27, "+1", () -> transpose(1));
        text(TOOLS_X, TOOLS_Y + 67, 27, "-12", () -> transpose(-12));
        text(TOOLS_X + 27, TOOLS_Y + 67, 27, "+12", () -> transpose(12));
        setShown(shown);
    }

    void setShown(boolean shown) {
        this.shown = shown;
        for (AbstractWidget w : widgets)
            w.visible = shown;
        sync();
    }

    private void sync() {
        if (play == null)
            return;
        play.visible = shown && playing == null;
        stop.visible = shown && playing != null;
        rowsPerBar12.setMessage(CardPunchScreen.marked(Component.literal("12"), canvas.barLength == 12));
        rowsPerBar16.setMessage(CardPunchScreen.marked(Component.literal("16"), canvas.barLength == 16));
    }

    private void say(Component message) {
        status = message;
        statusTicks = STATUS_TICKS;
    }

    private static Component key(String name, Object... args) {
        return Component.translatable("createoddsnends.score." + name, args);
    }

    private boolean idle() {
        return drag == Drag.NONE;
    }

    private void setBars(int bars) {
        if (idle() && bars >= 1 && bars <= canvas.maxBars() && bars != canvas.bars) {
            canvas.checkpoint();
            canvas.setBars(bars);
            clampScroll();
        }
    }

    private void setBarLength(int length) {
        if (idle() && length != canvas.barLength) {
            canvas.checkpoint();
            canvas.setBarLength(length);
            clampScroll();
            sync();
        }
    }

    private void undo() {
        if (idle() && canvas.undo())
            afterHistory();
    }

    private void redo() {
        if (idle() && canvas.redo())
            afterHistory();
    }

    private void afterHistory() {
        clampScroll();
        sync();
    }

    private void clear() {
        if (idle()) {
            canvas.checkpoint();
            canvas.clear();
        }
    }

    private void copy() {
        if (idle() && !canvas.selected.isEmpty())
            canvas.copy();
    }

    private void paste() {
        if (!idle() || !canvas.hasClipboard())
            return;
        if (!canvas.canPaste()) {
            say(key("full", ScoreCanvas.MAX_NOTES));
            return;
        }
        canvas.checkpoint();
        canvas.paste(cursorRow);
        tool = Tool.SELECT;
    }

    private void delete() {
        if (idle() && !canvas.selected.isEmpty()) {
            canvas.checkpoint();
            canvas.deleteSelected();
        }
    }

    private void transpose(int semitones) {
        if (!idle())
            return;
        if (canvas.canTranspose(semitones)) {
            canvas.checkpoint();
            canvas.transpose(semitones);
        } else if (!canvas.selected.isEmpty()) {
            say(key("no_room"));
        }
    }

    private void save() {
        if (idle()) {
            sent = canvas.toScore();
            confirming = SAVE_TICKS;
            PacketDistributor.sendToServer(SaveScore.of(screen.getMenu().containerId, sent));
            say(key("saving"));
        }
    }

    void load(Score score) {
        canvas.load(score);
        clampScroll();
        sync();
    }

    void imported(MidiImport.Result result) {
        stopPreview();
        canvas.checkpoint();
        canvas.set(result.bars(), result.barLength(), 0, result.notes());
        rpm = result.rpm();
        scrollX = 0;
        clampScroll();
        sync();
        say(key("imported", result.barsKept(), result.notes().size()));
    }

    private void pickFile() {
        if (picking)
            return;
        picking = true;
        String prompt = key("import.pick").getString();
        Thread thread = new Thread(() -> {
            String path = openDialog(prompt);
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                picking = false;
                if (path != null && mc.screen == screen)
                    screen.openMidi(java.nio.file.Path.of(path));
            });
        }, "Score MIDI picker");
        thread.setDaemon(true);
        thread.start();
    }

    @Nullable
    private static String openDialog(String title) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(2);
            filters.put(stack.UTF8("*.mid"));
            filters.put(stack.UTF8("*.midi"));
            filters.flip();
            return TinyFileDialogs.tinyfd_openFileDialog(title, null, filters, "MIDI", false);
        }
    }

    void failed(Component reason) {
        say(reason);
    }

    private void startPreview() {
        if (!idle())
            return;
        playing = canvas.toScore();
        playRow = 0;
        playLast = -1;
        sync();
    }

    void stopPreview() {
        playing = null;
        held.forEach(OrganNoteSound::release);
        held.clear();
        sync();
    }

    void tick() {
        if (statusTicks > 0 && --statusTicks == 0)
            status = null;
        if (confirming > 0) {
            int pending = screen.getMenu().punch.pending();
            if (screen.getMenu().punch.pendingIsScore() && sent.equals(ScoreSync.client(pending))) {
                confirming = 0;
                say(key("saved"));
            } else if (--confirming == 0) {
                say(key("save_failed"));
            }
        }
        if (playing == null)
            return;
        held.removeIf(OrganNoteSound::isStopped);
        float rowsPerTick = rpm / 16f / 20f, next = playRow + rowsPerTick;
        int target = Math.min(playing.rowCount() - 1, (int) next);
        List<Note> notes = playing.notes();
        for (int row = playLast + 1; row <= target; row++)
            for (int i = playing.firstAt(row); i < notes.size() && notes.get(i).row() == row; i++) {
                OrganNoteSound sound = OrganPlayback.sound(screen.getMenu().punch.getLevel(), screen.getMenu().punch.getBlockPos(), notes.get(i), rowsPerTick);
                if (sound != null)
                    held.add(sound);
            }
        playLast = Math.max(playLast, target);
        playRow = next;
        int x = (int) (playRow * rowW());
        if (x < scrollX || x >= scrollX + ROLL_W) {
            scrollX = x;
            clampScroll();
        }
        if (playRow >= playing.rowCount())
            stopPreview();
    }

    private int contentW() {
        return canvas.rows() * rowW();
    }

    private int contentH() {
        return Score.COLUMNS * laneH();
    }

    private void clampScroll() {
        scrollX = Mth.clamp(scrollX, 0, Math.max(0, contentW() - ROLL_W));
        scrollY = Mth.clamp(scrollY, 0, Math.max(0, contentH() - ROLL_H));
    }

    private void zoomTime(int step, double anchorX) {
        int next = Mth.clamp(rowZoom + step, 0, ROW_WIDTHS.length - 1);
        if (next == rowZoom)
            return;
        double px = anchorX - left - ROLL_X, row = (px + scrollX) / rowW();
        rowZoom = next;
        scrollX = (int) (row * rowW() - px);
        clampScroll();
    }

    private void zoomLanes(int step) {
        laneZoom = Mth.clamp(laneZoom + step, 0, LANE_HEIGHTS.length - 1);
        clampScroll();
    }

    private int rowAt(double mouseX) {
        return Mth.floor((mouseX - left - ROLL_X + scrollX) / rowW());
    }

    private int colAt(double mouseY) {
        return Score.COLUMNS - 1 - Mth.floor((mouseY - top - ROLL_Y + scrollY) / laneH());
    }

    private boolean inRoll(double x, double y) {
        return x >= left + ROLL_X && x < left + ROLL_X + ROLL_W && y >= top + ROLL_Y && y < top + ROLL_Y + ROLL_H;
    }

    private boolean inRange(int row, int col) {
        return row >= 0 && row < canvas.rows() && col >= 0 && col < Score.COLUMNS;
    }

    private int swatchAt(double x, double y) {
        int col = Mth.floor((x - left - PALETTE_X) / SWATCH), row = Mth.floor((y - top - PALETTE_Y) / SWATCH);
        return col >= 0 && col < 4 && row >= 0 && row < 4 ? row * 4 + col : -1;
    }

    private boolean inHBar(double x, double y) {
        return x >= left + ROLL_X && x < left + ROLL_X + ROLL_W && y >= top + HBAR_Y && y < top + HBAR_Y + HBAR_H;
    }

    private boolean inVBar(double x, double y) {
        return x >= left + VBAR_X && x < left + VBAR_X + VBAR_W && y >= top + ROLL_Y && y < top + ROLL_Y + ROLL_H;
    }

    private void dragBars(double x, double y) {
        if (drag == Drag.HBAR)
            scrollX = (int) ((x - left - ROLL_X) / ROLL_W * contentW() - ROLL_W / 2.0);
        else
            scrollY = (int) ((y - top - ROLL_Y) / ROLL_H * contentH() - ROLL_H / 2.0);
        clampScroll();
    }

    boolean mouseClicked(double x, double y, int button) {
        int swatch = swatchAt(x, y);
        if (swatch >= 0 && button == 0) {
            voice = swatch;
            return true;
        }
        if (button == 0 && (inHBar(x, y) || inVBar(x, y))) {
            drag = inHBar(x, y) ? Drag.HBAR : Drag.VBAR;
            dragBars(x, y);
            return true;
        }
        if (!inRoll(x, y) || (button != 0 && button != 1) || !idle())
            return false;
        int row = rowAt(x), col = colAt(y);
        if (!inRange(row, col))
            return true;
        cursorRow = row;
        startRow = nowRow = lastRow = row;
        startCol = nowCol = lastCol = col;
        if (tool == Tool.ERASE || (tool == Tool.DRAW && button == 1)) {
            canvas.checkpoint();
            canvas.erase(row, col);
            drag = Drag.ERASE;
        } else if (tool == Tool.DRAW) {
            if (canvas.full()) {
                say(key("full", ScoreCanvas.MAX_NOTES));
                return true;
            }
            drag = Drag.DRAW;
        } else if (button == 0) {
            pressSelect(row, col);
        }
        return true;
    }

    private void pressSelect(int row, int col) {
        Note hit = canvas.noteAt(row, col);
        if (hit == null) {
            canvas.selected.clear();
            drag = Drag.BOX;
            return;
        }
        if (!canvas.selected.contains(hit)) {
            canvas.selected.clear();
            canvas.selected.add(hit);
        }
        minRow = canvas.selected.stream().mapToInt(Note::row).min().orElse(0);
        maxEnd = canvas.selected.stream().mapToInt(n -> n.row() + n.length()).max().orElse(0);
        minCol = canvas.selected.stream().mapToInt(Note::column).min().orElse(0);
        maxCol = canvas.selected.stream().mapToInt(Note::column).max().orElse(0);
        drag = Drag.MOVE;
    }

    boolean busy() {
        return !idle();
    }

    boolean mouseDragged(double x, double y) {
        if (idle())
            return false;
        if (drag == Drag.HBAR || drag == Drag.VBAR) {
            dragBars(x, y);
            return true;
        }
        if (x >= left + ROLL_X + ROLL_W)
            scrollX += rowW();
        else if (x < left + ROLL_X)
            scrollX -= rowW();
        if (y >= top + ROLL_Y + ROLL_H)
            scrollY += laneH();
        else if (y < top + ROLL_Y)
            scrollY -= laneH();
        clampScroll();
        nowRow = Mth.clamp(rowAt(x), 0, canvas.rows() - 1);
        nowCol = Mth.clamp(colAt(y), 0, Score.COLUMNS - 1);
        if (drag == Drag.ERASE) {
            int steps = Math.max(Math.abs(nowRow - lastRow), Math.abs(nowCol - lastCol));
            for (int i = 0; i <= steps; i++) {
                float f = steps == 0 ? 1 : (float) i / steps;
                canvas.erase(Math.round(Mth.lerp(f, lastRow, nowRow)), Math.round(Mth.lerp(f, lastCol, nowCol)));
            }
            lastRow = nowRow;
            lastCol = nowCol;
        }
        return true;
    }

    boolean mouseReleased() {
        if (idle())
            return false;
        Drag finished = drag;
        drag = Drag.NONE;
        switch (finished) {
            case DRAW -> {
                canvas.checkpoint();
                canvas.place(new Note(startRow, startCol, Math.max(1, nowRow - startRow + 1), voice));
            }
            case BOX -> canvas.selectBox(startRow, startCol, nowRow, nowCol);
            case MOVE -> {
                int rows = moveRows(), cols = moveCols();
                if (rows != 0 || cols != 0) {
                    canvas.checkpoint();
                    canvas.move(rows, cols);
                }
            }
            default -> {}
        }
        return true;
    }

    private int moveRows() {
        return Mth.clamp(nowRow - startRow, -minRow, canvas.rows() - maxEnd);
    }

    private int moveCols() {
        return Mth.clamp(nowCol - startCol, -minCol, Score.COLUMNS - 1 - maxCol);
    }

    boolean mouseScrolled(double x, double y, double amount) {
        boolean overRoll = inRoll(x, y) || inHBar(x, y) || inVBar(x, y);
        if (!overRoll)
            return false;
        int step = amount > 0 ? 1 : -1;
        if (Screen.hasControlDown())
            zoomTime(step, x);
        else if (Screen.hasAltDown())
            zoomLanes(step);
        else if (Screen.hasShiftDown())
            scrollX -= step * rowW() * 4;
        else
            scrollY -= step * laneH() * 2;
        clampScroll();
        return true;
    }

    boolean keyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            delete();
            return true;
        }
        if (!Screen.hasControlDown())
            return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_Z -> {
                if (Screen.hasShiftDown())
                    redo();
                else
                    undo();
            }
            case GLFW.GLFW_KEY_Y -> redo();
            case GLFW.GLFW_KEY_C -> copy();
            case GLFW.GLFW_KEY_V -> paste();
            case GLFW.GLFW_KEY_X -> {
                copy();
                delete();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Nullable
    Component tooltip(int x, int y) {
        int swatch = swatchAt(x, y);
        if (swatch < 0)
            return null;
        return Component.translatable("createoddsnends.score.voice",
                Component.translatable("color.minecraft." + DyeColor.byId(swatch).getName()), Component.translatable(timbreKey(swatch)));
    }

    private int laneY(int col) {
        return top + ROLL_Y - scrollY + (Score.COLUMNS - 1 - col) * laneH();
    }

    private int rowX(float row) {
        return left + ROLL_X - scrollX + Math.round(row * rowW());
    }

    private static int laneColour(int col) {
        if (col < 2)
            return col == 0 ? 0xFFC9B79B : 0xFFBDAB90;
        int midi = col + Score.LOW_MIDI, pc = midi % 12;
        boolean sharp = pc == 1 || pc == 3 || pc == 6 || pc == 8 || pc == 10, odd = midi / 12 % 2 == 1;
        if (odd)
            return sharp ? 0xFFCFC2A5 : 0xFFE0D4B8;
        return sharp ? 0xFFD8CBAE : 0xFFEBDFC4;
    }

    private void drawNote(GuiGraphics g, Note n, int rowOffset, int colOffset, boolean ghost) {
        int x0 = rowX(n.row() + rowOffset), x1 = rowX(n.row() + rowOffset + n.length()), y0 = laneY(n.column() + colOffset), y1 = y0 + laneH();
        g.fill(x0, y0, x1, y1, CardPunchScreen.argb(n.voice()) & (ghost ? 0x90FFFFFF : 0xFFFFFFFF));
        if (x1 - x0 >= 3)
            g.fill(x1 - 1, y0, x1, y1, 0x70000000);
    }

    void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x0 = left + ROLL_X, y0 = top + ROLL_Y, x1 = x0 + ROLL_W, y1 = y0 + ROLL_H;
        int contentRight = Math.min(x1, rowX(canvas.rows()));

        g.enableScissor(x0, y0, x1, y1);
        g.fill(x0, y0, x1, y1, 0xFF8B8B8B);
        int firstLane = Math.max(0, Score.COLUMNS - 1 - (scrollY + ROLL_H) / laneH()), lastLane = Math.min(Score.COLUMNS - 1, Score.COLUMNS - 1 - scrollY / laneH());
        for (int col = firstLane; col <= lastLane; col++) {
            int y = laneY(col);
            g.fill(x0, y, contentRight, y + laneH(), laneColour(col));
            if (laneH() >= 6)
                g.fill(x0, y + laneH() - 1, contentRight, y + laneH(), 0x14000000);
        }
        int beat = Math.max(1, canvas.barLength / 4);
        if (rowW() * beat >= 4)
            for (int row = 0; row <= canvas.rows(); row += beat) {
                int x = rowX(row);
                if (x >= x0 && x < x1)
                    g.fill(x, y0, x + 1, y1, 0x20000000);
            }
        for (int row = 0; row <= canvas.rows(); row += canvas.barLength) {
            int x = rowX(row);
            if (x >= x0 && x < x1)
                g.fill(x, y0, x + 1, y1, 0xA0303030);
        }
        for (Note n : canvas.notes) {
            int nx0 = rowX(n.row()), nx1 = rowX(n.row() + n.length());
            if (nx1 < x0 || nx0 > x1)
                continue;
            drawNote(g, n, 0, 0, false);
            if (canvas.selected.contains(n))
                CardPunchScreen.outline(g, nx0, laneY(n.column()), Math.max(1, nx1 - nx0), laneH(), 0xFFFFFFFF);
        }
        if (drag == Drag.DRAW)
            drawNote(g, new Note(startRow, startCol, Math.max(1, nowRow - startRow + 1), voice), 0, 0, true);
        if (drag == Drag.MOVE)
            for (Note n : canvas.selected)
                drawNote(g, n, moveRows(), moveCols(), true);
        if (drag == Drag.BOX) {
            int bx0 = rowX(Math.min(startRow, nowRow)), bx1 = rowX(Math.max(startRow, nowRow) + 1);
            int by0 = laneY(Math.max(startCol, nowCol)), by1 = laneY(Math.min(startCol, nowCol)) + laneH();
            g.fill(bx0, by0, bx1, by1, 0x40FFFFFF);
            CardPunchScreen.outline(g, bx0, by0, bx1 - bx0, by1 - by0, 0xFFFFFFFF);
        }
        if (idle() && inRoll(mouseX, mouseY) && inRange(rowAt(mouseX), colAt(mouseY)))
            CardPunchScreen.outline(g, rowX(rowAt(mouseX)), laneY(colAt(mouseY)), Math.max(1, rowW()), laneH(), 0xC0FFFFFF);
        if (playing != null) {
            int x = rowX(playRow + rpm / 16f / 20f * partialTick);
            g.fill(x, y0, x + 1, y1, 0xFFE03020);
        }
        g.disableScissor();

        Font font = Minecraft.getInstance().font;
        g.enableScissor(left + LABEL_X, y0, x0, y1);
        for (int col = firstLane; col <= lastLane; col++) {
            String label = laneLabel(col);
            if (label == null)
                continue;
            g.pose().pushPose();
            g.pose().translate(left + LABEL_X + 1, laneY(col) + (laneH() - 5) / 2f, 0);
            g.pose().scale(0.6f, 0.6f, 1);
            g.drawString(font, label, 0, 0, 0xFFE0E0E0, false);
            g.pose().popPose();
        }
        g.disableScissor();

        scrollbar(g, x0, top + HBAR_Y, ROLL_W, HBAR_H, scrollX, contentW(), ROLL_W, true);
        scrollbar(g, left + VBAR_X, y0, VBAR_W, ROLL_H, scrollY, contentH(), ROLL_H, false);

        for (int i = 0; i < 16; i++) {
            int sx = left + PALETTE_X + i % 4 * SWATCH, sy = top + PALETTE_Y + i / 4 * SWATCH;
            g.fill(sx + 1, sy + 1, sx + SWATCH - 1, sy + SWATCH - 1, CardPunchScreen.argb(i));
            int[] rgb = com.mervyn.createoddsnends.jacquard.DesignGen.DYE_RGB[i];
            int ink = rgb[0] + rgb[1] + rgb[2] > 380 ? 0xFF202020 : 0xFFF0F0F0;
            g.drawString(font, Component.translatable(timbreKey(i)).getString().substring(0, 1), sx + 4, sy + 3, ink, false);
            if (i == voice)
                CardPunchScreen.outline(g, sx, sy, SWATCH, SWATCH, 0xFFFFFFFF);
        }
    }

    private String laneLabel(int col) {
        if (col == 0)
            return key("kick").getString();
        if (col == 1)
            return key("snare").getString();
        int midi = col + Score.LOW_MIDI;
        if (laneH() < 6 && midi % 12 != 0)
            return null;
        return NAMES[midi % 12] + (midi / 12 - 1);
    }

    private static void smallLabel(GuiGraphics g, Font font, Component text, int y) {
        g.pose().pushPose();
        g.pose().translate(RIGHT_X, y, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, text, 0, 0, 0x404040, false);
        g.pose().popPose();
    }

    private static void scrollbar(GuiGraphics g, int x, int y, int w, int h, int scroll, int content, int view, boolean horizontal) {
        if (content <= view)
            return;
        int track = horizontal ? w : h, size = Math.max(6, track * view / content), at = (track - size) * scroll / (content - view);
        if (horizontal)
            g.fill(x + at, y, x + at + size, y + h, 0xFFC6C6C6);
        else
            g.fill(x, y + at, x + w, y + at + size, 0xFFC6C6C6);
    }

    void labels(GuiGraphics g, Font font) {
        smallLabel(g, font, key("rpm_value", rpm), 43);
        smallLabel(g, font, key("bars_value", canvas.bars), 57);
        smallLabel(g, font, key("per_bar"), 71);
        smallLabel(g, font, key("zoom_label"), 85);
        smallLabel(g, font, key("lanes_label"), 99);
        Component line = status != null ? status : key("info", canvas.rows(), canvas.notes.size(), canvas.selected.size());
        g.drawString(font, line, 285 - font.width(line), 156, 0x404040, false);
    }
}
