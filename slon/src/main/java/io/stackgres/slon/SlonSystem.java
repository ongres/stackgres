package io.stackgres.slon;

import com.sun.management.OperatingSystemMXBean;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public final class SlonSystem {

    private static final System.Logger logger = System.getLogger("SlonSystem");

    private static final String clusterId;
    private static final String clusterName;
    private static final String slonVersion;
    private static final String instanceName;
    private static final String os;
    private static final String osId;
    private static final double cpus;
    private static final long memory;
    private static final String username;
    private static final String password;
    private static final String listenAddress;
    private static final String otlpEndpoint;
    private static final String otlpAuthHeader;

    /**
     * Architecture including potential variant, e.g. {@code amd64}, {@code s390x}, or {@code arm64/v7}.
     */
    private static final String arch = System.getProperty("os.arch");

    static {
        clusterId = System.getenv("STACKGRES_CLUSTER_ID");
        clusterName = System.getenv("STACKGRES_CLUSTER_NAME");
        slonVersion = Config.getAppProperty("slon.version", "999-SNAPSHOT");
        username = System.getenv("POSTGRES_USER");
        password = System.getenv("POSTGRES_PASSWORD");
        listenAddress = System.getenv("POSTGRES_LISTEN_ADDRESS");
        otlpEndpoint = System.getenv("OTLP_ENDPOINT");
        otlpAuthHeader = System.getenv("OTLP_AUTH_HEADER");

        List<String> lines = List.of();
        try {
            lines = Files.readAllLines(Paths.get("/etc/os-release"));
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Could not read /etc/os-release", e);
        }
        osId = lines.stream().filter(line -> line.startsWith("ID="))
                .map(line -> line.substring(3).replace("\"", ""))
                .findFirst().orElse(null);

        os = osId != null ? "linux/" + osId : "linux";

        instanceName = detectInstanceName();

        // TODO better way?
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        memory = osBean.getTotalPhysicalMemorySize();

        cpus = Runtime.getRuntime().availableProcessors();
    }

    private static String detectInstanceName() {
        String envInstanceName = System.getenv("STACKGRES_INSTANCE_NAME");
        return envInstanceName != null ? envInstanceName : detectHostname() + "-" + UUID.randomUUID().toString().substring(0, 8);
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

    public static String getClusterId() {
        return clusterId;
    }

    public static String getClusterName() {
        return clusterName;
    }

    public static String getInstanceName() {
        return instanceName;
    }

    public static String getOs() {
        return os;
    }

    public static String getArch() {
        return arch;
    }

    public static String getVersion() {
        return slonVersion;
    }

    public static double getNumberOfCpus() {
        return cpus;
    }

    public static long getMemoryBytes() {
        return memory;
    }

    public static String getPgUsername() {
        return username;
    }

    public static String getPgPassword() {
        return password;
    }

    public static String getPgListenAddress() {
        return listenAddress;
    }

    public static String getOtlpEndpoint() {
        return otlpEndpoint;
    }

    public static String getOtlpAuthHeader() {
        return otlpAuthHeader;
    }

}