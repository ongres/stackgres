/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ConfigLabelFactory extends AbstractLabelFactory<StackGresConfig>
    implements LabelFactoryForConfig {

  private final LabelMapperForConfig labelMapper;

  @Inject
  public ConfigLabelFactory(LabelMapperForConfig labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public Map<String, String> restapiLabels(@NotNull StackGresConfig resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().restapiKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> grafanaIntegrationLabels(@NotNull StackGresConfig resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().grafanaIntegrationKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public LabelMapperForConfig labelMapper() {
    return labelMapper;
  }

}
