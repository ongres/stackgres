<template>
    <div id="create-sharded-cluster" class="createShardedCluster noSubmit" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(cluster).length > 0"></template>

        <form id="createShardedCluster" class="form" :key="formHash" @submit.prevent v-if="!editMode || editReady">
            <div class="header stickyHeader">
                <h2>
                    <span>{{ editMode ? 'Edit' :  'Create' }} Sharded Cluster</span>
                </h2>
                <label for="advancedMode" class="floatRight">
                    <span>ADVANCED OPTIONS </span>
                    <input type="checkbox" id="advancedMode" name="advancedMode" v-model="advancedMode" class="switch" @change="( (!advancedMode && (currentStepIndex > 2)) && (currentStep = formSteps[0]))">
                </label>
            </div>
            
            <ul class="tabs">
                <template v-for="section in formSections">
                    <li :class="[section, ((currentSection == section) && 'active')]" @click="currentSection = section">
                        <a class="capitalize">{{ section }}</a>
                    </li>
                </template>
            </ul>
            <template v-if="(currentSection == 'overrides')">
                <template v-if="shards.overrides.length">
                    <ul class="tabs">
                        <template v-for="(override, index) in shards.overrides">
                            <li :class="['override-' + index, ( (index == overrideIndex) && 'active' )]">
                                <a @click="overrideIndex = index">
                                    Override #{{ index }}
                                    <button
                                        type="button"
                                        class="icon delete plain"
                                        @click="overrideIndex -= 1; spliceArray(shards.overrides, overrideIndex)"
                                    >
                                    </button>
                                </a>
                            </li>
                        </template>
                        <a
                            class="plain floatRight"
                            @click="(shards.overrides.length < shards.clusters) && pushOverride()"
                            :disabled="(shards.overrides.length >= shards.clusters)"
                            :title="
                                (shards.overrides.length >= shards.clusters) && 
                                'You cannot set more overrides than the amount of clusters you have defined'
                            "
                        >
                            Add Override
                        </a>
                    </ul>
                </template>
                <template v-else>
                    <span class="warning textCenter marginTop">
                        <br/>
                        No overrides have been defined yet<br/>
                        <button id="addOverride" type="button" class="btn border" @click="pushOverride()">
                            Add Override
                        </button>
                        <br/><br/>
                    </span>
                </template>

            </template>
            
            <template v-if="( (currentSection !== 'overrides') || shards.overrides.length )">
                <div class="stepsContainer">
                    <ul class="steps">
                        <button type="button" class="btn arrow prev" @click="currentStep[currentSection] = formSteps[currentSection][(currentStepIndex - 1)]" :disabled="( currentStepIndex == 0 )"></button>
                
                        <template v-for="(step, index) in formSteps[currentSection]"  v-if="( ((index < basicSteps[currentSection]) && !advancedMode) || advancedMode)">
                            <li @click="currentStep[currentSection] = step; checkValidSteps(_data, 'steps')" :class="[( (currentStep[currentSection] == step) && 'active'), ( (index < basicSteps[currentSection]) && 'basic' ), (errorStep.includes(currentSection + '.' + step) && 'notValid')]" :data-step="currentSection + '.' + step">
                                {{ step }}
                            </li>
                        </template>

                        <button type="button" class="btn arrow next" @click="currentStep[currentSection] = formSteps[currentSection][(currentStepIndex + 1)]" :disabled="(!advancedMode && ( currentStepIndex == 2 ) ) || ( (advancedMode && ( currentStepIndex == (formSteps[currentSection].length - 1) )) )"></button>
                    </ul>
                </div>
            </template>

            <template v-if="currentSection == 'general'">
                <fieldset v-if="(currentStep.general == 'cluster')" class="step active" data-fieldset="general.cluster">
                    <div class="header">
                        <h2>General Information</h2>
                    </div>

                    <div class="fields">
                        <div class="row-50">
                            <div class="col">
                                <label for="metadata.name">Sharded Cluster Name <span class="req">*</span></label>
                                <input v-model="name" :disabled="editMode" required data-field="metadata.name" autocomplete="off">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.metadata.name')"></span>
                            </div>

                            <span class="warning topAnchor" v-if="nameColission && !editMode">
                                There's already a <strong>SGShardedCluster</strong> with the same name on this namespace. Please specify a different name or create the resource on another namespace
                            </span>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Sharding</h3><br/>
                            
                            <div class="col">
                                <label for="spec.database">Database <span class="req">*</span></label>
                                <input v-model="database" :disabled="editMode" required data-field="spec.database" autocomplete="off">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.database')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.type">Type <span class="req">*</span></label>
                                <select v-model="shardingType" disabled required data-field="spec.type">
                                    <option :value="nullVal">Choose one...</option>
                                    <option value="citus">Citus</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.type')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Postgres</h3><br/>

                            <div class="col">                    
                                <div class="versionContainer">
                                    <label for="spec.postgres.version">Postgres Version <span class="req">*</span></label>
                                    <ul class="select" id="postgresVersion" data-field="spec.postgres.version" tabindex="0">
                                        <li class="selected">
                                            {{ (postgresVersion == 'latest') ? 'Latest' : 'Postgres '+postgresVersion }}
                                        </li>
                                        <li>
                                            <a @click="setVersion('latest')" data-val="latest" class="active">Latest</a>
                                        </li>

                                        <li v-for="version in Object.keys(postgresVersionsList[flavor]).reverse()">
                                            <strong>Postgres {{ version }}</strong>
                                            <ul>
                                                <li>
                                                    <a @click="setVersion(version)" :data-val="version">Postgres {{ version }} (Latest)</a>
                                                </li>
                                                <li v-for="minorVersion in postgresVersionsList[flavor][version]">
                                                    <a @click="setVersion(minorVersion)" :data-val="minorVersion">Postgres {{ minorVersion }}</a>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.version')"></span>

                                    <input v-model="postgresVersion" required class="hide">
                                </div>
                            </div>
                        </div>

                        <div class="row-50">
                            <h3>SSL Connections</h3>
                            <p>
                                By default, support for SSL connections to Postgres is disabled, to enable it configure this section. SSL connections will be handled by Envoy using Postgres filter’s SSL termination.
                            </p>
                            <div class="col">
                                <label>SSL Connections</label>  
                                <label for="enableSSL" class="switch yes-no">
                                    Enable
                                    <input type="checkbox" id="enableSSL" v-model="ssl.enabled" data-switch="YES" data-field="spec.postgres.ssl.enabled">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.enabled')"></span>
                            </div>
                        </div>
                        <div class="row-50" v-if="ssl.enabled && (Object.keys(ssl).length > 1)">
                            <div class="col">
                                <label for="spec.postgres.ssl.certificateSecretKeySelector.name">
                                    SSL Certificate Secret Name
                                </label>
                                <input v-model="ssl.certificateSecretKeySelector.name" data-field="spec.postgres.ssl.certificateSecretKeySelector.name" autocomplete="off">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.certificateSecretKeySelector.name')"></span>
                            </div>
                            <div class="col">
                                <label for="spec.postgres.ssl.certificateSecretKeySelector.key">
                                    SSL Certificate Secret Key
                                </label>
                                <input v-model="ssl.certificateSecretKeySelector.key" data-field="spec.postgres.ssl.certificateSecretKeySelector.key" autocomplete="off">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.certificateSecretKeySelector.key')"></span>
                            </div>
                            <div class="col">
                                <label for="spec.postgres.ssl.privateKeySecretKeySelector.name">
                                    SSL Private Key Secret Name
                                </label>
                                <input v-model="ssl.privateKeySecretKeySelector.name" data-field="spec.postgres.ssl.privateKeySecretKeySelector.name" autocomplete="off">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.privateKeySecretKeySelector.name')"></span>
                            </div>
                            <div class="col">
                                <label for="spec.postgres.ssl.privateKeySecretKeySelector.key">
                                    SSL Private Key Secret Key
                                </label>
                                <input v-model="ssl.privateKeySecretKeySelector.key" data-field="spec.postgres.ssl.privateKeySecretKeySelector.key" autocomplete="off">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.ssl.privateKeySecretKeySelector.key')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Monitoring</h3>
                            <p>
                                By enabling Monitoring, you are activating metrics scrapping via service monitors, which is done by enabling both, Prometheus Autobind and Metrics Exporter.
                            </p>
                            <div class="col">
                                <label>Monitoring</label>  
                                <label for="enableMonitoring" class="switch yes-no">Enable<input type="checkbox" id="enableMonitoring" v-model="enableMonitoring" data-switch="YES" @change="toggleMonitoring()"></label>
                                <span class="helpTooltip" data-tooltip="StackGres supports enabling automatic monitoring for your Postgres cluster, but you need to provide or install the <a href='https://stackgres.io/doc/latest/install/prerequisites/monitoring/' target='_blank'>Prometheus stack as a pre-requisite</a>. Then, check this option to configure automatically sending metrics to the Prometheus stack."></span>
                            </div>                  
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>
                                Distributed Logs
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs')"></span>
                            </h3>
                            <p>Send Postgres and Patroni logs to a central <a href="https://stackgres.io/doc/latest/reference/crd/sgdistributedlogs/" target="_blank">SGDistributedLogs</a> instance. Optional: if not enabled, logs are sent to the standard output.</p>

                            <div class="col">
                                <label for="spec.distributedLogs.sgDistributedLogs">Logs Cluster</label>
                                <select v-model="distributedLogs" class="distributedLogs" data-field="spec.distributedLogs.sgDistributedLogs" @change="(distributedLogs == 'createNewResource') && createNewResource('sgdistributedlogs')" :set="( (distributedLogs == 'createNewResource') && (distributedLogs = '') )">
                                    <option value="">Select Logs Server</option>
                                    <option v-for="cluster in logsClusters" :value="( (cluster.data.metadata.namespace !== $route.params.namespace) ? cluster.data.metadata.namespace + '.' : '') + cluster.data.metadata.name">{{ cluster.data.metadata.name }}</option>
                                    <template v-if="iCan('create', 'sgdistributedlogs', $route.params.namespace)">
                                        <option :value="nullVal" disabled>– OR –</option>
                                        <option value="createNewResource">Create new logs server</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.sgDistributedLogs')"></span>
                            </div>

                            <div class="col" v-if="( (typeof distributedLogs !== 'undefined') && distributedLogs.length)">
                                <label for="spec.distributedLogs.retention">Retention</label>
                                <input v-model="retention" data-field="spec.distributedLogs.retention" autocomplete="off">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.retention')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.general == 'extensions')" class="step active" data-fieldset="general.extensions">
                    <div class="header">
                        <h2>Postgres Extensions <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgres.extensions')"></span></h2>
                    </div>
                    
                    <div class="fields">
                        <div class="toolbar">
                            <div class="searchBar">
                                <label for="keyword">Search Extensions</label>
                                <input id="keyword" v-model="searchExtension" class="search" placeholder="Enter text..." autocomplete="off" data-field="spec.postgres.extensions">
                                <a @click="clearExtFilters()" class="btn clear border keyword" v-if="searchExtension.length">CLEAR</a>
                            </div>
                            <div class="extLicense">
                                <label for="extLicense">Extensions Licenses</label>
                                <select v-model="extLicense" id="extLicense">
                                    <option value="opensource">Open Source (OSS/OSI)</option>
                                    <option value="nonopensource">Non Open Source</option>
                                </select>
                            </div>
                        </div>

                        <p class="warning" v-if="(extLicense == 'nonopensource')">
                            The extensions listed below are not open source. Please check licensing details with the creators of the extensions.
                        </p>
                        
                        <div class="extHead">
                            <span class="install">Install</span>
                            <span class="name">Name</span>
                            <span class="version">Version</span>
                            <span class="description">Description</span>
                        </div>
                        <ul class="extensionsList">
                            <li class="extension notFound">
                                {{ searchExtension.length ? 'No extensions match your search terms...' : 'No extensions available for the postgres specs you selected...' }}
                            </li>
                            <li v-for="(ext, index) in extensionsList[flavor][postgresVersion]" 
                                v-if="( ( (extLicense == 'opensource') && (ext.name != 'timescaledb_tsl') ) || ( (extLicense == 'nonopensource') && (ext.name == 'timescaledb_tsl') ) ) && (!searchExtension.length || (ext.name+ext.description+ext.tags.toString()).includes(searchExtension)) && ext.versions.length" 
                                class="extension" 
                                :class="( (viewExtension == index) && 'show')">
                                <label><input type="checkbox" class="plain enableExtension" @change="setExtension(index)" :checked="(extIsSet(ext.name) !== -1)" :disabled="!ext.versions.length || !ext.selectedVersion.length" :data-field="'spec.postgres.extensions.' + ext.name" /></label>
                                
                                <span class="extInfo" @click.stop.prevent="viewExt(index)">
                                    <span class="hasTooltip extName">
                                        <span class="name">
                                            <span>{{ ext.name }}</span>
                                            <a v-if="ext.hasOwnProperty('url') && ext.url" :href="ext.url" class="newTab" target="_blank"></a>
                                        </span>
                                    </span>
                                    <span class="version">
                                        <select v-model="ext.selectedVersion" class="extVersion" @change="updateExtVersion(ext.name, ext.selectedVersion)">
                                            <option v-if="!ext.versions.length" selected>Not available for this postgres version</option>
                                            <option v-else value="">Select version...</option>
                                            <option v-for="v in ext.versions">{{ v }}</option>
                                        </select>
                                    </span>
                                    <span class="hasTooltip">
                                        <span class="description firstLetter">
                                            <span>{{ ext.abstract }}</span>
                                        </span>
                                    </span>
                                
                                    <button type="button" class="textBtn anchor toggleExt">-</button>
                                </span>

                                <div v-if="(viewExtension == index)" class="extDetails">
                                    <div class="header">
                                        <h3>{{ ext.name }}</h3>
                                    </div>
                                    <div class="description">
                                        {{ ext.description }}
                                    </div>
                                    <div class="header">
                                        <h3>Tags</h3>
                                    </div>
                                    <div class="tags" v-if="ext.tags.length">
                                        <span v-for="tag in ext.tags" class="extTag">
                                            {{ tag }}
                                        </span>
                                    </div>

                                    <div class="header">
                                        <h3>Source:</h3>
                                    </div>
                                    <a :href="ext.source" target="_blank">{{ ext.source }}</a>
                                </div>
                            </li>
                        </ul>
                    </div>
                    <div id="nameTooltip">
                        <div class="info"></div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.general == 'backups')" class="step active" data-fieldset="general.backups">
                    <div class="header">
                        <h2>Backups</h2>
                    </div>

                    <div class="fields">                        
                        <div class="row-50">
                            <div class="col">
                                <label>Managed Backups</label>  
                                <label for="managedBackups" class="switch yes-no" data-field="spec.configurations.backups">Enable<input type="checkbox" id="managedBackups" v-model="managedBackups" data-switch="YES"></label>
                                <span class="helpTooltip" data-tooltip="If enabled, allows specifying backup configurations to automate periodical backups"></span>
                            </div>

                            <div class="col" v-if="managedBackups">
                                <label for="spec.configurations.backups.sgObjectStorage">Object Storage <span class="req">*</span></label>

                                <select 
                                    v-model="backups[0].sgObjectStorage" 
                                    data-field="spec.configurations.backups.sgObjectStorage"
                                    @change="(backups[0].sgObjectStorage == 'createNewResource') && createNewResource('sgobjectstorages')"
                                    required
                                >
                                    <option value="" disabled>{{ sgobjectstorages.length ? 'Select Storage' : 'No object storage available' }}</option>
                                    <option v-for="storage in sgobjectstorages" v-if="storage.data.metadata.namespace == namespace">{{ storage.name }}</option>
                                    <template v-if="iCan('create', 'sgobjectstorages', $route.params.namespace)">
                                        <option value="" disabled v-if="sgobjectstorages.length">– OR –</option>
                                        <option value="createNewResource">Create new object storage</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.sgObjectStorage')"></span>
                            </div>
                        </div>

                        <template v-if="managedBackups">
                        
                            <hr/>
                    
                            <h4 for="spec.configurations.backups.cronSchedule">
                                Backup Schedule 
                                <span class="req">*</span>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.cronSchedule')"></span>
                            </h4><br/>

                            <div class="flex-center cron" data-field="spec.configurations.backups.cronSchedule">
                                <div class="col">
                                    <label for="backupConfigFullScheduleMin" title="Minute *">Minute <span class="req">*</span></label>
                                    <input v-model="cronSchedule[0].min" required id="backupConfigFullScheduleMin" @change="updateCronSchedule(0)" data-tzdep="true">
                                </div>

                                <div class="col">
                                    <label for="backupConfigFullScheduleHour" title="Hour *">Hour <span class="req">*</span></label>
                                    <input v-model="cronSchedule[0].hour" required id="backupConfigFullScheduleHour" @change="updateCronSchedule(0)" data-tzdep="true">
                                </div>

                                <div class="col">
                                    <label for="backupConfigFullScheduleDOM" title="Day of Month *">Day of Month <span class="req">*</span></label>
                                    <input v-model="cronSchedule[0].dom" required id="backupConfigFullScheduleDOM" @change="updateCronSchedule(0)" data-tzdep="true">
                                </div>

                                <div class="col">
                                    <label for="backupConfigFullScheduleMonth" title="Month *">Month <span class="req">*</span></label>
                                    <input v-model="cronSchedule[0].month" required id="backupConfigFullScheduleMonth" @change="updateCronSchedule(0)" data-tzdep="true">
                                </div>

                                <div class="col">
                                    <label for="backupConfigFullScheduleDOW" title="Day of Week *">Day of Week <span class="req">*</span></label>
                                    <input v-model="cronSchedule[0].dow" required id="backupConfigFullScheduleDOW" @change="updateCronSchedule(0)" data-tzdep="true">
                                </div>
                            </div>
                            <br/>
                            <div class="warning">
                                <strong>That is: </strong>
                                {{ tzCrontab(backups[0].cronSchedule) | prettyCRON(false) }}
                            </div>                    

                            <hr/>
                            
                            <div class="row-50">
                                <h3>Base Backup Details</h3>

                                <div class="col">
                                    <label for="spec.configurations.backups.retention">Retention Window (max. number of base backups)</label>
                                    <input v-model="backups[0].retention" data-field="spec.configurations.backups.retention" type="number">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.retention')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.configurations.backups.compression">Compression Method</label>
                                    <select v-model="backups[0].compression" data-field="spec.configurations.backups.compression">
                                        <option disabled value="">Select a method</option>
                                        <option value="lz4">LZ4</option>
                                        <option value="lzma">LZMA</option>
                                        <option value="brotli">Brotli</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.compression')"></span>
                                </div>
                            </div>

                             <div class="repeater">
                                <fieldset data-field="spec.configurations.backups.paths">
                                    <div class="header" :class="(!backups[0].paths.length && 'noMargin')">
                                        <h3 for="spec.configurations.backups.paths">
                                            Paths
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.paths')"></span>
                                        </h3>
                                    </div>
                                    <template v-if="backups[0].paths.length">
                                        <div class="row" v-for="(path, index) in backups[0].paths">
                                            <label>
                                                {{ !index ? 'Coordinator Path' : 'Shard Path #' + index }}
                                            </label>
                                            
                                            <input v-model="backups[0].paths[index]" :required="!index" autocomplete="off" :data-field="'spec.configurations.backups.paths[' + index + ']'" :class="( ( index == (backups[0].paths.length - 1) ) && 'noMargin')">

                                            <a class="addRow delete topRight" @click="spliceArray(backups[0].paths, index)">Delete</a>
                                        </div>
                                    </template>
                                </fieldset>
                                <div class="fieldsetFooter">
                                    <a class="addRow" @click="backups[0].paths.push(null)">Add Path</a>
                                </div>
                            </div>
                            
                            <hr/>
                            
                            <div class="row-50">
                                <h3>Performance Details</h3>

                                <div class="col">
                                    <label for="spec.configurations.backups.performance.maxNetworkBandwidth">Max Network Bandwidth</label>
                                    <input v-model="backups[0].performance.maxNetworkBandwidth" data-field="spec.configurations.backups.performance.maxNetworkBandwidth" type="number" min="0">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.performance.properties.maxNetworkBandwidth')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.configurations.backups.performance.maxDiskBandwidth">Max Disk Bandwidth</label>
                                    <input v-model="backups[0].performance.maxDiskBandwidth" data-field="spec.configurations.backups.performance.maxDiskBandwidth" type="number" min="0">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.performance.properties.maxDiskBandwidth')"></span>
                                </div>

                                <div class="col">                
                                    <label for="spec.configurations.backups.performance.uploadDiskConcurrency">Upload Disk Concurrency</label>
                                    <input v-model="backups[0].performance.uploadDiskConcurrency" data-field="spec.configurations.backups.performance.uploadDiskConcurrency" type="number">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.configurations.backups.performance.properties.uploadDiskConcurrency')"></span>
                                </div>                    
                            </div>
                        </template>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.general == 'sidecars')" class="step active" data-fieldset="general.sidecars">
                    <div class="header">
                        <h2>Sidecars</h2>
                    </div>

                    <div class="fields">
                        <div class="row-50">
                            <h3>Monitoring</h3>
                            <p>Enable Prometheus metrics scraping via service monitors. Check the <a href="https://stackgres.io/doc/latest/install/prerequisites/monitoring/" target="_blank">Installation -> Monitoring</a> section for information on how to enable in StackGres Grafana dashboard integration.</p>

                            <div class="col">
                                <label for="spec.prometheusAutobind">Prometheus Autobind</label>  
                                <label for="prometheusAutobind" class="switch yes-no">
                                    Enable
                                    <input type="checkbox" id="prometheusAutobind" v-model="prometheusAutobind" data-switch="NO" data-field="spec.prometheusAutobind" @change="checkEnableMonitoring()">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.prometheusAutobind')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.general == 'pods-replication')" class="step active" data-fieldset="general.pods-replication">
                    <div class="header">
                        <h2>Replication</h2>
                    </div>

                    <div class="fields">                    
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.replication.mode">Mode</label>
                                <select v-model="replication.mode" required data-field="spec.replication.mode" @change="['sync', 'strict-sync'].includes(replication.mode) ? (replication['syncInstances'] = 1) : ((replication.hasOwnProperty('syncInstances') && delete replication.syncInstances) )">    
                                    <option selected>async</option>
                                    <option>sync</option>
                                    <option>strict-sync</option>
                                    <option>sync-all</option>
                                    <option>strict-sync-all</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.replication.mode')"></span>
                            </div>

                            <div class="col" v-if="['sync', 'strict-sync'].includes(replication.mode)">
                                <label for="spec.replication.syncInstances">Sync Instances</label>
                                <input type="number" min="1" :max="(shards.instancesPerCluster - 1)" v-model="replication.syncInstances" data-field="spec.replication.syncInstances">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.replication.syncInstances')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.general == 'metadata')"  class="step podsMetadata active" data-fieldset="metadata">
                    <div class="header">
                        <h2>Metadata</h2>
                    </div>

                    <div class="fields">
                        <div class="repeater">
                            <div class="header">
                                <h3 for="spec.metadata.labels">
                                    Labels
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.labels')"></span> 
                                </h3>
                            </div>

                            <fieldset data-field="spec.metadata.labels.clusterPods">
                                <div class="header" :class="!podsMetadata.length && 'noMargin noPadding'">
                                    <h3 for="spec.metadata.labels.clusterPods">
                                        Cluster Pods
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.labels.clusterPods')"></span> 
                                    </h3>
                                </div>
                                <div class="metadata" v-if="podsMetadata.length">
                                    <div class="row" v-for="(field, index) in podsMetadata">
                                        <label>Label</label>
                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(podsMetadata, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushLabel(podsMetadata)">Add Label</a>
                            </div>
                        </div>

                        <br/><br/>

                        
                        <div class="header">
                            <h3 for="spec.metadata.annotations">
                                Annotations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations')"></span>
                            </h3>
                        </div>

                        <div class="repeater">
                            <fieldset data-field="spec.metadata.annotations.allResources">
                                <div class="header" :class="!annotationsAll.length && 'noMargin noPadding'">
                                    <h3 for="spec.metadata.annotations.allResources">
                                        All Resources
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.allResources')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="annotationsAll.length">
                                    <div class="row" v-for="(field, index) in annotationsAll">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.allResources[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.allResources[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(annotationsAll, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushAnnotation(annotationsAll)">Add Annotation</a>
                            </div>
                        </div>
                        
                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.metadata.annotations.clusterPods">
                                <div class="header" :class="!annotationsPods.length && 'noMargin noPadding'">
                                    <h3 for="spec.metadata.annotations.clusterPods">
                                        Cluster Pods
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.clusterPods')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="annotationsPods.length">
                                    <div class="row" v-for="(field, index) in annotationsPods">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.clusterPods[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.clusterPods[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(annotationsPods, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushAnnotation(annotationsPods)">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.metadata.annotations.services">
                                <div class="header" :class="!annotationsServices.length && 'noMargin noPadding'">
                                    <h3 for="spec.metadata.annotations.services">
                                        Services
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.services')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="annotationsServices.length">
                                    <div class="row" v-for="(field, index) in annotationsServices">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.services[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.services[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(annotationsServices, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushAnnotation(annotationsServices)">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.metadata.annotations.primaryService">
                                <div class="header" :class="!postgresServicesPrimaryAnnotations.length && 'noMargin noPadding'">
                                    <h3 for="spec.metadata.annotations.primaryService">
                                        Primary Service 
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.primaryService')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="postgresServicesPrimaryAnnotations.length">
                                    <div class="row" v-for="(field, index) in postgresServicesPrimaryAnnotations">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.primaryService[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.primaryService[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(postgresServicesPrimaryAnnotations, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushAnnotation(postgresServicesPrimaryAnnotations)">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.metadata.annotations.replicasService">
                                <div class="header" :class="!postgresServicesReplicasAnnotations.length && 'noMargin noPadding'">
                                    <h3 for="spec.metadata.annotations.replicasService">
                                        Replicas Service
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.metadata.annotations.replicasService')"></span>
                                    </h3>
                                </div>
                                <div class="annotation repeater" v-if="postgresServicesReplicasAnnotations.length">
                                    <div class="row" v-for="(field, index) in postgresServicesReplicasAnnotations">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.replicasService[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.replicasService[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(postgresServicesReplicasAnnotations, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushAnnotation(postgresServicesReplicasAnnotations)">Add Annotation</a>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.general == 'non-production')" class="step active" data-fieldset="general.non-production">
                    <div class="header">
                        <h2>Non Production Settings</h2>
                    </div>

                    <div class="fields">
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.nonProductionOptions.disableClusterPodAntiAffinity">Cluster Pod Anti Affinity</label>  
                                <label for="disableClusterPodAntiAffinity" class="switch yes-no">
                                    Enable 
                                    <input type="checkbox" id="disableClusterPodAntiAffinity" v-model="enableClusterPodAntiAffinity" data-switch="NO" data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.nonProductionOptions.disableClusterPodAntiAffinity').replace('Set this property to true','Disable this property')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>
            </template>

            <template v-else-if="currentSection == 'coordinator'">
                <fieldset v-if="(currentStep.coordinator == 'coordinator')" class="step active" data-fieldset="coordinator.cluster">
                    <div class="header">
                        <h2>Coordinator Information</h2>
                    </div>

                    <div class="fields">
                        <div class="row-50">
                            <h3>Instances</h3>

                            <div class="col">
                                <label for="spec.coordinator.instances">Number of Instances <span class="req">*</span></label>
                                <input type="number" v-model="coordinator.instances" required data-field="spec.coordinator.instances" min="0">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.instances')"></span>
                            </div>
                            <div class="col">
                                <label for="spec.coordinator.sgInstanceProfile">Instance Profile</label>  
                                <select v-model="coordinator.sgInstanceProfile" class="resourceProfile" data-field="spec.coordinator.sgInstanceProfile" @change="(resourceProfile == 'createNewResource') && createNewResource('sginstanceprofiles')" :set="( (resourceProfile == 'createNewResource') && (resourceProfile = '') )">
                                    <option selected value="">Default (Cores: 1, RAM: 2GiB)</option>
                                    <option v-for="prof in profiles" v-if="prof.data.metadata.namespace == namespace" :value="prof.name">{{ prof.name }} (Cores: {{ prof.data.spec.cpu }}, RAM: {{ prof.data.spec.memory }}B)</option>
                                    <template v-if="iCan('create', 'sginstanceprofiles', $route.params.namespace)">
                                        <option value="" disabled>– OR –</option>
                                        <option value="createNewResource">Create new profile</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.sgInstanceProfile')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Postgres</h3><br/>

                            <div class="col">
                                <label for="spec.coordinator.configurations.sgPostgresConfig">Postgres Configuration</label>
                                <select v-model="coordinator.configurations.sgPostgresConfig" class="pgConfig" data-field="spec.configurations.sgPostgresConfig" @change="(coordinator.configurations.sgPostgresConfig == 'createNewResource') && createNewResource('sgpgconfigs')" :set="( (coordinator.configurations.sgPostgresConfig == 'createNewResource') && (coordinator.configurations.sgPostgresConfig = '') )">
                                    <option value="" selected>Default</option>
                                    <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.postgresVersion == shortPostgresVersion) )">{{ conf.name }}</option>
                                    <template v-if="iCan('create', 'sgpgconfigs', $route.params.namespace)">
                                        <option value="" disabled>– OR –</option>
                                        <option value="createNewResource">Create new configuration</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.configurations.sgPostgresConfig')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Pods Storage</h3>

                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.coordinator.pods.persistentVolume.size">Volume Size <span class="req">*</span></label>  
                                    <input v-model="coordinator.pods.persistentVolume.size.size" class="size" required data-field="spec.coordinator.pods.persistentVolume.size" type="number">
                                    <select v-model="coordinator.pods.persistentVolume.size.unit" class="unit" required data-field="spec.coordinator.pods.persistentVolume.size" >
                                        <option disabled value="">Select Unit</option>
                                        <option value="Mi">MiB</option>
                                        <option value="Gi">GiB</option>
                                        <option value="Ti">TiB</option>   
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.persistentVolume.size')"></span>
                                </div>
                            </div>

                            <div class="col">
                                <label for="spec.coordinator.pods.persistentVolume.storageClass">Storage Class</label>
                                <select v-model="coordinator.pods.persistentVolume.storageClass" data-field="spec.coordinator.pods.persistentVolume.storageClass" :disabled="!storageClasses.length">
                                    <option value=""> {{ storageClasses.length ? 'Select Storage Class' : 'No storage classes available' }}</option>
                                    <option v-for="sClass in storageClasses">{{ sClass }}</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.persistentVolume.storageClass')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.coordinator == 'sidecars')" class="step active" data-fieldset="coordinator.sidecars">
                    <div class="header">
                        <h2>Sidecars</h2>
                    </div>

                    <div class="fields">
                        <div class="row-50">
                            <h3>Connection Pooling</h3>
                            <p>To solve the Postgres connection fan-in problem (handling large number of incoming connections) StackGres includes by default a connection pooler fronting every Postgres instance. It is deployed as a sidecar. You may opt-out as well as tune the connection pooler configuration.</p>

                            <div class="col">
                                <label for="spec.coordinator.configurations.sgPoolingConfig">
                                    Connection Pooling
                                </label>  
                                <label for="connPoolingCoord" class="switch yes-no">
                                    Enable
                                    <input :checked="!coordinator.pods.disableConnectionPooling" type="checkbox" id="connPoolingCoord" @change="( (coordinator.pods.disableConnectionPooling = !coordinator.pods.disableConnectionPooling)) " data-switch="NO">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.configurations.sgPoolingConfig')"></span>
                            </div>

                            <div class="col" v-if="!coordinator.pods.disableConnectionPooling">
                                <label for="connectionPoolingConfigCoord">
                                    Connection Pooling Configuration
                                </label>
                                <select v-model="coordinator.configurations.sgPoolingConfig" class="connectionPoolingConfig" @change="(coordinator.configurations.sgPoolingConfig == 'createNewResource') && createNewResource('sgpoolconfigs')" :set="( (coordinator.configurations.sgPoolingConfig == 'createNewResource') && (coordinator.configurations.sgPoolingConfig = '') )">
                                    <option value="" selected>Default</option>
                                    <option v-for="conf in connPoolConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                                    <template v-if="iCan('create', 'sgpoolconfigs', $route.params.namespace)">
                                        <option value="" disabled>– OR –</option>
                                        <option value="createNewResource">Create new configuration</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.configurations.sgPoolingConfig')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Postgres Utils</h3>
                            <p>Sidecar container with Postgres administration tools. Optional (on by default; recommended for troubleshooting).</p>

                            <div class="col">
                                <label for="spec.coordinator.pods.disablePostgresUtil">Postgres Utils</label>  
                                <label for="postgresUtil" class="switch yes-no">
                                    Enable
                                    <input :checked="!coordinator.pods.disablePostgresUtil" type="checkbox" id="postgresUtil" @change="coordinator.pods.disablePostgresUtil = !coordinator.pods.disablePostgresUtil" data-switch="YES">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.disablePostgresUtil').replace('If set to `true`', 'If disabled')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Monitoring</h3>
                            <p>Enable Prometheus metrics scraping via service monitors. Check the <a href="https://stackgres.io/doc/latest/install/prerequisites/monitoring/" target="_blank">Installation -> Monitoring</a> section for information on how to enable in StackGres Grafana dashboard integration.</p>

                            <div class="col">
                                <label for="spec.coordinator.pods.disableMetricsExporter">Metrics Exporter</label>  
                                <label for="metricsExporterCoord" class="switch yes-no">
                                    Enable
                                    <input :checked="!coordinator.pods.disableMetricsExporter" type="checkbox" id="metricsExporterCoord" @change="( (coordinator.pods.disableMetricsExporter = !coordinator.pods.disableMetricsExporter), checkEnableMonitoring() )" data-switch="YES">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.coordinator == 'scripts')" class="step active" data-fieldset="coordinator.scripts">
                    <div class="header">
                        <h2>Managed SQL</h2>
                    </div>

                    <p>Use this option to run a set of scripts on your cluster.</p><br/><br/>

                    <div class="fields">
                        <div class="scriptFieldset repeater">
                            <div class="header">
                                <h3 for="spec.coordinator.managedSql.scripts">
                                    Scripts
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.managedSql.scripts')"></span>
                                </h3>
                            </div>
                            
                            <fieldset
                                v-for="(baseScript, baseIndex) in coordinator.managedSql.scripts"
                                :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + ']'"
                            >
                                <div class="header">
                                    <h4>SGScript #{{baseIndex+1 }}</h4>
                                    <div class="addRow" v-if="(baseScript.sgScript != (name + '-default') )">
                                        <a class="delete" @click="spliceArray(coordinator.managedSql.scripts, baseIndex), spliceArray(scriptSource.coordinator, baseIndex)">Delete Script</a>
                                        <template v-if="baseIndex">
                                            <span class="separator"></span>
                                            <a @click="moveArrayItem(coordinator.managedSql.scripts, baseIndex, 'up')">Move Up</a>
                                        </template>
                                        <template  v-if="( (baseIndex + 1) != coordinator.managedSql.scripts.length)">
                                            <span class="separator"></span>
                                            <a @click="moveArrayItem(coordinator.managedSql.scripts, baseIndex, 'down')">Move Down</a>
                                        </template>
                                    </div>
                                </div>

                                <div class="row-50 noMargin">
                                    <div class="col">
                                        <label for="spec.coordinator.managedSql.scripts.scriptSource">Source</label>
                                        <select v-model="scriptSource.coordinator[baseIndex].base" :disabled="editMode && isDefaultScript(baseScript.sgScript) && baseScript.hasOwnProperty('scriptSpec')" @change="setBaseScriptSource(baseIndex, scriptSource.coordinator, coordinator.managedSql)" :data-field="'spec.coordinator.managedSql.scripts.scriptSource.coordinator[' + baseIndex + ']'">
                                            <option value="" selected>Select source script...</option>
                                            <option v-for="script in sgscripts" v-if="(script.data.metadata.namespace == $route.params.namespace)">
                                                {{ script.name }}
                                            </option>
                                            <template v-if="iCan('create', 'sgscripts', $route.params.namespace)">
                                                <option value="" disabled>– OR –</option>
                                                <option value="createNewScript">Create new script</option>
                                            </template>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded.'"></span>
                                    </div>
                                </div>

                                <template v-if="( ( !editMode &&(scriptSource.coordinator[baseIndex].base == 'createNewScript') ) || (editMode && baseScript.hasOwnProperty('scriptSpec')) )">
                                    <hr/>

                                    <div class="row-50 noMargin">
                                        <div class="col">
                                            <label for="spec.coordinator.managedSql.scripts.continueOnError">Continue on Error</label>  
                                            <label :for="'continueOnError-' + baseIndex" class="switch yes-no" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].continueOnError'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                Enable
                                                <input type="checkbox" :id="'continueOnError-' + baseIndex" v-model="coordinator.managedSql.scripts[baseIndex].scriptSpec.continueOnError" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                            </label>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.coordinator.managedSql.scripts.managedVersions">Managed Versions</label>  
                                            <label :for="'managedVersions-' + baseIndex" class="switch yes-no" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].managedVersions'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                Enable
                                                <input type="checkbox" :id="'managedVersions-' + baseIndex" v-model="coordinator.managedSql.scripts[baseIndex].scriptSpec.managedVersions" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                            </label>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions').replace(/true/g, 'Enabled')"></span>
                                        </div>
                                    </div>
                            
                                    <div class="section">
                                        <fieldset v-for="(script, index) in baseScript.scriptSpec.scripts">
                                            <div class="header">
                                                <h5>Script Entry #{{ index+1 }} <template v-if="script.hasOwnProperty('name') && script.name.length">–</template> <span class="scriptTitle">{{ script.name }}</span></h5>
                                                <div class="addRow" v-if="!isDefaultScript(baseScript.sgScript)">
                                                    <a @click="spliceArray(baseScript.scriptSpec.scripts, index) && spliceArray(scriptSource.coordinator[baseIndex].entries, index)">Delete Entry</a>
                                                    <template v-if="index">
                                                        <span class="separator"></span>
                                                        <a @click="moveArrayItem(baseScript.scriptSpec.scripts, index, 'up')">Move Up</a>
                                                    </template>
                                                    <template  v-if="( (index + 1) != baseScript.scriptSpec.scripts.length)">
                                                        <span class="separator"></span>
                                                        <a @click="moveArrayItem(baseScript.scriptSpec.scripts, index, 'down')">Move Down</a>
                                                    </template>
                                                </div>
                                            </div>
                                            <div class="row">
                                                <div class="row-50">
                                                    <div class="col" v-if="script.hasOwnProperty('version') && editMode">
                                                        <label for="spec.coordinator.managedSql.scripts.version">Version</label>
                                                        <input type="number" v-model="script.version" autocomplete="off" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].version'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                                    </div>
                                                </div>
                                                <div class="row-50">                                                
                                                    <div class="col">
                                                        <label for="spec.coordinator.managedSql.scripts.name">Name</label>
                                                        <input v-model="script.name" placeholder="Type a name..." autocomplete="off" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].name'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                    </div>

                                                    <div class="col" v-if="script.hasOwnProperty('database') || !isDefaultScript(baseScript.sgScript)">
                                                        <label for="spec.coordinator.managedSql.scripts.database">Database</label>
                                                        <input v-model="script.database" placeholder="Type a database name..." autocomplete="off" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].database'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                                    </div>

                                                    <div class="col" v-if="script.hasOwnProperty('user') || !isDefaultScript(baseScript.sgScript)">
                                                        <label for="spec.coordinator.managedSql.scripts.user">User</label>
                                                        <input v-model="script.user" placeholder="Type a user name..." autocomplete="off" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].user'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.user')"></span>
                                                    </div>
                                                    
                                                    <div class="col" v-if="script.hasOwnProperty('wrapInTransaction') || !isDefaultScript(baseScript.sgScript)">
                                                        <label for="spec.coordinator.managedSql.scripts.wrapInTransaction">Wrap in Transaction</label>
                                                        <select v-model="script.wrapInTransaction" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].wrapInTransaction'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            <option :value="nullVal">NONE</option>
                                                            <option value="read-committed">READ COMMITTED</option>
                                                            <option value="repeatable-read">REPEATABLE READ</option>
                                                            <option value="serializable">SERIALIZABLE</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                                                    </div>
                                                
                                                    <div class="col" v-if="script.hasOwnProperty('storeStatusInDatabase') || !isDefaultScript(baseScript.sgScript)">
                                                        <label for="spec.coordinator.managedSql.scripts.storeStatusInDatabase">Store Status in Databases</label>  
                                                        <label :for="'storeStatusInDatabase[' + baseIndex + '][' + index + ']'" class="switch yes-no" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].storeStatusInDatabase'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            Enable
                                                            <input type="checkbox" :id="'storeStatusInDatabase[' + baseIndex + '][' + index + ']'" v-model="script.storeStatusInDatabase" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        </label>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase')"></span>
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.coordinator.managedSql.scripts.retryOnError">Retry on Error</label>  
                                                        <label :for="'retryOnError[' + baseIndex + '][' + index + ']'" class="switch yes-no" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].retryOnError'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            Enable
                                                            <input type="checkbox" :id="'retryOnError[' + baseIndex + '][' + index + ']'" v-model="script.retryOnError" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        </label>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError')"></span>
                                                    </div>
                                                </div>

                                                <div class="row-100">
                                                    <div class="col">
                                                        <label for="spec.coordinator.managedSql.scripts.scriptSource">
                                                            Source
                                                            <span class="req">*</span>
                                                        </label>
                                                        <select v-model="scriptSource.coordinator[baseIndex].entries[index]" @change="setScriptSource(baseIndex, index, scriptSource.coordinator, coordinator.managedSql)" :disabled="isDefaultScript(baseScript.sgScript)" :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].source'" required>
                                                            <option value="raw">Raw script</option>
                                                            <option value="secretKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.secretScript')">From Secret</option>
                                                            <option value="configMapKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.configMapScript')">From ConfigMap</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'"></span>
                                                    </div>
                                                    <div class="col">                                                
                                                        <template  v-if="(!editMode && (scriptSource.coordinator[baseIndex].entries[index] == 'raw') ) || (editMode && script.hasOwnProperty('script') )">
                                                            <label for="spec.coordinator.managedSql.scripts.script" class="script">
                                                                Script
                                                                <span class="req">*</span>
                                                            </label> 
                                                            <span class="uploadScript" v-if="!editMode">or <a @click="getScriptFile(baseIndex, index)" class="uploadLink">upload a file</a></span> 
                                                            <input :id="'scriptFile-'+ baseIndex + '-' + index" type="file" @change="uploadScript" class="hide" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            <textarea v-model="script.script" placeholder="Type a script..." :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].script'" :disabled="isDefaultScript(baseScript.sgScript)" required></textarea>
                                                        </template>
                                                        <template v-else-if="(scriptSource.coordinator[baseIndex].entries[index] != 'raw')">
                                                            <div class="header">
                                                                <h3 :for="'spec.coordinator.managedSql.scripts.scriptFrom.properties' + scriptSource.coordinator[baseIndex].entries[index]" class="capitalize">
                                                                    {{ splitUppercase(scriptSource.coordinator[baseIndex].entries[index]) }}
                                                                    
                                                                    <span class="helpTooltip" :class="( (scriptSource.coordinator[baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef')"></span>
                                                                    <span class="helpTooltip" :class="( (scriptSource.coordinator[baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef')"></span>
                                                                </h3>
                                                            </div>
                                                            
                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label :for="'spec.coordinator.managedSql.scripts.scriptFrom.properties.' + scriptSource.coordinator[baseIndex].entries[index] + '.properties.name'">
                                                                        Name
                                                                        <span class="req">*</span>
                                                                    </label>
                                                                    <input v-model="script.scriptFrom[scriptSource.coordinator[baseIndex].entries[index]].name" placeholder="Type a name.." autocomplete="off" :disabled="isDefaultScript(baseScript.sgScript)" required>
                                                                    <span class="helpTooltip" :class="( (scriptSource.coordinator[baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>
                                                                    <span class="helpTooltip" :class="( (scriptSource.coordinator[baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>
                                                                </div>

                                                                <div class="col">
                                                                    <label :for="'spec.coordinator.managedSql.scripts.scriptFrom.properties.' + scriptSource.coordinator[baseIndex].entries[index] + '.properties.key'">
                                                                        Key
                                                                        <span class="req">*</span>
                                                                    </label>
                                                                    <input v-model="script.scriptFrom[scriptSource.coordinator[baseIndex].entries[index]].key" placeholder="Type a key.." autocomplete="off" :disabled="isDefaultScript(baseScript.sgScript)" required>
                                                                    <span class="helpTooltip" :class="( (scriptSource.coordinator[baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                                                    <span class="helpTooltip" :class="( (scriptSource.coordinator[baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                                                </div>
                                                            </div>

                                                            <template v-if="editMode && (script.scriptFrom.hasOwnProperty('configMapScript'))">
                                                                <label :for="'spec.coordinator.managedSql.scripts.scriptFrom.properties.' + scriptSource.coordinator[baseIndex].entries[index] + '.properties.configMapScript'" class="script">
                                                                    Script
                                                                <span class="req">*</span>
                                                                </label> 
                                                                <textarea v-model="script.scriptFrom.configMapScript" placeholder="Type a script..." :data-field="'spec.coordinator.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].scriptFrom.configMapScript'" :disabled="isDefaultScript(baseScript.sgScript)" required></textarea>
                                                            </template>
                                                        </template>
                                                    </div>
                                                </div>
                                            </div>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="!baseScript.scriptSpec.scripts.length && 'topBorder'" v-if="!isDefaultScript(baseScript.sgScript)">
                                            <a class="addRow" @click="pushScript(baseIndex, scriptSource.coordinator, coordinator.managedSql)" >Add Entry</a>
                                        </div>
                                    </div>
                                </template>
                            </fieldset>
                            <div class="fieldsetFooter" :class="!coordinator.managedSql.scripts.length && 'topBorder'">
                                <a class="addRow" @click="pushScriptSet(scriptSource.coordinator, coordinator.managedSql)">Add Script</a>
                            </div>
                            
                            <br/><br/>
                            
                            <div v-if="hasScripts(coordinator.managedSql.scripts, scriptSource.coordinator)" class="row row-50 noMargin">
                                <div class="col">
                                    <label for="spec.coordinator.managedSql.continueOnSGScriptError">Continue on SGScripts Error</label>  
                                    <label for="continueOnSGScriptError" class="switch yes-no" data-field="spec.coordinator.managedSql.continueOnSGScriptError">
                                        Enable
                                        <input type="checkbox" id="continueOnSGScriptError" v-model="coordinator.managedSql.continueOnSGScriptError" data-switch="NO">
                                    </label>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.managedSql.continueOnSGScriptError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                </div>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.coordinator == 'pods')" class="step active" data-fieldset="coordinator.pods">
                    <div class="header">
                        <h2>User-Supplied Pods Sidecars</h2>
                    </div>

                    <div class="fields">
                        <div class="header">
                            <h3 for="spec.coordinator.pods.customVolumes">
                                Custom Volumes
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes')"></span>
                            </h3>
                        </div>
                        <p>List of volumes that can be mounted by custom containers belonging to the pod</p>

                        <br/>
                        
                        <div class="repeater">
                            <fieldset
                                class="noPaddingBottom"
                                v-if="(coordinator.pods.hasOwnProperty('customVolumes') && coordinator.pods.customVolumes.length)"
                                data-fieldset="spec.coordinator.pods.customVolumes"
                            >
                                <template v-for="(vol, index) in coordinator.pods.customVolumes">
                                    <div class="section" :key="index">
                                        <div class="header">
                                            <h4>Volume #{{ index + 1 }}{{ !isNull(vol.name) ? (': ' + vol.name) : '' }}</h4>
                                            <a class="addRow delete" @click="spliceArray(coordinator.pods.customVolumes, index); spliceArray(customVolumesType.coordinator, index)">Delete</a>
                                        </div>
                                                        
                                        <div class="row-50">
                                            <div class="col">
                                                <label>Name</label>
                                                <input :required="(customVolumesType.coordinator[index] !== null)" v-model="vol.name" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].name'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.name')"></span>
                                            </div>
                                            
                                            <div class="col">
                                                <label>Type</label>
                                                <select v-model="customVolumesType.coordinator[index]" @change="initCustomVolume(index, coordinator.pods.customVolumes, customVolumesType.coordinator)" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].type'">
                                                    <option :value="null" selected>Choose one...</option>
                                                    <option value="emptyDir">Empty Directory</option>
                                                    <option value="configMap">ConfigMap</option>
                                                    <option value="secret">Secret</option>
                                                </select>
                                                <span class="helpTooltip" data-tooltip="Specifies the type of volume to be used"></span>
                                            </div>
                                        </div>

                                        <template v-if="(customVolumesType.coordinator[index] == 'emptyDir')">
                                            <div class="header">
                                                <h5 for="spec.coordinator.pods.customVolumes.emptyDir">
                                                    Empty Directory
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.emptyDir')"></span>
                                                </h5>
                                            </div>
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Medium</label>
                                                    <input v-model="vol.emptyDir.medium" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].emptyDir.medium'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.emptyDir.properties.medium')"></span>
                                                </div>
                                                <div class="col">
                                                    <label>Size Limit</label>
                                                    <input v-model="vol.emptyDir.sizeLimit" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].emptyDir.sizeLimit'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.emptyDir.properties.sizeLimit')"></span>
                                                </div>
                                            </div>

                                        </template>
                                        <template v-else-if="(customVolumesType.coordinator[index] == 'configMap')">
                                            <div class="header">
                                                <h5 for="spec.coordinator.pods.customVolumes.configMap">
                                                    ConfigMap
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap')"></span>
                                                </h5>
                                            </div>
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Name</label>
                                                    <input v-model="vol.configMap.name" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].configMap.name'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.name')"></span>
                                                </div>
                                                <div class="col">                    
                                                    <label :for="'spec.coordinator.pods.customVolumes[' + index + '].configMap.optional'">
                                                        Optional
                                                    </label>  
                                                    <label :for="'spec.coordinator.pods.customVolumes[' + index + '].configMap.optional'" class="switch yes-no">
                                                        Enable
                                                        <input type="checkbox" :id="'spec.coordinator.pods.customVolumes[' + index + '].configMap.optional'" v-model="vol.configMap.optional" data-switch="NO" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].configMap.optional'">
                                                    </label>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.optional')"></span>
                                                </div>
                                                <div class="col">
                                                    <label>Default Mode</label>
                                                    <input type="number" v-model="vol.configMap.defaultMode" min="0" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].configMap.defaultMode'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.defaultMode')"></span>
                                                </div>
                                            </div>

                                            <br/><br/>
                                            <div class="header">
                                                <h6 for="spec.coordinator.pods.customVolumes.configMap.items">
                                                    Items
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.items')"></span>
                                                </h6>
                                            </div>
                                            <fieldset
                                                class="noMargin"
                                                :data-field="'spec.coordinator.pods.customVolumes[' + index + '].configMap.items'"
                                                v-if="vol.configMap.items.length"
                                            >
                                                <template v-for="(item, itemIndex) in vol.configMap.items">
                                                    <div class="section" :key="itemIndex" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + ']'">
                                                        <div class="header">
                                                            <h4>Item #{{ itemIndex + 1 }}</h4>
                                                            <a class="addRow delete" @click="spliceArray(vol.configMap.items, itemIndex)">Delete</a>
                                                        </div>
                                                                        
                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label>Key</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.key" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].key'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.items.items.properties.key')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Mode</label>
                                                                <input type="number" v-model="item.mode" min="0" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].mode'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.items.items.properties.mode')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Path</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.path" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].path'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.configMap.properties.items.items.properties.path')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </template>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!vol.configMap.items.length && 'topBorder'">
                                                <a
                                                    class="addRow"
                                                    @click="vol.configMap.items.push({
                                                        key: null,
                                                        mode: null,
                                                        path: null,
                                                    })"
                                                >
                                                    Add Item
                                                </a>
                                            </div>
                                        </template>

                                        <template v-else-if="(customVolumesType.coordinator[index] == 'secret')">
                                            <div class="header">
                                                <h5 for="spec.coordinator.pods.customVolumes.secret">
                                                    Secret
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret')"></span>
                                                </h5>
                                            </div>
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Secret Name</label>
                                                    <input v-model="vol.secret.secretName" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].secret.secretName'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.secretName')"></span>
                                                </div>
                                                <div class="col">                    
                                                    <label :for="'spec.coordinator.pods.customVolumes[' + index + '].secret.optional'">
                                                        Optional
                                                    </label>  
                                                    <label :for="'spec.coordinator.pods.customVolumes[' + index + '].secret.optional'" class="switch yes-no">
                                                        Enable
                                                        <input type="checkbox" :id="'spec.coordinator.pods.customVolumes[' + index + '].secret.optional'" v-model="vol.secret.optional" data-switch="NO" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].secret.optional'">
                                                    </label>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.optional')"></span>
                                                </div>
                                                <div class="col">
                                                    <label>Default Mode</label>
                                                    <input type="number" v-model="vol.secret.defaultMode" min="0" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].secret.defaultMode'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.defaultMode')"></span>
                                                </div>
                                            </div>

                                            <br/><br/>
                                            <div class="header">
                                                <h6 for="spec.coordinator.pods.customVolumes.secret.items">
                                                    Items
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.items')"></span>
                                                </h6>
                                            </div>
                                            <fieldset
                                                class="noMargin"
                                                :data-field="'spec.coordinator.pods.customVolumes[' + index + '].secret.items'"
                                                v-if="vol.secret.items.length"
                                            >
                                                <template v-for="(item, itemIndex) in vol.secret.items">
                                                    <div class="section" :key="itemIndex" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].secret.items[' + itemIndex + ']'">
                                                        <div class="header">
                                                            <h4>Item #{{ itemIndex + 1 }}</h4>
                                                            <a class="addRow delete" @click="spliceArray(vol.secret.items, itemIndex)">Delete</a>
                                                        </div>
                                                                        
                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label>Key</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.key" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].key'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.items.items.properties.key')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Mode</label>
                                                                <input type="number" v-model="item.mode" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].mode'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.items.items.properties.mode')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Path</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.path" autocomplete="off" :data-field="'spec.coordinator.pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].path'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customVolumes.secret.properties.items.items.properties.path')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </template>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!vol.secret.items.length && 'topBorder'">
                                                <a
                                                    class="addRow"
                                                    @click="vol.secret.items.push({
                                                        key: '',
                                                        mode: '',
                                                        path: '',
                                                    })"
                                                >
                                                    Add Item
                                                </a>
                                            </div>
                                        </template>
                                    </div>
                                </template>
                            </fieldset>
                            <div class="fieldsetFooter" :class="(!coordinator.pods.hasOwnProperty('customVolumes') || (coordinator.pods.hasOwnProperty('customVolumes') && !coordinator.pods.customVolumes.length) ) && 'topBorder'">
                                <a 
                                    class="addRow"
                                    @click="
                                        customVolumesType.coordinator.push(null);
                                        (!coordinator.pods.hasOwnProperty('customVolumes') && (coordinator.pods['customVolumes'] = []) );
                                        coordinator.pods.customVolumes.push({ name: null});
                                        formHash = (+new Date).toString();
                                    "
                                >
                                    Add Volume
                                </a>
                            </div>
                        </div>

                        <br/><br/><br/>

                        <template v-if="!editMode || (coordinator.pods.hasOwnProperty('customInitContainers') && coordinator.pods.customInitContainers.length)">
                            <div class="header">
                                <h3 for="spec.coordinator.pods.customInitContainers">
                                    Custom Init Containers
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers')"></span>
                                </h3>
                            </div>
                            <p>A list of custom application init containers that run within the cluster’s Pods</p>

                            <br/>
                            
                            <div class="repeater">
                                <fieldset
                                    v-if="coordinator.pods.customInitContainers.length"
                                    data-fieldset="spec.coordinator.pods.customInitContainers"
                                >
                                    <template v-for="(container, index) in coordinator.pods.customInitContainers">
                                        <div class="section" :key="index" :data-field="'spec.coordinator.pods.customInitContainers[' + index + ']'">
                                            <div class="header">
                                                <h4>Init Container #{{ index + 1 }}{{ !isNull(container.name) ? (': ' + container.name) : '' }}</h4>
                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(coordinator.pods.customInitContainers, index)">Delete</a>
                                            </div>
                                                            
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Name</label>
                                                    <input :disabled="editMode" :required="!isNull(container.image) || !isNull(container.imagePullPolicy) || !isNull(container.workingDir)" v-model="container.name" autocomplete="off" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].name'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.name')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image</label>
                                                    <input v-model="container.image" autocomplete="off" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].image'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.image')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image Pull Policy</label>
                                                    <input :disabled="editMode" v-model="container.imagePullPolicy" autocomplete="off" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].imagePullPolicy'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.imagePullPolicy')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Working Directory</label>
                                                    <input :disabled="editMode" v-model="container.workingDir" autocomplete="off" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].workingDir'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.workingDir')"></span>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('args')">
                                                    <fieldset :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].args'">
                                                        <div class="header" :class="[container.args.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.coordinator.pods.customInitContainers[' + index + '].args'">
                                                                Arguments
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.args')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(arg, argIndex) in container.args">
                                                            <div :key="'arg-' + argIndex" class="inputContainer" :class="(container.args.length !== (argIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'arg-' + argIndex" 
                                                                    v-model="container.args[argIndex]" 
                                                                    :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].args[' + argIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.args, argIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.args.push(null)">Add Argument</a>
                                                    </div>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('command')">
                                                    <fieldset :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].command'">
                                                        <div class="header" :class="[container.command.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.coordinator.pods.customInitContainers[' + index + '].command'">
                                                                Command
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.command')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(command, commandIndex) in container.command">
                                                            <div :key="'command-' + commandIndex" class="inputContainer" :class="(container.command.length !== (commandIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'command-' + commandIndex" 
                                                                    v-model="container.command[commandIndex]" 
                                                                    :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].command[' + commandIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.command, commandIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.command.push(null)">Add Command</a>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="repeater marginBottom marginTop" v-if="!editMode || container.hasOwnProperty('env')">
                                                <fieldset :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].env'">
                                                    <div class="header" :class="!container.env.length && 'noMargin'">
                                                        <h5 :for="'spec.coordinator.pods.customInitContainers[' + index + '].env'">
                                                            Environment Variables
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.env')"></span> 
                                                        </h5>
                                                    </div>
                                                    <div class="variable" v-if="container.env.length">
                                                        <div class="row" v-for="(env, envIndex) in container.env" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].env[' + envIndex + ']'">
                                                            <label>Name</label>
                                                            <input :required="!isNull(env.value)" :disabled="editMode" class="label" v-model="env.name" autocomplete="off" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].env[' + envIndex + '].name'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input :disabled="editMode" class="labelValue" v-model="env.value" autocomplete="off" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].env[' + envIndex + '].value'">

                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.env, envIndex)">Delete</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.env.push({ name: null, value: null})">Add Variable</a>
                                                </div>
                                            </div>

                                            <br/>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('ports')">
                                                <div class="header">
                                                    <h5>
                                                        Ports
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater marginBottom">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        data-field="spec.coordinator.pods.customInitContainers.ports"
                                                        v-if="container.ports.length"
                                                    >
                                                        <div class="section" v-for="(port, portIndex) in container.ports" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].ports[' + portIndex + ']'">
                                                            <div class="header">
                                                                <h6>Port #{{ portIndex + 1 }}{{ !isNull(port.name) ? (': ' + port.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.ports, portIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.ports.name">Name</label>  
                                                                    <input :disabled="editMode" v-model="port.name" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].ports[' + portIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.ports.hostIP">Host IP</label>  
                                                                    <input :disabled="editMode" v-model="port.hostIP" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].ports[' + portIndex + '].hostIP'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.hostIP')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.ports.hostPort">Host Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.hostPort" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].ports[' + portIndex + '].hostPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.hostPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.ports.containerPort">Container Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.containerPort" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].ports[' + portIndex + '].containerPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.containerPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.ports.protocol">Protocol</label>  
                                                                    <select :disabled="editMode" v-model="port.protocol" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].ports[' + portIndex + '].protocol'">
                                                                        <option :value="nullVal" selected>Choose one...</option>
                                                                        <option>TCP</option>
                                                                        <option>UDP</option>
                                                                        <option>SCTP</option>
                                                                    </select>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.ports.items.properties.protocol')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('ports') && !container.ports.length) && 'topBorder'">
                                                        <a class="addRow" @click="!container.hasOwnProperty('ports') && (container['ports'] = []); container.ports.push({
                                                            name: null,
                                                            hostIP: null,
                                                            hostPort: null,
                                                            containerPort: null,
                                                            protocol: null
                                                        })">
                                                            Add Port
                                                        </a>
                                                    </div>
                                                </div>
                                                <br/>
                                            </template>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('volumeMounts')">
                                                <div class="header">
                                                    <h5>
                                                        Volume Mounts
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts'"
                                                        v-if="container.hasOwnProperty('volumeMounts') && container.volumeMounts.length"
                                                    >
                                                        <div class="section" v-for="(mount, mountIndex) in container.volumeMounts" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + ']'">
                                                            <div class="header">
                                                                <h6>Mount #{{ mountIndex + 1 }}{{ !isNull(mount.name) ? (': ' + mount.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.volumeMounts, mountIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.volumeMounts.name">Name</label>  
                                                                    <input :required="!isNull(mount.mountPath)" :disabled="editMode" v-model="mount.name" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">                    
                                                                    <label :for="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts.readOnly'">
                                                                        Read Only
                                                                    </label>  
                                                                    <label :disabled="editMode" :for="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" class="switch yes-no">
                                                                        Enable
                                                                        <input :disabled="editMode" type="checkbox" :id="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" v-model="mount.readOnly" data-switch="NO" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'">
                                                                    </label>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.volumeMounts.mountPath">Mount Path</label>  
                                                                    <input :required="!isNull(mount.name)" :disabled="editMode" v-model="mount.mountPath" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.volumeMounts.mountPropagation">Mount Propagation</label>  
                                                                    <input :disabled="editMode" v-model="mount.mountPropagation" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPropagation'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.volumeMounts.subPath">Sub Path</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPathExpr') && !isNull(mount.subPathExpr))" v-model="mount.subPath" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].subPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.subPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customInitContainers.volumeMounts.subPathExpr">Sub Path Expr</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPath') && !isNull(mount.subPath))" v-model="mount.subPathExpr" :data-field="'spec.coordinator.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].subPathExpr'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customInitContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('volumeMounts') && !container.volumeMounts.length) && 'topBorder'">
                                                        <a 
                                                            class="addRow"
                                                            @click="
                                                                !container.hasOwnProperty('volumeMounts') && (container['volumeMounts'] = []);
                                                                container.volumeMounts.push({
                                                                    mountPath: null,
                                                                    mountPropagation: null,
                                                                    name: null,
                                                                    readOnly: false,
                                                                    subPath: null,
                                                                    subPathExpr: null
                                                                });
                                                                formHash = (+new Date).toString();
                                                            ">
                                                            Add Volume
                                                        </a>
                                                    </div>
                                                </div>
                                            </template>
                                        </div>
                                    </template>
                                </fieldset>
                                <div v-if="!editMode" class="fieldsetFooter" :class="!coordinator.pods.customInitContainers.length && 'topBorder'">
                                    <a 
                                        class="addRow"
                                        @click="coordinator.pods.customInitContainers.push({
                                            name: null,
                                            image: null,
                                            imagePullPolicy: null,
                                            args: [null],
                                            command: [null],
                                            workingDir: null,
                                            env: [ { name: null, value: null } ],
                                            ports: [{
                                                containerPort: null,
                                                hostIP: null,
                                                hostPort: null,
                                                name: null,
                                                protocol: null
                                            }],
                                            volumeMounts: [{
                                                mountPath: null,
                                                mountPropagation: null,
                                                name: null,
                                                readOnly: false,
                                                subPath: null,
                                                subPathExpr: null,
                                            }]
                                        })"
                                    >
                                        Add Init Container
                                    </a>
                                </div>
                            </div>

                            <br/><br/><br/>

                        </template>

                        <template v-if="!editMode || (coordinator.pods.hasOwnProperty('customContainers') && coordinator.pods.customContainers.length)">
                            <div class="header">
                                <h3 for="spec.coordinator.pods.customContainers">
                                    Custom Containers
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers')"></span>
                                </h3>
                            </div>
                            <p>A list of custom application containers that run within the cluster’s Pods</p>

                            <br/>
                            
                            <div class="repeater">
                                <fieldset
                                    v-if="coordinator.pods.customContainers.length"
                                    data-fieldset="spec.coordinator.pods.customContainers"
                                >
                                    <template v-for="(container, index) in coordinator.pods.customContainers">
                                        <div class="section" :key="index" :data-field="'spec.coordinator.pods.customContainers[' + index + ']'">
                                            <div class="header">
                                                <h4>Container #{{ index + 1 }}{{ !isNull(container.name) ? (': ' + container.name) : '' }}</h4>
                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(coordinator.pods.customContainers, index)">Delete</a>
                                            </div>
                                                            
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Name</label>
                                                    <input :disabled="editMode" :required="!isNull(container.image) || !isNull(container.imagePullPolicy) || !isNull(container.workingDir)" v-model="container.name" autocomplete="off" :data-field="'spec.coordinator.pods.customContainers[' + index + '].name'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.name')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image</label>
                                                    <input v-model="container.image" autocomplete="off" :data-field="'spec.coordinator.pods.customContainers[' + index + '].image'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.image')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image Pull Policy</label>
                                                    <input :disabled="editMode" v-model="container.imagePullPolicy" autocomplete="off" :data-field="'spec.coordinator.pods.customContainers[' + index + '].imagePullPolicy'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.imagePullPolicy')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Working Directory</label>
                                                    <input :disabled="editMode" v-model="container.workingDir" autocomplete="off" :data-field="'spec.coordinator.pods.customContainers[' + index + '].workingDir'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.workingDir')"></span>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('args')">
                                                    <fieldset :data-field="'spec.coordinator.pods.customContainers[' + index + '].args'">
                                                        <div class="header" :class="[container.args.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.coordinator.pods.customContainers[' + index + '].args'">
                                                                Arguments
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.args')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(arg, argIndex) in container.args">
                                                            <div :key="'arg-' + argIndex" class="inputContainer" :class="(container.args.length !== (argIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'arg-' + argIndex" 
                                                                    v-model="container.args[argIndex]" 
                                                                    :data-field="'spec.coordinator.pods.customContainers[' + index + '].args[' + argIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.args, argIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.args.push(null)">Add Argument</a>
                                                    </div>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('command')">
                                                    <fieldset :data-field="'spec.coordinator.pods.customContainers[' + index + '].command'">
                                                        <div class="header" :class="[container.command.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.coordinator.pods.customContainers[' + index + '].command'">
                                                                Command
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.command')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(command, commandIndex) in container.command">
                                                            <div :key="'command-' + commandIndex" class="inputContainer" :class="(container.command.length !== (commandIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'command-' + commandIndex" 
                                                                    v-model="container.command[commandIndex]" 
                                                                    :data-field="'spec.coordinator.pods.customContainers[' + index + '].command[' + commandIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.command, commandIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.command.push(null)">Add Command</a>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="repeater marginBottom marginTop" v-if="!editMode || container.hasOwnProperty('env')">
                                                <fieldset :data-field="'spec.coordinator.pods.customContainers[' + index + '].env'">
                                                    <div class="header" :class="!container.env.length && 'noMargin'">
                                                        <h5 :for="'spec.coordinator.pods.customContainers[' + index + '].env'">
                                                            Environment Variables
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.env')"></span> 
                                                        </h5>
                                                    </div>
                                                    <div class="variable" v-if="container.env.length">
                                                        <div class="row" v-for="(env, envIndex) in container.env" :data-field="'spec.coordinator.pods.customContainers[' + index + '].env[' + envIndex + ']'">
                                                            <label>Name</label>
                                                            <input :required="!isNull(env.value)" :disabled="editMode" class="label" v-model="env.name" autocomplete="off" :data-field="'spec.coordinator.pods.customContainers[' + index + '].env[' + envIndex + '].name'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input :disabled="editMode" class="labelValue" v-model="env.value" autocomplete="off" :data-field="'spec.coordinator.pods.customContainers[' + index + '].env[' + envIndex + '].value'">

                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.env, envIndex)">Delete</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.env.push({ name: null, value: null})">Add Variable</a>
                                                </div>
                                            </div>

                                            <br/>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('ports')">
                                                <div class="header">
                                                    <h5>
                                                        Ports
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater marginBottom">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        data-field="spec.coordinator.pods.customContainers.ports"
                                                        v-if="container.ports.length"
                                                    >
                                                        <div class="section" v-for="(port, portIndex) in container.ports" :data-field="'spec.coordinator.pods.customContainers[' + index + '].ports[' + portIndex + ']'">
                                                            <div class="header">
                                                                <h6>Port #{{ portIndex + 1 }}{{ !isNull(port.name) ? (': ' + port.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.ports, portIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.ports.name">Name</label>  
                                                                    <input :disabled="editMode" v-model="port.name" :data-field="'spec.coordinator.pods.customContainers[' + index + '].ports[' + portIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.ports.hostIP">Host IP</label>  
                                                                    <input :disabled="editMode" v-model="port.hostIP" :data-field="'spec.coordinator.pods.customContainers[' + index + '].ports[' + portIndex + '].hostIP'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.hostIP')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.ports.hostPort">Host Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.hostPort" :data-field="'spec.coordinator.pods.customContainers[' + index + '].ports[' + portIndex + '].hostPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.hostPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.ports.containerPort">Container Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.containerPort" :data-field="'spec.coordinator.pods.customContainers[' + index + '].ports[' + portIndex + '].containerPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.containerPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.ports.protocol">Protocol</label>  
                                                                    <select :disabled="editMode" v-model="port.protocol" :data-field="'spec.coordinator.pods.customContainers[' + index + '].ports[' + portIndex + '].protocol'">
                                                                        <option :value="nullVal" selected>Choose one...</option>
                                                                        <option>TCP</option>
                                                                        <option>UDP</option>
                                                                        <option>SCTP</option>
                                                                    </select>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.ports.items.properties.protocol')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('ports') && !container.ports.length) && 'topBorder'">
                                                        <a class="addRow" @click="!container.hasOwnProperty('ports') && (container['ports'] = []); container.ports.push({
                                                            name: null,
                                                            hostIP: null,
                                                            hostPort: null,
                                                            containerPort: null,
                                                            protocol: null
                                                        })">
                                                            Add Port
                                                        </a>
                                                    </div>
                                                </div>
                                                <br/>
                                            </template>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('volumeMounts')">
                                                <div class="header">
                                                    <h5>
                                                        Volume Mounts
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        :data-field="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts'"
                                                        v-if="container.hasOwnProperty('volumeMounts') && container.volumeMounts.length"
                                                    >
                                                        <div class="section" v-for="(mount, mountIndex) in container.volumeMounts" :data-field="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + ']'">
                                                            <div class="header">
                                                                <h6>Mount #{{ mountIndex + 1 }}{{ !isNull(mount.name) ? (': ' + mount.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.volumeMounts, mountIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.volumeMounts.name">Name</label>  
                                                                    <input :required="!isNull(mount.mountPath)" :disabled="editMode" v-model="mount.name" :data-field="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">                    
                                                                    <label :for="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts.readOnly'">
                                                                        Read Only
                                                                    </label>  
                                                                    <label :disabled="editMode" :for="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" class="switch yes-no">
                                                                        Enable
                                                                        <input :disabled="editMode" type="checkbox" :id="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" v-model="mount.readOnly" data-switch="NO" :data-field="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'">
                                                                    </label>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.volumeMounts.mountPath">Mount Path</label>  
                                                                    <input :required="!isNull(mount.name)" :disabled="editMode" v-model="mount.mountPath" :data-field="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.volumeMounts.mountPropagation">Mount Propagation</label>  
                                                                    <input :disabled="editMode" v-model="mount.mountPropagation" :data-field="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPropagation'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.volumeMounts.subPath">Sub Path</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPathExpr') && !isNull(mount.subPathExpr))" v-model="mount.subPath" :data-field="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].subPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.subPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.coordinator.pods.customContainers.volumeMounts.subPathExpr">Sub Path Expr</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPath') && !isNull(mount.subPath))" v-model="mount.subPathExpr" :data-field="'spec.coordinator.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].subPathExpr'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.customContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('volumeMounts') && !container.volumeMounts.length) && 'topBorder'">
                                                        <a 
                                                            class="addRow"
                                                            @click="
                                                                !container.hasOwnProperty('volumeMounts') && (container['volumeMounts'] = []);
                                                                container.volumeMounts.push({
                                                                    mountPath: null,
                                                                    mountPropagation: null,
                                                                    name: null,
                                                                    readOnly: false,
                                                                    subPath: null,
                                                                    subPathExpr: null
                                                                });
                                                                formHash = (+new Date).toString();
                                                            ">
                                                            Add Volume
                                                        </a>
                                                    </div>
                                                </div>
                                            </template>
                                        </div>
                                    </template>
                                </fieldset>
                                <div v-if="!editMode" class="fieldsetFooter" :class="!coordinator.pods.customContainers.length && 'topBorder'">
                                    <a 
                                        class="addRow"
                                        @click="coordinator.pods.customContainers.push({
                                            name: null,
                                            image: null,
                                            imagePullPolicy: null,
                                            args: [null],
                                            command: [null],
                                            workingDir: null,
                                            env: [ { name: null, value: null } ],
                                            ports: [{
                                                containerPort: null,
                                                hostIP: null,
                                                hostPort: null,
                                                name: null,
                                                protocol: null
                                            }],
                                            volumeMounts: [{
                                                mountPath: null,
                                                mountPropagation: null,
                                                name: null,
                                                readOnly: false,
                                                subPath: null,
                                                subPathExpr: null,
                                            }]
                                        })"
                                    >
                                        Add Container
                                    </a>
                                </div>
                            </div>

                            <br/><br/><br/>

                        </template>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.coordinator == 'pods-replication')" class="step active" data-fieldset="coordinator.pods-replication">
                    <div class="header">
                        <h2>Replication</h2>
                    </div>

                    <div class="fields">                    
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.coordinator.replication.mode">Mode</label>
                                <select v-model="coordinator.replication.mode" required data-field="spec.coordinator.replication.mode" @change="['sync', 'strict-sync'].includes(coordinator.replication.mode) ? (coordinator.replication['syncInstances'] = 1) : ((coordinator.replication.hasOwnProperty('syncInstances') && delete coordinator.replication.syncInstances) )">    
                                    <option>async</option>
                                    <option>sync</option>
                                    <option>strict-sync</option>
                                    <option selected>sync-all</option>
                                    <option>strict-sync-all</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.replication.mode')"></span>
                            </div>

                            <div class="col" v-if="['sync', 'strict-sync'].includes(coordinator.replication.mode)">
                                <label for="spec.coordinator.replication.syncInstances">Sync Instances</label>
                                <input type="number" min="1" :max="(coordinator.instances - 1)" v-model="coordinator.replication.syncInstances" data-field="spec.coordinator.replication.syncInstances">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.replication.syncInstances')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.coordinator == 'services')" class="step active" data-fieldset="coordinator.services">
                    <div class="header">
                        <h2>Customize generated Kubernetes service</h2>
                    </div>

                    <div class="fields">                    
                        <div class="header">
                            <h3 for="spec.postgresServices.coordinator.primary">
                                Primary Service
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.primary')"></span>
                            </h3>
                        </div>
                        
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.postgresServices.coordinator.primary.enabled">Service</label>  
                                <label for="postgresServicesPrimary" class="switch yes-no" data-field="spec.postgresServices.coordinator.primary.enabled">Enable<input type="checkbox" id="postgresServicesPrimary" v-model="postgresServices.coordinator.primary.enabled" data-switch="YES"></label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.primary.enabled')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.postgresServices.coordinator.primary.type">Type</label>
                                <select v-model="postgresServices.coordinator.primary.type" required data-field="spec.postgresServices.coordinator.primary.type">    
                                    <option>ClusterIP</option>
                                    <option>LoadBalancer</option>
                                    <option>NodePort</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.primary.type')"></span>
                            </div>

                            <div class="col">
                                <label>Load Balancer IP</label>
                                <input 
                                    v-model="postgresServices.coordinator.primary.loadBalancerIP" 
                                    autocomplete="off" 
                                    data-field="spec.postgresServices.coordinator.primary.loadBalancerIP">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.primary.loadBalancerIP')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="header">
                            <h3 for="spec.postgresServices.coordinator.any">
                                Any Service
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.any')"></span>
                            </h3>
                        </div>
                        
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.postgresServices.coordinator.any.enabled">Service</label>  
                                <label for="postgresServicesPrimary" class="switch yes-no" data-field="spec.postgresServices.coordinator.any.enabled">Enable<input type="checkbox" id="postgresServicesPrimary" v-model="postgresServices.coordinator.any.enabled" data-switch="YES"></label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.any.enabled')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.postgresServices.coordinator.any.type">Type</label>
                                <select v-model="postgresServices.coordinator.any.type" required data-field="spec.postgresServices.coordinator.any.type">    
                                    <option>ClusterIP</option>
                                    <option>LoadBalancer</option>
                                    <option>NodePort</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.any.type')"></span>
                            </div>

                            <div class="col">
                                <label>Load Balancer IP</label>
                                <input 
                                    v-model="postgresServices.coordinator.any.loadBalancerIP" 
                                    autocomplete="off" 
                                    data-field="spec.postgresServices.coordinator.any.loadBalancerIP">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.any.loadBalancerIP')"></span>
                            </div>
                        </div>

                        <div class="repeater sidecars">
                            <div class="header">
                                <h4 for="spec.postgresServices.coordinator.customPorts">
                                    Custom Ports
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts')"></span>
                                </h4>
                            </div>
                            <fieldset
                                data-field="spec.postgresServices.coordinator.customPorts"
                                v-if="postgresServices.coordinator.hasOwnProperty('customPorts') && postgresServices.coordinator.customPorts.length"
                            >
                                <div class="section" v-for="(port, index) in postgresServices.coordinator.customPorts">
                                    <div class="header">
                                        <h5>Port #{{ index + 1 }}</h5>
                                        <a class="addRow delete" @click="spliceArray(postgresServices.coordinator.customPorts, index)">Delete</a>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label for="spec.postgresServices.coordinator.customPorts.appProtocol">Application Protocol</label>  
                                            <input v-model="port.appProtocol" :data-field="'spec.postgresServices.coordinator.customPorts[' + index + '].appProtocol'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.appProtocol')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.coordinator.customPorts.name">Name</label>  
                                            <input v-model="port.name" :data-field="'spec.postgresServices.coordinator.customPorts[' + index + '].name'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.name')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.coordinator.customPorts.nodePort">Node Port</label>  
                                            <input type="number" v-model="port.nodePort" :data-field="'spec.postgresServices.coordinator.customPorts[' + index + '].nodePort'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.nodePort')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.coordinator.customPorts.port">Port</label>  
                                            <input 
                                                type="number"
                                                v-model="port.port"
                                                :data-field="'spec.postgresServices.coordinator.customPorts[' + index + '].port'"
                                                :required="(port.appProtocol != null) || (port.name != null) || (port.nodePort != null) || (port.protocol != null) || (port.targetPort != null)"
                                                autocomplete="off"
                                            >
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.port')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.coordinator.customPorts.protocol">Protocol</label>  
                                            <select v-model="port.protocol" :data-field="'spec.postgresServices.coordinator.customPorts[' + index + '].protocol'">
                                                <option :value="nullVal" selected>Choose one...</option>
                                                <option>TCP</option>
                                                <option>UDP</option>
                                                <option>SCTP</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.protocol')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.coordinator.customPorts.targetPort">Target Port</label>  
                                            <input v-model="port.targetPort" :data-field="'spec.postgresServices.coordinator.customPorts[' + index + '].targetPort'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.coordinator.customPorts.targetPort')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="(postgresServices.coordinator.hasOwnProperty('customPorts') && !postgresServices.coordinator.customPorts.length) && 'topBorder'">
                                <a class="addRow" @click="!postgresServices.coordinator.hasOwnProperty('customPorts') && (postgresServices.coordinator['customPorts'] = []); postgresServices.coordinator.customPorts.push({
                                    appProtocol: null,
                                    name: null,
                                    nodePort: null,
                                    port: null,
                                    protocol: null,
                                    targetPort: null
                                })">
                                    Add Port
                                </a>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.coordinator == 'metadata')"  class="step active podsMetadata" data-fieldset="coordinator.metadata">
                    <div class="header">
                        <h2>Metadata</h2>
                    </div>

                    <div class="fields">
                        <div class="repeater">
                            <div class="header">
                                <h3 for="spec.coordinator.metadata.labels">
                                    Labels
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.labels')"></span> 
                                </h3>
                            </div>

                            <fieldset data-field="spec.coordinator.metadata.labels.clusterPods">
                                <div class="header" :class="( !hasProp(coordinator, 'metadata.labels.clusterPods') || !coordinator.metadata.labels.clusterPods.length ) && 'noMargin noPadding'">
                                    <h3 for="spec.coordinator.metadata.labels.clusterPods">
                                        Cluster Pods
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.labels.clusterPods')"></span> 
                                    </h3>
                                </div>
                                <div class="metadata" v-if="hasProp(coordinator, 'metadata.labels.clusterPods') && coordinator.metadata.labels.clusterPods.length">
                                    <div class="row" v-for="(field, index) in coordinator.metadata.labels.clusterPods">
                                        <label>Label</label>
                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.coordinator.metadata.labels.clusterPods[' + index + '].label'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.coordinator.metadata.labels.clusterPods[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(coordinator.metadata.labels.clusterPods, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(coordinator, 'metadata.labels.clusterPods', { label: '', value: ''})">Add Label</a>
                            </div>
                        </div>

                        <br/><hr/><br/>

                        
                        <div class="header">
                            <h3 for="spec.coordinator.metadata.annotations">
                                Annotations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations')"></span>
                            </h3>
                        </div>

                        <div class="repeater">
                            <fieldset data-field="spec.coordinator.metadata.annotations.allResources">
                                <div class="header" :class="(!hasProp(coordinator, 'metadata.annotations.allResources') || !coordinator.metadata.annotations.allResources.length ) && 'noMargin noPadding'">
                                    <h3 for="spec.coordinator.metadata.annotations.allResources">
                                        All Resources
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.allResources')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(coordinator, 'metadata.annotations.allResources') && coordinator.metadata.annotations.allResources.length)">
                                    <div class="row" v-for="(field, index) in coordinator.metadata.annotations.allResources">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.allResources[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.allResources[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(coordinator.metadata.annotations.allResources, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(coordinator, 'metadata.annotations.allResources', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>
                        
                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.coordinator.metadata.annotations.clusterPods">
                                <div class="header" :class="(!hasProp(coordinator, 'metadata.annotations.clusterPods') || !coordinator.metadata.annotations.clusterPods.length ) && 'noMargin noPadding'">
                                    <h3 for="spec.coordinator.metadata.annotations.clusterPods">
                                        Cluster Pods
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.clusterPods')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(coordinator, 'metadata.annotations.clusterPods') && coordinator.metadata.annotations.clusterPods.length)">
                                    <div class="row" v-for="(field, index) in coordinator.metadata.annotations.clusterPods">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.clusterPods[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.clusterPods[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(coordinator.metadata.annotations.clusterPods, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(coordinator, 'metadata.annotations.clusterPods', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.coordinator.metadata.annotations.services">
                                <div class="header" :class="(!hasProp(coordinator, 'metadata.annotations.services') || !coordinator.metadata.annotations.services.length ) && 'noMargin noPadding'">
                                    <h3 for="spec.coordinator.metadata.annotations.services">
                                        Services
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.services')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(coordinator, 'metadata.annotations.services') && coordinator.metadata.annotations.services.length)">
                                    <div class="row" v-for="(field, index) in coordinator.metadata.annotations.services">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.services[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.services[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(coordinator.metadata.annotations.services, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(coordinator, 'metadata.annotations.services', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.coordinator.metadata.annotations.primaryService">
                                <div class="header" :class="(!hasProp(coordinator, 'metadata.annotations.primaryService') || !coordinator.metadata.annotations.primaryService.length ) && 'noMargin noPadding'">
                                    <h3 for="spec.coordinator.metadata.annotations.primaryService">
                                        Primary Service 
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.primaryService')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(coordinator, 'metadata.annotations.primaryService') && coordinator.metadata.annotations.primaryService.length)">
                                    <div class="row" v-for="(field, index) in coordinator.metadata.annotations.primaryService">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.primaryService[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.primaryService[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(coordinator.metadata.annotations.primaryService, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(coordinator, 'metadata.annotations.primaryService', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.coordinator.metadata.annotations.replicasService">
                                <div class="header" :class="(!hasProp(coordinator, 'metadata.annotations.replicasService') || !coordinator.metadata.annotations.replicasService.length) && 'noMargin noPadding'">
                                    <h3 for="spec.coordinator.metadata.annotations.replicasService">
                                        Replicas Service
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.metadata.annotations.replicasService')"></span>
                                    </h3>
                                </div>
                                <div class="annotation repeater" v-if="(hasProp(coordinator, 'metadata.annotations.replicasService') && coordinator.metadata.annotations.replicasService.length)">
                                    <div class="row" v-for="(field, index) in coordinator.metadata.annotations.replicasService">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.replicasService[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.coordinator.metadata.annotations.replicasService[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(coordinator.metadata.annotations.replicasService, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(coordinator, 'metadata.annotations.replicasService', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.coordinator == 'scheduling')" class="step active podsMetadata" id="podsSchedulingCoord" data-fieldset="coordinator.scheduling">
                    <div class="header">
                        <h2>Pods Scheduling</h2>
                    </div>
                    
                    <div class="fields">
                        <div class="repeater">
                            <div class="header">
                                <h3 for="spec.coordinator.pods.scheduling.nodeSelector">
                                    Node Selectors
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeSelector')"></span>
                                </h3>
                            </div>
                            <fieldset v-if="(hasProp(coordinator, 'pods.scheduling.nodeSelector') && coordinator.pods.scheduling.nodeSelector.length)" data-field="spec.coordinator.pods.scheduling.nodeSelector">
                                <div class="scheduling">
                                    <div class="row" v-for="(field, index) in coordinator.pods.scheduling.nodeSelector">
                                        <label>Label</label>
                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.coordinator.pods.scheduling.nodeSelector[' + index + '].label'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.coordinator.pods.scheduling.nodeSelector[' + index + '].value'">
                                        
                                        <a class="addRow" @click="spliceArray(coordinator.pods.scheduling.nodeSelector, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( (!hasProp(coordinator, 'pods.scheduling.nodeSelector') || !coordinator.pods.scheduling.nodeSelector.length) && 'topBorder' )">
                                <a class="addRow" @click="pushElement(coordinator, 'pods.scheduling.nodeSelector', { label: '', value: ''})">Add Node Selector</a>
                            </div>
                        </div>

                        <br/><br/>
                    
                        <div class="header">
                            <h3 for="spec.coordinator.pods.scheduling.tolerations">
                                Node Tolerations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations')"></span>
                            </h3>
                        </div>
                
                        <div class="scheduling repeater">
                            <fieldset v-if="(hasProp(coordinator, 'pods.scheduling.tolerations') && coordinator.pods.scheduling.tolerations.length)" data-field="spec.coordinator.pods.scheduling.tolerations">
                                <div class="section" v-for="(field, index) in coordinator.pods.scheduling.tolerations">
                                    <div class="header">
                                        <h4 for="spec.coordinator.pods.scheduling.tolerations">Toleration #{{ index+1 }}</h4>
                                        <a class="addRow del" @click="spliceArray(coordinator.pods.scheduling.tolerations, index)">Delete</a>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label :for="'spec.coordinator.pods.scheduling.tolerations[' + index + '].key'">Key</label>
                                            <input v-model="field.key" autocomplete="off" :data-field="'spec.coordinator.pods.scheduling.tolerations[' + index + '].key'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.key')"></span>
                                        </div>
                                        
                                        <div class="col">
                                            <label :for="'spec.coordinator.pods.scheduling.tolerations[' + index + '].operator'">Operator</label>
                                            <select v-model="field.operator" @change="( (field.operator == 'Exists') ? (delete field.value) : (field.value = '') )" :data-field="'spec.coordinator.pods.scheduling.tolerations[' + index + '].operator'">
                                                <option>Equal</option>
                                                <option>Exists</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.operator')"></span>
                                        </div>

                                        <div class="col" v-if="field.operator == 'Equal'">
                                            <label :for="'spec.coordinator.pods.scheduling.tolerations[' + index + '].value'">Value</label>
                                            <input v-model="field.value" :disabled="(field.operator == 'Exists')" autocomplete="off" :data-field="'spec.coordinator.pods.scheduling.tolerations[' + index + '].value'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.value')"></span>
                                        </div>

                                        <div class="col">
                                            <label :for="'spec.coordinator.pods.scheduling.tolerations[' + index + '].operator'">Effect</label>
                                            <select v-model="field.effect" :data-field="'spec.coordinator.pods.scheduling.tolerations[' + index + '].effect'">
                                                <option :value="nullVal">MatchAll</option>
                                                <option>NoSchedule</option>
                                                <option>PreferNoSchedule</option>
                                                <option>NoExecute</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.effect')"></span>
                                        </div>

                                        <div class="col" v-if="field.effect == 'NoExecute'">
                                            <label :for="'spec.coordinator.pods.scheduling.tolerations[' + index + '].tolerationSeconds'">Toleration Seconds</label>
                                            <input type="number" min="0" v-model="field.tolerationSeconds" :data-field="'spec.coordinator.pods.scheduling.tolerations[' + index + '].tolerationSeconds'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( (!hasProp(coordinator, 'pods.scheduling.tolerations') || !coordinator.pods.scheduling.tolerations.length) && 'topBorder')">
                                <a class="addRow" @click="pushElement(coordinator, 'pods.scheduling.tolerations', { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null })">Add Toleration</a>
                            </div>
                        </div>

                        <br/><br/><br/>

                        <div class="header">
                            <h3 for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                <span class="normal">Required During Scheduling Ignored During Execution</span>
                            </h3>                            
                        </div>

                        <br/><br/>
                        
                        <div class="scheduling repeater">
                            <div class="header">
                                <h4 for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                    Node Selector Terms
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')"></span>
                                </h4>
                            </div>
                            <fieldset v-if="(hasProp(coordinator, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') && coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length)">
                                <div class="section" v-for="(requiredAffinityTerm, termIndex) in coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                    <div class="header">
                                        <h5>Term #{{ termIndex + 1 }}</h5>
                                        <a class="addRow" @click="spliceArray(coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms, termIndex)">Delete</a>
                                    </div>
                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions">
                                                Match Expressions
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span> 
                                            </label>
                                        </div>
                                        <fieldset v-if="requiredAffinityTerm.matchExpressions.length">
                                            <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.matchExpressions">
                                                <div class="header">
                                                    <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items">
                                                        Match Expression #{{ expIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items')"></span> 
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchExpressions, expIndex)">Delete</a>
                                                </div>
                                                
                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">Key</label>
                                                        <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span> 
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">Operator</label>
                                                        <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( !expression.hasOwnProperty('values') && (expression['values'] = ['']) ) )" data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span> 
                                                    </div>
                                                </div>

                                                <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                        <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                    <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!requiredAffinityTerm.matchExpressions.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchExpressions)">Add Expression</a>
                                    </div>

                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields">
                                                Match Fields
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span> 
                                            </label>
                                        </div>
                                        <fieldset v-if="requiredAffinityTerm.matchFields.length">
                                            <div class="section" v-for="(field, fieldIndex) in requiredAffinityTerm.matchFields">
                                                <div class="header">
                                                    <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items">
                                                        Match Field #{{ fieldIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items')"></span> 
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchFields, fieldIndex)">Delete</a>
                                                </div>
                                                
                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">Key</label>
                                                        <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span> 
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">Operator</label>
                                                        <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( !field.hasOwnProperty('values') && (field['values'] = ['']) ) )" data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                    </div>
                                                </div>

                                                <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span> 
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                        <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="!['Gt', 'Lt'].includes(field.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(field.operator)" :class="!field.values.length && 'topBorder'">
                                                    <a class="addRow" @click="field.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!requiredAffinityTerm.matchFields.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchFields)">Add Field</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( (!hasProp(coordinator, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') || !coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length) && 'topBorder' )">
                                <a class="addRow" @click="addRequiredAffinityTerm(coordinator, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')">Add Term</a>
                            </div>
                        </div>

                        <br/><br/>
                    
                        <div class="header">
                            <h3 for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                <span class="normal">Preferred During Scheduling Ignored During Execution</span>
                            </h3>
                        </div>

                        <br/><br/>

                        <div class="scheduling repeater">
                            <div class="header">
                                <h4 for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items">
                                    Node Selector Terms
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span> 
                                </h4>
                            </div>
                            <fieldset v-if="(hasProp(coordinator, 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') && coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.length)">
                                <div class="section" v-for="(preferredAffinityTerm, termIndex) in coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                    <div class="header">
                                        <h5>Term #{{ termIndex + 1 }}</h5>
                                        <a class="addRow" @click="spliceArray(coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution, termIndex)">Delete</a>
                                    </div>
                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions">
                                                Match Expressions
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                            </label>
                                        </div>
                                        <fieldset v-if="preferredAffinityTerm.preference.matchExpressions.length">
                                            <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.preference.matchExpressions">
                                                <div class="header">
                                                    <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                        Match Expression #{{ expIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchExpressions, expIndex)">Delete</a>
                                                </div>

                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                        <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                        <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                    </div>
                                                </div>

                                                <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                        <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                    <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!preferredAffinityTerm.preference.matchExpressions.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchExpressions)">Add Expression</a>
                                    </div>

                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields">
                                                Match Fields
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                            </label>
                                        </div>
                                        <fieldset v-if="preferredAffinityTerm.preference.matchFields.length">
                                            <div class="section" v-for="(field, fieldIndex) in preferredAffinityTerm.preference.matchFields">
                                                <div class="header">
                                                    <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items">
                                                        Match Field #{{ fieldIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items')"></span>
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchFields, fieldIndex)">Delete</a>
                                                </div>

                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">Key</label>
                                                        <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">Operator</label>
                                                        <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( (!field.hasOwnProperty('values') || (field.values.length > 1) ) && (field['values'] = ['']) ) )" data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                    </div>
                                                </div>

                                                <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span> 
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                        <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="!['Gt', 'Lt'].includes(field.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(field.operator)" :class="!field.values.length && 'topBorder'">
                                                    <a class="addRow" @click="field.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!preferredAffinityTerm.preference.matchFields.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchFields)">Add Field</a>
                                    </div>

                                    <label for="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">Weight</label>
                                    <input v-model="preferredAffinityTerm.weight" autocomplete="off" type="number" min="1" max="100" class="affinityWeight" data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight')"></span>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="!preferredAffinity.length && 'topBorder'">
                                <a class="addRow" @click="addPreferredAffinityTerm(coordinator, 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">Add Term</a>
                            </div>
                        </div>

                        <span class="warning" v-if="editMode">Please, be aware that any changes made to the <code>Pods Scheduling</code> will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance of the cluster</span>
                    </div>
                </fieldset>
            </template>

            <template v-else-if="currentSection == 'shards'">
                <fieldset v-if="(currentStep.shards == 'shards')" class="step active" data-fieldset="shards.cluster">
                    <div class="header">
                        <h2>Shards Information</h2>
                    </div>

                    <div class="fields">
                        <div class="row-50">
                            <h3>Clusters</h3>

                            <div class="col">
                                <label for="spec.shards.clusters">Number of Clusters <span class="req">*</span></label>
                                <input type="number" v-model="shards.clusters" required data-field="spec.shards.clusters" min="1" max="16">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.clusters')"></span>
                            </div>
                        </div>
                        
                        <hr/>
                        
                        <div class="row-50">
                            <h3>Instances</h3>

                            <div class="col">
                                <label for="spec.shards.instancesPerCluster">Number of Instances per Cluster<span class="req">*</span></label>
                                <input type="number" v-model="shards.instancesPerCluster" required data-field="spec.shards.instancesPerCluster" min="1" max="16">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.instancesPerCluster')"></span>
                            </div>
                            <div class="col">
                                <label for="spec.shards.sgInstanceProfile">Instance Profile</label>  
                                <select v-model="shards.sgInstanceProfile" class="resourceProfile" data-field="spec.shards.sgInstanceProfile" @change="(resourceProfile == 'createNewResource') && createNewResource('sginstanceprofiles')" :set="( (resourceProfile == 'createNewResource') && (resourceProfile = '') )">
                                    <option selected value="">Default (Cores: 1, RAM: 2GiB)</option>
                                    <option v-for="prof in profiles" v-if="prof.data.metadata.namespace == namespace" :value="prof.name">{{ prof.name }} (Cores: {{ prof.data.spec.cpu }}, RAM: {{ prof.data.spec.memory }}B)</option>
                                    <template v-if="iCan('create', 'sginstanceprofiles', $route.params.namespace)">
                                        <option disabled>– OR –</option>
                                        <option value="createNewResource">Create new profile</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.sgInstanceProfile')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Postgres</h3><br/>

                            <div class="col">
                                <label for="spec.shards.configurations.sgPostgresConfig">Postgres Configuration</label>
                                <select v-model="shards.configurations.sgPostgresConfig" class="pgConfig" data-field="spec.configurations.sgPostgresConfig" @change="(shards.configurations.sgPostgresConfig == 'createNewResource') && createNewResource('sgpgconfigs')" :set="( (shards.configurations.sgPostgresConfig == 'createNewResource') && (shards.configurations.sgPostgresConfig = '') )">
                                    <option value="" selected>Default</option>
                                    <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.postgresVersion == shortPostgresVersion) )">{{ conf.name }}</option>
                                    <template v-if="iCan('create', 'sgpgconfigs', $route.params.namespace)">
                                        <option disabled>– OR –</option>
                                        <option value="createNewResource">Create new configuration</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.configurations.sgPostgresConfig')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Pods Storage</h3>

                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.shards.pods.persistentVolume.size">Volume Size <span class="req">*</span></label>  
                                    <input v-model="shards.pods.persistentVolume.size.size" class="size" required data-field="spec.shards.pods.persistentVolume.size" type="number">
                                    <select v-model="shards.pods.persistentVolume.size.unit" class="unit" required data-field="spec.shards.pods.persistentVolume.size" >
                                        <option disabled value="">Select Unit</option>
                                        <option value="Mi">MiB</option>
                                        <option value="Gi">GiB</option>
                                        <option value="Ti">TiB</option>   
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.persistentVolume.size')"></span>
                                </div>
                            </div>

                            <div class="col">
                                <label for="spec.shards.pods.persistentVolume.storageClass">Storage Class</label>
                                <select v-model="shards.pods.persistentVolume.storageClass" data-field="spec.shards.pods.persistentVolume.storageClass" :disabled="!storageClasses.length">
                                    <option value=""> {{ storageClasses.length ? 'Select Storage Class' : 'No storage classes available' }}</option>
                                    <option v-for="sClass in storageClasses">{{ sClass }}</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.persistentVolume.storageClass')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.shards == 'sidecars')" class="step active" data-fieldset="shards.sidecars">
                    <div class="header">
                        <h2>Sidecars</h2>
                    </div>

                    <div class="fields">
                        <div class="row-50">
                            <h3>Connection Pooling</h3>
                            <p>To solve the Postgres connection fan-in problem (handling large number of incoming connections) StackGres includes by default a connection pooler fronting every Postgres instance. It is deployed as a sidecar. You may opt-out as well as tune the connection pooler configuration.</p>

                            <div class="col">
                                <label for="spec.shards.configurations.sgPoolingConfig">
                                    Connection Pooling
                                </label>  
                                <label for="connPoolingShards" class="switch yes-no">
                                    Enable
                                    <input :checked="!shards.pods.disableConnectionPooling" type="checkbox" id="connPoolingShards" @change="( (shards.pods.disableConnectionPooling = !shards.pods.disableConnectionPooling)) " data-switch="NO">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.configurations.sgPoolingConfig')"></span>
                            </div>

                            <div class="col" v-if="!shards.pods.disableConnectionPooling">
                                <label for="connectionPoolingConfigShards">
                                    Connection Pooling Configuration
                                </label>
                                <select v-model="shards.configurations.sgPoolingConfig" class="connectionPoolingConfig" @change="(shards.configurations.sgPoolingConfig == 'createNewResource') && createNewResource('sgpoolconfigs')" :set="( (shards.configurations.sgPoolingConfig == 'createNewResource') && (shards.configurations.sgPoolingConfig = '') )">
                                    <option value="" selected>Default</option>
                                    <option v-for="conf in connPoolConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                                    <template v-if="iCan('create', 'sgpoolconfigs', $route.params.namespace)">
                                        <option value="" disabled>– OR –</option>
                                        <option value="createNewResource">Create new configuration</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.configurations.sgPoolingConfig')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Postgres Utils</h3>
                            <p>Sidecar container with Postgres administration tools. Optional (on by default; recommended for troubleshooting).</p>

                            <div class="col">
                                <label for="spec.shards.pods.disablePostgresUtil">Postgres Utils</label>  
                                <label for="postgresUtil" class="switch yes-no">
                                    Enable
                                    <input :checked="!shards.pods.disablePostgresUtil" type="checkbox" id="postgresUtil" @change="shards.pods.disablePostgresUtil = !shards.pods.disablePostgresUtil" data-switch="YES">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.disablePostgresUtil').replace('If set to `true`', 'If disabled')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Monitoring</h3>
                            <p>Enable Prometheus metrics scraping via service monitors. Check the <a href="https://stackgres.io/doc/latest/install/prerequisites/monitoring/" target="_blank">Installation -> Monitoring</a> section for information on how to enable in StackGres Grafana dashboard integration.</p>

                            <div class="col">
                                <label for="spec.shards.pods.disableMetricsExporter">Metrics Exporter</label>  
                                <label for="metricsExporterShards" class="switch yes-no">
                                    Enable
                                    <input :checked="!shards.pods.disableMetricsExporter" type="checkbox" id="metricsExporterShards" @change="( (shards.pods.disableMetricsExporter = !shards.pods.disableMetricsExporter), checkEnableMonitoring() )" data-switch="YES">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.shards == 'scripts')" class="step active" data-fieldset="shards.scripts">
                    <div class="header">
                        <h2>Managed SQL</h2>
                    </div>

                    <p>Use this option to run a set of scripts on your cluster.</p><br/><br/>

                    <div class="fields">
                        <div class="scriptFieldset repeater">
                            <div class="header">
                                <h3 for="spec.shards.managedSql.scripts">
                                    Scripts
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.managedSql.scripts')"></span>
                                </h3>
                            </div>
                            
                            <fieldset
                                v-for="(baseScript, baseIndex) in shards.managedSql.scripts"
                                :data-field="'spec.shards.managedSql.scripts[' + baseIndex + ']'"
                            >
                                <div class="header">
                                    <h4>SGScript #{{baseIndex+1 }}</h4>
                                    <div class="addRow" v-if="(baseScript.sgScript != (name + '-default') )">
                                        <a class="delete" @click="spliceArray(shards.managedSql.scripts, baseIndex), spliceArray(scriptSource.shards, baseIndex)">Delete Script</a>
                                        <template v-if="baseIndex">
                                            <span class="separator"></span>
                                            <a @click="moveArrayItem(shards.managedSql.scripts, baseIndex, 'up')">Move Up</a>
                                        </template>
                                        <template  v-if="( (baseIndex + 1) != shards.managedSql.scripts.length)">
                                            <span class="separator"></span>
                                            <a @click="moveArrayItem(shards.managedSql.scripts, baseIndex, 'down')">Move Down</a>
                                        </template>
                                    </div>
                                </div>

                                <div class="row-50 noMargin">
                                    <div class="col">
                                        <label for="spec.shards.managedSql.scripts.scriptSource">Source</label>
                                        <select v-model="scriptSource.shards[baseIndex].base" :disabled="editMode && isDefaultScript(baseScript.sgScript) && baseScript.hasOwnProperty('scriptSpec')" @change="setBaseScriptSource(baseIndex, scriptSource.shards, shards.managedSql)" :data-field="'spec.shards.managedSql.scripts.scriptSource.shards[' + baseIndex + ']'">
                                            <option value="" selected>Select source script...</option>
                                            <option v-for="script in sgscripts" v-if="(script.data.metadata.namespace == $route.params.namespace)">
                                                {{ script.name }}
                                            </option>
                                            <template v-if="iCan('create', 'sgscripts', $route.params.namespace)">
                                                <option value="" disabled>– OR –</option>
                                                <option value="createNewScript">Create new script</option>
                                            </template>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded.'"></span>
                                    </div>
                                </div>

                                <template v-if="( ( !editMode &&(scriptSource.shards[baseIndex].base == 'createNewScript') ) || (editMode && baseScript.hasOwnProperty('scriptSpec')) )">
                                    <hr/>

                                    <div class="row-50 noMargin">
                                        <div class="col">
                                            <label for="spec.shards.managedSql.scripts.continueOnError">Continue on Error</label>  
                                            <label :for="'continueOnError-' + baseIndex" class="switch yes-no" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].continueOnError'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                Enable
                                                <input type="checkbox" :id="'continueOnError-' + baseIndex" v-model="shards.managedSql.scripts[baseIndex].scriptSpec.continueOnError" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                            </label>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.shards.managedSql.scripts.managedVersions">Managed Versions</label>  
                                            <label :for="'managedVersions-' + baseIndex" class="switch yes-no" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].managedVersions'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                Enable
                                                <input type="checkbox" :id="'managedVersions-' + baseIndex" v-model="shards.managedSql.scripts[baseIndex].scriptSpec.managedVersions" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                            </label>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions').replace(/true/g, 'Enabled')"></span>
                                        </div>
                                    </div>
                            
                                    <div class="section">
                                        <fieldset v-for="(script, index) in baseScript.scriptSpec.scripts">
                                            <div class="header">
                                                <h5>Script Entry #{{ index+1 }} <template v-if="script.hasOwnProperty('name') && script.name.length">–</template> <span class="scriptTitle">{{ script.name }}</span></h5>
                                                <div class="addRow" v-if="!isDefaultScript(baseScript.sgScript)">
                                                    <a @click="spliceArray(baseScript.scriptSpec.scripts, index) && spliceArray(scriptSource.shards[baseIndex].entries, index)">Delete Entry</a>
                                                    <template v-if="index">
                                                        <span class="separator"></span>
                                                        <a @click="moveArrayItem(baseScript.scriptSpec.scripts, index, 'up')">Move Up</a>
                                                    </template>
                                                    <template  v-if="( (index + 1) != baseScript.scriptSpec.scripts.length)">
                                                        <span class="separator"></span>
                                                        <a @click="moveArrayItem(baseScript.scriptSpec.scripts, index, 'down')">Move Down</a>
                                                    </template>
                                                </div>
                                            </div>
                                            <div class="row">
                                                <div class="row-50">
                                                    <div class="col" v-if="script.hasOwnProperty('version') && editMode">
                                                        <label for="spec.shards.managedSql.scripts.version">Version</label>
                                                        <input type="number" v-model="script.version" autocomplete="off" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].version'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                                    </div>
                                                </div>
                                                <div class="row-50">                                                
                                                    <div class="col">
                                                        <label for="spec.shards.managedSql.scripts.name">Name</label>
                                                        <input v-model="script.name" placeholder="Type a name..." autocomplete="off" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].name'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                    </div>

                                                    <div class="col" v-if="script.hasOwnProperty('database') || !isDefaultScript(baseScript.sgScript)">
                                                        <label for="spec.shards.managedSql.scripts.database">Database</label>
                                                        <input v-model="script.database" placeholder="Type a database name..." autocomplete="off" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].database'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                                    </div>

                                                    <div class="col" v-if="script.hasOwnProperty('user') || !isDefaultScript(baseScript.sgScript)">
                                                        <label for="spec.shards.managedSql.scripts.user">User</label>
                                                        <input v-model="script.user" placeholder="Type a user name..." autocomplete="off" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].user'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.user')"></span>
                                                    </div>
                                                    
                                                    <div class="col" v-if="script.hasOwnProperty('wrapInTransaction') || !isDefaultScript(baseScript.sgScript)">
                                                        <label for="spec.shards.managedSql.scripts.wrapInTransaction">Wrap in Transaction</label>
                                                        <select v-model="script.wrapInTransaction" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].wrapInTransaction'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            <option :value="nullVal">NONE</option>
                                                            <option value="read-committed">READ COMMITTED</option>
                                                            <option value="repeatable-read">REPEATABLE READ</option>
                                                            <option value="serializable">SERIALIZABLE</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                                                    </div>
                                                
                                                    <div class="col" v-if="script.hasOwnProperty('storeStatusInDatabase') || !isDefaultScript(baseScript.sgScript)">
                                                        <label for="spec.shards.managedSql.scripts.storeStatusInDatabase">Store Status in Databases</label>  
                                                        <label :for="'storeStatusInDatabase[' + baseIndex + '][' + index + ']'" class="switch yes-no" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].storeStatusInDatabase'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            Enable
                                                            <input type="checkbox" :id="'storeStatusInDatabase[' + baseIndex + '][' + index + ']'" v-model="script.storeStatusInDatabase" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        </label>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase')"></span>
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.shards.managedSql.scripts.retryOnError">Retry on Error</label>  
                                                        <label :for="'retryOnError[' + baseIndex + '][' + index + ']'" class="switch yes-no" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].retryOnError'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            Enable
                                                            <input type="checkbox" :id="'retryOnError[' + baseIndex + '][' + index + ']'" v-model="script.retryOnError" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        </label>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError')"></span>
                                                    </div>
                                                </div>

                                                <div class="row-100">
                                                    <div class="col">
                                                        <label for="spec.shards.managedSql.scripts.scriptSource">
                                                            Source
                                                            <span class="req">*</span>
                                                        </label>
                                                        <select v-model="scriptSource.shards[baseIndex].entries[index]" @change="setScriptSource(baseIndex, index, scriptSource.shards, shards.managedSql)" :disabled="isDefaultScript(baseScript.sgScript)" :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].source'" required>
                                                            <option value="raw">Raw script</option>
                                                            <option value="secretKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.secretScript')">From Secret</option>
                                                            <option value="configMapKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.configMapScript')">From ConfigMap</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'"></span>
                                                    </div>
                                                    <div class="col">                                                
                                                        <template  v-if="(!editMode && (scriptSource.shards[baseIndex].entries[index] == 'raw') ) || (editMode && script.hasOwnProperty('script') )">
                                                            <label for="spec.shards.managedSql.scripts.script" class="script">
                                                                Script
                                                                <span class="req">*</span>
                                                            </label> 
                                                            <span class="uploadScript" v-if="!editMode">or <a @click="getScriptFile(baseIndex, index)" class="uploadLink">upload a file</a></span> 
                                                            <input :id="'scriptFile-'+ baseIndex + '-' + index" type="file" @change="uploadScript" class="hide" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            <textarea v-model="script.script" placeholder="Type a script..." :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].script'" :disabled="isDefaultScript(baseScript.sgScript)" required></textarea>
                                                        </template>
                                                        <template v-else-if="(scriptSource.shards[baseIndex].entries[index] != 'raw')">
                                                            <div class="header">
                                                                <h3 :for="'spec.shards.managedSql.scripts.scriptFrom.properties' + scriptSource.shards[baseIndex].entries[index]" class="capitalize">
                                                                    {{ splitUppercase(scriptSource.shards[baseIndex].entries[index]) }}
                                                                    
                                                                    <span class="helpTooltip" :class="( (scriptSource.shards[baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef')"></span>
                                                                    <span class="helpTooltip" :class="( (scriptSource.shards[baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef')"></span>
                                                                </h3>
                                                            </div>
                                                            
                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label :for="'spec.shards.managedSql.scripts.scriptFrom.properties.' + scriptSource.shards[baseIndex].entries[index] + '.properties.name'">
                                                                        Name
                                                                        <span class="req">*</span>
                                                                    </label>
                                                                    <input v-model="script.scriptFrom[scriptSource.shards[baseIndex].entries[index]].name" placeholder="Type a name.." autocomplete="off" :disabled="isDefaultScript(baseScript.sgScript)" required>
                                                                    <span class="helpTooltip" :class="( (scriptSource.shards[baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>
                                                                    <span class="helpTooltip" :class="( (scriptSource.shards[baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>
                                                                </div>

                                                                <div class="col">
                                                                    <label :for="'spec.shards.managedSql.scripts.scriptFrom.properties.' + scriptSource.shards[baseIndex].entries[index] + '.properties.key'">
                                                                        Key
                                                                        <span class="req">*</span>
                                                                    </label>
                                                                    <input v-model="script.scriptFrom[scriptSource.shards[baseIndex].entries[index]].key" placeholder="Type a key.." autocomplete="off" :disabled="isDefaultScript(baseScript.sgScript)" required>
                                                                    <span class="helpTooltip" :class="( (scriptSource.shards[baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                                                    <span class="helpTooltip" :class="( (scriptSource.shards[baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                                                </div>
                                                            </div>

                                                            <template v-if="editMode && (script.scriptFrom.hasOwnProperty('configMapScript'))">
                                                                <label :for="'spec.shards.managedSql.scripts.scriptFrom.properties.' + scriptSource.shards[baseIndex].entries[index] + '.properties.configMapScript'" class="script">
                                                                    Script
                                                                <span class="req">*</span>
                                                                </label> 
                                                                <textarea v-model="script.scriptFrom.configMapScript" placeholder="Type a script..." :data-field="'spec.shards.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].scriptFrom.configMapScript'" :disabled="isDefaultScript(baseScript.sgScript)" required></textarea>
                                                            </template>
                                                        </template>
                                                    </div>
                                                </div>
                                            </div>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="!baseScript.scriptSpec.scripts.length && 'topBorder'" v-if="!isDefaultScript(baseScript.sgScript)">
                                            <a class="addRow" @click="pushScript(baseIndex, scriptSource.shards, shards.managedSql)" >Add Entry</a>
                                        </div>
                                    </div>
                                </template>
                            </fieldset>
                            <div class="fieldsetFooter" :class="!shards.managedSql.scripts.length && 'topBorder'">
                                <a class="addRow" @click="pushScriptSet(scriptSource.shards, shards.managedSql)">Add Script</a>
                            </div>
                            
                            <br/><br/>
                            
                            <div v-if="hasScripts(shards.managedSql.scripts, scriptSource.shards)" class="row row-50 noMargin">
                                <div class="col">
                                    <label for="spec.shards.managedSql.continueOnSGScriptError">Continue on SGScripts Error</label>  
                                    <label for="continueOnSGScriptError" class="switch yes-no" data-field="spec.shards.managedSql.continueOnSGScriptError">
                                        Enable
                                        <input type="checkbox" id="continueOnSGScriptError" v-model="shards.managedSql.continueOnSGScriptError" data-switch="NO">
                                    </label>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.managedSql.continueOnSGScriptError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                </div>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.shards == 'pods')" class="step active" data-fieldset="shards.pods">
                    <div class="header">
                        <h2>User-Supplied Pods Sidecars</h2>
                    </div>

                    <div class="fields">
                        <div class="header">
                            <h3 for="spec.shards.pods.customVolumes">
                                Custom Volumes
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes')"></span>
                            </h3>
                        </div>
                        <p>List of volumes that can be mounted by custom containers belonging to the pod</p>

                        <br/>
                        
                        <div class="repeater">
                            <fieldset
                                class="noPaddingBottom"
                                v-if="(shards.pods.hasOwnProperty('customVolumes') && shards.pods.customVolumes.length)"
                                data-fieldset="spec.shards.pods.customVolumes"
                            >
                                <template v-for="(vol, index) in shards.pods.customVolumes">
                                    <div class="section" :key="index">
                                        <div class="header">
                                            <h4>Volume #{{ index + 1 }}{{ !isNull(vol.name) ? (': ' + vol.name) : '' }}</h4>
                                            <a class="addRow delete" @click="spliceArray(shards.pods.customVolumes, index); spliceArray(customVolumesType.shards, index)">Delete</a>
                                        </div>
                                                        
                                        <div class="row-50">
                                            <div class="col">
                                                <label>Name</label>
                                                <input :required="(customVolumesType.shards[index] !== null)" v-model="vol.name" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].name'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.name')"></span>
                                            </div>
                                            
                                            <div class="col">
                                                <label>Type</label>
                                                <select v-model="customVolumesType.shards[index]" @change="initCustomVolume(index, shards.pods.customVolumes, customVolumesType.shards)" :data-field="'spec.shards.pods.customVolumes[' + index + '].type'">
                                                    <option :value="null" selected>Choose one...</option>
                                                    <option value="emptyDir">Empty Directory</option>
                                                    <option value="configMap">ConfigMap</option>
                                                    <option value="secret">Secret</option>
                                                </select>
                                                <span class="helpTooltip" data-tooltip="Specifies the type of volume to be used"></span>
                                            </div>
                                        </div>

                                        <template v-if="(customVolumesType.shards[index] == 'emptyDir')">
                                            <div class="header">
                                                <h5 for="spec.shards.pods.customVolumes.emptyDir">
                                                    Empty Directory
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.emptyDir')"></span>
                                                </h5>
                                            </div>
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Medium</label>
                                                    <input v-model="vol.emptyDir.medium" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].emptyDir.medium'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.emptyDir.properties.medium')"></span>
                                                </div>
                                                <div class="col">
                                                    <label>Size Limit</label>
                                                    <input v-model="vol.emptyDir.sizeLimit" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].emptyDir.sizeLimit'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.emptyDir.properties.sizeLimit')"></span>
                                                </div>
                                            </div>

                                        </template>
                                        <template v-else-if="(customVolumesType.shards[index] == 'configMap')">
                                            <div class="header">
                                                <h5 for="spec.shards.pods.customVolumes.configMap">
                                                    ConfigMap
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap')"></span>
                                                </h5>
                                            </div>
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Name</label>
                                                    <input v-model="vol.configMap.name" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].configMap.name'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.name')"></span>
                                                </div>
                                                <div class="col">                    
                                                    <label :for="'spec.shards.pods.customVolumes[' + index + '].configMap.optional'">
                                                        Optional
                                                    </label>  
                                                    <label :for="'spec.shards.pods.customVolumes[' + index + '].configMap.optional'" class="switch yes-no">
                                                        Enable
                                                        <input type="checkbox" :id="'spec.shards.pods.customVolumes[' + index + '].configMap.optional'" v-model="vol.configMap.optional" data-switch="NO" :data-field="'spec.shards.pods.customVolumes[' + index + '].configMap.optional'">
                                                    </label>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.optional')"></span>
                                                </div>
                                                <div class="col">
                                                    <label>Default Mode</label>
                                                    <input type="number" v-model="vol.configMap.defaultMode" min="0" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].configMap.defaultMode'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.defaultMode')"></span>
                                                </div>
                                            </div>

                                            <br/><br/>
                                            <div class="header">
                                                <h6 for="spec.shards.pods.customVolumes.configMap.items">
                                                    Items
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.items')"></span>
                                                </h6>
                                            </div>
                                            <fieldset
                                                class="noMargin"
                                                :data-field="'spec.shards.pods.customVolumes[' + index + '].configMap.items'"
                                                v-if="vol.configMap.items.length"
                                            >
                                                <template v-for="(item, itemIndex) in vol.configMap.items">
                                                    <div class="section" :key="itemIndex" :data-field="'spec.shards.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + ']'">
                                                        <div class="header">
                                                            <h4>Item #{{ itemIndex + 1 }}</h4>
                                                            <a class="addRow delete" @click="spliceArray(vol.configMap.items, itemIndex)">Delete</a>
                                                        </div>
                                                                        
                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label>Key</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.key" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].key'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.items.items.properties.key')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Mode</label>
                                                                <input type="number" v-model="item.mode" min="0" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].mode'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.items.items.properties.mode')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Path</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.path" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].path'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.configMap.properties.items.items.properties.path')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </template>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!vol.configMap.items.length && 'topBorder'">
                                                <a
                                                    class="addRow"
                                                    @click="vol.configMap.items.push({
                                                        key: null,
                                                        mode: null,
                                                        path: null,
                                                    })"
                                                >
                                                    Add Item
                                                </a>
                                            </div>
                                        </template>

                                        <template v-else-if="(customVolumesType.shards[index] == 'secret')">
                                            <div class="header">
                                                <h5 for="spec.shards.pods.customVolumes.secret">
                                                    Secret
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret')"></span>
                                                </h5>
                                            </div>
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Secret Name</label>
                                                    <input v-model="vol.secret.secretName" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].secret.secretName'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.secretName')"></span>
                                                </div>
                                                <div class="col">                    
                                                    <label :for="'spec.shards.pods.customVolumes[' + index + '].secret.optional'">
                                                        Optional
                                                    </label>  
                                                    <label :for="'spec.shards.pods.customVolumes[' + index + '].secret.optional'" class="switch yes-no">
                                                        Enable
                                                        <input type="checkbox" :id="'spec.shards.pods.customVolumes[' + index + '].secret.optional'" v-model="vol.secret.optional" data-switch="NO" :data-field="'spec.shards.pods.customVolumes[' + index + '].secret.optional'">
                                                    </label>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.optional')"></span>
                                                </div>
                                                <div class="col">
                                                    <label>Default Mode</label>
                                                    <input type="number" v-model="vol.secret.defaultMode" min="0" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].secret.defaultMode'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.defaultMode')"></span>
                                                </div>
                                            </div>

                                            <br/><br/>
                                            <div class="header">
                                                <h6 for="spec.shards.pods.customVolumes.secret.items">
                                                    Items
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.items')"></span>
                                                </h6>
                                            </div>
                                            <fieldset
                                                class="noMargin"
                                                :data-field="'spec.shards.pods.customVolumes[' + index + '].secret.items'"
                                                v-if="vol.secret.items.length"
                                            >
                                                <template v-for="(item, itemIndex) in vol.secret.items">
                                                    <div class="section" :key="itemIndex" :data-field="'spec.shards.pods.customVolumes[' + index + '].secret.items[' + itemIndex + ']'">
                                                        <div class="header">
                                                            <h4>Item #{{ itemIndex + 1 }}</h4>
                                                            <a class="addRow delete" @click="spliceArray(vol.secret.items, itemIndex)">Delete</a>
                                                        </div>
                                                                        
                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label>Key</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.key" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].key'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.items.items.properties.key')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Mode</label>
                                                                <input type="number" v-model="item.mode" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].mode'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.items.items.properties.mode')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Path</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.path" autocomplete="off" :data-field="'spec.shards.pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].path'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customVolumes.secret.properties.items.items.properties.path')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </template>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!vol.secret.items.length && 'topBorder'">
                                                <a
                                                    class="addRow"
                                                    @click="vol.secret.items.push({
                                                        key: '',
                                                        mode: '',
                                                        path: '',
                                                    })"
                                                >
                                                    Add Item
                                                </a>
                                            </div>
                                        </template>
                                    </div>
                                </template>
                            </fieldset>
                            <div class="fieldsetFooter" :class="(!shards.pods.hasOwnProperty('customVolumes') || (shards.pods.hasOwnProperty('customVolumes') && !shards.pods.customVolumes.length) ) && 'topBorder'">
                                <a 
                                    class="addRow"
                                    @click="
                                        customVolumesType.shards.push(null);
                                        (!shards.pods.hasOwnProperty('customVolumes') && (shards.pods['customVolumes'] = []) );
                                        shards.pods.customVolumes.push({ name: null});
                                        formHash = (+new Date).toString();
                                    "
                                >
                                    Add Volume
                                </a>
                            </div>
                        </div>

                        <br/><br/><br/>

                        <template v-if="!editMode || (shards.pods.hasOwnProperty('customInitContainers') && shards.pods.customInitContainers.length)">
                            <div class="header">
                                <h3 for="spec.shards.pods.customInitContainers">
                                    Custom Init Containers
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers')"></span>
                                </h3>
                            </div>
                            <p>A list of custom application init containers that run within the cluster’s Pods</p>

                            <br/>
                            
                            <div class="repeater">
                                <fieldset
                                    v-if="shards.pods.customInitContainers.length"
                                    data-fieldset="spec.shards.pods.customInitContainers"
                                >
                                    <template v-for="(container, index) in shards.pods.customInitContainers">
                                        <div class="section" :key="index" :data-field="'spec.shards.pods.customInitContainers[' + index + ']'">
                                            <div class="header">
                                                <h4>Init Container #{{ index + 1 }}{{ !isNull(container.name) ? (': ' + container.name) : '' }}</h4>
                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(shards.pods.customInitContainers, index)">Delete</a>
                                            </div>
                                                            
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Name</label>
                                                    <input :disabled="editMode" :required="!isNull(container.image) || !isNull(container.imagePullPolicy) || !isNull(container.workingDir)" v-model="container.name" autocomplete="off" :data-field="'spec.shards.pods.customInitContainers[' + index + '].name'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.name')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image</label>
                                                    <input v-model="container.image" autocomplete="off" :data-field="'spec.shards.pods.customInitContainers[' + index + '].image'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.image')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image Pull Policy</label>
                                                    <input :disabled="editMode" v-model="container.imagePullPolicy" autocomplete="off" :data-field="'spec.shards.pods.customInitContainers[' + index + '].imagePullPolicy'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.imagePullPolicy')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Working Directory</label>
                                                    <input :disabled="editMode" v-model="container.workingDir" autocomplete="off" :data-field="'spec.shards.pods.customInitContainers[' + index + '].workingDir'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.workingDir')"></span>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('args')">
                                                    <fieldset :data-field="'spec.shards.pods.customInitContainers[' + index + '].args'">
                                                        <div class="header" :class="[container.args.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.shards.pods.customInitContainers[' + index + '].args'">
                                                                Arguments
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.args')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(arg, argIndex) in container.args">
                                                            <div :key="'arg-' + argIndex" class="inputContainer" :class="(container.args.length !== (argIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'arg-' + argIndex" 
                                                                    v-model="container.args[argIndex]" 
                                                                    :data-field="'spec.shards.pods.customInitContainers[' + index + '].args[' + argIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.args, argIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.args.push(null)">Add Argument</a>
                                                    </div>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('command')">
                                                    <fieldset :data-field="'spec.shards.pods.customInitContainers[' + index + '].command'">
                                                        <div class="header" :class="[container.command.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.shards.pods.customInitContainers[' + index + '].command'">
                                                                Command
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.command')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(command, commandIndex) in container.command">
                                                            <div :key="'command-' + commandIndex" class="inputContainer" :class="(container.command.length !== (commandIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'command-' + commandIndex" 
                                                                    v-model="container.command[commandIndex]" 
                                                                    :data-field="'spec.shards.pods.customInitContainers[' + index + '].command[' + commandIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.command, commandIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.command.push(null)">Add Command</a>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="repeater marginBottom marginTop" v-if="!editMode || container.hasOwnProperty('env')">
                                                <fieldset :data-field="'spec.shards.pods.customInitContainers[' + index + '].env'">
                                                    <div class="header" :class="!container.env.length && 'noMargin'">
                                                        <h5 :for="'spec.shards.pods.customInitContainers[' + index + '].env'">
                                                            Environment Variables
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.env')"></span> 
                                                        </h5>
                                                    </div>
                                                    <div class="variable" v-if="container.env.length">
                                                        <div class="row" v-for="(env, envIndex) in container.env" :data-field="'spec.shards.pods.customInitContainers[' + index + '].env[' + envIndex + ']'">
                                                            <label>Name</label>
                                                            <input :required="!isNull(env.value)" :disabled="editMode" class="label" v-model="env.name" autocomplete="off" :data-field="'spec.shards.pods.customInitContainers[' + index + '].env[' + envIndex + '].name'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input :disabled="editMode" class="labelValue" v-model="env.value" autocomplete="off" :data-field="'spec.shards.pods.customInitContainers[' + index + '].env[' + envIndex + '].value'">

                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.env, envIndex)">Delete</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.env.push({ name: null, value: null})">Add Variable</a>
                                                </div>
                                            </div>

                                            <br/>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('ports')">
                                                <div class="header">
                                                    <h5>
                                                        Ports
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater marginBottom">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        data-field="spec.shards.pods.customInitContainers.ports"
                                                        v-if="container.ports.length"
                                                    >
                                                        <div class="section" v-for="(port, portIndex) in container.ports" :data-field="'spec.shards.pods.customInitContainers[' + index + '].ports[' + portIndex + ']'">
                                                            <div class="header">
                                                                <h6>Port #{{ portIndex + 1 }}{{ !isNull(port.name) ? (': ' + port.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.ports, portIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.ports.name">Name</label>  
                                                                    <input :disabled="editMode" v-model="port.name" :data-field="'spec.shards.pods.customInitContainers[' + index + '].ports[' + portIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.ports.hostIP">Host IP</label>  
                                                                    <input :disabled="editMode" v-model="port.hostIP" :data-field="'spec.shards.pods.customInitContainers[' + index + '].ports[' + portIndex + '].hostIP'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.hostIP')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.ports.hostPort">Host Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.hostPort" :data-field="'spec.shards.pods.customInitContainers[' + index + '].ports[' + portIndex + '].hostPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.hostPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.ports.containerPort">Container Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.containerPort" :data-field="'spec.shards.pods.customInitContainers[' + index + '].ports[' + portIndex + '].containerPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.containerPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.ports.protocol">Protocol</label>  
                                                                    <select :disabled="editMode" v-model="port.protocol" :data-field="'spec.shards.pods.customInitContainers[' + index + '].ports[' + portIndex + '].protocol'">
                                                                        <option :value="nullVal" selected>Choose one...</option>
                                                                        <option>TCP</option>
                                                                        <option>UDP</option>
                                                                        <option>SCTP</option>
                                                                    </select>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.ports.items.properties.protocol')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('ports') && !container.ports.length) && 'topBorder'">
                                                        <a class="addRow" @click="!container.hasOwnProperty('ports') && (container['ports'] = []); container.ports.push({
                                                            name: null,
                                                            hostIP: null,
                                                            hostPort: null,
                                                            containerPort: null,
                                                            protocol: null
                                                        })">
                                                            Add Port
                                                        </a>
                                                    </div>
                                                </div>
                                                <br/>
                                            </template>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('volumeMounts')">
                                                <div class="header">
                                                    <h5>
                                                        Volume Mounts
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        :data-field="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts'"
                                                        v-if="container.hasOwnProperty('volumeMounts') && container.volumeMounts.length"
                                                    >
                                                        <div class="section" v-for="(mount, mountIndex) in container.volumeMounts" :data-field="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + ']'">
                                                            <div class="header">
                                                                <h6>Mount #{{ mountIndex + 1 }}{{ !isNull(mount.name) ? (': ' + mount.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.volumeMounts, mountIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.volumeMounts.name">Name</label>  
                                                                    <input :required="!isNull(mount.mountPath)" :disabled="editMode" v-model="mount.name" :data-field="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">                    
                                                                    <label :for="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts.readOnly'">
                                                                        Read Only
                                                                    </label>  
                                                                    <label :disabled="editMode" :for="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" class="switch yes-no">
                                                                        Enable
                                                                        <input :disabled="editMode" type="checkbox" :id="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" v-model="mount.readOnly" data-switch="NO" :data-field="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'">
                                                                    </label>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.volumeMounts.mountPath">Mount Path</label>  
                                                                    <input :required="!isNull(mount.name)" :disabled="editMode" v-model="mount.mountPath" :data-field="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.volumeMounts.mountPropagation">Mount Propagation</label>  
                                                                    <input :disabled="editMode" v-model="mount.mountPropagation" :data-field="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPropagation'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.volumeMounts.subPath">Sub Path</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPathExpr') && !isNull(mount.subPathExpr))" v-model="mount.subPath" :data-field="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].subPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.subPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customInitContainers.volumeMounts.subPathExpr">Sub Path Expr</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPath') && !isNull(mount.subPath))" v-model="mount.subPathExpr" :data-field="'spec.shards.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].subPathExpr'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customInitContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('volumeMounts') && !container.volumeMounts.length) && 'topBorder'">
                                                        <a
                                                            class="addRow" 
                                                            @click="
                                                                !container.hasOwnProperty('volumeMounts') && (container['volumeMounts'] = []);
                                                                container.volumeMounts.push({
                                                                    mountPath: null,
                                                                    mountPropagation: null,
                                                                    name: null,
                                                                    readOnly: false,
                                                                    subPath: null,
                                                                    subPathExpr: null
                                                                });
                                                                formHash = (+new Date).toString();
                                                            "
                                                        >
                                                            Add Volume
                                                        </a>
                                                    </div>
                                                </div>
                                            </template>
                                        </div>
                                    </template>
                                </fieldset>
                                <div v-if="!editMode" class="fieldsetFooter" :class="!shards.pods.customInitContainers.length && 'topBorder'">
                                    <a 
                                        class="addRow"
                                        @click="shards.pods.customInitContainers.push({
                                            name: null,
                                            image: null,
                                            imagePullPolicy: null,
                                            args: [null],
                                            command: [null],
                                            workingDir: null,
                                            env: [ { name: null, value: null } ],
                                            ports: [{
                                                containerPort: null,
                                                hostIP: null,
                                                hostPort: null,
                                                name: null,
                                                protocol: null
                                            }],
                                            volumeMounts: [{
                                                mountPath: null,
                                                mountPropagation: null,
                                                name: null,
                                                readOnly: false,
                                                subPath: null,
                                                subPathExpr: null,
                                            }]
                                        })"
                                    >
                                        Add Init Container
                                    </a>
                                </div>
                            </div>

                            <br/><br/><br/>

                        </template>

                        <template v-if="!editMode || (shards.pods.hasOwnProperty('customContainers') && shards.pods.customContainers.length)">
                            <div class="header">
                                <h3 for="spec.shards.pods.customContainers">
                                    Custom Containers
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers')"></span>
                                </h3>
                            </div>
                            <p>A list of custom application containers that run within the cluster’s Pods</p>

                            <br/>
                            
                            <div class="repeater">
                                <fieldset
                                    v-if="shards.pods.customContainers.length"
                                    data-fieldset="spec.shards.pods.customContainers"
                                >
                                    <template v-for="(container, index) in shards.pods.customContainers">
                                        <div class="section" :key="index" :data-field="'spec.shards.pods.customContainers[' + index + ']'">
                                            <div class="header">
                                                <h4>Container #{{ index + 1 }}{{ !isNull(container.name) ? (': ' + container.name) : '' }}</h4>
                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(shards.pods.customContainers, index)">Delete</a>
                                            </div>
                                                            
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Name</label>
                                                    <input :disabled="editMode" :required="!isNull(container.image) || !isNull(container.imagePullPolicy) || !isNull(container.workingDir)" v-model="container.name" autocomplete="off" :data-field="'spec.shards.pods.customContainers[' + index + '].name'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.name')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image</label>
                                                    <input v-model="container.image" autocomplete="off" :data-field="'spec.shards.pods.customContainers[' + index + '].image'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.image')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image Pull Policy</label>
                                                    <input :disabled="editMode" v-model="container.imagePullPolicy" autocomplete="off" :data-field="'spec.shards.pods.customContainers[' + index + '].imagePullPolicy'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.imagePullPolicy')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Working Directory</label>
                                                    <input :disabled="editMode" v-model="container.workingDir" autocomplete="off" :data-field="'spec.shards.pods.customContainers[' + index + '].workingDir'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.workingDir')"></span>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('args')">
                                                    <fieldset :data-field="'spec.shards.pods.customContainers[' + index + '].args'">
                                                        <div class="header" :class="[container.args.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.shards.pods.customContainers[' + index + '].args'">
                                                                Arguments
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.args')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(arg, argIndex) in container.args">
                                                            <div :key="'arg-' + argIndex" class="inputContainer" :class="(container.args.length !== (argIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'arg-' + argIndex" 
                                                                    v-model="container.args[argIndex]" 
                                                                    :data-field="'spec.shards.pods.customContainers[' + index + '].args[' + argIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.args, argIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.args.push(null)">Add Argument</a>
                                                    </div>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('command')">
                                                    <fieldset :data-field="'spec.shards.pods.customContainers[' + index + '].command'">
                                                        <div class="header" :class="[container.command.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.shards.pods.customContainers[' + index + '].command'">
                                                                Command
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.command')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(command, commandIndex) in container.command">
                                                            <div :key="'command-' + commandIndex" class="inputContainer" :class="(container.command.length !== (commandIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'command-' + commandIndex" 
                                                                    v-model="container.command[commandIndex]" 
                                                                    :data-field="'spec.shards.pods.customContainers[' + index + '].command[' + commandIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.command, commandIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.command.push(null)">Add Command</a>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="repeater marginBottom marginTop" v-if="!editMode || container.hasOwnProperty('env')">
                                                <fieldset :data-field="'spec.shards.pods.customContainers[' + index + '].env'">
                                                    <div class="header" :class="!container.env.length && 'noMargin'">
                                                        <h5 :for="'spec.shards.pods.customContainers[' + index + '].env'">
                                                            Environment Variables
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.env')"></span> 
                                                        </h5>
                                                    </div>
                                                    <div class="variable" v-if="container.env.length">
                                                        <div class="row" v-for="(env, envIndex) in container.env" :data-field="'spec.shards.pods.customContainers[' + index + '].env[' + envIndex + ']'">
                                                            <label>Name</label>
                                                            <input :required="!isNull(env.value)" :disabled="editMode" class="label" v-model="env.name" autocomplete="off" :data-field="'spec.shards.pods.customContainers[' + index + '].env[' + envIndex + '].name'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input :disabled="editMode" class="labelValue" v-model="env.value" autocomplete="off" :data-field="'spec.shards.pods.customContainers[' + index + '].env[' + envIndex + '].value'">

                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.env, envIndex)">Delete</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.env.push({ name: null, value: null})">Add Variable</a>
                                                </div>
                                            </div>

                                            <br/>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('ports')">
                                                <div class="header">
                                                    <h5>
                                                        Ports
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater marginBottom">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        data-field="spec.shards.pods.customContainers.ports"
                                                        v-if="container.ports.length"
                                                    >
                                                        <div class="section" v-for="(port, portIndex) in container.ports" :data-field="'spec.shards.pods.customContainers[' + index + '].ports[' + portIndex + ']'">
                                                            <div class="header">
                                                                <h6>Port #{{ portIndex + 1 }}{{ !isNull(port.name) ? (': ' + port.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.ports, portIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.ports.name">Name</label>  
                                                                    <input :disabled="editMode" v-model="port.name" :data-field="'spec.shards.pods.customContainers[' + index + '].ports[' + portIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.ports.hostIP">Host IP</label>  
                                                                    <input :disabled="editMode" v-model="port.hostIP" :data-field="'spec.shards.pods.customContainers[' + index + '].ports[' + portIndex + '].hostIP'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.hostIP')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.ports.hostPort">Host Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.hostPort" :data-field="'spec.shards.pods.customContainers[' + index + '].ports[' + portIndex + '].hostPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.hostPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.ports.containerPort">Container Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.containerPort" :data-field="'spec.shards.pods.customContainers[' + index + '].ports[' + portIndex + '].containerPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.containerPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.ports.protocol">Protocol</label>  
                                                                    <select :disabled="editMode" v-model="port.protocol" :data-field="'spec.shards.pods.customContainers[' + index + '].ports[' + portIndex + '].protocol'">
                                                                        <option :value="nullVal" selected>Choose one...</option>
                                                                        <option>TCP</option>
                                                                        <option>UDP</option>
                                                                        <option>SCTP</option>
                                                                    </select>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.ports.items.properties.protocol')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('ports') && !container.ports.length) && 'topBorder'">
                                                        <a class="addRow" @click="!container.hasOwnProperty('ports') && (container['ports'] = []); container.ports.push({
                                                            name: null,
                                                            hostIP: null,
                                                            hostPort: null,
                                                            containerPort: null,
                                                            protocol: null
                                                        })">
                                                            Add Port
                                                        </a>
                                                    </div>
                                                </div>
                                                <br/>
                                            </template>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('volumeMounts')">
                                                <div class="header">
                                                    <h5>
                                                        Volume Mounts
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        :data-field="'spec.shards.pods.customContainers[' + index + '].volumeMounts'"
                                                        v-if="container.hasOwnProperty('volumeMounts') && container.volumeMounts.length"
                                                    >
                                                        <div class="section" v-for="(mount, mountIndex) in container.volumeMounts" :data-field="'spec.shards.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + ']'">
                                                            <div class="header">
                                                                <h6>Mount #{{ mountIndex + 1 }}{{ !isNull(mount.name) ? (': ' + mount.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.volumeMounts, mountIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.volumeMounts.name">Name</label>  
                                                                    <input :required="!isNull(mount.mountPath)" :disabled="editMode" v-model="mount.name" :data-field="'spec.shards.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">                    
                                                                    <label :for="'spec.shards.pods.customContainers[' + index + '].volumeMounts.readOnly'">
                                                                        Read Only
                                                                    </label>  
                                                                    <label :disabled="editMode" :for="'spec.shards.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" class="switch yes-no">
                                                                        Enable
                                                                        <input :disabled="editMode" type="checkbox" :id="'spec.shards.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" v-model="mount.readOnly" data-switch="NO" :data-field="'spec.shards.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'">
                                                                    </label>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.volumeMounts.mountPath">Mount Path</label>  
                                                                    <input :required="!isNull(mount.name)" :disabled="editMode" v-model="mount.mountPath" :data-field="'spec.shards.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.volumeMounts.mountPropagation">Mount Propagation</label>  
                                                                    <input :disabled="editMode" v-model="mount.mountPropagation" :data-field="'spec.shards.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPropagation'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.volumeMounts.subPath">Sub Path</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPathExpr') && !isNull(mount.subPathExpr))" v-model="mount.subPath" :data-field="'spec.shards.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].subPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.subPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.pods.customContainers.volumeMounts.subPathExpr">Sub Path Expr</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPath') && !isNull(mount.subPath))" v-model="mount.subPathExpr" :data-field="'spec.shards.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].subPathExpr'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.customContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('volumeMounts') && !container.volumeMounts.length) && 'topBorder'">
                                                        <a
                                                            class="addRow"
                                                            @click="
                                                                !container.hasOwnProperty('volumeMounts') && (container['volumeMounts'] = []); container.volumeMounts.push({
                                                                    mountPath: null,
                                                                    mountPropagation: null,
                                                                    name: null,
                                                                    readOnly: false,
                                                                    subPath: null,
                                                                    subPathExpr: null
                                                                });
                                                                formHash = (+new Date).toString();
                                                            "
                                                        >
                                                            Add Volume
                                                        </a>
                                                    </div>
                                                </div>
                                            </template>
                                        </div>
                                    </template>
                                </fieldset>
                                <div v-if="!editMode" class="fieldsetFooter" :class="!shards.pods.customContainers.length && 'topBorder'">
                                    <a 
                                        class="addRow"
                                        @click="shards.pods.customContainers.push({
                                            name: null,
                                            image: null,
                                            imagePullPolicy: null,
                                            args: [null],
                                            command: [null],
                                            workingDir: null,
                                            env: [ { name: null, value: null } ],
                                            ports: [{
                                                containerPort: null,
                                                hostIP: null,
                                                hostPort: null,
                                                name: null,
                                                protocol: null
                                            }],
                                            volumeMounts: [{
                                                mountPath: null,
                                                mountPropagation: null,
                                                name: null,
                                                readOnly: false,
                                                subPath: null,
                                                subPathExpr: null,
                                            }]
                                        })"
                                    >
                                        Add Container
                                    </a>
                                </div>
                            </div>

                            <br/><br/><br/>

                        </template>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.shards == 'pods-replication')" class="step active" data-fieldset="shards.pods-replication">
                    <div class="header">
                        <h2>Replication</h2>
                    </div>

                    <div class="fields">                    
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.shards.replication.mode">Mode</label>
                                <select v-model="shards.replication.mode" required data-field="spec.shards.replication.mode" @change="['sync', 'strict-sync'].includes(shards.replication.mode) ? (shards.replication['syncInstances'] = 1) : ((shards.replication.hasOwnProperty('syncInstances') && delete shards.replication.syncInstances) )">    
                                    <option selected>async</option>
                                    <option>sync</option>
                                    <option>strict-sync</option>
                                    <option>sync-all</option>
                                    <option>strict-sync-all</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.replication.mode')"></span>
                            </div>

                            <div class="col" v-if="['sync', 'strict-sync'].includes(shards.replication.mode)">
                                <label for="spec.shards.replication.syncInstances">Sync Instances</label>
                                <input type="number" min="1" :max="(shards.instancesPerCluster - 1)" v-model="shards.replication.syncInstances" data-field="spec.shards.replication.syncInstances">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.replication.syncInstances')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.shards == 'services')" class="step active" data-fieldset="shards.services">
                    <div class="header">
                        <h2>Customize generated Kubernetes service</h2>
                    </div>

                    <div class="fields">                    
                        <div class="header">
                            <h3 for="spec.postgresServices.shards.primaries">
                                Primaries Service
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.primaries')"></span>
                            </h3>
                        </div>
                        
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.postgresServices.shards.primaries.enabled">Service</label>  
                                <label for="postgresServicesPrimaries" class="switch yes-no" data-field="spec.postgresServices.shards.primaries.enabled">Enable<input type="checkbox" id="postgresServicesPrimaries" v-model="postgresServices.shards.primaries.enabled" data-switch="YES"></label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.primaries.enabled')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.postgresServices.shards.primaries.type">Type</label>
                                <select v-model="postgresServices.shards.primaries.type" required data-field="spec.postgresServices.shards.primaries.type">    
                                    <option>ClusterIP</option>
                                    <option>LoadBalancer</option>
                                    <option>NodePort</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.primaries.type')"></span>
                            </div>

                            <div class="col">
                                <label>Load Balancer IP</label>
                                <input 
                                    v-model="postgresServices.shards.primaries.loadBalancerIP" 
                                    autocomplete="off" 
                                    data-field="spec.postgresServices.shards.primaries.loadBalancerIP">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.primaries.loadBalancerIP')"></span>
                            </div>
                        </div>

                        <div class="repeater sidecars">
                            <div class="header">
                                <h4 for="spec.postgresServices.shards.customPorts">
                                    Custom Ports
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts')"></span>
                                </h4>
                            </div>
                            <fieldset
                                data-field="spec.postgresServices.shards.customPorts"
                                v-if="postgresServices.shards.hasOwnProperty('customPorts') && postgresServices.shards.customPorts.length"
                            >
                                <div class="section" v-for="(port, index) in postgresServices.shards.customPorts">
                                    <div class="header">
                                        <h5>Port #{{ index + 1 }}</h5>
                                        <a class="addRow delete" @click="spliceArray(postgresServices.shards.customPorts, index)">Delete</a>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.customPorts.appProtocol">Application Protocol</label>  
                                            <input v-model="port.appProtocol" :data-field="'spec.postgresServices.shards.customPorts[' + index + '].appProtocol'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.appProtocol')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.customPorts.name">Name</label>  
                                            <input v-model="port.name" :data-field="'spec.postgresServices.shards.customPorts[' + index + '].name'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.name')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.customPorts.nodePort">Node Port</label>  
                                            <input type="number" v-model="port.nodePort" :data-field="'spec.postgresServices.shards.customPorts[' + index + '].nodePort'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.nodePort')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.customPorts.port">Port</label>  
                                            <input 
                                                type="number"
                                                v-model="port.port"
                                                :data-field="'spec.postgresServices.shards.customPorts[' + index + '].port'"
                                                :required="(port.appProtocol != null) || (port.name != null) || (port.nodePort != null) || (port.protocol != null) || (port.targetPort != null)"
                                                autocomplete="off"
                                            >
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.port')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.customPorts.protocol">Protocol</label>  
                                            <select v-model="port.protocol" :data-field="'spec.postgresServices.shards.customPorts[' + index + '].protocol'">
                                                <option :value="nullVal" selected>Choose one...</option>
                                                <option>TCP</option>
                                                <option>UDP</option>
                                                <option>SCTP</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.protocol')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.customPorts.targetPort">Target Port</label>  
                                            <input v-model="port.targetPort" :data-field="'spec.postgresServices.shards.customPorts[' + index + '].targetPort'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.customPorts.targetPort')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="(postgresServices.shards.hasOwnProperty('customPorts') && !postgresServices.shards.customPorts.length) && 'topBorder'">
                                <a class="addRow" @click="!postgresServices.shards.hasOwnProperty('customPorts') && (postgresServices.shards['customPorts'] = []); postgresServices.shards.customPorts.push({
                                    appProtocol: null,
                                    name: null,
                                    nodePort: null,
                                    port: null,
                                    protocol: null,
                                    targetPort: null
                                })">
                                    Add Port
                                </a>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.shards == 'metadata')"  class="step active podsMetadata" data-fieldset="shards.metadata">
                    <div class="header">
                        <h2>Metadata</h2>
                    </div>

                    <div class="fields">
                        <div class="repeater">
                            <div class="header">
                                <h3 for="spec.shards.metadata.labels">
                                    Labels
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.labels')"></span> 
                                </h3>
                            </div>

                            <fieldset data-field="spec.shards.metadata.labels.clusterPods">
                                <div class="header" :class="( !hasProp(shards, 'metadata.labels.clusterPods') || !shards.metadata.labels.clusterPods.length ) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.metadata.labels.clusterPods">
                                        Cluster Pods
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.labels.clusterPods')"></span> 
                                    </h3>
                                </div>
                                <div class="metadata" v-if="hasProp(shards, 'metadata.labels.clusterPods') && shards.metadata.labels.clusterPods.length">
                                    <div class="row" v-for="(field, index) in shards.metadata.labels.clusterPods">
                                        <label>Label</label>
                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.shards.metadata.labels.clusterPods[' + index + '].label'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.metadata.labels.clusterPods[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.metadata.labels.clusterPods, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards, 'metadata.labels.clusterPods', { label: '', value: ''})">Add Label</a>
                            </div>
                        </div>

                        <br/><hr/><br/>

                        
                        <div class="header">
                            <h3 for="spec.shards.metadata.annotations">
                                Annotations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations')"></span>
                            </h3>
                        </div>

                        <div class="repeater">
                            <fieldset data-field="spec.shards.metadata.annotations.allResources">
                                <div class="header" :class="(!hasProp(shards, 'metadata.annotations.allResources') || !shards.metadata.annotations.allResources.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.metadata.annotations.allResources">
                                        All Resources
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.allResources')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(shards, 'metadata.annotations.allResources') && shards.metadata.annotations.allResources.length)">
                                    <div class="row" v-for="(field, index) in shards.metadata.annotations.allResources">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.metadata.annotations.allResources[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.metadata.annotations.allResources[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.metadata.annotations.allResources, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards, 'metadata.annotations.allResources', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>
                        
                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.shards.metadata.annotations.clusterPods">
                                <div class="header" :class="(!hasProp(shards, 'metadata.annotations.clusterPods') || !shards.metadata.annotations.clusterPods.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.metadata.annotations.clusterPods">
                                        Cluster Pods
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.clusterPods')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(shards, 'metadata.annotations.clusterPods') && shards.metadata.annotations.clusterPods.length)">
                                    <div class="row" v-for="(field, index) in shards.metadata.annotations.clusterPods">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.metadata.annotations.clusterPods[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.metadata.annotations.clusterPods[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.metadata.annotations.clusterPods, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards, 'metadata.annotations.clusterPods', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.shards.metadata.annotations.services">
                                <div class="header" :class="(!hasProp(shards, 'metadata.annotations.services') || !shards.metadata.annotations.services.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.metadata.annotations.services">
                                        Services
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.services')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(shards, 'metadata.annotations.services') && shards.metadata.annotations.services.length)">
                                    <div class="row" v-for="(field, index) in shards.metadata.annotations.services">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.metadata.annotations.services[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.metadata.annotations.services[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.metadata.annotations.services, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards, 'metadata.annotations.services', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.shards.metadata.annotations.primaryService">
                                <div class="header" :class="(!hasProp(shards, 'metadata.annotations.primaryService') || !shards.metadata.annotations.primaryService.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.metadata.annotations.primaryService">
                                        Primary Service 
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.primaryService')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(shards, 'metadata.annotations.primaryService') && shards.metadata.annotations.primaryService.length)">
                                    <div class="row" v-for="(field, index) in shards.metadata.annotations.primaryService">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.metadata.annotations.primaryService[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.metadata.annotations.primaryService[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.metadata.annotations.primaryService, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards, 'metadata.annotations.primaryService', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.shards.metadata.annotations.replicasService">
                                <div class="header" :class="(!hasProp(shards, 'metadata.annotations.replicasService') || !shards.metadata.annotations.replicasService.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.metadata.annotations.replicasService">
                                        Replicas Service
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.metadata.annotations.replicasService')"></span>
                                    </h3>
                                </div>
                                <div class="annotation repeater" v-if="(hasProp(shards, 'metadata.annotations.replicasService') && shards.metadata.annotations.replicasService.length)">
                                    <div class="row" v-for="(field, index) in shards.metadata.annotations.replicasService">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.metadata.annotations.replicasService[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.metadata.annotations.replicasService[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.metadata.annotations.replicasService, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards, 'metadata.annotations.replicasService', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.shards == 'scheduling')" class="step active podsMetadata" id="podsSchedulingShards" data-fieldset="shards.scheduling">
                    <div class="header">
                        <h2>Pods Scheduling</h2>
                    </div>
                    
                    <div class="fields">
                        <div class="repeater">
                            <div class="header">
                                <h3 for="spec.shards.pods.scheduling.nodeSelector">
                                    Node Selectors
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeSelector')"></span>
                                </h3>
                            </div>
                            <fieldset v-if="(hasProp(shards, 'pods.scheduling.nodeSelector') && shards.pods.scheduling.nodeSelector.length)" data-field="spec.shards.pods.scheduling.nodeSelector">
                                <div class="scheduling">
                                    <div class="row" v-for="(field, index) in shards.pods.scheduling.nodeSelector">
                                        <label>Label</label>
                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.shards.pods.scheduling.nodeSelector[' + index + '].label'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.pods.scheduling.nodeSelector[' + index + '].value'">
                                        
                                        <a class="addRow" @click="spliceArray(shards.pods.scheduling.nodeSelector, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( (!hasProp(shards, 'pods.scheduling.nodeSelector') || !shards.pods.scheduling.nodeSelector.length) && 'topBorder' )">
                                <a class="addRow" @click="pushElement(shards, 'pods.scheduling.nodeSelector', { label: '', value: ''})">Add Node Selector</a>
                            </div>
                        </div>

                        <br/><br/>
                    
                        <div class="header">
                            <h3 for="spec.shards.pods.scheduling.tolerations">
                                Node Tolerations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations')"></span>
                            </h3>
                        </div>
                
                        <div class="scheduling repeater">
                            <fieldset v-if="(hasProp(shards, 'pods.scheduling.tolerations') && shards.pods.scheduling.tolerations.length)" data-field="spec.shards.pods.scheduling.tolerations">
                                <div class="section" v-for="(field, index) in shards.pods.scheduling.tolerations">
                                    <div class="header">
                                        <h4 for="spec.shards.pods.scheduling.tolerations">Toleration #{{ index+1 }}</h4>
                                        <a class="addRow del" @click="spliceArray(shards.pods.scheduling.tolerations, index)">Delete</a>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label :for="'spec.shards.pods.scheduling.tolerations[' + index + '].key'">Key</label>
                                            <input v-model="field.key" autocomplete="off" :data-field="'spec.shards.pods.scheduling.tolerations[' + index + '].key'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.key')"></span>
                                        </div>
                                        
                                        <div class="col">
                                            <label :for="'spec.shards.pods.scheduling.tolerations[' + index + '].operator'">Operator</label>
                                            <select v-model="field.operator" @change="( (field.operator == 'Exists') ? (delete field.value) : (field.value = '') )" :data-field="'spec.shards.pods.scheduling.tolerations[' + index + '].operator'">
                                                <option>Equal</option>
                                                <option>Exists</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.operator')"></span>
                                        </div>

                                        <div class="col" v-if="field.operator == 'Equal'">
                                            <label :for="'spec.shards.pods.scheduling.tolerations[' + index + '].value'">Value</label>
                                            <input v-model="field.value" :disabled="(field.operator == 'Exists')" autocomplete="off" :data-field="'spec.shards.pods.scheduling.tolerations[' + index + '].value'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.value')"></span>
                                        </div>

                                        <div class="col">
                                            <label :for="'spec.shards.pods.scheduling.tolerations[' + index + '].operator'">Effect</label>
                                            <select v-model="field.effect" :data-field="'spec.shards.pods.scheduling.tolerations[' + index + '].effect'">
                                                <option :value="nullVal">MatchAll</option>
                                                <option>NoSchedule</option>
                                                <option>PreferNoSchedule</option>
                                                <option>NoExecute</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.effect')"></span>
                                        </div>

                                        <div class="col" v-if="field.effect == 'NoExecute'">
                                            <label :for="'spec.shards.pods.scheduling.tolerations[' + index + '].tolerationSeconds'">Toleration Seconds</label>
                                            <input type="number" min="0" v-model="field.tolerationSeconds" :data-field="'spec.shards.pods.scheduling.tolerations[' + index + '].tolerationSeconds'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( (!hasProp(shards, 'pods.scheduling.tolerations') || !shards.pods.scheduling.tolerations.length) && 'topBorder')">
                                <a class="addRow" @click="pushElement(shards, 'pods.scheduling.tolerations', { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null })">Add Toleration</a>
                            </div>
                        </div>

                        <br/><br/><br/>

                        <div class="header">
                            <h3 for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                <span class="normal">Required During Scheduling Ignored During Execution</span>
                            </h3>                            
                        </div>

                        <br/><br/>
                        
                        <div class="scheduling repeater">
                            <div class="header">
                                <h4 for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                    Node Selector Terms
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')"></span>
                                </h4>
                            </div>
                            <fieldset v-if="(hasProp(shards, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') && shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length)">
                                <div class="section" v-for="(requiredAffinityTerm, termIndex) in shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                    <div class="header">
                                        <h5>Term #{{ termIndex + 1 }}</h5>
                                        <a class="addRow" @click="spliceArray(shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms, termIndex)">Delete</a>
                                    </div>
                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions">
                                                Match Expressions
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span> 
                                            </label>
                                        </div>
                                        <fieldset v-if="requiredAffinityTerm.matchExpressions.length">
                                            <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.matchExpressions">
                                                <div class="header">
                                                    <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items">
                                                        Match Expression #{{ expIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items')"></span> 
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchExpressions, expIndex)">Delete</a>
                                                </div>
                                                
                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">Key</label>
                                                        <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span> 
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">Operator</label>
                                                        <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( !expression.hasOwnProperty('values') && (expression['values'] = ['']) ) )" data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span> 
                                                    </div>
                                                </div>

                                                <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                        <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                    <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!requiredAffinityTerm.matchExpressions.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchExpressions)">Add Expression</a>
                                    </div>

                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields">
                                                Match Fields
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span> 
                                            </label>
                                        </div>
                                        <fieldset v-if="requiredAffinityTerm.matchFields.length">
                                            <div class="section" v-for="(field, fieldIndex) in requiredAffinityTerm.matchFields">
                                                <div class="header">
                                                    <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items">
                                                        Match Field #{{ fieldIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items')"></span> 
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchFields, fieldIndex)">Delete</a>
                                                </div>
                                                
                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">Key</label>
                                                        <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span> 
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">Operator</label>
                                                        <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( !field.hasOwnProperty('values') && (field['values'] = ['']) ) )" data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                    </div>
                                                </div>

                                                <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span> 
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                        <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="!['Gt', 'Lt'].includes(field.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(field.operator)" :class="!field.values.length && 'topBorder'">
                                                    <a class="addRow" @click="field.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!requiredAffinityTerm.matchFields.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchFields)">Add Field</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( (!hasProp(shards, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') || !shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length) && 'topBorder' )">
                                <a class="addRow" @click="addRequiredAffinityTerm(shards, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')">Add Term</a>
                            </div>
                        </div>

                        <br/><br/>
                    
                        <div class="header">
                            <h3 for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                <span class="normal">Preferred During Scheduling Ignored During Execution</span>
                            </h3>
                        </div>

                        <br/><br/>

                        <div class="scheduling repeater">
                            <div class="header">
                                <h4 for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items">
                                    Node Selector Terms
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span> 
                                </h4>
                            </div>
                            <fieldset v-if="(hasProp(shards, 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') && shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.length)">
                                <div class="section" v-for="(preferredAffinityTerm, termIndex) in shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                    <div class="header">
                                        <h5>Term #{{ termIndex + 1 }}</h5>
                                        <a class="addRow" @click="spliceArray(shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution, termIndex)">Delete</a>
                                    </div>
                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions">
                                                Match Expressions
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                            </label>
                                        </div>
                                        <fieldset v-if="preferredAffinityTerm.preference.matchExpressions.length">
                                            <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.preference.matchExpressions">
                                                <div class="header">
                                                    <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                        Match Expression #{{ expIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchExpressions, expIndex)">Delete</a>
                                                </div>

                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                        <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                        <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                    </div>
                                                </div>

                                                <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                        <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                    <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!preferredAffinityTerm.preference.matchExpressions.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchExpressions)">Add Expression</a>
                                    </div>

                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields">
                                                Match Fields
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                            </label>
                                        </div>
                                        <fieldset v-if="preferredAffinityTerm.preference.matchFields.length">
                                            <div class="section" v-for="(field, fieldIndex) in preferredAffinityTerm.preference.matchFields">
                                                <div class="header">
                                                    <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items">
                                                        Match Field #{{ fieldIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items')"></span>
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchFields, fieldIndex)">Delete</a>
                                                </div>

                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">Key</label>
                                                        <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">Operator</label>
                                                        <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( (!field.hasOwnProperty('values') || (field.values.length > 1) ) && (field['values'] = ['']) ) )" data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                    </div>
                                                </div>

                                                <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span> 
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                        <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="!['Gt', 'Lt'].includes(field.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(field.operator)" :class="!field.values.length && 'topBorder'">
                                                    <a class="addRow" @click="field.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!preferredAffinityTerm.preference.matchFields.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchFields)">Add Field</a>
                                    </div>

                                    <label for="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">Weight</label>
                                    <input v-model="preferredAffinityTerm.weight" autocomplete="off" type="number" min="1" max="100" class="affinityWeight" data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight')"></span>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="!preferredAffinity.length && 'topBorder'">
                                <a class="addRow" @click="addPreferredAffinityTerm(shards, 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">Add Term</a>
                            </div>
                        </div>

                        <span class="warning" v-if="editMode">Please, be aware that any changes made to the <code>Pods Scheduling</code> will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance of the cluster</span>
                    </div>
                </fieldset>
            </template>

            <template v-else-if="( (currentSection == 'overrides') && shards.overrides.length )">

                <fieldset v-if="(currentStep.overrides == 'shards')" class="step active" :data-fieldset="'shards.overrides[' + overrideIndex + '].shards'">
                    <div class="header">
                        <h2>Shards Information</h2>
                    </div>

                    <div class="fields">
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.shards.overrides.index">Cluster Identifier <span class="req">*</span></label>
                                <select v-model="shards.overrides[overrideIndex].index" required :data-field="'spec.shards.overrides[' + overrideIndex + '].index'">
                                    <option :value="nullVal" selected>Choose one...</option>
                                    <template v-for="(n, index) in parseInt(shards.clusters)">
                                        <option :value="index">Cluster #{{ index }}</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.index')"></span>
                            </div>
                        </div>
                        
                        <hr/>
                        
                        <div class="row-50">
                            <h3>Instances</h3>

                            <div class="col">
                                <label for="spec.shards.overrides.instancesPerCluster">Number of Instances per Cluster<span class="req">*</span></label>
                                <input type="number" v-model="shards.overrides[overrideIndex].instancesPerCluster" required :data-field="'spec.shards.overrides[' + overrideIndex + '].instancesPerCluster'" min="1" max="16">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.instancesPerCluster')"></span>
                            </div>
                            <div class="col">
                                <label for="spec.shards.overrides.sgInstanceProfile">Instance Profile</label>  
                                <select v-model="shards.overrides[overrideIndex].sgInstanceProfile" class="resourceProfile" :data-field="'spec.shards.overrides[' + overrideIndex + '].sgInstanceProfile'" @change="(resourceProfile == 'createNewResource') && createNewResource('sginstanceprofiles')" :set="( (resourceProfile == 'createNewResource') && (resourceProfile = '') )">
                                    <option selected value="">Default (Cores: 1, RAM: 2GiB)</option>
                                    <option v-for="prof in profiles" v-if="prof.data.metadata.namespace == namespace" :value="prof.name">{{ prof.name }} (Cores: {{ prof.data.spec.cpu }}, RAM: {{ prof.data.spec.memory }}B)</option>
                                    <template v-if="iCan('create', 'sginstanceprofiles', $route.params.namespace)">
                                        <option disabled>– OR –</option>
                                        <option value="createNewResource">Create new profile</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.sgInstanceProfile')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Postgres</h3><br/>

                            <div class="col">
                                <label for="spec.shards.overrides.configurations.sgPostgresConfig">Postgres Configuration</label>
                                <select v-model="shards.overrides[overrideIndex].configurations.sgPostgresConfig" class="pgConfig" :data-field="'spec.shards.overrides[' + overrideIndex + '].configurations.sgPostgresConfig'" @change="(shards.overrides[overrideIndex].configurations.sgPostgresConfig == 'createNewResource') && createNewResource('sgpgconfigs')" :set="( (shards.overrides[overrideIndex].configurations.sgPostgresConfig == 'createNewResource') && (shards.overrides[overrideIndex].configurations.sgPostgresConfig = '') )">
                                    <option value="" selected>Default</option>
                                    <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.postgresVersion == shortPostgresVersion) )">{{ conf.name }}</option>
                                    <template v-if="iCan('create', 'sgpgconfigs', $route.params.namespace)">
                                        <option disabled>– OR –</option>
                                        <option value="createNewResource">Create new configuration</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.configurations.sgPostgresConfig')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Pods Storage</h3>

                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.shards.overrides.pods.persistentVolume.size">Volume Size <span class="req">*</span></label>  
                                    <input v-model="shards.overrides[overrideIndex].pods.persistentVolume.size.size" class="size" required :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.persistentVolume.size'" type="number">
                                    <select v-model="shards.overrides[overrideIndex].pods.persistentVolume.size.unit" class="unit" required :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.persistentVolume.size'" >
                                        <option disabled value="">Select Unit</option>
                                        <option value="Mi">MiB</option>
                                        <option value="Gi">GiB</option>
                                        <option value="Ti">TiB</option>   
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.persistentVolume.size')"></span>
                                </div>
                            </div>

                            <div class="col">
                                <label for="spec.shards.overrides.pods.persistentVolume.storageClass">Storage Class</label>
                                <select v-model="shards.overrides[overrideIndex].pods.persistentVolume.storageClass" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.persistentVolume.storageClass'" :disabled="!storageClasses.length">
                                    <option value=""> {{ storageClasses.length ? 'Select Storage Class' : 'No storage classes available' }}</option>
                                    <option v-for="sClass in storageClasses">{{ sClass }}</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.persistentVolume.storageClass')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.overrides == 'sidecars')" class="step active" :data-fieldset="'shards.overrides[' + overrideIndex + '].sidecars'">
                    <div class="header">
                        <h2>Sidecars</h2>
                    </div>

                    <div class="fields">
                        <div class="row-50">
                            <h3>Connection Pooling</h3>
                            <p>To solve the Postgres connection fan-in problem (handling large number of incoming connections) StackGres includes by default a connection pooler fronting every Postgres instance. It is deployed as a sidecar. You may opt-out as well as tune the connection pooler configuration.</p>

                            <div class="col">
                                <label for="spec.shards.overrides.configurations.sgPoolingConfig">
                                    Connection Pooling
                                </label>  
                                <label for="connPoolingShards" class="switch yes-no">
                                    Enable
                                    <input :checked="!shards.overrides[overrideIndex].pods.disableConnectionPooling" type="checkbox" id="connPoolingShards" @change="( (shards.overrides[overrideIndex].pods.disableConnectionPooling = !shards.overrides[overrideIndex].pods.disableConnectionPooling)) " data-switch="NO">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.configurations.sgPoolingConfig')"></span>
                            </div>

                            <div class="col" v-if="!shards.overrides[overrideIndex].pods.disableConnectionPooling">
                                <label for="connectionPoolingConfigShards">
                                    Connection Pooling Configuration
                                </label>
                                <select v-model="shards.overrides[overrideIndex].configurations.sgPoolingConfig" class="connectionPoolingConfig" @change="(shards.overrides[overrideIndex].configurations.sgPoolingConfig == 'createNewResource') && createNewResource('sgpoolconfigs')" :set="( (shards.overrides[overrideIndex].configurations.sgPoolingConfig == 'createNewResource') && (shards.overrides[overrideIndex].configurations.sgPoolingConfig = '') )">
                                    <option value="" selected>Default</option>
                                    <option v-for="conf in connPoolConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                                    <template v-if="iCan('create', 'sgpoolconfigs', $route.params.namespace)">
                                        <option value="" disabled>– OR –</option>
                                        <option value="createNewResource">Create new configuration</option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.configurations.sgPoolingConfig')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Postgres Utils</h3>
                            <p>Sidecar container with Postgres administration tools. Optional (on by default; recommended for troubleshooting).</p>

                            <div class="col">
                                <label for="spec.shards.overrides.pods.disablePostgresUtil">Postgres Utils</label>  
                                <label for="postgresUtil" class="switch yes-no">
                                    Enable
                                    <input :checked="!shards.overrides[overrideIndex].pods.disablePostgresUtil" type="checkbox" id="postgresUtil" @change="shards.overrides[overrideIndex].pods.disablePostgresUtil = !shards.overrides[overrideIndex].pods.disablePostgresUtil" data-switch="YES">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.disablePostgresUtil').replace('If set to `true`', 'If disabled')"></span>
                            </div>
                        </div>

                        <hr/>

                        <div class="row-50">
                            <h3>Monitoring</h3>
                            <p>Enable Prometheus metrics scraping via service monitors. Check the <a href="https://stackgres.io/doc/latest/install/prerequisites/monitoring/" target="_blank">Installation -> Monitoring</a> section for information on how to enable in StackGres Grafana dashboard integration.</p>

                            <div class="col">
                                <label for="spec.shards.overrides.pods.disableMetricsExporter">Metrics Exporter</label>  
                                <label for="metricsExporterShards" class="switch yes-no">
                                    Enable
                                    <input :checked="!shards.overrides[overrideIndex].pods.disableMetricsExporter" type="checkbox" id="metricsExporterShards" @change="(shards.overrides[overrideIndex].pods.disableMetricsExporter = !shards.overrides[overrideIndex].pods.disableMetricsExporter)" data-switch="YES">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.overrides == 'scripts')" class="step active" :data-fieldset="'shards.overrides[' + overrideIndex + '].scripts'">
                    <div class="header">
                        <h2>Managed SQL</h2>
                    </div>

                    <p>Use this option to run a set of scripts on your cluster.</p><br/><br/>

                    <div class="fields">
                        <div class="scriptFieldset repeater">
                            <div class="header">
                                <h3 :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts'">
                                    Scripts
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.managedSql.scripts')"></span>
                                </h3>
                            </div>
                            
                            <fieldset
                                v-if="shards.overrides[overrideIndex].hasOwnProperty('managedSql')"
                                v-for="(baseScript, baseIndex) in shards.overrides[overrideIndex].managedSql.scripts"
                                :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + ']'"
                            >
                                <div class="header">
                                    <h4>SGScript #{{baseIndex+1 }}</h4>
                                    <div class="addRow" v-if="(baseScript.sgScript != (name + '-default') )">
                                        <a class="delete" @click="spliceArray(shards.overrides[overrideIndex].managedSql.scripts, baseIndex), spliceArray(scriptSource.overrides[overrideIndex], baseIndex)">Delete Script</a>
                                        <template v-if="baseIndex">
                                            <span class="separator"></span>
                                            <a @click="moveArrayItem(shards.overrides[overrideIndex].managedSql.scripts, baseIndex, 'up')">Move Up</a>
                                        </template>
                                        <template  v-if="( (baseIndex + 1) != shards.overrides[overrideIndex].managedSql.scripts.length)">
                                            <span class="separator"></span>
                                            <a @click="moveArrayItem(shards.overrides[overrideIndex].managedSql.scripts, baseIndex, 'down')">Move Down</a>
                                        </template>
                                    </div>
                                </div>

                                <div class="row-50 noMargin">
                                    <div class="col">
                                        <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.scriptSource'">Source</label>
                                        <select v-model="scriptSource.overrides[overrideIndex][baseIndex].base" :disabled="editMode && isDefaultScript(baseScript.sgScript) && baseScript.hasOwnProperty('scriptSpec')" @change="setBaseScriptSource(baseIndex, scriptSource.overrides[overrideIndex], shards.overrides[overrideIndex].managedSql)" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.scriptSource.shards[' + baseIndex + ']'">
                                            <option value="" selected>Select source script...</option>
                                            <option v-for="script in sgscripts" v-if="(script.data.metadata.namespace == $route.params.namespace)">
                                                {{ script.name }}
                                            </option>
                                            <template v-if="iCan('create', 'sgscripts', $route.params.namespace)">
                                                <option value="" disabled>– OR –</option>
                                                <option value="createNewScript">Create new script</option>
                                            </template>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded.'"></span>
                                    </div>
                                </div>

                                <template v-if="( ( !editMode &&(scriptSource.overrides[overrideIndex][baseIndex].base == 'createNewScript') ) || (editMode && baseScript.hasOwnProperty('scriptSpec')) )">
                                    <hr/>

                                    <div class="row-50 noMargin">
                                        <div class="col">
                                            <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.continueOnError'">Continue on Error</label>  
                                            <label :for="'continueOnError-' + baseIndex" class="switch yes-no" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].continueOnError'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                Enable
                                                <input type="checkbox" :id="'continueOnError-' + baseIndex" v-model="shards.overrides[overrideIndex].managedSql.scripts[baseIndex].scriptSpec.continueOnError" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                            </label>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                        </div>
                                        <div class="col">
                                            <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.managedVersions'">Managed Versions</label>  
                                            <label :for="'managedVersions-' + baseIndex" class="switch yes-no" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].managedVersions'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                Enable
                                                <input type="checkbox" :id="'managedVersions-' + baseIndex" v-model="shards.overrides[overrideIndex].managedSql.scripts[baseIndex].scriptSpec.managedVersions" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                            </label>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions').replace(/true/g, 'Enabled')"></span>
                                        </div>
                                    </div>
                            
                                    <div class="section">
                                        <fieldset v-for="(script, index) in baseScript.scriptSpec.scripts">
                                            <div class="header">
                                                <h5>Script Entry #{{ index+1 }} <template v-if="script.hasOwnProperty('name') && script.name.length">–</template> <span class="scriptTitle">{{ script.name }}</span></h5>
                                                <div class="addRow" v-if="!isDefaultScript(baseScript.sgScript)">
                                                    <a @click="spliceArray(baseScript.scriptSpec.scripts, index) && spliceArray(scriptSource.overrides[overrideIndex][baseIndex].entries, index)">Delete Entry</a>
                                                    <template v-if="index">
                                                        <span class="separator"></span>
                                                        <a @click="moveArrayItem(baseScript.scriptSpec.scripts, index, 'up')">Move Up</a>
                                                    </template>
                                                    <template  v-if="( (index + 1) != baseScript.scriptSpec.scripts.length)">
                                                        <span class="separator"></span>
                                                        <a @click="moveArrayItem(baseScript.scriptSpec.scripts, index, 'down')">Move Down</a>
                                                    </template>
                                                </div>
                                            </div>
                                            <div class="row">
                                                <div class="row-50">
                                                    <div class="col" v-if="script.hasOwnProperty('version') && editMode">
                                                        <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.version'">Version</label>
                                                        <input type="number" v-model="script.version" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].version'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                                    </div>
                                                </div>
                                                <div class="row-50">                                                
                                                    <div class="col">
                                                        <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.name'">Name</label>
                                                        <input v-model="script.name" placeholder="Type a name..." autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].name'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                    </div>

                                                    <div class="col" v-if="script.hasOwnProperty('database') || !isDefaultScript(baseScript.sgScript)">
                                                        <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.database'">Database</label>
                                                        <input v-model="script.database" placeholder="Type a database name..." autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].database'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                                    </div>

                                                    <div class="col" v-if="script.hasOwnProperty('user') || !isDefaultScript(baseScript.sgScript)">
                                                        <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.user'">User</label>
                                                        <input v-model="script.user" placeholder="Type a user name..." autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].user'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.user')"></span>
                                                    </div>
                                                    
                                                    <div class="col" v-if="script.hasOwnProperty('wrapInTransaction') || !isDefaultScript(baseScript.sgScript)">
                                                        <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.wrapInTransaction'">Wrap in Transaction</label>
                                                        <select v-model="script.wrapInTransaction" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].wrapInTransaction'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            <option :value="nullVal">NONE</option>
                                                            <option value="read-committed">READ COMMITTED</option>
                                                            <option value="repeatable-read">REPEATABLE READ</option>
                                                            <option value="serializable">SERIALIZABLE</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                                                    </div>
                                                
                                                    <div class="col" v-if="script.hasOwnProperty('storeStatusInDatabase') || !isDefaultScript(baseScript.sgScript)">
                                                        <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.storeStatusInDatabase'">Store Status in Databases</label>  
                                                        <label :for="'storeStatusInDatabase[' + baseIndex + '][' + index + ']'" class="switch yes-no" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].storeStatusInDatabase'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            Enable
                                                            <input type="checkbox" :id="'storeStatusInDatabase[' + baseIndex + '][' + index + ']'" v-model="script.storeStatusInDatabase" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        </label>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase')"></span>
                                                    </div>

                                                    <div class="col">
                                                        <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.retryOnError'">Retry on Error</label>  
                                                        <label :for="'retryOnError[' + baseIndex + '][' + index + ']'" class="switch yes-no" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].retryOnError'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            Enable
                                                            <input type="checkbox" :id="'retryOnError[' + baseIndex + '][' + index + ']'" v-model="script.retryOnError" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        </label>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError')"></span>
                                                    </div>
                                                </div>

                                                <div class="row-100">
                                                    <div class="col">
                                                        <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.scriptSource'">
                                                            Source
                                                            <span class="req">*</span>
                                                        </label>
                                                        <select v-model="scriptSource.overrides[overrideIndex][baseIndex].entries[index]" @change="setScriptSource(baseIndex, index, scriptSource.overrides[overrideIndex], shards.overrides.managedSql)" :disabled="isDefaultScript(baseScript.sgScript)" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].source'" required>
                                                            <option value="raw">Raw script</option>
                                                            <option value="secretKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.secretScript')">From Secret</option>
                                                            <option value="configMapKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.configMapScript')">From ConfigMap</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'"></span>
                                                    </div>
                                                    <div class="col">                                                
                                                        <template  v-if="(!editMode && (scriptSource.overrides[overrideIndex][baseIndex].entries[index] == 'raw') ) || (editMode && script.hasOwnProperty('script') )">
                                                            <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts.script'" class="script">
                                                                Script
                                                                <span class="req">*</span>
                                                            </label> 
                                                            <span class="uploadScript" v-if="!editMode">or <a @click="getScriptFile(baseIndex, index)" class="uploadLink">upload a file</a></span> 
                                                            <input :id="'scriptFile-'+ baseIndex + '-' + index" type="file" @change="uploadScript" class="hide" :disabled="isDefaultScript(baseScript.sgScript)">
                                                            <textarea v-model="script.script" placeholder="Type a script..." :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].script'" :disabled="isDefaultScript(baseScript.sgScript)" required></textarea>
                                                        </template>
                                                        <template v-else-if="(scriptSource.overrides[overrideIndex][baseIndex].entries[index] != 'raw')">
                                                            <div class="header">
                                                                <h3 :for="'spec.shards.overrides.managedSql.scripts.scriptFrom.properties' + scriptSource.overrides[overrideIndex][baseIndex].entries[index]" class="capitalize">
                                                                    {{ splitUppercase(scriptSource.overrides[overrideIndex][baseIndex].entries[index]) }}
                                                                    
                                                                    <span class="helpTooltip" :class="( (scriptSource.overrides[overrideIndex][baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef')"></span>
                                                                    <span class="helpTooltip" :class="( (scriptSource.overrides[overrideIndex][baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef')"></span>
                                                                </h3>
                                                            </div>
                                                            
                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label :for="'spec.shards.overrides.managedSql.scripts.scriptFrom.properties.' + scriptSource.overrides[overrideIndex][baseIndex].entries[index] + '.properties.name'">
                                                                        Name
                                                                        <span class="req">*</span>
                                                                    </label>
                                                                    <input v-model="script.scriptFrom[scriptSource.overrides[overrideIndex][baseIndex].entries[index]].name" placeholder="Type a name.." autocomplete="off" :disabled="isDefaultScript(baseScript.sgScript)" required>
                                                                    <span class="helpTooltip" :class="( (scriptSource.overrides[overrideIndex][baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>
                                                                    <span class="helpTooltip" :class="( (scriptSource.overrides[overrideIndex][baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>
                                                                </div>

                                                                <div class="col">
                                                                    <label :for="'spec.shards.overrides.managedSql.scripts.scriptFrom.properties.' + scriptSource.overrides[overrideIndex][baseIndex].entries[index] + '.properties.key'">
                                                                        Key
                                                                        <span class="req">*</span>
                                                                    </label>
                                                                    <input v-model="script.scriptFrom[scriptSource.overrides[overrideIndex][baseIndex].entries[index]].key" placeholder="Type a key.." autocomplete="off" :disabled="isDefaultScript(baseScript.sgScript)" required>
                                                                    <span class="helpTooltip" :class="( (scriptSource.overrides[overrideIndex][baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                                                    <span class="helpTooltip" :class="( (scriptSource.overrides[overrideIndex][baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                                                </div>
                                                            </div>

                                                            <template v-if="editMode && (script.scriptFrom.hasOwnProperty('configMapScript'))">
                                                                <label :for="'spec.shards.overrides.managedSql.scripts.scriptFrom.properties.' + scriptSource.overrides[overrideIndex][baseIndex].entries[index] + '.properties.configMapScript'" class="script">
                                                                    Script
                                                                <span class="req">*</span>
                                                                </label> 
                                                                <textarea v-model="script.scriptFrom.configMapScript" placeholder="Type a script..." :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].scriptFrom.configMapScript'" :disabled="isDefaultScript(baseScript.sgScript)" required></textarea>
                                                            </template>
                                                        </template>
                                                    </div>
                                                </div>
                                            </div>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="!baseScript.scriptSpec.scripts.length && 'topBorder'" v-if="!isDefaultScript(baseScript.sgScript)">
                                            <a class="addRow" @click="pushScript(baseIndex, scriptSource.overrides[overrideIndex], shards.overrides[overrideIndex].managedSql)" >Add Entry</a>
                                        </div>
                                    </div>
                                </template>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( !shards.overrides[overrideIndex].hasOwnProperty('managedSql') || !shards.overrides[overrideIndex].managedSql.scripts.length ) && 'topBorder'">
                                <a
                                    class="addRow"
                                    @click="pushScriptSet(scriptSource.overrides[overrideIndex], shards.overrides[overrideIndex].managedSql)">
                                        Add Script
                                    </a>
                            </div>
                            
                            <br/><br/>
                            
                            <div v-if="shards.overrides[overrideIndex].hasOwnProperty('managedSql') && hasScripts(shards.overrides[overrideIndex].managedSql.scripts, scriptSource.overrides[overrideIndex])" class="row row-50 noMargin">
                                <div class="col">
                                    <label :for="'spec.shards.overrides[' + overrideIndex + '].managedSql.continueOnSGScriptError'">Continue on SGScripts Error</label>  
                                    <label for="continueOnSGScriptError" class="switch yes-no" :data-field="'spec.shards.overrides[' + overrideIndex + '].managedSql.continueOnSGScriptError'">
                                        Enable
                                        <input type="checkbox" id="continueOnSGScriptError" v-model="shards.overrides[overrideIndex].managedSql.continueOnSGScriptError" data-switch="NO">
                                    </label>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.managedSql.continueOnSGScriptError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                </div>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.overrides == 'pods')" class="step active" :data-fieldset="'shards.overrides[' + overrideIndex + '].pods'">
                    <div class="header">
                        <h2>User-Supplied Pods Sidecars</h2>
                    </div>

                    <div class="fields">
                        <div class="header">
                            <h3 for="spec.shards.overrides.pods.customVolumes">
                                Custom Volumes
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes')"></span>
                            </h3>
                        </div>
                        <p>List of volumes that can be mounted by custom containers belonging to the pod</p>

                        <br/>
                        
                        <div class="repeater">
                            <fieldset
                                class="noPaddingBottom"
                                v-if="(shards.overrides[overrideIndex].pods.hasOwnProperty('customVolumes') && shards.overrides[overrideIndex].pods.customVolumes.length)"
                                :data-fieldset="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes'"
                            >
                                <template v-for="(vol, index) in shards.overrides[overrideIndex].pods.customVolumes">
                                    <div class="section" :key="index">
                                        <div class="header">
                                            <h4>Volume #{{ index + 1 }}{{ !isNull(vol.name) ? (': ' + vol.name) : '' }}</h4>
                                            <a class="addRow delete" @click="spliceArray(shards.overrides[overrideIndex].pods.customVolumes, index); spliceArray(customVolumesType.overrides[overrideIndex], index)">Delete</a>
                                        </div>
                                                        
                                        <div class="row-50">
                                            <div class="col">
                                                <label>Name</label>
                                                <input :required="(customVolumesType.overrides[overrideIndex][index] !== null)" v-model="vol.name" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].name'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.name')"></span>
                                            </div>
                                            
                                            <div class="col">
                                                <label>Type</label>
                                                <select v-model="customVolumesType.overrides[overrideIndex][index]" @change="initCustomVolume(index, shards.overrides[overrideIndex].pods.customVolumes, customVolumesType.overrides[overrideIndex])" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].type'">
                                                    <option :value="null" selected>Choose one...</option>
                                                    <option value="emptyDir">Empty Directory</option>
                                                    <option value="configMap">ConfigMap</option>
                                                    <option value="secret">Secret</option>
                                                </select>
                                                <span class="helpTooltip" data-tooltip="Specifies the type of volume to be used"></span>
                                            </div>
                                        </div>

                                        <template v-if="(customVolumesType.overrides[overrideIndex][index] == 'emptyDir')">
                                            <div class="header">
                                                <h5 for="spec.shards.overrides.pods.customVolumes.emptyDir">
                                                    Empty Directory
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.emptyDir')"></span>
                                                </h5>
                                            </div>
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Medium</label>
                                                    <input v-model="vol.emptyDir.medium" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].emptyDir.medium'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.emptyDir.properties.medium')"></span>
                                                </div>
                                                <div class="col">
                                                    <label>Size Limit</label>
                                                    <input v-model="vol.emptyDir.sizeLimit" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].emptyDir.sizeLimit'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.emptyDir.properties.sizeLimit')"></span>
                                                </div>
                                            </div>

                                        </template>
                                        <template v-else-if="(customVolumesType.overrides[overrideIndex][index] == 'configMap')">
                                            <div class="header">
                                                <h5 for="spec.shards.overrides.pods.customVolumes.configMap">
                                                    ConfigMap
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap')"></span>
                                                </h5>
                                            </div>
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Name</label>
                                                    <input v-model="vol.configMap.name" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].configMap.name'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.name')"></span>
                                                </div>
                                                <div class="col">                    
                                                    <label :for="'spec.shards.overrides.pods.customVolumes[' + index + '].configMap.optional'">
                                                        Optional
                                                    </label>  
                                                    <label :for="'spec.shards.overrides.pods.customVolumes[' + index + '].configMap.optional'" class="switch yes-no">
                                                        Enable
                                                        <input type="checkbox" :id="'spec.shards.overrides.pods.customVolumes[' + index + '].configMap.optional'" v-model="vol.configMap.optional" data-switch="NO" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].configMap.optional'">
                                                    </label>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.optional')"></span>
                                                </div>
                                                <div class="col">
                                                    <label>Default Mode</label>
                                                    <input type="number" v-model="vol.configMap.defaultMode" min="0" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].configMap.defaultMode'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.defaultMode')"></span>
                                                </div>
                                            </div>

                                            <br/><br/>
                                            <div class="header">
                                                <h6 for="spec.shards.overrides.pods.customVolumes.configMap.items">
                                                    Items
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.items')"></span>
                                                </h6>
                                            </div>
                                            <fieldset
                                                class="noMargin"
                                                :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].configMap.items'"
                                                v-if="vol.configMap.items.length"
                                            >
                                                <template v-for="(item, itemIndex) in vol.configMap.items">
                                                    <div class="section" :key="itemIndex" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].configMap.items[' + itemIndex + ']'">
                                                        <div class="header">
                                                            <h4>Item #{{ itemIndex + 1 }}</h4>
                                                            <a class="addRow delete" @click="spliceArray(vol.configMap.items, itemIndex)">Delete</a>
                                                        </div>
                                                                        
                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label>Key</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.key" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].key'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.items.items.properties.key')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Mode</label>
                                                                <input type="number" v-model="item.mode" min="0" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].mode'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.items.items.properties.mode')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Path</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.path" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].path'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.configMap.properties.items.items.properties.path')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </template>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!vol.configMap.items.length && 'topBorder'">
                                                <a
                                                    class="addRow"
                                                    @click="vol.configMap.items.push({
                                                        key: null,
                                                        mode: null,
                                                        path: null,
                                                    })"
                                                >
                                                    Add Item
                                                </a>
                                            </div>
                                        </template>

                                        <template v-else-if="(customVolumesType.overrides[overrideIndex][index] == 'secret')">
                                            <div class="header">
                                                <h5 for="spec.shards.overrides.pods.customVolumes.secret">
                                                    Secret
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret')"></span>
                                                </h5>
                                            </div>
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Secret Name</label>
                                                    <input v-model="vol.secret.secretName" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].secret.secretName'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.secretName')"></span>
                                                </div>
                                                <div class="col">                    
                                                    <label :for="'spec.shards.overrides.pods.customVolumes[' + index + '].secret.optional'">
                                                        Optional
                                                    </label>  
                                                    <label :for="'spec.shards.overrides.pods.customVolumes[' + index + '].secret.optional'" class="switch yes-no">
                                                        Enable
                                                        <input type="checkbox" :id="'spec.shards.overrides.pods.customVolumes[' + index + '].secret.optional'" v-model="vol.secret.optional" data-switch="NO" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].secret.optional'">
                                                    </label>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.optional')"></span>
                                                </div>
                                                <div class="col">
                                                    <label>Default Mode</label>
                                                    <input type="number" v-model="vol.secret.defaultMode" min="0" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].secret.defaultMode'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.defaultMode')"></span>
                                                </div>
                                            </div>

                                            <br/><br/>
                                            <div class="header">
                                                <h6 for="spec.shards.overrides.pods.customVolumes.secret.items">
                                                    Items
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.items')"></span>
                                                </h6>
                                            </div>
                                            <fieldset
                                                class="noMargin"
                                                :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].secret.items'"
                                                v-if="vol.secret.items.length"
                                            >
                                                <template v-for="(item, itemIndex) in vol.secret.items">
                                                    <div class="section" :key="itemIndex" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].secret.items[' + itemIndex + ']'">
                                                        <div class="header">
                                                            <h4>Item #{{ itemIndex + 1 }}</h4>
                                                            <a class="addRow delete" @click="spliceArray(vol.secret.items, itemIndex)">Delete</a>
                                                        </div>
                                                                        
                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label>Key</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.key" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].key'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.items.items.properties.key')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Mode</label>
                                                                <input type="number" v-model="item.mode" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].mode'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.items.items.properties.mode')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label>Path</label>
                                                                <input :required="!isNull(vol.name)" v-model="item.path" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].path'">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customVolumes.secret.properties.items.items.properties.path')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </template>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!vol.secret.items.length && 'topBorder'">
                                                <a
                                                    class="addRow"
                                                    @click="vol.secret.items.push({
                                                        key: '',
                                                        mode: '',
                                                        path: '',
                                                    })"
                                                >
                                                    Add Item
                                                </a>
                                            </div>
                                        </template>
                                    </div>
                                </template>
                            </fieldset>
                            <div class="fieldsetFooter" :class="(!shards.overrides[overrideIndex].pods.hasOwnProperty('customVolumes') || (shards.overrides[overrideIndex].pods.hasOwnProperty('customVolumes') && !shards.overrides[overrideIndex].pods.customVolumes.length) ) && 'topBorder'">
                                <a 
                                    class="addRow"
                                    @click="
                                        customVolumesType.overrides[overrideIndex].push(null);
                                        (!shards.overrides[overrideIndex].pods.hasOwnProperty('customVolumes') && (shards.overrides[overrideIndex].pods['customVolumes'] = []) );
                                        shards.overrides[overrideIndex].pods.customVolumes.push({ name: null});
                                        formHash = (+new Date).toString();
                                    "
                                >
                                    Add Volume
                                </a>
                            </div>
                        </div>

                        <br/><br/><br/>

                        <template v-if="!editMode || (shards.overrides[overrideIndex].pods.hasOwnProperty('customInitContainers') && shards.overrides[overrideIndex].pods.customInitContainers.length)">
                            <div class="header">
                                <h3 for="spec.shards.overrides.pods.customInitContainers">
                                    Custom Init Containers
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers')"></span>
                                </h3>
                            </div>
                            <p>A list of custom application init containers that run within the cluster’s Pods</p>

                            <br/>
                            
                            <div class="repeater">
                                <fieldset
                                    v-if="shards.overrides[overrideIndex].pods.customInitContainers.length"
                                    :data-fieldset="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers'"
                                >
                                    <template v-for="(container, index) in shards.overrides[overrideIndex].pods.customInitContainers">
                                        <div class="section" :key="index" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + ']'">
                                            <div class="header">
                                                <h4>Init Container #{{ index + 1 }}{{ !isNull(container.name) ? (': ' + container.name) : '' }}</h4>
                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(shards.overrides[overrideIndex].pods.customInitContainers, index)">Delete</a>
                                            </div>
                                                            
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Name</label>
                                                    <input :disabled="editMode" :required="!isNull(container.image) || !isNull(container.imagePullPolicy) || !isNull(container.workingDir)" v-model="container.name" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].name'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.name')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image</label>
                                                    <input v-model="container.image" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].image'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.image')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image Pull Policy</label>
                                                    <input :disabled="editMode" v-model="container.imagePullPolicy" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].imagePullPolicy'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.imagePullPolicy')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Working Directory</label>
                                                    <input :disabled="editMode" v-model="container.workingDir" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].workingDir'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.workingDir')"></span>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('args')">
                                                    <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].args'">
                                                        <div class="header" :class="[container.args.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.shards.overrides.pods.customInitContainers[' + index + '].args'">
                                                                Arguments
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.args')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(arg, argIndex) in container.args">
                                                            <div :key="'arg-' + argIndex" class="inputContainer" :class="(container.args.length !== (argIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'arg-' + argIndex" 
                                                                    v-model="container.args[argIndex]" 
                                                                    :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].args[' + argIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.args, argIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.args.push(null)">Add Argument</a>
                                                    </div>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('command')">
                                                    <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].command'">
                                                        <div class="header" :class="[container.command.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.shards.overrides.pods.customInitContainers[' + index + '].command'">
                                                                Command
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.command')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(command, commandIndex) in container.command">
                                                            <div :key="'command-' + commandIndex" class="inputContainer" :class="(container.command.length !== (commandIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'command-' + commandIndex" 
                                                                    v-model="container.command[commandIndex]" 
                                                                    :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].command[' + commandIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.command, commandIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.command.push(null)">Add Command</a>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="repeater marginBottom marginTop" v-if="!editMode || container.hasOwnProperty('env')">
                                                <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].env'">
                                                    <div class="header" :class="!container.env.length && 'noMargin'">
                                                        <h5 :for="'spec.shards.overrides.pods.customInitContainers[' + index + '].env'">
                                                            Environment Variables
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.env')"></span> 
                                                        </h5>
                                                    </div>
                                                    <div class="variable" v-if="container.env.length">
                                                        <div class="row" v-for="(env, envIndex) in container.env" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].env[' + envIndex + ']'">
                                                            <label>Name</label>
                                                            <input :required="!isNull(env.value)" :disabled="editMode" class="label" v-model="env.name" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].env[' + envIndex + '].name'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input :disabled="editMode" class="labelValue" v-model="env.value" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].env[' + envIndex + '].value'">

                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.env, envIndex)">Delete</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.env.push({ name: null, value: null})">Add Variable</a>
                                                </div>
                                            </div>

                                            <br/>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('ports')">
                                                <div class="header">
                                                    <h5>
                                                        Ports
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater marginBottom">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers.ports'"
                                                        v-if="container.ports.length"
                                                    >
                                                        <div class="section" v-for="(port, portIndex) in container.ports" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].ports[' + portIndex + ']'">
                                                            <div class="header">
                                                                <h6>Port #{{ portIndex + 1 }}{{ !isNull(port.name) ? (': ' + port.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.ports, portIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.ports.name">Name</label>  
                                                                    <input :disabled="editMode" v-model="port.name" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].ports[' + portIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.ports.hostIP">Host IP</label>  
                                                                    <input :disabled="editMode" v-model="port.hostIP" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].ports[' + portIndex + '].hostIP'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.hostIP')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.ports.hostPort">Host Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.hostPort" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].ports[' + portIndex + '].hostPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.hostPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.ports.containerPort">Container Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.containerPort" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].ports[' + portIndex + '].containerPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.containerPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.ports.protocol">Protocol</label>  
                                                                    <select :disabled="editMode" v-model="port.protocol" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].ports[' + portIndex + '].protocol'">
                                                                        <option :value="nullVal" selected>Choose one...</option>
                                                                        <option>TCP</option>
                                                                        <option>UDP</option>
                                                                        <option>SCTP</option>
                                                                    </select>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.ports.items.properties.protocol')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('ports') && !container.ports.length) && 'topBorder'">
                                                        <a class="addRow" @click="!container.hasOwnProperty('ports') && (container['ports'] = []); container.ports.push({
                                                            name: null,
                                                            hostIP: null,
                                                            hostPort: null,
                                                            containerPort: null,
                                                            protocol: null
                                                        })">
                                                            Add Port
                                                        </a>
                                                    </div>
                                                </div>
                                                <br/>
                                            </template>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('volumeMounts')">
                                                <div class="header">
                                                    <h5>
                                                        Volume Mounts
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].volumeMounts'"
                                                        v-if="container.hasOwnProperty('volumeMounts') && container.volumeMounts.length"
                                                    >
                                                        <div class="section" v-for="(mount, mountIndex) in container.volumeMounts" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + ']'">
                                                            <div class="header">
                                                                <h6>Mount #{{ mountIndex + 1 }}{{ !isNull(mount.name) ? (': ' + mount.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.volumeMounts, mountIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.volumeMounts.name">Name</label>  
                                                                    <input :required="!isNull(mount.mountPath)" :disabled="editMode" v-model="mount.name" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">                    
                                                                    <label :for="'spec.shards.overrides.pods.customInitContainers[' + index + '].volumeMounts.readOnly'">
                                                                        Read Only
                                                                    </label>  
                                                                    <label :disabled="editMode" :for="'spec.shards.overrides.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" class="switch yes-no">
                                                                        Enable
                                                                        <input :disabled="editMode" type="checkbox" :id="'spec.shards.overrides.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" v-model="mount.readOnly" data-switch="NO" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'">
                                                                    </label>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.volumeMounts.mountPath">Mount Path</label>  
                                                                    <input :required="!isNull(mount.name)" :disabled="editMode" v-model="mount.mountPath" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.volumeMounts.mountPropagation">Mount Propagation</label>  
                                                                    <input :disabled="editMode" v-model="mount.mountPropagation" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPropagation'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.volumeMounts.subPath">Sub Path</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPathExpr') && !isNull(mount.subPathExpr))" v-model="mount.subPath" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].subPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.subPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customInitContainers.volumeMounts.subPathExpr">Sub Path Expr</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPath') && !isNull(mount.subPath))" v-model="mount.subPathExpr" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].subPathExpr'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customInitContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('volumeMounts') && !container.volumeMounts.length) && 'topBorder'">
                                                        <a
                                                            class="addRow"
                                                            @click="
                                                                !container.hasOwnProperty('volumeMounts') && (container['volumeMounts'] = []); container.volumeMounts.push({
                                                                    mountPath: null,
                                                                    mountPropagation: null,
                                                                    name: null,
                                                                    readOnly: false,
                                                                    subPath: null,
                                                                    subPathExpr: null
                                                                });
                                                                formHash = (+new Date).toString();
                                                            "
                                                        >
                                                            Add Volume
                                                        </a>
                                                    </div>
                                                </div>
                                            </template>
                                        </div>
                                    </template>
                                </fieldset>
                                <div v-if="!editMode" class="fieldsetFooter" :class="!shards.overrides[overrideIndex].pods.customInitContainers.length && 'topBorder'">
                                    <a 
                                        class="addRow"
                                        @click="shards.overrides[overrideIndex].pods.customInitContainers.push({
                                            name: null,
                                            image: null,
                                            imagePullPolicy: null,
                                            args: [null],
                                            command: [null],
                                            workingDir: null,
                                            env: [ { name: null, value: null } ],
                                            ports: [{
                                                containerPort: null,
                                                hostIP: null,
                                                hostPort: null,
                                                name: null,
                                                protocol: null
                                            }],
                                            volumeMounts: [{
                                                mountPath: null,
                                                mountPropagation: null,
                                                name: null,
                                                readOnly: false,
                                                subPath: null,
                                                subPathExpr: null,
                                            }]
                                        })"
                                    >
                                        Add Init Container
                                    </a>
                                </div>
                            </div>

                            <br/><br/><br/>

                        </template>

                        <template v-if="!editMode || (shards.overrides[overrideIndex].pods.hasOwnProperty('customContainers') && shards.overrides[overrideIndex].pods.customContainers.length)">
                            <div class="header">
                                <h3 for="spec.shards.overrides.pods.customContainers">
                                    Custom Containers
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers')"></span>
                                </h3>
                            </div>
                            <p>A list of custom application containers that run within the cluster’s Pods</p>

                            <br/>
                            
                            <div class="repeater">
                                <fieldset
                                    v-if="shards.overrides[overrideIndex].pods.customContainers.length"
                                    :data-fieldset="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers'"
                                >
                                    <template v-for="(container, index) in shards.overrides[overrideIndex].pods.customContainers">
                                        <div class="section" :key="index" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + ']'">
                                            <div class="header">
                                                <h4>Container #{{ index + 1 }}{{ !isNull(container.name) ? (': ' + container.name) : '' }}</h4>
                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(shards.overrides[overrideIndex].pods.customContainers, index)">Delete</a>
                                            </div>
                                                            
                                            <div class="row-50">
                                                <div class="col">
                                                    <label>Name</label>
                                                    <input :disabled="editMode" :required="!isNull(container.image) || !isNull(container.imagePullPolicy) || !isNull(container.workingDir)" v-model="container.name" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].name'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.name')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image</label>
                                                    <input v-model="container.image" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].image'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.image')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Image Pull Policy</label>
                                                    <input :disabled="editMode" v-model="container.imagePullPolicy" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].imagePullPolicy'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.imagePullPolicy')"></span>
                                                </div>

                                                <div class="col">
                                                    <label>Working Directory</label>
                                                    <input :disabled="editMode" v-model="container.workingDir" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].workingDir'">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.workingDir')"></span>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('args')">
                                                    <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].args'">
                                                        <div class="header" :class="[container.args.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.shards.overrides.pods.customContainers[' + index + '].args'">
                                                                Arguments
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.args')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(arg, argIndex) in container.args">
                                                            <div :key="'arg-' + argIndex" class="inputContainer" :class="(container.args.length !== (argIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'arg-' + argIndex" 
                                                                    v-model="container.args[argIndex]" 
                                                                    :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].args[' + argIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.args, argIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.args.push(null)">Add Argument</a>
                                                    </div>
                                                </div>

                                                <div class="col repeater" v-if="!editMode || container.hasOwnProperty('command')">
                                                    <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].command'">
                                                        <div class="header" :class="[container.command.length ? 'marginBottom' : 'no-margin' ]">
                                                            <h5 :for="'spec.shards.overrides.pods.customContainers[' + index + '].command'">
                                                                Command
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.command')"></span> 
                                                            </h5>
                                                        </div>
                                                        <template v-for="(command, commandIndex) in container.command">
                                                            <div :key="'command-' + commandIndex" class="inputContainer" :class="(container.command.length !== (commandIndex + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off" 
                                                                    :disabled="editMode"
                                                                    :key="'command-' + commandIndex" 
                                                                    v-model="container.command[commandIndex]" 
                                                                    :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].command[' + commandIndex + ']'"
                                                                >
                                                                <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.command, commandIndex)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="!editMode">
                                                        <a class="addRow" @click="container.command.push(null)">Add Command</a>
                                                    </div>
                                                </div>
                                            </div>

                                            <div class="repeater marginBottom marginTop" v-if="!editMode || container.hasOwnProperty('env')">
                                                <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].env'">
                                                    <div class="header" :class="!container.env.length && 'noMargin'">
                                                        <h5 :for="'spec.shards.overrides.pods.customContainers[' + index + '].env'">
                                                            Environment Variables
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.env')"></span> 
                                                        </h5>
                                                    </div>
                                                    <div class="variable" v-if="container.env.length">
                                                        <div class="row" v-for="(env, envIndex) in container.env" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].env[' + envIndex + ']'">
                                                            <label>Name</label>
                                                            <input :required="!isNull(env.value)" :disabled="editMode" class="label" v-model="env.name" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].env[' + envIndex + '].name'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input :disabled="editMode" class="labelValue" v-model="env.value" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].env[' + envIndex + '].value'">

                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.env, envIndex)">Delete</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.env.push({ name: null, value: null})">Add Variable</a>
                                                </div>
                                            </div>

                                            <br/>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('ports')">
                                                <div class="header">
                                                    <h5>
                                                        Ports
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater marginBottom">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers.ports'"
                                                        v-if="container.ports.length"
                                                    >
                                                        <div class="section" v-for="(port, portIndex) in container.ports" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].ports[' + portIndex + ']'">
                                                            <div class="header">
                                                                <h6>Port #{{ portIndex + 1 }}{{ !isNull(port.name) ? (': ' + port.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.ports, portIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.ports.name">Name</label>  
                                                                    <input :disabled="editMode" v-model="port.name" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].ports[' + portIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.ports.hostIP">Host IP</label>  
                                                                    <input :disabled="editMode" v-model="port.hostIP" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].ports[' + portIndex + '].hostIP'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.hostIP')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.ports.hostPort">Host Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.hostPort" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].ports[' + portIndex + '].hostPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.hostPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.ports.containerPort">Container Port</label>  
                                                                    <input :disabled="editMode" type="number" v-model="port.containerPort" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].ports[' + portIndex + '].containerPort'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.containerPort')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.ports.protocol">Protocol</label>  
                                                                    <select :disabled="editMode" v-model="port.protocol" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].ports[' + portIndex + '].protocol'">
                                                                        <option :value="nullVal" selected>Choose one...</option>
                                                                        <option>TCP</option>
                                                                        <option>UDP</option>
                                                                        <option>SCTP</option>
                                                                    </select>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.ports.items.properties.protocol')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('ports') && !container.ports.length) && 'topBorder'">
                                                        <a class="addRow" @click="!container.hasOwnProperty('ports') && (container['ports'] = []); container.ports.push({
                                                            name: null,
                                                            hostIP: null,
                                                            hostPort: null,
                                                            containerPort: null,
                                                            protocol: null
                                                        })">
                                                            Add Port
                                                        </a>
                                                    </div>
                                                </div>
                                                <br/>
                                            </template>
                                            
                                            <template v-if="!editMode || container.hasOwnProperty('volumeMounts')">
                                                <div class="header">
                                                    <h5>
                                                        Volume Mounts
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater">
                                                    <fieldset
                                                        class="noPaddingBottom"
                                                        :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].volumeMounts'"
                                                        v-if="container.hasOwnProperty('volumeMounts') && container.volumeMounts.length"
                                                    >
                                                        <div class="section" v-for="(mount, mountIndex) in container.volumeMounts" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].volumeMounts[' + mountIndex + ']'">
                                                            <div class="header">
                                                                <h6>Mount #{{ mountIndex + 1 }}{{ !isNull(mount.name) ? (': ' + mount.name) : '' }}</h6>
                                                                <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.volumeMounts, mountIndex)">Delete</a>
                                                            </div>

                                                            <div class="row-50">
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.volumeMounts.name">Name</label>  
                                                                    <input :required="!isNull(mount.mountPath)" :disabled="editMode" v-model="mount.name" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].name'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.name')"></span>
                                                                </div>
                                                                <div class="col">                    
                                                                    <label :for="'spec.shards.overrides.pods.customContainers[' + index + '].volumeMounts.readOnly'">
                                                                        Read Only
                                                                    </label>  
                                                                    <label :disabled="editMode" :for="'spec.shards.overrides.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" class="switch yes-no">
                                                                        Enable
                                                                        <input :disabled="editMode" type="checkbox" :id="'spec.shards.overrides.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" v-model="mount.readOnly" data-switch="NO" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'">
                                                                    </label>
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.readOnly')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.volumeMounts.mountPath">Mount Path</label>  
                                                                    <input :required="!isNull(mount.name)" :disabled="editMode" v-model="mount.mountPath" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.mountPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.volumeMounts.mountPropagation">Mount Propagation</label>  
                                                                    <input :disabled="editMode" v-model="mount.mountPropagation" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPropagation'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.volumeMounts.subPath">Sub Path</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPathExpr') && !isNull(mount.subPathExpr))" v-model="mount.subPath" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].subPath'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.subPath')"></span>
                                                                </div>
                                                                <div class="col">
                                                                    <label for="spec.shards.overrides.pods.customContainers.volumeMounts.subPathExpr">Sub Path Expr</label>  
                                                                    <input :disabled="editMode || (mount.hasOwnProperty('subPath') && !isNull(mount.subPath))" v-model="mount.subPathExpr" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].subPathExpr'" autocomplete="off">
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.customContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </fieldset>
                                                    <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('volumeMounts') && !container.volumeMounts.length) && 'topBorder'">
                                                        <a
                                                            class="addRow"
                                                            @click="
                                                                !container.hasOwnProperty('volumeMounts') && (container['volumeMounts'] = []); container.volumeMounts.push({
                                                                    mountPath: null,
                                                                    mountPropagation: null,
                                                                    name: null,
                                                                    readOnly: false,
                                                                    subPath: null,
                                                                    subPathExpr: null
                                                                });
                                                                formHash = (+new Date).toString();
                                                            "
                                                        >
                                                            Add Volume
                                                        </a>
                                                    </div>
                                                </div>
                                            </template>
                                        </div>
                                    </template>
                                </fieldset>
                                <div v-if="!editMode" class="fieldsetFooter" :class="!shards.overrides[overrideIndex].pods.customContainers.length && 'topBorder'">
                                    <a 
                                        class="addRow"
                                        @click="shards.overrides[overrideIndex].pods.customContainers.push({
                                            name: null,
                                            image: null,
                                            imagePullPolicy: null,
                                            args: [null],
                                            command: [null],
                                            workingDir: null,
                                            env: [ { name: null, value: null } ],
                                            ports: [{
                                                containerPort: null,
                                                hostIP: null,
                                                hostPort: null,
                                                name: null,
                                                protocol: null
                                            }],
                                            volumeMounts: [{
                                                mountPath: null,
                                                mountPropagation: null,
                                                name: null,
                                                readOnly: false,
                                                subPath: null,
                                                subPathExpr: null,
                                            }]
                                        })"
                                    >
                                        Add Container
                                    </a>
                                </div>
                            </div>

                            <br/><br/><br/>

                        </template>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.overrides == 'pods-replication')" class="step active" :data-fieldset="'shards.overrides[' + overrideIndex + '].pods-replication'">
                    <div class="header">
                        <h2>Replication</h2>
                    </div>

                    <div class="fields">                    
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.shards.overrides.replication.mode">Mode</label>
                                <select v-model="shards.overrides[overrideIndex].replication.mode" required :data-field="'spec.shards.overrides[' + overrideIndex + '].replication.mode'" @change="['sync', 'strict-sync'].includes(shards.overrides[overrideIndex].replication.mode) ? (shards.overrides[overrideIndex].replication['syncInstances'] = 1) : ((shards.overrides[overrideIndex].replication.hasOwnProperty('syncInstances') && delete shards.overrides[overrideIndex].replication.syncInstances) )">    
                                    <option selected>async</option>
                                    <option>sync</option>
                                    <option>strict-sync</option>
                                    <option>sync-all</option>
                                    <option>strict-sync-all</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.replication.mode')"></span>
                            </div>

                            <div class="col" v-if="['sync', 'strict-sync'].includes(shards.overrides[overrideIndex].replication.mode)">
                                <label for="spec.shards.overrides.replication.syncInstances">Sync Instances</label>
                                <input type="number" min="1" :max="(shards.overrides[overrideIndex].instancesPerCluster - 1)" v-model="shards.overrides[overrideIndex].replication.syncInstances" :data-field="'spec.shards.overrides[' + overrideIndex + '].replication.syncInstances'">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.replication.syncInstances')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.overrides == 'services')" class="step active" :data-fieldset="'shards.overrides[' + overrideIndex + '].services'">
                    <div class="header">
                        <h2>Customize generated Kubernetes service</h2>
                    </div>

                    <div class="fields">                    
                        <div class="header">
                            <h3 for="spec.postgresServices.shards.overrides.primaries">
                                Primaries Service
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.primaries')"></span>
                            </h3>
                        </div>
                        
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.postgresServices.shards.overrides.primaries.enabled">Service</label>  
                                <label for="postgresServicesPrimaries" class="switch yes-no" data-field="spec.postgresServices.shards.overrides.primaries.enabled">Enable<input type="checkbox" id="postgresServicesPrimaries" v-model="postgresServices.shards.overrides.primaries.enabled" data-switch="YES"></label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.primaries.enabled')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.postgresServices.shards.overrides.primaries.type">Type</label>
                                <select v-model="postgresServices.shards.overrides.primaries.type" required data-field="spec.postgresServices.shards.overrides.primaries.type">    
                                    <option>ClusterIP</option>
                                    <option>LoadBalancer</option>
                                    <option>NodePort</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.primaries.type')"></span>
                            </div>

                            <div class="col">
                                <label>Load Balancer IP</label>
                                <input 
                                    v-model="postgresServices.shards.overrides.primaries.loadBalancerIP" 
                                    autocomplete="off" 
                                    data-field="spec.postgresServices.shards.overrides.primaries.loadBalancerIP">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.primaries.loadBalancerIP')"></span>
                            </div>
                        </div>

                        <div class="repeater sidecars">
                            <div class="header">
                                <h4 for="spec.postgresServices.shards.overrides.customPorts">
                                    Custom Ports
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.customPorts')"></span>
                                </h4>
                            </div>
                            <fieldset
                                data-field="spec.postgresServices.shards.overrides.customPorts"
                                v-if="postgresServices.shards.overrides.hasOwnProperty('customPorts') && postgresServices.shards.overrides.customPorts.length"
                            >
                                <div class="section" v-for="(port, index) in postgresServices.shards.overrides.customPorts">
                                    <div class="header">
                                        <h5>Port #{{ index + 1 }}</h5>
                                        <a class="addRow delete" @click="spliceArray(postgresServices.shards.overrides.customPorts, index)">Delete</a>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.overrides.customPorts.appProtocol">Application Protocol</label>  
                                            <input v-model="port.appProtocol" :data-field="'spec.postgresServices.shards.overrides.customPorts[' + index + '].appProtocol'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.customPorts.appProtocol')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.overrides.customPorts.name">Name</label>  
                                            <input v-model="port.name" :data-field="'spec.postgresServices.shards.overrides.customPorts[' + index + '].name'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.customPorts.name')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.overrides.customPorts.nodePort">Node Port</label>  
                                            <input type="number" v-model="port.nodePort" :data-field="'spec.postgresServices.shards.overrides.customPorts[' + index + '].nodePort'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.customPorts.nodePort')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.overrides.customPorts.port">Port</label>  
                                            <input 
                                                type="number"
                                                v-model="port.port"
                                                :data-field="'spec.postgresServices.shards.overrides.customPorts[' + index + '].port'"
                                                :required="(port.appProtocol != null) || (port.name != null) || (port.nodePort != null) || (port.protocol != null) || (port.targetPort != null)"
                                                autocomplete="off"
                                            >
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.customPorts.port')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.overrides.customPorts.protocol">Protocol</label>  
                                            <select v-model="port.protocol" :data-field="'spec.postgresServices.shards.overrides.customPorts[' + index + '].protocol'">
                                                <option :value="nullVal" selected>Choose one...</option>
                                                <option>TCP</option>
                                                <option>UDP</option>
                                                <option>SCTP</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.customPorts.protocol')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.postgresServices.shards.overrides.customPorts.targetPort">Target Port</label>  
                                            <input v-model="port.targetPort" :data-field="'spec.postgresServices.shards.overrides.customPorts[' + index + '].targetPort'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.postgresServices.shards.overrides.customPorts.targetPort')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="(postgresServices.shards.overrides.hasOwnProperty('customPorts') && !postgresServices.shards.overrides.customPorts.length) && 'topBorder'">
                                <a class="addRow" @click="!postgresServices.shards.overrides.hasOwnProperty('customPorts') && (postgresServices.shards['customPorts'] = []); postgresServices.shards.overrides.customPorts.push({
                                    appProtocol: null,
                                    name: null,
                                    nodePort: null,
                                    port: null,
                                    protocol: null,
                                    targetPort: null
                                })">
                                    Add Port
                                </a>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.overrides == 'metadata')"  class="step active podsMetadata" :data-fieldset="'shards.overrides[' + overrideIndex + '].metadata'">
                    <div class="header">
                        <h2>Metadata</h2>
                    </div>

                    <div class="fields">
                        <div class="repeater">
                            <div class="header">
                                <h3 for="spec.shards.overrides.metadata.labels">
                                    Labels
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.labels')"></span> 
                                </h3>
                            </div>

                            <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.labels.clusterPods'">
                                <div class="header" :class="( !hasProp(shards.overrides[overrideIndex], 'metadata.labels.clusterPods') || !shards.overrides[overrideIndex].metadata.labels.clusterPods.length ) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.overrides.metadata.labels.clusterPods">
                                        Cluster Pods
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.labels.clusterPods')"></span> 
                                    </h3>
                                </div>
                                <div class="metadata" v-if="hasProp(shards.overrides[overrideIndex], 'metadata.labels.clusterPods') && shards.overrides[overrideIndex].metadata.labels.clusterPods.length">
                                    <div class="row" v-for="(field, index) in shards.overrides[overrideIndex].metadata.labels.clusterPods">
                                        <label>Label</label>
                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.labels.clusterPods[' + index + '].label'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.labels.clusterPods[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.overrides[overrideIndex].metadata.labels.clusterPods, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards.overrides[overrideIndex], 'metadata.labels.clusterPods', { label: '', value: ''})">Add Label</a>
                            </div>
                        </div>

                        <br/><hr/><br/>

                        
                        <div class="header">
                            <h3 for="spec.shards.overrides.metadata.annotations">
                                Annotations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations')"></span>
                            </h3>
                        </div>

                        <div class="repeater">
                            <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.allResources'">
                                <div class="header" :class="(!hasProp(shards.overrides[overrideIndex], 'metadata.annotations.allResources') || !shards.overrides[overrideIndex].metadata.annotations.allResources.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.overrides.metadata.annotations.allResources">
                                        All Resources
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.allResources')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(shards.overrides[overrideIndex], 'metadata.annotations.allResources') && shards.overrides[overrideIndex].metadata.annotations.allResources.length)">
                                    <div class="row" v-for="(field, index) in shards.overrides[overrideIndex].metadata.annotations.allResources">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.allResources[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.allResources[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.overrides[overrideIndex].metadata.annotations.allResources, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards.overrides[overrideIndex], 'metadata.annotations.allResources', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>
                        
                        <br/><br/>

                        <div class="repeater">
                            <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.clusterPods'">
                                <div class="header" :class="(!hasProp(shards.overrides[overrideIndex], 'metadata.annotations.clusterPods') || !shards.overrides[overrideIndex].metadata.annotations.clusterPods.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.overrides.metadata.annotations.clusterPods">
                                        Cluster Pods
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.clusterPods')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(shards.overrides[overrideIndex], 'metadata.annotations.clusterPods') && shards.overrides[overrideIndex].metadata.annotations.clusterPods.length)">
                                    <div class="row" v-for="(field, index) in shards.overrides[overrideIndex].metadata.annotations.clusterPods">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.clusterPods[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.clusterPods[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.overrides[overrideIndex].metadata.annotations.clusterPods, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards.overrides[overrideIndex], 'metadata.annotations.clusterPods', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.services'">
                                <div class="header" :class="(!hasProp(shards.overrides[overrideIndex], 'metadata.annotations.services') || !shards.overrides[overrideIndex].metadata.annotations.services.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.overrides.metadata.annotations.services">
                                        Services
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.services')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(shards.overrides[overrideIndex], 'metadata.annotations.services') && shards.overrides[overrideIndex].metadata.annotations.services.length)">
                                    <div class="row" v-for="(field, index) in shards.overrides[overrideIndex].metadata.annotations.services">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.services[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.services[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.overrides[overrideIndex].metadata.annotations.services, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards.overrides[overrideIndex], 'metadata.annotations.services', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.primaryService'">
                                <div class="header" :class="(!hasProp(shards.overrides[overrideIndex], 'metadata.annotations.primaryService') || !shards.overrides[overrideIndex].metadata.annotations.primaryService.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.overrides.metadata.annotations.primaryService">
                                        Primary Service 
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.primaryService')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="(hasProp(shards.overrides[overrideIndex], 'metadata.annotations.primaryService') && shards.overrides[overrideIndex].metadata.annotations.primaryService.length)">
                                    <div class="row" v-for="(field, index) in shards.overrides[overrideIndex].metadata.annotations.primaryService">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.primaryService[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.primaryService[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.overrides[overrideIndex].metadata.annotations.primaryService, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards.overrides[overrideIndex], 'metadata.annotations.primaryService', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.replicasService'">
                                <div class="header" :class="(!hasProp(shards.overrides[overrideIndex], 'metadata.annotations.replicasService') || !shards.overrides[overrideIndex].metadata.annotations.replicasService.length) && 'noMargin noPadding'">
                                    <h3 for="spec.shards.overrides.metadata.annotations.replicasService">
                                        Replicas Service
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.metadata.annotations.replicasService')"></span>
                                    </h3>
                                </div>
                                <div class="annotation repeater" v-if="(hasProp(shards.overrides[overrideIndex], 'metadata.annotations.replicasService') && shards.overrides[overrideIndex].metadata.annotations.replicasService.length)">
                                    <div class="row" v-for="(field, index) in shards.overrides[overrideIndex].metadata.annotations.replicasService">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.replicasService[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].metadata.annotations.replicasService[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(shards.overrides[overrideIndex].metadata.annotations.replicasService, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushElement(shards.overrides[overrideIndex], 'metadata.annotations.replicasService', { label: '', value: ''})">Add Annotation</a>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset v-if="(currentStep.overrides == 'scheduling')" class="step active podsMetadata" id="podsSchedulingShards" :data-fieldset="'shards.overrides[' + overrideIndex + '].scheduling'">
                    <div class="header">
                        <h2>Pods Scheduling</h2>
                    </div>
                    
                    <div class="fields">
                        <div class="repeater">
                            <div class="header">
                                <h3 for="spec.shards.overrides.pods.scheduling.nodeSelector">
                                    Node Selectors
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeSelector')"></span>
                                </h3>
                            </div>
                            <fieldset v-if="(hasProp(shards.overrides[overrideIndex], 'pods.scheduling.nodeSelector') && shards.overrides[overrideIndex].pods.scheduling.nodeSelector.length)" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeSelector'">
                                <div class="scheduling">
                                    <div class="row" v-for="(field, index) in shards.overrides[overrideIndex].pods.scheduling.nodeSelector">
                                        <label>Label</label>
                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeSelector[' + index + '].label'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeSelector[' + index + '].value'">
                                        
                                        <a class="addRow" @click="spliceArray(shards.overrides[overrideIndex].pods.scheduling.nodeSelector, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( (!hasProp(shards.overrides[overrideIndex], 'pods.scheduling.nodeSelector') || !shards.overrides[overrideIndex].pods.scheduling.nodeSelector.length) && 'topBorder' )">
                                <a class="addRow" @click="pushElement(shards.overrides[overrideIndex], 'pods.scheduling.nodeSelector', { label: '', value: ''})">Add Node Selector</a>
                            </div>
                        </div>

                        <br/><br/>
                    
                        <div class="header">
                            <h3 for="spec.shards.overrides.pods.scheduling.tolerations">
                                Node Tolerations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations')"></span>
                            </h3>
                        </div>
                
                        <div class="scheduling repeater">
                            <fieldset v-if="(hasProp(shards.overrides[overrideIndex], 'pods.scheduling.tolerations') && shards.overrides[overrideIndex].pods.scheduling.tolerations.length)" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.tolerations'">
                                <div class="section" v-for="(field, index) in shards.overrides[overrideIndex].pods.scheduling.tolerations">
                                    <div class="header">
                                        <h4 for="spec.shards.overrides.pods.scheduling.tolerations">Toleration #{{ index+1 }}</h4>
                                        <a class="addRow del" @click="spliceArray(shards.overrides[overrideIndex].pods.scheduling.tolerations, index)">Delete</a>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label :for="'spec.shards.overrides.pods.scheduling.tolerations[' + index + '].key'">Key</label>
                                            <input v-model="field.key" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.tolerations[' + index + '].key'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.key')"></span>
                                        </div>
                                        
                                        <div class="col">
                                            <label :for="'spec.shards.overrides.pods.scheduling.tolerations[' + index + '].operator'">Operator</label>
                                            <select v-model="field.operator" @change="( (field.operator == 'Exists') ? (delete field.value) : (field.value = '') )" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.tolerations[' + index + '].operator'">
                                                <option>Equal</option>
                                                <option>Exists</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.operator')"></span>
                                        </div>

                                        <div class="col" v-if="field.operator == 'Equal'">
                                            <label :for="'spec.shards.overrides.pods.scheduling.tolerations[' + index + '].value'">Value</label>
                                            <input v-model="field.value" :disabled="(field.operator == 'Exists')" autocomplete="off" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.tolerations[' + index + '].value'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.value')"></span>
                                        </div>

                                        <div class="col">
                                            <label :for="'spec.shards.overrides.pods.scheduling.tolerations[' + index + '].operator'">Effect</label>
                                            <select v-model="field.effect" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.tolerations[' + index + '].effect'">
                                                <option :value="nullVal">MatchAll</option>
                                                <option>NoSchedule</option>
                                                <option>PreferNoSchedule</option>
                                                <option>NoExecute</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.effect')"></span>
                                        </div>

                                        <div class="col" v-if="field.effect == 'NoExecute'">
                                            <label :for="'spec.shards.overrides.pods.scheduling.tolerations[' + index + '].tolerationSeconds'">Toleration Seconds</label>
                                            <input type="number" min="0" v-model="field.tolerationSeconds" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.tolerations[' + index + '].tolerationSeconds'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( (!hasProp(shards.overrides[overrideIndex], 'pods.scheduling.tolerations') || !shards.overrides[overrideIndex].pods.scheduling.tolerations.length) && 'topBorder')">
                                <a class="addRow" @click="pushElement(shards.overrides[overrideIndex], 'pods.scheduling.tolerations', { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null })">Add Toleration</a>
                            </div>
                        </div>

                        <br/><br/><br/>

                        <div class="header">
                            <h3 for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                <span class="normal">Required During Scheduling Ignored During Execution</span>
                            </h3>                            
                        </div>

                        <br/><br/>
                        
                        <div class="scheduling repeater">
                            <div class="header">
                                <h4 for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                    Node Selector Terms
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')"></span>
                                </h4>
                            </div>
                            <fieldset v-if="(hasProp(shards.overrides[overrideIndex], 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') && shards.overrides[overrideIndex].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length)">
                                <div class="section" v-for="(requiredAffinityTerm, termIndex) in shards.overrides[overrideIndex].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                    <div class="header">
                                        <h5>Term #{{ termIndex + 1 }}</h5>
                                        <a class="addRow" @click="spliceArray(shards.overrides[overrideIndex].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms, termIndex)">Delete</a>
                                    </div>
                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions">
                                                Match Expressions
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span> 
                                            </label>
                                        </div>
                                        <fieldset v-if="requiredAffinityTerm.matchExpressions.length">
                                            <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.matchExpressions">
                                                <div class="header">
                                                    <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items">
                                                        Match Expression #{{ expIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items')"></span> 
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchExpressions, expIndex)">Delete</a>
                                                </div>
                                                
                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">Key</label>
                                                        <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key'">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span> 
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">Operator</label>
                                                        <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( !expression.hasOwnProperty('values') && (expression['values'] = ['']) ) )" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator'">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span> 
                                                    </div>
                                                </div>

                                                <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values'">
                                                        <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                    <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!requiredAffinityTerm.matchExpressions.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchExpressions)">Add Expression</a>
                                    </div>

                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields">
                                                Match Fields
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span> 
                                            </label>
                                        </div>
                                        <fieldset v-if="requiredAffinityTerm.matchFields.length">
                                            <div class="section" v-for="(field, fieldIndex) in requiredAffinityTerm.matchFields">
                                                <div class="header">
                                                    <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items">
                                                        Match Field #{{ fieldIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items')"></span> 
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchFields, fieldIndex)">Delete</a>
                                                </div>
                                                
                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">Key</label>
                                                        <input v-model="field.key" autocomplete="off" placeholder="Type a key..." :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key'">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span> 
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">Operator</label>
                                                        <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( !field.hasOwnProperty('values') && (field['values'] = ['']) ) )" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator'">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                    </div>
                                                </div>

                                                <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span> 
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values'">
                                                        <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="!['Gt', 'Lt'].includes(field.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(field.operator)" :class="!field.values.length && 'topBorder'">
                                                    <a class="addRow" @click="field.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!requiredAffinityTerm.matchFields.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchFields)">Add Field</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="( (!hasProp(shards.overrides[overrideIndex], 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') || !shards.overrides[overrideIndex].pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length) && 'topBorder' )">
                                <a class="addRow" @click="addRequiredAffinityTerm(shards.overrides[overrideIndex], 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')">Add Term</a>
                            </div>
                        </div>

                        <br/><br/>
                    
                        <div class="header">
                            <h3 for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                <span class="normal">Preferred During Scheduling Ignored During Execution</span>
                            </h3>
                        </div>

                        <br/><br/>

                        <div class="scheduling repeater">
                            <div class="header">
                                <h4 for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items">
                                    Node Selector Terms
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span> 
                                </h4>
                            </div>
                            <fieldset v-if="(hasProp(shards.overrides[overrideIndex], 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') && shards.overrides[overrideIndex].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.length)">
                                <div class="section" v-for="(preferredAffinityTerm, termIndex) in shards.overrides[overrideIndex].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                    <div class="header">
                                        <h5>Term #{{ termIndex + 1 }}</h5>
                                        <a class="addRow" @click="spliceArray(shards.overrides[overrideIndex].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution, termIndex)">Delete</a>
                                    </div>
                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions">
                                                Match Expressions
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                            </label>
                                        </div>
                                        <fieldset v-if="preferredAffinityTerm.preference.matchExpressions.length">
                                            <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.preference.matchExpressions">
                                                <div class="header">
                                                    <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                        Match Expression #{{ expIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchExpressions, expIndex)">Delete</a>
                                                </div>

                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                        <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key'">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                        <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator'">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                    </div>
                                                </div>

                                                <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values'">
                                                        <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                    <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!preferredAffinityTerm.preference.matchExpressions.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchExpressions)">Add Expression</a>
                                    </div>

                                    <fieldset class="affinityMatch noMargin">
                                        <div class="header">
                                            <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields">
                                                Match Fields
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                            </label>
                                        </div>
                                        <fieldset v-if="preferredAffinityTerm.preference.matchFields.length">
                                            <div class="section" v-for="(field, fieldIndex) in preferredAffinityTerm.preference.matchFields">
                                                <div class="header">
                                                    <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items">
                                                        Match Field #{{ fieldIndex + 1 }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items')"></span>
                                                    </label>
                                                    <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchFields, fieldIndex)">Delete</a>
                                                </div>

                                                <div class="row-50">
                                                    <div class="col">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">Key</label>
                                                        <input v-model="field.key" autocomplete="off" placeholder="Type a key..." :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key'">
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                    </div>

                                                    <div class="col">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">Operator</label>
                                                        <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( (!field.hasOwnProperty('values') || (field.values.length > 1) ) && (field['values'] = ['']) ) )" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator'">
                                                            <option value="" selected>Select an operator</option>
                                                            <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                        </select>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                    </div>
                                                </div>

                                                <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                    <div class="header">
                                                        <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                            {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span> 
                                                        </label>
                                                    </div>
                                                    <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                        <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                            Value #{{ (valIndex + 1) }}
                                                        </label>
                                                        <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values'">
                                                        <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="!['Gt', 'Lt'].includes(field.operator)">Delete</a>
                                                    </div>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(field.operator)" :class="!field.values.length && 'topBorder'">
                                                    <a class="addRow" @click="field.values.push('')">Add Value</a>
                                                </div>
                                            </div>
                                        </fieldset>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!preferredAffinityTerm.preference.matchFields.length && 'topBorder'">
                                        <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchFields)">Add Field</a>
                                    </div>

                                    <label for="spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">Weight</label>
                                    <input v-model="preferredAffinityTerm.weight" autocomplete="off" type="number" min="1" max="100" class="affinityWeight" :data-field="'spec.shards.overrides[' + overrideIndex + '].pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight'">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.overrides.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight')"></span>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="!preferredAffinity.length && 'topBorder'">
                                <a class="addRow" @click="addPreferredAffinityTerm(shards.overrides[overrideIndex], 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">Add Term</a>
                            </div>
                        </div>

                        <span class="warning" v-if="editMode">Please, be aware that any changes made to the <code>Pods Scheduling</code> will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance of the cluster</span>
                    </div>
                </fieldset>
            </template>

            <div class="clearfix"></div>

            <hr/>
            
            <template v-if="editMode">
                <button type="submit" class="btn" @click="createCluster(false)">Update Cluster</button>
            </template>
            <template v-else>
                <button type="submit" class="btn" @click="createCluster(false)">Create Cluster</button>
            </template>

            <button type="button" class="btn floatRight" @click="createCluster(true)">View Summary</button>

            <button type="button" @click="cancel" class="btn border">Cancel</button>
        
        </form>
        
        <ShardedClusterSummary :cluster="previewCRD" :extensionsList="extensionsList[flavor][postgresVersion]" v-if="showSummary" @closeSummary="showSummary = false"></ShardedClusterSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import {sgclusterform} from '../mixins/forms/sgclusterform'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import ShardedClusterSummary from './summary/SGShardedClusterSummary.vue'

    export default {
        name: 'CreateSGShardedClusters',

        mixins: [mixin, sgclusterform],

        components: {
          ShardedClusterSummary
        },

        data: function() {

            const vc = this;

            return {
                formSections: ['general', 'coordinator', 'shards', 'overrides'],
                currentSection: 'general',
                formSteps: {
                    general: ['cluster', 'extensions', 'backups', 'sidecars', 'pods-replication', 'metadata', 'non-production'],
                    coordinator: ['coordinator', 'sidecars', 'scripts', 'pods', 'pods-replication', 'services', 'metadata', 'scheduling'],
                    shards: ['shards', 'sidecars', 'scripts', 'pods', 'pods-replication', 'services', 'metadata', 'scheduling'],
                    overrides: ['shards', 'sidecars', 'scripts', 'pods', 'pods-replication', 'metadata', 'scheduling']
                },
                currentStep: {
                    general: 'cluster',
                    coordinator: 'coordinator',
                    shards: 'shards',
                    overrides: 'shards',
                },
                basicSteps: {
                    general: 3,
                    coordinator: 2,
                    shards: 2,
                    overrides: 2,
                },
                editMode: (vc.$route.name === 'EditShardedCluster'),
                editReady: false,
                database: '',
                shardingType: 'citus',
                backups: [{
                    paths: [null],
                    compression: 'lz4',
                    cronSchedule: '0 5 * * *',
                    retention: 5,
                    performance: {
                        maxNetworkBandwidth: '',
                        maxDiskBandwidth: '',
                        uploadDiskConcurrency: 1
                    },
                    sgObjectStorage: ''
                }],
                replication: {
                    mode: 'async'
                },
                pgConfigExists: true,
                ssl: {
                    enabled: true,
                    certificateSecretKeySelector: {
                        name: '',
                        key: ''
                    },
                    privateKeySecretKeySelector: {
                        name: '',
                        key: ''
                    }
                },
                postgresServices: {
                    coordinator: {
                        any: {
                            enabled: true,
                            type: 'ClusterIP',
                            loadBalancerIP: '',
                        },
                        primary: {
                            enabled: true,
                            type: 'ClusterIP',
                            loadBalancerIP: '',
                        },
                        customPorts: [{
                            appProtocol: null,
                            name: null,
                            nodePort: null,
                            port: null,
                            protocol: null,
                            targetPort: null
                        }]
                    },
                    shards: {
                        primaries: {
                            enabled: true,
                            type: 'ClusterIP',
                            loadBalancerIP: '',
                        },
                        customPorts: [{
                            appProtocol: null,
                            name: null,
                            nodePort: null,
                            port: null,
                            protocol: null,
                            targetPort: null
                        }]
                    }
                },
                coordinator: {
                    instances: 1,
                    sgInstanceProfile: '',
                    replication: {
                        mode: 'sync-all'
                    },
                    configurations: {
                        sgPostgresConfig: '',
                        sgPoolingConfig: '',
                    },
                    managedSql: {
                        continueOnSGScriptError: false,
                        scripts: [ {} ]
                    },
                    metadata: {
                        labels: {
                            clusterPods: [ { label: '', value: ''} ],
                        },
                        annotations: {
                            allResources: [ { annotation: '', value: ''} ],
                            clusterPods: [ { annotation: '', value: ''} ],
                            primaryService: [ { annotation: '', value: ''} ],
                            replicasService: [ { annotation: '', value: ''} ],
                            services: [ { annotation: '', value: ''} ],
                        }
                    },
                    pods: {
                        disableConnectionPooling: false,
                        disablePostgresUtil: false,
                        disableMetricsExporter: false,
                        persistentVolume: {
                            size: {
                                size: 1,
                                unit: 'Gi'
                            },
                            storageClass: ''
                        },
                        customVolumes: [{
                            name: null,
                        }],
                        customInitContainers: [{
                            name: null,
                            image: null,
                            imagePullPolicy: null,
                            args: [null],
                            command: [null],
                            workingDir: null,
                            env: [ { name: null, value: null } ],
                            ports: [{
                                containerPort: null,
                                hostIP: null,
                                hostPort: null,
                                name: null,
                                protocol: null
                            }],
                            volumeMounts: [{
                                mountPath: null,
                                mountPropagation: null,
                                name: null,
                                readOnly: false,
                                subPath: null,
                                subPathExpr: null,
                            }]
                        }],
                        customContainers: [{
                            name: null,
                            image: null,
                            imagePullPolicy: null,
                            args: [null],
                            command: [null],
                            workingDir: null,
                            env: [ { name: null, value: null } ],
                            ports: [{
                                containerPort: null,
                                hostIP: null,
                                hostPort: null,
                                name: null,
                                protocol: null
                            }],
                            volumeMounts: [{
                                mountPath: null,
                                mountPropagation: null,
                                name: null,
                                readOnly: false,
                                subPath: null,
                                subPathExpr: null,
                            }]
                        }],
                        scheduling: {
                            nodeSelector: [ { label: '', value: ''} ],
                            tolerations: [ { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null } ],
                            nodeAffinity: {
                                requiredDuringSchedulingIgnoredDuringExecution: {
                                    nodeSelectorTerms: [
                                        {   
                                            matchExpressions: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ],
                                            matchFields: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ]
                                        }
                                    ],
                                },
                                preferredDuringSchedulingIgnoredDuringExecution: [
                                    {
                                        preference: {
                                            matchExpressions: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ],
                                            matchFields: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ]
                                        },
                                        weight:  1
                                    }
                                ],
                            },
                        }
                    },
                },
                shards: {
                    clusters: 1,
                    instancesPerCluster: 1,
                    sgInstanceProfile: '',
                    replication: {
                        mode: 'async'
                    },
                    configurations: {
                        sgPostgresConfig: '',
                        sgPoolingConfig: '',
                    },
                    managedSql: {
                        continueOnSGScriptError: false,
                        scripts: [ {} ]
                    },
                    metadata: {
                        labels: {
                            clusterPods: [ { label: '', value: ''} ],
                        },
                        annotations: {
                            allResources: [ { annotation: '', value: ''} ],
                            clusterPods: [ { annotation: '', value: ''} ],
                            primaryService: [ { annotation: '', value: ''} ],
                            replicasService: [ { annotation: '', value: ''} ],
                            services: [ { annotation: '', value: ''} ],
                        }
                    },
                    pods: {
                        disableConnectionPooling: false,
                        disablePostgresUtil: false,
                        disableMetricsExporter: false,
                        persistentVolume: {
                            size: {
                                size: 1,
                                unit: 'Gi'
                            },
                            storageClass: ''
                        },
                        customVolumes: [{
                            name: null,
                        }],
                        customInitContainers: [{
                            name: null,
                            image: null,
                            imagePullPolicy: null,
                            args: [null],
                            command: [null],
                            workingDir: null,
                            env: [ { name: null, value: null } ],
                            ports: [{
                                containerPort: null,
                                hostIP: null,
                                hostPort: null,
                                name: null,
                                protocol: null
                            }],
                            volumeMounts: [{
                                mountPath: null,
                                mountPropagation: null,
                                name: null,
                                readOnly: false,
                                subPath: null,
                                subPathExpr: null,
                            }]
                        }],
                        customContainers: [{
                            name: null,
                            image: null,
                            imagePullPolicy: null,
                            args: [null],
                            command: [null],
                            workingDir: null,
                            env: [ { name: null, value: null } ],
                            ports: [{
                                containerPort: null,
                                hostIP: null,
                                hostPort: null,
                                name: null,
                                protocol: null
                            }],
                            volumeMounts: [{
                                mountPath: null,
                                mountPropagation: null,
                                name: null,
                                readOnly: false,
                                subPath: null,
                                subPathExpr: null,
                            }]
                        }],
                        scheduling: {
                            nodeSelector: [ { label: '', value: ''} ],
                            tolerations: [ { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null } ],
                            nodeAffinity: {
                                requiredDuringSchedulingIgnoredDuringExecution: {
                                    nodeSelectorTerms: [
                                        {   
                                            matchExpressions: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ],
                                            matchFields: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ]
                                        }
                                    ],
                                },
                                preferredDuringSchedulingIgnoredDuringExecution: [
                                    {
                                        preference: {
                                            matchExpressions: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ],
                                            matchFields: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ]
                                        },
                                        weight:  1
                                    }
                                ],
                            },
                        }
                    },
                    overrides: [],
                },
                scriptSource: {
                    coordinator: [ 
                        { base: '', entries: ['raw'] }
                    ],
                    shards: [ 
                        { base: '', entries: ['raw'] }
                    ],
                    overrides: [
                        [
                            { base: '', entries: ['raw'] }
                        ]
                    ]
                },
                currentScriptIndex: {
                    coordinator: { base: 0, entry: 0 },
                    shards: { base: 0, entry: 0 },
                    overrides: [{ base: 0, entry: 0 }]
                },
                customVolumesType: {
                    coordinator: [null], 
                    shards: [null],
                    overrides: [[null]]
                },
                overrideIndex: -1,
            }

        },
        
        computed: {

            sgshardedclusters() {
                return store.state.sgshardedclusters
            },

            cluster() {

                var vm = this;
                var cluster = {};
                
                if( vm.editMode && !vm.editReady ) {
                    store.state.sgshardedclusters.forEach(function( c ){
                        if( (c.data.metadata.name === vm.$route.params.name) && (c.data.metadata.namespace === vm.$route.params.namespace) ) {
                            vm.database = c.data.spec.database;
                            vm.shardingType = c.data.spec.type;

                            vm.managedBackups = vm.hasProp(c, 'data.spec.configurations.backups') && c.data.spec.configurations.backups.length;
                            if (vm.managedBackups) {
                                vm.backups = c.data.spec.configurations.backups;
                                let cronScheduleSplit = vm.tzCrontab(vm.backups[0].cronSchedule, true).split(' ');
                                vm.cronSchedule[0].ref = {};
                                vm.cronSchedule[0].ref.value = vm.backups[0].cronSchedule;
                                vm.cronSchedule[0].ref.min = cronScheduleSplit[0];
                                vm.cronSchedule[0].ref.hour = cronScheduleSplit[1];
                                vm.cronSchedule[0].ref.dom = cronScheduleSplit[2];
                                vm.cronSchedule[0].ref.month = cronScheduleSplit[3];
                                vm.cronSchedule[0].ref.dow = cronScheduleSplit[4];
                                vm.cronSchedule[0].min = cronScheduleSplit[0];
                                vm.cronSchedule[0].hour = cronScheduleSplit[1];
                                vm.cronSchedule[0].dom = cronScheduleSplit[2];
                                vm.cronSchedule[0].month = cronScheduleSplit[3];
                                vm.cronSchedule[0].dow = cronScheduleSplit[4];

                                if(!c.data.spec.configurations.backups[0].hasOwnProperty('performance')) {
                                    vm.backups[0].performance = {
                                        maxNetworkBandwidth: '',
                                        maxDiskBandwidth: '',
                                        uploadDiskConcurrency: 1
                                    }
                                }
                            }

                            vm.distributedLogs = (typeof c.data.spec.distributedLogs !== 'undefined') ? c.data.spec.distributedLogs.sgDistributedLogs : '';
                            vm.retention = vm.hasProp(c, 'data.spec.distributedLogs.retention') ? c.data.spec.distributedLogs.retention : ''; 
                            vm.replication = vm.hasProp(c, 'data.spec.replication') && c.data.spec.replication;
                            vm.prometheusAutobind =  (typeof c.data.spec.prometheusAutobind !== 'undefined') ? c.data.spec.prometheusAutobind : false;
                            vm.enableClusterPodAntiAffinity = vm.hasProp(c, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') ? !c.data.spec.nonProductionOptions.disableClusterPodAntiAffinity : true;
                            vm.babelfishFeatureGates = vm.hasProp(c, 'data.spec.nonProductionOptions.enabledFeatureGates') && c.data.spec.nonProductionOptions.enabledFeatureGates.includes('babelfish-flavor');
                            
                            vm.podsMetadata = vm.hasProp(c, 'data.spec.metadata.labels.clusterPods') ? vm.unparseProps(c.data.spec.metadata.labels.clusterPods, 'label') : [];
                            vm.annotationsAll = vm.hasProp(c, 'data.spec.metadata.annotations.allResources') ? vm.unparseProps(c.data.spec.metadata.annotations.allResources) : [];
                            vm.annotationsPods = vm.hasProp(c, 'data.spec.metadata.annotations.clusterPods') ? vm.unparseProps(c.data.spec.metadata.annotations.clusterPods) : [];
                            vm.annotationsServices = vm.hasProp(c, 'data.spec.metadata.annotations.services') ? vm.unparseProps(c.data.spec.metadata.annotations.services) : [];
                            vm.postgresServicesPrimaryAnnotations = vm.hasProp(c, 'data.spec.metadata.annotations.primaryService') ?  vm.unparseProps(c.data.spec.metadata.annotations.primaryService) : [];
                            vm.postgresServicesReplicasAnnotations = vm.hasProp(c, 'data.spec.metadata.annotations.replicasService') ?  vm.unparseProps(c.data.spec.metadata.annotations.replicasService) : [];
                            vm.postgresServices = vm.hasProp(c, 'data.spec.postgresServices') && c.data.spec.postgresServices;

                            vm.flavor = c.data.spec.postgres.hasOwnProperty('flavor') ? c.data.spec.postgres.flavor : 'vanilla' ;
                            vm.selectedExtensions = vm.hasProp(c, 'data.spec.postgres.extensions') ? c.data.spec.postgres.extensions : [];

                            if (vm.postgresVersion != c.data.spec.postgres.version) {
                                vm.postgresVersion = c.data.spec.postgres.version;
                                vm.getFlavorExtensions()
                            }

                            if(c.data.spec.postgres.hasOwnProperty('ssl')) {
                                vm.ssl = c.data.spec.postgres.ssl

                                if(!vm.ssl.hasOwnProperty('certificateSecretKeySelector')) {
                                    vm.ssl['certificateSecretKeySelector'] = {
                                        name: '',
                                        key: ''
                                    };
                                }
                                if(!vm.ssl.hasOwnProperty('privateKeySecretKeySelector')) {
                                    vm.ssl['privateKeySecretKeySelector'] = {
                                        name: '',
                                        key: ''
                                    }
                                }
                            }

                            // Set Coordinator & Shards spec
                            ['coordinator', 'shards'].forEach( (type) => {
                                vm[type] = c.data.spec[type];

                                // Volume Size
                                let volumeSize = {
                                   size: vm[type].pods.persistentVolume.size.match(/\d+/g)[0],
                                   unit: vm[type].pods.persistentVolume.size.match(/[a-zA-Z]+/g)[0],
                                };
                                vm[type].pods.persistentVolume.size = volumeSize;

                                // Script Sources
                                vm.setScriptsSource(c.data.spec[type], type);

                                // Custom Volumes
                                vm.customVolumesType[type] = [];
                                if(vm.hasProp(c, 'data.spec.' + type + '.pods.customVolumes')) {
                                    vm[type].pods.customVolumes.forEach( (v) => {
                                        if(v.hasOwnProperty('emptyDir')) {
                                            vm.customVolumesType[type].push('emptyDir');
                                        } else if(v.hasOwnProperty('configMap')) {
                                            vm.customVolumesType[type].push('configMap');
                                        } else if(v.hasOwnProperty('secret')) {
                                            vm.customVolumesType[type].push('secret');
                                        }
                                    });
                                }

                                // Replication
                                if(!vm.hasProp(c, 'data.spec.' + type + '.replication')) {
                                    vm[type]['replication'] = {
                                        mode: 'async'
                                    };
                                }

                                // Metadata
                                if(vm.hasProp(c, 'data.spec.' + type + '.metadata.labels.clusterPods')) {
                                    vm[type].metadata.labels.clusterPods = vm.unparseProps(c.data.spec[type].metadata.labels.clusterPods, 'label');
                                }

                                ['allResources', 'clusterPods', 'services', 'primaryService', 'replicasService'].forEach( (annotation) => {
                                    if(vm.hasProp(c, 'data.spec.' + type + '.metadata.annotations.' + annotation)) {
                                        vm[type].metadata.annotations[annotation] = vm.unparseProps(c.data.spec[type].metadata.annotations[annotation]);
                                    }
                                })

                                // Scheduling
                                if(vm.hasProp(c, 'data.spec.' + type + '.pods.scheduling.nodeSelector')) {
                                    vm[type].pods.scheduling.nodeSelector = vm.unparseProps(c.data.spec[type].pods.scheduling.nodeSelector, 'label');
                                }
                            });

                            // Overrides
                            if(vm.hasProp(c, 'data.spec.shards.overrides')) {

                                // Initialize overrideIndex
                                vm.overrideIndex = 0;

                                c.data.spec.shards.overrides.forEach( (override, index) => {
                                    // Volume Size
                                    let volumeSize = {
                                        size: override.pods.persistentVolume.size.match(/\d+/g)[0],
                                        unit: override.pods.persistentVolume.size.match(/[a-zA-Z]+/g)[0],
                                    };
                                    override.pods.persistentVolume.size = volumeSize;

                                    // ManagedSQL    
                                    vm.setScriptsSource(override, 'overrides');

                                    // Custom Volumes
                                    vm.customVolumesType.overrides[index] = [];
                                    if(vm.hasProp(override, 'pods.customVolumes')) {
                                        override.pods.customVolumes.forEach( (v) => {
                                            if(v.hasOwnProperty('emptyDir')) {
                                                vm.customVolumesType.overrides[index].push('emptyDir');
                                            } else if(v.hasOwnProperty('configMap')) {
                                                vm.customVolumesType.overrides[index].push('configMap');
                                            } else if(v.hasOwnProperty('secret')) {
                                                vm.customVolumesType.overrides[index].push('secret');
                                            }
                                        });
                                    }

                                    // Replication
                                    if(!vm.hasProp(override, 'replication')) {
                                        override['replication'] = {
                                            mode: 'async'
                                        };
                                    }

                                    // Metadata
                                    if(vm.hasProp(override, 'metadata.labels.clusterPods')) {
                                        override.metadata.labels.clusterPods = vm.unparseProps(override.metadata.labels.clusterPods, 'label');
                                    }

                                    ['allResources', 'clusterPods', 'services', 'primaryService', 'replicasService'].forEach( (annotation) => {
                                        if(vm.hasProp(override, 'metadata.annotations.' + annotation)) {
                                            override.metadata.annotations[annotation] = vm.unparseProps(override.metadata.annotations[annotation]);
                                        }
                                    })

                                    // Scheduling
                                    if(vm.hasProp(override, 'pods.scheduling.nodeSelector')) {
                                        override.pods.scheduling.nodeSelector = vm.unparseProps(override.pods.scheduling.nodeSelector, 'label');
                                    }
                                });
                            } else {
                                vm.$set(vm.shards, 'overrides', []);
                            }

                            // Check if Monitoring should be enabled
                            vm.checkEnableMonitoring(c.data.spec);

                            vm.editReady = vm.advancedMode = true
                            return false
                        }
                    });
                }
                
                return cluster
            },

            currentStepIndex() {
                return this.formSteps[this.currentSection].indexOf(this.currentStep[this.currentSection])
            },

            nameColission() {

                const vc = this;
                var nameColission = false;
                
                store.state.sgshardedclusters.forEach(function(item, index){
                    if( (item.name == vc.name) && (item.data.metadata.namespace == vc.$route.params.namespace ) ) {
                        nameColission = true;
                        return false
                    }
                })

                return nameColission
            },

        },

        methods: {

            createCluster(preview = false, previous) {
                const vc = this;

                if(!vc.checkRequired()) {
                  return;
                }

                if (!previous) {
                    sgApi
                    .getResourceDetails('sgshardedclusters', this.namespace, this.name)
                    .then(function (response) {
                        vc.createCluster(preview, response.data);
                    })
                    .catch(function (error) {
                        if ( vc.hasProp(error, 'response.status') && (error.response.status != 404) ) {
                          console.log(error.response);
                          vc.notify(error.response.data,'error', 'sgshardedclusters');
                          return;
                        }
                        vc.createCluster(preview, {});
                    });
                    return;
                }

                let requiredAffinity = {
                    coordinator: this.hasProp(this.coordinator, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') ?
                        vc.cleanNodeAffinity(this.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms) : [],
                    shards: this.hasProp(this.shards, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') ?
                        vc.cleanNodeAffinity(this.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms) : [],
                };
                let preferredAffinity = {
                    coordinator: this.hasProp(this.coordinator, 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') ?
                        vc.cleanNodeAffinity(this.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution) : [],
                    shards: this.hasProp(this.shards, 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') ?
                        vc.cleanNodeAffinity(this.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution) : [],
                };
                let userSuppliedSidecars = {
                    coordinator: this.coordinator.hasOwnProperty('pods') ? vc.cleanUpUserSuppliedSidecars($.extend(true,{},this.coordinator.pods)) : {},
                    shards: this.shards.hasOwnProperty('pods') ? vc.cleanUpUserSuppliedSidecars($.extend(true,{},this.shards.pods)) : {},
                };

                var cluster = {
                    "metadata": {
                        ...(this.hasProp(previous, 'metadata') && previous.metadata),
                        "name": this.name,
                        "namespace": this.namespace
                    },
                    "spec": {
                        ...(this.hasProp(previous, 'spec') && previous.spec),
                        "database": this.database,
                        "type": this.shardingType,
                        ...( (this.hasProp(previous, 'spec.configurations') || this.managedBackups ) && ({
                            "configurations": {
                                ...(this.hasProp(previous, 'spec.configurations') && previous.spec.configurations),
                                ...(this.managedBackups && {
                                    "backups": this.backups
                                } || { "backups": null }),
                            }
                        }) ),
                        ...( (this.hasProp(previous, 'spec.distributedLogs') || ( (typeof this.distributedLogs !== 'undefined') && this.distributedLogs.length) ) && {
                            "distributedLogs": {
                                ...(this.hasProp(previous, 'spec.distributedLogs') && previous.spec.distributedLogs),
                                ...(( (typeof this.distributedLogs !== 'undefined') && this.distributedLogs.length ) && { "sgDistributedLogs": this.distributedLogs }),
                                ...(this.retention.length && {
                                    "retention": this.retention
                                })
                            }
                        } || {"distributedLogs": null} ),
                        ...( (this.hasProp(previous, 'spec.replication') || (this.replication.mode !== 'async') ) && {
                            "replication": {
                                "mode": this.replication.mode,
                                ...(['sync', 'strict-sync'].includes(this.replication.mode) && ({
                                    "syncInstances": this.replication.syncInstances
                                }) )
                            }
                        } || {"replication": null} ),
                        ...(this.prometheusAutobind && ( {"prometheusAutobind": this.prometheusAutobind }) ),
                        ...((this.hasProp(previous, 'spec.nonProductionOptions') || !this.enableClusterPodAntiAffinity || (this.flavor == 'babelfish' && this.babelfishFeatureGates)) && ( {
                            "nonProductionOptions": { 
                                ...(this.hasProp(previous, 'spec.nonProductionOptions') && previous.spec.nonProductionOptions),
                                ...(!this.enableClusterPodAntiAffinity && {"disableClusterPodAntiAffinity": !this.enableClusterPodAntiAffinity} || {"disableClusterPodAntiAffinity": null} ),
                                ...((this.flavor == 'babelfish' && this.babelfishFeatureGates) && {"enabledFeatureGates": ['babelfish-flavor'] } || {"enabledFeatureGates": null} )
                                } 
                            }) ),
                        ...( (this.hasProp(previous, 'spec.metadata') || !$.isEmptyObject(this.parseProps(this.annotationsAll)) || !$.isEmptyObject(this.parseProps(this.annotationsPods)) || !$.isEmptyObject(this.parseProps(this.annotationsServices))
                            || !$.isEmptyObject(this.parseProps(this.postgresServicesPrimaryAnnotations)) || !$.isEmptyObject(this.parseProps(this.postgresServicesReplicasAnnotations)) || !$.isEmptyObject(this.parseProps(this.podsMetadata, 'label')) ) && ({
                            "metadata": {
                                ...(this.hasProp(previous, 'spec.metadata') && previous.spec.metadata),
                                ...( (this.hasProp(previous, 'spec.metadata') || !$.isEmptyObject(this.parseProps(this.annotationsAll)) || !$.isEmptyObject(this.parseProps(this.annotationsPods)) || !$.isEmptyObject(this.parseProps(this.annotationsServices))
                                || !$.isEmptyObject(this.parseProps(this.postgresServicesPrimaryAnnotations)) || !$.isEmptyObject(this.parseProps(this.postgresServicesReplicasAnnotations))) && {
                                    "annotations": {
                                        ...(this.hasProp(previous, 'spec.metadata.annotations') && previous.spec.metadata.annotations),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsAll)) && ( {"allResources": this.parseProps(this.annotationsAll) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsPods)) && ( {"clusterPods": this.parseProps(this.annotationsPods) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsServices)) && ( {"services": this.parseProps(this.annotationsServices) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.postgresServicesPrimaryAnnotations)) && ( {"primaryService": this.parseProps(this.postgresServicesPrimaryAnnotations) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.postgresServicesReplicasAnnotations)) && ( {"replicasService": this.parseProps(this.postgresServicesReplicasAnnotations) }) ),
                                    }
                                } || {"annotations": null}),
                                ...( (this.hasProp(previous, 'spec.metadata.labels') || !$.isEmptyObject(this.parseProps(this.podsMetadata, 'label'))) && {
                                    "labels": {
                                        ...(this.hasProp(previous, 'spec.metadata.labels') && previous.spec.metadata.labels),
                                        ...(!$.isEmptyObject(this.parseProps(this.podsMetadata, 'label')) && {
                                            "clusterPods": this.parseProps(this.podsMetadata, 'label')
                                        })
                                    }
                                } || {"labels": null})
                            }
                        }) ),
                        "postgresServices": {
                            ...(this.hasProp(previous, 'spec.postgresServices') && previous.spec.postgresServices),
                            "coordinator": {
                                ...(this.hasProp(previous, 'spec.postgresServices.coordinator') && previous.spec.postgresServices.coordinator),
                                ...((this.hasProp(previous, 'spec.postgresServices.coordinator.primary') || this.hasProp(this.postgresServices, 'coordinator.primary')) && ({
                                    "primary": {
                                        ...(this.hasProp(previous, 'spec.postgresServices.coordinator.primary') && previous.spec.postgresServices.coordinator.primary),
                                        "enabled": this.postgresServices.coordinator.primary.enabled,
                                        "type": this.postgresServices.coordinator.primary.type,
                                        ...( (this.hasProp(this.postgresServices, 'coordinator.primary.loadBalancerIP') && this.postgresServices.coordinator.primary.loadBalancerIP.length) && {
                                            "loadBalancerIP": this.postgresServices.coordinator.primary.loadBalancerIP,
                                        }),
                                    }
                                }) ),
                                ...((this.hasProp(previous, 'spec.postgresServices.coordinator.any') || this.hasProp(this.postgresServices, 'coordinator.any')) && ({
                                    "any": {
                                        ...(this.hasProp(previous, 'spec.postgresServices.coordinator.any') && previous.spec.postgresServices.coordinator.any),
                                        "enabled": this.postgresServices.coordinator.any.enabled,
                                        "type": this.postgresServices.coordinator.any.type,
                                        ...( (this.hasProp(this.postgresServices, 'coordinator.any.loadBalancerIP') && this.postgresServices.coordinator.any.loadBalancerIP.length) && {
                                            "loadBalancerIP": this.postgresServices.coordinator.any.loadBalancerIP,
                                        }),
                                    }
                                }) ),
                                ...( (this.postgresServices.coordinator.hasOwnProperty('customPorts') && !this.isNullObjectArray(this.postgresServices.coordinator.customPorts) ) && {
                                    "customPorts": this.postgresServices.coordinator.customPorts
                                })
                            },
                            "shards": {
                                ...(this.hasProp(previous, 'spec.postgresServices.shards') && previous.spec.postgresServices.shards),
                                ...((this.hasProp(previous, 'spec.postgresServices.shards.primaries') || this.hasProp(this.postgresServices, 'shards.primaries')) && ({
                                    "primaries": {
                                        ...(this.hasProp(previous, 'spec.postgresServices.shards.primaries') && previous.spec.postgresServices.shards.primaries),
                                        "enabled": this.postgresServices.shards.primaries.enabled,
                                        "type": this.postgresServices.shards.primaries.type,
                                        ...( (this.hasProp(this.postgresServices, 'shards.primaries.loadBalancerIP') && this.postgresServices.shards.primaries.loadBalancerIP.length) && {
                                            "loadBalancerIP": this.postgresServices.shards.primaries.loadBalancerIP,
                                        }),
                                    }
                                }) ),
                                ...( (this.postgresServices.shards.hasOwnProperty('customPorts') && !this.isNullObjectArray(this.postgresServices.shards.customPorts) ) && {
                                    "customPorts": this.postgresServices.shards.customPorts
                                })
                            },
                        },
                        "postgres": {
                            ...(this.hasProp(previous, 'spec.postgres') && previous.spec.postgres),
                            "version": this.postgresVersion,
                            ...(this.selectedExtensions.length && {
                                "extensions": this.selectedExtensions
                            } || {"extensions": null} ),
                            "flavor": this.flavor,
                            "ssl": {
                                enabled: this.ssl.enabled,
                                ...( (this.ssl.hasOwnProperty('certificateSecretKeySelector') && this.ssl.certificateSecretKeySelector.name.length && this.ssl.certificateSecretKeySelector.key.length) && {
                                    certificateSecretKeySelector: this.ssl.certificateSecretKeySelector
                                }),
                                ...( (this.ssl.hasOwnProperty('privateKeySecretKeySelector') && this.ssl.privateKeySecretKeySelector.name.length && this.ssl.privateKeySecretKeySelector.key.length) && {
                                    privateKeySecretKeySelector: this.ssl.privateKeySecretKeySelector
                                }),
                            }
                        },
                        "coordinator": {
                            ...(this.hasProp(previous, 'spec.coordinator') && previous.spec.coordinator),
                            "instances": this.coordinator.instances,
                            ...(this.coordinator.sgInstanceProfile.length && {
                                "sgInstanceProfile": this.coordinator.sgInstanceProfile
                            }),
                            ...( (this.hasProp(previous, 'spec.coordinator.configurations') || this.coordinator.configurations.sgPoolingConfig.length || this.coordinator.configurations.sgPostgresConfig.length) && ({
                                "configurations": {
                                    ...(this.hasProp(previous, 'spec.coordinator.configurations') && previous.spec.coordinator.configurations),
                                    ...(this.coordinator.configurations.sgPoolingConfig.length && {
                                        "sgPoolingConfig": this.coordinator.configurations.sgPoolingConfig
                                    }),
                                    ...(this.coordinator.configurations.sgPostgresConfig.length && {
                                        "sgPostgresConfig": this.coordinator.configurations.sgPostgresConfig
                                    }),
                                }
                            }) ),
                            ...( vc.hasScripts(vc.coordinator.managedSql.scripts, vc.scriptSource.coordinator) && {
                                "managedSql": vc.cleanUpScripts($.extend(true,{},vc.coordinator.managedSql))
                            }),
                            "pods": {
                                ...(this.hasProp(previous, 'spec.coordinator.pods') && previous.spec.coordinator.pods),
                                "persistentVolume": {
                                    "size": this.coordinator.pods.persistentVolume.size.size+this.coordinator.pods.persistentVolume.size.unit,
                                    ...( this.coordinator.pods.persistentVolume.hasOwnProperty('storageClass') && {
                                        "storageClass": this.coordinator.pods.persistentVolume.storageClass
                                    })
                                },
                                ...(this.coordinator.pods.disableConnectionPooling && {
                                    "disableConnectionPooling": this.coordinator.pods.disableConnectionPooling
                                }),
                                ...(this.coordinator.pods.disablePostgresUtil && {
                                    "disablePostgresUtil": this.coordinator.pods.disablePostgresUtil
                                }),
                                ...(this.coordinator.pods.hasOwnProperty('disableMetricsExporter') && {
                                    "disableMetricsExporter": this.coordinator.pods.disableMetricsExporter
                                }),
                                ...((
                                    this.coordinator.pods.hasOwnProperty('customVolumes') && !this.isNullObjectArray(this.coordinator.pods.customVolumes) && {
                                        "customVolumes": this.coordinator.pods.customVolumes
                                    } || { "customVolumes": null }
                                )),
                                ...((
                                    userSuppliedSidecars.coordinator.hasOwnProperty('customInitContainers') && userSuppliedSidecars.coordinator.customInitContainers.length && {
                                        "customInitContainers": userSuppliedSidecars.coordinator.customInitContainers
                                    } || { "customInitContainers": null }
                                )),
                                ...((
                                    userSuppliedSidecars.coordinator.hasOwnProperty('customContainers') && userSuppliedSidecars.coordinator.customContainers.length && {
                                        "customContainers": userSuppliedSidecars.coordinator.customContainers
                                    } || { "customContainers": null }
                                )),
                                ...( ( 
                                    this.hasProp(previous, 'spec.coordinator.pods.scheduling') || 
                                    (this.hasProp(this.coordinator, 'pods.scheduling.nodeSelector') && this.hasNodeSelectors(this.coordinator.pods.scheduling.nodeSelector)) || 
                                    (this.hasProp(this.coordinator, 'pods.scheduling.tolerations') && this.hasTolerations(this.coordinator.pods.scheduling.tolerations)) || 
                                    requiredAffinity.coordinator.length || preferredAffinity.coordinator.length 
                                    ) && {
                                    "scheduling": {
                                        ...(this.hasProp(previous, 'spec.coordinator.pods.scheduling') && previous.spec.coordinator.pods.scheduling),
                                        ...( (this.hasProp(this.coordinator, 'pods.scheduling.nodeSelector') && this.hasNodeSelectors(this.coordinator.pods.scheduling.nodeSelector)) && {"nodeSelector": this.parseProps(this.coordinator.pods.scheduling.nodeSelector, 'label')} || {"nodeSelector": null} ),
                                        ...( (this.hasProp(this.coordinator, 'pods.scheduling.tolerations') && this.hasTolerations(this.coordinator.pods.scheduling.tolerations)) && {"tolerations": this.coordinator.pods.scheduling.tolerations} || {"tolerations": null} ),
                                        ...(requiredAffinity.coordinator.length || preferredAffinity.coordinator.length ) && {
                                            "nodeAffinity": {
                                                ...(requiredAffinity.coordinator.length && {
                                                    "requiredDuringSchedulingIgnoredDuringExecution": {
                                                        "nodeSelectorTerms": requiredAffinity.coordinator
                                                    }
                                                }),
                                                ...(preferredAffinity.coordinator.length && {
                                                    "preferredDuringSchedulingIgnoredDuringExecution": preferredAffinity.coordinator
                                                })
                                            }
                                        } || { "nodeAffinity": null }
                                    }
                                } || { "scheduling": null } ),
                            },
                            ...( (this.hasProp(previous, 'spec.coordinator.replication') || ( this.hasProp(this.coordinator, 'replication.mode') && (this.coordinator.replication.mode !== 'async') ) ) && {
                                "replication": {
                                    "mode": this.coordinator.replication.mode,
                                    ...(['sync', 'strict-sync'].includes(this.coordinator.replication.mode) && ({
                                        "syncInstances": this.coordinator.replication.syncInstances
                                    }) )
                                }
                            } || {"replication": null} ),
                            ...( (
                                this.hasProp(previous, 'spec.coordinator.metadata') || 
                                (
                                    (this.hasProp(this.coordinator, 'metadata.annotations.allResources') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.allResources)) ) ||
                                    (this.hasProp(this.coordinator, 'metadata.annotations.clusterPods') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.clusterPods)) ) ||
                                    (this.hasProp(this.coordinator, 'metadata.annotations.services') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.services)) ) ||
                                    (this.hasProp(this.coordinator, 'metadata.annotations.primaryService') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.primaryService)) ) ||
                                    (this.hasProp(this.coordinator, 'metadata.annotations.replicasService') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.replicasService)) )
                                ) ||
                                (
                                    ( this.hasProp(this.coordinator, 'metadata.labels.clusterPods')  && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.labels.clusterPods, 'label')) )
                                ) ) && {
                                "metadata": {
                                    ...(this.hasProp(previous, 'spec.coordinator.metadata') && previous.spec.coordinator.metadata),
                                    ...( (this.hasProp(previous, 'spec.coordinator.metadata.annotations') || this.hasProp(this.coordinator, 'metadata.annotations.clusterPods') || this.hasProp(this.coordinator, 'metadata.annotations.services')
                                    || this.hasProp(this.coordinator, 'metadata.annotations.primaryService') || this.hasProp(this.coordinator, 'metadata.annotations.replicasService') ) && {
                                            "annotations": {
                                                ...(this.hasProp(previous, 'spec.coordinator.metadata.annotations') && previous.spec.coordinator.metadata.annotations),
                                                ...( (this.hasProp(this.coordinator, 'metadata.annotations.allResources') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.allResources)) ) && ( {"allResources": this.parseProps(this.coordinator.metadata.annotations.allResources) }) ),
                                                ...( (this.hasProp(this.coordinator, 'metadata.annotations.clusterPods') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.clusterPods)) ) && ( {"clusterPods": this.parseProps(this.coordinator.metadata.annotations.clusterPods) }) ),
                                                ...( (this.hasProp(this.coordinator, 'metadata.annotations.services') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.services)) ) && ( {"services": this.parseProps(this.coordinator.metadata.annotations.services) }) ),
                                                ...( (this.hasProp(this.coordinator, 'metadata.annotations.primaryService') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.primaryService)) ) && ( {"primaryService": this.parseProps(this.coordinator.metadata.annotations.primaryService) }) ),
                                                ...( (this.hasProp(this.coordinator, 'metadata.annotations.replicasService') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.annotations.replicasService)) ) && ( {"replicasService": this.parseProps(this.coordinator.metadata.annotations.replicasService) }) ),
                                            }
                                        } || {"annotations": null}),
                                        ...( ( this.hasProp(previous, 'spec.coordinator.metadata.labels') || ( this.hasProp(this.coordinator, 'metadata.labels.clusterPods')  && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.labels.clusterPods, 'label')) ) ) && {
                                            "labels": {
                                                ...(this.hasProp(previous, 'spec.coordinator.metadata.labels') && previous.spec.coordinator.metadata.labels),
                                                ...( (this.hasProp(this.coordinator, 'metadata.labels.clusterPods') && !$.isEmptyObject(this.parseProps(this.coordinator.metadata.labels.clusterPods, 'label')) ) && {
                                                    "clusterPods": this.parseProps(this.coordinator.metadata.labels.clusterPods, 'label')
                                                })
                                            }
                                        } || {"labels": null})
                                }
                            } || { "metadata": null })
                        },
                        "shards": {
                            ...(this.hasProp(previous, 'spec.shards') && previous.spec.shards),
                            "clusters": this.shards.clusters,
                            "instancesPerCluster": this.shards.instancesPerCluster,
                            ...(this.shards.sgInstanceProfile.length && {
                                "sgInstanceProfile": this.shards.sgInstanceProfile
                            }),
                            ...( (this.hasProp(previous, 'spec.shards.configurations') || this.shards.configurations.sgPoolingConfig.length || this.shards.configurations.sgPostgresConfig.length) && ({
                                "configurations": {
                                    ...(this.hasProp(previous, 'spec.shards.configurations') && previous.spec.shards.configurations),
                                    ...(this.shards.configurations.sgPoolingConfig.length && {
                                        "sgPoolingConfig": this.shards.configurations.sgPoolingConfig
                                    }),
                                    ...(this.shards.configurations.sgPostgresConfig.length && {
                                        "sgPostgresConfig": this.shards.configurations.sgPostgresConfig
                                    }),
                                }
                            }) ),
                            ...( vc.hasScripts(vc.shards.managedSql.scripts, vc.scriptSource.shards) && {
                                "managedSql": vc.cleanUpScripts($.extend(true,{},vc.shards.managedSql))
                            }),
                            "pods": {
                                ...(this.hasProp(previous, 'spec.shards.pods') && previous.spec.shards.pods),
                                "persistentVolume": {
                                    "size": this.shards.pods.persistentVolume.size.size+this.shards.pods.persistentVolume.size.unit,
                                    ...( (this.shards.pods.persistentVolume.hasOwnProperty('storageClass') && this.shards.pods.persistentVolume.storageClass.length) && {
                                        "storageClass": this.shards.pods.persistentVolume.storageClass
                                    })
                                },
                                ...(this.shards.pods.disableConnectionPooling && {
                                    "disableConnectionPooling": this.shards.pods.disableConnectionPooling
                                }),
                                ...(this.shards.pods.disablePostgresUtil && {
                                    "disablePostgresUtil": this.shards.pods.disablePostgresUtil
                                }),
                                ...(this.shards.pods.hasOwnProperty('disableMetricsExporter') && {
                                    "disableMetricsExporter": this.shards.pods.disableMetricsExporter
                                }),
                                ...((
                                    this.shards.pods.hasOwnProperty('customVolumes') && !this.isNullObjectArray(this.shards.pods.customVolumes) && {
                                        "customVolumes": this.shards.pods.customVolumes
                                    } || { "customVolumes": null }
                                )),
                                ...((
                                    userSuppliedSidecars.shards.hasOwnProperty('customInitContainers') && userSuppliedSidecars.shards.customInitContainers.length && {
                                        "customInitContainers": userSuppliedSidecars.shards.customInitContainers
                                    } || { "customInitContainers": null }
                                )),
                                ...((
                                    userSuppliedSidecars.shards.hasOwnProperty('customContainers') && userSuppliedSidecars.shards.customContainers.length && {
                                        "customContainers": userSuppliedSidecars.shards.customContainers
                                    } || { "customContainers": null }
                                )),
                                ...( ( 
                                    this.hasProp(previous, 'spec.shards.pods.scheduling') || 
                                    (this.hasProp(this.shards, 'pods.scheduling.nodeSelector') && this.hasNodeSelectors(this.shards.pods.scheduling.nodeSelector)) || 
                                    (this.hasProp(this.shards, 'pods.scheduling.tolerations') && this.hasTolerations(this.shards.pods.scheduling.tolerations)) || 
                                    requiredAffinity.shards.length || preferredAffinity.shards.length 
                                    ) && {
                                    "scheduling": {
                                        ...(this.hasProp(previous, 'spec.shards.pods.scheduling') && previous.spec.shards.pods.scheduling),
                                        ...( (this.hasProp(this.shards, 'pods.scheduling.nodeSelector') && this.hasNodeSelectors(this.shards.pods.scheduling.nodeSelector)) && {"nodeSelector": this.parseProps(this.shards.pods.scheduling.nodeSelector, 'label')} || {"nodeSelector": null} ),
                                        ...( (this.hasProp(this.shards, 'pods.scheduling.tolerations') && this.hasTolerations(this.shards.pods.scheduling.tolerations)) && {"tolerations": this.shards.pods.scheduling.tolerations} || {"tolerations": null} ),
                                        ...(requiredAffinity.shards.length || preferredAffinity.shards.length ) && {
                                            "nodeAffinity": {
                                                ...(requiredAffinity.shards.length && {
                                                    "requiredDuringSchedulingIgnoredDuringExecution": {
                                                        "nodeSelectorTerms": requiredAffinity.shards
                                                    }
                                                }),
                                                ...(preferredAffinity.shards.length && {
                                                    "preferredDuringSchedulingIgnoredDuringExecution": preferredAffinity.shards
                                                })
                                            }
                                        } || { "nodeAffinity": null }
                                    }
                                } || { "scheduling": null } ),
                            },
                            ...( (this.hasProp(previous, 'spec.shards.replication') || ( this.hasProp(this.shards, 'replication.mode') && (this.shards.replication.mode !== 'async') ) ) && {
                                "replication": {
                                    "mode": this.shards.replication.mode,
                                    ...(['sync', 'strict-sync'].includes(this.shards.replication.mode) && ({
                                        "syncInstances": this.shards.replication.syncInstances
                                    }) )
                                }
                            } || {"replication": null} ),
                            ...( (
                                this.hasProp(previous, 'spec.shards.metadata') || 
                                (
                                    (this.hasProp(this.shards, 'metadata.annotations.allResources') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.allResources)) ) ||
                                    (this.hasProp(this.shards, 'metadata.annotations.clusterPods') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.clusterPods)) ) ||
                                    (this.hasProp(this.shards, 'metadata.annotations.services') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.services)) ) ||
                                    (this.hasProp(this.shards, 'metadata.annotations.primaryService') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.primaryService)) ) ||
                                    (this.hasProp(this.shards, 'metadata.annotations.replicasService') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.replicasService)) )
                                ) ||
                                (
                                    ( this.hasProp(this.shards, 'metadata.labels.clusterPods')  && !$.isEmptyObject(this.parseProps(this.shards.metadata.labels.clusterPods, 'label')) )
                                ) ) && {
                                "metadata": {
                                    ...(this.hasProp(previous, 'spec.shards.metadata') && previous.spec.shards.metadata),
                                    ...( (this.hasProp(previous, 'spec.shards.metadata.annotations') || this.hasProp(this.shards, 'metadata.annotations.clusterPods') || this.hasProp(this.shards, 'metadata.annotations.services')
                                    || this.hasProp(this.shards, 'metadata.annotations.primaryService') || this.hasProp(this.shards, 'metadata.annotations.replicasService') ) && {
                                            "annotations": {
                                                ...(this.hasProp(previous, 'spec.shards.metadata.annotations') && previous.spec.shards.metadata.annotations),
                                                ...( (this.hasProp(this.shards, 'metadata.annotations.allResources') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.allResources)) ) && ( {"allResources": this.parseProps(this.shards.metadata.annotations.allResources) }) ),
                                                ...( (this.hasProp(this.shards, 'metadata.annotations.clusterPods') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.clusterPods)) ) && ( {"clusterPods": this.parseProps(this.shards.metadata.annotations.clusterPods) }) ),
                                                ...( (this.hasProp(this.shards, 'metadata.annotations.services') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.services)) ) && ( {"services": this.parseProps(this.shards.metadata.annotations.services) }) ),
                                                ...( (this.hasProp(this.shards, 'metadata.annotations.primaryService') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.primaryService)) ) && ( {"primaryService": this.parseProps(this.shards.metadata.annotations.primaryService) }) ),
                                                ...( (this.hasProp(this.shards, 'metadata.annotations.replicasService') && !$.isEmptyObject(this.parseProps(this.shards.metadata.annotations.replicasService)) ) && ( {"replicasService": this.parseProps(this.shards.metadata.annotations.replicasService) }) ),
                                            }
                                        } || {"annotations": null}),
                                        ...( ( this.hasProp(previous, 'spec.shards.metadata.labels') || ( this.hasProp(this.shards, 'metadata.labels.clusterPods')  && !$.isEmptyObject(this.parseProps(this.shards.metadata.labels.clusterPods, 'label')) ) ) && {
                                            "labels": {
                                                ...(this.hasProp(previous, 'spec.shards.metadata.labels') && previous.spec.shards.metadata.labels),
                                                ...( (this.hasProp(this.shards, 'metadata.labels.clusterPods') && !$.isEmptyObject(this.parseProps(this.shards.metadata.labels.clusterPods, 'label')) ) && {
                                                    "clusterPods": this.parseProps(this.shards.metadata.labels.clusterPods, 'label')
                                                })
                                            }
                                        } || {"labels": null})
                                }
                            } || { "metadata": null }),
                            ...( (this.shards.hasOwnProperty('overrides') && this.shards.overrides.length) && {
                                "overrides": this.cleanupOverrides(structuredClone(this.shards.overrides))
                            } || { "overrides": null }),
                        },

                    }
                }

                if(preview) {

                    vc.previewCRD = {};
                    vc.previewCRD['data'] = cluster;
                    vc.showSummary = true;

                } else {

                    if(this.editMode) {
                        sgApi
                        .update('sgshardedclusters', cluster)
                        .then(function (response) {
                            vc.notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> updated successfully', 'message', 'sgshardedclusters');

                            vc.fetchAPI('sgshardedclusters');
                            router.push('/' + cluster.metadata.namespace + '/sgshardedcluster/' + cluster.metadata.name);
                            
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error', 'sgshardedclusters');
                        });
                    } else {
                        sgApi
                        .create('sgshardedclusters', cluster)
                        .then(function (response) {
                            vc.notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> created successfully', 'message', 'sgshardedclusters');

                            vc.fetchAPI('sgshardedclusters');
                            router.push('/' + cluster.metadata.namespace + '/sgshardedclusters');
                            
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error','sgshardedclusters');
                        });
                    }
                    
                }

            }, 

            updateCronSchedule(index) {
                if (this.cronSchedule[index].ref
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.min
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.hour
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.dom
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.month
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.dow) {
                  return;
                }
                this.backups[index].cronSchedule = this.tzCrontab(
                    this.cronSchedule[index].min
                        + ' ' + this.cronSchedule[index].hour
                        + ' ' + this.cronSchedule[index].dom
                        + ' ' + this.cronSchedule[index].month
                        + ' ' + this.cronSchedule[index].dow, false);
            },

            getScriptFile( baseIndex, index ){
                this.currentScriptIndex[this.currentSection] = { base: baseIndex, entry: index };
                $('input#scriptFile-' + baseIndex + '-' + index).click();
            },

            uploadScript: function(e) {
                var files = e.target.files || e.dataTransfer.files;
                var vm = this;

                if (!files.length){
                    console.log("File not loaded")
                    return;
                } else {
                    var reader = new FileReader();
                    
                    reader.onload = function(e) {
                        vm[vm.currentSection].managedSql.scripts[vm.currentScriptIndex[vm.currentSection].base].scriptSpec.scripts[vm.currentScriptIndex[vm.currentSection].entry].script = e.target.result;
                    };
                    reader.readAsText(files[0]);
                }

            },

            setScriptsSource(el, type) {
                const vm = this;

                if(el.hasOwnProperty('managedSql')) {
                    vm.scriptSource[type] = [];
                    el.managedSql.scripts.forEach( (baseScript, baseIndex) => {
                        vm.scriptSource[type].push({ base: baseScript.sgScript, entries: [] });

                        if(vm.hasProp(baseScript, 'scriptSpec.scripts')) {
                            baseScript.scriptSpec.scripts.forEach(function(script, index){
                                if(script.hasOwnProperty('script')) {
                                    vm.scriptSource[type][baseIndex].entries.push('raw');
                                } else if(script.scriptFrom.hasOwnProperty('secretKeyRef')) {
                                    vm.scriptSource[type][baseIndex].entries.push('secretKeyRef');
                                } else if(script.scriptFrom.hasOwnProperty('configMapScript')) {
                                    vm.scriptSource[type][baseIndex].entries.push('configMapKeyRef');
                                }
                            })
                        }
                    })
                } else {
                    let scriptParent = ( (type === 'overrides') ? vm.shards.overrides[vm.overrideIndex] : vm[type] );
                    
                    scriptParent['managedSql'] = {
                        continueOnSGScriptError: false,
                        scripts: []
                    };

                    if(type === 'overrides') {
                        vm.scriptSource.overrides[vm.overrideIndex] = [];
                    } else {
                        vm.scriptSource[type] = [];
                    }
                }
            },

            checkEnableMonitoring(el = this) {
                this.enableMonitoring = (
                    (
                        el.hasOwnProperty('prometheusAutobind') && el.prometheusAutobind) && (
                        ( 
                            !this.hasProp(this.coordinator, 'pods.disableMetricsExporter') || 
                                ( this.hasProp(this.coordinator, 'pods.disableMetricsExporter') && !this.coordinator.pods.disableMetricsExporter ) 
                        ) && ( 
                            !this.hasProp(this.shards, 'pods.disableMetricsExporter') || 
                                ( this.hasProp(this.shards, 'pods.disableMetricsExporter') && !this.shards.pods.disableMetricsExporter ) 
                        )
                    )
                );
            },

            toggleMonitoring() {
                this.prometheusAutobind = this.enableMonitoring;
                
                if(this.hasProp(this.coordinator, 'pods.disableMetricsExporter')) {
                    this.coordinator.pods.disableMetricsExporter = !this.enableMonitoring;
                }

                if(this.hasProp(this.shards, 'pods.disableMetricsExporter')) {
                    this.shards.pods.disableMetricsExporter = !this.enableMonitoring;
                }
            },

            pushOverride() {

                this.shards.overrides.push({
                    index: null,
                    instancesPerCluster: 1,
                    sgInstanceProfile: '',
                    replication: {
                        mode: 'async'
                    },
                    configurations: {
                        sgPostgresConfig: '',
                        sgPoolingConfig: '',
                    },
                    managedSql: {
                        continueOnSGScriptError: false,
                        scripts: [ {} ]
                    },
                    metadata: {
                        labels: {
                            clusterPods: [ { label: '', value: ''} ],
                        },
                        annotations: {
                            allResources: [ { annotation: '', value: ''} ],
                            clusterPods: [ { annotation: '', value: ''} ],
                            primaryService: [ { annotation: '', value: ''} ],
                            replicasService: [ { annotation: '', value: ''} ],
                            services: [ { annotation: '', value: ''} ],
                        }
                    },
                    pods: {
                        disableConnectionPooling: false,
                        disablePostgresUtil: false,
                        disableMetricsExporter: false,
                        persistentVolume: {
                            size: {
                                size: 1,
                                unit: 'Gi'
                            },
                            storageClass: ''
                        },
                        customVolumes: [{
                            name: null,
                        }],
                        customInitContainers: [{
                            name: null,
                            image: null,
                            imagePullPolicy: null,
                            args: [null],
                            command: [null],
                            workingDir: null,
                            env: [ { name: null, value: null } ],
                            ports: [{
                                containerPort: null,
                                hostIP: null,
                                hostPort: null,
                                name: null,
                                protocol: null
                            }],
                            volumeMounts: [{
                                mountPath: null,
                                mountPropagation: null,
                                name: null,
                                readOnly: false,
                                subPath: null,
                                subPathExpr: null,
                            }]
                        }],
                        customContainers: [{
                            name: null,
                            image: null,
                            imagePullPolicy: null,
                            args: [null],
                            command: [null],
                            workingDir: null,
                            env: [ { name: null, value: null } ],
                            ports: [{
                                containerPort: null,
                                hostIP: null,
                                hostPort: null,
                                name: null,
                                protocol: null
                            }],
                            volumeMounts: [{
                                mountPath: null,
                                mountPropagation: null,
                                name: null,
                                readOnly: false,
                                subPath: null,
                                subPathExpr: null,
                            }]
                        }],
                        scheduling: {
                            nodeSelector: [ { label: '', value: ''} ],
                            tolerations: [ { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null } ],
                            nodeAffinity: {
                                requiredDuringSchedulingIgnoredDuringExecution: {
                                    nodeSelectorTerms: [
                                        {   
                                            matchExpressions: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ],
                                            matchFields: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ]
                                        }
                                    ],
                                },
                                preferredDuringSchedulingIgnoredDuringExecution: [
                                    {
                                        preference: {
                                            matchExpressions: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ],
                                            matchFields: [
                                                { key: '', operator: '', values: [ '' ] }
                                            ]
                                        },
                                        weight:  1
                                    }
                                ],
                            },
                        }
                    },
                });

                this.scriptSource.overrides.push([{ base: '', entries: ['raw'] }]);
                this.currentScriptIndex.overrides.push({ base: 0, entry: 0 });
                this.customVolumesType.overrides.push([null]);

                this.overrideIndex = this.shards.overrides.length - 1;
                this.currentSection = 'overrides';                
            },

            cleanupOverrides(overrides) {
                const vc = this;

                overrides.forEach( (override, overrideIndex) => {

                    override.sgInstanceProfile = override.sgInstanceProfile.length ? override.sgInstanceProfile : null;
                    
                    if(
                        override.configurations.hasOwnProperty('sgPoolingConfig') && override.configurations.sgPoolingConfig.length || 
                        override.configurations.hasOwnProperty('sgPostgresConfig') && override.configurations.sgPostgresConfig.length
                    ) {
                        override.configurations['sgPoolingConfig'] = override.configurations.hasOwnProperty('sgPoolingConfig') && override.configurations.sgPoolingConfig.length ? override.configurations.sgPoolingConfig : null;
                        override.configurations['sgPostgresConfig'] = override.configurations.hasOwnProperty('sgPostgresConfig') && override.configurations.sgPostgresConfig.length ? override.configurations.sgPostgresConfig : null;
                    } else {
                        override.configurations = null;
                    }

                    if(vc.hasProp(override, 'managedSql.scripts')) {
                        override.managedSql = vc.hasScripts(override.managedSql.scripts, vc.scriptSource.overrides[overrideIndex]) ? vc.cleanUpScripts($.extend(true,{},override.managedSql)) : null;
                    }
                    
                    override.pods.persistentVolume = {
                        "size": override.pods.persistentVolume.size.size + override.pods.persistentVolume.size.unit,
                        ...( (override.pods.persistentVolume.hasOwnProperty('storageClass') && override.pods.persistentVolume.storageClass.length) && {
                            "storageClass": override.pods.persistentVolume.storageClass
                        })
                    };
                    override.pods.disableConnectionPooling = override.pods.disableConnectionPooling ? true : null;
                    override.pods.disablePostgresUtil = override.pods.disablePostgresUtil ? true : null;
                    override.pods.disableMetricsExporter = override.pods.disableMetricsExporter ? true : null;
                    

                    let userSuppliedSidecars = vc.cleanUpUserSuppliedSidecars($.extend(true, {}, override.pods));

                    override.pods.customVolumes = userSuppliedSidecars.hasOwnProperty('customVolumes')
                        ? userSuppliedSidecars.customVolumes
                        : null;

                    override.pods.customInitContainers = userSuppliedSidecars.hasOwnProperty('customInitContainers') && userSuppliedSidecars.customInitContainers.length
                        ? userSuppliedSidecars.customInitContainers
                        : null;

                    override.pods.customContainers = userSuppliedSidecars.hasOwnProperty('customContainers') && userSuppliedSidecars.customContainers.length
                        ? userSuppliedSidecars.customContainers
                        : null;

                    let requiredAffinity = this.hasProp(override, 'pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') 
                        ? vc.cleanNodeAffinity(override.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms)
                        : [];

                    let preferredAffinity = this.hasProp(override, 'pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')
                        ? vc.cleanNodeAffinity(override.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution)
                        : [];

                    override.pods.scheduling = ( 
                            (this.hasProp(override, 'pods.scheduling.nodeSelector') && this.hasNodeSelectors(override.pods.scheduling.nodeSelector)) || 
                            (this.hasProp(override, 'pods.scheduling.tolerations') && this.hasTolerations(override.pods.scheduling.tolerations)) || 
                            requiredAffinity.length || preferredAffinity.length 
                        )
                            ? {
                                ...( (this.hasProp(override, 'pods.scheduling.nodeSelector') && this.hasNodeSelectors(override.pods.scheduling.nodeSelector)) && {
                                    "nodeSelector": this.parseProps(override.pods.scheduling.nodeSelector, 'label')
                                } || {"nodeSelector": null} ),
                                ...( (this.hasProp(override, 'pods.scheduling.tolerations') && this.hasTolerations(override.pods.scheduling.tolerations)) && {
                                    "tolerations": override.pods.scheduling.tolerations
                                } || {"tolerations": null} ),
                                ...(requiredAffinity.length || preferredAffinity.length ) && {
                                    "nodeAffinity": {
                                        ...(requiredAffinity.length && {
                                            "requiredDuringSchedulingIgnoredDuringExecution": {
                                                "nodeSelectorTerms": requiredAffinity
                                            }
                                        }),
                                        ...(preferredAffinity.length && {
                                            "preferredDuringSchedulingIgnoredDuringExecution": preferredAffinity
                                        })
                                    }
                                } || { "nodeAffinity": null }
                            }
                            : null;
                    
                    override.replication = (
                            this.hasProp(override, 'replication.mode') &&
                            (override.replication.mode !== 'async')
                        )
                            ? {
                                "mode": override.replication.mode,
                                ...(['sync', 'strict-sync'].includes(override.replication.mode) && ({
                                    "syncInstances": override.replication.syncInstances
                                }) )
                            }
                            : null;

                    override.metadata = (
                            ( 
                                this.hasProp(override, 'metadata.labels.clusterPods') && 
                                !$.isEmptyObject(this.parseProps(override.metadata.labels.clusterPods, 'label'))
                            ) || 
                            (
                                (this.hasProp(override, 'metadata.annotations.allResources') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.allResources)) ) ||
                                (this.hasProp(override, 'metadata.annotations.clusterPods') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.clusterPods)) ) ||
                                (this.hasProp(override, 'metadata.annotations.services') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.services)) ) ||
                                (this.hasProp(override, 'metadata.annotations.primaryService') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.primaryService)) ) ||
                                (this.hasProp(override, 'metadata.annotations.replicasService') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.replicasService)) )
                            )
                        )
                            ? {
                                ...( 
                                    this.hasProp(override, 'metadata.annotations.allResources') ||
                                    this.hasProp(override, 'metadata.annotations.clusterPods') || 
                                    this.hasProp(override, 'metadata.annotations.services') ||
                                    this.hasProp(override, 'metadata.annotations.primaryService') ||
                                    this.hasProp(override, 'metadata.annotations.replicasService') )
                                        ? {
                                            "annotations": {
                                                ...( (this.hasProp(override, 'metadata.annotations.allResources') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.allResources)) ) && ( {"allResources": this.parseProps(override.metadata.annotations.allResources) }) ),
                                                ...( (this.hasProp(override, 'metadata.annotations.clusterPods') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.clusterPods)) ) && ( {"clusterPods": this.parseProps(override.metadata.annotations.clusterPods) }) ),
                                                ...( (this.hasProp(override, 'metadata.annotations.services') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.services)) ) && ( {"services": this.parseProps(override.metadata.annotations.services) }) ),
                                                ...( (this.hasProp(override, 'metadata.annotations.primaryService') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.primaryService)) ) && ( {"primaryService": this.parseProps(override.metadata.annotations.primaryService) }) ),
                                                ...( (this.hasProp(override, 'metadata.annotations.replicasService') && !$.isEmptyObject(this.parseProps(override.metadata.annotations.replicasService)) ) && ( {"replicasService": this.parseProps(override.metadata.annotations.replicasService) }) ),
                                            }
                                        }
                                        : {
                                            "annotations": null
                                        },
                                    ...( 
                                        this.hasProp(override, 'metadata.labels.clusterPods') &&
                                        !$.isEmptyObject(this.parseProps(override.metadata.labels.clusterPods, 'label')) )
                                        ? {
                                            "labels": {
                                                ...( (this.hasProp(override, 'metadata.labels.clusterPods') && !$.isEmptyObject(this.parseProps(override.metadata.labels.clusterPods, 'label')) ) && {
                                                    "clusterPods": this.parseProps(override.metadata.labels.clusterPods, 'label')
                                                })
                                            }
                                        }
                                        : {
                                            "labels": null
                                        }
                            }
                            : null;
                });

                return overrides;
            }
        },
    }
