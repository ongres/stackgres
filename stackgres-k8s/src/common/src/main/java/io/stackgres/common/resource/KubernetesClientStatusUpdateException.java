/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.io.PrintStream;
import java.io.PrintWriter;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class KubernetesClientStatusUpdateException extends KubernetesClientException {

  private static final long serialVersionUID = 1L;

  private final KubernetesClientException cause;

  public KubernetesClientStatusUpdateException(KubernetesClientException cause) {
    super(cause.getMessage());
    this.cause = cause;
  }

  public Status getStatus() {
    return cause.getStatus();
  }

  public int getCode() {
    return cause.getCode();
  }

  public int hashCode() {
    return cause.hashCode();
  }

  public boolean equals(Object obj) {
    return cause.equals(obj);
  }

  public String getMessage() {
    return cause.getMessage();
  }

  public String getLocalizedMessage() {
    return cause.getLocalizedMessage();
  }

  public Throwable getCause() {
    return cause.getCause();
  }

  public Throwable initCause(Throwable cause) {
    return cause.initCause(cause);
  }

  public String toString() {
    return cause.toString();
  }

  public void printStackTrace() {
    cause.printStackTrace();
  }

  public void printStackTrace(PrintStream s) {
    cause.printStackTrace(s);
  }

  public void printStackTrace(PrintWriter s) {
    cause.printStackTrace(s);
  }

  public Throwable fillInStackTrace() {
    return cause.fillInStackTrace();
  }

  public StackTraceElement[] getStackTrace() {
    return cause.getStackTrace();
  }

  public void setStackTrace(StackTraceElement[] stackTrace) {
    cause.setStackTrace(stackTrace);
  }
}
