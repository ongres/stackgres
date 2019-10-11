/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgexporter;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.StackgresClusterReview;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfigList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PostgresExporterTest {

  private PostgresExporter exporter = new PostgresExporter();


  private StackGresCluster cluster;

  @Mock
  private KubernetesClient client;

  @BeforeEach
  void setUp(){

    StackgresClusterReview review = JsonUtil.readFromJson(
        "cluster_allow_requests/valid_creation.json",
        StackgresClusterReview.class);
    cluster = review.getRequest().getObject();

  }



  @Test
  void testPrometheusParsing() {

    JsonUtil.readFromJson("prometheus/prometheus_list.json", PrometheusConfigList.class);


  }
}
