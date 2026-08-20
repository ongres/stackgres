package io.stackgres.slony.cri;

import io.stackgres.postgres.*;
import io.stackgres.slony.Config;
import runtime.v1.Api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.stackgres.slony.client.Mappers.mapToString;

public final class PodContainerConfigs {

    public static Api.PodSandboxConfig postgresPodSandboxConfig(PostgresCluster cluster, SlonyLinuxInstance instance) {
        Api.PodSandboxConfig.Builder builder = Api.PodSandboxConfig.newBuilder()
                .setMetadata(Api.PodSandboxMetadata.newBuilder()
                        .setName(instance.getId().toString())
                        .setUid(instance.getId().toString())
                        .setNamespace("stackgres")
                        .build())
                .putLabels(Cri.LABEL_MANAGED_BY, "stackgres")
                .putLabels(Cri.LABEL_COMPONENT, "postgres")
                .putAnnotations(Cri.ANNOTATION_CLUSTER_ID, cluster.getId().toString())
                .putAnnotations(Cri.ANNOTATION_NAME, cluster.getName())
                .putAnnotations(Cri.ANNOTATION_INSTANCE_NAME, instance.getName())
                .putAnnotations(Cri.ANNOTATION_PG_VERSION, instance.getVersion())
                .putAnnotations(Cri.ANNOTATION_STANDALONE, String.valueOf(cluster.isStandalone()))
                .putAnnotations(Cri.ANNOTATION_PORT, String.valueOf(instance.getPort()))
                .putAnnotations(Cri.ANNOTATION_USERNAME, cluster.getUsername())
                .putAnnotations(Cri.ANNOTATION_PASSWORD, cluster.getPassword())
                .putAnnotations(Cri.ANNOTATION_TAGS, mapToString(cluster.getTags()))
                .putAnnotations(Cri.ANNOTATION_EXTENSIONS, cluster.getExtensions().stream().map(Extension::toString).collect(Collectors.joining(",")))
                .putAnnotations(Cri.ANNOTATION_LISTEN_ADDRESS, instance.getListenAddress())
                .putAnnotations(Cri.ANNOTATION_PG_CONFIG_FILE, instance.getConfigPath().toString())
                .putAnnotations(Cri.ANNOTATION_PG_DATA_PATH, instance.getDataDir().toString())
                .putAnnotations(Cri.ANNOTATION_LOG_DIR, instance.getLogDir().toString())
                .putAnnotations(Cri.ANNOTATION_WAL_DIR, instance.getWalDir().toString())
                .putAnnotations(Cri.ANNOTATION_VOLUME_MOUNTS, instance.getVolumeMounts().stream().map(v -> v.hostPath() + ":" + v.mountPath()).collect(Collectors.joining(",")))
                .putAnnotations(Cri.ANNOTATION_FLAVOR, (cluster.getFlavor() != null ? cluster.getFlavor() : Flavor.POSTGRES).id());

        if (instance.getIvorySqlPort() != null)
            builder.putAnnotations(Cri.ANNOTATION_IVORY_SQL_PORT, String.valueOf(instance.getIvorySqlPort()));

        if (instance instanceof SlonyLinuxHAInstance haInstance) {
            builder.putAnnotations(Cri.ANNOTATION_ETCD_NAME, haInstance.getEtcdName())
                    .putAnnotations(Cri.ANNOTATION_ETCD_CLIENT_URL, haInstance.getEtcdClientUrl())
                    .putAnnotations(Cri.ANNOTATION_ETCD_SERVER_URL, haInstance.getEtcdServerUrl())
                    .putAnnotations(Cri.ANNOTATION_ETCD_INITIAL_CLUSTER, mapToString(cluster.getEtcdInitialCluster()));
        }

        builder.setLinux(Api.LinuxPodSandboxConfig.newBuilder()
                        .setSecurityContext(Api.LinuxSandboxSecurityContext.newBuilder()
                                .setNamespaceOptions(Api.NamespaceOption.newBuilder()
                                        .setNetworkValue(Api.NamespaceMode.NODE_VALUE)
                                        .build())
                                .build()))
                .setLogDirectory(instance.getLogDir().toString());
        return builder.build();
    }

