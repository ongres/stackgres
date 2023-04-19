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
                                <li v-if="showDefaults || (cluster.data.spec.postgres.flavor != 'vanilla') || (cluster.data.spec.postgres.version != 'latest') || hasProp(cluster, 'data.spec.configurations.sgPostgresConfig') || hasProp(cluster, 'data.spec.postgres.ssl')">
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
                                        <li v-if="hasProp(cluster, 'data.spec.postgres.ssl.enabled') && cluster.data.spec.postgres.ssl.enabled">
                                            <strong>SSL Connections:</strong> Enabled
                                            <ul>
                                                <li>
                                                    <strong class="sectionTitle">Certificate Secret Key Selector</strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Name:</strong>
                                                            <span class="value">{{ cluster.data.spec.postgres.ssl.certificateSecretKeySelector.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Key:</strong>
                                                            <span class="value">{{ cluster.data.spec.postgres.ssl.certificateSecretKeySelector.key }}</span>
                                                        </li>                                                                            
                                                    </ul>
                                                </li>
                                                <li>
                                                    <strong class="sectionTitle">Private Key Secret Key Selector</strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Name:</strong>
                                                            <span class="value">{{ cluster.data.spec.postgres.ssl.privateKeySecretKeySelector.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Key:</strong>
                                                            <span class="value">{{ cluster.data.spec.postgres.ssl.privateKeySecretKeySelector.key }}</span>
                                                        </li>                                                                            
                                                    </ul>
                                                </li>
                                            </ul>
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

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.configurations.backups')">
                        <li>
                            <strong class="sectionTitle">Managed Backups Specs</strong>
                            <ul v-for="backup in cluster.data.spec.configurations.backups">
                                <li>
                                    <strong class="label">Object Storage:</strong>
                                    <span class="value">
                                        <router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + backup.sgObjectStorage" target="_blank"> 
                                            {{ backup.sgObjectStorage }}
                                        </router-link>
                                    </span>
                                </li>
                                <li v-if="( showDefaults || ( backup.cronSchedule != '0 3 * * *' ) )">
                                    <strong class="label">Cron Schedule:</strong>
                                    <span class="value">{{ tzCrontab(backup.cronSchedule) }} ({{ tzCrontab(backup.cronSchedule) | prettyCRON(false) }})</span>
                                </li>
                                <li v-if="!isNull(backup.path)">
                                    <strong class="label">
                                        Path:
                                    </strong>
                                    <span class="value">
                                        {{ backup.path }}
                                    </span>
                                </li>
                                <li v-if="( showDefaults || (backup.retention != 5) )">
                                    <strong class="label">Retention Window:</strong>
                                    <span class="value">{{ backup.retention }}</span>
                                </li>
                                <li v-if="( showDefaults || (backup.compression != 'lz4') )">
                                    <strong class="label">Compression Method:</strong>
                                    <span class="value">{{ backup.compression }}</span>
                                </li>
                                <li v-if="( 
                                    showDefaults || ( 
                                        ( (hasProp(backup, 'performance.maxNetworkBandwidth')) && (backup.performance.maxNetworkBandwidth.length) )|| 
                                        ( (hasProp(backup, 'performance.maxDiskBandwidth')) && (backup.performance.maxDiskBandwidth.length) )|| 
                                        (backup.performance.uploadDiskConcurrency != 1) 
                                    ) 
                                )">
                                    <strong class="sectionTitle">Performance Specs</strong>
                                    <ul>
                                        <li v-if="( showDefaults || ( hasProp(backup, 'performance.maxNetworkBandwidth') && backup.performance.maxNetworkBandwidth.length) )">
                                            <strong class="label">Maximum Network Bandwidth:</strong>
                                            <span class="value">{{ ( hasProp(backup, 'performance.maxNetworkBandwidth') && backup.performance.maxNetworkBandwidth.length ) ? backup.performance.maxNetworkBandwidth : 'unlimited' }} </span>
                                        </li>
                                        <li v-if="( showDefaults || ( hasProp(backup, 'performance.maxDiskBandwidth') && backup.performance.maxDiskBandwidth.length) )">
                                            <strong class="label">Maximum Disk Bandwidth:</strong>
                                            <span class="value">{{ ( hasProp(backup, 'performance.maxDiskBandwidth') && backup.performance.maxDiskBandwidth.length ) ? backup.performance.maxDiskBandwidth : 'unlimited' }} </span>
                                        </li>
                                        <li v-if="( showDefaults || ( hasProp(backup, 'performance.uploadDiskConcurrency') && (backup.performance.uploadDiskConcurrency != 1) ) )">
                                            <strong class="label">Upload Disk Concurrency:</strong>
                                            <span class="value">{{ hasProp(backup, 'performance.uploadDiskConcurrency') ? backup.performance.uploadDiskConcurrency : 1 }} </span>
                                        </li>
                                        
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>


                    <ul class="section" v-if="hasProp(cluster, 'data.spec.initialData')">
                        <strong class="sectionTitle">Cluster Initialization</strong>
                        
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
                            <li v-if="showDefaults || hasProp(cluster, 'data.spec.initialData.restore.downloadDiskConcurrency')">
                                <strong class="label">Download Disk Concurrency:</strong>
                                <span class="value">{{ hasProp(cluster, 'data.spec.initialData.restore.downloadDiskConcurrency') ? cluster.data.spec.initialData.restore.downloadDiskConcurrency : 1 }}</span>
                            </li>
                        </ul>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.managedSql')">
                        <li>
                            <strong class="sectionTitle">Managed SQL</strong>
                            <ul>
                                <li>
                                    <strong class="sectionTitle">Scripts</strong>
                                    <ul>
                                        <li v-for="(baseScript, baseIndex) in cluster.data.spec.managedSql.scripts">
                                            <strong class="sectionTitle">SGScript #{{ baseIndex + 1 }}</strong>
                                            <ul>
                                                <li v-if="( showDefaults || ( hasProp(baseScript, 'scriptSpec.continueOnError') && baseScript.scriptSpec.continueOnError ) )">
                                                    <strong class="label">Continue on Error:</strong>
                                                    <span class="value">{{ hasProp(baseScript, 'scriptSpec.continueOnError') ? isEnabled(baseScript.continueOnError) : 'Disabled' }}</span>
                                                </li>
                                                <li v-if="( showDefaults || ( hasProp(baseScript, 'scriptSpec.managedVersions') && !baseScript.scriptSpec.managedVersions) )">
                                                    <strong class="label">Managed Versions:</strong>
                                                    <span class="value">{{ hasProp(baseScript, 'scriptSpec.managedVersions') && isEnabled(baseScript.scriptSpec.managedVersions) }}</span>
                                                </li>
                                                <li v-if="baseScript.hasOwnProperty('scriptSpec')">
                                                    <strong class="sectionTitle">Script Entries</strong>

                                                    <ul>
                                                        <li v-for="(script, index) in baseScript.scriptSpec.scripts">
                                                            <strong class="sectionTitle">Script #{{ index + 1 }}</strong>

                                                            <ul>
                                                                <li v-if="hasProp(script, 'name')">
                                                                    <strong class="label">Name:</strong>
                                                                    <span class="value">{{ script.name }}</span>
                                                                </li>
                                                                <li v-if="hasProp(script, 'version')">
                                                                    <strong class="label">Version:</strong>
                                                                    <span class="value">{{ script.version }}</span>
                                                                </li>
                                                                <li v-if="showDefaults || hasProp(script, 'database')">
                                                                    <strong class="label">Database:</strong>
                                                                    <span class="value">{{ script.hasOwnProperty('database') ? script.database : 'postgres' }}</span>
                                                                </li>
                                                                <li v-if="showDefaults || hasProp(script, 'user')">
                                                                    <strong class="label">User:</strong>
                                                                    <span class="value">{{ script.hasOwnProperty('user') ? script.database : 'postgres' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('retryOnError') && script.retryOnError) )">
                                                                    <strong class="label">Retry on Error:</strong>
                                                                    <span class="value">{{ script.hasOwnProperty('retryOnError') ? isEnabled(script.retryOnError) : 'Disabled' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('storeStatusInDatabase') && script.storeStatusInDatabase) )">
                                                                    <strong class="label">Store Status in Database:</strong>
                                                                    <span class="value">{{ script.hasOwnProperty('storeStatusInDatabase') ? isEnabled(script.storeStatusInDatabase) : 'Disabled' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('wrapInTransaction') && (script.wrapInTransaction != null) ) )">
                                                                    <strong class="label">Wrap in Transaction:</strong>
                                                                    <span class="value">{{ script.hasOwnProperty('wrapInTransaction') ? script.wrapInTransaction : 'Disabled' }}</span>
                                                                </li>
                                                                <li>
                                                                    <strong class="label">Script Source:</strong>
                                                                    <span class="value">{{ hasProp(script, 'script') ? 'Raw Script' : (hasProp(script, 'scriptFrom.secretKeyRef') ? 'Secret Key' : "Config Map") }}</span>
                                                                </li>
                                                                <li v-if="hasProp(script, 'script')">
                                                                    <strong class="label">Script:</strong>
                                                                    <span class="value script">
                                                                        <span>
                                                                            <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)">View Script</a>
                                                                        </span>
                                                                        <div :id="'script-' + baseIndex + '-' + index" class="hidden">
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
                                                <li v-else-if="baseScript.hasOwnProperty('sgScript')">
                                                    <strong class="label">SGScript:</strong>
                                                    <span class="value">
                                                        <router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.sgScript" target="_blank">
                                                            {{ baseScript.sgScript }}
                                                        </router-link>
                                                    </span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="( showDefaults || (cluster.data.spec.managedSql.hasOwnProperty('continueOnSGScriptError') && cluster.data.spec.managedSql.continueOnSGScriptError) )">
                                    <strong class="label">Continue on SGScript Error:</strong>
                                    <span class="value">{{ hasProp(cluster, 'data.spec.managedSql.continueOnSGScriptError') ? isEnabled(cluster.data.spec.managedSql.continueOnSGScriptError) : 'Disabled' }}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="showDefaults || cluster.data.spec.pods.disableConnectionPooling || hasProp(cluster, 'data.spec.configurations.sgPoolingConfig') || cluster.data.spec.pods.disablePostgresUtil || cluster.data.spec.pods.disableMetricsExporter || cluster.data.spec.prometheusAutobind || hasProp(cluster, 'data.spec.distributedLogs.sgDistributedLogs')">
                        <li>
                            <strong class="sectionTitle">Sidecars</strong>
                            <ul>
                                <li v-if="showDefaults || cluster.data.spec.pods.disableConnectionPooling || hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                    <strong class="sectionTitle">Connection Pooling</strong>
                                    <span v-if="showDefaults || cluster.data.spec.pods.disableConnectionPooling"><strong>:</strong> {{ isEnabled(cluster.data.spec.pods.disableConnectionPooling, true) }}</span>
                                    <ul v-if="(showDefaults && !cluster.data.spec.pods.disableConnectionPooling) || hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                        <li>
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
                                    <span><strong>:</strong> {{ isEnabled(cluster.data.spec.pods.disablePostgresUtil, true) }}</span>
                                </li>
                                <li v-if="showDefaults || cluster.data.spec.pods.disableMetricsExporter || cluster.data.spec.prometheusAutobind">
                                    <strong class="sectionTitle">Monitoring</strong>
                                    <span><strong>:</strong> {{ (!cluster.data.spec.pods.disableMetricsExporter && cluster.data.spec.prometheusAutobind) ? ' Enabled' : ' Disabled' }}</span>
                                    <ul>
                                        <li v-if="showDefaults || cluster.data.spec.pods.disableMetricsExporter">
                                            <strong class="sectionTitle">Metrics Exporter</strong>
                                            <span><strong>:</strong> {{ isEnabled(cluster.data.spec.pods.disableMetricsExporter, true) }}</span>
                                        </li>
                                        <li v-if="showDefaults || cluster.data.spec.prometheusAutobind">
                                            <strong class="sectionTitle">Prometheus Autobind</strong>
                                            <span><strong>:</strong> {{ isEnabled(cluster.data.spec.prometheusAutobind) }}</span>
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

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.replicateFrom')">
                        <li>
                            <strong class="sectionTitle">
                                Replicate From
                            </strong>
                            <ul>
                                <li>
                                    <strong class="label">Source:</strong>
                                    <span class="value">
                                        <template v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.sgCluster')">
                                            Local Cluster
                                        </template>
                                        <template v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.external')">
                                            External Instance
                                        </template>
                                        <template v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.external') && hasProp(cluster, 'data.spec.replicateFrom.storage')">
                                             + 
                                        </template>
                                        <template v-if="hasProp(cluster, 'data.spec.replicateFrom.storage')">
                                            Object Storage
                                        </template>
                                    </span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.sgCluster')">
                                    <strong class="label">Cluster:</strong>
                                    <span class="value">
                                        <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.data.spec.replicateFrom.instance.sgCluster" target="_blank">
                                            {{ cluster.data.spec.replicateFrom.instance.sgCluster }}
                                        </router-link>
                                    </span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.external')">
                                    <strong class="sectionTitle">
                                        External Instance Specs
                                    </strong>
                                    <ul>
                                        <li>
                                            <strong class="label">Host:</strong>
                                            <span class="value">{{ cluster.data.spec.replicateFrom.instance.external.host }}</span>
                                        </li>
                                        <li>
                                            <strong class="label">Port:</strong>
                                            <span class="value">{{ cluster.data.spec.replicateFrom.instance.external.port }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.storage')">
                                    <strong class="sectionTitle">
                                        Storage Specs
                                    </strong>
                                    <ul>
                                        <li>
                                            <strong class="label">Object Storage:</strong>
                                            <span class="value">
                                                <router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + cluster.data.spec.replicateFrom.storage.sgObjectStorage" target="_blank">
                                                    {{ cluster.data.spec.replicateFrom.storage.sgObjectStorage }}
                                                </router-link>
                                            </span>
                                        </li>
                                        <li>
                                            <strong class="label">Path:</strong>
                                            <span class="value">{{ cluster.data.spec.replicateFrom.storage.path }}</span>
                                        </li>
                                        <li v-if="(
                                            hasProp(cluster, 'data.spec.replicateFrom.storage.performance') && (
                                                ( (hasProp(cluster, 'data.spec.replicateFrom.storage.performance.downloadConcurrency')) && (cluster.data.spec.replicateFrom.storage.performance.downloadConcurrency.length) )||
                                                ( (hasProp(cluster, 'data.spec.replicateFrom.storage.performance.maxNetworkBandwidth')) & (cluster.data.spec.replicateFrom.storage.performance.maxNetworkBandwidth.length) ) ||
                                                ( (hasProp(cluster, 'data.spec.replicateFrom.storage.performance.maxDiskBandwidth')) && (cluster.data.spec.replicateFrom.storage.performance.maxDiskBandwidth.length) )
                                            ) 
                                        )">
                                            <strong class="sectionTitle">
                                                Performance Specs
                                            </strong>
                                            <ul>
                                                <li v-if="cluster.data.spec.replicateFrom.storage.performance.downloadConcurrency.length">
                                                    <strong class="label">Download Concurrency:</strong>
                                                    <span class="value">{{ cluster.data.spec.replicateFrom.storage.performance.downloadConcurrency }}</span>
                                                </li>
                                                <li v-if="cluster.data.spec.replicateFrom.storage.performance.maxDiskBandwidth.length">
                                                    <strong class="label">Maximum Disk Bandwidth:</strong>
                                                    <span class="value">{{ cluster.data.spec.replicateFrom.storage.performance.maxDiskBandwidth }}</span>
                                                </li>
                                                <li v-if="cluster.data.spec.replicateFrom.storage.performance.maxNetworkBandwidth.length">
                                                    <strong class="label">Maximum Network Bandwidth:</strong>
                                                    <span class="value">{{ cluster.data.spec.replicateFrom.storage.performance.maxNetworkBandwidth }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.users')">
                                    <strong class="sectionTitle">
                                        Users Credentials
                                    </strong>
                                    <ul>
                                        <li>
                                            <strong class="sectionTitle">
                                                Superuser
                                            </strong>
                                            <ul>
                                                <li>
                                                    <strong class="sectionTitle">
                                                        Username
                                                    </strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.superuser.username.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.superuser.username.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li>
                                                    <strong class="sectionTitle">
                                                        Password
                                                    </strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.superuser.password.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.superuser.password.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                        <li>
                                            <strong class="sectionTitle">
                                                Replication
                                            </strong>
                                            <ul>
                                                <li>
                                                    <strong class="sectionTitle">
                                                        Username
                                                    </strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.replication.username.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.replication.username.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li>
                                                    <strong class="sectionTitle">
                                                        Password
                                                    </strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.replication.password.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.replication.password.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                        <li>
                                            <strong class="sectionTitle">
                                                Authenticator
                                            </strong>
                                            <ul>
                                                <li>
                                                    <strong class="sectionTitle">
                                                        Username
                                                    </strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.authenticator.username.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.authenticator.username.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li>
                                                    <strong class="sectionTitle">
                                                        Password
                                                    </strong>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.authenticator.password.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key:</strong>
                                                            <span class="value">{{ cluster.data.spec.replicateFrom.users.authenticator.password.key }}</span>
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

                    <ul
                        class="section"
                        v-if="( 
                            (cluster.data.spec.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.pods.customVolumes)) || 
                            (cluster.data.spec.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.pods.customInitContainers)) || 
                            (cluster.data.spec.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.pods.customContainers)) )
                        ">
                        <li>
                            <strong class="sectionTitle">
                                User-Supplied Pods' Sidecars
                            </strong>
                            <ul>
                                <li v-if="cluster.data.spec.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.pods.customVolumes)">
                                    <strong class="sectionTitle">
                                        Custom Volumes
                                    </strong>

                                    <ul>
                                        <template v-for="(vol, index) in cluster.data.spec.pods.customVolumes">
                                            <li :key="index">
                                                <strong class="sectionTitle">
                                                    Volume #{{ index + 1 }}
                                                </strong>

                                                <ul>
                                                    <li v-if="vol.hasOwnProperty('name')">
                                                        <strong class="label">Name:</strong>
                                                        <span class="value">{{ vol.name }}</span>
                                                    </li>
                                                    <li v-if="vol.hasOwnProperty('emptyDir') && Object.keys(vol.emptyDir).length && (!isNull(vol.emptyDir.medium) || !isNull(vol.emptyDir.sizeLimit))">
                                                        <strong class="sectionTitle">
                                                            Empty Directory
                                                        </strong>

                                                        <ul>
                                                            <li v-if="vol.emptyDir.hasOwnProperty('medium') && !isNull(vol.emptyDir.medium)">
                                                                <strong class="label">Medium:</strong>
                                                                <span class="value">{{ vol.emptyDir.medium }}</span>
                                                            </li>
                                                            <li v-if="vol.emptyDir.hasOwnProperty('sizeLimit') && !isNull(vol.emptyDir.sizeLimit)">
                                                                <strong class="label">Size Limit:</strong>
                                                                <span class="value">{{ vol.emptyDir.sizeLimit }}</span>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                    <li v-if="vol.hasOwnProperty('configMap') && Object.keys(vol.configMap).length">
                                                        <strong class="sectionTitle">
                                                            Config Map
                                                        </strong>

                                                        <ul>
                                                            <li v-if="vol.configMap.hasOwnProperty('name') && !isNull(vol.configMap.name)">
                                                                <strong class="label">Name:</strong>
                                                                <span class="value">{{ vol.configMap.name }}</span>
                                                            </li>
                                                            <li v-if="vol.configMap.hasOwnProperty('optional')">
                                                                <strong class="label">Optional:</strong>
                                                                <span class="value">{{ isEnabled(vol.configMap.optional) }}</span>
                                                            </li>
                                                            <li v-if="vol.configMap.hasOwnProperty('defaultMode') && !isNull(vol.configMap.defaultMode)">
                                                                <strong class="label">Default Mode:</strong>
                                                                <span class="value">{{ vol.configMap.defaultMode }}</span>
                                                            </li>
                                                            <li v-if="vol.configMap.hasOwnProperty('items') && vol.configMap.items.length">
                                                                <strong class="sectionTitle">
                                                                    Items
                                                                </strong>

                                                                <ul>
                                                                    <template v-for="(item, index) in vol.configMap.items">
                                                                        <li :key="index">
                                                                            <strong class="sectionTitle">
                                                                                Item #{{ index + 1 }}
                                                                            </strong>

                                                                            <ul>
                                                                                <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                    <strong class="label">Key:</strong>
                                                                                    <span class="value">{{ item.key }}</span>
                                                                                </li>
                                                                                <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                    <strong class="label">Mode:</strong>
                                                                                    <span class="value">{{ item.mode }}</span>
                                                                                </li>
                                                                                <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                    <strong class="label">Path:</strong>
                                                                                    <span class="value">{{ item.path }}</span>
                                                                                </li>
                                                                            </ul>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                    <li v-if="vol.hasOwnProperty('secret') && Object.keys(vol.secret).length">
                                                        <strong class="sectionTitle">
                                                            Secret
                                                        </strong>

                                                        <ul>
                                                            <li v-if="vol.secret.hasOwnProperty('name') && !isNull(vol.secret.secretName)">
                                                                <strong class="label">Secret Name:</strong>
                                                                <span class="value">{{ vol.secret.secretName }}</span>
                                                            </li>
                                                            <li v-if="vol.secret.hasOwnProperty('optional')">
                                                                <strong class="label">Optional:</strong>
                                                                <span class="value">{{ isEnabled(vol.secret.optional) }}</span>
                                                            </li>
                                                            <li v-if="vol.secret.hasOwnProperty('defaultMode') && !isNull(vol.secret.defaultMode)">
                                                                <strong class="label">Default Mode:</strong>
                                                                <span class="value">{{ vol.secret.defaultMode }}</span>
                                                            </li>
                                                            <li v-if="vol.secret.hasOwnProperty('items') && vol.secret.items.length">
                                                                <strong class="sectionTitle">
                                                                    Items
                                                                </strong>

                                                                <ul>
                                                                    <template v-for="(item, index) in vol.secret.items">
                                                                        <li :key="index">
                                                                            <strong class="sectionTitle">
                                                                                Item #{{ index + 1 }}
                                                                            </strong>

                                                                            <ul>
                                                                                <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                    <strong class="label">Key:</strong>
                                                                                    <span class="value">{{ item.key }}</span>
                                                                                </li>
                                                                                <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                    <strong class="label">Mode:</strong>
                                                                                    <span class="value">{{ item.mode }}</span>
                                                                                </li>
                                                                                <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                    <strong class="label">Path:</strong>
                                                                                    <span class="value">{{ item.path }}</span>
                                                                                </li>
                                                                            </ul>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                </ul>
                                            </li>
                                        </template>
                                    </ul>
                                </li>
                                <li v-if="cluster.data.spec.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.pods.customInitContainers)">
                                    <strong class="sectionTitle">
                                        Custom Init Containers
                                    </strong>

                                    <ul>
                                        <template v-for="(container, index) in cluster.data.spec.pods.customInitContainers">
                                            <li :key="'container-' + index">
                                                <strong class="sectionTitle">
                                                    Init Container #{{ index + 1 }}
                                                </strong>

                                                <ul>
                                                    <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                        <strong class="label">Name:</strong>
                                                        <span class="value">{{ container.name }}</span>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                        <strong class="label">Image:</strong>
                                                        <span class="value">{{ container.image }}</span>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                        <strong class="label">Image Pull Policy:</strong>
                                                        <span class="value">{{ container.imagePullPolicy }}</span>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                        <strong class="label">Working Directory:</strong>
                                                        <span class="value">{{ container.workingDir }}</span>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                        <strong class="sectionTitle">
                                                            Arguments
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(arg, aIndex) in container.args">
                                                                <li :key="'argument-' + index + '-' + aIndex">
                                                                    <span class="value">{{ arg }}</span>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('command') && !isNull(container.command)">
                                                        <strong class="sectionTitle">
                                                            Command
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(command, cIndex) in container.command">
                                                                <li :key="'command-' + index + '-' + cIndex">
                                                                    <span class="value">{{ command }}</span>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('env') && !isNull(container.env)">
                                                        <strong class="sectionTitle">
                                                            Environment Variables
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(envVar, vIndex) in container.env">
                                                                <li :key="'var-' + index + '-' + vIndex">
                                                                    <strong class="label">{{ envVar.name }}:</strong>
                                                                    <span class="value">{{ envVar.value }}</span>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('ports') && !isNull(container.ports)">
                                                        <strong class="sectionTitle">
                                                            Ports
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(port, pIndex) in container.ports">
                                                                <li :key="'port-' + index + '-' + pIndex">
                                                                    <strong class="sectionTitle">
                                                                        Port #{{ pIndex + 1 }}
                                                                    </strong>

                                                                    <ul>
                                                                        <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                            <strong class="label">Name:</strong>
                                                                            <span class="value">{{ port.name }}</span>
                                                                        </li>
                                                                        <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                            <strong class="label">Host IP:</strong>
                                                                            <span class="value">{{ port.hostIP }}</span>
                                                                        </li>
                                                                        <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                            <strong class="label">Host Port:</strong>
                                                                            <span class="value">{{ port.hostPort }}</span>
                                                                        </li>
                                                                        <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                            <strong class="label">Container Port:</strong>
                                                                            <span class="value">{{ port.containerPort }}</span>
                                                                        </li>
                                                                        <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                            <strong class="label">Protocol:</strong>
                                                                            <span class="value">{{ port.protocol }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('volumeMounts') && !isNull(container.volumeMounts)">
                                                        <strong class="sectionTitle">
                                                            Volume Mounts
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                <li :key="'vol-' + index + '-' + vIndex">
                                                                    <strong class="sectionTitle">
                                                                        Volume #{{ vIndex + 1 }}
                                                                    </strong>

                                                                    <ul>
                                                                        <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                            <strong class="label">Name:</strong>
                                                                            <span class="value">{{ vol.name }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('readOnly')">
                                                                            <strong class="label">Read Only:</strong>
                                                                            <span class="value">{{ isEnabled(vol.readOnly) }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                            <strong class="label">Mount Path:</strong>
                                                                            <span class="value">{{ vol.mountPath }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                            <strong class="label">Mount Propagation:</strong>
                                                                            <span class="value">{{ vol.mountPropagation }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                            <strong class="label">Sub Path:</strong>
                                                                            <span class="value">{{ vol.subPath }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                            <strong class="label">Sub Path Expr:</strong>
                                                                            <span class="value">{{ vol.subPathExpr }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                </ul>
                                            </li>
                                        </template>
                                    </ul>
                                </li>
                                <li v-if="cluster.data.spec.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.pods.customContainers)">
                                    <strong class="sectionTitle">
                                        Custom Containers
                                    </strong>

                                    <ul>
                                        <template v-for="(container, index) in cluster.data.spec.pods.customContainers">
                                            <li :key="'container-' + index">
                                                <strong class="sectionTitle">
                                                    Container #{{ index + 1 }}
                                                </strong>

                                                <ul>
                                                    <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                        <strong class="label">Name:</strong>
                                                        <span class="value">{{ container.name }}</span>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                        <strong class="label">Image:</strong>
                                                        <span class="value">{{ container.image }}</span>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                        <strong class="label">Image Pull Policy:</strong>
                                                        <span class="value">{{ container.imagePullPolicy }}</span>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                        <strong class="label">Working Directory:</strong>
                                                        <span class="value">{{ container.workingDir }}</span>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                        <strong class="sectionTitle">
                                                            Arguments
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(arg, aIndex) in container.args">
                                                                <li :key="'argument-' + index + '-' + aIndex">
                                                                    <span class="value">{{ arg }}</span>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('command') && !isNull(container.command)">
                                                        <strong class="sectionTitle">
                                                            Command
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(command, cIndex) in container.command">
                                                                <li :key="'command-' + index + '-' + cIndex">
                                                                    <span class="value">{{ command }}</span>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('env') && !isNull(container.env)">
                                                        <strong class="sectionTitle">
                                                            Environment Variables
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(envVar, vIndex) in container.env">
                                                                <li :key="'var-' + index + '-' + vIndex">
                                                                    <strong class="label">{{ envVar.name }}:</strong>
                                                                    <span class="value">{{ envVar.value }}</span>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('ports') && !isNull(container.ports)">
                                                        <strong class="sectionTitle">
                                                            Ports
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(port, pIndex) in container.ports">
                                                                <li :key="'port-' + index + '-' + pIndex">
                                                                    <strong class="sectionTitle">
                                                                        Port #{{ pIndex + 1 }}
                                                                    </strong>

                                                                    <ul>
                                                                        <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                            <strong class="label">Name:</strong>
                                                                            <span class="value">{{ port.name }}</span>
                                                                        </li>
                                                                        <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                            <strong class="label">Host IP:</strong>
                                                                            <span class="value">{{ port.hostIP }}</span>
                                                                        </li>
                                                                        <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                            <strong class="label">Host Port:</strong>
                                                                            <span class="value">{{ port.hostPort }}</span>
                                                                        </li>
                                                                        <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                            <strong class="label">Container Port:</strong>
                                                                            <span class="value">{{ port.containerPort }}</span>
                                                                        </li>
                                                                        <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                            <strong class="label">Protocol:</strong>
                                                                            <span class="value">{{ port.protocol }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                    <li v-if="container.hasOwnProperty('volumeMounts') && !isNull(container.volumeMounts)">
                                                        <strong class="sectionTitle">
                                                            Volume Mounts
                                                        </strong>

                                                        <ul>
                                                            <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                <li :key="'vol-' + index + '-' + vIndex">
                                                                    <strong class="sectionTitle">
                                                                        Volume #{{ vIndex + 1 }}
                                                                    </strong>

                                                                    <ul>
                                                                        <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                            <strong class="label">Name:</strong>
                                                                            <span class="value">{{ vol.name }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('readOnly')">
                                                                            <strong class="label">Read Only:</strong>
                                                                            <span class="value">{{ isEnabled(vol.readOnly) }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                            <strong class="label">Mount Path:</strong>
                                                                            <span class="value">{{ vol.mountPath }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                            <strong class="label">Mount Propagation:</strong>
                                                                            <span class="value">{{ vol.mountPropagation }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                            <strong class="label">Sub Path:</strong>
                                                                            <span class="value">{{ vol.subPath }}</span>
                                                                        </li>
                                                                        <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                            <strong class="label">Sub Path Expr:</strong>
                                                                            <span class="value">{{ vol.subPathExpr }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </template>
                                                        </ul>
                                                    </li>
                                                </ul>
                                            </li>
                                        </template>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="( showDefaults || ( (cluster.data.spec.replication.role != 'ha-read') || (cluster.data.spec.replication.mode != 'async') || cluster.data.spec.replication.hasOwnProperty('groups') ) )">
                        <li>
                            <strong class="sectionTitle">
                                Pods Replication
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

                    <ul class="section" v-if="showDefaults || !cluster.data.spec.postgresServices.primary.enabled || (cluster.data.spec.postgresServices.primary.type != 'ClusterIP') || !cluster.data.spec.postgresServices.replicas.enabled || (cluster.data.spec.postgresServices.replicas.type != 'ClusterIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('customPorts') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('customPorts')">
                        <li>
                            <strong class="sectionTitle">Customize generated Kubernetes service</strong>
                            <ul>
                                <li v-if="showDefaults || !cluster.data.spec.postgresServices.primary.enabled || (cluster.data.spec.postgresServices.primary.type != 'ClusterIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('customPorts')">
                                    <strong class="sectionTitle">Primary Service</strong>
                                    <span><strong>:</strong> {{ isEnabled(cluster.data.spec.postgresServices.primary.enabled) }}</span>
                                    <ul v-if="( showDefaults || (cluster.data.spec.postgresServices.primary.type != 'ClusterIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('customPorts'))">
                                        <li v-if="( showDefaults || (cluster.data.spec.postgresServices.primary.type != 'ClusterIP') )">
                                            <strong class="label">Type:</strong>
                                            <span class="value">{{ cluster.data.spec.postgresServices.primary.type }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP')">
                                            <strong class="label">Load Balancer IP:</strong>
                                            <span class="value">{{ cluster.data.spec.postgresServices.primary.loadBalancerIP }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.primary.hasOwnProperty('customPorts')">
                                            <strong class="sectionTitle">Custom Ports</strong>
                                            <ul>
                                                <li v-for="(port, index) in cluster.data.spec.postgresServices.primary.customPorts">
                                                    <strong class="sectionTitle">Port #{{ index + 1 }}</strong>
                                                    <ul>
                                                        <li v-if="port.hasOwnProperty('appProtocol') && (port.appProtocol != null)">
                                                            <strong class="label">Application Protocol:</strong>
                                                            <span class="value">{{ port.appProtocol }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('name') && (port.name != null)">
                                                            <strong class="label">Name:</strong>
                                                            <span class="value">{{ port.name }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('nodePort') && (port.nodePort != null)">
                                                            <strong class="label">Node Port:</strong>
                                                            <span class="value">{{ port.nodePort }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('port')">
                                                            <strong class="label">Port:</strong>
                                                            <span class="value">{{ port.port }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('protocol') && (port.protocol != null)">
                                                            <strong class="label">Protocol:</strong>
                                                            <span class="value">{{ port.protocol }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('targetPort') && (port.targetPort != null)">
                                                            <strong class="label">Target Port:</strong>
                                                            <span class="value">{{ port.targetPort }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="showDefaults || !cluster.data.spec.postgresServices.replicas.enabled || (cluster.data.spec.postgresServices.replicas.type != 'ClusterIP') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('customPorts')">
                                    <strong class="sectionTitle">Replicas Service</strong>
                                    <span><strong>:</strong> {{ isEnabled(cluster.data.spec.postgresServices.replicas.enabled) }}</span>
                                    <ul v-if="( showDefaults || (cluster.data.spec.postgresServices.replicas.type != 'ClusterIP') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('customPorts'))">
                                        <li v-if="( showDefaults || (cluster.data.spec.postgresServices.replicas.type != 'ClusterIP') )">
                                            <strong class="label">Type:</strong>
                                            <span class="value">{{ cluster.data.spec.postgresServices.replicas.type }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP')">
                                            <strong class="label">Load Balancer IP:</strong>
                                            <span class="value">{{ cluster.data.spec.postgresServices.replicas.loadBalancerIP }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.replicas.hasOwnProperty('customPorts')">
                                            <strong class="sectionTitle">Custom Ports</strong>
                                            <ul>
                                                <li v-for="(port, index) in cluster.data.spec.postgresServices.replicas.customPorts">
                                                    <strong class="sectionTitle">Port #{{ index + 1 }}</strong>
                                                    <ul>
                                                        <li v-if="port.hasOwnProperty('appProtocol') && (port.appProtocol != null)">
                                                            <strong class="label">Application Protocol:</strong>
                                                            <span class="value">{{ port.appProtocol }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('name') && (port.name != null)">
                                                            <strong class="label">Name:</strong>
                                                            <span class="value">{{ port.name }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('nodePort') && (port.nodePort != null)">
                                                            <strong class="label">Node Port:</strong>
                                                            <span class="value">{{ port.nodePort }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('port')">
                                                            <strong class="label">Port:</strong>
                                                            <span class="value">{{ port.port }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('protocol') && (port.protocol != null)">
                                                            <strong class="label">Protocol:</strong>
                                                            <span class="value">{{ port.protocol }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('targetPort') && (port.targetPort != null)">
                                                            <strong class="label">Target Port:</strong>
                                                            <span class="value">{{ port.targetPort }}</span>
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
                                                <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                                    <strong class="label">Toleration Seconds:</strong>
                                                    <span class="value">{{ toleration.tolerationSeconds }}</span>
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
                                    <span><strong>:</strong> {{ hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') ? isEnabled(cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity, true) : 'Enabled'}}</span>
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
        height: calc(100% - 12px);
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