package io.stackgres.cli.commands;

import io.stackgres.cli.CliContext;
import io.stackgres.cli.Jwt;
import picocli.CommandLine.Command;

/**
 * Prints the current user, read straight from the bearer JWT — no server call. The cloud identifies
 * the tenant by the token (it doesn't serve the legacy AccountService), and a local matriarch is
 * unauthenticated, so decoding the token offline is both correct and robust everywhere: email &gt;
 * preferred_username &gt; sub, or "anonymous" when there is no token. Scripting-friendly: emits just the
 * identity. See {@code status} for the fuller picture.
 */
@Command(name = "whoami", description = "Displays the current user (read from the bearer token)")
public class WhoAmICommand extends StackGresSubCommand {

    @Override
    public void run() {
        String subject = Jwt.subject(CliContext.resolve().token());
        outln(subject != null ? subject : "anonymous");
    }

}
