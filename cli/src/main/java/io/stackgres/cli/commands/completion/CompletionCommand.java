package io.stackgres.cli.commands.completion;

import picocli.CommandLine.Command;

@Command(name = "completion", mixinStandardHelpOptions = true, subcommands = {ZshCompletionCommand.class}, description = "Generates shell completions", hidden = true)
public class CompletionCommand {
}