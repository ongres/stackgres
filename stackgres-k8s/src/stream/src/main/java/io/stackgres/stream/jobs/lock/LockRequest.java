/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.lock;

import org.immutables.value.Value;

@Value.Immutable
public interface LockRequest {

  String getServiceAccount();

  String getPodName();

  String getNamespace();

  String getLockResourceName();

  int getDuration();

  int getPollInterval();

  class Builder extends ImmutableLockRequest.Builder {
  }

  static Builder builder() {
    return new Builder();
  }

}
