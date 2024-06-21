/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.source;

import java.util.Optional;
import java.util.Properties;

import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgresDebeziumProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.stream.jobs.DebeziumUtil;
import io.stackgres.stream.jobs.StreamSourceOperation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@StreamSourceOperation(StreamSourceType.SGCLUSTER)
public class SgClusterDebeziumEngineHandler extends AbstractPostgresDebeziumEngineHandler {

  @Override
  protected void setSourceProperties(StackGresStream stream, Properties props) {
    String namespace = stream.getMetadata().getNamespace();
    var sgCluster = Optional.of(stream.getSpec().getSource().getSgCluster());
    DebeziumUtil.configureDebeziumSectionProperties(
        props,
        sgCluster
        .map(StackGresStreamSourceSgCluster::getDebeziumProperties)
        .orElse(null),
        StackGresStreamSourcePostgresDebeziumProperties.class);
    props.setProperty("slot.name", slotName(stream));
    props.setProperty("publication.name", publicationName(stream));

    DebeziumUtil.configureDebeziumIncludesAndExcludes(props, sgCluster.orElseThrow());
    String clusterName = sgCluster.map(StackGresStreamSourceSgCluster::getName)
        .orElseThrow(() -> new IllegalArgumentException("The name of SGCluster is not specified"));
    String usernameSecretName = sgCluster
        .map(StackGresStreamSourceSgCluster::getUsername)
        .map(SecretKeySelector::getName)
        .orElseGet(() -> PatroniUtil.secretName(clusterName));
    String usernameSecretKey = sgCluster
        .map(StackGresStreamSourceSgCluster::getUsername)
        .map(SecretKeySelector::getKey)
        .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_USERNAME_KEY);
    var username = getSecretKeyValue(namespace, usernameSecretName, usernameSecretKey);
    String passwordSecretName = sgCluster
        .map(StackGresStreamSourceSgCluster::getPassword)
        .map(SecretKeySelector::getName)
        .orElseGet(() -> PatroniUtil.secretName(clusterName));
    String passwordSecretKey = sgCluster
        .map(StackGresStreamSourceSgCluster::getPassword)
        .map(SecretKeySelector::getKey)
        .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY);
    var password = getSecretKeyValue(namespace, passwordSecretName, passwordSecretKey);

    props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
    props.setProperty("database.hostname", clusterName);
    props.setProperty("database.port", String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT));
    props.setProperty("database.dbname", Optional
        .ofNullable(stream.getSpec().getSource().getSgCluster())
        .map(StackGresStreamSourceSgCluster::getDatabase)
        .orElse("postgres"));
    props.setProperty("database.user", username);
    props.setProperty("database.password", password);
    props.setProperty("database.server.name", clusterName);
  }

}
