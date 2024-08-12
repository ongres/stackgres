/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigDto;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresConfigTransformerTest {

  PostgresConfigTransformer transformer;

  @BeforeEach
  void setup() {
    var mapper = JsonMapper.builder().build();
    transformer = new PostgresConfigTransformer(mapper);
  }

  @Test
  void emptyConfig_shouldBeTransformerd() {
    List<String> clusters = ImmutableList.of("test");
    StackGresPostgresConfig source = createEmptyConfig();
    PostgresConfigDto transformed = transformer.toResource(source, clusters);
    Assertions.assertNotNull(transformed.getMetadata());
    Assertions.assertEquals("default", transformed.getMetadata().getNamespace());
    Assertions.assertEquals("test", transformed.getMetadata().getName());
    Assertions.assertEquals("test", transformed.getMetadata().getUid());
    Assertions.assertNotNull(transformed.getSpec());
    Assertions.assertEquals("11", transformed.getSpec().getPostgresVersion());
    Assertions.assertNotNull(transformed.getSpec().getPostgresqlConf());
    Assertions.assertEquals("", transformed.getSpec().getPostgresqlConf());
    Assertions.assertNotNull(transformed.getStatus());
    Assertions.assertNotNull(transformed.getStatus().getDefaultParameters());
    Assertions.assertEquals(0, transformed.getStatus().getDefaultParameters().size());
    Assertions.assertNotNull(transformed.getStatus().getPostgresqlConf());
    Assertions.assertEquals(0, transformed.getStatus().getPostgresqlConf().size());
    Assertions.assertNotNull(transformed.getStatus().getClusters());
    Assertions.assertEquals(clusters, transformed.getStatus().getClusters());
  }

  @Test
  void simpleConfig_shouldBeTransformerd() {
    List<String> clusters = ImmutableList.of("test");
    StackGresPostgresConfig source = createEmptyConfig();
    source.getSpec().setPostgresqlConf(ImmutableMap.of("test", "1"));
    PostgresConfigDto transformed = transformer.toResource(source, clusters);
    Assertions.assertNotNull(transformed.getMetadata());
    Assertions.assertEquals("default", transformed.getMetadata().getNamespace());
    Assertions.assertEquals("test", transformed.getMetadata().getName());
    Assertions.assertEquals("test", transformed.getMetadata().getUid());
    Assertions.assertNotNull(transformed.getSpec());
    Assertions.assertEquals("11", transformed.getSpec().getPostgresVersion());
    Assertions.assertNotNull(transformed.getSpec().getPostgresqlConf());
    Assertions.assertEquals("test='1'", transformed.getSpec().getPostgresqlConf());
    Assertions.assertNotNull(transformed.getStatus());
    Assertions.assertNotNull(transformed.getStatus().getDefaultParameters());
    Assertions.assertEquals(0, transformed.getStatus().getDefaultParameters().size());
    Assertions.assertNotNull(transformed.getStatus().getPostgresqlConf());
    Assertions.assertEquals(1, transformed.getStatus().getPostgresqlConf().size());
    Assertions.assertEquals("test",
        transformed.getStatus().getPostgresqlConf().get(0).getParameter());
    Assertions.assertEquals("1", transformed.getStatus().getPostgresqlConf().get(0).getValue());
    Assertions.assertEquals("https://postgresqlco.nf/en/doc/param/test/11/",
        transformed.getStatus().getPostgresqlConf().get(0).getDocumentationLink());
    Assertions.assertNotNull(transformed.getStatus().getClusters());
    Assertions.assertEquals(clusters, transformed.getStatus().getClusters());
  }

  @Test
  void advancedConfig_shouldBeTransformerd() {
    List<String> clusters = ImmutableList.of("test");
    StackGresPostgresConfig source = createEmptyConfig();
    source.getSpec().setPostgresqlConf(ImmutableMap.<String, String>builder()
        .put("test4", "")
        .put("test5", "1")
        .put("test7", "1'")
        .build());
    PostgresConfigDto transformed = transformer.toResource(source, clusters);
    Assertions.assertNotNull(transformed.getMetadata());
    Assertions.assertEquals("default", transformed.getMetadata().getNamespace());
    Assertions.assertEquals("test", transformed.getMetadata().getName());
    Assertions.assertEquals("test", transformed.getMetadata().getUid());
    Assertions.assertNotNull(transformed.getSpec());
    Assertions.assertEquals("11", transformed.getSpec().getPostgresVersion());
    Assertions.assertNotNull(transformed.getSpec().getPostgresqlConf());
    Assertions.assertEquals("test4=''\ntest5='1'\ntest7='1'''",
        transformed.getSpec().getPostgresqlConf());
    Assertions.assertNotNull(transformed.getStatus());
    Assertions.assertNotNull(transformed.getStatus().getDefaultParameters());
    Assertions.assertEquals(0, transformed.getStatus().getDefaultParameters().size());
    Assertions.assertNotNull(transformed.getStatus().getPostgresqlConf());
    Assertions.assertEquals(3, transformed.getStatus().getPostgresqlConf().size());
    Assertions.assertEquals("test4",
        transformed.getStatus().getPostgresqlConf().get(0).getParameter());
    Assertions.assertEquals("", transformed.getStatus().getPostgresqlConf().get(0).getValue());
    Assertions.assertEquals("https://postgresqlco.nf/en/doc/param/test4/11/",
        transformed.getStatus().getPostgresqlConf().get(0).getDocumentationLink());
    Assertions.assertEquals("test5",
        transformed.getStatus().getPostgresqlConf().get(1).getParameter());
    Assertions.assertEquals("1", transformed.getStatus().getPostgresqlConf().get(1).getValue());
    Assertions.assertEquals("https://postgresqlco.nf/en/doc/param/test5/11/",
        transformed.getStatus().getPostgresqlConf().get(1).getDocumentationLink());
    Assertions.assertEquals("test7",
        transformed.getStatus().getPostgresqlConf().get(2).getParameter());
    Assertions.assertEquals("1'", transformed.getStatus().getPostgresqlConf().get(2).getValue());
    Assertions.assertEquals("https://postgresqlco.nf/en/doc/param/test7/11/",
        transformed.getStatus().getPostgresqlConf().get(2).getDocumentationLink());
    Assertions.assertNotNull(transformed.getStatus().getClusters());
    Assertions.assertEquals(clusters, transformed.getStatus().getClusters());
  }

  @Test
  void emptyConfigDto_shouldBeTransformerd() {
    StackGresPostgresConfig original = null;
    PostgresConfigDto source = createEmptyConfigDto();
    StackGresPostgresConfig transformed = transformer.toCustomResource(source, original);
    Assertions.assertNotNull(transformed.getMetadata());
    Assertions.assertEquals("default", transformed.getMetadata().getNamespace());
    Assertions.assertEquals("test", transformed.getMetadata().getName());
    Assertions.assertEquals("test", transformed.getMetadata().getUid());
    Assertions.assertNotNull(transformed.getSpec());
    Assertions.assertEquals("11", transformed.getSpec().getPostgresVersion());
    Assertions.assertNull(transformed.getSpec().getPostgresqlConf());
  }

  @Test
  void simpleConfigDto_shouldBeTransformerd() {
    StackGresPostgresConfig original = createOriginal();
    PostgresConfigDto source = createEmptyConfigDto();
    source.getSpec().setPostgresqlConf("test=1");
    StackGresPostgresConfig transformed = transformer.toCustomResource(source, original);
    Assertions.assertNotNull(transformed.getMetadata());
    Assertions.assertEquals("default", transformed.getMetadata().getNamespace());
    Assertions.assertEquals("test", transformed.getMetadata().getName());
    Assertions.assertEquals("test", transformed.getMetadata().getUid());
    Assertions.assertNotNull(transformed.getSpec());
    Assertions.assertEquals("11", transformed.getSpec().getPostgresVersion());
    Assertions.assertEquals(ImmutableMap.of("test", "1"),
        transformed.getSpec().getPostgresqlConf());
  }

  @Test
  void advancedConfigDto_shouldBeTransformerd() {
    StackGresPostgresConfig original = createOriginal();
    PostgresConfigDto source = createEmptyConfigDto();
    source.getSpec().setPostgresqlConf(
        "" + "\n"
            + " " + "\n"
            + "#test1=1" + "\n"
            + " #test2=1" + "\n"
            + " #test3=1 # " + "\n"
            + "test4=" + "\n"
            + "test5=1" + "\n"
            + "test6=1 " + "\n"
            + "test7=1'" + "\n"
            + "test8=1#2" + "\n"
            + "test9=1 #2" + "\n"
            + "test10=''" + "\n"
            + "test11='1'" + "\n"
            + "test12='1' " + "\n"
            + "test13='1' #2" + "\n"
            + "test14='1''" + "\n"
            + "test15='1'''" + "\n"
            + "test16 1" + "\n");
    StackGresPostgresConfig transformed = transformer.toCustomResource(source, original);
    Assertions.assertNotNull(transformed.getMetadata());
    Assertions.assertEquals("default", transformed.getMetadata().getNamespace());
    Assertions.assertEquals("test", transformed.getMetadata().getName());
    Assertions.assertEquals("test", transformed.getMetadata().getUid());
    Assertions.assertNotNull(transformed.getSpec());
    Assertions.assertEquals("11", transformed.getSpec().getPostgresVersion());
    Assertions.assertNotNull(transformed.getSpec().getPostgresqlConf());
    Assertions.assertEquals(13,
        transformed.getSpec().getPostgresqlConf().size());
    Assertions.assertEquals("", transformed.getSpec().getPostgresqlConf().get("test4"));
    Assertions.assertEquals("1", transformed.getSpec().getPostgresqlConf().get("test5"));
    Assertions.assertEquals("1", transformed.getSpec().getPostgresqlConf().get("test6"));
    Assertions.assertEquals("1'", transformed.getSpec().getPostgresqlConf().get("test7"));
    Assertions.assertEquals("1", transformed.getSpec().getPostgresqlConf().get("test8"));
    Assertions.assertEquals("1", transformed.getSpec().getPostgresqlConf().get("test9"));
    Assertions.assertEquals("", transformed.getSpec().getPostgresqlConf().get("test10"));
    Assertions.assertEquals("1", transformed.getSpec().getPostgresqlConf().get("test11"));
    Assertions.assertEquals("1", transformed.getSpec().getPostgresqlConf().get("test12"));
    Assertions.assertEquals("1", transformed.getSpec().getPostgresqlConf().get("test13"));
    Assertions.assertEquals("1'", transformed.getSpec().getPostgresqlConf().get("test14"));
    Assertions.assertEquals("1'", transformed.getSpec().getPostgresqlConf().get("test15"));
    Assertions.assertEquals("1", transformed.getSpec().getPostgresqlConf().get("test16"));
  }

  @Test
  void realConfigDto_shouldBeTransformerd() throws Exception {
    StackGresPostgresConfig original = createOriginal();
    PostgresConfigDto source = createEmptyConfigDto();
    source.getSpec().setPostgresqlConf(
        IOUtils.resourceToString("/postgresql.conf",
            StandardCharsets.UTF_8));
    StackGresPostgresConfig transformed = transformer.toCustomResource(source, original);
    Assertions.assertNotNull(transformed.getMetadata());
    Assertions.assertEquals("default", transformed.getMetadata().getNamespace());
    Assertions.assertEquals("test", transformed.getMetadata().getName());
    Assertions.assertEquals("test", transformed.getMetadata().getUid());
    Assertions.assertNotNull(transformed.getSpec());
    Assertions.assertEquals("11", transformed.getSpec().getPostgresVersion());
    Assertions.assertNotNull(transformed.getSpec().getPostgresqlConf());
    Assertions.assertEquals(24,
        transformed.getSpec().getPostgresqlConf().size());
    Assertions.assertEquals("/var/lib/postgresql/10/main",
        transformed.getSpec().getPostgresqlConf().get("data_directory"));
    Assertions.assertEquals("/etc/postgresql/10/main/pg_hba.conf",
        transformed.getSpec().getPostgresqlConf().get("hba_file"));
    Assertions.assertEquals("/etc/postgresql/10/main/pg_ident.conf",
        transformed.getSpec().getPostgresqlConf().get("ident_file"));
    Assertions.assertEquals("/var/run/postgresql/10-main.pid",
        transformed.getSpec().getPostgresqlConf().get("external_pid_file"));
    Assertions.assertEquals("5432", transformed.getSpec().getPostgresqlConf().get("port"));
    Assertions.assertEquals("100",
        transformed.getSpec().getPostgresqlConf().get("max_connections"));
    Assertions.assertEquals("/var/run/postgresql",
        transformed.getSpec().getPostgresqlConf().get("unix_socket_directories"));
    Assertions.assertEquals("on", transformed.getSpec().getPostgresqlConf().get("ssl"));
    Assertions.assertEquals("/etc/ssl/certs/ssl-cert-snakeoil.pem",
        transformed.getSpec().getPostgresqlConf().get("ssl_cert_file"));
    Assertions.assertEquals("/etc/ssl/private/ssl-cert-snakeoil.key",
        transformed.getSpec().getPostgresqlConf().get("ssl_key_file"));
    Assertions.assertEquals("128MB",
        transformed.getSpec().getPostgresqlConf().get("shared_buffers"));
    Assertions.assertEquals("posix",
        transformed.getSpec().getPostgresqlConf().get("dynamic_shared_memory_type"));
    Assertions.assertEquals("%m [%p] %q%u@%d ",
        transformed.getSpec().getPostgresqlConf().get("log_line_prefix"));
    Assertions.assertEquals("Europe/Madrid",
        transformed.getSpec().getPostgresqlConf().get("log_timezone"));
    Assertions.assertEquals("10/main",
        transformed.getSpec().getPostgresqlConf().get("cluster_name"));
    Assertions.assertEquals("/var/run/postgresql/10-main.pg_stat_tmp",
        transformed.getSpec().getPostgresqlConf().get("stats_temp_directory"));
    Assertions.assertEquals("iso, dmy", transformed.getSpec().getPostgresqlConf().get("datestyle"));
    Assertions.assertEquals("Europe/Madrid",
        transformed.getSpec().getPostgresqlConf().get("timezone"));
    Assertions.assertEquals("en_US.UTF-8",
        transformed.getSpec().getPostgresqlConf().get("lc_messages"));
    Assertions.assertEquals("es_ES.UTF-8",
        transformed.getSpec().getPostgresqlConf().get("lc_monetary"));
    Assertions.assertEquals("es_ES.UTF-8",
        transformed.getSpec().getPostgresqlConf().get("lc_numeric"));
    Assertions.assertEquals("es_ES.UTF-8",
        transformed.getSpec().getPostgresqlConf().get("lc_time"));
    Assertions.assertEquals("pg_catalog.english",
        transformed.getSpec().getPostgresqlConf().get("default_text_search_config"));
    Assertions.assertEquals("conf.d", transformed.getSpec().getPostgresqlConf().get("include_dir"));
  }

  @Test
  void wrongConfigDto_shouldFail() {
    StackGresPostgresConfig original = createOriginal();
    PostgresConfigDto source = createEmptyConfigDto();
    source.getSpec().setPostgresqlConf("test");
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> transformer.toCustomResource(source, original));
  }

  StackGresPostgresConfig createEmptyConfig() {
    StackGresPostgresConfig source = new StackGresPostgresConfig();
    source.setMetadata(new ObjectMeta());
    source.getMetadata().setNamespace("default");
    source.getMetadata().setName("test");
    source.getMetadata().setUid("test");
    source.setSpec(new StackGresPostgresConfigSpec());
    source.getSpec().setPostgresVersion("11");
    source.getSpec().setPostgresqlConf(ImmutableMap.of());
    source.setStatus(new StackGresPostgresConfigStatus());
    source.getStatus().setDefaultParameters(ImmutableMap.of());
    return source;
  }

  StackGresPostgresConfig createOriginal() {
    StackGresPostgresConfig original = new StackGresPostgresConfig();
    original.setMetadata(new ObjectMeta());
    original.getMetadata().setNamespace("default");
    original.getMetadata().setName("test");
    original.getMetadata().setUid("?");
    return original;
  }

  PostgresConfigDto createEmptyConfigDto() {
    PostgresConfigDto source = new PostgresConfigDto();
    source.getMetadata().setNamespace("default");
    source.getMetadata().setName("test");
    source.getMetadata().setUid("test");
    source.setSpec(new PostgresConfigSpec());
    source.getSpec().setPostgresVersion("11");
    return source;
  }
}
