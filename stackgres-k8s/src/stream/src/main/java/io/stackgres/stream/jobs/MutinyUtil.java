/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.function.Function;

import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public interface MutinyUtil {

  Logger LOGGER = LoggerFactory.getLogger(MutinyUtil.class);

  static Function<? super Throwable, ? extends Throwable> logOnFailureToRetry(String message) {
    return ex -> logOnFailureToRetry(ex, message);
  }

  static Throwable logOnFailureToRetry(Throwable ex, String message, Object...args) {
    LOGGER.warn(MessageFormatter.arrayFormat("Transient failure " + message + ": {}",
        Seq.of(args).append(ex.getMessage()).toArray()).getMessage(), ex);
    return ex;
  }

}
