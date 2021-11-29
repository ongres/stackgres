/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer
class StackGresKubernetesClientTest {

  String randomFieldManager = StringUtils.getRandomString(6);
  String randomNamespace = StringUtils.getRandomNamespace();
  String randomName = StringUtils.getRandomClusterName();

  @Test
  void givenARole_ShouldProduceAValidUrl() throws MalformedURLException {

    final StackGresDefaultKubernetesClient stackGresKubernetesClient
        = new StackGresDefaultKubernetesClient();
    var actual = stackGresKubernetesClient.getResourceUrl(new PatchContext.Builder()
            .withFieldManager(randomFieldManager).withForce(true).build(),
        new RoleBuilder()
            .withNewMetadata()
            .withName(randomName)
            .withNamespace(randomNamespace)
            .endMetadata()
            .build()
    );

    var expectedUrl = new URL(stackGresKubernetesClient.getMasterUrl(),
        "/apis/rbac.authorization.k8s.io/v1/namespaces/"
            + randomNamespace + "/roles/" + randomName + getUrlOptions());
    assertEquals(expectedUrl, actual);

    stackGresKubernetesClient.close();
  }

  @Test
  void givenACronJob_ShouldProduceAValidUrl() throws MalformedURLException {

    final StackGresDefaultKubernetesClient stackGresKubernetesClient
        = new StackGresDefaultKubernetesClient();
    var actual = stackGresKubernetesClient.getResourceUrl(new PatchContext.Builder()
            .withFieldManager(randomFieldManager).withForce(true).build(),
        new CronJobBuilder()
            .withNewMetadata()
            .withName(randomName)
            .withNamespace(randomNamespace)
            .endMetadata()
            .build()
    );

    var expectedUrl = new URL(stackGresKubernetesClient.getMasterUrl(),
        "/apis/batch/v1beta1/namespaces/"
            + randomNamespace + "/cronjobs/" + randomName + getUrlOptions());
    assertEquals(expectedUrl, actual);

    stackGresKubernetesClient.close();
  }

  @Test
  void givenAService_shouldProduceAValidUrl() throws MalformedURLException {

    final StackGresDefaultKubernetesClient stackGresKubernetesClient
        = new StackGresDefaultKubernetesClient();

    var actual = stackGresKubernetesClient.getResourceUrl(new PatchContext.Builder()
            .withFieldManager(randomFieldManager).withForce(true).build(),
        new ServiceBuilder()
            .withNewMetadata()
            .withName(randomName)
            .withNamespace(randomNamespace)
            .endMetadata()
            .build()
    );

    var expectedUrl = new URL(stackGresKubernetesClient.getMasterUrl(),
        "/api/v1/namespaces/"
            + randomNamespace + "/services/" + randomName + getUrlOptions());
    assertEquals(expectedUrl, actual);

    stackGresKubernetesClient.close();

  }

  @Test
  void givenAManagedListObject_shouldSuccessfullyParseIt() throws JsonProcessingException {
    try (final StackGresDefaultKubernetesClient stackGresKubernetesClient
        = new StackGresDefaultKubernetesClient()) {
      ObjectNode list = JsonUtil.readFromJsonAsJson("statefulset/k8s-sts-list-response.json");

      var resources = stackGresKubernetesClient.parseListObject(list,
          StatefulSet.class,
          ResourceWriter.STACKGRES_FIELD_MANAGER);

      assertEquals(list.get("items").size(), resources.size());
    }
  }

  @Test
  void givenAnEmptyListObject_shouldNotFail() throws JsonProcessingException {
    try (final StackGresDefaultKubernetesClient stackGresKubernetesClient
        = new StackGresDefaultKubernetesClient()) {
      ObjectNode list = (ObjectNode) Serialization.jsonMapper().readTree("{\n"
          + "  \"kind\" : \"StatefulSetList\",\n"
          + "  \"apiVersion\" : \"apps/v1\",\n"
          + "  \"metadata\" : {\n"
          + "    \"resourceVersion\" : \"52518\"\n"
          + "  },\n"
          + "  \"items\" : [ ]\n"
          + "}\n");

      var sts = stackGresKubernetesClient.parseListObject(list,
          StatefulSet.class,
          ResourceWriter.STACKGRES_FIELD_MANAGER);

      assertEquals(0, sts.size());
    }
  }

  private String getUrlOptions() {
    return "?fieldManager=" + randomFieldManager
        + "&force=true";
  }
}
