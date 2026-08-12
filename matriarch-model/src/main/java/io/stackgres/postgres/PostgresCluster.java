package io.stackgres.postgres;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PostgresCluster {

    private UUID id;
    private String name;
    private String username;
    private String password;
    private Flavor flavor;

    /**
     * User-defined tags as metadata, which can be used for filtering clusters.
     */
    private Map<String, String> tags = new HashMap<>();
    private List<Extension> extensions = new ArrayList<>();

    private boolean standalone;
    private boolean noTls;

    // Accumulated resource information (for cluster-level views)
    private double cpu;
    private long memory;
    private long dbSize;

    private Set<ClusterInstance> instances = new HashSet<>();
    private Map<String, String> etcdInitialCluster = new HashMap<>();

    public PostgresCluster() {
    }

    public PostgresCluster(UUID id, String name, String username, String password, List<Extension> extensions, Map<String, String> tags, boolean standalone, List<? extends ClusterInstance> instances) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.extensions.addAll(extensions);
        this.tags.putAll(tags);
        this.standalone = standalone;
        this.instances.addAll(instances);
    }

    public void merge(PostgresCluster clusterInstance) {
        this.name = clusterInstance.getName();
        this.username = clusterInstance.getUsername();
        this.password = clusterInstance.getPassword();
        this.flavor = clusterInstance.getFlavor();
        this.tags.clear();
        this.tags.putAll(clusterInstance.getTags());
        this.extensions.clear();
        this.extensions.addAll(clusterInstance.getExtensions());
        this.standalone = clusterInstance.isStandalone();
        for (ClusterInstance newInstance : clusterInstance.getInstances()) {
            this.instances.stream()
                    .filter(i -> i.getId().equals(newInstance.getId()))
                    .findFirst()
                    .ifPresent(existing -> newInstance.setStatus(existing.getStatus()));
            this.instances.removeIf(i -> i.getId().equals(newInstance.getId()));
        }
        this.instances.addAll(clusterInstance.getInstances());
    }

    public boolean containsTags(Map<String, String> tags) {
        return tags.entrySet().stream().allMatch(e -> e.getValue().equals(this.tags.get(e.getKey())));
    }

    public boolean allInstancesHavePorts() {
        return instances.stream().allMatch(i -> i.hasAllPorts(flavor));
    }

    public void updateEtcdInitialCluster() {
        if (standalone)
            throw new IllegalStateException("Cannot update etcd initial cluster for a standalone cluster");
        etcdInitialCluster.clear();
        etcdInitialCluster.putAll(
                instances.stream()
                        .filter(i -> i instanceof SlonyLinuxHAInstance)
                        .map(i -> (SlonyLinuxHAInstance) i)
                        .filter(i -> i.getEtcdName() != null && i.getEtcdServerUrl() != null)
                        .collect(Collectors.toMap(SlonyLinuxHAInstance::getEtcdName, SlonyLinuxHAInstance::getEtcdServerUrl)));
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Flavor getFlavor() {
        return flavor;
    }

    public void setFlavor(Flavor flavor) {
        this.flavor = flavor;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public List<Extension> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<Extension> extensions) {
        this.extensions = extensions;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    public boolean isRunning() {
        return instances.stream().anyMatch(i -> {
            Status s = i.getStatus();
            return s == Status.STARTED || s == Status.HEALTHY;
        });
    }

    public boolean isNoTls() {
        return noTls;
    }

    public void setNoTls(boolean noTls) {
        this.noTls = noTls;
    }

    public Set<ClusterInstance> getInstances() {
        return instances;
    }

    public void setInstances(Set<ClusterInstance> instances) {
        this.instances = instances;
    }

    public Map<String, String> getEtcdInitialCluster() {
        return etcdInitialCluster;
    }

    public void setEtcdInitialCluster(Map<String, String> etcdInitialCluster) {
        this.etcdInitialCluster = etcdInitialCluster;
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

    @Override
    public String toString() {
        return "PostgresCluster{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", username='" + username + '\'' +
               ", password='" + password + '\'' +
               ", flavor=" + flavor +
               ", tags=" + tags +
               ", extensions=" + extensions +
               ", standalone=" + standalone +
               ", instances=" + instances +
               ", etcdInitialCluster=" + etcdInitialCluster +
               '}';
    }

    public static PostgresCluster clusterSingleSlonyInstance() {
        return clusterSingleSlonyInstance(null, null, null, null, null, null, null, Map.of(), List.of(), null, null, null, null, null, List.of());
    }

    public static PostgresCluster clusterSingleSlonyInstance(String name, Flavor flavor, String version, Integer port, String listenAddress, String username, String password, Map<String, String> tags, List<Extension> extensions,
                                                             UUID slonyId, Path configDir, Path dataDir, Path logsDir, Path walDir, List<VolumeMount> volumes) {
        PostgresCluster cluster = new PostgresCluster();
        cluster.id = UUID.randomUUID();
        cluster.name = name;
        cluster.username = username;
        cluster.password = password;
        cluster.flavor = flavor;
        cluster.tags.putAll(tags);
        cluster.extensions.addAll(extensions);
        cluster.standalone = true;
        String instanceName = name + "-0";
        SlonyLinuxInstance instance = new SlonyLinuxInstance(instanceName, version, port, listenAddress, Status.CREATED);
        instance.setSlonyId(slonyId);
        instance.setConfigPath(configDir);
        instance.setDataDir(dataDir);
        instance.setLogDir(logsDir);
        instance.setWalDir(walDir);
        instance.setVolumeMounts(volumes);
        cluster.instances.add(instance);
        return cluster;
    }

    public static PostgresCluster clusterHASlonyInstance(String name, Flavor flavor, String version, int instances, Integer port, String listenAddress, String username, String password, Map<String, String> tags, List<Extension> extensions,
                                                         UUID slonyId, Path configDir, Path dataDir, Path logsDir, Path walDir, List<VolumeMount> volumes) {
        PostgresCluster cluster = new PostgresCluster();
        cluster.id = UUID.randomUUID();
        cluster.name = name;
        cluster.username = username;
        cluster.password = password;
        cluster.flavor = flavor;
        cluster.tags.putAll(tags);
        cluster.extensions.addAll(extensions);
        cluster.standalone = false;
        for (int i = 0; i < instances; i++) {
            String instanceName = name + "-" + i;
            SlonyLinuxInstance instance = new SlonyLinuxInstance(instanceName, version, port, listenAddress, Status.CREATED);
            instance.setSlonyId(slonyId);
            instance.setConfigPath(configDir);
            instance.setDataDir(dataDir);
            instance.setLogDir(logsDir);
            instance.setWalDir(walDir);
            instance.setVolumeMounts(volumes);
            cluster.instances.add(instance);
        }
        return cluster;
    }

}