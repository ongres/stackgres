/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Properties;

import com.google.common.base.Preconditions;

public enum StackGresContext {

  INSTANCE;

  public static final String CRD_GROUP = INSTANCE.group;
  public static final String CRD_VERSION = INSTANCE.version;

  public static final String CONTAINER_BUILD = INSTANCE.containerBuild;

  public static final String OPERATOR_VERSION = INSTANCE.operatorVersion;
  public static final String DOCUMENTATION_URI = INSTANCE.documentationUri;
  public static final String DOCUMENTATION_ERRORS_PATH = INSTANCE.documentationErrorsPath;

  private final String group;
  private final String version;

  private final String containerBuild;
  private final String operatorVersion;
  private final String documentationUri;
  private final String documentationErrorsPath;

  StackGresContext() {
    try {

      Properties properties = new Properties();
      properties.load(StackGresContext.class.getResourceAsStream("/application.properties"));

      group = getProperty(properties, StackGresProperty.CRD_GROUP);
      version = getProperty(properties, StackGresProperty.CRD_VERSION);
      containerBuild = getProperty(properties, StackGresProperty.CONTAINER_BUILD);
      operatorVersion = getProperty(properties, StackGresProperty.OPERATOR_VERSION);
      documentationUri = getProperty(properties, StackGresProperty.DOCUMENTATION_URI);
      documentationErrorsPath = getProperty(properties,
          StackGresProperty.DOCUMENTATION_ERRORS_PATH);
      Preconditions.checkNotNull(group);
      Preconditions.checkNotNull(version);
      Preconditions.checkNotNull(containerBuild);

    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

  }

  private static String getProperty(Properties properties, StackGresProperty configProperty) {
    return properties.getProperty(configProperty.systemProperty());
  }

}
