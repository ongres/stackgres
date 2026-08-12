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
        Throwable cause = exception.getCause();
        if (exception.getMessage() != null)
            return exception.getMessage();
        if (cause == null)
            return exception.toString();
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }

}