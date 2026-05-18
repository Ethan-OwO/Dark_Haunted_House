package com.escapencu.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * In-game developer command console (Dev Mode only).
 * Press Enter to open / submit. Escape to cancel.
 *
 * Usage:
 *   console.isOpen()          — whether the input bar is visible
 *   console.typeChar(ch)      — append a character to the buffer
 *   console.backspace()       — delete the last character
 *   console.submit()          — return the command string and close
 *   console.showOutput(text)  — display a result message
 *   console.update(dt)        — tick the output timer
 *   console.draw(gc, W, H)    — render (call after HUD)
 */
public class CommandConsole {

    private boolean       open        = false;
    private final StringBuilder buffer = new StringBuilder();
    private String        outputText  = null;
    private double        outputTimer = 0;

    // ── State ──────────────────────────────────────────────────────────────
    public boolean isOpen() { return open; }

    public void open() {
        open = true;
        buffer.setLength(0);
    }

    public void close() {
        open = false;
        buffer.setLength(0);
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
        // Ignore control characters (newline, carriage return, tab, backspace)
        if (ch == null || ch.isEmpty()) return;
        char c = ch.charAt(0);
        if (c < 32 || c == 127) return;
        buffer.append(c);
    }

    public void backspace() {
        if (open && !buffer.isEmpty())
            buffer.deleteCharAt(buffer.length() - 1);
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
        // Output message (shows above input bar when both are visible)
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

        // Input bar
        if (open) {
            gc.setFill(Color.color(0.0, 0.0, 0.0, 0.82));
            gc.fillRect(0, H - 36, W, 36);
            gc.setFill(Color.WHITE);
            gc.fillText("> " + buffer + "█", 10, H - 12);
        }
    }
}
