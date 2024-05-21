/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgstream.StackGresStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamLabelFactory
    extends AbstractLabelFactory<StackGresStream> implements LabelFactoryForStream {

  private final LabelMapperForStream labelMapper;

  @Inject
  public StreamLabelFactory(LabelMapperForStream labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public Map<String, String> streamPodLabels(StackGresStream resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().streamKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public LabelMapperForStream labelMapper() {
    return labelMapper;
  }

}
