package com.escapencu.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game developer command console (Dev Mode only).
 *
 * Keys:
 *   Enter    — open / submit
 *   Escape   — cancel
 *   Backspace— delete last character
 *   Tab      — cycle through completions (GameScene calls applyTab)
 *
 * API:
 *   console.isOpen()                      — input bar visible?
 *   console.getBuffer()                   — current typed text
 *   console.typeChar(ch)                  — append a printable character
 *   console.backspace()                   — delete last character
 *   console.submit()                      — return command and close
 *   console.applyTab(options, prefix)     — apply Tab completion
 *   console.resetCompletions()            — clear completion state
 *   console.showOutput(text)              — display result message
 *   console.update(dt)                    — tick output timer
 *   console.draw(gc, W, H)               — render (call after HUD)
 */
public class CommandConsole {

    private boolean            open        = false;
    private final StringBuilder buffer      = new StringBuilder();
    private String             outputText  = null;
    private double             outputTimer = 0;

    // ── Tab completion state ───────────────────────────────────────────────
    private List<String> completions      = new ArrayList<>();
    private int          completionIdx    = 0;
    private String       completionPrefix = ""; // fixed part before the cycling token

    private static final int    MAX_VISIBLE  = 8;   // max rows shown in popup
    private static final double ITEM_H       = 22;
    private static final double POPUP_W      = 300;

    // ── State ──────────────────────────────────────────────────────────────
    public boolean isOpen()    { return open; }
    public String  getBuffer() { return buffer.toString(); }

    public void open() {
        open = true;
        buffer.setLength(0);
        resetCompletions();
    }

    public void close() {
        open = false;
        buffer.setLength(0);
        resetCompletions();
    }

    /** Returns the typed command string and closes the console. Returns null if empty. */
    public String submit() {
        String cmd = buffer.toString().trim();
        close();
        return cmd.isEmpty() ? null : cmd;
    }

    // ── Input ──────────────────────────────────────────────────────────────
    public void typeChar(String ch) {
        if (!open) return;
        if (ch == null || ch.isEmpty()) return;
        char c = ch.charAt(0);
        if (c < 32 || c == 127) return; // ignore control characters
        buffer.append(c);
        resetCompletions(); // user typed → restart completion cycle
    }

    public void backspace() {
        if (open && !buffer.isEmpty()) {
            buffer.deleteCharAt(buffer.length() - 1);
            resetCompletions();
        }
    }

    // ── Tab completion ─────────────────────────────────────────────────────

    /**
     * Start a new completion cycle with the given options.
     * Always resets to the first item — call this only when starting fresh
     * (i.e. the user has not yet begun cycling).
     *
     * @param options  Completable strings for the current token.
     * @param prefix   Fixed part kept in the buffer (e.g. "/summon ").
     */
    public void applyTab(List<String> options, String prefix) {
        if (options.isEmpty()) return;
        completions      = new ArrayList<>(options);
        completionIdx    = 0;
        completionPrefix = prefix;
        writeCompletion();
    }

    /**
     * Advance to the next option within the current cycle.
     * Call this when the user presses Tab again without typing anything.
     */
    public void advanceCycle() {
        if (completions.isEmpty()) return;
        completionIdx = (completionIdx + 1) % completions.size();
        writeCompletion();
    }

    /** True while a completion cycle is active (i.e. Tab was pressed at least once
     *  since the last typeChar / backspace / close). */
    public boolean isInCompletionCycle() { return !completions.isEmpty(); }

    private void writeCompletion() {
        buffer.setLength(0);
        buffer.append(completionPrefix);
        buffer.append(completions.get(completionIdx));
    }

    /** Clear completion state (called automatically on typeChar / backspace). */
    public void resetCompletions() {
        completions.clear();
        completionIdx    = 0;
        completionPrefix = "";
    }

    // ── Output ─────────────────────────────────────────────────────────────
    public void showOutput(String text)                  { showOutput(text, 4.0); }
    public void showOutput(String text, double duration) { outputText = text; outputTimer = duration; }

    // ── Update ─────────────────────────────────────────────────────────────
    public void update(double dt) {
        if (outputTimer > 0 && (outputTimer -= dt) <= 0) outputText = null;
    }

    // ── Draw (screen coordinates, no camera transform) ─────────────────────
    public void draw(GraphicsContext gc, double W, double H) {

        // ── Output message ─────────────────────────────────────────────────
        if (outputText != null) {
            String[] lines = outputText.split("\n");
            double boxH = 10 + lines.length * 20;
            double by   = H - (open ? 40 : 4) - boxH;
            gc.setFill(Color.color(0.0, 0.0, 0.0, 0.72));
            gc.fillRect(0, by, W, boxH);
            gc.setFill(Color.LIMEGREEN);
            for (int i = 0; i < lines.length; i++)
                gc.fillText(lines[i], 10, by + 16 + i * 20);
        }

        // ── Tab completion popup ───────────────────────────────────────────
        if (open && !completions.isEmpty()) {
            // Sliding window: keep selected item visible
            int total   = completions.size();
            int visible = Math.min(total, MAX_VISIBLE);
            // Window start: centre the selection, clamp to valid range
            int windowStart = Math.max(0, Math.min(completionIdx - MAX_VISIBLE / 2,
                                                    total - visible));

            double popupH = visible * ITEM_H + 6;
            double popupY = H - 36 - popupH - 4;

            // Background panel
            gc.setFill(Color.color(0.06, 0.06, 0.12, 0.92));
            gc.fillRect(0, popupY, POPUP_W, popupH);
            gc.setStroke(Color.color(0.35, 0.35, 0.5, 1.0));
            gc.setLineWidth(1);
            gc.strokeRect(0, popupY, POPUP_W, popupH);

            for (int row = 0; row < visible; row++) {
                int    realIdx  = windowStart + row;
                boolean selected = (realIdx == completionIdx);
                double  iy      = popupY + 3 + row * ITEM_H;

                if (selected) {
                    gc.setFill(Color.color(0.12, 0.45, 0.18, 0.95));
                    gc.fillRect(1, iy, POPUP_W - 2, ITEM_H);
                }

                gc.setFill(selected ? Color.WHITE : Color.color(0.78, 0.78, 0.78, 1.0));
                gc.fillText(completions.get(realIdx), 12, iy + 15);
            }

            // Scroll hint when list is longer than the visible window
            if (total > MAX_VISIBLE) {
                gc.setFill(Color.color(0.55, 0.55, 0.55, 0.9));
                gc.fillText(String.format("Tab ↕   %d / %d", completionIdx + 1, total),
                        POPUP_W - 110, popupY + popupH - 5);
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────
        if (open) {
            gc.setFill(Color.color(0.0, 0.0, 0.0, 0.82));
            gc.fillRect(0, H - 36, W, 36);
            gc.setFill(Color.WHITE);
            gc.fillText("> " + buffer + "█", 10, H - 12);
        }
    }
}
