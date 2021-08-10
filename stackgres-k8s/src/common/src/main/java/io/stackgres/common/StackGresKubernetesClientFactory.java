/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public interface StackGresKubernetesClientFactory extends KubernetesClientFactory {

  @Override
  StackGresKubernetesClient create();

}
