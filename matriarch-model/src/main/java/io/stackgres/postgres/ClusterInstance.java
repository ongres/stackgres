package io.stackgres.postgres;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class ClusterInstance {

    protected static final String SLON_REV = "7572";
    protected static final String VECTOR_AGENT_REV = "7568";

    private UUID id;
    private String name;
    private String version;
    private Integer port;
    private Integer ivorySqlPort;
    private String listenAddress;
    private Status status;
    private ReplicationStatus replicationStatus = ReplicationStatus.UNKNOWN;
    private String pgControlData;
    private Instant diagnosticsReceivedAt;
    private String imageName;
    private String imageDigest;
    private double cpu;
    private long memory;
    private long dbSize;
    private String externalAddress;

    public ClusterInstance() {
    }

    public ClusterInstance(String name, String version, Integer port, String listenAddress, Status status) {
        this(UUID.randomUUID(), name, version, port, listenAddress, status);
    }

    public ClusterInstance(UUID id, String name, String version, Integer port, String listenAddress, Status status) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.port = port;
        this.listenAddress = listenAddress;
        this.status = status;
    }

    public String imageRef(List<Extension> extensions, Flavor flavor) {
        String extension = extensions.isEmpty() ? ""
                : "/e/" + extensions.stream()
                          .map(e -> {
                              if (e.version() != null) {
                                  if (e.revision() != null)
                                      return e.name() + "--" + e.version() + "--" + e.revision();
                                  return e.name() + "--" + e.version();
                              }
                              return e.name();
                          }).collect(Collectors.joining("/"));
        return "sgcr.dev:1443/stackgres/" + flavor.id() + "--" + version + extension + "/a/slon--0.1--" + SLON_REV + "/a/vector-agent--0.55.0--" + VECTOR_AGENT_REV;
    }

    public boolean hasAllPorts(Flavor flavor) {
        return port != null && (flavor != Flavor.IVORY_SQL || ivorySqlPort != null);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getIvorySqlPort() {
        return ivorySqlPort;
    }

    public void setIvorySqlPort(Integer ivorySqlPort) {
        this.ivorySqlPort = ivorySqlPort;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ReplicationStatus getReplicationStatus() {
        return replicationStatus;
    }

    public void setReplicationStatus(ReplicationStatus replicationStatus) {
        this.replicationStatus = replicationStatus;
    }

    public String getPgControlData() {
        return pgControlData;
    }

    public void setPgControlData(String pgControlData) {
        this.pgControlData = pgControlData;
    }

    public Instant getDiagnosticsReceivedAt() {
        return diagnosticsReceivedAt;
    }

    public void setDiagnosticsReceivedAt(Instant diagnosticsReceivedAt) {
        this.diagnosticsReceivedAt = diagnosticsReceivedAt;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public void setImageDigest(String imageDigest) {
        this.imageDigest = imageDigest;
    }

    public double getCpu() {
        return cpu;
    }

    public void setCpu(double cpu) {
        this.cpu = cpu;
    }

    public long getMemory() {
        return memory;
    }

    public void setMemory(long memory) {
        this.memory = memory;
    }

    public long getDbSize() {
        return dbSize;
    }

    public void setDbSize(long dbSize) {
        this.dbSize = dbSize;
    }

    public String getExternalAddress() {
        return externalAddress;
    }

    public void setExternalAddress(String externalAddress) {
        this.externalAddress = externalAddress;
    }

    @Override
    public String toString() {
        return "ClusterInstance{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", version='" + version + '\'' +
               ", port=" + port +
               ", listenAddress='" + listenAddress + '\'' +
               ", status=" + status +
               ", replicationStatus=" + replicationStatus +
               ", imageName='" + imageName + '\'' +
               ", imageDigest='" + imageDigest + '\'' +
               '}';
    }

}