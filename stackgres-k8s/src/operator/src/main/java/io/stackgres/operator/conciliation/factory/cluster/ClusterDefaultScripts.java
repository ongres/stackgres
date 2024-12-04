/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.io.Resources;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryBuilder;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@Singleton
public class ClusterDefaultScripts {

  public List<StackGresScriptEntry> getDefaultScripts(StackGresCluster cluster) {
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
            .asCharSource(ClusterDefaultScripts.class.getResource(
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
            .asCharSource(ClusterDefaultScripts.class.getResource(
                "/babelfish/init.sql"),
                StandardCharsets.UTF_8)
            .read()).get())
        .build();
  }

}
