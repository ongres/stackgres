/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniSecret implements StackGresClusterResourceStreamFactory {

  public static final String SUPERUSER_PASSWORD_KEY = "superuser-password";

  private LabelFactoryDelegator factoryDelegator;

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName());
  }

  /**
   * Create the Secret for patroni associated to the cluster.
   */
  @Override
  public Stream<HasMetadata> streamResources(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = factoryDelegator.pickFactory(context)
        .clusterLabels(cluster);

    Map<String, String> data = new HashMap<>();
    data.put(SUPERUSER_PASSWORD_KEY, generatePassword());
    data.put("replication-password", generatePassword());
    data.put("authenticator-password", generatePassword());
    data.put("restapi-password", generatePassword());

    return Seq.of(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .withType("Opaque")
        .withData(data)
        .build());

  }

  private static String generatePassword() {
    return ResourceUtil.encodeSecret(StringUtil.generateRandom(32));
  }

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
