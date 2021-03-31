/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class PatroniSecret implements
    ResourceGenerator<StackGresClusterContext> {

  public static final String SUPERUSER_PASSWORD_KEY = "superuser-password";
  protected static final String REPLICATION_PASSWORD_KEY = "replication-password";
  protected static final String AUTHENTICATOR_PASSWORD_KEY = "authenticator-password";
  private static final String RESTAPI_PASSWORD_KEY = "restapi-password";

  private LabelFactory<StackGresCluster> factoryFactory;

  private ResourceFinder<Secret> secretFinder;

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getSource().getMetadata().getName());
  }

  public static String name(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName());
  }

  private static String generatePassword() {
    return ResourceUtil.encodeSecret(UUID.randomUUID().toString().substring(4, 22));
  }

  /**
   * Create the Secret for patroni associated to the cluster.
   */
  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = factoryFactory
        .clusterLabels(cluster);

    Map<String, String> generatedPasswords = secretFinder.findByNameAndNamespace(name, namespace)
        .map(Secret::getData).orElse(new HashMap<>());

    Map<String, String> data = new HashMap<>();
    data.put(SUPERUSER_PASSWORD_KEY, generatedPasswords
        .getOrDefault(SUPERUSER_PASSWORD_KEY, generatePassword()));
    data.put(REPLICATION_PASSWORD_KEY, generatedPasswords
        .getOrDefault(REPLICATION_PASSWORD_KEY, generatePassword()));
    data.put(AUTHENTICATOR_PASSWORD_KEY, generatedPasswords
        .getOrDefault(AUTHENTICATOR_PASSWORD_KEY, generatePassword()));
    data.put(RESTAPI_PASSWORD_KEY, generatedPasswords
        .getOrDefault(RESTAPI_PASSWORD_KEY, generatePassword()));

    return Stream.of(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withType("Opaque")
        .withData(data)
        .build());

  }

  @Inject
  public void setFactoryFactory(LabelFactory<StackGresCluster> factoryFactory) {
    this.factoryFactory = factoryFactory;
  }

  @Inject
  public void setSecretFinder(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }
}
