/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.prometheus;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@Group("monitoring.coreos.com")
@Version("v1")
@Kind("Prometheus")
@Plural("prometheuses")
public final class PrometheusConfig
    extends CustomResource<PrometheusConfigSpec, Void>
    implements Namespaced {

  private static final long serialVersionUID = 1L;

  public PrometheusConfig() {
    super();
  }

  // TODO: remove on update to Kubernetes-Client 5.2.0
  @Override
  protected Void initStatus() {
    return null;
  }

}
