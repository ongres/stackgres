package io.stackgres.cli;

import io.stackgres.postgres.*;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class YamlParserTest {

    private final YamlParser yamlParser = new YamlParser();

    @Test
    void test_can_handle_iterable() {
        Iterable<?> values = new Yaml().loadAll("""
                name: test123
                version: 123
                ---
                ---
                name: test234
                version: 234
                ---
                """);

        List<Object> maps = new ArrayList<>();
        for (Object value : values)
            maps.add(value);

        assertThat(maps).containsExactly(
                Map.of("name", "test123", "version", 123),
                null,
                Map.of("name", "test234", "version", 234),
                null
        );
    }

    @Test
    void should_map_standalone_cluster() {
        PostgresCluster cluster = yamlParser.parseYaml("""
                name: 'test'
                version: '15.3'
                listenAddress: '0.0.0.0'
                port: 5432
                node: '6a1dc06c-b9c1-47c2-8382-9d827d831349'
                configDir: /tmp/pgdata/config
                dataDir: /tmp/pgdata
                logsDir: /tmp/logs
                walDir: /tmp/pgdata/pg_wal
                username: 'test'
                password: 'test123'
                tags:
                  hello: world
                  env: prod
                extensions:
                - age@1.5.0
                - vector
                highAvailability: false
                volumeMounts:
                - /tmp/foo/:/foo/
                - /tmp/foo.txt:/foo.txt
                """);

        assertThat(cluster.getInstances()).hasSize(1);
        SlonyLinuxInstance instance = (SlonyLinuxInstance) cluster.getInstances().iterator().next();
        assertThat(cluster.getName()).isEqualTo("test");
        assertThat(cluster.isStandalone()).isTrue();
        assertThat(cluster.getUsername()).isEqualTo("test");
        assertThat(cluster.getPassword()).isEqualTo("test123");
        assertThat(cluster.getTags()).containsExactlyInAnyOrderEntriesOf(Map.of("hello", "world", "env", "prod"));
        assertThat(cluster.getExtensions()).containsExactly(new Extension("age", "1.5.0", null), new Extension("vector", null, null));

        assertThat(instance.getVersion()).isEqualTo("15.3");
        assertThat(instance.getPort()).isEqualTo(5432);
        assertThat(instance.getListenAddress()).isEqualTo("0.0.0.0");
        assertThat(instance.getSlonyId()).isEqualTo(UUID.fromString("6a1dc06c-b9c1-47c2-8382-9d827d831349"));
        assertThat(instance.getConfigPath()).isEqualTo(Paths.get("/tmp/pgdata/config"));
        assertThat(instance.getDataDir()).isEqualTo(Paths.get("/tmp/pgdata"));
        assertThat(instance.getLogDir()).isEqualTo(Paths.get("/tmp/logs"));
        assertThat(instance.getWalDir()).isEqualTo(Paths.get("/tmp/pgdata/pg_wal"));
        assertThat(instance.getVolumeMounts()).containsExactly(new VolumeMount("/tmp/foo/", "/foo/"), new VolumeMount("/tmp/foo.txt", "/foo.txt"));
    }

    @Test
    void should_map_standalone_cluster_override_instance_values() {
        PostgresCluster cluster = yamlParser.parseYaml("""
                listenAddress: 'overridden'
                port: 5432
                node: '351b9269-1f88-4b4e-a1eb-7d69672cf0ca'
                configDir: /tmp/overridden
                dataDir: /tmp/overridden
                logsDir: /tmp/overridden
                walDir: /tmp/overridden
                volumeMounts:
                - /tmp/first/:/tmp/first/
                instances:
                - listenAddress: '0.0.0.0'
                  port: 5431
                  node: '6a1dc06c-b9c1-47c2-8382-9d827d831349'
                  configDir: /tmp/pgdata/config
                  dataDir: /tmp/pgdata
                  logsDir: /tmp/logs
                  walDir: /tmp/pgdata/pg_wal
                  volumeMounts:
                  - /tmp/second/:/second/
                """);

        assertThat(cluster.getInstances()).hasSize(1);
        SlonyLinuxInstance instance = (SlonyLinuxInstance) cluster.getInstances().iterator().next();
        assertThat(cluster.isStandalone()).isFalse();

        assertThat(instance.getVersion()).isNull();
        assertThat(instance.getPort()).isEqualTo(5431);
        assertThat(instance.getListenAddress()).isEqualTo("0.0.0.0");
        assertThat(instance.getSlonyId()).isEqualTo(UUID.fromString("6a1dc06c-b9c1-47c2-8382-9d827d831349"));
        assertThat(instance.getConfigPath()).isEqualTo(Paths.get("/tmp/pgdata/config"));
        assertThat(instance.getDataDir()).isEqualTo(Paths.get("/tmp/pgdata"));
        assertThat(instance.getLogDir()).isEqualTo(Paths.get("/tmp/logs"));
        assertThat(instance.getWalDir()).isEqualTo(Paths.get("/tmp/pgdata/pg_wal"));
        assertThat(instance.getVolumeMounts()).containsExactly(new VolumeMount("/tmp/first/", "/tmp/first/"), new VolumeMount("/tmp/second/", "/second/"));
    }

    @Test
    void should_map_ha_cluster() {
        PostgresCluster cluster = yamlParser.parseYaml("""
                highAvailability: true
                """);

        assertThat(cluster.isStandalone()).isFalse();
        assertThat(cluster.getInstances()).hasSize(1);
        SlonyLinuxHAInstance instance = (SlonyLinuxHAInstance) cluster.getInstances().iterator().next();
        assertThat(instance.getVersion()).isNull();
    }

    @Test
    void should_imply_ha_cluster_multiple_instances() {
        PostgresCluster cluster = yamlParser.parseYaml("""
                instances:
                - listenAddress: '0.0.0.0'
                - {}
                """);

        assertThat(cluster.isStandalone()).isFalse();
        assertThat(cluster.getInstances()).hasSize(2);
        assertThat(cluster.getInstances()).areExactly(1, new Condition<>(i -> "0.0.0.0".equals(i.getListenAddress()), null));
    }

    @Test
    void should_map_default_values() {
        PostgresCluster cluster = yamlParser.parseYaml("""
                name: 'test'
                """);

        assertThat(cluster.getName()).isEqualTo("test");
        assertThat(cluster.isStandalone()).isTrue();
        assertThat(cluster.getTags()).isEmpty();
        assertThat(cluster.getExtensions()).isEmpty();
        assertThat(cluster.getFlavor()).isEqualTo(Flavor.POSTGRES);
        assertThat(cluster.getInstances()).hasSize(1);
        SlonyLinuxInstance instance = (SlonyLinuxInstance) cluster.getInstances().iterator().next();
        assertThat(instance.getVersion()).isNull();
    }

    @Test
    void should_map_flavor() {
        PostgresCluster cluster = yamlParser.parseYaml("""
                name: 'test'
                flavor: 'ivorysql'
                """);

        assertThat(cluster.getFlavor()).isEqualTo(Flavor.IVORY_SQL);
    }

    @Test
    void should_reject_unknown_flavor() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: 'test'
                        flavor: 'mysql'
                        """))
                .withMessageContaining("Unknown flavor");
    }

    @Test
    void should_map_ivory_sql_port() {
        PostgresCluster cluster = yamlParser.parseYaml("""
                name: 'test'
                flavor: 'ivorysql'
                ivorySqlPort: 1521
                """);

        assertThat(cluster.getInstances()).hasSize(1);
        assertThat(cluster.getInstances().iterator().next().getIvorySqlPort()).isEqualTo(1521);
    }

    @Test
    void should_reject_ivory_sql_port_for_postgres_flavor() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: 'test'
                        ivorySqlPort: 1521
                        """))
                .withMessageContaining("ivorySqlPort");
    }

    @Test
    void should_verify_name_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: 123
                        """))
                .withMessageContaining("Expected YAML property 'name' to be a string.");
    }

    @Test
    void should_verify_version_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        version: 15.3
                        """))
                .withMessageContaining("Expected YAML property 'version' to be a string.");
    }

    @Test
    void should_verify_port_integer() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        port: a123
                        """))
                .withMessageContaining("Expected YAML property 'port' to be an integer.");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        port: 5432
                        instances:
                        - port: a123
                        """))
                .withMessageContaining("Expected YAML property 'port' to be an integer.");
    }

    @Test
    void should_verify_listenAddress_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        listenAddress: 123
                        """))
                .withMessageContaining("Expected YAML property 'listenAddress' to be a string.");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        listenAddress: '0.0.0.0'
                        instances:
                        - listenAddress: 123
                        """))
                .withMessageContaining("Expected YAML property 'listenAddress' to be a string.");
    }

    @Test
    void should_verify_username_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        username: 123
                        """))
                .withMessageContaining("Expected YAML property 'username' to be a string.");
    }

    @Test
    void should_verify_password_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        password: true
                        """))
                .withMessageContaining("Expected YAML property 'password' to be a string.");
    }

    @Test
    void should_verify_config_dir_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        configDir: no
                        """))
                .withMessageContaining("Expected YAML property 'configDir' to be a string.");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        configDir: /tmp/
                        instances:
                        - configDir: no
                        """))
                .withMessageContaining("Expected YAML property 'configDir' to be a string.");
    }

    @Test
    void should_verify_data_dir_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        dataDir: no
                        """))
                .withMessageContaining("Expected YAML property 'dataDir' to be a string.");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        dataDir: /tmp/pgdata
                        instances:
                        - dataDir: no
                        """))
                .withMessageContaining("Expected YAML property 'dataDir' to be a string.");
    }

    @Test
    void should_verify_logs_dir_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        logsDir: true
                        """))
                .withMessageContaining("Expected YAML property 'logsDir' to be a string.");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        logsDir: /tmp/logs
                        instances:
                        - logsDir: true
                        """))
                .withMessageContaining("Expected YAML property 'logsDir' to be a string.");
    }

    @Test
    void should_verify_wal_dir_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        walDir: 123
                        """))
                .withMessageContaining("Expected YAML property 'walDir' to be a string.");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: test
                        walDir: /tmp/pg_wal
                        instances:
                        - walDir: 123
                        """))
                .withMessageContaining("Expected YAML property 'walDir' to be a string.");
    }

    @Test
    void should_verify_extension_list() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: 'test'
                        extensions: vector
                        """))
                .withMessageContaining("Expected YAML property 'extensions' to be a list.");
    }

    @Test
    void should_verify_tags_map() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: 'test'
                        tags: vector
                        """))
                .withMessageContaining("Expected YAML property 'tags' to be an object.");
    }

    @Test
    void should_verify_tags_values() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: 'test'
                        tags:
                          hello: world
                          key: 123
                        """))
                .withMessageContaining("Expected value '123' in YAML object 'key' (in map 'tags') to be a string.");
    }

    @Test
    void should_verify_extension_elements() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> yamlParser.parseYaml("""
                        name: 'test'
                        extensions: [vector, xxhash, 123]
                        """))
                .withMessage("Expected element '123' of YAML list 'extensions' to be a string.");
    }

    @Test
    void should_parse_empty_yaml() {
        PostgresCluster cluster = yamlParser.parseYaml("""
                # empty
                """);

        assertThat(cluster.getName()).isNull();
        assertThat(cluster.getTags()).isEmpty();
        assertThat(cluster.getExtensions()).isEmpty();
        assertThat(cluster.getInstances()).hasSize(1);
        SlonyLinuxInstance instance = (SlonyLinuxInstance) cluster.getInstances().iterator().next();
        assertThat(instance.getVersion()).isNull();
        assertThat(instance.getPort()).isNull();
    }

    @Test
    void should_ignore_unknown_and_ignored_fields() {
        PostgresCluster cluster = yamlParser.parseYaml("""
                name: 'test'
                unknown1: value1
                unknown2: 123
                unknown3: true
                unknown4:
                  nested: value
                instances:
                - etcdName: foobar
                - etcdClientUrl: http://localhost:2379
                - etcdServerUrl: http://localhost:2380
                """);

        assertThat(cluster.getName()).isEqualTo("test");
        SlonyLinuxHAInstance instance = (SlonyLinuxHAInstance) cluster.getInstances().iterator().next();
        assertThat(instance.getVersion()).isNull();
        assertThat(instance.getEtcdName()).isNull();
        assertThat(instance.getEtcdServerUrl()).isNull();
        assertThat(instance.getEtcdClientUrl()).isNull();
    }

}