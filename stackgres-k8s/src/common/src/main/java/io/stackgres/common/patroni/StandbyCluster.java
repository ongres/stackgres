/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StandbyCluster {

  @JsonProperty("host")
  private String host;

  @JsonProperty("port")
  private String port;

  @JsonProperty("primary_slot_name")
  private String primarySlotName;

  @JsonProperty("create_replica_methods")
  private List<String> createReplicaMethods;

  @JsonProperty("restore_command")
  private String restoreCommand;

  @JsonProperty("archive_cleanup_command")
  private String archiveCleanupCommand;

  @JsonProperty("recovery_min_apply_delay")
  private String recoveryMinApplyDelay;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getPrimarySlotName() {
    return primarySlotName;
  }

  public void setPrimarySlotName(String primarySlotName) {
    this.primarySlotName = primarySlotName;
  }

  public List<String> getCreateReplicaMethods() {
    return createReplicaMethods;
  }

  public void setCreateReplicaMethods(List<String> createReplicaMethods) {
    this.createReplicaMethods = createReplicaMethods;
  }

  public String getRestoreCommand() {
    return restoreCommand;
  }

  public void setRestoreCommand(String restoreCommand) {
    this.restoreCommand = restoreCommand;
  }

  public String getArchiveCleanupCommand() {
    return archiveCleanupCommand;
  }

  public void setArchiveCleanupCommand(String archiveCleanupCommand) {
    this.archiveCleanupCommand = archiveCleanupCommand;
  }

  public String getRecoveryMinApplyDelay() {
    return recoveryMinApplyDelay;
  }

  public void setRecoveryMinApplyDelay(String recoveryMinApplyDelay) {
    this.recoveryMinApplyDelay = recoveryMinApplyDelay;
  }
}
