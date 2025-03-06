/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryBuilder;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder
public class ClusterDefaultScript implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster labelFactory;

  @Inject
  public ClusterDefaultScript(LabelFactoryForCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream.of(getDefaultScript(context.getSource()));
  }

  private StackGresScript getDefaultScript(StackGresCluster cluster) {
    return new StackGresScriptBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(ManagedSqlUtil.defaultName(cluster))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withScripts(getDefaultScripts(cluster))
        .endSpec()
        .build();
  }

  private List<StackGresScriptEntry> getDefaultScripts(StackGresCluster cluster) {
    return Seq.of(getPostgresExporterInitScript(0))
        .append(Seq.of(
            getBabelfishUserScript(cluster, 1),
            getBabelfishDatabaseScript(2),
            getBabelfishInitDatabaseScript(3))
            .filter(
                script -> getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH))
        .append(getPasswordsUpdateScript(cluster, 4))
        .toList();
  }

  private StackGresScriptEntry getPostgresExporterInitScript(int id) {
    return new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("prometheus-postgres-exporter-init")
        .withRetryOnError(true)
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(ClusterDefaultScript.class.getResource(
                "/prometheus-postgres-exporter/init.sql"),
                StandardCharsets.UTF_8)
            .read()).get())
        .build();
  }

  private StackGresScriptEntry getPasswordsUpdateScript(StackGresCluster cluster, int id) {
    return new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("password-update")
        .withRetryOnError(true)
        .withNewScriptFrom()
        .withNewSecretKeyRef()
        .withName(PatroniSecret.name(cluster))
        .withKey(PatroniSecret.ROLES_UPDATE_SQL_KEY)
        .endSecretKeyRef()
        .endScriptFrom()
        .build();
  }

  private StackGresScriptEntry getBabelfishUserScript(StackGresCluster cluster, int id) {
    return new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("babelfish-user")
        .withRetryOnError(true)
        .withNewScriptFrom()
        .withNewSecretKeyRef()
        .withName(PatroniSecret.name(cluster))
        .withKey(StackGresPasswordKeys.BABELFISH_CREATE_USER_SQL_KEY)
        .endSecretKeyRef()
        .endScriptFrom()
        .build();
  }

  private StackGresScriptEntry getBabelfishDatabaseScript(int id) {
    return new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("babelfish-database")
        .withRetryOnError(true)
        .withScript(
            "DROP DATABASE IF EXISTS babelfish;\n"
                + " CREATE DATABASE babelfish OWNER babelfish;")
        .build();
  }

  private StackGresScriptEntry getBabelfishInitDatabaseScript(int id) {
    return new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("babelfish-init")
        .withDatabase("babelfish")
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(ClusterDefaultScript.class.getResource(
                "/babelfish/init.sql"),
                StandardCharsets.UTF_8)
            .read()).get())
        .build();
  }

}
