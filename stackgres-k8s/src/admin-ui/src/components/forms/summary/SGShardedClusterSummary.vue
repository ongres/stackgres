<template>
	<div
        id="crdSummary"
        class="visible loadingContainer"
        :class="details ? 'crdDetails' : 'contentTooltip'"
    >
        <div v-if="!details" class="close" @click="closeSummary()"></div>
        
        <div class="info">
        
            <span v-if="!details" class="close" @click="closeSummary()">CLOSE</span>
            
            <div class="content">
                <div v-if="!details" class="header">
                    <template v-if="dryRun">
                        <h2>Dry Run Results</h2>
                    </template>
                    <template v-else>
                        <h2>Summary</h2>
                    </template>
                    <label
                        v-if="!dryRun" for="showDefaults" class="switch floatRight upper">
                        <span>Show Default Values</span>
                        <input type="checkbox" id="showDefaults" class="switch" v-model="showDefaults">
                    </label>
                </div>

                <div class="summary" v-if="cluster.hasOwnProperty('data')">
                    <strong class="sectionTitle">General</strong>
                    <ul class="section">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Metadata </strong>
                            <ul>
                                <li v-if="showDefaults">
                                    <strong class="label">Namespace</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.metadata.namespace')"></span>
                                    <span class="value"> : 
                                        <router-link :to="'/' + cluster.data.metadata.namespace">
                                            {{ cluster.data.metadata.namespace }}
                                            <span class="eyeIcon"></span>
                                        </router-link>
                                    </span>
                                </li>
                                <li>
                                    <strong class="label">Name</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.metadata.name')"></span>
                                    <span class="value"> :
                                        <router-link :to="'/' + cluster.data.metadata.namespace + '/sgshardedcluster/' + cluster.data.metadata.name">
                                            {{ cluster.data.metadata.name }}
                                            <span class="eyeIcon"></span>
                                        </router-link>
                                    </span>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Specs</strong>
                            <ul>
                                <li v-if="showDefaults || (cluster.data.spec.profile != 'production')">
                                    <strong class="label">Profile</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.profile')"></span>
                                    <span class="value capitalize"> : {{ cluster.data.spec.profile }}</span>
                                </li>
                                <li>
                                    <strong class="label">Database</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.database')"></span>
                                    <span class="value"> : {{ cluster.data.spec.database }}</span>
                                </li>
                                <li v-if="(showDefaults || (cluster.data.spec.type !== 'citus'))">
                                    <strong class="label">Type</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.type')"></span>
                                    <span class="value"> : {{ cluster.data.spec.type }}</span>
                                </li>
                                <li v-if="(showDefaults || (cluster.data.spec.postgres.flavor != 'vanilla') || (cluster.data.spec.postgres.version != 'latest') || (hasProp(cluster, 'data.spec.postgres.ssl.enabled') && !cluster.data.spec.postgres.ssl.enabled) )">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Postgres</strong>
                                     <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres')"></span>
                                    <ul>
                                        <li v-if="(cluster.data.spec.postgres.flavor != 'vanilla') || showDefaults">
                                            <strong class="label">Flavor</strong>
                                             <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.flavor')"></span>
                                            <span class="value capitalize"> : {{ cluster.data.spec.postgres.flavor }}</span>
                                        </li>
                                        <li v-if="(cluster.data.spec.postgres.version != 'latest') || showDefaults">
                                            <strong class="label">Version</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.version')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgres.version }}</span>
                                        </li>
                                        <li v-if="showDefaults || (hasProp(cluster, 'data.spec.postgres.ssl.enabled') && !cluster.data.spec.postgres.ssl.enabled)">
                                            <template v-if="Object.keys(cluster.data.spec.postgres.ssl).length > 1">
                                                <button class="toggleSummary"></button>
                                            </template>
                                            <strong class="label">SSL Connections</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl')"></span>
                                            <span class="value"> : {{ hasProp(cluster, 'data.spec.postgres.ssl.enabled') ? isEnabled(cluster.data.spec.postgres.ssl.enabled) : 'Disabled' }}</span>
                                            <ul v-if="hasProp(cluster, 'data.spec.postgres.ssl.certificateSecretKeySelector') || hasProp(cluster, 'data.spec.postgres.ssl.privateKeySecretKeySelector')">
                                                <li v-if="hasProp(cluster, 'data.spec.postgres.ssl.certificateSecretKeySelector')">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Certificate Secret Key Selector</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.certificateSecretKeySelector')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.certificateSecretKeySelector.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.postgres.ssl.certificateSecretKeySelector.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Key</strong>
                                                             <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.certificateSecretKeySelector.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.postgres.ssl.certificateSecretKeySelector.key }}</span>
                                                        </li>                                                                            
                                                    </ul>
                                                </li>
                                                <li v-if="hasProp(cluster, 'data.spec.postgres.ssl.privateKeySecretKeySelector')">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Private Key Secret Key Selector</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.privateKeySecretKeySelector')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.privateKeySecretKeySelector.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.postgres.ssl.privateKeySecretKeySelector.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Key</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.privateKeySecretKeySelector.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.postgres.ssl.privateKeySecretKeySelector.key }}</span>
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

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.postgres.extensions')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Postgres Extensions Deployed/To Be Deployed </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.extensions')"></span>
                            <ul>
                                <li class="warning">
                                    <span>The extension(s) are installed into the StackGres Postgres container. To start using them, you need to execute an appropriate <code>CREATE EXTENSION</code> command in the database(s) where you want to use the extension(s). Note that depending on each extension's requisites you may also need to add configuration to the cluster's <code>SGPostgresConfig</code> configuration, like adding the extension to <code>shared_preload_libraries</code> or adding extension-specific configuration parameters.</span>
                                </li>
                                <li v-for="ext in cluster.data.spec.postgres.extensions" :set="extData = extensionsList.find(e => (e.name == ext.name))">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">{{ ext.name }}</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgextensions.extensions.name')"></span>
                                    <ul>
                                        <li>
                                            <strong class="label">Version</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgextensions.extensions.versions')"></span>
                                            <span class="value"> : {{ ext.version }}</span>
                                        </li>
                                        <template v-for="extInfo in extensionsList" v-if="ext.name == extInfo.name">
                                            <li>
                                                <strong class="label">Description</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgextensions.extensions.description')"></span>
                                                <span class="value"> : 
                                                    {{ extData.abstract }}
                                                    <template v-if="(ext.name == 'timescaledb_tsl')">
                                                        The license for this extension is not open source. Please check licensing details with the creators of the extension.
                                                    </template>
                                                </span>
                                            </li>
                                            <li>
                                                <strong class="label">Webpage</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgextensions.extensions.source')"></span>
                                                <span class="value"> : 
                                                    <a :href="extData.url" target="_blank">{{ extData.url }}</a>
                                                </span>
                                            </li>
                                        </template>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.configurations.backups')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Managed Backups Specs </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups')"></span>
                            <ul v-for="backup in cluster.data.spec.configurations.backups">
                                <li>
                                    <strong class="label">Object Storage</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.sgObjectStorage')"></span>
                                    <span class="value"> : 
                                        <router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + backup.sgObjectStorage" target="_blank"> 
                                            {{ backup.sgObjectStorage }}
                                            <span class="eyeIcon"></span>
                                        </router-link>
                                    </span>
                                </li>
                                <li v-if="!isNull(backup.cronSchedule) && ( showDefaults || ( backup.cronSchedule != '0 3 * * *' ) )">
                                    <strong class="label">Cron Schedule </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.cronSchedule')"></span>
                                    <span class="value"> : {{ tzCrontab(backup.cronSchedule) }} ({{ tzCrontab(backup.cronSchedule) | prettyCRON(false) }})</span>
                                </li>
                                <li>
                                    <button class="toggleSummary"></button>
                                    <strong class="sectionTitle">Paths </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.paths')"></span>
                                    <ul>
                                        <li v-for="(path, index) in backup.paths">
                                            <strong class="label">
                                                {{ !index ? 'Coordinator Path' : 'Shard Path #' + index }}
                                            </strong>
                                            <span class="value">: {{ path }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="( showDefaults || (backup.retention != 5) )">
                                    <strong class="label">Retention Window</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.retention')"></span>
                                    <span class="value"> : {{ backup.retention }}</span>
                                </li>
                                <li v-if="( showDefaults || (backup.compression != 'lz4') )">
                                    <strong class="label">Compression Method</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.compression')"></span>
                                    <span class="value"> : {{ backup.compression }}</span>
                                </li>
                                <li v-if="( 
                                    showDefaults || ( 
                                        ( hasProp(backup, 'performance.maxNetworkBandwidth') && !isNull(backup.performance.maxNetworkBandwidth) )|| 
                                        ( hasProp(backup, 'performance.maxDiskBandwidth') && !isNull(backup.performance.maxDiskBandwidth) )|| 
                                        (backup.performance.uploadDiskConcurrency != 1) 
                                    ) 
                                )">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Performance Specs</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.performance')"></span>
                                    <ul>
                                        <li v-if="( showDefaults || ( hasProp(backup, 'performance.maxNetworkBandwidth') && !isNull(backup.performance.maxNetworkBandwidth)) )">
                                            <strong class="label">Max Network Bandwidth</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.performance.properties.maxNetworkBandwidth')"></span>
                                            <span class="value"> : {{ ( hasProp(backup, 'performance.maxNetworkBandwidth') && !isNull(backup.performance.maxNetworkBandwidth) ) ? backup.performance.maxNetworkBandwidth : 'Unlimited' }} </span>
                                        </li>
                                        <li v-if="( showDefaults || ( hasProp(backup, 'performance.maxDiskBandwidth') && !isNull(backup.performance.maxDiskBandwidth)) )">
                                            <strong class="label">Max Disk Bandwidth</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.performance.properties.maxDiskBandwidth')"></span>
                                            <span class="value"> : {{ ( hasProp(backup, 'performance.maxDiskBandwidth') && !isNull(backup.performance.maxDiskBandwidth) ) ? backup.performance.maxDiskBandwidth : 'Unlimited' }} </span>
                                        </li>
                                        <li v-if="( showDefaults || ( hasProp(backup, 'performance.uploadDiskConcurrency') && (backup.performance.uploadDiskConcurrency != 1) ) )">
                                            <strong class="label">Upload Disk Concurrency</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.performance.properties.uploadDiskConcurrency')"></span>
                                            <span class="value"> : {{ hasProp(backup, 'performance.uploadDiskConcurrency') ? backup.performance.uploadDiskConcurrency : 1 }} </span>
                                        </li>
                                        
                                    </ul>
                                </li>
                                <li v-if="showDefaults || (hasProp(backup, 'useVolumeSnapshot') && backup.useVolumeSnapshot)">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">
                                        Volume Snapshot Specs
                                    </strong>
                                    <span
                                        class="helpTooltip"
                                        :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.useVolumeSnapshot')"
                                    ></span>
                                    <ul>
                                        <li v-if="showDefaults || (hasProp(backup, 'useVolumeSnapshot') && backup.useVolumeSnapshot)">
                                            <strong class="label">
                                                Use Volume Snapshot
                                            </strong>
                                            <span
                                                class="helpTooltip"
                                                :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.useVolumeSnapshot')"
                                            ></span>
                                            <span class="value"> : {{ isEnabled(backup.useVolumeSnapshot) }} </span>
                                        </li>
                                        <li v-if="hasProp(backup, 'volumeSnapshotClass') && !isNull(backup.volumeSnapshotClass)">
                                            <strong class="label">
                                                Volume Snapshot Class
                                            </strong>
                                            <span
                                                class="helpTooltip"
                                                :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.volumeSnapshotClass')"
                                            ></span>
                                            <span class="value"> : {{ backup.volumeSnapshotClass }} </span>
                                        </li>
                                        <li v-if="showDefaults || hasProp(backup, 'fastVolumeSnapshot') && backup.fastVolumeSnapshot">
                                            <strong class="label">
                                                Fast Volume Snapshot
                                            </strong>
                                            <span
                                                class="helpTooltip"
                                                :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.fastVolumeSnapshot')"
                                            ></span>
                                            <span class="value"> : {{ isEnabled(backup.fastVolumeSnapshot) }} </span>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="showDefaults || (hasProp(cluster, 'data.spec.prometheusAutobind') && cluster.data.spec.prometheusAutobind) || hasProp(cluster, 'data.spec.distributedLogs.sgDistributedLogs')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Sidecars </strong>
                            <ul>
                                <li v-if="showDefaults || cluster.data.spec.prometheusAutobind">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Monitoring</strong>
                                    <span class="helpTooltip" data-tooltip="StackGres supports enabling automatic monitoring for your Postgres cluster, but you need to provide or install the <a href='https://stackgres.io/doc/latest/install/prerequisites/monitoring/' target='_blank'>Prometheus stack as a pre-requisite</a>. Then, check this option to configure automatically sending metrics to the Prometheus stack."></span>
                                    <span> : {{ (!cluster.data.spec.coordinator.pods.disableMetricsExporter && !cluster.data.spec.shards.pods.disableMetricsExporter && cluster.data.spec.prometheusAutobind) ? ' Enabled' : ' Disabled' }}</span>
                                    <ul>
                                        <li v-if="showDefaults || cluster.data.spec.prometheusAutobind">
                                            <strong class="label">Prometheus Autobind</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.prometheusAutobind')"></span>
                                            <span> : {{ isEnabled(cluster.data.spec.prometheusAutobind) }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.distributedLogs.sgDistributedLogs')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Distributed Logs</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.distributedLogs')"></span>
                                    <ul>
                                        <li>
                                            <strong class="label">Logs Server</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.distributedLogs.sgDistributedLogs')"></span>
                                            <span class="value"> :
                                                <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.data.spec.distributedLogs.sgDistributedLogs" target="_blank">
                                                    {{ cluster.data.spec.distributedLogs.sgDistributedLogs }}
                                                     <span class="eyeIcon"></span>
                                                </router-link>
                                            </span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.distributedLogs.retention')">
                                            <strong class="label">Retention</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.distributedLogs.retention')"></span>
                                            <span class="value"> : {{ cluster.data.spec.distributedLogs.retention }}</span>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="( showDefaults || (hasProp(cluster, 'data.spec.replication.mode') && (cluster.data.spec.replication.mode != 'async')) )">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Pods Replication </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.replication')"></span>
                            <ul>
                                <li v-if="(showDefaults || (hasProp(cluster, 'data.spec.replication.mode') && cluster.data.spec.replication.mode != 'async') )">
                                    <strong class="label">Mode</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.replication.mode')"></span>
                                    <span class="value"> : {{ hasProp(cluster, 'data.spec.replication.mode') ? cluster.data.spec.replication.mode : 'async' }}</span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.replication.syncInstances')">
                                    <strong class="label">Sync Instances</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.replication.syncInstances')"></span>
                                    <span class="value"> : {{ cluster.data.spec.replication.syncInstances }}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.metadata.labels.clusterPods') || hasProp(cluster, 'data.spec.metadata.annotations')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Metadata </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata')"></span>
                            <ul>
                                <li v-if="hasProp(cluster, 'data.spec.pods.metadata')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Labels</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.labels')"></span>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.labels.clusterPods')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Cluster Pods</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.labels.clusterPods')"></span>
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
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Annotations</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations')"></span>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.allResources')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">All Resources</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.allResources')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.annotations.allResources">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.clusterPods')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Cluster Pods</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.clusterPods')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.annotations.clusterPods">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.services')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Services</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.services')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.annotations.services">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.primaryService')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Primary Service</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.primaryService')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.metadata.annotations.primaryService">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.replicasService')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Replicas Service</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.replicasService')"></span>
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

                    <ul class="section" v-if="
                        showDefaults || 
                        (hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') && (cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity != null)) ||
                        (hasProp(cluster, 'data.spec.nonProductionOptions.disablePatroniResourceRequirements') && (cluster.data.spec.nonProductionOptions.disablePatroniResourceRequirements != null)) ||
                        (hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterResourceRequirements') && (cluster.data.spec.nonProductionOptions.disableClusterResourceRequirements != null))
                    ">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Non Production Settings </strong>
                            <ul>
                                <li v-if="showDefaults || (hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') && (cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity != null))">
                                    <strong class="label">Cluster Pod Anti Affinity</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.nonProductionOptions.disableClusterPodAntiAffinity').replace('Set this property to true to allow','When disabled, it allows running')"></span>
                                    <span> : {{ (hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') && (cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity != null)) ? isEnabled(cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity, true) : 'Default'}}</span>
                                </li>
                                <li v-if="showDefaults || (hasProp(cluster, 'data.spec.nonProductionOptions.disablePatroniResourceRequirements') && (cluster.data.spec.nonProductionOptions.disablePatroniResourceRequirements != null))">
                                    <strong class="label">Patroni Resource Requirements</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.nonProductionOptions.disablePatroniResourceRequirements').replace('Set this property to true to allow','When disabled, it allows running')"></span>
                                    <span> : {{ (hasProp(cluster, 'data.spec.nonProductionOptions.disablePatroniResourceRequirements') && (cluster.data.spec.nonProductionOptions.disablePatroniResourceRequirements != null)) ? isEnabled(cluster.data.spec.nonProductionOptions.disablePatroniResourceRequirements, true) : 'Default'}}</span>
                                </li>
                                <li v-if="showDefaults || (hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterResourceRequirements') && (cluster.data.spec.nonProductionOptions.disableClusterResourceRequirements != null))">
                                    <strong class="label">Cluster Resourc eRequirements</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.nonProductionOptions.disableClusterResourceRequirements').replace('Set this property to true to allow','When disabled, it allows running')"></span>
                                    <span> : {{ (hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterResourceRequirements') && (cluster.data.spec.nonProductionOptions.disableClusterResourceRequirements != null)) ? isEnabled(cluster.data.spec.nonProductionOptions.disableClusterResourceRequirements, true) : 'Default'}}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <hr/><br/>

                    <strong class="sectionTitle">Coordinator </strong>
                    
                    <ul class="section" v-if="
                        showDefaults ||
                        (cluster.data.spec.coordinator.instances !== 1) ||
                        hasProp(cluster, 'data.spec.coordinator.sgInstanceProfile') ||
                        hasProp(cluster, 'data.spec.coordinator.configurations.sgPostgresConfig') ||
                        (cluster.data.spec.coordinator.pods.persistentVolume.size != '1Gi') || 
                        hasProp(cluster, 'data.spec.coordinator.pods.persistentVolume.storageClass')
                    ">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Specs </strong>
                            <ul>
                                <li v-if="(showDefaults || (cluster.data.spec.coordinator.instances !== 1))">
                                    <strong class="label">Instances</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.instances')"></span>
                                    <span> : {{ cluster.data.spec.coordinator.instances }}</span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.coordinator.sgInstanceProfile') || showDefaults">
                                    <strong class="label">Instance Profile </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.sgInstanceProfile')"></span>
                                    <span class="value">
                                        : 
                                        <template v-if="hasProp(cluster, 'data.spec.coordinator.sgInstanceProfile')">
                                            <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + profile.name" target="_blank" v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.coordinator.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )"> 
                                                {{ profile.data.metadata.name }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }})
                                                <span class="eyeIcon"></span>
                                            </router-link>
                                        </template>
                                        <template v-else>
                                            Default (Cores: 1, RAM: 2GiB)
                                        </template>
                                    </span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.coordinator.configurations.sgPostgresConfig') || showDefaults">
                                    <strong class="label">Postgres Configuration </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.configurations.sgPostgresConfig')"></span>
                                    <span class="value"> : 
                                        <template v-if="hasProp(cluster, 'data.spec.coordinator.configurations.sgPostgresConfig')">
                                            <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + cluster.data.spec.coordinator.configurations.sgPostgresConfig" target="_blank"> 
                                                {{ cluster.data.spec.coordinator.configurations.sgPostgresConfig }}
                                                <span class="eyeIcon"></span>
                                            </router-link>
                                        </template>
                                        <template v-else>
                                            Default
                                        </template>
                                    </span>
                                </li>
                                <li v-if="showDefaults || (cluster.data.spec.coordinator.pods.persistentVolume.size != '1Gi') || hasProp(cluster, 'data.spec.coordinator.pods.persistentVolume.storageClass')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Pods Storage</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods')"></span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Persistent Volume</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.persistentVolume')"></span>
                                            <ul>
                                                <li v-if="showDefaults || (cluster.data.spec.coordinator.pods.persistentVolume.size != '1Gi')">
                                                    <strong class="label">Volume Size</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.persistentVolume.size')"></span>
                                                    <span class="value"> : {{ cluster.data.spec.coordinator.pods.persistentVolume.size }}B</span>
                                                </li>
                                                <li v-if="hasProp(cluster, 'data.spec.coordinator.pods.persistentVolume.storageClass')">
                                                    <strong class="label">Storage Class</strong>
                                                     <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.persistentVolume.storageClass')"></span>
                                                    <span class="value"> : {{ cluster.data.spec.coordinator.pods.persistentVolume.storageClass }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="
                        showDefaults || 
                        hasProp(cluster, 'data.spec.coordinator.pods.disableConnectionPooling') || 
                        hasProp(cluster, 'data.spec.coordinator.configurations.sgPoolingConfig') || 
                        hasProp(cluster, 'data.spec.coordinator.pods.disablePostgresUtil') || 
                        hasProp(cluster, 'data.spec.coordinator.pods.disableMetricsExporter') ||
                        (cluster.data.spec.coordinator.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.coordinator.pods.customVolumes)) || 
                        (cluster.data.spec.coordinator.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.coordinator.pods.customInitContainers)) || 
                        (cluster.data.spec.coordinator.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.coordinator.pods.customContainers)) 
                    ">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Sidecars </strong>
                            <ul>
                                <li v-if="showDefaults || hasProp(cluster, 'data.spec.coordinator.pods.disableConnectionPooling') || hasProp(cluster, 'data.spec.coordinator.configurations.sgPoolingConfig')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Connection Pooling</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.configurations')"></span>
                                    <span v-if="showDefaults || cluster.data.spec.coordinator.pods.disableConnectionPooling"> : {{ isEnabled(cluster.data.spec.coordinator.pods.disableConnectionPooling, true) }}</span>
                                    <ul v-if="(showDefaults && !cluster.data.spec.coordinator.pods.disableConnectionPooling) || hasProp(cluster, 'data.spec.coordinator.configurations.sgPoolingConfig')">
                                        <li>
                                            <strong class="label">Connection Pooling Configuration</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.configurations.sgPoolingConfig')"></span>
                                            <span class="value"> :
                                                <template v-if="hasProp(cluster, 'data.spec.coordinator.configurations.sgPoolingConfig')">
                                                    <router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + cluster.data.spec.coordinator.configurations.sgPoolingConfig" target="_blank">
                                                        {{ cluster.data.spec.coordinator.configurations.sgPoolingConfig }}
                                                        <span class="eyeIcon"></span>
                                                    </router-link>
                                                </template>
                                                <template v-else>
                                                    Default
                                                </template>
                                            </span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || hasProp(cluster, 'data.spec.coordinator.pods.disablePostgresUtil')">
                                    <strong class="label">Postgres Utils</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.disablePostgresUtil').replace('If set to `true`', 'If disabled')"></span>
                                    <span> : {{ isEnabled(cluster.data.spec.coordinator.pods.disablePostgresUtil, true) }}</span>
                                </li>
                                <li v-if="showDefaults || hasProp(cluster, 'data.spec.coordinator.pods.disableMetricsExporter')">    
                                    <strong class="label">Metrics Exporter</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                                    <span> : {{ isEnabled(cluster.data.spec.coordinator.pods.disableMetricsExporter, true) }}</span>
                                </li>
                                <li v-if="( 
                                    (cluster.data.spec.coordinator.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.coordinator.pods.customVolumes)) || 
                                    (cluster.data.spec.coordinator.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.coordinator.pods.customInitContainers)) || 
                                    (cluster.data.spec.coordinator.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.coordinator.pods.customContainers)) )
                                ">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">User-Supplied Pods' Sidecars </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods')"></span>
                                    <ul>
                                        <li v-if="cluster.data.spec.coordinator.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.coordinator.pods.customVolumes)">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Volumes</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes')"></span>
                                            <ul>
                                                <template v-for="(vol, index) in cluster.data.spec.coordinator.pods.customVolumes">
                                                    <li :key="index">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Volume #{{ index + 1 }}</strong>
                                                        <ul>
                                                            <li v-if="vol.hasOwnProperty('name')">
                                                                <strong class="label">Name</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.name')"></span>
                                                                <span class="value"> : {{ vol.name }}</span>
                                                            </li>
                                                            <li v-if="vol.hasOwnProperty('emptyDir') && Object.keys(vol.emptyDir).length && (!isNull(vol.emptyDir.medium) || !isNull(vol.emptyDir.sizeLimit))">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Empty Directory</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.emptyDir')"></span>
                                                                <ul>
                                                                    <li v-if="vol.emptyDir.hasOwnProperty('medium') && !isNull(vol.emptyDir.medium)">
                                                                        <strong class="label">Medium</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.emptyDir.properties.medium')"></span>
                                                                        <span class="value"> : {{ vol.emptyDir.medium }}</span>
                                                                    </li>
                                                                    <li v-if="vol.emptyDir.hasOwnProperty('sizeLimit') && !isNull(vol.emptyDir.sizeLimit)">
                                                                        <strong class="label">Size Limit</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.emptyDir.properties.sizeLimit')"></span>
                                                                        <span class="value"> : {{ vol.emptyDir.sizeLimit }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                            <li v-if="vol.hasOwnProperty('configMap') && Object.keys(vol.configMap).length">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Config Map</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap')"></span>
                                                                <ul>
                                                                    <li v-if="vol.configMap.hasOwnProperty('name') && !isNull(vol.configMap.name)">
                                                                        <strong class="label">Name</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.name')"></span>
                                                                        <span class="value"> : {{ vol.configMap.name }}</span>
                                                                    </li>
                                                                    <li v-if="vol.configMap.hasOwnProperty('optional')">
                                                                        <strong class="label">Optional</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.name')"></span>
                                                                        <span class="value"> : {{ isEnabled(vol.configMap.optional) }}</span>
                                                                    </li>
                                                                    <li v-if="vol.configMap.hasOwnProperty('defaultMode') && !isNull(vol.configMap.defaultMode)">
                                                                        <strong class="label">Default Mode</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.defaultMode')"></span>
                                                                        <span class="value"> : {{ vol.configMap.defaultMode }}</span>
                                                                    </li>
                                                                    <li v-if="vol.configMap.hasOwnProperty('items') && vol.configMap.items.length">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Items</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.items')"></span>
                                                                        <ul>
                                                                            <template v-for="(item, index) in vol.configMap.items">
                                                                                <li :key="index">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Item #{{ index + 1 }}</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.items.items')"></span>
                                                                                    <ul>
                                                                                        <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                            <strong class="label">Key</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.items.items.properties.key')"></span>
                                                                                            <span class="value"> : {{ item.key }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                            <strong class="label">Mode</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.items.items.properties.mode')"></span>
                                                                                            <span class="value"> : {{ item.mode }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                            <strong class="label">Path</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.items.items.properties.path')"></span>
                                                                                            <span class="value"> : {{ item.path }}</span>
                                                                                        </li>
                                                                                    </ul>
                                                                                </li>
                                                                            </template>
                                                                        </ul>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                            <li v-if="vol.hasOwnProperty('secret') && Object.keys(vol.secret).length">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Secret</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret')"></span>
                                                                <ul>
                                                                    <li v-if="vol.secret.hasOwnProperty('secretName') && !isNull(vol.secret.secretName)">
                                                                        <strong class="label">Secret Name</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.secretName')"></span>
                                                                        <span class="value"> : {{ vol.secret.secretName }}</span>
                                                                    </li>
                                                                    <li v-if="vol.secret.hasOwnProperty('optional')">
                                                                        <strong class="label">Optional</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.optional')"></span>
                                                                        <span class="value"> : {{ isEnabled(vol.secret.optional) }}</span>
                                                                    </li>
                                                                    <li v-if="vol.secret.hasOwnProperty('defaultMode') && !isNull(vol.secret.defaultMode)">
                                                                        <strong class="label">Default Mode</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.defaultMode')"></span>
                                                                        <span class="value"> : {{ vol.secret.defaultMode }}</span>
                                                                    </li>
                                                                    <li v-if="vol.secret.hasOwnProperty('items') && vol.secret.items.length">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Items</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.items')"></span>
                                                                        <ul>
                                                                            <template v-for="(item, index) in vol.secret.items">
                                                                                <li :key="index">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Item #{{ index + 1 }}</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.items.items')"></span>
                                                                                    <ul>
                                                                                        <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                            <strong class="label">Key:</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.items.items.properties.key')"></span>
                                                                                            <span class="value"> : {{ item.key }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                            <strong class="label">Mode</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.items.items.properties.mode')"></span>
                                                                                            <span class="value"> : {{ item.mode }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                            <strong class="label">Path</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.items.items.properties.path')"></span>
                                                                                            <span class="value"> : {{ item.path }}</span>
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

                                        <li v-if="cluster.data.spec.coordinator.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.coordinator.pods.customInitContainers)">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Init Containers</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers')"></span>

                                            <ul>
                                                <template v-for="(container, index) in cluster.data.spec.coordinator.pods.customInitContainers">
                                                    <li :key="'container-' + index">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Init Container #{{ index + 1 }}</strong>
                                                        <ul>
                                                            <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                                <strong class="label">Name</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.name')"></span>
                                                                <span class="value"> : {{ container.name }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                                <strong class="label">Image</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.image')"></span>
                                                                <span class="value"> : {{ container.image }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                                <strong class="label">Image Pull Policy</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.imagePullPolicy')"></span>
                                                                <span class="value"> : {{ container.imagePullPolicy }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                                <strong class="label">Working Directory</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.workingDir')"></span>
                                                                <span class="value"> : {{ container.workingDir }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Arguments</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.arguments')"></span>
                                                                <ul>
                                                                    <template v-for="(arg, aIndex) in container.args">
                                                                        <li :key="'argument-' + index + '-' + aIndex">
                                                                            <span class="value">{{ arg }}</span>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('command') && !isNull(container.command)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Command</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.command')"></span>
                                                                <ul>
                                                                    <template v-for="(command, cIndex) in container.command">
                                                                        <li :key="'command-' + index + '-' + cIndex">
                                                                            <span class="value">{{ command }}</span>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('env') && !isNull(container.env)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Environment Variables</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.env')"></span>
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
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Ports</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports')"></span>
                                                                <ul>
                                                                    <template v-for="(port, pIndex) in container.ports">
                                                                        <li :key="'port-' + index + '-' + pIndex">
                                                                            <button class="toggleSummary"></button>
                                                                            <strong class="label">Port #{{ pIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ port.name }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                                    <strong class="label">Host IP</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.hostIP')"></span>
                                                                                    <span class="value"> : {{ port.hostIP }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                                    <strong class="label">Host Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.hostPort')"></span>
                                                                                    <span class="value"> : {{ port.hostPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                                    <strong class="label">Container Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.containerPort')"></span>
                                                                                    <span class="value"> : {{ port.containerPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                                    <strong class="label">Protocol</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.protocol')"></span>
                                                                                    <span class="value"> : {{ port.protocol }}</span>
                                                                                </li>
                                                                            </ul>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('volumeMounts') && !isNull(container.volumeMounts)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Volume Mounts</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts')"></span>
                                                                <ul>
                                                                    <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                        <li :key="'vol-' + index + '-' + vIndex">
                                                                            <strong class="label">Volume #{{ vIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ vol.name }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('readOnly')">
                                                                                    <strong class="label">Read Only</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                                    <span class="value"> : {{ isEnabled(vol.readOnly) }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                                    <strong class="label">Mount Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                                    <span class="value"> : {{ vol.mountPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                                    <strong class="label">Mount Propagation</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                                    <span class="value"> : {{ vol.mountPropagation }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                                    <strong class="label">Sub Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.subPath')"></span>
                                                                                    <span class="value"> : {{ vol.subPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                                    <strong class="label">Sub Path Expr</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                                    <span class="value"> : {{ vol.subPathExpr }}</span>
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

                                        <li v-if="cluster.data.spec.coordinator.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.coordinator.pods.customContainers)">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Containers</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers')"></span>
                                            <ul>
                                                <template v-for="(container, index) in cluster.data.spec.coordinator.pods.customContainers">
                                                    <li :key="'container-' + index">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Container #{{ index + 1 }}</strong>
                                                        <ul>
                                                            <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                                <strong class="label">Name</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.name')"></span>
                                                                <span class="value"> : {{ container.name }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                                <strong class="label">Image</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.image')"></span>
                                                                <span class="value"> : {{ container.image }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                                <strong class="label">Image Pull Policy</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.imagePullPolicy')"></span>
                                                                <span class="value"> : {{ container.imagePullPolicy }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                                <strong class="label">Working Directory</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.workingDir')"></span>
                                                                <span class="value"> : {{ container.workingDir }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Arguments</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.arguments')"></span>
                                                                <ul>
                                                                    <template v-for="(arg, aIndex) in container.args">
                                                                        <li :key="'argument-' + index + '-' + aIndex">
                                                                            <span class="value">{{ arg }}</span>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('command') && !isNull(container.command)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Command</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.command')"></span>
                                                                <ul>
                                                                    <template v-for="(command, cIndex) in container.command">
                                                                        <li :key="'command-' + index + '-' + cIndex">
                                                                            <span class="value">{{ command }}</span>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('env') && !isNull(container.env)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Environment Variables</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.env')"></span>
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
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Ports</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports')"></span>
                                                                <ul>
                                                                    <template v-for="(port, pIndex) in container.ports">
                                                                        <li :key="'port-' + index + '-' + pIndex">
                                                                            <strong class="label">Port #{{ pIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ port.name }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                                    <strong class="label">Host IP</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.hostIP')"></span>
                                                                                    <span class="value"> : {{ port.hostIP }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                                    <strong class="label">Host Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.hostPort')"></span>
                                                                                    <span class="value"> : {{ port.hostPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                                    <strong class="label">Container Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.containerPort')"></span>
                                                                                    <span class="value"> : {{ port.containerPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                                    <strong class="label">Protocol</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.protocol')"></span>
                                                                                    <span class="value"> : {{ port.protocol }}</span>
                                                                                </li>
                                                                            </ul>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('volumeMounts') && !isNull(container.volumeMounts)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Volume Mounts</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts')"></span>
                                                                <ul>
                                                                    <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                        <li :key="'vol-' + index + '-' + vIndex">
                                                                            <strong class="label">Volume #{{ vIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ vol.name }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('readOnly')">
                                                                                    <strong class="label">Read Only</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                                    <span class="value"> : {{ isEnabled(vol.readOnly) }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                                    <strong class="label">Mount Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                                    <span class="value"> : {{ vol.mountPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                                    <strong class="label">Mount Propagation</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                                    <span class="value"> : {{ vol.mountPropagation }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                                    <strong class="label">Sub Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.subPath')"></span>
                                                                                    <span class="value"> : {{ vol.subPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                                    <strong class="label">Sub Path Expr</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                                    <span class="value"> : {{ vol.subPathExpr }}</span>
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
                        </li>
                    </ul>
                    
                    <ul class="section" v-if="hasProp(cluster, 'data.spec.coordinator.managedSql')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Managed SQL </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.managedSql')"></span>
                            <ul>
                                <li>
                                    <button class="toggleSummary"></button>
                                    <strong class="sectionTitle">Scripts </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.managedSql.scripts')"></span>
                                    <ul>
                                        <li v-for="(baseScript, baseIndex) in cluster.data.spec.coordinator.managedSql.scripts">
                                            <button class="toggleSummary"></button>
                                            <strong class="sectionTitle">SGScript #{{ baseIndex + 1 }}</strong>
                                            <ul>
                                                <li v-if="( showDefaults || ( hasProp(baseScript, 'scriptSpec.continueOnError') && baseScript.scriptSpec.continueOnError ) )">
                                                    <strong class="label">Continue on Error</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.managedSql.scripts.continueOnError')"></span>
                                                    <span class="value"> : {{ hasProp(baseScript, 'scriptSpec.continueOnError') ? isEnabled(baseScript.continueOnError) : 'Disabled' }}</span>
                                                </li>
                                                <li v-if="( showDefaults || ( hasProp(baseScript, 'scriptSpec.managedVersions') && !baseScript.scriptSpec.managedVersions) )">
                                                    <strong class="label">Managed Versions:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.managedSql.scripts')"></span>
                                                    <span class="value">{{ hasProp(baseScript, 'scriptSpec.managedVersions') && isEnabled(baseScript.scriptSpec.managedVersions) }}</span>
                                                </li>
                                                <li v-if="baseScript.hasOwnProperty('scriptSpec')">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="sectionTitle">Script Entries</strong>

                                                    <ul>
                                                        <li v-for="(script, index) in baseScript.scriptSpec.scripts">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="sectionTitle">Script #{{ index + 1 }}</strong>

                                                            <ul>
                                                                <li v-if="hasProp(script, 'name')">
                                                                    <strong class="label">Name</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                                    <span class="value"> : {{ script.name }}</span>
                                                                </li>
                                                                <li v-if="hasProp(script, 'version')">
                                                                    <strong class="label">Version </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                                                    <span class="value"> : {{ script.version }}</span>
                                                                </li>
                                                                <li v-if="showDefaults || hasProp(script, 'database')">
                                                                    <strong class="label">Database</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('database') ? script.database : 'postgres' }}</span>
                                                                </li>
                                                                <li v-if="showDefaults || hasProp(script, 'user')">
                                                                    <strong class="label">User </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('user') ? script.database : 'postgres' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('retryOnError') && script.retryOnError) )">
                                                                    <strong class="label">Retry on Error </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('retryOnError') ? isEnabled(script.retryOnError) : 'Disabled' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('storeStatusInDatabase') && script.storeStatusInDatabase) )">
                                                                    <strong class="label">Store Status in Database </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('storeStatusInDatabase') ? isEnabled(script.storeStatusInDatabase) : 'Disabled' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('wrapInTransaction') && (script.wrapInTransaction != null) ) )">
                                                                    <strong class="label">Wrap in Transaction </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('wrapInTransaction') ? script.wrapInTransaction : 'Disabled' }}</span>
                                                                </li>
                                                                <li>
                                                                    <strong class="label">Script Source </strong>
                                                                    <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'"></span>
                                                                    <span class="value"> : {{ hasProp(script, 'script') ? 'Raw Script' : (hasProp(script, 'scriptFrom.secretKeyRef') ? 'Secret Key' : "Config Map") }}</span>
                                                                </li>
                                                                <li v-if="hasProp(script, 'script')">
                                                                    <strong class="label">Script</strong>
                                                                    <span class="value script"> : 
                                                                        <span>
                                                                            <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)">
                                                                                View Script
                                                                                <span class="eyeIcon"></span>
                                                                            </a>
                                                                        </span>
                                                                        <div :id="'script-' + baseIndex + '-' + index" class="hidden">
                                                                            <pre>{{ script.script }}</pre>
                                                                        </div>
                                                                    </span>
                                                                </li>
                                                                <li v-else-if="hasProp(script, 'scriptFrom.secretKeyRef')">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong>Secret Key Reference:</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Name </strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.secretKeyRef.name }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Key </strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.secretKeyRef.key }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                                <li v-else-if="hasProp(script, 'scriptFrom.configMapKeyRef')">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong>Config Map Key Reference:</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Name </strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.configMapKeyRef.name }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Key </strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.configMapKeyRef.key }}</span>
                                                                        </li>                                                                            
                                                                    </ul>
                                                                </li>
                                                                <li v-if="hasProp(script, 'scriptFrom.configMapScript')">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">
                                                                        Config Map Script:
                                                                    </strong>
                                                                    <span class="value script">
                                                                        <span>
                                                                            <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)">View Script</a>
                                                                        </span>
                                                                        <div :id="'script-' + baseIndex + '-' + index" class="hidden">
                                                                            <pre>{{ script.scriptFrom.configMapScript }}</pre>
                                                                        </div>
                                                                    </span>
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
                                <li v-if="( showDefaults || (cluster.data.spec.coordinator.managedSql.hasOwnProperty('continueOnSGScriptError') && cluster.data.spec.coordinator.managedSql.continueOnSGScriptError) )">
                                    <strong class="label">Continue on SGScript Error </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.managedSql.continueOnSGScriptError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                    <span class="value"> : {{ hasProp(cluster, 'data.spec.coordinator.managedSql.continueOnSGScriptError') ? isEnabled(cluster.data.spec.coordinator.managedSql.continueOnSGScriptError) : 'Disabled' }}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>
                    
                    <ul class="section" v-if="( showDefaults || (hasProp(cluster, 'data.spec.coordinator.replication.mode') && (cluster.data.spec.coordinator.replication.mode != 'async')) )">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Pods Replication </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.replication')"></span>
                            <ul>
                                <li v-if="(showDefaults || (cluster.data.spec.coordinator.replication.mode != 'async') )">
                                    <strong class="label">Mode</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.replication.mode')"></span>
                                    <span class="value"> : {{ hasProp(cluster, 'data.spec.coordinator.replication.mode') ? cluster.data.spec.coordinator.replication.mode : 'async' }}</span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.coordinator.replication.syncInstances')">
                                    <strong class="label">Sync Instances</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.replication.syncInstances')"></span>
                                    <span class="value"> : {{ cluster.data.spec.coordinator.replication.syncInstances }}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="showDefaults || 
                        !cluster.data.spec.postgresServices.coordinator.any.enabled || 
                        (cluster.data.spec.postgresServices.coordinator.any.type != 'ClusterIP') || 
                        !cluster.data.spec.postgresServices.coordinator.primary.enabled || 
                        (cluster.data.spec.postgresServices.coordinator.primary.type != 'ClusterIP') || 
                        hasProp(cluster, 'data.spec.postgresServices.coordinator.primary.loadBalancerIP') ||
                        hasProp(cluster, 'data.spec.postgresServices.coordinator.any.loadBalancerIP') ||
                        hasProp(cluster, 'data.spec.postgresServices.coordinator.customPorts')
                    ">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Customize generated Kubernetes service </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices')"></span>
                            <ul>
                                <li v-if="showDefaults || !cluster.data.spec.postgresServices.coordinator.primary.enabled || (cluster.data.spec.postgresServices.coordinator.primary.type != 'ClusterIP') || cluster.data.spec.postgresServices.coordinator.primary.hasOwnProperty('loadBalancerIP')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Primary Service</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.primary')"></span>
                                    <span class="value"> : {{ isEnabled(cluster.data.spec.postgresServices.coordinator.primary.enabled) }}</span>
                                    <ul v-if="( showDefaults || (cluster.data.spec.postgresServices.coordinator.primary.type != 'ClusterIP') || cluster.data.spec.postgresServices.coordinator.primary.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.coordinator.hasOwnProperty('customPorts'))">
                                        <li v-if="showDefaults">
                                            <strong class="label">Name</strong>
                                            <span class="helpTooltip" data-tooltip="Name of the -primary service."></span>
                                            <span class="value"> : {{ cluster.data.metadata.name }}-primary.{{cluster.data.metadata.namespace}}</span>	
                                        </li>
                                        <li v-if="( showDefaults || (cluster.data.spec.postgresServices.coordinator.primary.type != 'ClusterIP') )">
                                            <strong class="label">Type</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.primary.type')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.coordinator.primary.type }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.coordinator.primary.hasOwnProperty('loadBalancerIP')">
                                            <strong class="label">Load Balancer IP</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.primary.loadBalancerIP')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.coordinator.primary.loadBalancerIP }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || !cluster.data.spec.postgresServices.coordinator.any.enabled || (cluster.data.spec.postgresServices.coordinator.any.type != 'ClusterIP') || cluster.data.spec.postgresServices.coordinator.any.hasOwnProperty('loadBalancerIP')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Any Service</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.any')"></span>
                                    <span class="value"> : {{ isEnabled(cluster.data.spec.postgresServices.coordinator.any.enabled) }}</span>
                                    <ul v-if="( showDefaults || (cluster.data.spec.postgresServices.coordinator.any.type != 'ClusterIP') || cluster.data.spec.postgresServices.coordinator.any.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.coordinator.hasOwnProperty('customPorts'))">
                                        <li v-if="showDefaults">
                                            <strong class="label">Name</strong>
                                            <span class="helpTooltip" data-tooltip="Name of the -any service."></span>
                                            <span class="value"> : {{ cluster.data.metadata.name }}-any.{{cluster.data.metadata.namespace}}</span>	
                                        </li>
                                        <li v-if="( showDefaults || (cluster.data.spec.postgresServices.coordinator.any.type != 'ClusterIP') )">
                                            <strong class="label">Type</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.any.type')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.coordinator.any.type }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.coordinator.any.hasOwnProperty('loadBalancerIP')">
                                            <strong class="label">Load Balancer IP</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.any.loadBalancerIP')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.coordinator.any.loadBalancerIP }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="cluster.data.spec.postgresServices.coordinator.hasOwnProperty('customPorts')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Custom Ports</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts')"></span>
                                    <ul>
                                        <li v-for="(port, index) in cluster.data.spec.postgresServices.coordinator.customPorts">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Port #{{ index + 1 }}</strong>
                                            <ul>
                                                <li v-if="port.hasOwnProperty('appProtocol') && (port.appProtocol != null)">
                                                    <strong class="label">Application Protocol</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.appProtocol')"></span>
                                                    <span class="value"> : {{ port.appProtocol }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('name') && (port.name != null)">
                                                    <strong class="label">Name:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.name')"></span>
                                                    <span class="value"> : {{ port.name }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('nodePort') && (port.nodePort != null)">
                                                    <strong class="label">Node Port:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.nodePort')"></span>
                                                    <span class="value"> : {{ port.nodePort }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('port')">
                                                    <strong class="label">Port:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.port')"></span>
                                                    <span class="value"> : {{ port.port }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('protocol') && (port.protocol != null)">
                                                    <strong class="label">Protocol:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.protocol')"></span>
                                                    <span class="value"> : {{ port.protocol }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('targetPort') && (port.targetPort != null)">
                                                    <strong class="label">Target Port:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.targetPort')"></span>
                                                    <span class="value"> : {{ port.targetPort }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>

                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.coordinator.metadata.labels.clusterPods') || hasProp(cluster, 'data.spec.coordinator.metadata.annotations')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Metadata </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata')"></span>
                            <ul>
                                <li v-if="hasProp(cluster, 'data.spec.coordinator.metadata')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Labels</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.labels')"></span>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.coordinator.metadata.labels.clusterPods')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Cluster Pods</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.labels.clusterPods')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.coordinator.metadata.labels.clusterPods">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.coordinator.metadata.annotations')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Annotations</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations')"></span>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.coordinator.metadata.annotations.allResources')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">All Resources</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.allResources')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.coordinator.metadata.annotations.allResources">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.coordinator.metadata.annotations.clusterPods')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Cluster Pods</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.clusterPods')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.coordinator.metadata.annotations.clusterPods">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.coordinator.metadata.annotations.services')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Services</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.services')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.coordinator.metadata.annotations.services">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.coordinator.metadata.annotations.primaryService')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Primary Service</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.primaryService')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.coordinator.metadata.annotations.primaryService">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.coordinator.metadata.annotations.replicasService')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Replicas Service</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.replicasService')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.coordinator.metadata.annotations.replicasService">
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

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.coordinator.pods.scheduling')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Pods Scheduling </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling')"></span>
                            <ul>
                                <li v-if="hasProp(cluster, 'data.spec.coordinator.pods.scheduling.nodeSelector')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Selectors</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeSelector')"></span>
                                    <ul>
                                        <li v-for="(value, key) in cluster.data.spec.coordinator.pods.scheduling.nodeSelector">
                                            <strong class="label">{{ key }}</strong>
                                            <span class="value"> : {{ value }}</span>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="hasProp(cluster, 'data.spec.coordinator.pods.scheduling.tolerations')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Tolerations</strong>                                    
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations')"></span>
                                    <ul>
                                        <li v-for="(toleration, index) in cluster.data.spec.coordinator.pods.scheduling.tolerations">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Toleration #{{ index+1 }}</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">Key</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.key')"></span>
                                                    <span class="value"> : {{ toleration.key }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Operator</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.operator')"></span>
                                                    <span class="value"> : {{ toleration.operator }}</span>
                                                </li>
                                                <li v-if="toleration.hasOwnProperty('value')">
                                                    <strong class="label">Value</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.value')"></span>
                                                    <span class="value"> : {{ toleration.value }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Effect</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.effect')"></span>
                                                    <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                                </li>
                                                <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                                    <strong class="label">Toleration Seconds</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                                    <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="hasProp(cluster, 'data.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Affinity</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                                    <span>Required During Scheduling Ignored During Execution</span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Terms</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items')"></span>
                                            <ul>
                                                <li v-for="(term, index) in cluster.data.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Term #{{ index+1 }}</strong>
                                                    <ul>
                                                        <li v-if="term.hasOwnProperty('matchExpressions')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Expressions</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span>
                                                            <ul>
                                                                <li v-for="(exp, index) in term.matchExpressions">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Expression #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span>
                                                                            <span class="value"> : {{ exp.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ exp.operator }}</span>
                                                                        </li>
                                                                        <li v-if="exp.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                        <li v-if="term.hasOwnProperty('matchFields')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Fields</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span>
                                                            <ul>
                                                                <li v-for="(field, index) in term.matchFields">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Field #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span>
                                                                            <span class="value"> : {{ field.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ field.operator }}</span>
                                                                        </li>
                                                                        <li v-if="field.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
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
                                </li>

                                <li v-if="hasProp(cluster, 'data.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Affinity</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                                    <span>Preferred During Scheduling Ignored During Execution</span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Terms</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span>
                                            <ul>
                                                <li v-for="(term, index) in cluster.data.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Term #{{ index+1 }}</strong>
                                                    <ul>
                                                        <li v-if="term.preference.hasOwnProperty('matchExpressions')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Expressions</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                            <ul>
                                                                <li v-for="(exp, index) in term.preference.matchExpressions">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Expression #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                            <span class="value"> : {{ exp.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ exp.operator }}</span>
                                                                        </li>
                                                                        <li v-if="exp.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                        <li v-if="term.preference.hasOwnProperty('matchFields')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Fields</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                            <ul>
                                                                <li v-for="(field, index) in term.preference.matchFields">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Field #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                            <span class="value"> : {{ field.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ field.operator }}</span>
                                                                        </li>
                                                                        <li v-if="field.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                        <li v-if="term.hasOwnProperty('weight')">
                                                            <strong class="label">Weight</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                            <span class="value"> : {{ term.weight }}</span>
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

                    <hr/><br/>

                    <strong class="sectionTitle">Shards </strong>
                    
                    <ul class="section"  v-if="
                        showDefaults ||
                        (cluster.data.spec.shards.clusters !== 1) ||
                        (cluster.data.spec.shards.instancesPerCluster !== 1) ||
                        hasProp(cluster, 'data.spec.shards.sgInstanceProfile') ||
                        hasProp(cluster, 'data.spec.shards.configurations.sgPostgresConfig') || 
                        (cluster.data.spec.shards.pods.persistentVolume.size != '1Gi') || 
                        hasProp(cluster, 'data.spec.shards.pods.persistentVolume.storageClass')
                    ">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Specs </strong>
                            <ul>
                                <li v-if="(showDefaults || (cluster.data.spec.shards.clusters !== 1))">
                                    <strong class="label">Clusters</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.clusters')"></span>
                                    <span> : {{ cluster.data.spec.shards.clusters }}</span>
                                </li>
                                <li v-if="(showDefaults || (cluster.data.spec.shards.instancesPerCluster !== 1))">
                                    <strong class="label">Instances per Cluster</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.instancesPerCluster')"></span>
                                    <span> : {{ cluster.data.spec.shards.instancesPerCluster }}</span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.shards.sgInstanceProfile') || showDefaults">
                                    <strong class="label">Instance Profile </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.sgInstanceProfile')"></span>
                                    <span class="value">
                                        : 
                                        <template v-if="hasProp(cluster, 'data.spec.shards.sgInstanceProfile')">
                                            <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + profile.name" target="_blank" v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.shards.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )"> 
                                                {{ profile.data.metadata.name }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }})
                                                <span class="eyeIcon"></span>
                                            </router-link>
                                        </template>
                                        <template v-else>
                                            Default (Cores: 1, RAM: 2GiB)
                                        </template>
                                    </span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.shards.configurations.sgPostgresConfig') || showDefaults">
                                    <strong class="label">Postgres Configuration </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.configurations.sgPostgresConfig')"></span>
                                    <span class="value"> : 
                                        <template v-if="hasProp(cluster, 'data.spec.shards.configurations.sgPostgresConfig')">
                                            <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + cluster.data.spec.shards.configurations.sgPostgresConfig" target="_blank"> 
                                                {{ cluster.data.spec.shards.configurations.sgPostgresConfig }}
                                                <span class="eyeIcon"></span>
                                            </router-link>
                                        </template>
                                        <template v-else>
                                            Default
                                        </template>
                                    </span>
                                </li>
                                <li v-if="showDefaults || (cluster.data.spec.shards.pods.persistentVolume.size != '1Gi') || hasProp(cluster, 'data.spec.shards.pods.persistentVolume.storageClass')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Pods Storage</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods')"></span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Persistent Volume</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.persistentVolume')"></span>
                                            <ul>
                                                <li v-if="showDefaults || (cluster.data.spec.shards.pods.persistentVolume.size != '1Gi')">
                                                    <strong class="label">Volume Size</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.persistentVolume.size')"></span>
                                                    <span class="value"> : {{ cluster.data.spec.shards.pods.persistentVolume.size }}B</span>
                                                </li>
                                                <li v-if="hasProp(cluster, 'data.spec.shards.pods.persistentVolume.storageClass')">
                                                    <strong class="label">Storage Class</strong>
                                                     <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.persistentVolume.storageClass')"></span>
                                                    <span class="value"> : {{ cluster.data.spec.shards.pods.persistentVolume.storageClass }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="
                        showDefaults || 
                        hasProp(cluster, 'data.spec.shards.pods.disableConnectionPooling') || 
                        hasProp(cluster, 'data.spec.shards.configurations.sgPoolingConfig') || 
                        hasProp(cluster, 'data.spec.shards.pods.disablePostgresUtil') || 
                        hasProp(cluster, 'data.spec.shards.pods.disableMetricsExporter') ||
                        (cluster.data.spec.shards.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.shards.pods.customVolumes)) || 
                        (cluster.data.spec.shards.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.shards.pods.customInitContainers)) || 
                        (cluster.data.spec.shards.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.shards.pods.customContainers))
                    ">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Sidecars </strong>
                            <ul>
                                <li v-if="showDefaults || hasProp(cluster, 'data.spec.shards.pods.disableConnectionPooling') || hasProp(cluster, 'data.spec.shards.configurations.sgPoolingConfig')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Connection Pooling</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.configurations')"></span>
                                    <span v-if="showDefaults || cluster.data.spec.shards.pods.disableConnectionPooling"> : {{ isEnabled(cluster.data.spec.shards.pods.disableConnectionPooling, true) }}</span>
                                    <ul v-if="(showDefaults && !cluster.data.spec.shards.pods.disableConnectionPooling) || hasProp(cluster, 'data.spec.shards.configurations.sgPoolingConfig')">
                                        <li>
                                            <strong class="label">Connection Pooling Configuration</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.configurations.sgPoolingConfig')"></span>
                                            <span class="value"> :
                                                <template v-if="hasProp(cluster, 'data.spec.shards.configurations.sgPoolingConfig')">
                                                    <router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + cluster.data.spec.shards.configurations.sgPoolingConfig" target="_blank">
                                                        {{ cluster.data.spec.shards.configurations.sgPoolingConfig }}
                                                        <span class="eyeIcon"></span>
                                                    </router-link>
                                                </template>
                                                <template v-else>
                                                    Default
                                                </template>
                                            </span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || hasProp(cluster, 'data.spec.shards.pods.disablePostgresUtil')">
                                    <strong class="label">Postgres Utils</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.disablePostgresUtil').replace('If set to `true`', 'If disabled')"></span>
                                    <span> : {{ isEnabled(cluster.data.spec.shards.pods.disablePostgresUtil, true) }}</span>
                                </li>
                                <li v-if="showDefaults || hasProp(cluster, 'data.spec.shards.pods.disableMetricsExporter')">
                                    <strong class="label">Metrics Exporter</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                                    <span> : {{ isEnabled(cluster.data.spec.shards.pods.disableMetricsExporter, true) }}</span>
                                </li>
                                <li v-if="( 
                                    (cluster.data.spec.shards.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.shards.pods.customVolumes)) || 
                                    (cluster.data.spec.shards.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.shards.pods.customInitContainers)) || 
                                    (cluster.data.spec.shards.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.shards.pods.customContainers)) )
                                ">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">User-Supplied Pods' Sidecars </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods')"></span>
                                    <ul>
                                        <li v-if="cluster.data.spec.shards.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.shards.pods.customVolumes)">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Volumes</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes')"></span>
                                            <ul>
                                                <template v-for="(vol, index) in cluster.data.spec.shards.pods.customVolumes">
                                                    <li :key="index">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Volume #{{ index + 1 }}</strong>
                                                        <ul>
                                                            <li v-if="vol.hasOwnProperty('name')">
                                                                <strong class="label">Name</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.name')"></span>
                                                                <span class="value"> : {{ vol.name }}</span>
                                                            </li>
                                                            <li v-if="vol.hasOwnProperty('emptyDir') && Object.keys(vol.emptyDir).length && (!isNull(vol.emptyDir.medium) || !isNull(vol.emptyDir.sizeLimit))">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Empty Directory</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.emptyDir')"></span>
                                                                <ul>
                                                                    <li v-if="vol.emptyDir.hasOwnProperty('medium') && !isNull(vol.emptyDir.medium)">
                                                                        <strong class="label">Medium</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.emptyDir.properties.medium')"></span>
                                                                        <span class="value"> : {{ vol.emptyDir.medium }}</span>
                                                                    </li>
                                                                    <li v-if="vol.emptyDir.hasOwnProperty('sizeLimit') && !isNull(vol.emptyDir.sizeLimit)">
                                                                        <strong class="label">Size Limit</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.emptyDir.properties.sizeLimit')"></span>
                                                                        <span class="value"> : {{ vol.emptyDir.sizeLimit }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                            <li v-if="vol.hasOwnProperty('configMap') && Object.keys(vol.configMap).length">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Config Map</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap')"></span>
                                                                <ul>
                                                                    <li v-if="vol.configMap.hasOwnProperty('name') && !isNull(vol.configMap.name)">
                                                                        <strong class="label">Name</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.name')"></span>
                                                                        <span class="value"> : {{ vol.configMap.name }}</span>
                                                                    </li>
                                                                    <li v-if="vol.configMap.hasOwnProperty('optional')">
                                                                        <strong class="label">Optional</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.name')"></span>
                                                                        <span class="value"> : {{ isEnabled(vol.configMap.optional) }}</span>
                                                                    </li>
                                                                    <li v-if="vol.configMap.hasOwnProperty('defaultMode') && !isNull(vol.configMap.defaultMode)">
                                                                        <strong class="label">Default Mode</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.defaultMode')"></span>
                                                                        <span class="value"> : {{ vol.configMap.defaultMode }}</span>
                                                                    </li>
                                                                    <li v-if="vol.configMap.hasOwnProperty('items') && vol.configMap.items.length">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Items</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.items')"></span>
                                                                        <ul>
                                                                            <template v-for="(item, index) in vol.configMap.items">
                                                                                <li :key="index">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Item #{{ index + 1 }}</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.items.items')"></span>
                                                                                    <ul>
                                                                                        <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                            <strong class="label">Key</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.items.items.properties.key')"></span>
                                                                                            <span class="value"> : {{ item.key }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                            <strong class="label">Mode</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.items.items.properties.mode')"></span>
                                                                                            <span class="value"> : {{ item.mode }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                            <strong class="label">Path</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.items.items.properties.path')"></span>
                                                                                            <span class="value"> : {{ item.path }}</span>
                                                                                        </li>
                                                                                    </ul>
                                                                                </li>
                                                                            </template>
                                                                        </ul>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                            <li v-if="vol.hasOwnProperty('secret') && Object.keys(vol.secret).length">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Secret</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret')"></span>
                                                                <ul>
                                                                    <li v-if="vol.secret.hasOwnProperty('secretName') && !isNull(vol.secret.secretName)">
                                                                        <strong class="label">Secret Name</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.secretName')"></span>
                                                                        <span class="value"> : {{ vol.secret.secretName }}</span>
                                                                    </li>
                                                                    <li v-if="vol.secret.hasOwnProperty('optional')">
                                                                        <strong class="label">Optional</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.optional')"></span>
                                                                        <span class="value"> : {{ isEnabled(vol.secret.optional) }}</span>
                                                                    </li>
                                                                    <li v-if="vol.secret.hasOwnProperty('defaultMode') && !isNull(vol.secret.defaultMode)">
                                                                        <strong class="label">Default Mode</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.defaultMode')"></span>
                                                                        <span class="value"> : {{ vol.secret.defaultMode }}</span>
                                                                    </li>
                                                                    <li v-if="vol.secret.hasOwnProperty('items') && vol.secret.items.length">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Items</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.items')"></span>
                                                                        <ul>
                                                                            <template v-for="(item, index) in vol.secret.items">
                                                                                <li :key="index">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Item #{{ index + 1 }}</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.items.items')"></span>
                                                                                    <ul>
                                                                                        <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                            <strong class="label">Key:</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.items.items.properties.key')"></span>
                                                                                            <span class="value"> : {{ item.key }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                            <strong class="label">Mode</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.items.items.properties.mode')"></span>
                                                                                            <span class="value"> : {{ item.mode }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                            <strong class="label">Path</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.items.items.properties.path')"></span>
                                                                                            <span class="value"> : {{ item.path }}</span>
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

                                        <li v-if="cluster.data.spec.shards.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.shards.pods.customInitContainers)">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Init Containers</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers')"></span>

                                            <ul>
                                                <template v-for="(container, index) in cluster.data.spec.shards.pods.customInitContainers">
                                                    <li :key="'container-' + index">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Init Container #{{ index + 1 }}</strong>
                                                        <ul>
                                                            <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                                <strong class="label">Name</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.name')"></span>
                                                                <span class="value"> : {{ container.name }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                                <strong class="label">Image</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.image')"></span>
                                                                <span class="value"> : {{ container.image }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                                <strong class="label">Image Pull Policy</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.imagePullPolicy')"></span>
                                                                <span class="value"> : {{ container.imagePullPolicy }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                                <strong class="label">Working Directory</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.workingDir')"></span>
                                                                <span class="value"> : {{ container.workingDir }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Arguments</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.arguments')"></span>
                                                                <ul>
                                                                    <template v-for="(arg, aIndex) in container.args">
                                                                        <li :key="'argument-' + index + '-' + aIndex">
                                                                            <span class="value">{{ arg }}</span>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('command') && !isNull(container.command)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Command</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.command')"></span>
                                                                <ul>
                                                                    <template v-for="(command, cIndex) in container.command">
                                                                        <li :key="'command-' + index + '-' + cIndex">
                                                                            <span class="value">{{ command }}</span>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('env') && !isNull(container.env)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Environment Variables</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.env')"></span>
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
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Ports</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports')"></span>
                                                                <ul>
                                                                    <template v-for="(port, pIndex) in container.ports">
                                                                        <li :key="'port-' + index + '-' + pIndex">
                                                                            <button class="toggleSummary"></button>
                                                                            <strong class="label">Port #{{ pIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ port.name }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                                    <strong class="label">Host IP</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.hostIP')"></span>
                                                                                    <span class="value"> : {{ port.hostIP }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                                    <strong class="label">Host Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.hostPort')"></span>
                                                                                    <span class="value"> : {{ port.hostPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                                    <strong class="label">Container Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.containerPort')"></span>
                                                                                    <span class="value"> : {{ port.containerPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                                    <strong class="label">Protocol</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.protocol')"></span>
                                                                                    <span class="value"> : {{ port.protocol }}</span>
                                                                                </li>
                                                                            </ul>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('volumeMounts') && !isNull(container.volumeMounts)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Volume Mounts</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts')"></span>
                                                                <ul>
                                                                    <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                        <li :key="'vol-' + index + '-' + vIndex">
                                                                            <strong class="label">Volume #{{ vIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ vol.name }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('readOnly')">
                                                                                    <strong class="label">Read Only</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                                    <span class="value"> : {{ isEnabled(vol.readOnly) }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                                    <strong class="label">Mount Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                                    <span class="value"> : {{ vol.mountPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                                    <strong class="label">Mount Propagation</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                                    <span class="value"> : {{ vol.mountPropagation }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                                    <strong class="label">Sub Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.subPath')"></span>
                                                                                    <span class="value"> : {{ vol.subPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                                    <strong class="label">Sub Path Expr</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                                    <span class="value"> : {{ vol.subPathExpr }}</span>
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

                                        <li v-if="cluster.data.spec.shards.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.shards.pods.customContainers)">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Containers</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers')"></span>
                                            <ul>
                                                <template v-for="(container, index) in cluster.data.spec.shards.pods.customContainers">
                                                    <li :key="'container-' + index">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Container #{{ index + 1 }}</strong>
                                                        <ul>
                                                            <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                                <strong class="label">Name</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.name')"></span>
                                                                <span class="value"> : {{ container.name }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                                <strong class="label">Image</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.image')"></span>
                                                                <span class="value"> : {{ container.image }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                                <strong class="label">Image Pull Policy</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.imagePullPolicy')"></span>
                                                                <span class="value"> : {{ container.imagePullPolicy }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                                <strong class="label">Working Directory</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.workingDir')"></span>
                                                                <span class="value"> : {{ container.workingDir }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Arguments</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.arguments')"></span>
                                                                <ul>
                                                                    <template v-for="(arg, aIndex) in container.args">
                                                                        <li :key="'argument-' + index + '-' + aIndex">
                                                                            <span class="value">{{ arg }}</span>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('command') && !isNull(container.command)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Command</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.command')"></span>
                                                                <ul>
                                                                    <template v-for="(command, cIndex) in container.command">
                                                                        <li :key="'command-' + index + '-' + cIndex">
                                                                            <span class="value">{{ command }}</span>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('env') && !isNull(container.env)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Environment Variables</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.env')"></span>
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
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Ports</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports')"></span>
                                                                <ul>
                                                                    <template v-for="(port, pIndex) in container.ports">
                                                                        <li :key="'port-' + index + '-' + pIndex">
                                                                            <strong class="label">Port #{{ pIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ port.name }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                                    <strong class="label">Host IP</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.hostIP')"></span>
                                                                                    <span class="value"> : {{ port.hostIP }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                                    <strong class="label">Host Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.hostPort')"></span>
                                                                                    <span class="value"> : {{ port.hostPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                                    <strong class="label">Container Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.containerPort')"></span>
                                                                                    <span class="value"> : {{ port.containerPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                                    <strong class="label">Protocol</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.protocol')"></span>
                                                                                    <span class="value"> : {{ port.protocol }}</span>
                                                                                </li>
                                                                            </ul>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('volumeMounts') && !isNull(container.volumeMounts)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Volume Mounts</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts')"></span>
                                                                <ul>
                                                                    <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                        <li :key="'vol-' + index + '-' + vIndex">
                                                                            <strong class="label">Volume #{{ vIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ vol.name }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('readOnly')">
                                                                                    <strong class="label">Read Only</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                                    <span class="value"> : {{ isEnabled(vol.readOnly) }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                                    <strong class="label">Mount Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                                    <span class="value"> : {{ vol.mountPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                                    <strong class="label">Mount Propagation</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                                    <span class="value"> : {{ vol.mountPropagation }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                                    <strong class="label">Sub Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.subPath')"></span>
                                                                                    <span class="value"> : {{ vol.subPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                                    <strong class="label">Sub Path Expr</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                                    <span class="value"> : {{ vol.subPathExpr }}</span>
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
                        </li>
                    </ul>
                    
                    <ul class="section" v-if="hasProp(cluster, 'data.spec.shards.managedSql')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Managed SQL </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.managedSql')"></span>
                            <ul>
                                <li>
                                    <button class="toggleSummary"></button>
                                    <strong class="sectionTitle">Scripts </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.managedSql.scripts')"></span>
                                    <ul>
                                        <li v-for="(baseScript, baseIndex) in cluster.data.spec.shards.managedSql.scripts">
                                            <button class="toggleSummary"></button>
                                            <strong class="sectionTitle">SGScript #{{ baseIndex + 1 }}</strong>
                                            <ul>
                                                <li v-if="( showDefaults || ( hasProp(baseScript, 'scriptSpec.continueOnError') && baseScript.scriptSpec.continueOnError ) )">
                                                    <strong class="label">Continue on Error</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.managedSql.scripts.continueOnError')"></span>
                                                    <span class="value"> : {{ hasProp(baseScript, 'scriptSpec.continueOnError') ? isEnabled(baseScript.continueOnError) : 'Disabled' }}</span>
                                                </li>
                                                <li v-if="( showDefaults || ( hasProp(baseScript, 'scriptSpec.managedVersions') && !baseScript.scriptSpec.managedVersions) )">
                                                    <strong class="label">Managed Versions:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.managedSql.scripts')"></span>
                                                    <span class="value">{{ hasProp(baseScript, 'scriptSpec.managedVersions') && isEnabled(baseScript.scriptSpec.managedVersions) }}</span>
                                                </li>
                                                <li v-if="baseScript.hasOwnProperty('scriptSpec')">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="sectionTitle">Script Entries</strong>

                                                    <ul>
                                                        <li v-for="(script, index) in baseScript.scriptSpec.scripts">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="sectionTitle">Script #{{ index + 1 }}</strong>

                                                            <ul>
                                                                <li v-if="hasProp(script, 'name')">
                                                                    <strong class="label">Name</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                                    <span class="value"> : {{ script.name }}</span>
                                                                </li>
                                                                <li v-if="hasProp(script, 'version')">
                                                                    <strong class="label">Version </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                                                    <span class="value"> : {{ script.version }}</span>
                                                                </li>
                                                                <li v-if="showDefaults || hasProp(script, 'database')">
                                                                    <strong class="label">Database</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('database') ? script.database : 'postgres' }}</span>
                                                                </li>
                                                                <li v-if="showDefaults || hasProp(script, 'user')">
                                                                    <strong class="label">User </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('user') ? script.database : 'postgres' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('retryOnError') && script.retryOnError) )">
                                                                    <strong class="label">Retry on Error </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('retryOnError') ? isEnabled(script.retryOnError) : 'Disabled' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('storeStatusInDatabase') && script.storeStatusInDatabase) )">
                                                                    <strong class="label">Store Status in Database </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('storeStatusInDatabase') ? isEnabled(script.storeStatusInDatabase) : 'Disabled' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('wrapInTransaction') && (script.wrapInTransaction != null) ) )">
                                                                    <strong class="label">Wrap in Transaction </strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('wrapInTransaction') ? script.wrapInTransaction : 'Disabled' }}</span>
                                                                </li>
                                                                <li>
                                                                    <strong class="label">Script Source </strong>
                                                                    <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'"></span>
                                                                    <span class="value"> : {{ hasProp(script, 'script') ? 'Raw Script' : (hasProp(script, 'scriptFrom.secretKeyRef') ? 'Secret Key' : "Config Map") }}</span>
                                                                </li>
                                                                <li v-if="hasProp(script, 'script')">
                                                                    <strong class="label">Script</strong>
                                                                    <span class="value script"> : 
                                                                        <span>
                                                                            <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)">
                                                                                View Script
                                                                                <span class="eyeIcon"></span>
                                                                            </a>
                                                                        </span>
                                                                        <div :id="'script-' + baseIndex + '-' + index" class="hidden">
                                                                            <pre>{{ script.script }}</pre>
                                                                        </div>
                                                                    </span>
                                                                </li>
                                                                <li v-else-if="hasProp(script, 'scriptFrom.secretKeyRef')">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong>Secret Key Reference:</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Name </strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.secretKeyRef.name }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Key </strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.secretKeyRef.key }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                                <li v-else-if="hasProp(script, 'scriptFrom.configMapKeyRef')">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong>Config Map Key Reference:</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Name </strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.configMapKeyRef.name }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Key </strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.configMapKeyRef.key }}</span>
                                                                        </li>                                                                            
                                                                    </ul>
                                                                </li>
                                                                <li v-if="hasProp(script, 'scriptFrom.configMapScript')">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">
                                                                        Config Map Script:
                                                                    </strong>
                                                                    <span class="value script">
                                                                        <span>
                                                                            <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)">View Script</a>
                                                                        </span>
                                                                        <div :id="'script-' + baseIndex + '-' + index" class="hidden">
                                                                            <pre>{{ script.scriptFrom.configMapScript }}</pre>
                                                                        </div>
                                                                    </span>
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
                                <li v-if="( showDefaults || (cluster.data.spec.shards.managedSql.hasOwnProperty('continueOnSGScriptError') && cluster.data.spec.shards.managedSql.continueOnSGScriptError) )">
                                    <strong class="label">Continue on SGScript Error </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.managedSql.continueOnSGScriptError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                    <span class="value"> : {{ hasProp(cluster, 'data.spec.shards.managedSql.continueOnSGScriptError') ? isEnabled(cluster.data.spec.shards.managedSql.continueOnSGScriptError) : 'Disabled' }}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>
                    
                    <ul class="section" v-if="( showDefaults || (hasProp(cluster, 'data.spec.shards.replication.mode') && (cluster.data.spec.shards.replication.mode != 'async')) )">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Pods Replication </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.replication')"></span>
                            <ul>
                                <li v-if="(showDefaults || (cluster.data.spec.shards.replication.mode != 'async') )">
                                    <strong class="label">Mode</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.replication.mode')"></span>
                                    <span class="value"> : {{ hasProp(cluster, 'data.spec.shards.replication.mode') ? cluster.data.spec.shards.replication.mode : 'async' }}</span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.shards.replication.syncInstances')">
                                    <strong class="label">Sync Instances</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.replication.syncInstances')"></span>
                                    <span class="value"> : {{ cluster.data.spec.shards.replication.syncInstances }}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="showDefaults || 
                        !cluster.data.spec.postgresServices.shards.primaries.enabled || 
                        (cluster.data.spec.postgresServices.shards.primaries.type != 'ClusterIP') || 
                        hasProp(cluster, 'data.spec.postgresServices.shards.primaries.loadBalancerIP') ||
                        hasProp(cluster, 'data.spec.postgresServices.shards.customPorts')
                    ">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Customize generated Kubernetes service </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices')"></span>
                            <ul>
                                <li v-if="showDefaults || !cluster.data.spec.postgresServices.shards.primaries.enabled || (cluster.data.spec.postgresServices.shards.primaries.type != 'ClusterIP') || cluster.data.spec.postgresServices.shards.primaries.hasOwnProperty('loadBalancerIP')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Primary Service</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.primaries')"></span>
                                    <span class="value"> : {{ isEnabled(cluster.data.spec.postgresServices.shards.primaries.enabled) }}</span>
                                    <ul v-if="( showDefaults || (cluster.data.spec.postgresServices.shards.primaries.type != 'ClusterIP') || cluster.data.spec.postgresServices.shards.primaries.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.shards.hasOwnProperty('customPorts'))">
                                        <li v-if="showDefaults">
                                            <strong class="label">Name</strong>
                                            <span class="helpTooltip" data-tooltip="Name of the -primaries service."></span>
                                            <span class="value"> : {{ cluster.data.metadata.name }}-primaries.{{cluster.data.metadata.namespace}}</span>	
                                        </li>
                                        <li v-if="( showDefaults || (cluster.data.spec.postgresServices.shards.primaries.type != 'ClusterIP') )">
                                            <strong class="label">Type</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.primaries.type')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.shards.primaries.type }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.shards.primaries.hasOwnProperty('loadBalancerIP')">
                                            <strong class="label">Load Balancer IP</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.primaries.loadBalancerIP')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.shards.primaries.loadBalancerIP }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="cluster.data.spec.postgresServices.shards.hasOwnProperty('customPorts')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Custom Ports</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts')"></span>
                                    <ul>
                                        <li v-for="(port, index) in cluster.data.spec.postgresServices.shards.customPorts">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Port #{{ index + 1 }}</strong>
                                            <ul>
                                                <li v-if="port.hasOwnProperty('appProtocol') && (port.appProtocol != null)">
                                                    <strong class="label">Application Protocol</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.appProtocol')"></span>
                                                    <span class="value"> : {{ port.appProtocol }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('name') && (port.name != null)">
                                                    <strong class="label">Name:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.name')"></span>
                                                    <span class="value"> : {{ port.name }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('nodePort') && (port.nodePort != null)">
                                                    <strong class="label">Node Port:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.nodePort')"></span>
                                                    <span class="value"> : {{ port.nodePort }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('port')">
                                                    <strong class="label">Port:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.port')"></span>
                                                    <span class="value"> : {{ port.port }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('protocol') && (port.protocol != null)">
                                                    <strong class="label">Protocol:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.protocol')"></span>
                                                    <span class="value"> : {{ port.protocol }}</span>
                                                </li>
                                                <li v-if="port.hasOwnProperty('targetPort') && (port.targetPort != null)">
                                                    <strong class="label">Target Port:</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.targetPort')"></span>
                                                    <span class="value"> : {{ port.targetPort }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>

                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.shards.metadata.labels.clusterPods') || hasProp(cluster, 'data.spec.shards.metadata.annotations')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Metadata </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata')"></span>
                            <ul>
                                <li v-if="hasProp(cluster, 'data.spec.shards.metadata')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Labels</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.labels')"></span>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.shards.metadata.labels.clusterPods')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Cluster Pods</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.labels.clusterPods')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.shards.metadata.labels.clusterPods">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.shards.metadata.annotations')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Annotations</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations')"></span>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.shards.metadata.annotations.allResources')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">All Resources</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.allResources')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.shards.metadata.annotations.allResources">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.shards.metadata.annotations.clusterPods')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Cluster Pods</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.clusterPods')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.shards.metadata.annotations.clusterPods">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.shards.metadata.annotations.services')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Services</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.services')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.shards.metadata.annotations.services">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.shards.metadata.annotations.primaryService')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Primary Service</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.primaryService')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.shards.metadata.annotations.primaryService">
                                                    <strong class="label">{{ label }}:</strong>
                                                    <span class="value">{{ value }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.shards.metadata.annotations.replicasService')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Replicas Service</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.replicasService')"></span>
                                            <ul>
                                                <li v-for="(value, label) in cluster.data.spec.shards.metadata.annotations.replicasService">
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

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.shards.pods.scheduling')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Pods Scheduling </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling')"></span>
                            <ul>
                                <li v-if="hasProp(cluster, 'data.spec.shards.pods.scheduling.nodeSelector')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Selectors</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeSelector')"></span>
                                    <ul>
                                        <li v-for="(value, key) in cluster.data.spec.shards.pods.scheduling.nodeSelector">
                                            <strong class="label">{{ key }}</strong>
                                            <span class="value"> : {{ value }}</span>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="hasProp(cluster, 'data.spec.shards.pods.scheduling.tolerations')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Tolerations</strong>                                    
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations')"></span>
                                    <ul>
                                        <li v-for="(toleration, index) in cluster.data.spec.shards.pods.scheduling.tolerations">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Toleration #{{ index+1 }}</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">Key</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.key')"></span>
                                                    <span class="value"> : {{ toleration.key }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Operator</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.operator')"></span>
                                                    <span class="value"> : {{ toleration.operator }}</span>
                                                </li>
                                                <li v-if="toleration.hasOwnProperty('value')">
                                                    <strong class="label">Value</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.value')"></span>
                                                    <span class="value"> : {{ toleration.value }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Effect</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.effect')"></span>
                                                    <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                                </li>
                                                <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                                    <strong class="label">Toleration Seconds</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                                    <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="hasProp(cluster, 'data.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Affinity</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                                    <span>Required During Scheduling Ignored During Execution</span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Terms</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items')"></span>
                                            <ul>
                                                <li v-for="(term, index) in cluster.data.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Term #{{ index+1 }}</strong>
                                                    <ul>
                                                        <li v-if="term.hasOwnProperty('matchExpressions')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Expressions</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span>
                                                            <ul>
                                                                <li v-for="(exp, index) in term.matchExpressions">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Expression #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span>
                                                                            <span class="value"> : {{ exp.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ exp.operator }}</span>
                                                                        </li>
                                                                        <li v-if="exp.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                        <li v-if="term.hasOwnProperty('matchFields')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Fields</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span>
                                                            <ul>
                                                                <li v-for="(field, index) in term.matchFields">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Field #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span>
                                                                            <span class="value"> : {{ field.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ field.operator }}</span>
                                                                        </li>
                                                                        <li v-if="field.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
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
                                </li>

                                <li v-if="hasProp(cluster, 'data.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Affinity</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                                    <span>Preferred During Scheduling Ignored During Execution</span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Terms</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span>
                                            <ul>
                                                <li v-for="(term, index) in cluster.data.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Term #{{ index+1 }}</strong>
                                                    <ul>
                                                        <li v-if="term.preference.hasOwnProperty('matchExpressions')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Expressions</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                            <ul>
                                                                <li v-for="(exp, index) in term.preference.matchExpressions">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Expression #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                            <span class="value"> : {{ exp.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ exp.operator }}</span>
                                                                        </li>
                                                                        <li v-if="exp.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                        <li v-if="term.preference.hasOwnProperty('matchFields')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Fields</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                            <ul>
                                                                <li v-for="(field, index) in term.preference.matchFields">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Field #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                            <span class="value"> : {{ field.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ field.operator }}</span>
                                                                        </li>
                                                                        <li v-if="field.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                        <li v-if="term.hasOwnProperty('weight')">
                                                            <strong class="label">Weight</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                            <span class="value"> : {{ term.weight }}</span>
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
                        v-if="hasProp(cluster, 'data.spec.shards.overrides') && !isNull(cluster.data.spec.shards.overrides)"
                    >
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">
                                Overrides
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides')"></span>
                            </strong>

                            <ul>
                                <template v-for="(override, overrideIndex) in cluster.data.spec.shards.overrides">
                                    <li :key="'override-' + override.index">
                                        <button class="toggleSummary"></button>
                                        <strong class="sectionTitle">
                                            Override #{{ overrideIndex }}
                                        </strong>
                                        
                                        <ul>
                                            <li>
                                                <button class="toggleSummary"></button>
                                                <strong class="sectionTitle">Specs </strong>
                                                <ul>
                                                    <li>
                                                        <strong class="label">Cluster Identifier</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.index')"></span>
                                                        <span> : {{ override.index }}</span>
                                                    </li>
                                                    <li v-if="( override.hasOwnProperty('instancesPerCluster') && (showDefaults || ![1,null,''].includes(override.instancesPerCluster) ) )">
                                                        <strong class="label">Instances per Cluster</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.instancesPerCluster')"></span>
                                                        <span> : {{ override.instancesPerCluster }}</span>
                                                    </li>
                                                    <li v-if="hasProp(override, 'sgInstanceProfile')">
                                                        <strong class="label">Instance Profile </strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.sgInstanceProfile')"></span>
                                                        <span class="value">
                                                            : 
                                                            <template v-if="hasProp(override, 'sgInstanceProfile')">
                                                                <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + profile.name" target="_blank" v-for="profile in profiles" v-if="( (profile.name == override.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )"> 
                                                                    {{ profile.data.metadata.name }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }})
                                                                    <span class="eyeIcon"></span>
                                                                </router-link>
                                                            </template>
                                                            <template v-else>
                                                                Default (Cores: 1, RAM: 2GiB)
                                                            </template>
                                                        </span>
                                                    </li>
                                                    <li v-if="hasProp(override, 'configurations.sgPostgresConfig')">
                                                        <strong class="label">Postgres Configuration </strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.configurations.properties.sgPostgresConfig')"></span>
                                                        <span class="value"> : 
                                                            <template v-if="hasProp(override, 'configurations.sgPostgresConfig')">
                                                                <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + override.configurations.sgPostgresConfig" target="_blank"> 
                                                                    {{ override.configurations.sgPostgresConfig }}
                                                                    <span class="eyeIcon"></span>
                                                                </router-link>
                                                            </template>
                                                            <template v-else>
                                                                Default
                                                            </template>
                                                        </span>
                                                    </li>
                                                    <li v-if="hasProp(override, 'pods.persistentVolume.size') || hasProp(override, 'pods.persistentVolume.storageClass')">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Pods Storage</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods')"></span>
                                                        <ul>
                                                            <li>
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Persistent Volume</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.persistentVolume')"></span>
                                                                <ul>
                                                                    <li v-if="hasProp(override, 'pods.persistentVolume.size')">
                                                                        <strong class="label">Volume Size</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.persistentVolume.size')"></span>
                                                                        <span class="value"> : {{ override.pods.persistentVolume.size }}B</span>
                                                                    </li>
                                                                    <li v-if="hasProp(override, 'pods.persistentVolume.storageClass')">
                                                                        <strong class="label">Storage Class</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.persistentVolume.storageClass')"></span>
                                                                        <span class="value"> : {{ override.pods.persistentVolume.storageClass }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                </ul>
                                            </li>
                                        </ul>

                                        <ul v-if="hasProp(override, 'pods.disableConnectionPooling') || 
                                            hasProp(override, 'configurations.sgPoolingConfig') || 
                                            hasProp(override, 'pods.disablePostgresUtil') || 
                                            hasProp(override, 'pods.disableMetricsExporter') ||
                                            (hasProp(override, 'pods.customVolumes') && !isNull(override.pods.customVolumes)) || 
                                            (hasProp(override, 'pods.customInitContainers') && !isNull(override.pods.customInitContainers)) || 
                                            (hasProp(override, 'pods.customContainers') && !isNull(override.pods.customContainers))
                                        ">
                                            <li>
                                                <button class="toggleSummary"></button>
                                                <strong class="sectionTitle">Sidecars </strong>
                                                <ul>
                                                    <li v-if="hasProp(override, 'pods.disableConnectionPooling') || hasProp(override, 'configurations.sgPoolingConfig')">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Connection Pooling</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.configurations')"></span>
                                                        <span v-if="override.pods.disableConnectionPooling"> : {{ isEnabled(override.pods.disableConnectionPooling, true) }}</span>
                                                        <ul v-if="!override.pods.disableConnectionPooling || hasProp(override, 'configurations.sgPoolingConfig')">
                                                            <li>
                                                                <strong class="label">Connection Pooling Configuration</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.configurations.sgPoolingConfig')"></span>
                                                                <span class="value"> :
                                                                    <template v-if="hasProp(override, 'configurations.sgPoolingConfig')">
                                                                        <router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + override.configurations.sgPoolingConfig" target="_blank">
                                                                            {{ override.configurations.sgPoolingConfig }}
                                                                            <span class="eyeIcon"></span>
                                                                        </router-link>
                                                                    </template>
                                                                    <template v-else>
                                                                        Default
                                                                    </template>
                                                                </span>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                    <li v-if="hasProp(override, 'pods.disablePostgresUtil')">
                                                        <strong class="label">Postgres Utils</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.disablePostgresUtil').replace('If set to `true`', 'If disabled')"></span>
                                                        <span> : {{ isEnabled(override.pods.disablePostgresUtil, true) }}</span>
                                                    </li>
                                                    <li v-if="hasProp(override, 'pods.disableMetricsExporter')">
                                                        <strong class="label">Metrics Exporter</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                                                        <span> : {{ isEnabled(override.pods.disableMetricsExporter, true) }}</span>
                                                    </li>
                                                    <li v-if="( 
                                                        (override.pods.hasOwnProperty('customVolumes') && !isNull(override.pods.customVolumes)) || 
                                                        (override.pods.hasOwnProperty('customInitContainers') && !isNull(override.pods.customInitContainers)) || 
                                                        (override.pods.hasOwnProperty('customContainers') && !isNull(override.pods.customContainers)) )
                                                    ">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">User-Supplied Pods' Sidecars </strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods')"></span>
                                                        <ul>
                                                            <li v-if="override.pods.hasOwnProperty('customVolumes') && !isNull(override.pods.customVolumes)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Custom Volumes</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes')"></span>
                                                                <ul>
                                                                    <template v-for="(vol, index) in override.pods.customVolumes">
                                                                        <li :key="index">
                                                                            <button class="toggleSummary"></button>
                                                                            <strong class="label">Volume #{{ index + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="vol.hasOwnProperty('name')">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.name')"></span>
                                                                                    <span class="value"> : {{ vol.name }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('emptyDir') && Object.keys(vol.emptyDir).length && (!isNull(vol.emptyDir.medium) || !isNull(vol.emptyDir.sizeLimit))">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Empty Directory</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.emptyDir')"></span>
                                                                                    <ul>
                                                                                        <li v-if="vol.emptyDir.hasOwnProperty('medium') && !isNull(vol.emptyDir.medium)">
                                                                                            <strong class="label">Medium</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.emptyDir.properties.medium')"></span>
                                                                                            <span class="value"> : {{ vol.emptyDir.medium }}</span>
                                                                                        </li>
                                                                                        <li v-if="vol.emptyDir.hasOwnProperty('sizeLimit') && !isNull(vol.emptyDir.sizeLimit)">
                                                                                            <strong class="label">Size Limit</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.emptyDir.properties.sizeLimit')"></span>
                                                                                            <span class="value"> : {{ vol.emptyDir.sizeLimit }}</span>
                                                                                        </li>
                                                                                    </ul>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('configMap') && Object.keys(vol.configMap).length">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Config Map</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap')"></span>
                                                                                    <ul>
                                                                                        <li v-if="vol.configMap.hasOwnProperty('name') && !isNull(vol.configMap.name)">
                                                                                            <strong class="label">Name</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.name')"></span>
                                                                                            <span class="value"> : {{ vol.configMap.name }}</span>
                                                                                        </li>
                                                                                        <li v-if="vol.configMap.hasOwnProperty('optional')">
                                                                                            <strong class="label">Optional</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.name')"></span>
                                                                                            <span class="value"> : {{ isEnabled(vol.configMap.optional) }}</span>
                                                                                        </li>
                                                                                        <li v-if="vol.configMap.hasOwnProperty('defaultMode') && !isNull(vol.configMap.defaultMode)">
                                                                                            <strong class="label">Default Mode</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.defaultMode')"></span>
                                                                                            <span class="value"> : {{ vol.configMap.defaultMode }}</span>
                                                                                        </li>
                                                                                        <li v-if="vol.configMap.hasOwnProperty('items') && vol.configMap.items.length">
                                                                                            <button class="toggleSummary"></button>
                                                                                            <strong class="label">Items</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.items')"></span>
                                                                                            <ul>
                                                                                                <template v-for="(item, index) in vol.configMap.items">
                                                                                                    <li :key="index">
                                                                                                        <button class="toggleSummary"></button>
                                                                                                        <strong class="label">Item #{{ index + 1 }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.items.items')"></span>
                                                                                                        <ul>
                                                                                                            <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                                                <strong class="label">Key</strong>
                                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.items.items.properties.key')"></span>
                                                                                                                <span class="value"> : {{ item.key }}</span>
                                                                                                            </li>
                                                                                                            <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                                                <strong class="label">Mode</strong>
                                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.items.items.properties.mode')"></span>
                                                                                                                <span class="value"> : {{ item.mode }}</span>
                                                                                                            </li>
                                                                                                            <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                                                <strong class="label">Path</strong>
                                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.items.items.properties.path')"></span>
                                                                                                                <span class="value"> : {{ item.path }}</span>
                                                                                                            </li>
                                                                                                        </ul>
                                                                                                    </li>
                                                                                                </template>
                                                                                            </ul>
                                                                                        </li>
                                                                                    </ul>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('secret') && Object.keys(vol.secret).length">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Secret</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret')"></span>
                                                                                    <ul>
                                                                                        <li v-if="vol.secret.hasOwnProperty('secretName') && !isNull(vol.secret.secretName)">
                                                                                            <strong class="label">Secret Name</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.secretName')"></span>
                                                                                            <span class="value"> : {{ vol.secret.secretName }}</span>
                                                                                        </li>
                                                                                        <li v-if="vol.secret.hasOwnProperty('optional')">
                                                                                            <strong class="label">Optional</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.optional')"></span>
                                                                                            <span class="value"> : {{ isEnabled(vol.secret.optional) }}</span>
                                                                                        </li>
                                                                                        <li v-if="vol.secret.hasOwnProperty('defaultMode') && !isNull(vol.secret.defaultMode)">
                                                                                            <strong class="label">Default Mode</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.defaultMode')"></span>
                                                                                            <span class="value"> : {{ vol.secret.defaultMode }}</span>
                                                                                        </li>
                                                                                        <li v-if="vol.secret.hasOwnProperty('items') && vol.secret.items.length">
                                                                                            <button class="toggleSummary"></button>
                                                                                            <strong class="label">Items</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.items')"></span>
                                                                                            <ul>
                                                                                                <template v-for="(item, index) in vol.secret.items">
                                                                                                    <li :key="index">
                                                                                                        <button class="toggleSummary"></button>
                                                                                                        <strong class="label">Item #{{ index + 1 }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.items.items')"></span>
                                                                                                        <ul>
                                                                                                            <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                                                <strong class="label">Key:</strong>
                                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.items.items.properties.key')"></span>
                                                                                                                <span class="value"> : {{ item.key }}</span>
                                                                                                            </li>
                                                                                                            <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                                                <strong class="label">Mode</strong>
                                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.items.items.properties.mode')"></span>
                                                                                                                <span class="value"> : {{ item.mode }}</span>
                                                                                                            </li>
                                                                                                            <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                                                <strong class="label">Path</strong>
                                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.items.items.properties.path')"></span>
                                                                                                                <span class="value"> : {{ item.path }}</span>
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

                                                            <li v-if="override.pods.hasOwnProperty('customInitContainers') && !isNull(override.pods.customInitContainers)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Custom Init Containers</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers')"></span>

                                                                <ul>
                                                                    <template v-for="(container, index) in override.pods.customInitContainers">
                                                                        <li :key="'container-' + index">
                                                                            <button class="toggleSummary"></button>
                                                                            <strong class="label">Init Container #{{ index + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.name')"></span>
                                                                                    <span class="value"> : {{ container.name }}</span>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                                                    <strong class="label">Image</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.image')"></span>
                                                                                    <span class="value"> : {{ container.image }}</span>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                                                    <strong class="label">Image Pull Policy</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.imagePullPolicy')"></span>
                                                                                    <span class="value"> : {{ container.imagePullPolicy }}</span>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                                                    <strong class="label">Working Directory</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.workingDir')"></span>
                                                                                    <span class="value"> : {{ container.workingDir }}</span>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Arguments</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.arguments')"></span>
                                                                                    <ul>
                                                                                        <template v-for="(arg, aIndex) in container.args">
                                                                                            <li :key="'argument-' + index + '-' + aIndex">
                                                                                                <span class="value">{{ arg }}</span>
                                                                                            </li>
                                                                                        </template>
                                                                                    </ul>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('command') && !isNull(container.command)">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Command</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.command')"></span>
                                                                                    <ul>
                                                                                        <template v-for="(command, cIndex) in container.command">
                                                                                            <li :key="'command-' + index + '-' + cIndex">
                                                                                                <span class="value">{{ command }}</span>
                                                                                            </li>
                                                                                        </template>
                                                                                    </ul>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('env') && !isNull(container.env)">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Environment Variables</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.env')"></span>
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
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Ports</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports')"></span>
                                                                                    <ul>
                                                                                        <template v-for="(port, pIndex) in container.ports">
                                                                                            <li :key="'port-' + index + '-' + pIndex">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Port #{{ pIndex + 1 }}</strong>
                                                                                                <ul>
                                                                                                    <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                                                        <strong class="label">Name</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.name')"></span>
                                                                                                        <span class="value"> : {{ port.name }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                                                        <strong class="label">Host IP</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.hostIP')"></span>
                                                                                                        <span class="value"> : {{ port.hostIP }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                                                        <strong class="label">Host Port</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.hostPort')"></span>
                                                                                                        <span class="value"> : {{ port.hostPort }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                                                        <strong class="label">Container Port</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.containerPort')"></span>
                                                                                                        <span class="value"> : {{ port.containerPort }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                                                        <strong class="label">Protocol</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.protocol')"></span>
                                                                                                        <span class="value"> : {{ port.protocol }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </template>
                                                                                    </ul>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('volumeMounts') && !isNull(container.volumeMounts)">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Volume Mounts</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts')"></span>
                                                                                    <ul>
                                                                                        <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                                            <li :key="'vol-' + index + '-' + vIndex">
                                                                                                <strong class="label">Volume #{{ vIndex + 1 }}</strong>
                                                                                                <ul>
                                                                                                    <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                                                        <strong class="label">Name</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.name')"></span>
                                                                                                        <span class="value"> : {{ vol.name }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('readOnly')">
                                                                                                        <strong class="label">Read Only</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                                                        <span class="value"> : {{ isEnabled(vol.readOnly) }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                                                        <strong class="label">Mount Path</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                                                        <span class="value"> : {{ vol.mountPath }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                                                        <strong class="label">Mount Propagation</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                                                        <span class="value"> : {{ vol.mountPropagation }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                                                        <strong class="label">Sub Path</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.subPath')"></span>
                                                                                                        <span class="value"> : {{ vol.subPath }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                                                        <strong class="label">Sub Path Expr</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                                                        <span class="value"> : {{ vol.subPathExpr }}</span>
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

                                                            <li v-if="override.pods.hasOwnProperty('customContainers') && !isNull(override.pods.customContainers)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Custom Containers</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers')"></span>
                                                                <ul>
                                                                    <template v-for="(container, index) in override.pods.customContainers">
                                                                        <li :key="'container-' + index">
                                                                            <button class="toggleSummary"></button>
                                                                            <strong class="label">Container #{{ index + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.name')"></span>
                                                                                    <span class="value"> : {{ container.name }}</span>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                                                    <strong class="label">Image</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.image')"></span>
                                                                                    <span class="value"> : {{ container.image }}</span>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                                                    <strong class="label">Image Pull Policy</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.imagePullPolicy')"></span>
                                                                                    <span class="value"> : {{ container.imagePullPolicy }}</span>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                                                    <strong class="label">Working Directory</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.workingDir')"></span>
                                                                                    <span class="value"> : {{ container.workingDir }}</span>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Arguments</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.arguments')"></span>
                                                                                    <ul>
                                                                                        <template v-for="(arg, aIndex) in container.args">
                                                                                            <li :key="'argument-' + index + '-' + aIndex">
                                                                                                <span class="value">{{ arg }}</span>
                                                                                            </li>
                                                                                        </template>
                                                                                    </ul>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('command') && !isNull(container.command)">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Command</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.command')"></span>
                                                                                    <ul>
                                                                                        <template v-for="(command, cIndex) in container.command">
                                                                                            <li :key="'command-' + index + '-' + cIndex">
                                                                                                <span class="value">{{ command }}</span>
                                                                                            </li>
                                                                                        </template>
                                                                                    </ul>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('env') && !isNull(container.env)">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Environment Variables</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.env')"></span>
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
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Ports</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports')"></span>
                                                                                    <ul>
                                                                                        <template v-for="(port, pIndex) in container.ports">
                                                                                            <li :key="'port-' + index + '-' + pIndex">
                                                                                                <strong class="label">Port #{{ pIndex + 1 }}</strong>
                                                                                                <ul>
                                                                                                    <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                                                        <strong class="label">Name</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.name')"></span>
                                                                                                        <span class="value"> : {{ port.name }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                                                        <strong class="label">Host IP</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.hostIP')"></span>
                                                                                                        <span class="value"> : {{ port.hostIP }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                                                        <strong class="label">Host Port</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.hostPort')"></span>
                                                                                                        <span class="value"> : {{ port.hostPort }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                                                        <strong class="label">Container Port</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.containerPort')"></span>
                                                                                                        <span class="value"> : {{ port.containerPort }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                                                        <strong class="label">Protocol</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.protocol')"></span>
                                                                                                        <span class="value"> : {{ port.protocol }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </template>
                                                                                    </ul>
                                                                                </li>
                                                                                <li v-if="container.hasOwnProperty('volumeMounts') && !isNull(container.volumeMounts)">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Volume Mounts</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts')"></span>
                                                                                    <ul>
                                                                                        <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                                            <li :key="'vol-' + index + '-' + vIndex">
                                                                                                <strong class="label">Volume #{{ vIndex + 1 }}</strong>
                                                                                                <ul>
                                                                                                    <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                                                        <strong class="label">Name</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.name')"></span>
                                                                                                        <span class="value"> : {{ vol.name }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('readOnly')">
                                                                                                        <strong class="label">Read Only</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                                                        <span class="value"> : {{ isEnabled(vol.readOnly) }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                                                        <strong class="label">Mount Path</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                                                        <span class="value"> : {{ vol.mountPath }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                                                        <strong class="label">Mount Propagation</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                                                        <span class="value"> : {{ vol.mountPropagation }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                                                        <strong class="label">Sub Path</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.subPath')"></span>
                                                                                                        <span class="value"> : {{ vol.subPath }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                                                        <strong class="label">Sub Path Expr</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                                                        <span class="value"> : {{ vol.subPathExpr }}</span>
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
                                            </li>
                                        </ul>
                                        
                                        <ul v-if="hasProp(override, 'managedSql')">
                                            <li>
                                                <button class="toggleSummary"></button>
                                                <strong class="sectionTitle">Managed SQL </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.managedSql')"></span>
                                                <ul>
                                                    <li>
                                                        <button class="toggleSummary"></button>
                                                        <strong class="sectionTitle">Scripts </strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.managedSql.scripts')"></span>
                                                        <ul>
                                                            <li v-for="(baseScript, baseIndex) in override.managedSql.scripts">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="sectionTitle">SGScript #{{ baseIndex + 1 }}</strong>
                                                                <ul>
                                                                    <li v-if="( ( hasProp(baseScript, 'scriptSpec.continueOnError') && baseScript.scriptSpec.continueOnError ) )">
                                                                        <strong class="label">Continue on Error</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.managedSql.scripts.continueOnError')"></span>
                                                                        <span class="value"> : {{ hasProp(baseScript, 'scriptSpec.continueOnError') ? isEnabled(baseScript.continueOnError) : 'Disabled' }}</span>
                                                                    </li>
                                                                    <li v-if="( ( hasProp(baseScript, 'scriptSpec.managedVersions') && !baseScript.scriptSpec.managedVersions) )">
                                                                        <strong class="label">Managed Versions:</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.managedSql.scripts')"></span>
                                                                        <span class="value">{{ hasProp(baseScript, 'scriptSpec.managedVersions') && isEnabled(baseScript.scriptSpec.managedVersions) }}</span>
                                                                    </li>
                                                                    <li v-if="baseScript.hasOwnProperty('scriptSpec')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="sectionTitle">Script Entries</strong>

                                                                        <ul>
                                                                            <li v-for="(script, index) in baseScript.scriptSpec.scripts">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="sectionTitle">Script #{{ index + 1 }}</strong>

                                                                                <ul>
                                                                                    <li v-if="hasProp(script, 'name')">
                                                                                        <strong class="label">Name</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                                                        <span class="value"> : {{ script.name }}</span>
                                                                                    </li>
                                                                                    <li v-if="hasProp(script, 'version')">
                                                                                        <strong class="label">Version </strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                                                                        <span class="value"> : {{ script.version }}</span>
                                                                                    </li>
                                                                                    <li v-if="hasProp(script, 'database')">
                                                                                        <strong class="label">Database</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                                                                        <span class="value"> : {{ script.hasOwnProperty('database') ? script.database : 'postgres' }}</span>
                                                                                    </li>
                                                                                    <li v-if="hasProp(script, 'user')">
                                                                                        <strong class="label">User </strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                                                        <span class="value"> : {{ script.hasOwnProperty('user') ? script.database : 'postgres' }}</span>
                                                                                    </li>
                                                                                    <li v-if="( ( script.hasOwnProperty('retryOnError') && script.retryOnError) )">
                                                                                        <strong class="label">Retry on Error </strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError')"></span>
                                                                                        <span class="value"> : {{ script.hasOwnProperty('retryOnError') ? isEnabled(script.retryOnError) : 'Disabled' }}</span>
                                                                                    </li>
                                                                                    <li v-if="( ( script.hasOwnProperty('storeStatusInDatabase') && script.storeStatusInDatabase) )">
                                                                                        <strong class="label">Store Status in Database </strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase')"></span>
                                                                                        <span class="value"> : {{ script.hasOwnProperty('storeStatusInDatabase') ? isEnabled(script.storeStatusInDatabase) : 'Disabled' }}</span>
                                                                                    </li>
                                                                                    <li v-if="( ( script.hasOwnProperty('wrapInTransaction') && (script.wrapInTransaction != null) ) )">
                                                                                        <strong class="label">Wrap in Transaction </strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                                                                                        <span class="value"> : {{ script.hasOwnProperty('wrapInTransaction') ? script.wrapInTransaction : 'Disabled' }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Script Source </strong>
                                                                                        <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'"></span>
                                                                                        <span class="value"> : {{ hasProp(script, 'script') ? 'Raw Script' : (hasProp(script, 'scriptFrom.secretKeyRef') ? 'Secret Key' : "Config Map") }}</span>
                                                                                    </li>
                                                                                    <li v-if="hasProp(script, 'script')">
                                                                                        <strong class="label">Script</strong>
                                                                                        <span class="value script"> : 
                                                                                            <span>
                                                                                                <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)">
                                                                                                    View Script
                                                                                                    <span class="eyeIcon"></span>
                                                                                                </a>
                                                                                            </span>
                                                                                            <div :id="'script-' + baseIndex + '-' + index" class="hidden">
                                                                                                <pre>{{ script.script }}</pre>
                                                                                            </div>
                                                                                        </span>
                                                                                    </li>
                                                                                    <li v-else-if="hasProp(script, 'scriptFrom.secretKeyRef')">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong>Secret Key Reference:</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Name </strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>
                                                                                                <span class="value"> : {{ script.scriptFrom.secretKeyRef.name }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Key </strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                                                                                <span class="value"> : {{ script.scriptFrom.secretKeyRef.key }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-else-if="hasProp(script, 'scriptFrom.configMapKeyRef')">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong>Config Map Key Reference:</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Name </strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>
                                                                                                <span class="value"> : {{ script.scriptFrom.configMapKeyRef.name }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Key </strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                                                                                <span class="value"> : {{ script.scriptFrom.configMapKeyRef.key }}</span>
                                                                                            </li>                                                                            
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(script, 'scriptFrom.configMapScript')">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">
                                                                                            Config Map Script:
                                                                                        </strong>
                                                                                        <span class="value script">
                                                                                            <span>
                                                                                                <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)">View Script</a>
                                                                                            </span>
                                                                                            <div :id="'script-' + baseIndex + '-' + index" class="hidden">
                                                                                                <pre>{{ script.scriptFrom.configMapScript }}</pre>
                                                                                            </div>
                                                                                        </span>
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
                                                    <li v-if="( (override.managedSql.hasOwnProperty('continueOnSGScriptError') && override.managedSql.continueOnSGScriptError) )">
                                                        <strong class="label">Continue on SGScript Error </strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.managedSql.continueOnSGScriptError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                                        <span class="value"> : {{ hasProp(override, 'managedSql.continueOnSGScriptError') ? isEnabled(override.managedSql.continueOnSGScriptError) : 'Disabled' }}</span>
                                                    </li>
                                                </ul>
                                            </li>
                                        </ul>
                                        
                                        <ul v-if="( (hasProp(override, 'replication.mode') && (override.replication.mode != 'async')) )">
                                            <li>
                                                <button class="toggleSummary"></button>
                                                <strong class="sectionTitle">Pods Replication </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.replication')"></span>
                                                <ul>
                                                    <li v-if="((override.replication.mode != 'async') )">
                                                        <strong class="label">Mode</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.replication.mode')"></span>
                                                        <span class="value"> : {{ hasProp(override, 'replication.mode') ? override.replication.mode : 'async' }}</span>
                                                    </li>
                                                    <li v-if="hasProp(override, 'replication.syncInstances')">
                                                        <strong class="label">Sync Instances</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.replication.syncInstances')"></span>
                                                        <span class="value"> : {{ override.replication.syncInstances }}</span>
                                                    </li>
                                                </ul>
                                            </li>
                                        </ul>

                                        <ul v-if="hasProp(override, 'metadata.labels.clusterPods') || hasProp(override, 'metadata.annotations')">
                                            <li>
                                                <button class="toggleSummary"></button>
                                                <strong class="sectionTitle">Metadata </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata')"></span>
                                                <ul>
                                                    <li v-if="hasProp(override, 'metadata')">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Labels</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.labels')"></span>
                                                        <ul>
                                                            <li v-if="hasProp(override, 'metadata.labels.clusterPods')">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Cluster Pods</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.labels.clusterPods')"></span>
                                                                <ul>
                                                                    <li v-for="(value, label) in override.metadata.labels.clusterPods">
                                                                        <strong class="label">{{ label }}:</strong>
                                                                        <span class="value">{{ value }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                    <li v-if="hasProp(override, 'metadata.annotations')">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Annotations</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations')"></span>
                                                        <ul>
                                                            <li v-if="hasProp(override, 'metadata.annotations.allResources')">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">All Resources</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.allResources')"></span>
                                                                <ul>
                                                                    <li v-for="(value, label) in override.metadata.annotations.allResources">
                                                                        <strong class="label">{{ label }}:</strong>
                                                                        <span class="value">{{ value }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                            <li v-if="hasProp(override, 'metadata.annotations.clusterPods')">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Cluster Pods</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.clusterPods')"></span>
                                                                <ul>
                                                                    <li v-for="(value, label) in override.metadata.annotations.clusterPods">
                                                                        <strong class="label">{{ label }}:</strong>
                                                                        <span class="value">{{ value }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                            <li v-if="hasProp(override, 'metadata.annotations.services')">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Services</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.services')"></span>
                                                                <ul>
                                                                    <li v-for="(value, label) in override.metadata.annotations.services">
                                                                        <strong class="label">{{ label }}:</strong>
                                                                        <span class="value">{{ value }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                            <li v-if="hasProp(override, 'metadata.annotations.primaryService')">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Primary Service</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.primaryService')"></span>
                                                                <ul>
                                                                    <li v-for="(value, label) in override.metadata.annotations.primaryService">
                                                                        <strong class="label">{{ label }}:</strong>
                                                                        <span class="value">{{ value }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                            <li v-if="hasProp(override, 'metadata.annotations.replicasService')">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Replicas Service</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.replicasService')"></span>
                                                                <ul>
                                                                    <li v-for="(value, label) in override.metadata.annotations.replicasService">
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

                                        <ul v-if="hasProp(override, 'pods.scheduling')">
                                            <li>
                                                <button class="toggleSummary"></button>
                                                <strong class="sectionTitle">Pods Scheduling </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling')"></span>
                                                <ul>
                                                    <li v-if="hasProp(override, 'pods.scheduling.nodeSelector')">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Node Selectors</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeSelector')"></span>
                                                        <ul>
                                                            <li v-for="(value, key) in override.pods.scheduling.nodeSelector">
                                                                <strong class="label">{{ key }}</strong>
                                                                <span class="value"> : {{ value }}</span>
                                                            </li>
                                                        </ul>
                                                    </li>

                                                    <li v-if="hasProp(override, 'pods.scheduling.tolerations')">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Node Tolerations</strong>                                    
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations')"></span>
                                                        <ul>
                                                            <li v-for="(toleration, index) in override.pods.scheduling.tolerations">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Toleration #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li>
                                                                        <strong class="label">Key</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.key')"></span>
                                                                        <span class="value"> : {{ toleration.key }}</span>
                                                                    </li>
                                                                    <li>
                                                                        <strong class="label">Operator</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.operator')"></span>
                                                                        <span class="value"> : {{ toleration.operator }}</span>
                                                                    </li>
                                                                    <li v-if="toleration.hasOwnProperty('value')">
                                                                        <strong class="label">Value</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.value')"></span>
                                                                        <span class="value"> : {{ toleration.value }}</span>
                                                                    </li>
                                                                    <li>
                                                                        <strong class="label">Effect</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.effect')"></span>
                                                                        <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                                                    </li>
                                                                    <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                                                        <strong class="label">Toleration Seconds</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                                                        <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </ul>
                                                    </li>

                                                    <li v-if="hasProp(override, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Node Affinity</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                                                        <span>Required During Scheduling Ignored During Execution</span>
                                                        <ul>
                                                            <li>
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Terms</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items')"></span>
                                                                <ul>
                                                                    <li v-for="(term, index) in override.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Term #{{ index+1 }}</strong>
                                                                        <ul>
                                                                            <li v-if="term.hasOwnProperty('matchExpressions')">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span>
                                                                                <ul>
                                                                                    <li v-for="(exp, index) in term.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="term.hasOwnProperty('matchFields')">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Fields</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span>
                                                                                <ul>
                                                                                    <li v-for="(field, index) in term.matchFields">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Field #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ field.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ field.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="field.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
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
                                                    </li>

                                                    <li v-if="hasProp(override, 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Node Affinity</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                                                        <span>Preferred During Scheduling Ignored During Execution</span>
                                                        <ul>
                                                            <li>
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Terms</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span>
                                                                <ul>
                                                                    <li v-for="(term, index) in override.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Term #{{ index+1 }}</strong>
                                                                        <ul>
                                                                            <li v-if="term.preference.hasOwnProperty('matchExpressions')">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                                                <ul>
                                                                                    <li v-for="(exp, index) in term.preference.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="term.preference.hasOwnProperty('matchFields')">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Fields</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                                                <ul>
                                                                                    <li v-for="(field, index) in term.preference.matchFields">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Field #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ field.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ field.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="field.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="term.hasOwnProperty('weight')">
                                                                                <strong class="label">Weight</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                                <span class="value"> : {{ term.weight }}</span>
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
                                    </li>
                                </template>
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

        props: {
            cluster: Object,
            extensionsList: Array,
            details: {
                type: Boolean,
                default: false
            },
            dryRun: {
                type: Boolean,
                default: false
            }
        },

        data() {
            return {
                showDefaults: this.dryRun || this.details
            }
        },

		computed: {

            profiles () {
				return store.state.sginstanceprofiles
			},

			backups () {
				return store.state.sgbackups
			},
            
            
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

    .summary strong.label + span[data-tooltip] {
        display: inline-block;
        margin-left: 7px;
    }

    .summary li {
        margin-bottom: 10px;
        position: relative;
        list-style: none;
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

    .summary ul ul li:not(.warning):before {
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

    .crdDetails .summary {
        height: 100%;
        max-height: 100%;
    }

    .darkmode .summary ul ul ul li:last-of-type:after, .darkmode .summary ul.section > li > ul > li:last-child:after {
        background: var(--activeBg);
    }

    .darkmode .crdDetails .summary ul ul ul li:last-of-type:after, .darkmode .crdDetails .summary ul.section > li > ul > li:last-child:after {
        background: var(--bgColor);
    }

    span.arrow {
        background: "▾";
    }

    .summary button.toggleSummary {
        background: transparent;
        font-weight: bold;
        border: 0;
        margin: 0;
        padding: 10px;
        position: relative;
        top: -5px;
        left: -5px;
        margin-bottom: -6px;
        background: var(--activeBg);
        z-index: 1;
    }

    .crdDetails .summary button.toggleSummary {
        background: var(--bgColor);
    }

    .summary button.toggleSummary:before {
        content: "▾";
        display: block;
        position: absolute;
        top: 1px;
        left: 4px;
        width: 14px;
        color: var(--textColor);
        transition: all .3s ease-out;
    }

    .summary .collapsed .toggleSummary:before {
        transform: rotate(-90deg);
        transition: all .3s ease-out;
    }

    .summary .warning {
        background: none;
     }

    .summary .warning span {
        background: rgba(0,173,181,.05);
        border: 1px solid var(--blue) !important;
        padding: 15px;
        border-radius: 6px;
        display: inline-block;
        line-height: 1.1;
        width: 100%;
        margin-top: -5px;
        margin-left: -5px;
    }
    

</style>