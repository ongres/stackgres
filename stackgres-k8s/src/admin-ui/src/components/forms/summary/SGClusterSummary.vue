<template>
	<div id="clusterSummary" class="contentTooltip show">
        <div class="close" @click="closeSummary()"></div>
        
        <div class="info">
        
            <span class="close" @click="closeSummary()">CLOSE</span>
            
            <div class="content">
                <div class="header">
                    <h2>Summary</h2>
                    <label for="showDefaults" class="switch floatRight upper">
                        <span>Show Default Values</span>
                        <input type="checkbox" id="showDefaults" class="switch" v-model="showDefaults">
                    </label>
                </div>
                <div class="summary" v-if="cluster.hasOwnProperty('data')">
                    <ul class="section">
                        <li>
                            <strong class="sectionTitle">Cluster</strong>
                            <ul>
                                <li>
                                    <strong class="sectionTitle">Metadata</strong>
                                    <ul>
                                        <li v-if="showDefaults">
                                            <strong class="label">Namespace:</strong>
                                            <span class="value">{{ cluster.data.metadata.namespace }}</span>
                                        </li>
                                        <li>
                                            <strong class="label">Name:</strong>
                                            <span class="value">{{ cluster.data.metadata.name }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || (cluster.data.spec.instances > 1) || hasProp(cluster, 'data.spec.sgInstanceProfile')" :set="showInstances = true">
                                    <strong class="sectionTitle">Instances</strong>
                                    <ul>
                                        <li v-if="showInstances">
                                            <strong class="label">Number of Instances:</strong>
                                            <span class="value">{{ cluster.data.spec.instances }}</span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.sgInstanceProfile') || showDefaults">
                                            <strong class="label">Instance Profile:</strong>
                                            <span class="value">
                                                <template v-if="hasProp(cluster, 'data.spec.sgInstanceProfile')">
                                                    <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + profile.name" target="_blank" v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )"> 
                                                        {{ profile.data.metadata.name }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }})
                                                    </router-link>
                                                </template>
                                                <template v-else>
                                                    Default (Cores: 1, RAM: 2GiB)
                                                </template>
                                            </span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || (cluster.data.spec.postgres.flavor != 'vanilla') || (cluster.data.spec.postgres.version != 'latest') || hasProp(cluster, 'data.spec.configurations.sgPostgresConfig')">
                                    <strong class="sectionTitle">Postgres</strong>
                                    <ul>
                                        <li v-if="(cluster.data.spec.postgres.flavor != 'vanilla') || showDefaults">
                                            <strong class="label">Flavor:</strong>
                                            <span class="value">{{ cluster.data.spec.postgres.flavor }}</span>
                                        </li>
                                        <li v-if="(cluster.data.spec.postgres.version != 'latest') || showDefaults">
                                            <strong class="label">Version:</strong>
                                            <span class="value">{{ cluster.data.spec.postgres.version }}</span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.configurations.sgPostgresConfig') || showDefaults">
                                            <strong class="label">Configuration:</strong>
                                            <span class="value">
                                                <span class="value">
                                                <template v-if="hasProp(cluster, 'data.spec.configurations.sgPostgresConfig')">
                                                    <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + cluster.data.spec.configurations.sgPostgresConfig" target="_blank"> 
                                                        {{ cluster.data.spec.configurations.sgPostgresConfig }}
                                                    </router-link>
                                                </template>
                                                <template v-else>
                                                    Default
                                                </template>
                                            </span>
                                            </span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || (cluster.data.spec.pods.persistentVolume.size != '1Gi') || hasProp(cluster, 'data.spec.pods.persistentVolume.storageClass')">
                                    <strong class="sectionTitle">Pods Storage</strong>
                                    <ul>
                                        <li v-if="showDefaults || (cluster.data.spec.pods.persistentVolume.size != '1Gi')">
                                            <strong class="label">Volume Size:</strong>
                                            <span class="value">{{ cluster.data.spec.pods.persistentVolume.size }}B</span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.pods.persistentVolume.storageClass')">
                                            <strong class="label">Storage Class:</strong>
                                            <span class="value">{{ cluster.data.spec.pods.persistentVolume.storageClass }}</span>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.postgres.extensions')">
                        <li>
                            <strong class="sectionTitle">Extensions</strong>
                            <ul>
                                <li v-for="ext in cluster.data.spec.postgres.extensions" :set="extData = extensionsList[cluster.data.spec.postgres.flavor][cluster.data.spec.postgres.version].find(e => (e.name == ext.name))">
                                    <strong class="sectionTitle">{{ ext.name }}</strong><br/>
                                    <ul>
                                        <li>
                                            <strong class="label">Description:</strong>
                                            <span class="value">{{ extData.abstract }}</span>
                                        </li>
                                        <li>
                                            <strong class="label">Version:</strong>
                                            <span class="value">{{ ext.version }}</span>
                                        </li>
                                        <li>
                                            <strong class="label">Webpage:</strong>
                                            <span class="value">
                                                <a :href="extData.url" target="_blank">{{ extData.url }}</a>
                                            </span>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.configurations.sgBackupConfig')">
                        <li>
                            <strong class="sectionTitle">Backups</strong>
                            <ul>
                                <li>
                                    <strong class="label">Backup Configuration:</strong>
                                    <span class="value">
                                        <router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + cluster.data.spec.configurations.sgBackupConfig" target="_blank"> 
                                            {{ cluster.data.spec.configurations.sgBackupConfig }}
                                        </router-link>
                                    </span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.configurations.backupPath')">
                                    <strong class="label">
                                        Backup Path:
                                    </strong>
                                    <span class="value">
                                        {{ cluster.data.spec.configurations.backupPath }}
                                    </span>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.initialData')">
                        <li>
                            <strong class="sectionTitle">Cluster Initialization</strong>
                            <ul v-if="hasProp(cluster, 'data.spec.initialData.restore.fromBackup.name')">
                                <li>
                                    <strong class="sectionTitle">Initialization Backup</strong>
                                    <ul>
                                        <li :set="backup = backups.find( b => (b.data.metadata.name == cluster.data.spec.initialData.restore.fromBackup.name) )">
                                            <strong class="label">Backup:</strong>
                                            <span class="value">
                                                <template v-if="(typeof backup !== 'undefined')">
                                                    <router-link :to="'/' + backup.data.metadata.namespace + '/sgbackup/' + backup.data.metadata.name" target="_blank"> 
                                                        {{ backup.data.metadata.name }} [{{ backup.data.metadata.uid.substring(0,4) }}...{{ backup.data.metadata.uid.slice(-4) }}]
                                                    </router-link>
                                                </template>
                                                <template v-else>
                                                    {{ cluster.data.spec.initialData.restore.fromBackup.name }}
                                                </template>
                                            </span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.initialData.restore.fromBackup.pointInTimeRecovery')">
                                            <strong class="label">Point-in-Time Recovery:</strong>
                                            <span class="value timestamp">
                                                <span class='date'>
                                                    {{ cluster.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp | formatTimestamp('date') }}
                                                </span>
                                                <span class='time'>
                                                    {{ cluster.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp | formatTimestamp('time') }}
                                                </span>
                                                <span class='ms'>
                                                    {{ cluster.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp | formatTimestamp('ms') }}
                                                </span>
                                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                                            </span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.initialData.restore.downloadDiskConcurrency')">
                                            <strong class="label">Download Disk Concurrency:</strong>
                                            <span class="value">{{ cluster.data.spec.initialData.restore.downloadDiskConcurrency }}</span>
                                        </li>
                                    </ul>
                                </li>
                            </ul>

                            <ul v-if="hasProp(cluster, 'data.spec.initialData.scripts')">
                                <li>
                                    <strong class="sectionTitle">Initialization Scripts</strong>
                                    <ul>
                                        <li v-for="(script, index) in cluster.data.spec.initialData.scripts">
                                            <strong class="sectionTitle">Script #{{ index+1 }}</strong>
                                            <ul>
                                                <li v-if="hasProp(script, 'name')">
                                                    <strong class="label">Name:</strong>
                                                    <span class="value">{{ script.name }}</span>
                                                </li>
                                                <li v-if="hasProp(script, 'database')">
                                                    <strong class="label">Database:</strong>
                                                    <span class="value">{{ script.database }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Script Source:</strong>
                                                    <span class="value">{{ hasProp(script, 'script') ? 'Raw Script' : (hasProp(script, 'scriptFrom.secretKeyRef') ? 'Secret Key' : "Config Map") }}</span>
                                                </li>
                                                <li v-if="hasProp(script, 'script')">
                                                    <strong class="label">Script:</strong>
                                                    <span class="value script">
                                                        <span>
                                                            <a @click="setContentTooltip('#script-'+index)">View Script</a>
                                                        </span>
                                                        <div :id="'script-' + index" class="hidden">
                                                            <pre>{{ script.script }}</pre>
                                                        </div>
                                                    </span>
                                                </li>
                                                <li v-else-if="hasProp(script, 'scriptFrom.secretKeyRef')">
                                                    <strong>Secret Key Reference:</strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Name:</strong>
                                                            <span class="value">{{ script.scriptFrom.secretKeyRef.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Key:</strong>
                                                            <span class="value">{{ script.scriptFrom.secretKeyRef.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li v-else-if="hasProp(script, 'scriptFrom.configMapKeyRef')">
                                                    <strong>Config Map Key Reference:</strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Name:</strong>
                                                            <span class="value">{{ script.scriptFrom.configMapKeyRef.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Key:</strong>
                                                            <span class="value">{{ script.scriptFrom.configMapKeyRef.key }}</span>
                                                        </li>
                                                        
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="showDefaults || cluster.data.spec.pods.disableConnectionPooling || hasProp(cluster, 'data.spec.configurations.sgPoolingConfig') || cluster.data.spec.pods.disablePostgresUtil || cluster.data.spec.pods.disableMetricsExporter || cluster.data.spec.prometheusAutobind">
                        <li>
                            <strong class="sectionTitle">Sidecars</strong>
                            <ul>
                                <li v-if="showDefaults || cluster.data.spec.pods.disableConnectionPooling || hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                    <strong class="sectionTitle">Connection Pooling</strong>
                                    <ul>
                                        <li v-if="showDefaults || cluster.data.spec.pods.disableConnectionPooling">
                                            <strong class="label">Enable:</strong>
                                            <span class="value">{{ cluster.data.spec.pods.disableConnectionPooling ? 'NO' : 'YES' }}</span>
                                        </li>
                                        <li v-if="showDefaults || hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                            <strong class="label">Connection Pooling Configuration:</strong>
                                            <span class="value">
                                                <template v-if="hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                                    <router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + cluster.data.spec.configurations.sgPoolingConfig" target="_blank">
                                                        {{ cluster.data.spec.configurations.sgPoolingConfig }}
                                                    </router-link>
                                                </template>
                                                <template v-else>
                                                    Default
                                                </template>
                                            </span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || cluster.data.spec.pods.disablePostgresUtil">
                                    <strong class="sectionTitle">Postgres Utils</strong>
                                    <ul>
                                        <li>
                                            <strong class="label">Enable:</strong>
                                            <span class="value">{{ cluster.data.spec.pods.disablePostgresUtil ? 'NO' : 'YES' }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || cluster.data.spec.pods.disableMetricsExporter || cluster.data.spec.prometheusAutobind">
                                    <strong class="sectionTitle">Monitoring</strong>
                                    <ul>
                                        <li v-if="showDefaults || cluster.data.spec.pods.disableMetricsExporter">
                                            <strong class="sectionTitle">Metrics Exporter</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">Enable:</strong>
                                                    <span class="value">{{ cluster.data.spec.pods.disableMetricsExporter ? 'NO' : 'YES' }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="showDefaults || cluster.data.spec.prometheusAutobind">
                                            <strong class="sectionTitle">Prometheus Autobind</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">Enable:</strong>
                                                    <span class="value">{{ cluster.data.spec.prometheusAutobind ? 'YES' : 'NO' }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.distributedLogs.sgDistributedLogs')">
                                    <strong class="sectionTitle">Distributed Logs</strong>
                                    <ul>
                                        <li>
                                            <strong class="label">Logs Server:</strong>
                                            <span class="value">
                                                <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.data.spec.distributedLogs.sgDistributedLogs" target="_blank">
                                                    {{ cluster.data.spec.distributedLogs.sgDistributedLogs }}
                                                </router-link>
                                            </span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.distributedLogs.retention')">
                                            <strong class="label">Retention:</strong>
                                            <span class="value">{{ cluster.data.spec.distributedLogs.retention }}</span>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="( showDefaults || ( (cluster.data.spec.replication.role != 'ha-read') || (cluster.data.spec.replication.mode != 'async') || cluster.data.spec.replication.hasOwnProperty('groups') ) )">
                        <li>
                            <strong class="sectionTitle">
                                Replication
                            </strong>
                            <ul>
                                <li v-if="(showDefaults || (cluster.data.spec.replication.role != 'ha-read') )">
                                    <strong class="label">Role:</strong>
                                    <span class="value">{{ cluster.data.spec.replication.role }}</span>
                                </li>
                                <li v-if="(showDefaults || (cluster.data.spec.replication.mode != 'async') )">
                                    <strong class="label">Mode:</strong>
                                    <span class="value">{{ cluster.data.spec.replication.mode }}</span>
                                </li>
                                <li v-if="cluster.data.spec.replication.hasOwnProperty('syncNodeCount')">
                                    <strong class="label">Sync Node Count:</strong>
                                    <span class="value">{{ cluster.data.spec.replication.syncNodeCount }}</span>
                                </li>
                                <li v-if="cluster.data.spec.replication.hasOwnProperty('groups')">
                                    <strong class="sectionTitle">
                                        Groups
                                    </strong>
                                    <ul>
                                        <li v-for="(group, index) in cluster.data.spec.replication.groups">
                                            <strong class="sectionTitle">
                                                Group #{{ index + 1}}
                                            </strong>
                                            <ul>
                                                <li v-if="group.name.length">
                                                    <strong class="label">Name:</strong>
                                                    <span class="value">{{ group.name }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Role:</strong>
                                                    <span class="value">{{ group.role }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Instances:</strong>
                                                    <span class="value">{{ group.instances }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="showDefaults || hasProp(cluster, 'data.spec.postgresServices')">
                        <li>
                            <strong class="sectionTitle">Customize generated Kubernetes service</strong>
                            <ul>
                                <li v-if="showDefaults || hasProp(cluster, 'data.spec.postgresServices.primary')">
                                    <strong class="sectionTitle">Primary</strong>
                                    <ul>
                                        <li v-if="( showDefaults || hasProp(cluster, 'data.spec.postgresServices.primary.enabled') )">
                                            <strong class="label">Enable:</strong>
                                            <span class="value">{{ hasProp(cluster, 'data.spec.postgresServices.primary.enabled') ? (cluster.data.spec.postgresServices.primary.enabled ? 'YES' : 'NO') : 'YES' }}</span>
                                        </li>
                                        <li v-if="( showDefaults || hasProp(cluster, 'data.spec.postgresServices.primary.type') )">
                                            <strong class="label">Type:</strong>
                                            <span class="value">{{ hasProp(cluster, 'data.spec.postgresServices.primary.type') ? cluster.data.spec.postgresServices.primary.type : 'ClusterIP' }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || hasProp(cluster, 'data.spec.postgresServices.replicas')">
                                    <strong class="sectionTitle">Replicas</strong>
                                    <ul>
                                        <li v-if="( showDefaults || hasProp(cluster, 'data.spec.postgresServices.replicas.enabled') )">
                                            <strong class="label">Enable:</strong>
                                            <span class="value">{{ hasProp(cluster, 'data.spec.postgresServices.replicas.enabled') ? (cluster.data.spec.postgresServices.replicas.enabled ? 'YES' : 'NO') : 'YES' }}</span>
                                        </li>
                                        <li v-if="( showDefaults || hasProp(cluster, 'data.spec.postgresServices.replicas.type') )">
                                            <strong class="label">Type:</strong>
                                            <span class="value">{{ hasProp(cluster, 'data.spec.postgresServices.replicas.type') ? cluster.data.spec.postgresServices.replicas.type : 'ClusterIP' }}</span>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.metadata.labels.clusterPods') || hasProp(cluster, 'data.spec.metadata.annotations')">
                        <li>
                            <strong class="sectionTitle">Metadata</strong>
                            <ul>
                                <li v-if="hasProp(cluster, 'data.spec.pods.metadata')">
                                    <strong class="sectionTitle">Labels</strong>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.labels.clusterPods')">
                                            <strong class="sectionTitle">Cluster Pods</strong>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.labels.clusterPods">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.metadata.annotations')">
                                    <strong class="sectionTitle">Annotations</strong>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.allResources')">
                                            <strong class="sectionTitle">All Resources</strong>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.annotations.allResources">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.clusterPods')">
                                            <strong class="sectionTitle">Cluster Pods</strong>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.annotations.clusterPods">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.services')">
                                            <strong class="sectionTitle">Services</strong>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.annotations.services">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.primaryService')">
                                            <strong class="sectionTitle">Primary Service</strong>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.annotations.primaryService">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.replicasService')">
                                            <strong class="sectionTitle">Replicas Service</strong>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.annotations.replicasService">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.pods.scheduling')">
                        <li>
                            <strong class="sectionTitle">Pods Scheduling</strong>
                            <ul>
                                <li v-if="hasProp(cluster, 'data.spec.pods.scheduling.nodeSelector')">
                                    <strong class="sectionTitle">Node Selectors</strong>
                                    <ul>
                                        <li v-for="(value, key) in cluster.data.spec.pods.scheduling.nodeSelector">
                                            <strong class="label">{{ key }}:</strong>
                                            <span class="value">{{ value }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.pods.scheduling.tolerations')">
                                    <strong class="sectionTitle">Node Tolerations</strong>
                                    <ul>
                                        <li v-for="(toleration, index) in cluster.data.spec.pods.scheduling.tolerations">
                                            <strong class="sectionTitle">Toleration #{{ index+1 }}</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">Key:</strong>
                                                    <span class="value">{{ toleration.key }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Operator:</strong>
                                                    <span class="value">{{ toleration.operator }}</span>
                                                </li>
                                                <li v-if="toleration.hasOwnProperty('value')">
                                                    <strong class="label">Value:</strong>
                                                    <span class="value">{{ toleration.value }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Effect:</strong>
                                                    <span class="value">{{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')">
                                    <strong class="sectionTitle">Node Affinity:</strong><br/>
                                    <span>Required During Scheduling Ignored During Execution</span>
                                    <ul>
                                        <li v-for="(term, index) in cluster.data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                            <strong class="sectionTitle">Term #{{ index+1 }}</strong>
                                            <ul>
                                                <li v-if="term.hasOwnProperty('matchExpressions')">
                                                    <strong class="sectionTitle">Match Expressions</strong>
                                                    <ul>
                                                        <li v-for="(exp, index) in term.matchExpressions">
                                                            <strong class="sectionTitle">Expression #{{ index+1 }}</strong>
                                                            <ul>
                                                                <li>
                                                                    <strong class="label">Key:</strong>
                                                                    <span class="value">{{ exp.key }}</span>
                                                                </li>
                                                                <li>
                                                                    <strong class="label">Operator:</strong>
                                                                    <span class="value">{{ exp.operator }}</span>
                                                                </li>
                                                                <li v-if="exp.hasOwnProperty('values')">
                                                                    <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}:</strong>
                                                                    <span class="value">{{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li v-if="term.hasOwnProperty('matchFields')">
                                                    <strong class="sectionTitle">Match Fields</strong>
                                                    <ul>
                                                        <li v-for="(field, index) in term.matchFields">
                                                            <strong class="sectionTitle">Field #{{ index+1 }}</strong>
                                                            <ul>
                                                                <li>
                                                                    <strong class="label">Key:</strong>
                                                                    <span class="value">{{ field.key }}</span>
                                                                </li>
                                                                <li>
                                                                    <strong class="label">Operator:</strong>
                                                                    <span class="value">{{ field.operator }}</span>
                                                                </li>
                                                                <li v-if="field.hasOwnProperty('values')">
                                                                    <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}:</strong>
                                                                    <span class="value">{{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">
                                    <strong class="sectionTitle">Node Affinity:</strong><br/>
                                    <span>Preferred During Scheduling Ignored During Execution</span>
                                    <ul>
                                        <li v-for="(term, index) in cluster.data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                            <strong class="sectionTitle">Term #{{ index+1 }}</strong>
                                            <ul>
                                                <li v-if="term.preference.hasOwnProperty('matchExpressions')">
                                                    <strong class="sectionTitle">Match Expressions</strong>
                                                    <ul>
                                                        <li v-for="(exp, index) in term.preference.matchExpressions">
                                                            <strong class="sectionTitle">Expression #{{ index+1 }}</strong>
                                                            <ul>
                                                                <li>
                                                                    <strong class="label">Key:</strong>
                                                                    <span class="value">{{ exp.key }}</span>
                                                                </li>
                                                                <li>
                                                                    <strong class="label">Operator:</strong>
                                                                    <span class="value">{{ exp.operator }}</span>
                                                                </li>
                                                                <li v-if="exp.hasOwnProperty('values')">
                                                                    <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}:</strong>
                                                                    <span class="value">{{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li v-if="term.preference.hasOwnProperty('matchFields')">
                                                    <strong class="sectionTitle">Match Fields</strong>
                                                    <ul>
                                                        <li v-for="(field, index) in term.preference.matchFields">
                                                            <strong class="sectionTitle">Field #{{ index+1 }}</strong>
                                                            <ul>
                                                                <li>
                                                                    <strong class="label">Key:</strong>
                                                                    <span class="value">{{ field.key }}</span>
                                                                </li>
                                                                <li>
                                                                    <strong class="label">Operator:</strong>
                                                                    <span class="value">{{ field.operator }}</span>
                                                                </li>
                                                                <li v-if="field.hasOwnProperty('values')">
                                                                    <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}:</strong>
                                                                    <span class="value">{{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li v-if="term.hasOwnProperty('weight')">
                                                    <strong class="label">Weight:</strong>
                                                    <span class="value">{{ term.weight }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="showDefaults || hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity')">
                        <li>
                            <strong class="sectionTitle">Non Production Settings</strong>
                            <ul>
                                <li>
                                    <strong class="sectionTitle">Cluster Pod Anti Affinity</strong>
                                    <ul>
                                        <li>
                                            <strong class="label">Enable:</strong>
                                            <span class="value">{{ hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') ? (cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity ? 'NO' : 'YES') : 'YES' }}</span>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
	import store from '../../../store'
	import { mixin } from '../../mixins/mixin'

    export default {
        name: 'SGClusterSummary',

		mixins: [mixin],

        props: ['cluster', 'extensionsList'],

        data() {
            return {
                showDefaults: false
            }
        },

		computed: {

            profiles () {
				return store.state.sginstanceprofiles
			},

			backups () {
				return store.state.sgbackups
			},

            backupConfig () {
				return store.state.sgbackupconfigs
			}
		},

        methods: {

            closeSummary() {
                this.$emit('closeSummary', true)
            }

        }

	}
</script>

<style scoped>

    .header {
        position: relative;
    }

    .header label {
        border: 0;
        position: absolute;
        background: transparent;
        right: 0;
        bottom: 0;
        height: auto;
        display: inline-block;
        margin: 0;
        padding: 0;
    }

    .header label span {
        display: inline-block;
        margin-right: 5px;
    }

    .header .switch input {
        transform: translate(5px, 2px);
    }

    .header .switch input:before {
        display: none;
    }

	.sectionTitle {
        font-size: 1rem;
    }

    .summary {
        max-height: 50vh;
        overflow-y: auto;
        min-width: 500px;
    }

    .summary ul.section {
        margin: 10px 0 35px;
    }

    .summary ul:not(.section) {
        position: relative;
        padding: 15px 25px 0;
        list-style: none;
    }

    .summary strong.label {
        display: inline-block;
        margin-right: 7px;
    }

    .summary li {
        margin-bottom: 10px;
        position: relative;
    }

    .summary ul li:last-child {
        margin-bottom: 5px;
    }

    .summary ul ul:before {
        content: "";
        display: inline;
        height: calc(100% - 5px);
        width: 2px;
        background: var(--borderColor);
        position: absolute;
        top: 5px;
        left: 5px;
    }

    .summary ul + ul {
        margin-top: -15px;
    }

    .summary ul ul ul li:last-of-type:after {
        content: "";
        display: inline;
        width: 4px;
        height: calc(100% + 7px);
        position: absolute;
        background: var(--bgColor);
        left: -20px;
        top: 8px;
    }

    .summary ul ul li:before {
        content: "";
        display: inline;
        height: 2px;
        width: 10px;
        position: absolute;
        background-color: var(--borderColor);
        left: -18px;
        top: 6px;
    }

    .summary ul.section > li > ul > li:last-child:after {
        content: " ";
        width: 2px;
        display: block;
        position: absolute;
        height: 100%;
        background: var(--bgColor);
        top: 8px;
        left: -20px;
        z-index: 3;
    }

    .darkmode .summary ul ul ul li:last-of-type:after, .darkmode .summary ul.section > li > ul > li:last-child:after {
        background: var(--activeBg);
    }

</style>