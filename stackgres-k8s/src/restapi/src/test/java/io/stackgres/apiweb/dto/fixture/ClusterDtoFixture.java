/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterPods;
import io.stackgres.apiweb.dto.cluster.ClusterPodsScheduling;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.testutil.fixture.Fixture;

public class ClusterDtoFixture extends Fixture<ClusterDto> {

  public static final String POSTGRES_LATEST_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().get(0).get();

  public ClusterDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_CLUSTER_DTO_JSON);
    return this;
  }

  public ClusterDtoFixture loadInlineScripts() {
    fixture = readFromJson(STACKGRES_CLUSTER_INLINE_SCRIPTS_JSON);
    return this;
  }

  public ClusterDtoFixture empty() {
    fixture = new ClusterDto();
    return this;
  }

  public ClusterDtoFixture withSpec() {
    if (fixture.getSpec() == null) {
      fixture.setSpec(new ClusterSpec());
    }
    return this;
  }

  public ClusterDtoFixture withPods() {
    withSpec();
    if (fixture.getSpec().getPods() == null) {
      fixture.getSpec().setPods(new ClusterPods());
    }
    return this;
  }

  public ClusterDtoFixture withScheduling() {
    withPods();
    if (fixture.getSpec().getPods().getScheduling() == null) {
      fixture.getSpec().getPods().setScheduling(new ClusterPodsScheduling());
    }
    return this;
  }

  public ClusterDtoFixture withNodeAffinity(NodeAffinity nodeAffinity) {
    withScheduling();
    fixture.getSpec().getPods().getScheduling().setNodeAffinity(nodeAffinity);
    return this;
  }

  public ClusterDtoFixture withLatestPostgresVersion() {
    fixture.getSpec().getPostgres().setVersion(POSTGRES_LATEST_VERSION);
    return this;
  }

}
