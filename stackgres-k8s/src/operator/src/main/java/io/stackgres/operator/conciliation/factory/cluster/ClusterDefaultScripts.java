/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.io.Resources;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@Singleton
public class ClusterDefaultScripts {

  public List<StackGresScriptEntry> getDefaultScripts(StackGresCluster cluster) {
    return Seq.of(getPgBouncerUserAuthenticatorScript())
        .append(getPostgresExporterInitScript())
        .append(Seq.of(
            getBabelfishUserScript(cluster),
            getBabelfishDatabaseScript(),
            getBabelfishInitDatabaseScript())
            .filter(
                script -> getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH))
        .toList();
  }

  private StackGresScriptEntry getPostgresExporterInitScript() {
    final StackGresScriptEntry script = new StackGresScriptEntry();
    script.setName("prometheus-postgres-exporter-init");
    script.setRetryOnError(true);
    script.setScript(Unchecked.supplier(() -> Resources
        .asCharSource(ClusterDefaultScripts.class.getResource(
            "/prometheus-postgres-exporter/init.sql"),
            StandardCharsets.UTF_8)
        .read()).get());
    return script;
  }

  private StackGresScriptEntry getBabelfishUserScript(StackGresCluster cluster) {
    final StackGresScriptEntry script = new StackGresScriptEntry();
    script.setName("babelfish-user");
    script.setRetryOnError(true);
    script.setScriptFrom(new StackGresScriptFrom());
    script.getScriptFrom().setSecretKeyRef(new SecretKeySelector(
        StackGresPasswordKeys.BABELFISH_CREATE_USER_SQL_KEY, PatroniSecret.name(cluster)));
    return script;
  }

  private StackGresScriptEntry getBabelfishDatabaseScript() {
    final StackGresScriptEntry script = new StackGresScriptEntry();
    script.setName("babelfish-database");
    script.setRetryOnError(true);
    script.setScript(
        "DROP DATABASE IF EXISTS babelfish;\n"
            + " CREATE DATABASE babelfish OWNER babelfish;");
    return script;
  }

  private StackGresScriptEntry getBabelfishInitDatabaseScript() {
    final StackGresScriptEntry script = new StackGresScriptEntry();
    script.setName("babelfish-init");
    script.setDatabase("babelfish");
    script.setScript(Unchecked.supplier(() -> Resources
        .asCharSource(ClusterDefaultScripts.class.getResource(
            "/babelfish/init.sql"),
            StandardCharsets.UTF_8)
        .read()).get());
    return script;
  }

  private StackGresScriptEntry getPgBouncerUserAuthenticatorScript() {
    final StackGresScriptEntry script = new StackGresScriptEntry();
    script.setName("pgbouncer-user-authenticator");
    script.setRetryOnError(true);
    script.setScript(Unchecked.supplier(() -> Resources
        .asCharSource(ClusterDefaultScripts.class.getResource(
            "/pgbouncer/init.sql"),
            StandardCharsets.UTF_8)
        .read()).get());
    return script;
  }

}
