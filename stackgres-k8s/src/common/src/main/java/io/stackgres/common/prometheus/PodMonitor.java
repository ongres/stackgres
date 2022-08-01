/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
@Group("monitoring.coreos.com")
@Version("v1")
@Kind(PodMonitor.KIND)
public final class PodMonitor
    extends CustomResource<PodMonitorSpec, Void>
    implements Namespaced {

  private static final long serialVersionUID = 2719099984653736636L;

  public static final String KIND = "PodMonitor";

  public PodMonitor() {
    super();
  }

  // TODO: remove on update to Kubernetes-Client 5.2.0
  @Override
  protected Void initStatus() {
    return null;
  }

}
