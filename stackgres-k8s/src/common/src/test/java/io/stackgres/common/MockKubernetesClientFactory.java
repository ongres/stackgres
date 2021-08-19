/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.inject.Produces;

import io.quarkus.test.Mock;

@Mock
public class MockKubernetesClientFactory {

  @Produces
  StackGresKubernetesClientFactory buildClientFactory() {
    return StackGresDefaultKubernetesClient::new;
  }
}
