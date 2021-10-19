/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;

@ApplicationScoped
public class PersistentVolumeClaimFinder implements
    ResourceFinder<PersistentVolumeClaim>,
    ResourceScanner<PersistentVolumeClaim> {

  private final KubernetesClient client;

  @Inject
  public PersistentVolumeClaimFinder(KubernetesClient client) {
    this.client = client;
  }

  public PersistentVolumeClaimFinder() {
    this.client = null;
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  public Optional<PersistentVolumeClaim> findByName(String name) {
    return Optional.ofNullable(client.persistentVolumeClaims()
        .withName(name).get());
  }

  @Override
  public Optional<PersistentVolumeClaim> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.persistentVolumeClaims()
        .inNamespace(namespace).withName(name).get());
  }

  @Override
  public List<PersistentVolumeClaim> findResources() {
    return client.persistentVolumeClaims().inAnyNamespace().list().getItems();
  }

  @Override
  public List<PersistentVolumeClaim> findResourcesInNamespace(String namespace) {
    return client.persistentVolumeClaims().inNamespace(namespace).list().getItems();
  }

  @Override
  public List<PersistentVolumeClaim> findByLabelsAndNamespace(String namespace,
      Map<String, String> labels) {
    return client.persistentVolumeClaims().inNamespace(namespace)
        .withLabels(labels).list().getItems();
  }

}
