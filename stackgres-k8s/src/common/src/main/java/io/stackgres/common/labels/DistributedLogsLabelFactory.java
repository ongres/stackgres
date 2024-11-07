/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsLabelFactory
    extends AbstractLabelFactory<StackGresDistributedLogs>
    implements LabelFactoryForDistributedLogs {

  private final DistributedLogsLabelMapper labelMapper;

  @Inject
  public DistributedLogsLabelFactory(DistributedLogsLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public DistributedLogsLabelMapper labelMapper() {
    return labelMapper;
  }

  @Override
  public Map<String, String> clusterLabels(StackGresDistributedLogs resource) {
    return ImmutableMap.<String, String>builder()
        .put(labelMapper().resourceScopeKey(resource), labelValue(resourceName(resource)))
        .put(labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE)
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .build();
  }

}
