/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Singleton;

import com.google.common.io.Resources;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptFrom;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.patroni.StackGresRandomPasswordKeys;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operator.conciliation.factory.cluster.patroni.v12.PatroniScriptsConfigMap;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple4;

@Singleton
public class ClusterDefaultScripts {

  public List<StackGresScriptEntry> getDefaultScripts(StackGresCluster cluster) {
    return Seq.of(getPostgresExporterInitScript())
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
        StackGresRandomPasswordKeys.BABELFISH_CREATE_USER_SQL_KEY, PatroniSecret.name(cluster)));
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

  public List<Tuple4<StackGresClusterScriptEntry, Long, String, Long>> getIndexedScripts(
      StackGresCluster cluster) {
    Seq<StackGresClusterScriptEntry> internalScripts =  Seq.seq(getInternalScripts(cluster));
    return internalScripts
        .zipWithIndex()
        .map(t -> t.concat(PatroniScriptsConfigMap.INTERNAL_SCRIPT))
        .append(Seq.of(Optional.ofNullable(
            cluster.getSpec().getInitData())
            .map(StackGresClusterInitData::getScripts))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .zipWithIndex()
            .map(t -> t.concat(PatroniScriptsConfigMap.SCRIPT)))
        .zipWithIndex()
        .map(t -> t.v1.concat(t.v2))
        .toList();
  }

  private Stream<StackGresClusterScriptEntry> getInternalScripts(StackGresCluster cluster) {
    return getDefaultScripts(cluster)
        .stream()
        .map(script -> {
          var clusterScript = new StackGresClusterScriptEntry();
          clusterScript.setName(script.getName());
          clusterScript.setDatabase(script.getDatabase());
          clusterScript.setScript(script.getScript());
          if (script.getScriptFrom() != null) {
            clusterScript.setScriptFrom(new StackGresClusterScriptFrom());
            clusterScript.getScriptFrom().setConfigMapKeyRef(
                script.getScriptFrom().getConfigMapKeyRef());
            clusterScript.getScriptFrom().setSecretKeyRef(
                script.getScriptFrom().getSecretKeyRef());
          }
          return clusterScript;
        });
  }

}