</script>

<style scoped>
    .scriptFieldset:first-child {
        border-top: 0;
        margin-top: 0;
        padding-top: 0;
    }

    input[type="checkbox"].plain:checked {
        border-color: var(--blue);
        background: var(--blue);
    }

    input[type="checkbox"].plain {
        width: 14px;
        height: 14px;
        border-radius: 2px;
        border: 1px solid var(--borderColor);
        padding: 0;
        display: inline-block;
        cursor: pointer;
        position: relative;
        top: 0;
        background: var(--bgColor);
    }

    input[type="checkbox"].plain:checked:after {
        border: 2px solid #fff;
        width: 3px;
        height: 7px;
        content: " ";
        border-left: 0;
        border-top: 0;
        display: block;
        transform: rotate(45deg);
        position: relative;
        top: 0px;
        left: 4px;
    }

    input[type="radio"]:checked {
        background: var(--blue);
    }

    #keyword {
        width: 100%;
        max-width: 100%;
        height: 38px;
        font-size: 100%;
    }

    .searchBar {
        position: relative;
        display: block;
        width: 70%;
        float: left;
    }
    

    .searchBar .clear {
        position: absolute;
        top: 15px;
        right: 10px;
        border: 0;
        padding: 11px 0;
        z-index: 1;
    }

    .searchBar .clear:hover {
        background: transparent;
    }

    .extLicense {
        width: 25%;
        float: right;
    }

    .notCompatible svg {
        fill: red;
        width: 13px;
        position: relative;
        top: 1px;
    }

    .colorRed svg path {
        fill: red;
    }

    ul.extensionsList {
        list-style: none;
        max-height: 40vh;
        overflow-y: auto;
        margin-bottom: 20px;
        padding-right: 10px;
    }

    .extension > label {
        cursor: pointer;
        width: 68px;
        display: inline-block;
    }

    .extInfo {
        cursor: pointer;
        width: calc(100% - 68px);
        display: inline-block;
    }

    .extName {
        font-weight: bold;
    }

    .extension > label input {
        margin: 0 40px 0 14px;
    }

    span.notCompatible {
        margin-left: 5px;
    }

    label[disabled], input[disabled] {
        cursor: not-allowed !important;
    }

    button.toggleExt {
        top: 0;
        position: absolute;
        right: 0;
        width: 35px;
        height: 35px;
        color: transparent;
    }

    button.toggleExt:before {
        content: " ";
        top: 10px;
        position: absolute;
        right: 12px;
        width: 8px;
        height: 8px;
        border: 2px solid var(--textColor);
        border-radius: 0;
        transform: rotate(45deg);
        border-top: 0;
        border-left: 0;
        opacity: .4;
    }

    .extension.show button.toggleExt:before {
        transform: rotate(-135deg);
        top: 14px;
    }
    
    li.extension {
        padding: 3px 0;
        position: relative;
        width: 100%;
        border: 1px solid transparent;
    }

    li.extension:nth-child(even), li.extension.notFound {
        background: var(--activeBg);
        border: 1px solid var(--activeBg);
    }

    .darkmode li.extension:nth-child(even) .header, .darkmode .form .extension select {
        border-color: #555;
    }

    .extDetails {
        padding: 20px 13px 10px;
    }

    .extDetails .description {
        line-height: 1.5;
    }

    .extHead .install {
        margin-right: 30px;
    }

    .extHead {
        font-weight: bold;
        margin: 10px 0;
        display: inline-block;
    }

    li.extension.notFound {
        display: none;
        padding: 12px 70px;
    }

    li.extension.notFound:first-child:last-child {
        display: block;
    }

    li.extension.show .extInfo:after {
        height: 1px;
        width: calc(100% + 68px);
        left: -68px;
        position: relative;
        content: " ";
        margin-top: 10px;
        display: block;
        background: var(--borderColor);
    }

    li.extension:nth-child(even).show > label:after  {
        background: var(--textColor);
        opacity: .2;
    }

    li.extension.show {
        border-color: var(--borderColor);
        margin-bottom: 10px;
    }

    .darkmode li.extension:nth-child(even).show {
        border-color: #555;
    }

    .colorRed {
        color: red;
    }

    .extDetails * + .header {
        margin-top: 25px;
    }

    .extension .tags {
        margin-bottom: 5px;
    }

    .extTag {
        display: inline-block;
        margin-right: 10px;
        border: 1px solid;
        border-radius: 10px;
        padding: 3px 10px;
        font-size: 85%;
        font-weight: bold;
    }

    .extDetails .notCompatible {
        display: block;
        border: 1px solid red;
        border-radius: 3px;
        padding: 10px;
        background: rgb(255 0 0 / 5%);
    }

    .extDetails .notCompatible strong {
        display: block;
    }

    .darkmode .extension > label input {
        background: #fbfbfb;
    }

    .extLinks li {
        margin-bottom: 10px;
    }

    select.extVersion {
        margin-bottom: 0;
        margin-top: 2px;
        padding: 7px;
        height: auto;
        background-position-x: 90%;
    }

    .versionContainer {
        min-height: 95px;
    }

    ul#postgresVersion.active {
        position: absolute;
        width: 100%;
        z-index: 10;
        max-height: 30vh;
        overflow: auto;
    }

    ul#postgresVersion + .helpTooltip {
        transform: translate(20px, -53px);
    }

    ul#postgresVersion.active + .helpTooltip {
        transform: translate(20px, 10px);
    }

    ul.select li.selected {
        position: sticky;
        top: 0;
    }

    .affinityValues a.addRow {
        transform: translateY(-75px);
        float: right;
    }

    .extHead span.name, .extensionsList span.name {
        width: 180px;
        display: inline-block;
    }

    .extHead span.version, .extensionsList span.version {
        width: 75px;
        display: inline-block;
    }

    .extensionsList span.version {
        font-weight: normal;
    }

    .extHead span.description, .extensionsList span.description {
        display: inline-block;
        margin-left: 15px;
        width: calc(100% - 350px);
    }

    .extensionsList span.description {
        font-weight: normal;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        transform: translateY(3px);
        width: calc(100% - 320px);
    }

    .extension a.newTab {
        width: 11px;
        height: 11px;
        display: inline-block;
        transform: translate(4px, 1px);
        position: absolute;
        right: 20px;
    }

    .contentTooltip #clusterDetails {
        margin-right: 10px;
    }

    .warning.babelfish {
        top: -5px;
        position: relative;
        margin-bottom: 25px;
    }

    input.affinityWeight + span {
        left: -20px;
        top: -15px;
    }

    input.affinityWeight {
        width: calc(100% - 25px);
    }

    fieldset.noRepeater {
        padding: 0 0 10px;
        border: 0;
        margin-bottom: -10px;
    }

    #podsMetadata fieldset, #podsScheduling .repeater > fieldset {
        padding-bottom: 10px;
    }

    fieldset.noMargin, .scriptFieldset fieldset fieldset:last-of-type {
        margin-bottom: 0;
        border-bottom-left-radius: 0;
        border-bottom-right-radius: 0;
    }

    .scriptFieldset fieldset fieldset .row {
        margin-bottom: 20px;
    }

    .scheduling .fieldsetFooter {
        margin-bottom: 20px;
    }

    .searchBar + .helpTooltip {
        top: -15px;
    }
    
    .warning.babelfish label, .warning.babelfish p, .warning.babelfish .col {
        margin-bottom: 0;
    }

    .warning.babelfish:before {
        left: 34%;
    }

    .warning.babelfish .helpTooltip {
        transform: translate(20px, -30px);
    }

    body:not(.darkmode) label[for="babelfish"] svg path[fill="#FFF"] {
        fill: #3452a8 !important;
    }

    .row-50.noMargin {
        margin-bottom: -20px;
    }

    .noMarginTop {
        margin-top: -20px;
    }

    .cron {
        gap: 15px;
    }

    .cron > * {
        flex-grow: 1;
    }

    form#createShardedCluster {
        width: 1120px;
        max-width: 100%;
    }

    .annotation.repeater .row:last-child input {
        margin-bottom: -10px;
    }

</style>
