/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgbouncer;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ResourceUtils;
import io.stackgres.sidecars.Sidecar;

public class PgBouncer implements Sidecar {

  private static final String NAME = "pgbouncer";
  private static final String IMAGE = "docker.io/ongres/pgbouncer:1.11";

  private final String clusterName;

  private final String configMapName;

  public PgBouncer(String clusterName) {
    this.clusterName = clusterName;
    this.configMapName = clusterName + "-pgbouncer-config";
  }

  @Override
  public Container create() {
    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")
        .build();

    VolumeMount pgbouncerConfig = new VolumeMountBuilder()
        .withName(NAME)
        .withMountPath("/etc/pgbouncer")
        .withReadOnly(Boolean.TRUE)
        .build();

    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(IMAGE)
        .withImagePullPolicy("Always")
        .withPorts(new ContainerPortBuilder().withContainerPort(6432).build())
        .withVolumeMounts(pgSocket, pgbouncerConfig);

    return container.build();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<HasMetadata> createDependencies() {
    String configFile = "[databases]\n" +
        " * = host=/run/postgresql \n" +
        "\n" +
        "[pgbouncer]\n" +
        " listen_port = 6432\n" +
        " listen_addr = 0.0.0.0\n" +
        " unix_socket_dir = /run/postgresql\n" +
        " auth_type = md5\n" +
        " auth_query = SELECT usename, passwd FROM pg_shadow WHERE usename=$1\n" +
        " admin_users = postgres\n" +
        " user = postgres\n" +
        " pool_mode = session\n" +
        " max_client_conn = 100\n" +
        " default_pool_size = 20\n" +
        " ignore_startup_parameters = extra_float_digits\n" +
        "";
    Map<String, String> data = ImmutableMap.of("pgbouncer.ini", configFile);

    ConfigMap cm = new ConfigMapBuilder()
        .withNewMetadata()
        // .withAnnotations(ImmutableMap.of("VolumeMount", "true"))
        .withName(configMapName)
        .withLabels(ResourceUtils.defaultLabels(clusterName))
        .endMetadata()
        .withData(data)
        .build();

    return ImmutableList.of(cm);
  }


}
