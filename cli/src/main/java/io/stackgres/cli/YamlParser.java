package io.stackgres.cli;

import io.stackgres.postgres.*;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class YamlParser {

    public PostgresCluster parseYaml(String yaml) {
        Map<String, Object> values = new Yaml().load(yaml);
        return mapValues(values);
    }

    private PostgresCluster mapValues(Map<String, Object> values) {
        if (values == null)
            return PostgresCluster.clusterSingleSlonyInstance();

        String name = getStringValue("name", values);
        Flavor flavor = Flavor.fromId(getStringValue("flavor", values));
        String version = getStringValue("version", values);
        Boolean highAvailability = getBooleanValue("highAvailability", values);
        String username = getStringValue("username", values);
        String password = getStringValue("password", values);
        Map<String, String> tags = getStringMap("tags", values);
        List<String> extensions = getStringList("extensions", values);

        Integer port = getIntegerValue("port", values);
        Integer ivorySqlPort = getIntegerValue("ivorySqlPort", values);
        UUID slony = getUUIDValue("node", values);
        String listenAddress = getStringValue("listenAddress", values);
        String config = getStringValue("configDir", values);
        Path configDir = config != null ? Paths.get(config) : null;
        String data = getStringValue("dataDir", values);
        Path dataDir = data != null ? Paths.get(data) : null;
        String logs = getStringValue("logsDir", values);
        Path logsDir = logs != null ? Paths.get(logs) : null;
        String wal = getStringValue("walDir", values);
        Path walDir = wal != null ? Paths.get(wal) : null;
        List<VolumeMount> volumeMounts = VolumeMount.parseMounts(getStringList("volumeMounts", values));

        List<ClusterInstance> instances = new ArrayList<>();
        List<Map<String, Object>> instanceList = getObjectList("instances", values);

        if (instanceList != null) {
            if (instanceList.isEmpty())
                throw new IllegalArgumentException("HA 'instances' cannot be an empty list.");
            if (highAvailability == Boolean.FALSE)
                throw new IllegalArgumentException("HA 'instances' cannot be defined when 'highAvailability' is set to false.");
            highAvailability = true;
            for (Map<String, Object> instanceMap : instanceList) {
                SlonyLinuxHAInstance instance = new SlonyLinuxHAInstance(UUID.randomUUID(), null, version, port, listenAddress, null, slony, configDir, dataDir, logsDir, walDir, volumeMounts, null, null, null);
                if (ivorySqlPort != null) instance.setIvorySqlPort(ivorySqlPort);

                Integer iPort = getIntegerValue("port", instanceMap);
                if (iPort != null) instance.setPort(iPort);
                Integer iIvorySqlPort = getIntegerValue("ivorySqlPort", instanceMap);
                if (iIvorySqlPort != null) instance.setIvorySqlPort(iIvorySqlPort);
                UUID iSlony = getUUIDValue("node", instanceMap);
                if (iSlony != null) instance.setSlonyId(iSlony);
                String iListenAddress = getStringValue("listenAddress", instanceMap);
                if (iListenAddress != null) instance.setListenAddress(iListenAddress);
                String iConfig = getStringValue("configDir", instanceMap);
                Path iConfigDir = iConfig != null ? Paths.get(iConfig) : null;
                if (iConfigDir != null) instance.setConfigPath(iConfigDir);
                String iData = getStringValue("dataDir", instanceMap);
                Path iDataDir = iData != null ? Paths.get(iData) : null;
                if (iDataDir != null) instance.setDataDir(iDataDir);
                String iLogs = getStringValue("logsDir", instanceMap);
                Path iLogsDir = iLogs != null ? Paths.get(iLogs) : null;
                if (iLogsDir != null) instance.setLogDir(iLogsDir);
                String iWal = getStringValue("walDir", instanceMap);
                Path iWalDir = iWal != null ? Paths.get(iWal) : null;
                if (iWalDir != null) instance.setWalDir(iWalDir);
                List<VolumeMount> iVolumeMounts = VolumeMount.parseMounts(getStringList("volumeMounts", instanceMap));
                if (!iVolumeMounts.isEmpty()) instance.getVolumeMounts().addAll(iVolumeMounts);

                instances.add(instance);
            }
        } else {
            highAvailability = highAvailability == Boolean.TRUE;
            ClusterInstance singleInstance;
            if (highAvailability)
                singleInstance = new SlonyLinuxHAInstance(UUID.randomUUID(), null, version, port, listenAddress, null, slony, configDir, dataDir, logsDir, walDir, volumeMounts, null, null, null);
            else
                singleInstance = new SlonyLinuxInstance(UUID.randomUUID(), null, version, port, listenAddress, null, slony, configDir, dataDir, logsDir, walDir, volumeMounts);
            if (ivorySqlPort != null) singleInstance.setIvorySqlPort(ivorySqlPort);
            instances.add(singleInstance);
        }

        if (instances.stream().anyMatch(i -> i.getIvorySqlPort() != null) && flavor != Flavor.IVORY_SQL)
            throw new IllegalArgumentException("'ivorySqlPort' can only be set for IvorySQL clusters");

        PostgresCluster cluster = new PostgresCluster(UUID.randomUUID(), name, username, password, Extension.extractExtensions(extensions), tags, !highAvailability, instances);
        cluster.setFlavor(flavor);
        return cluster;
    }

    private static String getStringValue(String key, Map<String, Object> values) {
        Object value = values.getOrDefault(key, null);
        if (value == null) return null;
        if (!(value instanceof String))
            throw new IllegalArgumentException("Expected YAML property '" + key + "' to be a string. Consider adding quotes.");
        return (String) value;
    }

    private static UUID getUUIDValue(String key, Map<String, Object> values) {
        Object value = values.getOrDefault(key, null);
        if (value == null) return null;
        if (!(value instanceof String))
            throw new IllegalArgumentException("Expected YAML property '" + key + "' to be a UUID string.");
        return UUID.fromString((String) value);
    }

    private static Integer getIntegerValue(String key, Map<String, Object> values) {
        Object value = values.getOrDefault(key, null);
        if (value == null) return null;
        if (!(value instanceof Integer))
            throw new IllegalArgumentException("Expected YAML property '" + key + "' to be an integer.");
        return (Integer) value;
    }

    private static Boolean getBooleanValue(String key, Map<String, Object> values) {
        Object value = values.getOrDefault(key, null);
        if (value == null) return null;
        if (!(value instanceof Boolean))
            throw new IllegalArgumentException("Expected YAML property '" + key + "' to be a boolean.");
        return (boolean) value;
    }

    private static boolean getBooleanValue(String key, Map<String, Object> values, boolean defaultValue) {
        Object value = values.getOrDefault(key, null);
        if (value == null) return defaultValue;
        if (!(value instanceof Boolean))
            throw new IllegalArgumentException("Expected YAML property '" + key + "' to be a boolean.");
        return (boolean) value;
    }

    private List<String> getStringList(String key, Map<String, Object> values) {
        Object value = values.getOrDefault(key, null);
        if (value == null) return List.of();
        if (!(value instanceof List))
            throw new IllegalArgumentException("Expected YAML property '" + key + "' to be a list.");
        return ((List<?>) value).stream()
                .map(o -> {
                    if (!(o instanceof String))
                        throw new IllegalArgumentException("Expected element '" + o + "' of YAML list '" + key + "' to be a string.");
                    return (String) o;
                }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getStringMap(String key, Map<String, Object> values) {
        Object value = values.getOrDefault(key, null);
        if (value == null) return Map.of();
        if (!(value instanceof Map))
            throw new IllegalArgumentException("Expected YAML property '" + key + "' to be an object.");

        ((Map<?, ?>) value).forEach((k, v) -> {
            if (!(v instanceof String))
                throw new IllegalArgumentException("Expected value '" + v + "' in YAML object '" + k + "' (in map '" + key + "') to be a string.");
        });
        return (Map<String, String>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getObjectList(String key, Map<String, Object> values) {
        Object value = values.getOrDefault(key, null);
        if (value == null) return null;
        if (!(value instanceof List))
            throw new IllegalArgumentException("Expected YAML property '" + key + "' to be a list.");
        return ((List<?>) value).stream()
                .map(o -> {
                    if (!(o instanceof Map))
                        throw new IllegalArgumentException("Expected element '" + o + "' of YAML list '" + key + "' to be an object.");
                    return (Map<String, Object>) o;
                }).collect(Collectors.toList());
    }

}