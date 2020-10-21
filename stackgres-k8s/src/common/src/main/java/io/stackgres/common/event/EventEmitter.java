/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import io.stackgres.operatorframework.resource.EventReason;

public interface EventEmitter<T> {

  void sendEvent(EventReason reason, String message, T involvedObject);
}
