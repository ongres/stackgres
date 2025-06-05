/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.metrics.AbstractMetricsTest;

@WithKubernetesTestServer
@QuarkusTest
class MetricsTest extends AbstractMetricsTest {
}
