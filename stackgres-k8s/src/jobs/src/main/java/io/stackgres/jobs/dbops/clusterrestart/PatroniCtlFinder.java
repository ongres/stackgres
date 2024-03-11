/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniCtl.PatroniCtlInstance;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class PatroniCtlFinder {

  @Inject
  KubernetesClient client;

  @Inject
  PatroniCtl patroniCtl;

  public PatroniCtlInstance findPatroniCtl(
      String clusterName, String namespace) {
    var cluster = findCluster(clusterName, namespace);
    return patroniCtl.instanceFor(cluster);
  }

  StackGresCluster findCluster(String clusterName, String namespace) {
    return Optional.ofNullable(client.resources(StackGresCluster.class)
        .inNamespace(namespace)
        .withName(clusterName)
        .get())
        .orElseThrow(() -> new RuntimeException("Can not find SGCluster " + clusterName));
  }

  public Tuple2<String, String> getSuperuserCredentials(String clusterName, String namespace) {
    return Optional.ofNullable(client.secrets()
        .inNamespace(namespace)
        .withName(PatroniUtil.secretName(clusterName))
        .get())
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .map(date -> Tuple.tuple(
            Optional.ofNullable(date.get(StackGresPasswordKeys.SUPERUSER_USERNAME_KEY))
            .orElseThrow(() -> new RuntimeException("Can not find key "
                + StackGresPasswordKeys.SUPERUSER_USERNAME_KEY
                + " in Secret " + PatroniUtil.secretName(clusterName))),
            Optional.ofNullable(date.get(StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY))
            .orElseThrow(() -> new RuntimeException("Can not find key "
                + StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY
                + " in Secret " + PatroniUtil.secretName(clusterName)))))
        .orElseThrow(() -> new RuntimeException(
            "Can not find Secret " + PatroniUtil.secretName(clusterName)));
  }

  public Tuple2<String, String> getPatroniCredentials(String clusterName, String namespace) {
    return Optional.ofNullable(client.secrets()
        .inNamespace(namespace)
        .withName(PatroniUtil.secretName(clusterName))
        .get())
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .map(date -> Tuple.tuple(
            Optional.ofNullable(date.get(StackGresPasswordKeys.RESTAPI_USERNAME_KEY))
            .orElseThrow(() -> new RuntimeException("Can not find key "
                + StackGresPasswordKeys.RESTAPI_USERNAME_KEY
                + " in Secret " + PatroniUtil.secretName(clusterName))),
            Optional.ofNullable(date.get(StackGresPasswordKeys.RESTAPI_PASSWORD_KEY))
            .orElseThrow(() -> new RuntimeException("Can not find key "
                + StackGresPasswordKeys.RESTAPI_PASSWORD_KEY
                + " in Secret " + PatroniUtil.secretName(clusterName)))))
        .orElseThrow(() -> new RuntimeException(
            "Can not find Secret " + PatroniUtil.secretName(clusterName)));
  }

}
