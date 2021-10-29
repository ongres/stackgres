<template>
    <form id="create-cluster" class="noSubmit" v-if="loggedIn && isReady && !notFound" @submit.prevent="createCluster()">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(cluster).length > 0"></template>
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
                    <router-link :to="'/' + $route.params.namespace + '/sgclusters'" title="SGClusters">SGClusters</router-link>
                </li>
                <li v-if="editMode">
                    <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name" title="Cluster Details">{{ $route.params.name }}</router-link>
                </li>
                <li class="action">
                    {{ $route.name == 'EditCluster' ? 'Edit' : 'Create' }}
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgcluster/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
            </div>
        </header>
        <div class="form">
            <ul class="steps">
                <li v-for="(step, index) in formSteps" @click="currentStep = step" :class="[( (currentStep == step) && 'active'), ( (index < 3) && 'basic' )]" v-if="( ((index < 3) && !advancedMode) || advancedMode)">
                    {{ step }}
                </li>
                <a v-if="!advancedMode" @click="advancedMode = true">
                    Show Advanced Options
                </a>
            </ul>

            <div class="clearfix"></div>

            <fieldset class="step" :class="(currentStep == 'cluster') && 'active'">
                <div class="header">
                    <h2>Cluster</h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <h3>Basic</h3>

                        <div class="col">
                            <label for="metadata.namespace">Namespace <span class="req">*</span></label>
                            <input disabled data-field="metadata.namespace" :value="$route.params.namespace">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.metadata.namespace')"></span>
                        </div>
                        <div class="col">
                            <label for="metadata.name">Name <span class="req">*</span></label>
                            <input v-model="name" :disabled="editMode" required data-field="metadata.name" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.metadata.name')"></span>
                        </div>

                        <span class="warning" v-if="nameColission && !editMode">
                            There's already a <strong>SGCluster</strong> with the same name on this namespace. Please specify a different name or create the cluster on another namespace
                        </span>
                    </div>

                    <div class="row-50">
                        <h3>Instances</h3>

                        <div class="col">
                            <label for="spec.instances">Number of Instances <span class="req">*</span></label>
                            <select v-model="instances" required data-field="spec.instances">
                                <option disabled value="">Instances</option>
                                <option>1</option>
                                <option>2</option>
                                <option>3</option>
                                <option>4</option>
                                <option>5</option>
                                <option>6</option>
                                <option>7</option>
                                <option>8</option>
                                <option>9</option>
                                <option>10</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.instances')"></span>
                        </div>
                        <div class="col">
                            <label for="spec.sgInstanceProfile">Instance Profile</label>  
                            <select v-model="resourceProfile" class="resourceProfile" data-field="spec.sgInstanceProfile">
                                <option selected value="">Default (Cores: 1, RAM: 2GiB)</option>
                                <option v-for="prof in profiles" v-if="prof.data.metadata.namespace == namespace" :value="prof.name">{{ prof.name }} (Cores: {{ prof.data.spec.cpu }}, RAM: {{ prof.data.spec.memory }}B)</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.sgInstanceProfile')"></span>
                        </div>
                    </div>

                    <div class="row-50">
                        <h3>Pods Storage</h3>

                        <div class="col">
                            <div class="unit-select">
                                <label for="spec.pods.persistentVolume.size">Volume Size <span class="req">*</span></label>  
                                <input v-model="volumeSize" class="size" required data-field="spec.pods.persistentVolume.size" type="number">
                                <select v-model="volumeUnit" class="unit" required data-field="spec.pods.persistentVolume.size" >
                                    <option disabled value="">Select Unit</option>
                                    <option value="Mi">MiB</option>
                                    <option value="Gi">GiB</option>
                                    <option value="Ti">TiB</option>   
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.size')"></span>
                            </div>
                        </div>

                        <div class="col">
                            <label for="spec.pods.persistentVolume.storageClass">Storage Class</label>
                            <select v-model="storageClass" data-field="spec.pods.persistentVolume.storageClass" :disabled="!storageClasses.length">
                                <option value=""> {{ storageClasses.length ? 'Select Storage Class' : 'No storage classes available' }}</option>
                                <option v-for="sClass in storageClasses">{{ sClass }}</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.storageClass')"></span>
                        </div>
                    </div>

                    <div class="row-50">
                        <h3>Non Production Settings</h3>
                        <div class="col">
                            <label for="spec.nonProductionOptions.disableClusterPodAntiAffinity" class="switch yes-no">disableClusterPodAntiAffinity <input type="checkbox" id="disableClusterPodAntiAffinity" v-model="disableClusterPodAntiAffinity" data-switch="NO"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.nonProductionOptions.disableClusterPodAntiAffinity')"></span>
                        </div>
                    </div>

                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'postgres') && 'active'">
                <div class="header">
                    <h2>Postgres</h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <div class="col">                    
                            <div class="versionContainer">
                                <label for="spec.postgres.version">Postgres Version <span class="req">*</span></label>
                                <ul class="select" id="postgresVersion" data-field="spec.postgres.version">
                                    <li class="selected">
                                        {{ (postgresVersion == 'latest') ? 'Latest' : 'Postgres '+postgresVersion }}
                                    </li>
                                    <li>
                                        <a @click="setVersion('latest')" data-val="latest" class="active">Latest</a>
                                    </li>

                                    <li v-for="version in Object.keys(postgresVersionsList).reverse()">
                                        <strong>Postgres {{ version }}</strong>
                                        <ul>
                                            <li>
                                                <a @click="setVersion(version)" :data-val="version">Postgres {{ version }} (Latest)</a>
                                            </li>
                                            <li v-for="minorVersion in postgresVersionsList[version]">
                                                <a @click="setVersion(minorVersion)" :data-val="minorVersion">Postgres {{ minorVersion }}</a>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.version')"></span>

                                <div class="warning" v-if="!pgConfigExists">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="18" viewBox="0 0 20 18"><g transform="translate(0 -183)"><path d="M18.994,201H1.006a1,1,0,0,1-.871-.516,1.052,1.052,0,0,1,0-1.031l8.993-15.974a1.033,1.033,0,0,1,1.744,0l8.993,15.974a1.052,1.052,0,0,1,0,1.031A1,1,0,0,1,18.994,201ZM2.75,198.937h14.5L10,186.059Z" fill="#00adb5"/><rect width="2" height="5.378" rx="0.947" transform="translate(9 189.059)" fill="#00adb5"/><rect width="2" height="2" rx="1" transform="translate(9 195.437)" fill="#00adb5"/></g></svg>
                                    <p>Please notice that <strong>there are no Postgres Configurations available</strong> for this Postgres Version in this Namespace. A <strong>default Postgres Configuration will be created and applied to the cluster</strong> if you continue.</p>
                                </div>

                                <input v-model="postgresVersion" @change="checkPgConfigVersion" required class="hide">
                            </div>
                        </div>
                        <div class="col">
                            <label for="spec.configurations.sgPostgresConfig">Postgres Configuration</label>
                            <select v-model="pgConfig" class="pgConfig" data-field="spec.configurations.sgPostgresConfig">
                                <option value="" selected>Default</option>
                                <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.postgresVersion == shortpostgresVersion) )">{{ conf.name }}</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPostgresConfig')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'extensions') && 'active'">
                <div class="header">
                    <h2>Postgres Extensions</h2>
                </div>
                
                <div class="fields">
                    <div class="toolbar">
                        <div class="searchBar">
                            <input id="keyword" v-model="searchExtension" class="search" placeholder="Search Extension..." autocomplete="off" data-field="spec.postgres.extensions">
                            <a @click="clearExtFilters()" class="btn clear border keyword" v-if="searchExtension.length">CLEAR</a>
                        </div>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.extensions')"></span>
                    </div>
                    
                    <div class="extHead">
                        <span class="install">Install</span>
                        <span class="name">Name</span>
                        <span class="version">Version</span>
                        <span class="description">Description</span>
                    </div>
                    <ul class="extensionsList">
                        <li class="extension notFound">No extensions match your search terms...</li>
                        <li v-for="(ext, index) in extensionsList" v-if="(!searchExtension.length || (ext.name+ext.description+ext.tags.toString()).includes(searchExtension)) && ext.versions.length" class="extension" :class="( (viewExtension == index) && !searchExtension.length) ? 'show' : ''">
                            <label class="hoverTooltip">
                                <input type="checkbox" class="plain" @change="setExtension(index)" :checked="(extIsSet(ext.name) !== -1)" :disabled="!ext.versions.length"/>
                                <span class="name">
                                    {{ ext.name }}
                                    <a v-if="ext.hasOwnProperty('url') && ext.url" :href="ext.url" class="newTab" target="_blank"></a>
                                </span>
                                <span class="version">
                                    <select v-model="ext.selectedVersion" class="extVersion" @change="updateExtVersion(ext.name, ext.selectedVersion)">
                                        <option v-if="!ext.versions.length" selected>Not available for this postgres version</option>
                                        <option v-for="v in ext.versions">{{ v }}</option>
                                    </select>
                                </span>
                                <span class="description firstLetter">
                                    {{ ext.description }}
                                </span>
                            </label>
                            <button class="textBtn anchor toggleExt" @click.stop.prevent="viewExt(index)">-</button>

                            <div v-if="(viewExtension == index)" class="extDetails">
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
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'initialization') && 'active'">
                <div class="header">
                    <h2>Cluster Initialization</h2>
                    <p>Use this option to either initialize the cluster with the data from an existing backup; or to start with an empty cluster and run some custom SQL scripts.</p>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <div class="col">
                            <label>Source</label>
                            <select v-model="initializationMethod">
                                <option value="">Choose one...</option>
                                <option value="backup" v-if="(!editMode && backups.length)">Restore from Backup</option>
                                <option value="script">Initialize from Scripts</option>
                            </select>
                        </div>
                        
                        <template v-if="( (initializationMethod == 'backup') && ( (editMode && restoreBackup.length) || (!editMode && backups.length) ) )">
                            <div class="col">
                                <label for="spec.initialData.restore.fromBackup">Backup Selection</label>
                                <select v-model="restoreBackup" data-field="spec.initialData.restore.fromBackup" @change="initDatepicker()">
                                    <option value="">Select a Backup</option>
                                    <template v-for="backup in backups" v-if="( (backup.data.metadata.namespace == namespace) && backup.data.status !== null )">
                                        <option v-if="backup.data.status.process.status === 'Completed'" :value="backup.data.metadata.uid">
                                            {{ backup.name }} ({{ backup.data.status.process.timing.stored | formatTimestamp('date') }} {{ backup.data.status.process.timing.stored | formatTimestamp('time') }} {{ showTzOffset() }}) [{{ backup.data.metadata.uid.substring(0,4) }}...{{ backup.data.metadata.uid.slice(-4) }}]
                                        </option>
                                    </template>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup')"></span>
                            </div>

                            <div :style="!restoreBackup.length ? 'display: none;' : ''">
                                <template v-if="!editMode || (editMode && pitr.length)">
                                    <div class="col">
                                        <label for="spec.initialData.restore.fromBackup.pointInTimeRecovery">Point-in-Time Recovery (PITR)</label>
                                        <input class="datePicker" autocomplete="off" placeholder="YYYY-MM-DD HH:MM:SS" :value="pitrTimezone">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup.pointInTimeRecovery')"></span>
                                    </div>
                                </template>

                                <div class="col">
                                    <label for="spec.initialData.restore.downloadDiskConcurrency">Download Disk Concurrency</label>
                                    <input v-model="downloadDiskConcurrency" data-field="spec.initialData.restore.downloadDiskConcurrency" autocomplete="off" type="number" min="0">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.downloadDiskConcurrency')"></span>
                                </div>
                            </div>
                        </template>
                    </div>

                    <div class="scriptFieldset section" v-if="(initializationMethod == 'script')">
                        <div class="header">
                            <h3 for="spec.initialData.scripts">Scripts</h3>
                            <a class="addRow" @click="pushScript()" v-if="!editMode">Add Script</a>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts')"></span>
                        </div>

                        <template v-if="initScripts.length">
                            <div class="script repeater">
                                <fieldset v-for="(script, index) in initScripts">
                                    <div class="header">
                                        <h2>Script #{{ index+1 }} <template v-if="script.hasOwnProperty('name')">–</template> <span class="scriptTitle">{{ script.name }}</span></h2>
                                        <a class="addRow" @click="spliceArray(initScripts, index)" v-if="!editMode">Delete</a>
                                    </div>    
                                    <div class="row">
                                        <div class="row-50">
                                            <template v-if="script.hasOwnProperty('name')">
                                                <div class="col">
                                                    <label for="spec.initialData.scripts.name">Name</label>
                                                    <input v-model="script.name" placeholder="Type a name..." :disabled="editMode" autocomplete="off">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts.name')"></span>
                                                </div>
                                            </template>

                                            <template v-if="script.hasOwnProperty('database')">
                                                <div class="col">
                                                    <label for="spec.initialData.scripts.database">Database</label>
                                                    <input v-model="script.database" placeholder="Type a database name..." :disabled="editMode" autocomplete="off">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts.database')"></span>
                                                </div>
                                            </template>
                                        </div>

                                        <div class="row-100">
                                            <div class="col">
                                                <label for="spec.initialData.scripts.scriptSource">Script Source</label>
                                                <select v-model="scriptSource[index]" @change="setScriptSource(index)" :disabled="editMode">
                                                    <option value="raw">Raw script</option>
                                                    <option value="secretKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.secretScript')">From Secret</option>
                                                    <option value="configMapKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.configMapScript')">From ConfigMap</option>
                                                </select>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts.scriptSource', 'Determines whether the script should be read from a Raw SQL, a Kubernetes Secret or a ConfigMap')"></span>
                                                
                                                <template  v-if="(!editMode && (scriptSource[index] == 'raw') ) || (editMode && ( script.hasOwnProperty('script') || hasProp(script, 'scriptFrom.ConfigMapScript') ) )">
                                                    <label for="spec.initialData.scripts.script" class="script">Script</label> <span class="uploadScript" v-if="!editMode">or <a @click="getScriptFile(index)" class="uploadLink">upload a file</a></span> 
                                                    <input :id="'scriptFile'+index" type="file" @change="uploadScript" class="hide">
                                                
                                                    <textarea v-model="script.script" placeholder="Type a script..." :disabled="editMode"></textarea>
                                                </template>
                                                <template v-else-if="(!editMode && (scriptSource[index] == 'configMapKeyRef') )">
                                                    <div class="header">
                                                        <h3 for="spec.initialData.scripts.scriptFrom.properties.configMapKeyRef">Config Map Key Reference</h3>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts.scriptFrom.properties.configMapKeyRef')"></span> 
                                                    </div>
                                                    
                                                    <label for="spec.initialData.scripts.scriptFrom.properties.configMapKeyRef.properties.name">Name</label>
                                                    <input v-model="script.scriptFrom.configMapKeyRef.name" placeholder="Type a name.." :disabled="editMode" autocomplete="off">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>

                                                    <label for="spec.initialData.scripts.scriptFrom.properties.configMapKeyRef.properties.key">Key</label>
                                                    <input v-model="script.scriptFrom.configMapKeyRef.key" placeholder="Type a key.." :disabled="editMode" autocomplete="off">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                                </template>
                                                <template v-else-if="(scriptSource[index] == 'secretKeyRef')">
                                                    <div class="header">
                                                        <h3 for="spec.initialData.scripts.scriptFrom.properties.secretKeyRef">Secret Key Reference</h3>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts.scriptFrom.properties.secretKeyRef')"></span> 
                                                    </div>
                                                    
                                                    <label for="spec.initialData.scripts.scriptFrom.properties.secretKeyRef.properties.name">Name</label>
                                                    <input v-model="script.scriptFrom.secretKeyRef.name" placeholder="Type a name.." :disabled="editMode" autocomplete="off">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>

                                                    <label for="spec.initialData.scripts.scriptFrom.properties.secretKeyRef.properties.key">Key</label>
                                                    <input v-model="script.scriptFrom.secretKeyRef.key" placeholder="Type a key.." :disabled="editMode" autocomplete="off">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                                </template>
                                            </div>
                                        </div>
                                    </div>
                                </fieldset>
                            </div>
                        </template>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'sidecars') && 'active'">
                <div class="header">
                    <h2>Sidecars</h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <h3>Connection Pooling</h3>
                        <p>To solve the Postgres connection fan-in problem (handling large number of incoming connections) StackGres includes by default a connection pooler fronting every Postgres instance. It is deployed as a sidecar. You may opt-out as well as tune the connection pooler configuration.</p>

                        <div class="col">
                            <label for="spec.configurations.sgPoolingConfig">Connection Pooling</label>  
                            <label for="connPooling" class="switch yes-no">Enable <input type="checkbox" id="connPooling" v-model="connPooling" data-switch="NO"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
                        </div>

                        <div class="col">
                            <label for="connectionPoolingConfig">Connection Pooling Configuration</label>
                            <select v-model="connectionPoolingConfig" class="connectionPoolingConfig" :disabled="!connPooling" >
                                <option value="" selected>Default</option>
                                <option v-for="conf in connPoolConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>Postgres Utils</h3>
                        <p>Sidecar container with Postgres administration tools. Optional (on by default; recommended for troubleshooting).</p>

                        <div class="col">
                            <label for="spec.pods.disablePostgresUtil">Postgres Utils</label>  
                            <label for="postgresUtil" class="switch">Postgres Utils <input type="checkbox" id="postgresUtil" v-model="postgresUtil" data-switch="ON"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.disablePostgresUtil')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>Monitoring</h3>
                        <p>Enable Prometheus metrics scraping via service monitors. Check the <a href="https://stackgres.io/doc/latest/install/prerequisites/monitoring/" target="_blank">Installation -> Monitoring</a> section for information on how to enable in StackGres Grafana dashboard integration.</p>

                        <div class="col">
                            <label for="spec.pods.disableMetricsExporter">Metrics Exporter</label>  
                            <label for="metricsExporter" class="switch">Metrics Exporter <input type="checkbox" id="metricsExporter" v-model="metricsExporter" data-switch="ON"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.disableMetricsExporter')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.prometheusAutobind">Prometheus Autobind</label>  
                            <label for="prometheusAutobind" class="switch" data-field="spec.prometheusAutobind">Prometheus Autobind <input type="checkbox" id="prometheusAutobind" v-model="prometheusAutobind" data-switch="OFF"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.prometheusAutobind')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>Distributed Logs</h3>
                        <p>Send Postgres and Patroni logs to a central <a href="https://stackgres.io/doc/latest/reference/crd/sgdistributedlogs/" target="_blank">SGDistributedLogs</a> instance. Optional: if not enabled, logs are sent to the standard output.</p>

                        <div class="col">
                            <label for="spec.distributedLogs.sgDistributedLogs">Distributed Logs</label>
                            <select v-model="distributedLogs" class="distributedLogs" data-field="spec.distributedLogs.sgDistributedLogs">
                                <option disabled value="">Select Logs Cluster</option>
                                <option v-for="cluster in logsClusters" :value="( (cluster.data.metadata.namespace !== $route.params.namespace) ? cluster.data.metadata.namespace + '.' : '') + cluster.data.metadata.name">{{ cluster.data.metadata.name }}</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.sgDistributedLogs')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'backups') && 'active'">
                <div class="header">
                    <h2>Backups</h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.configurations.sgBackupConfig">Automatic Backups</label>
                            <select v-model="backupConfig" class="backupConfig" data-field="spec.configurations.sgBackupConfig">
                                <option disabled value="">Select Backup Configuration</option>
                                <option v-for="conf in backupConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgBackupConfig')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'services') && 'active'">
                <div class="header">
                    <h2>Customize generated Kubernetes service</h2>
                </div>

                <div class="fields">
                    <fieldset class="postgresServicesPrimary">
                        <h3 for="spec.postgresServices.primary">
                            Primary
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary')"></span>
                        </h3>
                        
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.postgresServices.primary.enabled">Primary</label>  
                                <label for="postgresServicesPrimary" class="switch yes-no" data-field="spec.postgresServices.primary.enabled">Enable Primary <input type="checkbox" id="postgresServicesPrimary" v-model="postgresServicesPrimary" data-switch="YES"></label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.enabled')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.postgresServices.primary.type">Type</label>
                                <select v-model="postgresServicesPrimaryType" required data-field="spec.postgresServices.primary.type">    
                                    <option selected>ClusterIP</option>
                                    <option>LoadBalancer</option>
                                    <option>NodePort</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.type')"></span>
                            </div>
                        </div>
                    </fieldset>

                    <fieldset class="postgresServicesReplicas">
                        <h3 for="spec.postgresServices.replicas">
                            Replicas
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas')"></span>
                        </h3>
                            
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.postgresServices.replicas.enabled">Replicas</label>  
                                <label for="postgresServicesReplicas" class="switch yes-no" data-field="spec.postgresServices.replicas.enabled">Enable Replicas <input type="checkbox" id="postgresServicesReplicas" v-model="postgresServicesReplicas" data-switch="YES"></label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.enabled')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.postgresServices.replicas.type">Type</label>
                                <select v-model="postgresServicesReplicasType" required data-field="spec.postgresServices.replicas.type">    
                                    <option selected>ClusterIP</option>
                                    <option>LoadBalancer</option>
                                    <option>NodePort</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.type')"></span>
                            </div>
                        </div>
                    </fieldset>
                </div>
            </fieldset>

            <fieldset class="step podsMetadata" :class="(currentStep == 'metadata') && 'active'" id="podsMetadata">
                 <div class="header">
                    <h2>Metadata</h2>
                </div>

                <div class="fields">
                    <fieldset>
                        <div class="header">
                            <h3 for="spec.metadata.labels.clusterPods">
                                Pods Metadata
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.labels.clusterPods')"></span> 
                            </h3>
                            <a class="addRow" @click="pushLabel('podsMetadata')">Add Label</a>
                        </div>
                
                        <div class="metadata repeater" v-if="podsMetadata.length">
                            <div class="row" v-for="(field, index) in podsMetadata">
                                <label>Label</label>
                                <input class="label" v-model="field.label" autocomplete="off">

                                <span class="eqSign"></span>

                                <label>Value</label>
                                <input class="labelValue" v-model="field.value" autocomplete="off">

                                <a class="addRow" @click="spliceArray(podsMetadata, index)">Delete</a>
                            </div>
                        </div>
                    </fieldset>

                    <fieldset class="podsScheduling">
                        <div class="header">
                            <h3 for="spec.pods.scheduling">
                                Pods Scheduling
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling')"></span> 
                            </h3>
                        </div>
                
                        <fieldset class="nodeSelectors">
                            <div class="header">
                                <h4 for="spec.pods.scheduling.nodeSelector">
                                    Node Selectors
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeSelector')"></span> 
                                </h4>
                                <a class="addRow" @click="pushLabel('nodeSelector')">Add Node Selector</a>
                            </div>
                    
                            <div class="scheduling repeater" v-if="nodeSelector.length">
                                <div class="row" v-for="(field, index) in nodeSelector">
                                    <label>Key</label>
                                    <input class="label" v-model="field.label" autocomplete="off">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="labelValue" v-model="field.value" autocomplete="off">
                                    
                                    <a class="addRow" @click="spliceArray('nodeSelector', index)">Delete</a>
                                </a>
                                </div>
                            </div>
                        </fieldset>

                        <fieldset class="nodeTolerations">
                            <div class="header">
                                <h4 for="spec.pods.scheduling.tolerations">
                                    Node Tolerations
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations')"></span> 
                                </h4>
                                <a class="addRow" @click="pushToleration()">Add Toleration</a>
                            </div>
                    
                            <div class="scheduling repeater" v-if="tolerations.length">
                                <fieldset v-for="(field, index) in tolerations">
                                    <div class="header">
                                        <h4 for="spec.pods.scheduling.tolerations">Toleration #{{ index+1 }}</h4>
                                        <a class="addRow del" @click="spliceArray('tolerations', index)">Delete</a>
                                    </div>
                                    <div class="row-50">
                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.key">Key</label>
                                            <input v-model="field.key" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.key')"></span>
                                        </div>

                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.operator">Operator</label>
                                            <select v-model="field.operator" @change="(field.operator == 'Exists') ? (field.value = null) : null">
                                                <option>Equal</option>
                                                <option>Exists</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.operator')"></span>
                                        </div>

                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.value">Value</label>
                                            <input v-model="field.value" :disabled="(field.operator == 'Exists')" :title="(field.operator == 'Exists') ? 'When the selected operator is Exists, this value must be empty' : ''" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.value')"></span>
                                        </div>
                                        
                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.effect">Effect</label>
                                            <select v-model="field.effect">
                                                <option :value="nullVal">MatchAll</option>
                                                <option>NoSchedule</option>
                                                <option>PreferNoSchedule</option>
                                                <option>NoExecute</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.effect')"></span>
                                        </div>

                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.tolerationSeconds">Toleration Seconds</label>
                                            <input type="number" min="0" v-model="field.tolerationSeconds">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                        </div>
                                    </div>
                                </fieldset>
                            </div>
                        </fieldset>

                        <span class="warning" v-if="editMode">Please, be aware that any changes made to the <code>Pods Scheduling</code> will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance of the cluster</span>
                    </fieldset>

                    <fieldset class="resourcesMetadata">
                        <div class="header">
                            <h3 for="spec.metadata.annotations">
                                Resources Metadata
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations')"></span>
                            </h3>
                        </div>

                        <fieldset>
                            <div class="header">
                                <h3 for="spec.metadata.annotations.allResources">
                                    All Resources
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.allResources')"></span>
                                </h3>
                                <a class="addRow" @click="pushAnnotation('annotationsAll')">Add Annotation</a>
                            </div>
                            <div class="annotation repeater" v-if="annotationsAll.length">
                                <div class="row" v-for="(field, index) in annotationsAll">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off">

                                    <a class="addRow" @click="spliceArray(annotationsAll, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>

                        
                        <fieldset>
                            <div class="header">
                                <h3 for="spec.metadata.annotations.clusterPods">
                                    Cluster Pods
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.sgcluster', 'spec.metadata.annotations.clusterPods')"></span>
                                </h3>
                                <a class="addRow" @click="pushAnnotation('annotationsPods')">Add Annotation</a>
                            </div>
                            <div class="annotation repeater" v-if="annotationsPods.length">
                                <div class="row" v-for="(field, index) in annotationsPods">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off">

                                    <a class="addRow" @click="spliceArray(annotationsPods, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>
                    
                        <fieldset>
                            <div class="header">
                                <h3 for="spec.metadata.annotations.services">
                                    Services
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.services')"></span>
                                </h3>
                                <a class="addRow" @click="pushAnnotation('annotationsServices')">Add Annotation</a>
                            </div>
                            <div class="annotation repeater" v-if="annotationsServices.length">
                                <div class="row" v-for="(field, index) in annotationsServices">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off">

                                    <a class="addRow" @click="spliceArray(annotationsServices, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>

                        <fieldset>
                            <div class="header">
                                <h3 for="spec.metadata.annotations.primaryService">
                                    Primary Service 
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.primaryService')"></span>
                                </h3>
                                <a class="addRow" @click="pushAnnotation('postgresServicesPrimaryAnnotations')">Add Annotation</a>
                            </div>
                            <div class="annotation repeater" v-if="postgresServicesPrimaryAnnotations.length">
                                <div class="row" v-for="(field, index) in postgresServicesPrimaryAnnotations">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off">

                                    <a class="addRow" @click="spliceArray('postgresServicesPrimaryAnnotations', index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>

                        <fieldset>
                            <div class="header">
                                <h3 for="spec.metadata.annotations.replicasService">
                                    Replicas Service
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.replicasService')"></span>
                                </h3>
                                <a class="addRow" @click="pushAnnotation('postgresServicesReplicasAnnotations')">Add Annotation</a>
                            </div>
                            <div class="annotation repeater" v-if="postgresServicesReplicasAnnotations.length">
                                <div class="row" v-for="(field, index) in postgresServicesReplicasAnnotations">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off">

                                    <a class="addRow" @click="spliceArray(postgresServicesReplicasAnnotations, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>
                    </fieldset>
                </div>
            </fieldset>

            <fieldset class="step podsMetadata" :class="(currentStep == 'scheduling') && 'active'" id="podsScheduling">
                <div class="header">
                    <h2>Pods Scheduling</h2>
                </div>
                <div class="fields">
                    
                    <div class="section">
                        <div class="header">
                            <h3 for="spec.pods.scheduling.nodeSelector">
                                Node Selectors
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeSelector')"></span>
                            </h3>
                            <a class="addRow" @click="pushLabel('nodeSelector')">Add Node Selector</a>
                        </div>
                
                        <div class="scheduling repeater" v-if="nodeSelector.length">
                            <div class="row" v-for="(field, index) in nodeSelector">
                                <label>Key</label>
                                <input class="label" v-model="field.label" autocomplete="off">

                                <span class="eqSign"></span>

                                <label>Value</label>
                                <input class="labelValue" v-model="field.value" autocomplete="off">
                                
                                <a class="addRow" @click="spliceArray(nodeSelector, index)">Delete</a>
                            </div>
                        </div>
                    </div>

                    <div class="section">
                        <div class="header">
                            <h3 for="spec.pods.scheduling.tolerations">
                                Node Tolerations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations')"></span>
                            </h3>
                            <a class="addRow" @click="pushToleration()">Add Toleration</a>
                        </div>
                
                        <div class="scheduling repeater" v-if="tolerations.length">
                            <fieldset>
                                <div class="section" v-for="(field, index) in tolerations">
                                    <div class="header">
                                        <h4 for="spec.pods.scheduling.tolerations">Toleration #{{ index+1 }}</h4>
                                        <a class="addRow del" @click="spliceArray(tolerations, index)">Delete</a>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.key">Key</label>
                                            <input v-model="field.key" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.key')"></span>
                                        </div>
                                        
                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.operator">Operator</label>
                                            <select v-model="field.operator" @change="(field.operator == 'Exists') ? (field.value = null) : null">
                                                <option>Equal</option>
                                                <option>Exists</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.operator')"></span>
                                        </div>

                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.value">Value</label>
                                            <input v-model="field.value" :disabled="(field.operator == 'Exists')" :title="(field.operator == 'Exists') ? 'When the selected operator is Exists, this value must be empty' : ''" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.value')"></span>
                                        </div>

                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.effect">Effect</label>
                                            <select v-model="field.effect">
                                                <option :value="nullVal">MatchAll</option>
                                                <option>NoSchedule</option>
                                                <option>PreferNoSchedule</option>
                                                <option>NoExecute</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.effect')"></span>
                                        </div>

                                        <div class="col">
                                            <label for="spec.pods.scheduling.tolerations.tolerationSeconds">Toleration Seconds</label>
                                            <input type="number" min="0" v-model="field.tolerationSeconds">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                        </div>
                    </div>

                    <div class="section">                        
                        <div class="header">
                            <h3 for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                <span class="normal">Required During Scheduling Ignored During Execution</span>
                            </h3>                            
                        </div>

                        <div class="scheduling repeater">
                            <fieldset>
                                <div class="header">
                                    <h4 for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                        Node Selector Terms
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')"></span>
                                    </h4>
                                    <a class="addRow" @click="addRequiredAffinityTerm()">Add New</a>
                                </div>
                                <fieldset v-if="requiredAffinity.length">
                                    <div class="section" v-for="(requiredAffinityTerm, termIndex) in requiredAffinity">
                                        <div class="header">
                                            <h5>Term #{{ termIndex + 1 }}</h5>
                                            <a class="addRow" @click="spliceArray(requiredAffinity, termIndex)">Delete</a>
                                        </div>
                                        <fieldset>
                                            <div class="header">
                                                <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span> 
                                                </label>
                                                <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchExpressions)">Add Expression</a>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items')"></span> 
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchExpressions, expIndex)">Delete</a>
                                                    </div>
                                                    
                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key...">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span> 
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( !expression.hasOwnProperty('values') && (expression['values'] = ['']) ) )">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span> 
                                                        </div>
                                                    </div>

                                                    <div class="section" v-if="!['Exists', 'DoesNotExists'].includes(expression.operator)">
                                                        <div class="header">
                                                            <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                                Values
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                            <a class="addRow" @click="expression.values.push('')" v-if="!['Gt', 'Lt'].includes(expression.operator)">Add Value</a>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label>Value #{{ valIndex + 1 }}</label>
                                                            <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'">
                                                            <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="expression.values.length > 1">Delete</a>
                                                        </div>
                                                    </div>
                                                </div>
                                            </fieldset>
                                        </fieldset>
                                        <fieldset>
                                            <div class="header">
                                                <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields">
                                                    Match Fields
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span> 
                                                </label>
                                                <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchFields)">Add Field</a>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.matchFields.length">
                                                <div class="section" v-for="(field, fieldIndex) in requiredAffinityTerm.matchFields">
                                                    <div class="header">
                                                        <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items">
                                                            Match Field #{{ fieldIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items')"></span> 
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchFields, fieldIndex)">Delete</a>
                                                    </div>
                                                    
                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">Key</label>
                                                            <input v-model="field.key" autocomplete="off" placeholder="Type a key...">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span> 
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">Operator</label>
                                                            <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( !field.hasOwnProperty('values') && (field['values'] = ['']) ) )">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <div class="section" v-if="!['Exists', 'DoesNotExists'].includes(field.operator)">
                                                        <div class="header">
                                                            <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                                Values
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span> 
                                                            </label>
                                                            <a class="addRow" @click="field.values.push('')" v-if="!['Gt', 'Lt'].includes(field.operator)">Add Value</a>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                            <label>Value #{{ valIndex + 1 }}</label>
                                                            <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'">
                                                            <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="field.values.length > 1">Delete</a>
                                                        </div>
                                                    </div>
                                                </div>
                                            </fieldset>
                                        </fieldset>
                                    </div>
                                </fieldset>
                            </fieldset>
                        </div>
                    </div>
                    
                    <div class="section">                        
                        <div class="header">
                            <h3 for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                <span class="normal">Preferred During Scheduling Ignored During Execution</span>
                            </h3>
                        </div>

                        <div class="scheduling repeater">
                            <fieldset>
                                <div class="header">
                                    <h4 for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items">
                                        Node Selector Terms
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span> 
                                    </h4>
                                    <a class="addRow" @click="addPreferredAffinityTerm()">Add New</a>
                                </div>
                                <fieldset v-if="preferredAffinity.length">
                                    <div class="section" v-for="(preferredAffinityTerm, termIndex) in preferredAffinity">
                                        <div class="header">
                                            <h5>Term #{{ termIndex + 1 }}</h5>
                                            <a class="addRow" @click="spliceArray(preferredAffinity, termIndex)">Delete</a>
                                        </div>
                                        <fieldset>
                                            <div class="header">
                                                <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                </label>
                                                <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchExpressions)">Add Expression</a>
                                            </div>
                                            <fieldset v-if="preferredAffinityTerm.preference.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.preference.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchExpressions, expIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key...">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( !expression.hasOwnProperty('values') && (expression['values'] = ['']) ) )">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <div class="section" v-if="!['Exists', 'DoesNotExists'].includes(expression.operator)">
                                                        <div class="header">
                                                            <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                Values
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                            <a class="addRow" @click="expression.values.push('')" v-if="!['Gt', 'Lt'].includes(expression.operator)">Add Value</a>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label>Value #{{ valIndex + 1 }}</label>
                                                            <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'">
                                                            <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="expression.values.length > 1">Delete</a>
                                                        </div>
                                                    </div>
                                                </div>
                                            </fieldset>
                                        </fieldset>
                                        <fieldset>
                                            <div class="header">
                                                <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields">
                                                    Match Fields
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                </label>
                                                <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchFields)">Add Field</a>
                                            </div>
                                            <fieldset v-if="preferredAffinityTerm.preference.matchFields.length">
                                                <div class="section" v-for="(field, fieldIndex) in preferredAffinityTerm.preference.matchFields">
                                                    <div class="header">
                                                        <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items">
                                                            Match Field #{{ fieldIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchFields, fieldIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">Key</label>
                                                            <input v-model="field.key" autocomplete="off" placeholder="Type a key...">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">Operator</label>
                                                            <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( !field.hasOwnProperty('values') && (field['values'] = ['']) ) )">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <div class="section" v-if="!['Exists', 'DoesNotExists'].includes(field.operator)">
                                                        <div class="header">
                                                            <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                                Values
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                            </label>
                                                            <a class="addRow" @click="field.values.push('')" v-if="!['Gt', 'Lt'].includes(field.operator)">Add Value</a>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                            <label>Value #{{ valIndex + 1 }}</label>
                                                            <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'">
                                                            <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="field.values.length > 1">Delete</a>
                                                        </div>
                                                    </div>
                                                </div>
                                            </fieldset>
                                        </fieldset>

                                        <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">Weight</label>
                                        <input v-model="preferredAffinityTerm.weight" autocomplete="off" type="number" min="1" max="100">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight')"></span>
                                    </div>
                                </fieldset>
                            </fieldset>
                        </div>
                    </div>

                    <span class="warning" v-if="editMode">Please, be aware that any changes made to the <code>Pods Scheduling</code> will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance of the cluster</span>
                </div>
            </fieldset>

            <hr/>

            <template v-if="editMode">
                <a class="btn" @click="createCluster">Update Cluster</a>
            </template>
            <template v-else>
                <a class="btn" @click="createCluster">Create Cluster</a>
            </template>

            <a class="btn floatRight" @click="viewSummary">View Summary</a>

            <a @click="cancel" class="btn border">Cancel</a>
        
        </div>
    </form>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import axios from 'axios'
    import moment from 'moment'

    export default {
        name: 'CreateCluster',

        mixins: [mixin],

        data: function() {

            const vm = this;

            return {
                advancedMode: false,
                formSteps: ['cluster', 'postgres', 'extensions', 'initialization', 'sidecars', 'backups', 'services', 'metadata', 'scheduling'],
                currentStep: 'cluster',
                editMode: (vm.$route.name === 'EditCluster'),
                editReady: false,
                help: 'Click on a question mark to get help and tips about that field.',
                nullVal: null,
                name: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                namespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                postgresVersion: 'latest',
                instances: 1,
                resourceProfile: '',
                pgConfig: '',
                storageClass: '',
                volumeSize: 1,
                volumeUnit: 'Gi',
                connPooling: true,
                connectionPoolingConfig: '',
                initializationMethod: '',
                restoreBackup: '',
                pitr: '',
                downloadDiskConcurrency: '',
                backupConfig: '',
                distributedLogs: '',
                prometheusAutobind: false,
                disableClusterPodAntiAffinity: false,
                postgresUtil: true,
                metricsExporter: true,
                podsMetadata: [ { label: '', value: ''} ],
                nodeSelector: [ { label: '', value: ''} ],
                tolerations: [ { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null } ],
                pgConfigExists: true,
                currentScriptIndex: 0,
                initScripts: [ { name: '', database: '', script: '' } ],
                scriptSource: ['raw'],
                annotationsAll: [ { annotation: '', value: '' } ],
                annotationsAllText: '',
                annotationsPods: [ { annotation: '', value: '' } ],
                annotationsServices: [ { annotation: '', value: '' } ],
                postgresServicesPrimary: true,
                postgresServicesPrimaryType: 'ClusterIP',
                postgresServicesPrimaryAnnotations: [ { annotation: '', value: '' } ],
                postgresServicesReplicas: true,
                postgresServicesReplicasType: 'ClusterIP',
                postgresServicesReplicasAnnotations: [ { annotation: '', value: '' } ],
                searchExtension: '',
                extensionsList: [],
                selectedExtensions: [],
                viewExtension: -1,
                extVersion: {
                    name: '',
                    version: ''
                },
                affinityOperators: [
                    { label: 'In', value: 'In' },
                    { label: 'Not In', value: 'NotIn' },
                    { label: 'Exists', value: 'Exists' },
                    { label: 'Does Not Exists', value: 'DoesNotExists' },
                    { label: 'Greater Than', value: 'Gt' },
                    { label: 'Less Than', value: 'Lt' },
                ],
                requiredAffinity: [
                    {   
                        matchExpressions: [
                            { key: '', operator: '', values: [ '' ] }
                        ],
                        matchFields: [
                            { key: '', operator: '', values: [ '' ] }
                        ]
                    }
                ],
                preferredAffinity: [
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
                ]
            }

        },
        
        computed: {

            allNamespaces () {
                return store.state.allNamespaces
            },
            profiles () {
                return store.state.profiles
            },
            pgConf () {
                return store.state.pgConfig
            },
            connPoolConf () {
                return store.state.poolConfig
            },
            backupConf () {
                return store.state.backupConfig
            },
            backups () {
                return store.state.backups
            },
            shortpostgresVersion () {
                if (this.postgresVersion == 'latest')
                    return Object.keys(store.state.postgresVersions).sort().reverse()[0];
                else
                    return this.postgresVersion.substring(0,2)
            },
            storageClasses() {
                return store.state.storageClasses
            },
            
            logsClusters(){
                return store.state.logsClusters
            },
            nameColission() {

                const vc = this;
                var nameColission = false;
                
                store.state.clusters.forEach(function(item, index){
                    if( (item.name == vc.name) && (item.data.metadata.namespace == vc.$route.params.namespace ) ) {
                        nameColission = true;
                        return false
                    }
                })

                return nameColission
            },
            isReady() {
                return store.state.ready
            },

            postgresVersionsList() {
                return store.state.postgresVersions
            },

            cluster () {

                var vm = this;
                var cluster = {};
                
                if( vm.editMode && !vm.editReady ) {
                    store.state.clusters.forEach(function( c ){
                        if( (c.data.metadata.name === vm.$route.params.name) && (c.data.metadata.namespace === vm.$route.params.namespace) ) {

                            console.log(c)
                        
                            let volumeSize = c.data.spec.pods.persistentVolume.size.match(/\d+/g);
                            let volumeUnit = c.data.spec.pods.persistentVolume.size.match(/[a-zA-Z]+/g);

                            vm.postgresVersion = c.data.spec.postgres.version;
                            vm.instances = c.data.spec.instances;
                            vm.resourceProfile = c.data.spec.sgInstanceProfile;
                            vm.pgConfig = c.data.spec.configurations.sgPostgresConfig;
                            vm.storageClass = c.data.spec.pods.persistentVolume.hasOwnProperty('storageClass') ? c.data.spec.pods.persistentVolume.storageClass : '';                            
                            vm.volumeSize = volumeSize;
                            vm.volumeUnit = ''+volumeUnit;
                            vm.connPooling = !c.data.spec.pods.disableConnectionPooling,
                            vm.connectionPoolingConfig = (typeof c.data.spec.configurations.sgPoolingConfig !== 'undefined') ? c.data.spec.configurations.sgPoolingConfig : '';
                            vm.backupConfig = (typeof c.data.spec.configurations.sgBackupConfig !== 'undefined') ? c.data.spec.configurations.sgBackupConfig : '';
                            vm.distributedLogs = (typeof c.data.spec.distributedLogs !== 'undefined') ? c.data.spec.distributedLogs.sgDistributedLogs : '';
                            vm.prometheusAutobind =  (typeof c.data.spec.prometheusAutobind !== 'undefined') ? c.data.spec.prometheusAutobind : false;
                            vm.disableClusterPodAntiAffinity = ( (typeof c.data.spec.nonProductionOptions !== 'undefined') && (typeof c.data.spec.nonProductionOptions.disableClusterPodAntiAffinity !== 'undefined') ) ? c.data.spec.nonProductionOptions.disableClusterPodAntiAffinity : false;
                            vm.metricsExporter = vm.hasProp(c, 'data.spec.pods.disableMetricsExporter') ? !c.data.spec.pods.disableMetricsExporter : true ;
                            vm.postgresUtil = vm.hasProp(c, 'data.spec.pods.disablePostgresUtil') ? !c.data.spec.pods.disablePostgresUtil : true ;
                            vm.podsMetadata = vm.hasProp(c, 'data.spec.metadata.labels.clusterPods') ? vm.unparseProps(c.data.spec.metadata.labels.clusterPods, 'label') : [];
                            vm.nodeSelector = vm.hasProp(c, 'data.spec.pods.scheduling.nodeSelector') ? vm.unparseProps(c.data.spec.pods.scheduling.nodeSelector, 'label') : [];
                            vm.tolerations = vm.hasProp(c, 'data.spec.pods.scheduling.tolerations') ? c.data.spec.pods.scheduling.tolerations : [];
                            vm.preferredAffinity = vm.hasProp(c, 'data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') ? c.data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution : [];
                            vm.requiredAffinity = vm.hasProp(c, 'data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution') ? c.data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms : [];
                            vm.tolerations = vm.hasProp(c, 'data.spec.pods.scheduling.tolerations') ? c.data.spec.pods.scheduling.tolerations : [];
                            vm.pgConfigExists = true;

                            if(vm.hasProp(c, 'data.spec.initialData.scripts')) {
                                
                                c.data.spec.initialData.scripts.forEach(function(script, index) {
                                    if(script.hasOwnProperty('script')) {
                                        vm.scriptSource[index] = 'raw';
                                    } else if(script.scriptFrom.hasOwnProperty('secretKeyRef')) {
                                        vm.scriptSource[index] = 'secretKeyRef';
                                    } else if(script.scriptFrom.hasOwnProperty('configMapScript')) {
                                        vm.scriptSource[index] = 'configMapKeyRef';
                                        script['script'] = script.scriptFrom.configMapScript;
                                    }
                                })
                                
                                vm.initScripts = c.data.spec.initialData.scripts;
                                vm.initializationMethod = 'script';

                            }

                            vm.annotationsAll = vm.hasProp(c, 'data.spec.metadata.annotations.allResources') ? vm.unparseProps(c.data.spec.metadata.annotations.allResources) : [];
                            vm.annotationsPods = vm.hasProp(c, 'data.spec.metadata.annotations.pods') ? vm.unparseProps(c.data.spec.metadata.annotations.pods) : [];
                            vm.annotationsServices = vm.hasProp(c, 'data.spec.metadata.annotations.services') ? vm.unparseProps(c.data.spec.metadata.annotations.services) : [];
                            vm.postgresServicesPrimary = vm.hasProp(c, 'data.spec.postgresServices.primary.enabled') ? c.data.spec.postgresServices.primary.enabled : false;
                            vm.postgresServicesPrimaryType = vm.hasProp(c, 'data.spec.postgresServices.primary.type') ? c.data.spec.postgresServices.primary.type : 'ClusterIP';
                            vm.postgresServicesPrimaryAnnotations = vm.hasProp(c, 'data.spec.metadata.annotations.primaryService') ?  vm.unparseProps(c.data.spec.metadata.annotations.primaryService) : [];
                            vm.postgresServicesReplicas = vm.hasProp(c, 'data.spec.postgresServices.replicas.enabled') ? c.data.spec.postgresServices.replicas.enabled : false;
                            vm.postgresServicesReplicasType = vm.hasProp(c, 'data.spec.postgresServices.replicas.type') ? c.data.spec.postgresServices.replicas.type : 'ClusterIP';
                            vm.postgresServicesReplicasAnnotations = vm.hasProp(c, 'data.spec.metadata.annotations.replicasService') ?  vm.unparseProps(c.data.spec.metadata.annotations.replicasService) : [];
                            vm.selectedExtensions = vm.hasProp(c, 'data.spec.postgres.extensions') ? c.data.spec.postgres.extensions : [];

                            vm.restoreBackup = vm.hasProp(c, 'data.spec.initialData.restore.fromBackup.uid') ? c.data.spec.initialData.restore.fromBackup.uid : '';
                            vm.pitr = vm.hasProp(c, 'data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp') ? c.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp : ''
                            
                            if(vm.initScripts.length) {
                                vm.initializationMethod = 'script';
                            } else if (vm.restoreBackup.length) {
                                vm.initializationMethod = 'backup';
                            }
                            
                            vm.editReady = vm.advancedMode = true
                            return false
                        }
                    });
                }

                return cluster
            },

            pitrTimezone () {
                return this.pitr.length ? ( (store.state.timezone == 'local') ? moment.utc(this.pitr).local().format('YYYY-MM-DD HH:mm:ss') : moment.utc(this.pitr).format('YYYY-MM-DD HH:mm:ss') ) : '';
            }

        },

        methods: {

            getScriptFile: function( index ){
                this.currentScriptIndex = index;
                $('input#scriptFile'+index).click();
            },

            uploadScript: function(e) {
                var files = e.target.files || e.dataTransfer.files;
                var vm = this;

                if (!files.length){
                    console.log("File not loaded")
                    return;
                } else {
                    //console.log("File loaded");

                    var reader = new FileReader();
                    
                    reader.onload = function(e) {
                    vm.initScripts[vm.currentScriptIndex].script = e.target.result;
                    };
                    reader.readAsText(files[0]);
                }
            },

            pushScript() {
                this.initScripts.push( { name: '', database: '', script: ''} );
                this.scriptSource[this.scriptSource.length] = 'raw';
            },

            setScriptSource( index ) {
                const vc = this;

                if(vc.scriptSource[index] == 'raw') {
                    delete vc.initScripts[index].scriptFrom;
                } else {
                    delete vc.initScripts[index].script;
                    vc.initScripts[index]['scriptFrom'] = {
                        [vc.scriptSource[index]]: {
                            name: '', 
                            key: ''
                        }
                    }
                }

            },

            spliceArray: function( prop, index ) {
                prop.splice( index, 1 );

                if(this.initScripts.length != this.scriptSource.length) {
                    this.scriptSource.splice( index, 1 );
                }
            },

            cleanupScripts: function() {
                var vm = this;

                if(vm.initScripts.length){
                    vm.initScripts.forEach(function(script, index){
                        if(script.hasOwnProperty('script') && !script.script.length)
                            vm.initScripts.splice( index, 1 )
                        
                        if(!script.name.length)
                            delete script.name

                        if(script.hasOwnProperty('database') && !script.database.length)
                            delete script.database
                    })
                }
            },

            pushLabel: function( prop ) {
                this[prop].push( { label: '', value: '' } )
            },

            pushAnnotation: function( prop ) {
                this[prop].push( { annotation: '', value: '' } )
            },

            pushToleration () {
                this.tolerations.push({ key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null })
            },

            createCluster: function() {
                const vc = this;

                if(vc.checkRequired()) {

                    this.cleanupScripts()
                    
                    var cluster = { 
                        "metadata": {
                            "name": this.name,
                            "namespace": this.namespace
                        },
                        "spec": {
                            "instances": this.instances,
                            ...(this.resourceProfile.length && ( {"sgInstanceProfile": this.resourceProfile }) ),
                            "pods": {
                                "persistentVolume": {
                                    "size": this.volumeSize+this.volumeUnit,
                                    ...( ( (this.storageClass !== undefined) && (this.storageClass.length ) ) && ( {"storageClass": this.storageClass }) )
                                },
                                "disableConnectionPooling": !this.connPooling,
                                "disableMetricsExporter": !this.metricsExporter,
                                "disablePostgresUtil": !this.postgresUtil,
                                ...(!$.isEmptyObject(this.parseProps(this.podsMetadata, 'label')) && ({
                                    "metadata": {
                                        "labels": this.parseProps(this.podsMetadata, 'label')
                                    }
                                }) ),
                                ...( ( this.hasNodeSelectors() || this.hasTolerations() || this.hasNodeAffinity(this.requiredAffinity) || this.hasNodeAffinity(this.preferredAffinity) ) && ({
                                    "scheduling": {
                                        ...(this.hasNodeSelectors() && ({"nodeSelector": this.parseProps(this.nodeSelector, 'label')})),
                                        ...(this.hasTolerations() && ({"tolerations": this.tolerations})),
                                        ...(this.hasNodeAffinity(this.requiredAffinity) || this.hasNodeAffinity(this.preferredAffinity) ) && {
                                            "nodeAffinity": {
                                                ...(this.hasNodeAffinity(this.requiredAffinity) && {
                                                    "requiredDuringSchedulingIgnoredDuringExecution": {
                                                        "nodeSelectorTerms": this.requiredAffinity
                                                    },
                                                }),
                                                ...(this.hasNodeAffinity(this.preferredAffinity) && {
                                                    "preferredDuringSchedulingIgnoredDuringExecution": this.preferredAffinity
                                                })
                                            }
                                        }
                                    }
                                }) )                    
                            },
                            "configurations": {
                                ...(this.pgConfig.length && ( {"sgPostgresConfig": this.pgConfig }) ),
                                ...(this.backupConfig.length && ( {"sgBackupConfig": this.backupConfig }) ),
                                ...(this.connectionPoolingConfig.length && ( {"sgPoolingConfig": this.connectionPoolingConfig }) ),
                            },
                            ...(this.distributedLogs.length && ({
                                "distributedLogs": {
                                    "sgDistributedLogs": this.distributedLogs
                                }
                            })),
                            ...( (this.restoreBackup.length || this.initScripts.length) && ({
                                    "initialData": {
                                        ...( this.restoreBackup.length && ({
                                            "restore": { 
                                                "fromBackup": {
                                                    "uid": this.restoreBackup, 
                                                    ...(this.pitr.length  && ({
                                                        "pointInTimeRecovery": {
                                                            "restoreToTimestamp": this.pitr
                                                        }
                                                        })  
                                                    )
                                                },
                                                ...(this.downloadDiskConcurrency.length  && ({
                                                    "downloadDiskConcurrency": this.downloadDiskConcurrency 
                                                }) )
                                            },
                                        }) ),
                                        ...( this.initScripts.length && ({
                                            "scripts": this.initScripts.slice()
                                        }) )
                                    }
                                }) 
                            ),
                            ...(this.prometheusAutobind && ( {"prometheusAutobind": this.prometheusAutobind }) ),
                            ...(this.disableClusterPodAntiAffinity && ( {"nonProductionOptions": { "disableClusterPodAntiAffinity": this.disableClusterPodAntiAffinity } }) ),
                            ...( (!$.isEmptyObject(this.parseProps(this.annotationsAll)) || !$.isEmptyObject(this.parseProps(this.annotationsPods)) || !$.isEmptyObject(this.parseProps(this.annotationsServices)) || !$.isEmptyObject(this.parseProps(this.postgresServicesPrimaryAnnotations)) || !$.isEmptyObject(this.parseProps(this.postgresServicesReplicasAnnotations)) || !$.isEmptyObject(this.parseProps(this.podsMetadata, 'label')) ) && ({
                                "metadata": {
                                    "annotations": {
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsAll)) && ( {"allResources": this.parseProps(this.annotationsAll) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsPods)) && ( {"clusterPods": this.parseProps(this.annotationsPods) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsServices)) && ( {"services": this.parseProps(this.annotationsServices) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.postgresServicesPrimaryAnnotations)) && ( {"primaryService": this.parseProps(this.postgresServicesPrimaryAnnotations) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.postgresServicesReplicasAnnotations)) && ( {"replicasService": this.parseProps(this.postgresServicesReplicasAnnotations) }) ),
                                    },
                                    ...(!$.isEmptyObject(this.parseProps(this.podsMetadata, 'label')) && ({
                                        "labels": {
                                            "clusterPods": this.parseProps(this.podsMetadata, 'label')
                                        }
                                    }) )
                                }
                            }) ),
                            "postgresServices": {
                                "primary": {
                                    "enabled": this.postgresServicesPrimary,
                                    "type": this.postgresServicesPrimaryType,
                                },
                                "replicas": {
                                    "enabled": this.postgresServicesReplicas,
                                    "type": this.postgresServicesReplicasType,
                                }
                            },
                            "postgres": {
                                "version": this.postgresVersion,
                                ...(this.selectedExtensions.length && ({
                                    "extensions": this.selectedExtensions
                                }))
                            }

                        }
                    }  

                    if(this.editMode) {
                        const res = axios
                        .put(
                            '/stackgres/sgclusters', 
                            cluster 
                        )
                        .then(function (response) {
                            vc.notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> updated successfully', 'message', 'sgclusters');

                            vc.fetchAPI('sgcluster');
                            router.push('/' + cluster.metadata.namespace + '/sgcluster/' + cluster.metadata.name);
                            
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error', 'sgclusters');
                        });
                    } else {
                        const res = axios
                        .post(
                            '/stackgres/sgclusters', 
                            cluster 
                        )
                        .then(function (response) {
                            vc.notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> created successfully', 'message', 'sgclusters');

                            vc.fetchAPI('sgcluster');
                            router.push('/' + cluster.metadata.namespace + '/sgclusters');
                            
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error','sgclusters');
                        });
                    }

                }

            }, 

            checkPgConfigVersion: function() {
                let configs = store.state.pgConfig.length;
                let vc = this;

                store.state.pgConfig.forEach(function(item, index){
                    if( (item.data.spec.postgres.version !== vc.shortpostgresVersion) && (item.data.metadata.namespace == vc.$route.params.namespace) )
                        configs -= configs;
                });

                vc.pgConfigExists = (configs > 0);
            },

            setVersion: function( version = 'latest') {
                const vc = this

                if( vc.postgresVersion !== version.substring(0,2) ) {
                    vc.getExtensions(version);
                }
                
                vc.postgresVersion = version;
                $('#postgresVersion .active, #postgresVersion').removeClass('active');
                $('#postgresVersion [data-val="'+version+'"]').addClass('active');
            },

            sanitizeString( string ) {
               return string.replace(/\\/g, "\\\\").replace(/\n/g, "\\n").replace(/\r/g, "\\r").replace(/\t/g, "\\t").replace(/\f/g, "\\f").replace(/"/g,"\\\"").replace(/'/g,"\\\'").replace(/\&/g, "\\&"); 
            },

            parseProps ( props, key = 'annotation' ) {
                const vc = this
                var jsonString = '{';
                props.forEach(function(p, i){
                    if(p[key].length && p.value.length) {                    
                        if(i)
                            jsonString += ','
                        
                        jsonString += '"'+vc.sanitizeString(p[key])+'":"'+vc.sanitizeString(p.value)+'"'
                    }                
                })
                jsonString += '}'

                return JSON.parse(jsonString)
            },
            
            unparseProps ( props, key = 'annotation' ) {
                var propsArray = [];

                Object.entries(props).forEach(([k, v]) => {
                    var prop = {};
                    prop[key] = k;
                    prop['value'] = v;

                    propsArray.push(prop)
                });
                return propsArray
            },

            hasTolerations () {
                const vc = this
                let t = [...vc.tolerations]

                t.forEach(function(item, index) {
                    if(JSON.stringify(item) == '{"key":"","operator":"Equal","value":null,"effect":null,"tolerationSeconds":null}')
                        vc.tolerations.splice( index, 1 )
                })

                return vc.tolerations.length
            },

            hasNodeSelectors () {
                const vc = this
                let nS = [...vc.nodeSelector]

                nS.forEach(function(item, index) {
                    if(JSON.stringify(item) == '{"label":"","value":""}')
                        vc.nodeSelector.splice( index, 1 )
                })

                return vc.nodeSelector.length
            },

            toggleStep(id) {
                $(id + '> .fields').slideToggle()
                $(id + '> .header').toggleClass('open')

                if($(id + '> .header .toggleFields').text() == 'Expand')
                    $(id + '> .header .toggleFields').text('Collapse')
                else
                    $(id + '> .header .toggleFields').text('Expand')
            },

            viewExt(index) {
                const vc = this;
                
                vc.viewExtension = (vc.viewExtension == index) ? -1 : index

                let ext = vc.selectedExtensions.find(e => (e.name == vc.extensionsList[index].name))

                if(typeof ext !== 'undefined') {
                    vc.extVersion.version = ext.version
                    vc.extVersion.name = ext.name
                }
                else {
                    vc.extVersion.version = vc.extensionsList[index].versions[0]
                    vc.extVersion.name = vc.extensionsList[index].name
                }
            },

            setExtension(index) {
                const vc = this
                let i = -1
                
                vc.selectedExtensions.forEach(function(ext, j) {
                    if(ext.name == vc.extensionsList[index].name) {
                        i = j
                        return false
                    }
                })
                
                if( i == -1) // If not included, add extension
                    vc.selectedExtensions.push({
                        name: vc.extensionsList[index].name,
                        version: vc.extensionsList[index].selectedVersion,
                        publisher: vc.extensionsList[index].publisher,
                        repository: vc.extensionsList[index].repository
                    })
                else // If included, remove
                    vc.selectedExtensions.splice(i, 1);
            },

            extIsSet(ext) {
                const vc = this
                var index = -1

                vc.selectedExtensions.forEach(function(e, i){
                    if(e.name == ext) {
                        index = i
                        return false
                    }
                })

                return index
            },

            extAvailableFor(index) {
                const vc = this
                let available = false

                vc.extensionsList[index].versions.forEach(function(v) {
                    v.availableFor.forEach(function(vv) {
                        if(vc.shortpostgresVersion == vv.postgresVersion) {
                            available = true
                            return false
                        }
                    })
                })

                return available
            },

            setExtVersion(version) {
                const vc = this
                let ext = vc.selectedExtensions.find(e => (vc.extensionsList[vc.viewExtension].name == e.name)); 
                
                if( typeof ext == 'undefined')
                    vc.selectedExtensions.push({
                        name: vc.extensionsList[vc.viewExtension].name,
                        version: version,
                        publisher: vc.extensionsList[vc.viewExtension].publisher,
                        repository: vc.extensionsList[vc.viewExtension].repository
                    })
                else
                    ext.version = version
                
                vc.extVersion.name = vc.extensionsList[vc.viewExtension].name
                vc.extVersion.version = version
            },

            clearExtFilters() {
                this.searchExtension = ''
                this.viewExtension = -1
            },

            toggleStep(step) {
                $('[data-step].active, [data-step="' + step + '"]').toggleClass('active');
            },

            sortExtensions(ext) {
				return [...ext].sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0))
			},

            initDatepicker() {
                const vc = this;
                let minDate = null;
                let maxDate = null;

                store.state.backups.forEach(function(fromBackup, index) {
                    
                    if( fromBackup.data.metadata.uid == vc.restoreBackup ) {
                        minDate = new Date(new Date(fromBackup.data.status.process.timing.stored).getTime());

                        for(var i = index + 1; i < store.state.backups.length; i++) {
                            let nextBackup = store.state.backups[i];
                            
                            if( (nextBackup.data.metadata.namespace == fromBackup.data.metadata.namespace) && (nextBackup.data.status.process.status == 'Completed') ) {
                                maxDate = new Date(new Date(nextBackup.data.status.process.timing.stored).getTime());
                                return false;
                            }
                        }

                        return false;
                    }

                })
                
                if(!maxDate)
                    maxDate = new Date(new Date().getTime());

                // Load datepicker
			    require('daterangepicker');

                $('.daterangepicker').remove()
                $(document).find('.datePicker').daterangepicker({
                    "autoApply": true,
                    "singleDatePicker": true,
                    "timePicker": true,
                    "opens": "right",
                    "minDate": minDate,
                    "maxDate": maxDate,
                    "timePicker24Hour": true,
                    "timePickerSeconds": true,
                    locale: {
                        cancelLabel: "Clear",
                        format: 'YYYY-MM-DD HH:mm:ss'
                    }
                }, function(start, end, label) {
                    vc.pitr = (store.state.timezone == 'local') ? start.utc().format() : ( start.format('YYYY-MM-DDTHH:mm:ss') + 'Z' )
                });
            },

            addNodeSelectorRequirement(affinity) {
                affinity.push({ key: '', operator: '', values: [ '' ] })
            },

            addRequiredAffinityTerm() {
                const vc = this;
                vc.requiredAffinity.push({
                    matchExpressions: [
                        { key: '', operator: '', values: [ '' ] }
                    ],
                    matchFields: [
                        { key: '', operator: '', values: [ '' ] }
                    ]
                })
            },
            
            addPreferredAffinityTerm() {
                const vc = this;
                vc.preferredAffinity.push({
                    preference: {
                        matchExpressions: [
                            { key: '', operator: '', values: [ '' ] }
                        ],
                        matchFields: [
                            { key: '', operator: '', values: [ '' ] }
                        ]
                    },
                    weight: 1
                })
            },

            hasNodeAffinity (affinity) {
                let aff = Array.from(affinity);
                
                aff.forEach(function(a, affIndex) {

                    let item = a.hasOwnProperty('preference') ? a.preference : a;
                    
                    if(item.hasOwnProperty('matchExpressions')) {
                        item.matchExpressions.forEach(function(exp, expIndex) {
                            if(!exp.key.length || !exp.operator.length || (exp.hasOwnProperty('values') && (exp.values == ['']) ) ) {
                                if(affinity[affIndex].hasOwnProperty('preference')) {
                                    affinity[affIndex].preference.matchExpressions.splice( expIndex, 1 );
                                } else {
                                    affinity[affIndex].matchExpressions.splice( expIndex, 1 );  
                                }
                            }
                        });

                        if(affinity[affIndex].hasOwnProperty('preference') && !affinity[affIndex].preference.matchExpressions.length) {
                            delete affinity[affIndex].preference.matchExpressions;
                        } else if(!affinity[affIndex].hasOwnProperty('preference') && !affinity[affIndex].matchExpressions.length) {
                            delete affinity[affIndex].matchExpressions;
                        }
                    }

                    if(item.hasOwnProperty('matchFields')) {
                        item.matchFields.forEach(function(exp, expIndex) {
                            if(!exp.key.length || !exp.operator.length || (exp.hasOwnProperty('values') && (exp.values == ['']) ) ) {
                                if(affinity[affIndex].hasOwnProperty('preference')) {
                                    affinity[affIndex].preference.matchFields.splice( expIndex, 1 );
                                } else {
                                    affinity[affIndex].matchFields.splice( expIndex, 1 );  
                                }
                            }
                        });

                        if(affinity[affIndex].hasOwnProperty('preference') && !affinity[affIndex].preference.matchFields.length) {
                            delete affinity[affIndex].preference.matchFields;
                        } else if(!affinity[affIndex].hasOwnProperty('preference') && !affinity[affIndex].matchFields.length) {
                            delete affinity[affIndex].matchFields;
                        }
                    }

                    if(affinity[affIndex].hasOwnProperty('preference')) {
                        if(!affinity[affIndex].preference.hasOwnProperty('matchExpressions') && !affinity[affIndex].preference.hasOwnProperty('matchFields')) {
                            affinity.splice( affIndex, 1 );
                        }
                    } else {
                        if(!affinity[affIndex].hasOwnProperty('matchExpressions') && !affinity[affIndex].hasOwnProperty('matchFields')) {
                            affinity.splice( affIndex, 1 );
                        }
                    }

                });

                return (affinity.length > 0);
            },

            getExtensions(version) {
                const vc = this;
                axios
                .get('/stackgres/extensions/' + ( (version == 'latest') ? 'latest' : version.split('.')[0] ))
                .then(function (response) {
                    response.data.extensions.forEach(function(ext){
                        if(ext.versions.length) {
                            ext['selectedVersion'] = ext.versions[0];
                        }
                    })
                    vc.extensionsList =  vc.sortExtensions(response.data.extensions)
                })
                .catch(function (error) {
                    console.log(error.response);
                    vc.notify(error.response.data,'error','sgclusters');
                });
            },

            updateExtVersion(name, version) {
                const vc = this;
                
                vc.selectedExtensions.forEach(function(ext) {
                    if(ext.name == name) {
                        ext.version = version;
                        return false
                    }
                })
            },

            viewSummary() {

            }

        },

        created: function() {
            const vc = this;

            if(vc.loggedIn) {
                vc.getExtensions('latest');
            }
        },

        beforeDestroy: function() {
            store.commit('setTooltipsText','Click on a question mark to get help and tips about that field.');
            $('.daterangepicker').remove()
        }

    }
