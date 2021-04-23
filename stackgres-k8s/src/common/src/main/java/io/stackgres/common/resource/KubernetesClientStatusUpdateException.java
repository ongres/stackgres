/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class KubernetesClientStatusUpdateException extends KubernetesClientException {

  private static final long serialVersionUID = 1L;

  private final KubernetesClientException cause;

  public KubernetesClientStatusUpdateException(KubernetesClientException cause) {
    super(cause.getMessage(), cause);
    this.cause = cause;
  }

  @Override
  public Status getStatus() {
    return cause.getStatus();
  }

  @Override
  public int getCode() {
    return cause.getCode();
  }

}
