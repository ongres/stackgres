package io.stackgres.cli.commands;

import picocli.CommandLine;

public class CliExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception exception, CommandLine commandLine, CommandLine.ParseResult parseResult) {
        boolean debug = parseResult.originalArgs().stream().anyMatch(a -> "--debug".equals(a) || (a.startsWith("-") && a.contains("X")));

        String message = extractMessage(exception);

        if (exception instanceof StackGresPicocliException) {
            ProgressMessages messages = ((StackGresPicocliException) exception).getMessages();
            if (debug) messages.failedAddFirstLine(message);
            else messages.failed(message);
        } else {
            commandLine.getErr().println(commandLine.getColorScheme().errorText(message));
        }

        if (debug) {
            System.err.println("Error: " + exception);
            exception.printStackTrace();
        }

        return commandLine.getCommandSpec().exitCodeOnExecutionException();
    }

    private static String extractMessage(Exception exception) {
        // Peel "transparent" wrappers so we print the underlying clean message instead of leaking the
        // exception type — e.g. an async command whose CompletableFuture.join() surfaces the failure as
        // "java.util.concurrent.CompletionException: java.lang.IllegalStateException: <message>". Commands
        // that stream/await (psql, exec, ...) go through this; direct throws already had a clean message.
        Throwable e = unwrap(exception);
        if (e.getMessage() != null && !e.getMessage().isBlank())
            return e.getMessage();
        Throwable cause = e.getCause();
        if (cause == null)
            return e.toString();
        cause = unwrap(cause);
        return cause.getMessage() != null && !cause.getMessage().isBlank() ? cause.getMessage() : cause.toString();
    }

    /**
     * Drill through wrappers whose own message is just the cause's {@code toString()} (and would leak the
     * type): {@link java.util.concurrent.CompletionException}, {@link java.util.concurrent.ExecutionException},
     * {@link java.lang.reflect.InvocationTargetException}. Contextual wrappers — those with their own
     * message, like {@code StackGresPicocliException} or {@code new RuntimeException("Failed to X: ...", e)}
     * — are left alone so their added context survives.
     */
    private static Throwable unwrap(Throwable t) {
        while ((t instanceof java.util.concurrent.CompletionException
                || t instanceof java.util.concurrent.ExecutionException
                || t instanceof java.lang.reflect.InvocationTargetException)
                && t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }

}