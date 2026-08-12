package io.stackgres.cli.commands;

import picocli.CommandLine;

public class StackGresPicocliException extends CommandLine.PicocliException {

    private final ProgressMessages messages;

    public StackGresPicocliException(String msg, ProgressMessages messages) {
        super(msg);
        this.messages = messages;
    }

    public StackGresPicocliException(Exception exception, ProgressMessages messages) {
        super(null, exception);
        this.messages = messages;
    }

    public ProgressMessages getMessages() {
        return messages;
    }

}
