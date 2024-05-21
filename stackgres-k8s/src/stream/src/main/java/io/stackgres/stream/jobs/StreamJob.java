/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.concurrent.CompletableFuture;

import io.stackgres.common.crd.sgstream.StackGresStream;

public interface StreamJob {

  CompletableFuture<Void> runJob(StackGresStream stream);

}
