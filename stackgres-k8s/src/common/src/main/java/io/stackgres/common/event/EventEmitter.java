/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operatorframework.resource.EventReason;

public interface EventEmitter<T extends HasMetadata> {

  /**
   * Send an event related to a resource.
   */
  void sendEvent(EventReason reason, String message, T involvedObject);

}
