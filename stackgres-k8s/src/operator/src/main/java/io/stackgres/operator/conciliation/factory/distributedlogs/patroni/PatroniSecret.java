/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.patroni.StackGresRandomPasswordKeys;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V12)
public class PatroniSecret implements
    ResourceGenerator<StackGresDistributedLogsContext>, StackGresRandomPasswordKeys {

  private LabelFactoryForCluster<StackGresDistributedLogs> factoryFactory;

  public static String name(StackGresDistributedLogsContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getSource().getMetadata().getName());
  }

  private static String generatePassword() {
    return ResourceUtil.encodeSecret(UUID.randomUUID().toString().substring(4, 22));
  }

  /**
   * Create the Secret for patroni associated to the cluster.
   */
  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = factoryFactory.genericLabels(cluster);

    Map<String, String> generatedPasswords = context.getDatabaseCredentials()
        .map(Secret::getData)
        .orElse(Map.of());

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
  public void setFactoryFactory(LabelFactoryForCluster<StackGresDistributedLogs> factoryFactory) {
    this.factoryFactory = factoryFactory;
  }

}
