/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.stackgres.common.prometheus.PrometheusConfigList;
import io.stackgres.testutil.fixture.Fixture;

public class PrometheusListFixture extends Fixture<PrometheusConfigList> {

  public PrometheusListFixture loadDefault() {
    fixture = readFromJson(PROMETHEUS_PROMETHEUS_LIST_JSON);
    return this;
  }

}
