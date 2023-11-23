/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().restapiKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> grafanaIntegrationLabels(@NotNull StackGresConfig resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().grafanaIntegrationKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public LabelMapperForConfig labelMapper() {
    return labelMapper;
  }

}
