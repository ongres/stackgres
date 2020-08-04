/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.KubernetesClientFactory;

@ApplicationScoped
public class PersistentVolumeClaimFinder implements
    ResourceFinder<PersistentVolumeClaim>,
    ResourceScanner<PersistentVolumeClaim> {

  private final KubernetesClientFactory kubClientFactory;

  @Inject
  public PersistentVolumeClaimFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  public PersistentVolumeClaimFinder() {
    this.kubClientFactory = null;
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  public Optional<PersistentVolumeClaim> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<PersistentVolumeClaim> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return Optional.ofNullable(client.persistentVolumeClaims()
          .inNamespace(namespace).withName(name).get());
    }
  }

  @Override
  public List<PersistentVolumeClaim> findResources() {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.persistentVolumeClaims().inAnyNamespace().list().getItems();
    }
  }

  @Override
  public List<PersistentVolumeClaim> findResourcesInNamespace(String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.persistentVolumeClaims().inNamespace(namespace).list().getItems();
    }
  }

}
