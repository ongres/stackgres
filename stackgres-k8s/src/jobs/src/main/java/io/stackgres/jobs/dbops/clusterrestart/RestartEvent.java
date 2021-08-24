/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import org.immutables.value.Value;

@Value.Immutable
public interface RestartEvent {

  Optional<Pod> getPod();

  RestartEventType getEventType();

}
