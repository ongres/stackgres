package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.Times;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.cli.postgres.ClusterDiagnostics;
import io.stackgres.cli.postgres.InstanceDiagnostics;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "diagnostics", description = "Get diagnostics for a PostgreSQL cluster", hidden = true)
public class DiagnosticsClusterCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(description = "The cluster name")
    String name;

    @Override
    public void run() {
        if (debug) client.setDebug();
        ClusterDiagnostics diagnostics = client.getClusterDiagnostics(name);

        for (InstanceDiagnostics instance : diagnostics.instances()) {
            String receivedAt = instance.receivedAt() != null ? Times.stamp(instance.receivedAt()) : "N/A";
            String pgControlData = instance.pgControlData() != null ? instance.pgControlData() : "N/A";
            String imageName = instance.imageName() != null ? instance.imageName() : "N/A";
            String imageDigest = instance.imageDigest() != null ? instance.imageDigest() : "N/A";

            outln("""
                    Instance:     $name$
                    ID:           $id$
                    Image:        $imageName$
                    Image Digest: $imageDigest$
                    
                    Diagnostics received at:  $receivedAt$

                    pg_controldata:

                    $pgControlData$
                    ------------------------"""
                    .replace("$name$", instance.name())
                    .replace("$id$", instance.id().toString())
                    .replace("$imageName$", imageName)
                    .replace("$imageDigest$", imageDigest)
                    .replace("$receivedAt$", receivedAt)
                    .replace("$pgControlData$", pgControlData));
            outln("");
        }
    }

}