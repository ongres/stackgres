/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ScriptFinder implements CustomResourceFinder<StackGresScript> {

  private final KubernetesClient client;

  @Inject
  public ScriptFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<StackGresScript> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.resources(
        StackGresScript.class,
        StackGresScriptList.class)
        .inNamespace(namespace)
        .withName(name)
        .get());
  }
}
