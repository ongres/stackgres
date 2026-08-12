package io.stackgres.postgres;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SlonyLinuxHAInstance extends SlonyLinuxInstance {

    private static final String PATRONI_REV = "7566";

    private String etcdName;
    private String etcdServerUrl;
    private String etcdClientUrl;

    public SlonyLinuxHAInstance(UUID instanceId, String name, String version, Integer port, String listenAddress, Status status, UUID slonyId, Path configPath, Path dataDir, Path logDir, Path walDir, List<VolumeMount> volumeMounts, String etcdName, String etcdServerUrl, String etcdClientUrl) {
        super(instanceId, name, version, port, listenAddress, status, slonyId, configPath, dataDir, logDir, walDir, volumeMounts);
        this.etcdName = etcdName;
        this.etcdServerUrl = etcdServerUrl;
        this.etcdClientUrl = etcdClientUrl;
    }

    @Override
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
        return "sgcr.dev:1443/stackgres/" + flavor.id() + "--" + getVersion() + extension + "/a/slon--0.1--" + SLON_REV + "/a/patroni--4.1.0--" + PATRONI_REV + "/a/vector-agent--0.55.0--" + VECTOR_AGENT_REV;
    }

    @Override
    public boolean hasAllPorts(Flavor flavor) {
        return super.hasAllPorts(flavor) && etcdServerUrl != null && etcdClientUrl != null;
    }

    public Integer getEtcdServerPort() {
        if (etcdServerUrl == null) return null;
        return Integer.parseInt(etcdServerUrl.split(":")[1]);
    }

    public Integer getEtcdClientPort() {
        if (etcdClientUrl == null) return null;
        return Integer.parseInt(etcdClientUrl.split(":")[1]);
    }

    public String getEtcdName() {
        return etcdName;
    }

    public void setEtcdName(String etcdName) {
        this.etcdName = etcdName;
    }

    public String getEtcdServerUrl() {
        return etcdServerUrl;
    }

    public void setEtcdServerUrl(String etcdServerUrl) {
        this.etcdServerUrl = etcdServerUrl;
    }

    public String getEtcdClientUrl() {
        return etcdClientUrl;
    }

    public void setEtcdClientUrl(String etcdClientUrl) {
        this.etcdClientUrl = etcdClientUrl;
    }

    @Override
    public String toString() {
        return "SlonyLinuxHAInstance{" +
               "etcdName='" + etcdName + '\'' +
               ", etcdPeerUrl='" + etcdServerUrl + '\'' +
               ", etcdClientUrl='" + etcdClientUrl + '\'' +
               "} " + super.toString();
    }

}