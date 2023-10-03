/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;

@ApplicationScoped
public class ShardedBackupLabelFactory extends AbstractLabelFactory<StackGresShardedBackup>
    implements LabelFactoryForShardedBackup {

  private final LabelMapperForShardedBackup labelMapper;

  @Inject
  public ShardedBackupLabelFactory(LabelMapperForShardedBackup labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public Map<String, String> backupPodLabels(StackGresShardedBackup resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().shardedBackupKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public LabelMapperForShardedBackup labelMapper() {
    return labelMapper;
  }

}
