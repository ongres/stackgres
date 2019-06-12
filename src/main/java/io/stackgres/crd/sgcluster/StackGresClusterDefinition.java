/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.crd;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;

public class StackGresClusterCrd {

  private StackGresClusterCrd() {}

  public static final String CRD_GROUP = "stackgres.io";
  public static final String CRD_VERSION = "v1alpha1";
  public static final String CRD_KIND = "StackGresCluster";
  public static final String CRD_SINGULAR = "sgcluster";
  public static final String CRD_PLURAL = "sgclusters";
  public static final String CRD_NAME = CRD_PLURAL + "." + CRD_GROUP;
  public static final String CRD_APIVERSION = CRD_GROUP + "/" + CRD_VERSION;

  public static final CustomResourceDefinition CR_DEFINITION =
      new CustomResourceDefinitionBuilder()
          .withApiVersion("apiextensions.k8s.io/v1beta1")
          .withNewMetadata()
          .withName(CRD_NAME)
          .endMetadata()
          .withNewSpec()
          .withGroup(CRD_GROUP)
          .withVersion(CRD_VERSION)
          .withScope("Namespaced")
          .withNewNames()
          .withKind(CRD_KIND)
          .withListKind(CRD_KIND + "List")
          .withSingular(CRD_SINGULAR)
          .withPlural(CRD_PLURAL)
          .endNames()
          .endSpec()
          .build();

}
