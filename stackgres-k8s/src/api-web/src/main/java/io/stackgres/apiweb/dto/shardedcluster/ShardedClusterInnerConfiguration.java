/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterConfiguration;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(value = { "sgBackupConfig", "backupPath",
    "backups", "patroni", "credentials" })
public class ShardedClusterInnerConfiguration extends ClusterConfiguration {

}
