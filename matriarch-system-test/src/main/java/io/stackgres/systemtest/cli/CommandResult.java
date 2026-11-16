package io.stackgres.systemtest.cli;

/** The outcome of one {@code stackgres} invocation. */
public record CommandResult(int exitCode, String stdout, String stderr) {

    public boolean isSuccess() {
        return exitCode == 0;
    }

    /** stdout + stderr — handy when asserting that a message appeared, regardless of stream. */
    public String output() {
        return stdout + stderr;
    }
}
