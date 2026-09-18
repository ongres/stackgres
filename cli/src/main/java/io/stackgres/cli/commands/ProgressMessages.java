package io.stackgres.cli.commands;

import io.stackgres.cli.Strings;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static picocli.CommandLine.Help.Ansi.AUTO;

public class ProgressMessages {

    private static final char[] spinnerChars = {'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};

    private final List<String> messages = new ArrayList<>();
    private final CommandLine commandLine;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService executorService;
    private transient Status status;
    private int printedLines;
    private int index;

    public ProgressMessages(CommandLine commandLine) {
        this("", commandLine);
    }

    public ProgressMessages(String message, CommandLine commandLine) {
        this.commandLine = commandLine;
        add(message);
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(this::display, 75, 75, TimeUnit.MILLISECONDS);
        display();
    }

    private void display() {
        if (status == null) {
            commandLine.getOut().println();
            printedLines = 1;
            status = Status.RUNNING;
        }

        final String glyph;
        if (status == Status.SUCCESS)
            glyph = AUTO.string("@|green ✓|@ ");
        else if (status == Status.WARNING)
            glyph = AUTO.string("@|yellow ⚠|@ ");
        else if (status == Status.FAILED)
            glyph = AUTO.string("@|red ✗|@ ");
        else {
            glyph = spinnerChars[index] + " ";
            index = (index + 1) % spinnerChars.length;
        }

        final List<String> rows;
        try {
            lock.readLock().lock();
            rows = new ArrayList<>(messages);
        } finally {
            lock.readLock().unlock();
        }
        if (rows.isEmpty())
            rows.add("");
        rows.set(0, glyph + rows.get(0)); // the status glyph rides the first line

        var out = commandLine.getOut();
        out.print("\033[" + printedLines + "A"); // to the top of the block drawn last time
        for (String row : rows) {
            // "\033[K" clears from the cursor to the end of the line, so a shorter redraw — an error
            // collapsing several lines to one, or the short tail of a line overwriting a longer previous
            // one (incl. the last row of a wrapped line) — leaves no stale characters behind.
            out.print(row);
            out.print("\033[K");
            out.print("\n");
        }
        // If the block shrank (e.g. a failure replaced many lines with one), clear the rows it no longer
        // uses, then step the cursor back up so it rests just after the current content.
        int stale = printedLines - rows.size();
        for (int i = 0; i < stale; i++)
            out.print("\033[K\n");
        if (stale > 0)
            out.print("\033[" + stale + "A");
        out.flush();
        printedLines = rows.size();
    }

    public void add(String message) {
        try {
            lock.writeLock().lock();
            if (message.contains("\n"))
                message.lines().forEach(messages::add);
            else
                messages.add(message);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addFailed(String error) {
        status = Status.FAILED;
        add(Strings.errorAnsi(error));
        stopAndDisplayStatus();
    }

    public void replace(String message) {
        try {
            lock.writeLock().lock();
            messages.clear();
            messages.add(message);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void done() {
        if (status != Status.RUNNING) return;
        status = Status.SUCCESS;
        stopAndDisplayStatus();
    }

    /** Completed, but not fully — e.g. accepted-and-pending. Renders a yellow ⚠, not a green ✓. */
    public void warning() {
        if (status != Status.RUNNING) return;
        status = Status.WARNING;
        stopAndDisplayStatus();
    }

    public void doneAddFirstLine(String message) {
        addFirst(message);
        status = Status.SUCCESS;
        stopAndDisplayStatus();
    }

    public void failed() {
        status = Status.FAILED;
        stopAndDisplayStatus();
    }

    public void failed(String error) {
        finishFailed(error);
    }

    public void failedAddFirstLine(String error) {
        finishFailed(error);
    }

    /**
     * Finalize a failed operation: flip the block's status line to ✗ and repaint the block (which holds
     * only short status/info lines — no wrapping), then print the error as STATIC output BELOW the block.
     * Keeping the error out of the redrawn block is what lets an arbitrarily long message (e.g. an image
     * pull error) wrap cleanly: a spinner line is repainted by moving the cursor up N lines, which can't
     * line up once the terminal soft-wraps a long line — so a long error inside the block leaves stale
     * characters behind. As plain output below the finalized block it just wraps like any normal text.
     */
    private void finishFailed(String error) {
        status = Status.FAILED;
        stopAndDisplayStatus();
        commandLine.getOut().println(Strings.errorAnsi(error));
    }

    private void addFirst(String message) {
        try {
            lock.writeLock().lock();
            if ("".equals(messages.get(0)))
                messages.remove(0);
            messages.add(0, message);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void stopAndDisplayStatus() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        display();
    }

    public boolean isSuccessfullyDone() {
        return status == Status.SUCCESS;
    }

    private enum Status {
        RUNNING, SUCCESS, WARNING, FAILED
    }
}
