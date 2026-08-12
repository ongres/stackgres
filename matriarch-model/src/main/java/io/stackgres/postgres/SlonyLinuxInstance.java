package io.stackgres.postgres;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SlonyLinuxInstance extends ClusterInstance {

    private UUID slonyId;
    private Path configPath;
    private Path dataDir;
    private Path logDir;
    private Path walDir;
    private List<VolumeMount> volumeMounts = new ArrayList<>();

    public SlonyLinuxInstance() {
    }

    public SlonyLinuxInstance(String name, String version, Integer port, String listenAddress, Status status) {
        super(name, version, port, listenAddress, status);
    }

    public SlonyLinuxInstance(UUID instanceId, String name, String version, Integer port, String listenAddress, Status status, UUID slonyId, Path configPath, Path dataDir, Path logDir, Path walDir, List<VolumeMount> volumeMounts) {
        super(instanceId, name, version, port, listenAddress, status);
        this.slonyId = slonyId;
        this.configPath = configPath;
        this.dataDir = dataDir;
        this.logDir = logDir;
        this.walDir = walDir;
        this.volumeMounts.addAll(volumeMounts);
    }

    public UUID getSlonyId() {
        return slonyId;
    }

    public void setSlonyId(UUID slonyId) {
        this.slonyId = slonyId;
    }

    public Path getConfigPath() {
        return configPath;
    }

    public void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public void setDataDir(Path dataDir) {
        this.dataDir = dataDir;
    }

    public Path getLogDir() {
        return logDir;
    }

    public void setLogDir(Path logDir) {
        this.logDir = logDir;
    }

    public Path getWalDir() {
        return walDir;
    }

    public void setWalDir(Path walDir) {
        this.walDir = walDir;
    }

    public List<VolumeMount> getVolumeMounts() {
        return volumeMounts;
    }

    public void setVolumeMounts(List<VolumeMount> volumeMounts) {
        this.volumeMounts = volumeMounts;
    }

    @Override
    public String toString() {
        return "SlonyLinuxInstance{" +
               "slonyId=" + slonyId +
               ", configPath=" + configPath +
               ", dataDir=" + dataDir +
               ", logDir=" + logDir +
               ", walDir=" + walDir +
               ", volumeMounts=" + volumeMounts +
               "} " + super.toString();
    }
}