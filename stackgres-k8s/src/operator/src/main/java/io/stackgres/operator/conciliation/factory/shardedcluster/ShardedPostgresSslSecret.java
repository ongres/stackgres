/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterForCitusUtil.CERTIFICATE_KEY;
import static io.stackgres.common.StackGresShardedClusterForCitusUtil.PRIVATE_KEY_KEY;
import static io.stackgres.common.StackGresShardedClusterForCitusUtil.postgresSslSecretName;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.common.CryptoUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder
public class ShardedPostgresSslSecret
    implements ResourceGenerator<StackGresShardedClusterContext> {

  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedPostgresSslSecret(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    final StackGresShardedCluster cluster = context.getSource();
    final String name = postgresSslSecretName(context.getSource());
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(cluster);

    final Map<String, String> data = new HashMap<>();

    if (Optional.of(context.getSource())
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false)) {
      setCertificateAndPrivateKey(context, data);
    }

    return Stream.of(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withType("Opaque")
        .withData(ResourceUtil.encodeSecret(StackGresUtil.addMd5Sum(data)))
        .build());
  }

  private void setCertificateAndPrivateKey(StackGresShardedClusterContext context,
      Map<String, String> data) {
    var certificate = context.getPostgresSslCertificate();
    var privateKey = context.getPostgresSslPrivateKey();
    if (certificate.isEmpty() || privateKey.isEmpty()) {
      var certificateAndPrivateKey = CryptoUtil.generateCertificateAndPrivateKey();
      certificate = Optional.of(certificateAndPrivateKey.v1);
      privateKey = Optional.of(certificateAndPrivateKey.v2);
    }
    data.put(CERTIFICATE_KEY, certificate.orElseThrow());
    data.put(PRIVATE_KEY_KEY, privateKey.orElseThrow());
  }

}
