/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.exception;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ErrorResponse {
  private String type;
  private String title;
  private String detail;
  private Integer status;
  private String[] fields;

  public ErrorResponse(String type, String title, String detail, Integer status, String[] fields) {
    this.type = type;
    this.title = title;
    this.detail = detail;
    this.status = status;
    this.fields = fields != null ? fields.clone() : null;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String[] getFields() {
    return fields != null ? fields.clone() : new String[0];
  }

  public void setFields(String[] fields) {
    this.fields = fields != null ? fields.clone() : null;
  }

  public static ErrorResponse create(Throwable throwable) {
    return create(throwable, null);
  }

  public static ErrorResponse create(Throwable throwable, String message) {
    return new ErrorResponseBuilder(throwable.getClass().getName())
        .setDetail(message != null ? message : throwable.getMessage())
        .build();
  }
}
