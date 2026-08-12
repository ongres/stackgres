package io.stackgres.slony;

import io.stackgres.cloud.CloudEnvironment;
import io.stackgres.slony.client.Mappers;
import io.stackgres.slony.cloud.CloudDetector;
import com.sun.management.OperatingSystemMXBean;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public final class SlonySystem {

    private static final System.Logger logger = System.getLogger("SlonySystem");

    private static final String slonyVersion = "slony-linux/0.1";
    private static final String hostname;
    private static final String externalAddress;
    private static final String os;
    private static final String osId;
    private static final double cpus;
    private static final long memory;
    private static final CloudEnvironment cloudEnvironment;
    private static final Map<String, String> tags;

    /**
     * Architecture including potential variant, e.g. {@code amd64}, {@code s390x}, or {@code arm64/v7}.
     */
    private static final String arch = System.getProperty("os.arch");

    static {
        List<String> lines = List.of();
        try {
            lines = Files.readAllLines(Paths.get("/etc/os-release"));
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Could not read /etc/os-release", e);
        }
        osId = lines.stream().filter(line -> line.startsWith("ID="))
                .map(line -> line.substring(3))
                .findFirst().orElse(null);

        os = osId != null ? "linux/" + osId : "linux";

        hostname = detectHostname();
        externalAddress = detectExternalAddress();

        // TODO better way?
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        memory = osBean.getTotalPhysicalMemorySize();

        cpus = Runtime.getRuntime().availableProcessors();

        cloudEnvironment = CloudDetector.detect();
        if (cloudEnvironment != null)
            logger.log(System.Logger.Level.INFO, "Detected cloud: {0}, region: {1}, zone: {2}", cloudEnvironment.cloud(), cloudEnvironment.region(), cloudEnvironment.availabilityZone());

        tags = detectTags();
    }

    private static String detectHostname() {
        try {
            Process process = new ProcessBuilder("hostname").start();
            try (BufferedReader reader = process.inputReader()) {
                return reader.readLine();
            }
        } catch (IOException e) {
            // potentially `hostname` is not available
            try {
                return Files.readString(Paths.get("/etc/hostname")).strip();
            } catch (Exception e2) {
                try {
                    return InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e3) {
                    return osId != null ? osId : os;
                }
            }
        }
    }

    public static String getHostname() {
        return hostname;
    }

    private static String detectExternalAddress() {
        String listenAddress = Config.getValue("STACKGRES_SLONY_ADDRESS", null);
        if (listenAddress != null) return listenAddress;
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            try {
                return InetAddress.getByName(hostname).getHostAddress();
            } catch (UnknownHostException ex) {
                return "127.0.0.1";
            }
        }
    }

    private static Map<String, String> detectTags() {
        String value = Config.getValue("STACKGRES_SLONY_TAGS", null);
        if (value == null || value.isBlank())
            return Map.of();
        return Mappers.extractMap(value);
    }

    public static String getExternalAddress() {
        return externalAddress;
    }

    public static String getOs() {
        return os;
    }

    public static String getArch() {
        return arch;
    }

    public static String getVersion() {
        return slonyVersion;
    }

    public static double getNumberOfCpus() {
        return cpus;
    }

    public static long getMemoryBytes() {
        return memory;
    }

    public static CloudEnvironment getCloudEnvironment() {
        return cloudEnvironment;
    }

    public static Map<String, String> getTags() {
        return tags;
    }

}