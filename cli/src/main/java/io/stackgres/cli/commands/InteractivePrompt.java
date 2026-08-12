package io.stackgres.cli.commands;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.postgres.ClusterInstance;
import io.stackgres.postgres.Flavor;
import io.stackgres.postgres.PostgresCluster;
import io.stackgres.postgres.PostgresClusterValidator;
import io.stackgres.postgres.PostgresVersions;
import org.jline.keymap.KeyMap;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static picocli.CommandLine.Help.Ansi.AUTO;

public class InteractivePrompt {

    private final CommandLine commandLine;

    public InteractivePrompt(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    public boolean confirm(String prompt) {
        try {
            LineReader lineReader = LineReaderBuilder.builder().build();
            String line = lineReader.readLine(AUTO.string("To confirm type @|bold " + prompt + "|@ and press Enter: "), null, null).trim();
            return prompt.equals(line);
        } catch (Exception e) {
            return false;
        }
    }

    public void populateCreate(PostgresCluster cluster, MatriarchClient client) {
        commandLine.getOut().println("Creating a PostgreSQL cluster");

        try (Terminal terminal = TerminalBuilder.terminal()) {
            terminal.enterRawMode();

            while (cluster.getName() == null) {
                printComment("Select a name for the cluster");
                String clusterName = readLine(terminal, "Name: ");
                if (clusterName.isBlank() || !PostgresClusterValidator.isNameValid(clusterName)) {
                    printError("Cluster name cannot be blank or contain special characters (only [a-z0-9-_] allowed)");
                    continue;
                }
                cluster.setName(clusterName);
            }

            List<String> flavors = List.of(Flavor.POSTGRES.id(), Flavor.IVORY_SQL.id());
            while (cluster.getFlavor() == null) {
                printComment("Select a database flavor (" + String.join(" or ", flavors) + ")");
                String flavor = readOption(terminal, "Flavor: ", flavors);
                if (flavor.isBlank()) {
                    cluster.setFlavor(Flavor.POSTGRES);
                    break;
                }
                if (!flavors.contains(flavor)) {
                    printError("Please select a valid flavor");
                    continue;
                }
                cluster.setFlavor(Flavor.fromId(flavor));
            }

            // TODO
            ClusterInstance instance = cluster.getInstances().iterator().next();
            while (instance.getVersion() == null) {
                List<String> pgVersions = PostgresVersions.listAvailableVersions();
                printComment("Select a PostgreSQL version");
                String version = readOption(terminal, "Version: ", pgVersions);
                if (!pgVersions.contains(version)) {
                    printError("Please select a valid version");
                    continue;
                }
                instance.setVersion(version);
            }

            while (instance.getPort() == null) {
                printComment("Select a port under which the database will be available locally");
                String port = readLine(terminal, "Port: ", "5432");
                int number;
                if (port.isBlank() || !port.chars().allMatch(Character::isDigit) || (number = Integer.parseInt(port)) < 1 || number > 65535) {
                    printError("Please specify a valid port between 1 - 65535");
                    continue;
                }
                instance.setPort(number);
            }

            commandLine.getOut().println();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printComment(String comment) {
        commandLine.getOut().println(AUTO.string("\033[38:5:246m" + comment + "\033[39m"));
    }

    private void printError(String error) {
        commandLine.getErr().println(commandLine.getColorScheme().errorText(error));
    }

    private String readLine(Terminal terminal, String prompt) {
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        return readLine(lineReader, prompt, null).trim();
    }

    private String readLine(Terminal terminal, String prompt, String buffer) {
        LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        return readLine(lineReader, prompt, buffer).trim();
    }

    private String readLine(Terminal terminal, String prompt, String buffer, List<String> options) {
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter(options))
                .build();
        return readLine(lineReader, prompt, buffer).trim();
    }

    private String readLine(Terminal terminal, String prompt, List<String> options) {
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter(options))
                .build();
        return readLine(lineReader, prompt, null).trim();
    }

    private String readOption(Terminal terminal, String prompt, List<String> options) {
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter(options))
                .option(LineReader.Option.AUTO_MENU_LIST, true)
                .option(LineReader.Option.MENU_COMPLETE, true)
                .build();
        lineReader.runMacro(KeyMap.key(terminal, InfoCmp.Capability.tab));
        return readLine(lineReader, prompt, null).trim();
    }

    private Boolean readYesNoOption(Terminal terminal, String prompt) {
        String answer = readOption(terminal, prompt, List.of("yes", "no"));
        if ("yes".equalsIgnoreCase(answer))
            return Boolean.TRUE;
        if ("no".equalsIgnoreCase(answer))
            return Boolean.FALSE;
        return null;
    }

    private String readLine(LineReader lineReader, String prompt, String buffer) {
        try {
            return lineReader.readLine(AUTO.string("@|green " + prompt + "|@"), null, buffer);
        } catch (UserInterruptException e) {
            printError("Aborted cluster creation");
            System.exit(1);
            return "";
        } catch (EndOfFileException e) {
            return "";
        }
    }

}