/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.Optional;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operatorframework.resource.ResourceUtil;

public abstract class ShardedClusterContextAppenderWithSecrets
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final ResourceFinder<Secret> secretFinder;

  public ShardedClusterContextAppenderWithSecrets(
      ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  public ShardedClusterContextAppenderWithSecrets() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.secretFinder = null;
  }

  protected <T, S> Optional<String> getSecretAndKeyOrThrow(
      final String clusterNamespace,
      final Optional<T> secretSection,
      final Function<T, S> secretKeyRefGetter,
      final Function<S, SecretKeySelector> secretKeySelectorGetter,
      final Function<SecretKeySelector, String> onKeyNotFoundMessageGetter,
      final Function<SecretKeySelector, String> onSecretNotFoundMessageGetter) {
    return secretSection
        .map(secretKeyRefGetter)
        .map(secretKeySelectorGetter)
        .map(secretKeySelector -> secretFinder
            .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
            .flatMap(secret -> getSecretKeyOrThrow(secret, secretKeySelector.getKey(),
                onKeyNotFoundMessageGetter.apply(secretKeySelector)))
            .orElseThrow(() -> new IllegalArgumentException(
                onSecretNotFoundMessageGetter.apply(secretKeySelector))));
  }

  protected <T> Optional<String> getSecretAndKeyOrThrow(
      final String clusterNamespace,
      final Optional<T> credential,
      final Function<T, SecretKeySelector> secretKeySelectorGetter,
      final Function<SecretKeySelector, String> onKeyNotFoundMessageGetter,
      final Function<SecretKeySelector, String> onSecretNotFoundMessageGetter) {
    return credential
        .map(secretKeySelectorGetter)
        .map(secretKeySelector -> secretFinder
            .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
            .flatMap(secret -> getSecretKeyOrThrow(secret, secretKeySelector.getKey(),
                onKeyNotFoundMessageGetter.apply(secretKeySelector)))
            .orElseThrow(() -> new IllegalArgumentException(
                onSecretNotFoundMessageGetter.apply(secretKeySelector))));
  }

  protected Optional<String> getSecretKeyOrThrow(
      final Secret secret,
      final String key,
      final String onKeyNotFoundMessage) {
    return Optional.of(
        Optional.of(secret)
        .map(Secret::getData)
        .map(data -> data.get(key))
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new IllegalArgumentException(onKeyNotFoundMessage)));
  }

  protected <T> Optional<String> getSecretAndKey(
      final String clusterNamespace,
      final Optional<T> credential,
      final Function<T, SecretKeySelector> secretKeySelectorGetter) {
    return credential
        .map(secretKeySelectorGetter)
        .flatMap(secretKeySelector -> secretFinder
            .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
            .flatMap(secret -> getSecretKey(secret, secretKeySelector.getKey())));
  }

  protected Optional<String> getSecretKey(
      final Secret secret,
      final String key) {
    return Optional.of(secret)
        .map(Secret::getData)
        .map(data -> data.get(key))
        .map(ResourceUtil::decodeSecret);
  }

}
