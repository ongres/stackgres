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

        commandLine.getOut().print("\033[" + printedLines + "A"); // go n lines up
        if (status == Status.SUCCESS)
            commandLine.getOut().print(AUTO.string("@|green ✓|@ "));
        else if (status == Status.WARNING)
            commandLine.getOut().print(AUTO.string("@|yellow ⚠|@ "));
        else if (status == Status.FAILED)
            commandLine.getOut().print(AUTO.string("@|red ✗|@ "));
        else {
            commandLine.getOut().print(spinnerChars[index] + " ");
            index = (index + 1) % spinnerChars.length;
        }
        try {
            lock.readLock().lock();
            messages.forEach(commandLine.getOut()::println);
            printedLines = messages.size();
        } finally {
            lock.readLock().unlock();
        }
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
        status = Status.FAILED;
        replace(Strings.errorAnsi(error));
        stopAndDisplayStatus();
    }

    public void failedAddFirstLine(String error) {
        status = Status.FAILED;
        addFirst(Strings.errorAnsi(error));
        stopAndDisplayStatus();
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
