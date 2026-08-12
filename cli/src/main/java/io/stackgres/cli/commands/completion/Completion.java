package io.stackgres.cli.commands.completion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class Completion {

    public String getZshCompletion() {
        return readResource("/completions/stackgres.zsh");
    }

    public String getBashCompletion() {
        return readResource("/completions/stackgres.bash");
    }

    private static String readResource(String file) {
        try (InputStream input = Completion.class.getResourceAsStream(file)) {
            return new String(input.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
