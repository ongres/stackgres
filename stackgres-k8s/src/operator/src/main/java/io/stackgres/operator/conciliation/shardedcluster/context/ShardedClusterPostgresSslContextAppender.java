/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterPostgresSslContextAppender
    extends ShardedClusterContextAppenderWithSecrets {

  public ShardedClusterPostgresSslContextAppender(
      ResourceFinder<Secret> secretFinder) {
    super(secretFinder);
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    final PostgresSsl postgresSsl = getPostgresSsl(cluster);
    contextBuilder
        .postgresSslCertificate(postgresSsl.certificate)
        .postgresSslPrivateKey(postgresSsl.privateKey);
  }

  record PostgresSsl(
      Optional<String> certificate,
      Optional<String> privateKey) {
  }

  private PostgresSsl getPostgresSsl(
      final StackGresShardedCluster cluster) {
    var ssl = Optional.ofNullable(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl);
    if (ssl.map(StackGresClusterSsl::getEnabled).orElse(false)) {
      if (ssl.map(StackGresClusterSsl::getCertificateSecretKeySelector).isPresent()
          && ssl.map(StackGresClusterSsl::getPrivateKeySecretKeySelector).isPresent()) {
        return new PostgresSsl(
            getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), ssl,
                StackGresClusterSsl::getCertificateSecretKeySelector,
                secretKeySelector -> "Certificate key " + secretKeySelector.getKey()
                + " was not found in secret " + secretKeySelector.getName(),
                secretKeySelector -> "Certificate secret " + secretKeySelector.getName()
                + " was not found"),
            getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), ssl,
                StackGresClusterSsl::getPrivateKeySecretKeySelector,
                secretKeySelector -> "Private key key " + secretKeySelector.getKey()
                + " was not found in secret " + secretKeySelector.getName(),
                secretKeySelector -> "Private key secret " + secretKeySelector.getName()
                + " was not found"));
      }
      return new PostgresSsl(
          getSecretAndKey(cluster.getMetadata().getNamespace(), ssl,
              s -> new SecretKeySelector(
                  PatroniUtil.CERTIFICATE_KEY, StackGresShardedClusterUtil.postgresSslSecretName(cluster))),
          getSecretAndKey(cluster.getMetadata().getNamespace(), ssl,
              s -> new SecretKeySelector(
                  PatroniUtil.PRIVATE_KEY_KEY, StackGresShardedClusterUtil.postgresSslSecretName(cluster))));
    }

    return new PostgresSsl(Optional.empty(), Optional.empty());
  }

}