    public static Api.ContainerConfig postgresContainerConfig(PostgresCluster cluster, SlonyLinuxInstance instance, String logFileName) {
        Api.ContainerConfig.Builder builder = Api.ContainerConfig.newBuilder();
        List<String> commands = new ArrayList<>(List.of("slon", instance.getId().toString(), String.valueOf(instance.getPort())));
        String matriarchUrl = Config.getValue("STACKGRES_ENDPOINT_URL", "localhost:50051");
        String token = Config.getValue("STACKGRES_TOKEN", null);

        builder.setMetadata(Api.ContainerMetadata.newBuilder().setName("postgres").build())
                .setImage(Api.ImageSpec.newBuilder().setImage(instance.imageRef(cluster.getExtensions(), cluster.getFlavor())).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_CLUSTER_ID").setValue(cluster.getId().toString()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_CLUSTER_NAME").setValue(cluster.getName()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_INSTANCE_NAME").setValue(instance.getName()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("POSTGRES_USER").setValue(cluster.getUsername()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("POSTGRES_PASSWORD").setValue(cluster.getPassword()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("POSTGRES_LISTEN_ADDRESS").setValue(instance.getListenAddress()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_ENDPOINT_URL").setValue(matriarchUrl).build())
                // for psql
                .addEnvs(Api.KeyValue.newBuilder().setKey("PGHOST").setValue("/tmp").build())
                .setLinux(Api.LinuxContainerConfig.newBuilder()
                        .setSecurityContext(Api.LinuxContainerSecurityContext.newBuilder().setRunAsUsername("postgres").build()))
                .setWorkingDir("/")
                .addAllCommand(commands)
                .setLogPath(logFileName)
                .addMounts(Api.Mount.newBuilder()
                        .setHostPath(instance.getDataDir().toString())
                        .setContainerPath("/var/lib/postgresql/data")
                        .build());

        if (cluster.getFlavor() == Flavor.IVORY_SQL && instance.getIvorySqlPort() != null)
            builder.addEnvs(Api.KeyValue.newBuilder().setKey("IVORYSQL_PORT").setValue(String.valueOf(instance.getIvorySqlPort())).build());

        if (token != null)
            builder.addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_TOKEN").setValue(token).build());

        addOtlpEnvs(builder, matriarchUrl);

        instance.getVolumeMounts().forEach(v ->
                builder.addMounts(Api.Mount.newBuilder()
                        .setHostPath(v.hostPath())
                        .setContainerPath(v.mountPath())
                        .build()));

        return builder.build();
    }

    private static void addOtlpEnvs(Api.ContainerConfig.Builder builder, String matriarchUrl) {
        String otlpEndpoint = resolveOtlpEndpoint(matriarchUrl);
        if (otlpEndpoint != null)
            builder.addEnvs(Api.KeyValue.newBuilder().setKey("OTLP_ENDPOINT").setValue(otlpEndpoint).build());
        String otlpAuth = Config.getValue("STACKGRES_LOGS_OTLP_AUTH_HEADER", null);
        if (otlpAuth != null && !otlpAuth.isBlank())
            builder.addEnvs(Api.KeyValue.newBuilder().setKey("OTLP_AUTH_HEADER").setValue(otlpAuth).build());
    }

    private static String resolveOtlpEndpoint(String matriarchUrl) {
        String explicit = Config.getValue("STACKGRES_LOGS_OTLP_ENDPOINT", null);
        if (explicit != null && !explicit.isBlank())
            return explicit;
        if (!Boolean.parseBoolean(Config.getValue("STACKGRES_LOGS_OTLP_ENABLED", "true")))
            return null;
        int colon = matriarchUrl.lastIndexOf(':');
        String host = (colon > 0) ? matriarchUrl.substring(0, colon) : matriarchUrl;
        return "https://" + host + ":4319/v1/logs";
    }

    public static Api.PodSandboxConfig etcdPodSandboxConfig(SlonyLinuxHAInstance instance) {
        Api.PodSandboxConfig.Builder builder = Api.PodSandboxConfig.newBuilder()
                .setMetadata(Api.PodSandboxMetadata.newBuilder()
                        .setName("etcd-" + instance.getId().toString())
                        .setUid("etcd-" + instance.getId().toString())
                        .setNamespace("stackgres")
                        .build())
                .putLabels(Cri.LABEL_MANAGED_BY, "stackgres")
                .putLabels(Cri.LABEL_COMPONENT, "etcd")
                .setLinux(Api.LinuxPodSandboxConfig.newBuilder()
                        .setSecurityContext(Api.LinuxSandboxSecurityContext.newBuilder()
                                .setNamespaceOptions(Api.NamespaceOption.newBuilder()
                                        .setNetworkValue(Api.NamespaceMode.NODE_VALUE)
                                        .build())
                                .build()))
                .setLogDirectory(instance.getLogDir().toString());
        return builder.build();
    }

    public static Api.ContainerConfig etcdContainerConfig(PostgresCluster cluster, SlonyLinuxHAInstance instance, String logFileName, Path etcdDataPath) {
        String etcdName = instance.getEtcdName();
        String etcdClientUrl = instance.getEtcdClientUrl();
        String etcServerUrl = instance.getEtcdServerUrl();
        String initialCluster = cluster.getEtcdInitialCluster().entrySet().stream()
                .map(e -> e.getKey() + "=" + "http://" + e.getValue())
                .collect(Collectors.joining(","));
        return Api.ContainerConfig.newBuilder()
                .setMetadata(Api.ContainerMetadata.newBuilder().setName("etcd").build())
                .setImage(Api.ImageSpec.newBuilder().setImage(Cri.ETCD_IMAGE).build())
                .setWorkingDir("/")
                .addAllCommand(List.of(
                        "/usr/local/bin/etcd",
                        "--name", etcdName,
                        "--data-dir", "/etcd-data",
                        "--advertise-client-urls", "http://" + etcdClientUrl,
                        "--listen-client-urls", "http://" + etcdClientUrl,
                        "--initial-advertise-peer-urls", "http://" + etcServerUrl,
                        "--listen-peer-urls", "http://" + etcServerUrl,
                        "--initial-cluster", initialCluster,
                        "--log-outputs", "/tmp/etcd-logs/etcd.log",
                        "--log-rotation-config-json", "{\"maxsize\":10,\"maxage\":3,\"maxbackups\":1,\"compress\":true}"
                ))
                .setLogPath(logFileName)
                .addMounts(Api.Mount.newBuilder()
                        .setHostPath(instance.getLogDir().resolve("etcd").toString())
                        .setContainerPath("/tmp/etcd-logs")
                        .build())

                // TODO
//                .addMounts(Api.Mount.newBuilder()
//                        .setHostPath(etcdDataPath.toString())
//                        .setContainerPath("/etcd-data")
//                        .build())
                .build();
    }

    public static Api.ContainerConfig haPostgresContainerConfig(PostgresCluster cluster, SlonyLinuxHAInstance instance, String localIpAddress, int patroniRestApiPort, String logFileName) {
        String patroniName = instance.getEtcdName().replace("etcd", "patroni");
        List<String> commands = new ArrayList<>(List.of("slon", instance.getId().toString(), String.valueOf(instance.getPort())));
        String matriarchUrl = Config.getValue("STACKGRES_ENDPOINT_URL", "localhost:50051");
        String token = Config.getValue("STACKGRES_TOKEN", null);

        Api.ContainerConfig.Builder builder = Api.ContainerConfig.newBuilder()
                .setMetadata(Api.ContainerMetadata.newBuilder().setName("postgres").build())
                .setImage(Api.ImageSpec.newBuilder().setImage(instance.imageRef(cluster.getExtensions(), cluster.getFlavor())).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_CLUSTER_ID").setValue(cluster.getId().toString()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_CLUSTER_NAME").setValue(cluster.getName()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_INSTANCE_NAME").setValue(instance.getName()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("POSTGRES_USER").setValue(cluster.getUsername()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("POSTGRES_PASSWORD").setValue(cluster.getPassword()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("POSTGRES_LISTEN_ADDRESS").setValue(instance.getListenAddress()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_SUPERUSER_USERNAME").setValue(cluster.getUsername()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_SUPERUSER_PASSWORD").setValue(cluster.getPassword()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_REPLICATION_PASSWORD").setValue(cluster.getPassword()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_REWIND_PASSWORD").setValue(cluster.getPassword()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_NAME").setValue(patroniName).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_SCOPE").setValue(cluster.getName()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_POSTGRESQL_CONNECT_ADDRESS").setValue(localIpAddress + ":" + instance.getPort()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_POSTGRESQL_LISTEN").setValue(instance.getListenAddress() + ":" + instance.getPort()).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_RESTAPI_CONNECT_ADDRESS").setValue(localIpAddress + ":" + patroniRestApiPort).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_RESTAPI_LISTEN").setValue("0.0.0.0:" + patroniRestApiPort).build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_ETCD3_HOSTS").setValue(instance.getEtcdClientUrl()).build())
