/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptFrom;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.patroni.StackGresRandomPasswordKeys;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.v12.PatroniScriptsConfigMap;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple4;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V_1_3)
public class PatroniDefaultScripts implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public PatroniDefaultScripts(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream.of(getDefaultScript(context.getSource()));
  }

  private StackGresScript getDefaultScript(StackGresCluster cluster) {
    StackGresScript defaultScript = new StackGresScript();
    defaultScript.setMetadata(new ObjectMeta());
    defaultScript.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    defaultScript.getMetadata().setName(ManagedSqlUtil.defaultName(cluster));
    defaultScript.getMetadata().setLabels(labelFactory.genericLabels(cluster));
    defaultScript.setSpec(new StackGresScriptSpec());
    defaultScript.getSpec().setScripts(new ArrayList<>());
    defaultScript.getSpec().getScripts().addAll(
        Seq.of(getPostgresExporterInitScript())
        .append(Seq.of(
            getBabelfishUserScript(cluster),
            getBabelfishDatabaseScript(),
            getBabelfishInitDatabaseScript())
            .filter(
                script -> getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH))
        .toList());
    return defaultScript;
  }

  private StackGresScriptEntry getPostgresExporterInitScript() {
    final StackGresScriptEntry script = new StackGresScriptEntry();
    script.setName("prometheus-postgres-exporter-init");
    script.setDatabase("postgres");
    script.setScript(Unchecked.supplier(() -> Resources
        .asCharSource(PatroniDefaultScripts.class.getResource(
            "/prometheus-postgres-exporter/init.sql"),
            StandardCharsets.UTF_8)
        .read()).get());
    return script;
  }

  private StackGresScriptEntry getBabelfishUserScript(StackGresCluster cluster) {
    final StackGresScriptEntry script = new StackGresScriptEntry();
    script.setName("babelfish-user");
    script.setDatabase("postgres");
    script.setScriptFrom(new StackGresScriptFrom());
    script.getScriptFrom().setSecretKeyRef(new SecretKeySelector(
        StackGresRandomPasswordKeys.BABELFISH_CREATE_USER_SQL_KEY, PatroniSecret.name(cluster)));
    return script;
  }

  private StackGresScriptEntry getBabelfishDatabaseScript() {
    final StackGresScriptEntry script = new StackGresScriptEntry();
    script.setName("babelfish-database");
    script.setDatabase("postgres");
    script.setScript("CREATE DATABASE babelfish OWNER babelfish");
    return script;
  }

  private StackGresScriptEntry getBabelfishInitDatabaseScript() {
    final StackGresScriptEntry script = new StackGresScriptEntry();
    script.setName("babelfish-init");
    script.setDatabase("babelfish");
    script.setScript(Unchecked.supplier(() -> Resources
        .asCharSource(PatroniDefaultScripts.class.getResource(
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
    return getDefaultScript(cluster)
        .getSpec()
        .getScripts()
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