</script>

<style scoped>
    .form {
        max-width: none;
        width: auto;
    }

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
    }

    .searchBar .clear {
        position: absolute;
        top: 0;
        right: 10px;
        border: 0;
        padding: 11px 0;
        z-index: 1;
    }

    .searchBar .clear:hover {
        background: transparent;
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
        max-height: 300px;
        overflow-y: auto;
        margin-bottom: 20px;
        padding-right: 10px;
    }

    .extension > label {
        font-weight: bold;
        cursor: pointer;
        width: calc(100% - 30px);
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

    li.extension.show > label:after {
        height: 1px;
        width: calc(100% + 30px);
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
        min-height: 75px;
    }

    ul#postgresVersion.active {
        position: absolute;
        width: calc(100% - 42px);
        z-index: 10;
        max-height: 30vh;
        overflow: auto;
    }

    ul#postgresVersion.active + a.help {
        margin-top: 15px;
    }

    ul.select li.selected {
        position: sticky;
        top: 0;
    }

    .affinityValues a.addRow {
        transform: translateY(-75px);
    }

    .step {
        display: none;
        width: 820px;
        border: 0;
        padding: 20px 0 0;
        margin-bottom: 0;
    }

    .step.active {
        display: block;
    }

    ul.steps {
        display: inline-block;
        margin: 10px 0 20px;
    }

    ul.steps li {
        float: left;
        list-style: none;
        position: relative;
        text-align: center;
        margin: 0 10px;
        text-transform: capitalize;
        width: 70px;
        cursor: pointer;
    }

    ul.steps li:before {
        content: " ";
        display: block;
        background: var(--bgColor);
        width: 10px;
        height: 10px;
        margin: 0 auto 10px;
        border-radius: 100%;
        border: 2px solid var(--borderColor);
        position: relative;
        z-index: 2;
    }

    ul.steps li.basic:before {
        background: var(--borderColor);
    }

    ul.steps li.active:before {
        border-color: var(--baseColor);
        color: var(--bgColor);
    }

    ul.steps li.basic.active:before {
        background: var(--baseColor);
    }

    ul.steps li.active {
        color: var(--baseColor);
        font-weight: bold;
    }

    ul.steps li:after {
        height: 2px;
        background: var(--borderColor);
        content: " ";
        display: block;
        position: absolute;
        width: 90px;
        top: 6px;
        left: -10px;
    }

    ul.steps li:first-child:after {
        width: 45px;
        left: 45px;
    }

    ul.steps li:last-child:after {
        width: 45px;
    }

    .helpTooltip {
        float: right;
        transform: translate(20px, -50px);
    }

    .step p {
        display: inline-block;
        margin: 5px 0 10px;
    }
    
    .step .fields p {
        margin: 5px 0 30px;
    }

    .steps a {
        display: inline-block;
        margin-left: 10px;
        font-weight: bold;
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
        width: 380px;
    }

    .extensionsList span.description {
        font-weight: normal;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        transform: translateY(3px);
    }

    h3 .helpTooltip, h4 .helpTooltip, label .helpTooltip {
        float: none;
        transform: none;
        margin-left: 5px;
    }

    .extension a.newTab {
        width: 11px;
        height: 11px;
        display: inline-block;
        transform: translate(4px, 1px);
    }

    .extension a.newTab:not(:hover) {
        filter: brightness(100);
    }

</style>