//                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_LOG_LEVEL").setValue("INFO").build())
                .addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_ENDPOINT_URL").setValue(matriarchUrl).build())

//                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_LOG_DIR").setValue("/var/log/").build())
//                .addEnvs(Api.KeyValue.newBuilder().setKey("PATRONI_LOG_DIR").setValue("/tmp/").build())
                // for psql
                .addEnvs(Api.KeyValue.newBuilder().setKey("PGHOST").setValue("/tmp").build())
                .setWorkingDir("/")
                .addAllCommand(commands)
                .setLogPath(logFileName);

        // TODO
//                .addMounts(Api.Mount.newBuilder()
//                        .setHostPath(instance.getDataDir().toString())
//                        .setContainerPath("/var/lib/postgresql/data")
//                        .build());

        if (cluster.getFlavor() == Flavor.IVORY_SQL && instance.getIvorySqlPort() != null)
            builder.addEnvs(Api.KeyValue.newBuilder().setKey("IVORYSQL_PORT").setValue(String.valueOf(instance.getIvorySqlPort())).build());

        if (token != null)
            builder.addEnvs(Api.KeyValue.newBuilder().setKey("STACKGRES_TOKEN").setValue(token).build());

        addOtlpEnvs(builder, matriarchUrl);

        builder.addMounts(Api.Mount.newBuilder()
                .setHostPath(instance.getLogDir().resolve("etcd").toString())
                .setContainerPath("/tmp/etcd-logs")
                .build());

        instance.getVolumeMounts().forEach(v ->
                builder.addMounts(Api.Mount.newBuilder()
                        .setHostPath(v.hostPath())
                        .setContainerPath(v.mountPath())
                        .build()));

        return builder.build();
    }

}