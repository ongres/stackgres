/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgconfig.StackGresConfig;

public interface LabelMapperForConfig
    extends LabelMapper<StackGresConfig> {

  default String restapiKey(StackGresConfig resource) {
    return getKeyPrefix(resource) + StackGresContext.RESTAPI_KEY;
  }

  default String grafanaIntegrationKey(StackGresConfig resource) {
    return getKeyPrefix(resource) + StackGresContext.GRAFANA_INTEGRATION_KEY;
  }

  default String collectorKey(StackGresConfig resource) {
    return getKeyPrefix(resource) + StackGresContext.COLLECTOR_KEY;
  }

}
