<template>
	<div id="clusterSummary" class="visible" :class="details ? 'crdDetails' : 'contentTooltip'">
        <div v-if="!details" class="close" @click="closeSummary()"></div>
        
        <div class="info">
        
            <span v-if="!details" class="close" @click="closeSummary()">CLOSE</span>
            
            <div class="content">
                <div v-if="!details" class="header">
                    <h2>Summary</h2>
                    <label for="showDefaults" class="switch floatRight upper">
                        <span>Show Default Values</span>
                        <input type="checkbox" id="showDefaults" class="switch" v-model="showDefaults">
                    </label>
                </div>

                <div class="summary" v-if="cluster.hasOwnProperty('data')">
                    <ul class="section">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Metadata </strong>
                            <ul>
                                <li v-if="showDefaults">
                                    <strong class="label">Namespace</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.metadata.namespace')"></span>
                                    <span class="value"> : 
                                        <router-link :to="'/' + cluster.data.metadata.namespace">
                                            {{ cluster.data.metadata.namespace }}
                                            <span class="eyeIcon"></span>
                                        </router-link>
                                    </span>
                                </li>
                                <li>
                                    <strong class="label">Name</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.metadata.name')"></span>
                                    <span class="value"> :
                                        <router-link :to="'/' + cluster.data.metadata.namespace + '/sgcluster/' + cluster.data.metadata.name">
                                            {{ cluster.data.metadata.name }}
                                            <span class="eyeIcon"></span>
                                        </router-link>
                                    </span>
                                </li>
                            </ul>
                        </li>

                    </ul>

                    <ul class="section"
                        v-if="(showDefaults || (
                            (cluster.data.spec.profile != 'production') ||
                            (cluster.data.spec.instances > 1) || 
                            hasProp(cluster, 'data.spec.sgInstanceProfile') ||
                            (cluster.data.spec.postgres.flavor != 'vanilla') || 
                            (cluster.data.spec.postgres.version != 'latest') || 
                            hasProp(cluster, 'data.spec.configurations.sgPostgresConfig') || 
                            hasProp(cluster, 'data.spec.postgres.ssl') ||
                            (cluster.data.spec.pods.persistentVolume.size != '1Gi') ||
                            hasProp(cluster, 'data.spec.pods.persistentVolume.storageClass')
                        )
                    )">
    
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Specs </strong>
                            <ul>
                                <li v-if="showDefaults || (cluster.data.spec.profile != 'production')">
                                    <strong class="label">Profile</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.profile')"></span>
                                    <span class="value capitalize"> : {{ cluster.data.spec.profile }}</span>
                                </li>
                                <li v-if="showDefaults || (cluster.data.spec.instances > 1) || hasProp(cluster, 'data.spec.sgInstanceProfile')" :set="showInstances = true">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Instances</strong>
                                    <ul>
                                        <li v-if="showInstances">
                                            <strong class="label">Number of Instances</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.instances')"></span>
                                            <span class="value"> : {{ cluster.data.spec.instances }}</span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.sgInstanceProfile') || showDefaults">
                                            <strong class="label">Instance Profile</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.sgInstanceProfile')"></span>
                                            <span class="value"> : 
                                                <template v-if="hasProp(cluster, 'data.spec.sgInstanceProfile')">
                                                    <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + profile.name" target="_blank" v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )"> 
                                                        {{ profile.data.metadata.name }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }})
                                                        <span class="eyeIcon"></span>
                                                    </router-link>
                                                    <!--Temporarily disabled until REST API response data is set properly-->
                                                    
                                                    <!--<template v-if="clusterProfileMismatch(cluster, profile)">
                                                        <span class="helpTooltip alert" data-tooltip="This profile has been modified recently. Cluster must be restarted in order to apply such changes."></span>
                                                    </template>-->
                                                </template>
                                                <template v-else>
                                                    Default (Cores: 1, RAM: 2GiB)
                                                </template>
                                            </span>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="showDefaults || (cluster.data.spec.postgres.flavor != 'vanilla') || (cluster.data.spec.postgres.version != 'latest') || hasProp(cluster, 'data.spec.configurations.sgPostgresConfig') || hasProp(cluster, 'data.spec.postgres.ssl')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Postgres</strong>
                                     <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres')"></span>
                                    <ul>
                                        <li v-if="(cluster.data.spec.postgres.flavor != 'vanilla') || showDefaults">
                                            <strong class="label">Flavor</strong>
                                             <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.flavor')"></span>
                                            <span class="value capitalize"> : {{ cluster.data.spec.postgres.flavor }}</span>
                                        </li>
                                        <li v-if="(cluster.data.spec.postgres.version != 'latest') || showDefaults">
                                            <strong class="label">Version</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.version')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgres.version }}</span>
                                        </li>
                                        <li v-if="(hasProp(cluster, 'data.spec.configurations.sgPostgresConfig') || showDefaults)">
                                            <strong class="label">Configuration</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPostgresConfig')"></span>
                                            <span class="value"> :
                                                <template v-if="hasProp(cluster, 'data.spec.configurations.sgPostgresConfig')">
                                                    <template v-if="(cluster.data.spec.configurations.sgPostgresConfig === 'createNewResource')">
                                                        Create New Resource
                                                    </template>
                                                    <template v-else>
                                                        <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + cluster.data.spec.configurations.sgPostgresConfig" target="_blank"> 
                                                            {{ cluster.data.spec.configurations.sgPostgresConfig }}
                                                            <span class="eyeIcon"></span>
                                                        </router-link>
                                                    </template>
                                                </template>
                                                <template v-else>
                                                    Default
                                                </template>
                                            </span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.postgres.ssl.enabled') && cluster.data.spec.postgres.ssl.enabled">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">SSL Connections</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl')"></span>
                                            <span class="value"> : Enabled</span>
                                            <ul>
                                                <li>
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Certificate Secret Key Selector</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.certificateSecretKeySelector')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.certificateSecretKeySelector.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.postgres.ssl.certificateSecretKeySelector.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Key</strong>
                                                             <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.certificateSecretKeySelector.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.postgres.ssl.certificateSecretKeySelector.key }}</span>
                                                        </li>                                                                            
                                                    </ul>
                                                </li>
                                                <li>
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Private Key Secret Key Selector</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.privateKeySecretKeySelector')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.privateKeySecretKeySelector.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.postgres.ssl.privateKeySecretKeySelector.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Key</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.privateKeySecretKeySelector.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.postgres.ssl.privateKeySecretKeySelector.key }}</span>
                                                        </li>                                                                            
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="showDefaults || (cluster.data.spec.pods.persistentVolume.size != '1Gi') || hasProp(cluster, 'data.spec.pods.persistentVolume.storageClass')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Pods Storage</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods')"></span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Persistent Volume</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume')"></span>
                                            <ul>
                                                <li v-if="showDefaults || (cluster.data.spec.pods.persistentVolume.size != '1Gi')">
                                                    <strong class="label">Volume Size</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.size')"></span>
                                                    <span class="value"> : {{ cluster.data.spec.pods.persistentVolume.size }}B</span>
                                                </li>
                                                <li v-if="hasProp(cluster, 'data.spec.pods.persistentVolume.storageClass')">
                                                    <strong class="label">Storage Class</strong>
                                                     <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.storageClass')"></span>
                                                    <span class="value"> : {{ cluster.data.spec.pods.persistentVolume.storageClass }}</span>
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
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.extensions')"></span>
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
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups')"></span>
                            <ul v-for="backup in cluster.data.spec.configurations.backups">
                                <li>
                                    <strong class="label">Object Storage</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.sgObjectStorage')"></span>
                                    <span class="value"> : 
                                        <router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + backup.sgObjectStorage" target="_blank"> 
                                            {{ backup.sgObjectStorage }}
                                            <span class="eyeIcon"></span>
                                        </router-link>
                                    </span>
                                </li>
                                <li v-if="( showDefaults || ( backup.cronSchedule != '0 3 * * *' ) )">
                                    <strong class="label">Cron Schedule</strong>
                                     <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.cronSchedule')"></span>
                                    <span class="value"> : {{ tzCrontab(backup.cronSchedule) }} ({{ tzCrontab(backup.cronSchedule) | prettyCRON(false) }})</span>
                                </li>
                                <li v-if="!isNull(backup.path)">
                                    <strong class="label">Path</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.path')"></span>
                                    <span class="value"> : {{ backup.path }}</span>
                                </li>
                                <li v-if="( showDefaults || (backup.retention != 5) )">
                                    <strong class="label">Retention Window</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.retention')"></span>
                                    <span class="value"> : {{ backup.retention }}</span>
                                </li>
                                <li v-if="( showDefaults || (backup.compression != 'lz4') )">
                                    <strong class="label">Compression Method</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.compression')"></span>
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
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance')"></span>
                                    <ul>
                                        <li v-if="( showDefaults || ( hasProp(backup, 'performance.maxNetworkBandwidth') && !isNull(backup.performance.maxNetworkBandwidth)) )">
                                            <strong class="label">Max Network Bandwidth</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance.properties.maxNetworkBandwidth')"></span>
                                            <span class="value"> : {{ ( hasProp(backup, 'performance.maxNetworkBandwidth') && !isNull(backup.performance.maxNetworkBandwidth) ) ? backup.performance.maxNetworkBandwidth : 'Unlimited' }} </span>
                                        </li>
                                        <li v-if="( showDefaults || ( hasProp(backup, 'performance.maxDiskBandwidth') && !isNull(backup.performance.maxDiskBandwidth)) )">
                                            <strong class="label">Max Disk Bandwidth</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance.properties.maxDiskBandwidth')"></span>
                                            <span class="value"> : {{ ( hasProp(backup, 'performance.maxDiskBandwidth') && !isNull(backup.performance.maxDiskBandwidth) ) ? backup.performance.maxDiskBandwidth : 'Unlimited' }} </span>
                                        </li>
                                        <li v-if="( showDefaults || ( hasProp(backup, 'performance.uploadDiskConcurrency') && (backup.performance.uploadDiskConcurrency != 1) ) )">
                                            <strong class="label">Upload Disk Concurrency</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance.properties.uploadDiskConcurrency')"></span>
                                            <span class="value"> : {{ hasProp(backup, 'performance.uploadDiskConcurrency') ? backup.performance.uploadDiskConcurrency : 1 }} </span>
                                        </li>
                                        
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>


                    <ul class="section" v-if="hasProp(cluster, 'data.spec.initialData')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Cluster Initialization </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData')"></span>   
                            <ul>
                                <li :set="backup = backups.find( b => (b.data.metadata.name == cluster.data.spec.initialData.restore.fromBackup.name) )">
                                    <strong class="label">Backup</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup.name')"></span>
                                    <span class="value"> : 
                                        <template v-if="(typeof backup !== 'undefined')">
                                            <router-link :to="'/' + backup.data.metadata.namespace + '/sgbackup/' + backup.data.metadata.name" target="_blank"> 
                                                {{ backup.data.metadata.name }} [{{ backup.data.metadata.uid.substring(0,4) }}...{{ backup.data.metadata.uid.slice(-4) }}]
                                                <span class="eyeIcon"></span>
                                            </router-link>
                                        </template>
                                        <template v-else>
                                            {{ cluster.data.spec.initialData.restore.fromBackup.name }}
                                        </template>
                                    </span>
                                </li>
                                <li v-if="(typeof backup !== 'undefined')">
                                    <strong class="label">Backup Date</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.stored')"></span>
                                    <span class="value timestamp"> :
                                        <template v-if="backup.data.status.process.status == 'Completed'">
                                            <span class='date'>
                                                {{ backup.data.status.process.timing.stored | formatTimestamp('date') }}
                                            </span>
                                            <span class='time'>
                                                {{ backup.data.status.process.timing.stored | formatTimestamp('time') }}
                                            </span>
                                            <span class='ms'>
                                                {{ backup.data.status.process.timing.stored | formatTimestamp('ms') }}
                                            </span>
                                            <span class='tzOffset'>{{ showTzOffset() }}</span>
                                        </template>
                                    </span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.initialData.restore.fromBackup.pointInTimeRecovery')">
                                    <strong class="label">Point-in-Time Recovery</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup.pointInTimeRecovery')"></span>
                                    <span class="value timestamp"> :
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
                                    <strong class="label">Download Disk Concurrency</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.downloadDiskConcurrency')"></span>
                                    <span class="value"> : {{ hasProp(cluster, 'data.spec.initialData.restore.downloadDiskConcurrency') ? cluster.data.spec.initialData.restore.downloadDiskConcurrency : 1 }}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.replicateFrom')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Replicate From </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom')"></span>
                            <ul>
                                <li>
                                    <strong class="label">Source</strong>
                                    <span class="helpTooltip" data-tooltip="Specifies the source from which this cluster is being replicated."></span>
                                    <span class="value"> :
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
                                    <strong class="label">Cluster</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.sgCluster')"></span>
                                    <span class="value"> : 
                                        <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.data.spec.replicateFrom.instance.sgCluster" target="_blank">
                                            {{ cluster.data.spec.replicateFrom.instance.sgCluster }}
                                            <span class="eyeIcon"></span>
                                        </router-link>
                                    </span>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.external')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">External Instance Specs</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.external')"></span>
                                    <ul>
                                        <li>
                                            <strong class="label">Host</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.external.host')"></span>
                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.instance.external.host }}</span>
                                        </li>
                                        <li>
                                            <strong class="label">Port</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.external.port')"></span>
                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.instance.external.port }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.storage')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Storage Specs</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage')"></span>
                                    <ul>
                                        <li>
                                            <strong class="label">Object Storage</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.sgObjectStorage')"></span>
                                            <span class="value"> : 
                                                <router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + cluster.data.spec.replicateFrom.storage.sgObjectStorage" target="_blank">
                                                    {{ cluster.data.spec.replicateFrom.storage.sgObjectStorage }}
                                                    <span class="eyeIcon"></span>
                                                </router-link>
                                            </span>
                                        </li>
                                        <li>
                                            <strong class="label">Path</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.path')"></span>
                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.storage.path }}</span>
                                        </li>
                                        <li v-if="(
                                            hasProp(cluster, 'data.spec.replicateFrom.storage.performance') && (
                                                ( (hasProp(cluster, 'data.spec.replicateFrom.storage.performance.downloadConcurrency')) && !isNull(cluster.data.spec.replicateFrom.storage.performance.downloadConcurrency) )||
                                                ( (hasProp(cluster, 'data.spec.replicateFrom.storage.performance.maxNetworkBandwidth')) & !isNull(cluster.data.spec.replicateFrom.storage.performance.maxNetworkBandwidth) ) ||
                                                ( (hasProp(cluster, 'data.spec.replicateFrom.storage.performance.maxDiskBandwidth')) && !isNull(cluster.data.spec.replicateFrom.storage.performance.maxDiskBandwidth) )
                                            ) 
                                        )">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Performance Specs</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance')"></span>
                                            <ul>
                                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.storage.performance.downloadConcurrency') && !isNull(cluster.data.spec.replicateFrom.storage.performance.downloadConcurrency)">
                                                    <strong class="label">Download Concurrency</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance.downloadConcurrency')"></span>
                                                    <span class="value"> : {{ cluster.data.spec.replicateFrom.storage.performance.downloadConcurrency }}</span>
                                                </li>
                                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.storage.performance.maxDiskBandwidth') && !isNull(cluster.data.spec.replicateFrom.storage.performance.maxDiskBandwidth)">
                                                    <strong class="label">Max Disk Bandwidth</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance.maxDiskBandwidth')"></span>
                                                    <span class="value"> : {{ cluster.data.spec.replicateFrom.storage.performance.maxDiskBandwidth }}</span>
                                                </li>
                                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.storage.performance.maxNetworkBandwidth') && !isNull(cluster.data.spec.replicateFrom.storage.performance.maxNetworkBandwidth)">
                                                    <strong class="label">Max Network Bandwidth</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance.maxNetworkBandwidth')"></span>
                                                    <span class="value"> : {{ cluster.data.spec.replicateFrom.storage.performance.maxNetworkBandwidth }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.replicateFrom.users')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Users Credentials</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users')"></span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Superuser</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser')"></span>
                                            <ul>
                                                <li>
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Username</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.username')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.username.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.superuser.username.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.username.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.superuser.username.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li>
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Password</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.password')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.password.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.superuser.password.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.password.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.superuser.password.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                        <li>
                                            <strong class="label">Replication User</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication')"></span>
                                            <ul>
                                                <li>
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Username</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.username')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.username.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.replication.username.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.username.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.replication.username.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li>
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Password</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.password')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.password.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.replication.password.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.password.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.replication.password.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Authenticator User</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator')"></span>
                                            <ul>
                                                <li>
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Username</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.username')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.username.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.authenticator.username.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.username.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.authenticator.username.key }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                                <li>
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Password</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.password')"></span>
                                                    <ul>
                                                        <li>
                                                            <strong class="label">Secret Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.password.name')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.authenticator.password.name }}</span>
                                                        </li>
                                                        <li>
                                                            <strong class="label">Secret Key</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.password.key')"></span>
                                                            <span class="value"> : {{ cluster.data.spec.replicateFrom.users.authenticator.password.key }}</span>
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

                    <ul class="section" v-if="hasProp(cluster, 'data.spec.managedSql')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Managed SQL </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.managedSql')"></span>
                            <ul>
                                <li>
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Scripts</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.managedSql.scripts')"></span>
                                    <ul>
                                        <li v-for="(baseScript, baseIndex) in cluster.data.spec.managedSql.scripts">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">SGScript #{{ baseIndex + 1 }}</strong>
                                            <ul>
                                                <li v-if="hasProp(baseScript, 'id')">
                                                    <strong class="label">ID</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.id')"></span>
                                                    <span class="value"> : {{baseScript.id }}</span>
                                                </li>
                                                <li v-if="baseScript.hasOwnProperty('scriptSpec') && hasProp(baseScript, 'sgScript')">
                                                    <strong class="label">Name</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts')"></span>
                                                    <span class="value"> : {{ baseScript.sgScript }}</span>
                                                </li>
                                                <li v-if="( showDefaults || ( hasProp(baseScript, 'scriptSpec.continueOnError') && baseScript.scriptSpec.continueOnError ) )">
                                                    <strong class="label">Continue on Error</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                                    <span class="value"> : {{ hasProp(baseScript, 'scriptSpec.continueOnError') ? isEnabled(baseScript.continueOnError) : 'Disabled' }}</span>
                                                </li>
                                                <li v-if="( showDefaults || ( hasProp(baseScript, 'scriptSpec.managedVersions') && !baseScript.scriptSpec.managedVersions) )">
                                                    <strong class="label">Managed Versions</strong>
                                                     <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions').replace(/true/g, 'Enabled')"></span>
                                                    <span class="value"> : {{ hasProp(baseScript, 'scriptSpec.managedVersions') && isEnabled(baseScript.scriptSpec.managedVersions) }}</span>
                                                </li>
                                                <li v-if="baseScript.hasOwnProperty('scriptSpec')">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Script Entries</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.script').replace(/ This field is mutually exclusive with `scriptFrom` field./g, '').replace('script','scripts')"></span>

                                                    <ul>
                                                        <li v-for="(script, index) in baseScript.scriptSpec.scripts">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Entry #{{ index + 1 }}</strong>
                                                            <ul>
                                                                <li v-if="hasProp(script, 'id')">
                                                                    <strong class="label">ID</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.id')"></span>
                                                                    <span class="value"> : {{ script.id }}</span>
                                                                </li>
                                                                <li v-if="hasProp(script, 'version')">
                                                                    <strong class="label">Version</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                                                    <span class="value"> : {{ script.version }}</span>
                                                                </li>
                                                                <li v-if="hasProp(script, 'name')">
                                                                    <strong class="label">Name</strong>
                                                                     <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                                    <span class="value"> : {{ script.name }}</span>
                                                                </li>
                                                                <li v-if="showDefaults || hasProp(script, 'database')">
                                                                    <strong class="label">Database</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('database') ? script.database : 'postgres' }}</span>
                                                                </li>
                                                                <li v-if="showDefaults || hasProp(script, 'user')">
                                                                    <strong class="label">User</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.user')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('user') ? script.database : 'postgres' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('wrapInTransaction') && (script.wrapInTransaction != null) ) )">
                                                                    <strong class="label">Wrap in Transaction</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction').replace('If not set', 'If Disabled')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('wrapInTransaction') ? script.wrapInTransaction : 'Disabled' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('storeStatusInDatabase') && script.storeStatusInDatabase) )">
                                                                    <strong class="label">Store Status in Database</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase').replace(/false/g, 'Disabled').replace(/true/g, 'Disabled')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('storeStatusInDatabase') ? isEnabled(script.storeStatusInDatabase) : 'Disabled' }}</span>
                                                                </li>
                                                                <li v-if="( showDefaults || ( script.hasOwnProperty('retryOnError') && script.retryOnError) )">
                                                                    <strong class="label">Retry on Error</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError').replace(/false/g, 'Disabled').replace(/true/g, 'Disabled')"></span>
                                                                    <span class="value"> : {{ script.hasOwnProperty('retryOnError') ? isEnabled(script.retryOnError) : 'Disabled' }}</span>
                                                                </li>
                                                                <li>
                                                                    <strong class="label">Script Source</strong>
                                                                    <span class="helpTooltip" :data-tooltip="( script.hasOwnProperty('scriptFrom') ? getTooltip('sgscript.spec.scripts.scriptFrom') : getTooltip('sgscript.spec.scripts.script') )"></span>
                                                                    <span class="value"> : {{ hasProp(script, 'script') ? 'Raw Script' : (hasProp(script, 'scriptFrom.secretKeyRef') ? 'Secret Key' : "Config Map") }}</span>
                                                                </li>
                                                                <li v-if="hasProp(script, 'script')">
                                                                    <strong class="label">Script</strong>
                                                                    <span class="value script">:
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
                                                                    <strong class="label">Secret Key Reference</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.secretKeyRef')"></span>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Name</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.secretKeyRef.name')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.secretKeyRef.name }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.secretKeyRef.key')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.secretKeyRef.key }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                                <li v-else-if="hasProp(script, 'scriptFrom.configMapKeyRef')">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Config Map Key Reference</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.configMapKeyRef')"></span>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Name</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.configMapKeyRef.name')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.configMapKeyRef.name }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.configMapKeyRef.key')"></span>
                                                                            <span class="value"> : {{ script.scriptFrom.configMapKeyRef.key }}</span>
                                                                        </li>                                                                            
                                                                    </ul>
                                                                </li>
                                                                <li v-if="hasProp(script, 'scriptFrom.configMapScript')">
                                                                    <strong class="label">Config Map Script</strong>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.configMapScript')"></span>
                                                                    <span class="value script"> : 
                                                                        <span>
                                                                            <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)">
                                                                                View Script
                                                                                <span class="eyeIcon"></span>
                                                                            </a>
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
                                                    <strong class="label">SGScript</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript')"></span>
                                                    <span class="value"> :
                                                        <router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.sgScript" target="_blank">
                                                            {{ baseScript.sgScript }}
                                                            <span class="eyeIcon"></span>
                                                        </router-link>
                                                    </span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="( showDefaults || (cluster.data.spec.managedSql.hasOwnProperty('continueOnSGScriptError') && cluster.data.spec.managedSql.continueOnSGScriptError) )">
                                    <strong class="label">Continue on SGScript Error</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.managedSql.continueOnSGScriptError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                    <span class="value"> : {{ hasProp(cluster, 'data.spec.managedSql.continueOnSGScriptError') ? isEnabled(cluster.data.spec.managedSql.continueOnSGScriptError) : 'Disabled' }}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>

                    <ul class="section" v-if="showDefaults || cluster.data.spec.pods.disableConnectionPooling || hasProp(cluster, 'data.spec.configurations.sgPoolingConfig') || cluster.data.spec.pods.disablePostgresUtil || cluster.data.spec.pods.disableMetricsExporter || cluster.data.spec.prometheusAutobind || hasProp(cluster, 'data.spec.distributedLogs.sgDistributedLogs')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Sidecars </strong>
                            <ul>
                                <li v-if="showDefaults || cluster.data.spec.pods.disableConnectionPooling || hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Connection Pooling</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations')"></span>
                                    <span v-if="showDefaults || cluster.data.spec.pods.disableConnectionPooling"> : {{ isEnabled(cluster.data.spec.pods.disableConnectionPooling, true) }}</span>
                                    <ul v-if="(showDefaults && !cluster.data.spec.pods.disableConnectionPooling) || hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                        <li>
                                            <strong class="label">Connection Pooling Configuration</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
                                            <span class="value"> :
                                                <template v-if="hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                                    <template v-if="(cluster.data.spec.configurations.sgPoolingConfig === 'createNewResource')">
                                                        Create New Resource
                                                    </template>
                                                    <template v-else>
                                                        <router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + cluster.data.spec.configurations.sgPoolingConfig" target="_blank">
                                                            {{ cluster.data.spec.configurations.sgPoolingConfig }}
                                                            <span class="eyeIcon"></span>
                                                        </router-link>
                                                    </template>
                                                </template>
                                                <template v-else>
                                                    Default
                                                </template>
                                            </span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="showDefaults || cluster.data.spec.pods.disablePostgresUtil">
                                    <strong class="label">Postgres Utils</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.disablePostgresUtil').replace('If set to `true`', 'If disabled')"></span>
                                    <span> : {{ isEnabled(cluster.data.spec.pods.disablePostgresUtil, true) }}</span>
                                </li>
                                <li v-if="showDefaults || cluster.data.spec.pods.disableMetricsExporter || cluster.data.spec.prometheusAutobind">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Monitoring</strong>
                                     <span class="helpTooltip" data-tooltip="StackGres supports enabling automatic monitoring for your Postgres cluster, but you need to provide or install the <a href='https://stackgres.io/doc/latest/install/prerequisites/monitoring/' target='_blank'>Prometheus stack as a pre-requisite</a>. Then, check this option to configure automatically sending metrics to the Prometheus stack."></span>
                                    <span> : {{ (!cluster.data.spec.pods.disableMetricsExporter && cluster.data.spec.prometheusAutobind) ? ' Enabled' : ' Disabled' }}</span>
                                    <ul>
                                        <li v-if="showDefaults || cluster.data.spec.pods.disableMetricsExporter">
                                            <strong class="label">Metrics Exporter</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                                            <span> : {{ isEnabled(cluster.data.spec.pods.disableMetricsExporter, true) }}</span>
                                        </li>
                                        <li v-if="showDefaults || cluster.data.spec.prometheusAutobind">
                                            <strong class="label">Prometheus Autobind</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.prometheusAutobind')"></span>
                                            <span> : {{ isEnabled(cluster.data.spec.prometheusAutobind) }}</span>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="hasProp(cluster, 'data.spec.distributedLogs.sgDistributedLogs')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Distributed Logs</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs')"></span>
                                    <ul>
                                        <li>
                                            <strong class="label">Logs Server</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.sgDistributedLogs')"></span>
                                            <span class="value"> :
                                                <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.data.spec.distributedLogs.sgDistributedLogs" target="_blank">
                                                    {{ cluster.data.spec.distributedLogs.sgDistributedLogs }}
                                                     <span class="eyeIcon"></span>
                                                </router-link>
                                            </span>
                                        </li>
                                        <li v-if="hasProp(cluster, 'data.spec.distributedLogs.retention')">
                                            <strong class="label">Retention</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.retention')"></span>
                                            <span class="value"> : {{ cluster.data.spec.distributedLogs.retention }}</span>
                                        </li>
                                    </ul>
                                </li>
                   
                                <li v-if="( 
                                    (cluster.data.spec.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.pods.customVolumes)) || 
                                    (cluster.data.spec.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.pods.customInitContainers)) || 
                                    (cluster.data.spec.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.pods.customContainers)) )
                                ">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">User-Supplied Pods' Sidecars </strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods')"></span>
                                    <ul>
                                        <li v-if="cluster.data.spec.pods.hasOwnProperty('customVolumes') && !isNull(cluster.data.spec.pods.customVolumes)">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Volumes</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes')"></span>
                                            <ul>
                                                <template v-for="(vol, index) in cluster.data.spec.pods.customVolumes">
                                                    <li :key="index">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Volume #{{ index + 1 }}</strong>
                                                        <ul>
                                                            <li v-if="vol.hasOwnProperty('name')">
                                                                <strong class="label">Name</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.name')"></span>
                                                                <span class="value"> : {{ vol.name }}</span>
                                                            </li>
                                                            <li v-if="vol.hasOwnProperty('emptyDir') && Object.keys(vol.emptyDir).length && (!isNull(vol.emptyDir.medium) || !isNull(vol.emptyDir.sizeLimit))">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Empty Directory</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.emptyDir')"></span>
                                                                <ul>
                                                                    <li v-if="vol.emptyDir.hasOwnProperty('medium') && !isNull(vol.emptyDir.medium)">
                                                                        <strong class="label">Medium</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.emptyDir.properties.medium')"></span>
                                                                        <span class="value"> : {{ vol.emptyDir.medium }}</span>
                                                                    </li>
                                                                    <li v-if="vol.emptyDir.hasOwnProperty('sizeLimit') && !isNull(vol.emptyDir.sizeLimit)">
                                                                        <strong class="label">Size Limit</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.emptyDir.properties.sizeLimit')"></span>
                                                                        <span class="value"> : {{ vol.emptyDir.sizeLimit }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                            <li v-if="vol.hasOwnProperty('configMap') && Object.keys(vol.configMap).length">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Config Map</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap')"></span>
                                                                <ul>
                                                                    <li v-if="vol.configMap.hasOwnProperty('name') && !isNull(vol.configMap.name)">
                                                                        <strong class="label">Name</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.name')"></span>
                                                                        <span class="value"> : {{ vol.configMap.name }}</span>
                                                                    </li>
                                                                    <li v-if="vol.configMap.hasOwnProperty('optional')">
                                                                        <strong class="label">Optional</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.name')"></span>
                                                                        <span class="value"> : {{ isEnabled(vol.configMap.optional) }}</span>
                                                                    </li>
                                                                    <li v-if="vol.configMap.hasOwnProperty('defaultMode') && !isNull(vol.configMap.defaultMode)">
                                                                        <strong class="label">Default Mode</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.defaultMode')"></span>
                                                                        <span class="value"> : {{ vol.configMap.defaultMode }}</span>
                                                                    </li>
                                                                    <li v-if="vol.configMap.hasOwnProperty('items') && vol.configMap.items.length">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Items</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.items')"></span>
                                                                        <ul>
                                                                            <template v-for="(item, index) in vol.configMap.items">
                                                                                <li :key="index">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Item #{{ index + 1 }}</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.items.items')"></span>
                                                                                    <ul>
                                                                                        <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                            <strong class="label">Key</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.items.items.properties.key')"></span>
                                                                                            <span class="value"> : {{ item.key }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                            <strong class="label">Mode</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.items.items.properties.mode')"></span>
                                                                                            <span class="value"> : {{ item.mode }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                            <strong class="label">Path</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.items.items.properties.path')"></span>
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
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret')"></span>
                                                                <ul>
                                                                    <li v-if="vol.secret.hasOwnProperty('secretName') && !isNull(vol.secret.secretName)">
                                                                        <strong class="label">Secret Name</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.secretName')"></span>
                                                                        <span class="value"> : {{ vol.secret.secretName }}</span>
                                                                    </li>
                                                                    <li v-if="vol.secret.hasOwnProperty('optional')">
                                                                        <strong class="label">Optional</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.optional')"></span>
                                                                        <span class="value"> : {{ isEnabled(vol.secret.optional) }}</span>
                                                                    </li>
                                                                    <li v-if="vol.secret.hasOwnProperty('defaultMode') && !isNull(vol.secret.defaultMode)">
                                                                        <strong class="label">Default Mode</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.defaultMode')"></span>
                                                                        <span class="value"> : {{ vol.secret.defaultMode }}</span>
                                                                    </li>
                                                                    <li v-if="vol.secret.hasOwnProperty('items') && vol.secret.items.length">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Items</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.items')"></span>
                                                                        <ul>
                                                                            <template v-for="(item, index) in vol.secret.items">
                                                                                <li :key="index">
                                                                                    <button class="toggleSummary"></button>
                                                                                    <strong class="label">Item #{{ index + 1 }}</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.items.items')"></span>
                                                                                    <ul>
                                                                                        <li v-if="item.hasOwnProperty('key') && !isNull(item.key)">
                                                                                            <strong class="label">Key:</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.items.items.properties.key')"></span>
                                                                                            <span class="value"> : {{ item.key }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('mode') && !isNull(item.mode)">
                                                                                            <strong class="label">Mode</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.items.items.properties.mode')"></span>
                                                                                            <span class="value"> : {{ item.mode }}</span>
                                                                                        </li>
                                                                                        <li v-if="item.hasOwnProperty('path') && !isNull(item.path)">
                                                                                            <strong class="label">Path</strong>
                                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.items.items.properties.path')"></span>
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

                                        <li v-if="cluster.data.spec.pods.hasOwnProperty('customInitContainers') && !isNull(cluster.data.spec.pods.customInitContainers)">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Init Containers</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers')"></span>

                                            <ul>
                                                <template v-for="(container, index) in cluster.data.spec.pods.customInitContainers">
                                                    <li :key="'container-' + index">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Init Container #{{ index + 1 }}</strong>
                                                        <ul>
                                                            <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                                <strong class="label">Name</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.name')"></span>
                                                                <span class="value"> : {{ container.name }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                                <strong class="label">Image</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.image')"></span>
                                                                <span class="value"> : {{ container.image }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                                <strong class="label">Image Pull Policy</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.imagePullPolicy')"></span>
                                                                <span class="value"> : {{ container.imagePullPolicy }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                                <strong class="label">Working Directory</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.workingDir')"></span>
                                                                <span class="value"> : {{ container.workingDir }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Arguments</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.arguments')"></span>
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
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.command')"></span>
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
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.env')"></span>
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
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports')"></span>
                                                                <ul>
                                                                    <template v-for="(port, pIndex) in container.ports">
                                                                        <li :key="'port-' + index + '-' + pIndex">
                                                                            <button class="toggleSummary"></button>
                                                                            <strong class="label">Port #{{ pIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ port.name }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                                    <strong class="label">Host IP</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.hostIP')"></span>
                                                                                    <span class="value"> : {{ port.hostIP }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                                    <strong class="label">Host Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.hostPort')"></span>
                                                                                    <span class="value"> : {{ port.hostPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                                    <strong class="label">Container Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.containerPort')"></span>
                                                                                    <span class="value"> : {{ port.containerPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                                    <strong class="label">Protocol</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.protocol')"></span>
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
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts')"></span>
                                                                <ul>
                                                                    <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                        <li :key="'vol-' + index + '-' + vIndex">
                                                                            <strong class="label">Volume #{{ vIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ vol.name }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('readOnly')">
                                                                                    <strong class="label">Read Only</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                                    <span class="value"> : {{ isEnabled(vol.readOnly) }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                                    <strong class="label">Mount Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                                    <span class="value"> : {{ vol.mountPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                                    <strong class="label">Mount Propagation</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                                    <span class="value"> : {{ vol.mountPropagation }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                                    <strong class="label">Sub Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.subPath')"></span>
                                                                                    <span class="value"> : {{ vol.subPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                                    <strong class="label">Sub Path Expr</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.subPathExpr')"></span>
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

                                        <li v-if="cluster.data.spec.pods.hasOwnProperty('customContainers') && !isNull(cluster.data.spec.pods.customContainers)">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Containers</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers')"></span>
                                            <ul>
                                                <template v-for="(container, index) in cluster.data.spec.pods.customContainers">
                                                    <li :key="'container-' + index">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Container #{{ index + 1 }}</strong>
                                                        <ul>
                                                            <li v-if="container.hasOwnProperty('name') && !isNull(container.name)">
                                                                <strong class="label">Name</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.name')"></span>
                                                                <span class="value"> : {{ container.name }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('image') && !isNull(container.image)">
                                                                <strong class="label">Image</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.image')"></span>
                                                                <span class="value"> : {{ container.image }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('imagePullPolicy') && !isNull(container.imagePullPolicy)">
                                                                <strong class="label">Image Pull Policy</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.imagePullPolicy')"></span>
                                                                <span class="value"> : {{ container.imagePullPolicy }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('workingDir') && !isNull(container.workingDir)">
                                                                <strong class="label">Working Directory</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.workingDir')"></span>
                                                                <span class="value"> : {{ container.workingDir }}</span>
                                                            </li>
                                                            <li v-if="container.hasOwnProperty('args') && !isNull(container.args)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Arguments</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.arguments')"></span>
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
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.command')"></span>
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
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.env')"></span>
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
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports')"></span>
                                                                <ul>
                                                                    <template v-for="(port, pIndex) in container.ports">
                                                                        <li :key="'port-' + index + '-' + pIndex">
                                                                            <strong class="label">Port #{{ pIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="port.hasOwnProperty('name') && !isNull(port.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ port.name }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostIP') && !isNull(port.hostIP)">
                                                                                    <strong class="label">Host IP</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.hostIP')"></span>
                                                                                    <span class="value"> : {{ port.hostIP }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('hostPort') && !isNull(port.hostPort)">
                                                                                    <strong class="label">Host Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.hostPort')"></span>
                                                                                    <span class="value"> : {{ port.hostPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('containerPort') && !isNull(port.containerPort)">
                                                                                    <strong class="label">Container Port</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.containerPort')"></span>
                                                                                    <span class="value"> : {{ port.containerPort }}</span>
                                                                                </li>
                                                                                <li v-if="port.hasOwnProperty('protocol') && !isNull(port.protocol)">
                                                                                    <strong class="label">Protocol</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.protocol')"></span>
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
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts')"></span>
                                                                <ul>
                                                                    <template v-for="(vol, vIndex) in container.volumeMounts">
                                                                        <li :key="'vol-' + index + '-' + vIndex">
                                                                            <strong class="label">Volume #{{ vIndex + 1 }}</strong>
                                                                            <ul>
                                                                                <li v-if="vol.hasOwnProperty('name') && !isNull(vol.name)">
                                                                                    <strong class="label">Name</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.name')"></span>
                                                                                    <span class="value"> : {{ vol.name }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('readOnly')">
                                                                                    <strong class="label">Read Only</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                                    <span class="value"> : {{ isEnabled(vol.readOnly) }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPath') && !isNull(vol.mountPath)">
                                                                                    <strong class="label">Mount Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                                    <span class="value"> : {{ vol.mountPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('mountPropagation') && !isNull(vol.mountPropagation)">
                                                                                    <strong class="label">Mount Propagation</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                                    <span class="value"> : {{ vol.mountPropagation }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPath') && !isNull(vol.subPath)">
                                                                                    <strong class="label">Sub Path</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.subPath')"></span>
                                                                                    <span class="value"> : {{ vol.subPath }}</span>
                                                                                </li>
                                                                                <li v-if="vol.hasOwnProperty('subPathExpr') && !isNull(vol.subPathExpr)">
                                                                                    <strong class="label">Sub Path Expr</strong>
                                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.subPathExpr')"></span>
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

                    <ul class="section" v-if="( showDefaults || ( (cluster.data.spec.replication.role != 'ha-read') || (cluster.data.spec.replication.mode != 'async') || cluster.data.spec.replication.hasOwnProperty('groups') ) )">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Pods Replication </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication')"></span>
                            <ul>
                                <li v-if="(showDefaults || (cluster.data.spec.replication.role != 'ha-read') )">
                                    <strong class="label">Role</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.role')"></span>
                                    <span class="value"> : {{ cluster.data.spec.replication.role }}</span>
                                </li>
                                <li v-if="(showDefaults || (cluster.data.spec.replication.mode != 'async') )">
                                    <strong class="label">Mode</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.mode')"></span>
                                    <span class="value"> : {{ cluster.data.spec.replication.mode }}</span>
                                </li>
                                <li v-if="cluster.data.spec.replication.hasOwnProperty('syncNodeCount')">
                                    <strong class="label">Sync Node Count</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.syncInstances')"></span>
                                    <span class="value"> : {{ cluster.data.spec.replication.syncNodeCount }}</span>
                                </li>
                                <li v-if="cluster.data.spec.replication.hasOwnProperty('groups')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Groups</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.groups')"></span>
                                    <ul>
                                        <li v-for="(group, index) in cluster.data.spec.replication.groups">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Group #{{ index + 1}}</strong>
                                            <ul>
                                                <li v-if="group.name.length">
                                                    <strong class="label">Name</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.groups.items.properties.name')"></span>
                                                    <span class="value"> : {{ group.name }}</span>
                                                </li>
                                                <li v-if="showDefaults || (group.role != 'ha-read')">
                                                    <strong class="label">Role</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.groups.items.properties.role')"></span>
                                                    <span class="value"> : {{ group.role }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Instances</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.groups.items.properties.instances')"></span>
                                                    <span class="value"> : {{ group.instances }}</span>
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
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Customize generated Kubernetes service </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices')"></span>
                            <ul>
                                <li v-if="showDefaults || !cluster.data.spec.postgresServices.primary.enabled || (cluster.data.spec.postgresServices.primary.type != 'ClusterIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('customPorts')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Primary Service</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary')"></span>
                                    <span class="value"> : {{ isEnabled(cluster.data.spec.postgresServices.primary.enabled) }}</span>
                                    <ul v-if="( showDefaults || (cluster.data.spec.postgresServices.primary.type != 'ClusterIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.primary.hasOwnProperty('customPorts'))">
                                        <li v-if="showDefaults">
                                            <strong class="label">Name</strong>
                                            <span class="helpTooltip" data-tooltip="Name of the -primary service."></span>
                                            <span class="value"> : {{ cluster.data.metadata.name }}-primary.{{cluster.data.metadata.namespace}}</span>	
                                        </li>
                                        <li v-if="( showDefaults || (cluster.data.spec.postgresServices.primary.type != 'ClusterIP') )">
                                            <strong class="label">Type</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.type')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.primary.type }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP')">
                                            <strong class="label">Load Balancer IP</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.loadBalancerIP')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.primary.loadBalancerIP }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.primary.hasOwnProperty('customPorts')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Ports</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts')"></span>
                                            <ul>
                                                <li v-for="(port, index) in cluster.data.spec.postgresServices.primary.customPorts">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Port #{{ index + 1 }}</strong>
                                                    <ul>
                                                        <li v-if="port.hasOwnProperty('appProtocol') && (port.appProtocol != null)">
                                                            <strong class="label">Application Protocol</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.appProtocol')"></span>
                                                            <span class="value"> : {{ port.appProtocol }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('name') && (port.name != null)">
                                                            <strong class="label">Name:</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.name')"></span>
                                                            <span class="value"> : {{ port.name }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('nodePort') && (port.nodePort != null)">
                                                            <strong class="label">Node Port:</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.nodePort')"></span>
                                                            <span class="value"> : {{ port.nodePort }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('port')">
                                                            <strong class="label">Port:</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.port')"></span>
                                                            <span class="value"> : {{ port.port }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('protocol') && (port.protocol != null)">
                                                            <strong class="label">Protocol:</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.protocol')"></span>
                                                            <span class="value"> : {{ port.protocol }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('targetPort') && (port.targetPort != null)">
                                                            <strong class="label">Target Port:</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.targetPort')"></span>
                                                            <span class="value"> : {{ port.targetPort }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="showDefaults || !cluster.data.spec.postgresServices.replicas.enabled || (cluster.data.spec.postgresServices.replicas.type != 'ClusterIP') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('customPorts')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Replicas Service</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas')"></span>
                                    <span> : {{ isEnabled(cluster.data.spec.postgresServices.replicas.enabled) }}</span>
                                    <ul v-if="( showDefaults || (cluster.data.spec.postgresServices.replicas.type != 'ClusterIP') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP') || cluster.data.spec.postgresServices.replicas.hasOwnProperty('customPorts'))">
                                       <li v-if="showDefaults">
                                            <strong class="label">Name</strong>
                                            <span class="helpTooltip" data-tooltip="Name of the -replicas service."></span>
                                            <span class="value"> : {{ cluster.data.metadata.name }}-replicas.{{cluster.data.metadata.namespace}}</span>	
                                        </li>
                                        <li v-if="( showDefaults || (cluster.data.spec.postgresServices.replicas.type != 'ClusterIP') )">
                                            <strong class="label">Type</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.type')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.replicas.type }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP')">
                                            <strong class="label">Load Balancer IP</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.loadBalancerIP')"></span>
                                            <span class="value"> : {{ cluster.data.spec.postgresServices.replicas.loadBalancerIP }}</span>
                                        </li>
                                        <li v-if="cluster.data.spec.postgresServices.replicas.hasOwnProperty('customPorts')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Custom Ports</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts')"></span>
                                            <ul>
                                                <li v-for="(port, index) in cluster.data.spec.postgresServices.replicas.customPorts">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Port #{{ index + 1 }}</strong>
                                                    <ul>
                                                        <li v-if="port.hasOwnProperty('appProtocol') && (port.appProtocol != null)">
                                                            <strong class="label">Application Protocol:</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.appProtocol')"></span>
                                                            <span class="value">{{ port.appProtocol }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('name') && (port.name != null)">
                                                            <strong class="label">Name</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.name')"></span>
                                                            <span class="value"> : {{ port.name }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('nodePort') && (port.nodePort != null)">
                                                            <strong class="label">Node Port</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.nodePort')"></span>
                                                            <span class="value"> : {{ port.nodePort }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('port')">
                                                            <strong class="label">Port</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.port')"></span>
                                                            <span class="value"> : {{ port.port }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('protocol') && (port.protocol != null)">
                                                            <strong class="label">Protocol</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.protocol')"></span>
                                                            <span class="value"> : {{ port.protocol }}</span>
                                                        </li>
                                                        <li v-if="port.hasOwnProperty('targetPort') && (port.targetPort != null)">
                                                            <strong class="label">Target Port</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.appPrototargetPortcol')"></span>
                                                            <span class="value"> : {{ port.targetPort }}</span>
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
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Metadata </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata')"></span>
                            <ul>
                                <li v-if="hasProp(cluster, 'data.spec.pods.metadata')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Labels</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.labels')"></span>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.labels.clusterPods')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Cluster Pods</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.labels.clusterPods')"></span>
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
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations')"></span>
                                    <ul>
                                        <li v-if="hasProp(cluster, 'data.spec.metadata.annotations.allResources')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">All Resources</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.allResources')"></span>
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
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.clusterPods')"></span>
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
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.services')"></span>
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
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.primaryService')"></span>
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
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.replicasService')"></span>
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
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Pods Scheduling </strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling')"></span>
                            <ul>
                                <li v-if="hasProp(cluster, 'data.spec.pods.scheduling.nodeSelector')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Selectors</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeSelector')"></span>
                                    <ul>
                                        <li v-for="(value, key) in cluster.data.spec.pods.scheduling.nodeSelector">
                                            <strong class="label">{{ key }}</strong>
                                            <span class="value"> : {{ value }}</span>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="hasProp(cluster, 'data.spec.pods.scheduling.tolerations')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Tolerations</strong>                                    
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations')"></span>
                                    <ul>
                                        <li v-for="(toleration, index) in cluster.data.spec.pods.scheduling.tolerations">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Toleration #{{ index+1 }}</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">Key</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.key')"></span>
                                                    <span class="value"> : {{ toleration.key }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Operator</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.operator')"></span>
                                                    <span class="value"> : {{ toleration.operator }}</span>
                                                </li>
                                                <li v-if="toleration.hasOwnProperty('value')">
                                                    <strong class="label">Value</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.value')"></span>
                                                    <span class="value"> : {{ toleration.value }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Effect</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.effect')"></span>
                                                    <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                                </li>
                                                <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                                    <strong class="label">Toleration Seconds</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                                    <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>

                                <li v-if="hasProp(cluster, 'data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Affinity</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                                    <span>Required During Scheduling Ignored During Execution</span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Terms</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items')"></span>
                                            <ul>
                                                <li v-for="(term, index) in cluster.data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Term #{{ index+1 }}</strong>
                                                    <ul>
                                                        <li v-if="term.hasOwnProperty('matchExpressions')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Expressions</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span>
                                                            <ul>
                                                                <li v-for="(exp, index) in term.matchExpressions">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Expression #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span>
                                                                            <span class="value"> : {{ exp.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ exp.operator }}</span>
                                                                        </li>
                                                                        <li v-if="exp.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                        <li v-if="term.hasOwnProperty('matchFields')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Fields</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span>
                                                            <ul>
                                                                <li v-for="(field, index) in term.matchFields">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Field #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span>
                                                                            <span class="value"> : {{ field.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ field.operator }}</span>
                                                                        </li>
                                                                        <li v-if="field.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span>
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

                                <li v-if="hasProp(cluster, 'data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">
                                    <button class="toggleSummary"></button>
                                    <strong class="label">Node Affinity</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                                    <span>Preferred During Scheduling Ignored During Execution</span>
                                    <ul>
                                        <li>
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Terms</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span>
                                            <ul>
                                                <li v-for="(term, index) in cluster.data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Term #{{ index+1 }}</strong>
                                                    <ul>
                                                        <li v-if="term.preference.hasOwnProperty('matchExpressions')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Expressions</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                            <ul>
                                                                <li v-for="(exp, index) in term.preference.matchExpressions">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Expression #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                            <span class="value"> : {{ exp.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ exp.operator }}</span>
                                                                        </li>
                                                                        <li v-if="exp.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                        <li v-if="term.preference.hasOwnProperty('matchFields')">
                                                            <button class="toggleSummary"></button>
                                                            <strong class="label">Match Fields</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                            <ul>
                                                                <li v-for="(field, index) in term.preference.matchFields">
                                                                    <button class="toggleSummary"></button>
                                                                    <strong class="label">Field #{{ index+1 }}</strong>
                                                                    <ul>
                                                                        <li>
                                                                            <strong class="label">Key</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                            <span class="value"> : {{ field.key }}</span>
                                                                        </li>
                                                                        <li>
                                                                            <strong class="label">Operator</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                            <span class="value"> : {{ field.operator }}</span>
                                                                        </li>
                                                                        <li v-if="field.hasOwnProperty('values')">
                                                                            <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                            <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                        </li>
                                                                    </ul>
                                                                </li>
                                                            </ul>
                                                        </li>
                                                        <li v-if="term.hasOwnProperty('weight')">
                                                            <strong class="label">Weight</strong>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
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

                    <ul class="section" v-if="showDefaults || (hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') && (cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity != null))">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="sectionTitle">Non Production Settings </strong>
                            <ul>
                                <li>
                                    <strong class="label">Cluster Pod Anti Affinity</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.nonProductionOptions.disableClusterPodAntiAffinity').replace('Set this property to true to allow','When disabled, it allows running')"></span>
                                    <span> : {{ (hasProp(cluster, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') && (cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity != null)) ? isEnabled(cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity, true) : 'Default'}}</span>
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

        props: {
            cluster: Object,
            extensionsList: Array,
            details: {
                type: Boolean,
                default: false
            }
        },

        data() {
            return {
                showDefaults: this.details,
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

    .summary strong.label {
        display: inline-block;
        margin-right: 7px;
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