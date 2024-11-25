/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import static io.stackgres.common.RetryUtil.retry;
import static io.stackgres.common.RetryUtil.retryWithLimit;

import java.util.List;
import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface KubernetesClientUtil {

  static Logger LOGGER = LoggerFactory.getLogger(KubernetesClientUtil.class);

  /**
   * Return true when exception is a conflict (409) error.
   */
  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  static boolean isConflict(Throwable ex) {
    final boolean isConflict = ex instanceof KubernetesClientException kce
        && kce.getCode() == Response.Status.CONFLICT.getStatusCode()
        // The HTTP 409 conflict may be returned when a resource has been removed
        //  making the function to loop forever since such situation can not be recovered
        //  by simply retrying. To avoid this situation we have to filter the error
        //  message.
        && kce.getMessage().contains("the object has been modified");
    if (isConflict && LOGGER.isTraceEnabled()) {
      LOGGER.trace("An conflict error occurred", ex);
    }
    return isConflict;
  }

  /**
   * Retry on conflict (409) error with back-off.
   */
  static void retryOnConflict(Runnable runnable) {
    retryOnConflict((Supplier<Void>) () -> {
      runnable.run();
      return null;
    });
  }

  /**
   * Retry on conflict (409) error with back-off.
   */
  static <T> T retryOnConflict(Supplier<T> supplier) {
    return retry(supplier, KubernetesClientUtil::isConflict, 10, 600, 10);
  }

  /**
   * Retry on error.
   */
  static void retryOnError(Runnable runnable, int retryLimit) {
    retryOnError((Supplier<Void>) () -> {
      runnable.run();
      return null;
    }, retryLimit);
  }

  /**
   * Retry on error.
   */
  static <T> T retryOnError(Supplier<T> supplier, int retryLimit) {
    return retryWithLimit(supplier, ex -> true, retryLimit, 3000, 30000, 1000);
  }

  static <I> List<I> listOrEmptyOnForbiddenOrNotFound(Supplier<List<I>> supplier) {
    try {
      return supplier.get();
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == Response.Status.FORBIDDEN.getStatusCode()
          || ex.getCode() == Response.Status.NOT_FOUND.getStatusCode()) {
        LOGGER.trace("An error occurred, returning an empty list", ex);
        return List.of();
      }
      throw ex;
    }
  }

}
