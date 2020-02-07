var CreateCluster = Vue.component("create-cluster", {
	template: `
        <div id="create-cluster" class="form">

            <label for="namespace">K8S Namespace</label>
            <!-- <div class="list-create"> -->
                <select v-model="namespace" @change="showFields('.form > .hide')">
                    <option disabled value="">Choose a Namespace</option>
                    <option v-for="namespace in allNamespaces">{{ namespace }}</option>
                </select>
            <!-- <button>New Namespace</button> -->
            <!-- </div> -->
            
            <label for="name">Cluster Name</label>
            <input v-model="name">

            <div class="hide">
                <label>Restore from Backup</label>  
                <label for="restore" class="switch">Restore from Backup <input type="checkbox" id="restore" v-model="restore" data-switch="OFF"></label>

                <template v-if="restore">
                    <label for="restoreBackup">Backup Selection</label>
                    
                    <select v-model="restoreBackup">
                        <option disabled value="">Select a Backup</option>
                        <option v-for="backup in backups" v-if="( (backup.data.metadata.namespace == namespace) && (backup.data.status.phase === 'Completed') )" :value="backup.data.metadata.uid">{{ backup.name }} | {{ backup.data.status.time }}</option>
                    </select>
                </template>

                <label for="pgVersion">PostgreSQL Version</label>
                <select v-model="pgVersion">
                    <option disabled value="">Select PostgreSQL Version</option>
                    <option>11.6</option>
                    <option>12.1</option>
                </select>

                <label for="instances">Instances</label>
                <select v-model="instances">    
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

                <label for="resourceProfile">Instance Profile</label>  
                <div class="list-create">
                    <select v-model="resourceProfile" class="resourceProfile">
                        <option disabled value="">Select Instance Profile</option>
                        <option v-for="prof in profiles" v-if="prof.data.metadata.namespace == namespace" :value="prof.name">{{ prof.name }}</option>
                    </select>
                    <button @click="showFields('.profile')">New Instance Profile</button>
                </div>

                <div class="new profile hide">
                    <h3>Create New Instance Profile</h3>

                    <div class="fieldset">
                        <label for="profileNamespace">Profile Namespace</label>
                        <select v-model="profileNamespace">
                            <option disabled value="">Choose a Namespace</option>
                            <option v-for="namespace in allNamespaces">{{ namespace }}</option>
                        </select>

                        <label for="profileName">Profile Name</label>
                        <input v-model="profileName">

                        <div class="unit-select">
                            <label for="profileRAM">RAM</label>
                            <select v-model="profileRAM" class="size">    
                                <option disabled value="">RAM</option>
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

                            <select v-model="profileRAMUnit" class="unit">
                                <option disabled value="">Select Unit</option>
                                <option>Mi</option>
                                <option>Gi</option>
                                <option>Ti</option>
                                <option>Pi</option>
                                <option>Ei</option>
                                <option>Zi</option>
                                <option>Yi</option>        
                            </select>
                        </div>

                        <label for="profileCPU">CPU</label>
                        <select v-model="profileCPU">    
                            <option disabled value="">CPU</option>
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

                        <button @click="createProfile">Create Profile</button> <button @click="hideFields('.profile')" class="border">Cancel</button>
                    </div>
                </div>

                <label for="storageClass">Storage Class</label>
                <input v-model="storageClass">

                <div class="unit-select">
                    <label for="volumeSize">Volume Size</label>  
                    <input v-model="volumeSize" class="size">
                    <select v-model="volumeUnit" class="unit">
                        <option disabled value="">Select Unit</option>
                        <option>Mi</option>
                        <option>Gi</option>
                        <option>Ti</option>
                        <option>Pi</option>
                        <option>Ei</option>
                        <option>Zi</option>
                        <option>Yi</option>        
                    </select>
                </div>

                <label for="pgConfig">PostgreSQL Configuration</label>
                <div class="list-create">
                    <select v-model="pgConfig" class="pgConfig">
                        <option disabled value="">Select PostgreSQL Configuration</option>
                        <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.pgVersion == shortPGVersion) )">{{ conf.name }}</option>
                    </select>
                    <button @click="showFields('.pg-config')">New Configuration</button>
                </div>

                <div class="new pg-config hide">
                    <h3>Create New PostgreSQL Configuration</h3>

                    <div class="fieldset">
                        <label for="pgConfigNamespace">Configuration Namespace</label>
                        <select v-model="pgConfigNamespace">
                            <option disabled value="">Choose a Namespace</option>
                            <option v-for="namespace in allNamespaces">{{ namespace }}</option>
                        </select>

                        <label for="pgConfigName">Configuration Name</label>
                        <input v-model="pgConfigName">

                        <label for="pgConfigVersion">PostgreSQL Version</label>
                        <select v-model="pgConfigVersion">
                            <option disabled value="">Select PostgreSQL Version</option>
                            <option value="11">11.6</option>
                            <option value="12">12.1</option>
                        </select>

                        <label for="pgConfigParams">Parameters</label>
                        <textarea v-model="pgConfigParams" placeholder="parameter = value"></textarea>

                        <button @click="createPGConfig">Create Configuration</button> <button @click="hideFields('.pg-config')" class="border">Cancel</button>
                    </div>
                </div>

                <label>Enable Connection Pooling</label>  
                <label for="connPooling" class="switch">Connection Pooling <input type="checkbox" id="connPooling" v-model="connPooling" data-switch="OFF"></label>

                <template v-if="connPooling">
                    <label for="connectionPoolingConfig">Connection Pooling Configuration</label>
                    <div class="list-create">
                        <select v-model="connectionPoolingConfig" class="connectionPoolingConfig">
                            <option disabled value="">Select Configuration</option>
                            <option v-for="conf in connPoolConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                        </select>
                        <button @click="showFields('.pool-config')">New Configuration</button>
                    </div>

                    <div class="new pool-config hide">
                        <h3>Create New Connection Pooling Configuration</h3>

                        <div class="fieldset">
                            <label for="poolConfigNamespace">Configuration Namespace</label>
                            <select v-model="poolConfigNamespace">
                                <option disabled value="">Choose a Namespace</option>
                                <option v-for="namespace in allNamespaces">{{ namespace }}</option>
                            </select>

                            <label for="poolConfigName">Configuration Name</label>
                            <input v-model="poolConfigName">

                            <label for="poolConfigParams">Parameters</label>
                            <textarea v-model="poolConfigParams" placeholder="parameter = value"></textarea>

                            <button @click="createPoolConfig">Create Configuration</button> <button @click="hideFields('.pool-config')" class="border">Cancel</button>
                        </div>
                    </div>
                </template>

                <label>Enable Postgres Utils</label>  
                <label for="pgUtils" class="switch">Postgres Utils <input type="checkbox" id="pgUtils" v-model="pgUtils" data-switch="OFF"></label>

                <label for="backupConfig">Backup Configuration</label>
                <div class="list-create">
                    <select v-model="backupConfig" class="backupConfig">
                        <option disabled value="">Select Backup Configuration</option>
                        <option v-for="conf in backupConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                    </select>
                    <button @click="showFields('.backup-config')">New Configuration</button>
                </div>

                <div class="new backup-config hide">
                    <h3>Create New Backup Configuration</h3>

                    <div class="fieldset">
                        <label for="backupConfigNamespace">Configuration Namespace</label>
                        <select v-model="backupConfigNamespace">
                            <option disabled value="">Choose a Namespace</option>
                            <option v-for="namespace in allNamespaces">{{ namespace }}</option>
                        </select>

                        <label for="backupConfigName">Configuration Name</label>
                        <input v-model="backupConfigName">

                        <label for="backupConfigCompressionMethod">Compression Method</label>
                        <input v-model="backupConfigCompressionMethod">

                        <label for="backupConfigFullSchedule">Backup Schedule</label>
                        <input v-model="backupConfigFullSchedule">

                        <label for="backupConfigFullWindow">Full Window</label>
                        <input v-model="backupConfigFullWindow" value="">

                        <label for="backupConfigRetention">Retention Limit</label>
                        <input v-model="backupConfigRetention" value="">

                        <div class="unit-select">
                            <label for="backupConfigTarSizeThreshold">Tar Size Threshold</label>  
                            <input v-model="backupConfigTarSizeThreshold" class="size" value="">
                            <select v-model="backupConfigTarSizeThresholdUnit" class="unit">
                                <option disabled value="">Select Unit</option>
                                <option value="1024">Ki</option>
                                <option value="1048576">Mi</option>
                                <option value="1073741824">Gi</option>
                                <option value="1099511627776">Ti</option>
                                <option value="1125899906842624">Pi</option>
                                <option value="1152921504606846976">Ei</option>
                                <option value="1180591620717411303424">Zi</option>
                                <option value="1208925819614629174706176">Yi</option>        
                            </select>
                        </div>
                        
                        <label for="backupConfigUploadDiskConcurrency">Upload Disk Concurrency</label>
                        <input v-model="backupConfigUploadDiskConcurrency" value="">

                        <label for="backupConfigStorageType">Storage Type</label>
                        <select v-model="backupConfigStorageType">
                            <option disabled value="">Select Storage Type</option>
                            <option value="s3">Amazon S3</option>
                            <option value="gcs">Google Storage</option>
                            <option value="azureblob">Microsoft Azure</option>
                        </select>

                        <div class="fieldset" v-if="backupConfigStorageType === 's3'">
                            <label for="backupS3Prefix">Prefix</label>
                            <input v-model="backupS3Prefix">

                            <label for="backupS3AccessKeyName">Access Key Name</label>
                            <input v-model="backupS3AccessKeyName">

                            <label for="backupS3AccessKey">Access Key</label>
                            <input v-model="backupS3AccessKey">

                            <label for="backupS3SecretKeyName">Secret Key Name</label>
                            <input v-model="backupS3SecretKeyName">

                            <label for="backupS3SecretKey">Secret Key</label>
                            <input v-model="backupS3SecretKey">

                            <label for="backupS3Region">Region</label>
                            <input v-model="backupS3Region">

                            <label for="backupS3Endpoint">Endpoint</label>
                            <input v-model="backupS3Endpoint">

                            <label for="backupS3ForcePathStyle" class="switch">Force Path Style <input type="checkbox" id="backupS3ForcePathStyle" v-model="backupS3ForcePathStyle" data-switch="OFF"></label>

                            <label for="backupS3StorageClass">Storage Class</label>
                            <input v-model="backupS3StorageClass">

                            <label for="backupS3sse">SSE</label>
                            <input v-model="backupS3sse">

                            <label for="backupS3sseKmsId">SSE-KMS ID</label>
                            <input v-model="backupS3sseKmsId">

                            <label for="backupS3cseKmsId">CSE-KMS ID</label>
                            <input v-model="backupS3cseKmsId">

                            <label for="backupS3cseKmsRegion">CSE-KMS Region</label>
                            <input v-model="backupS3cseKmsRegion">
                        </div>

                        <div class="fieldset" v-if="backupConfigStorageType === 'gcs'">
                            <label for="backupGCSPrefix">Prefix</label>
                            <input v-model="backupGCSPrefix">

                            <label for="backupGCSKeyName">Service Account Key Name</label>
                            <input v-model="backupGCSKeyName">

                            <label for="backupGCSKey">Service Account Key</label>
                            <input v-model="backupGCSKey">
                        </div>

                        <div class="fieldset" v-if="backupConfigStorageType === 'azureblob'">
                            <label for="backupAzurePrefix">Prefix</label>
                            <input v-model="backupAzurePrefix">

                            <label for="backupAzureAccountName">Account Name</label>
                            <input v-model="backupAzureAccountName">

                            <label for="backupAzureAccountKey">Account Key</label>
                            <input v-model="backupAzureAccountKey">

                            <label for="backupAzureAccessKeyName">Access Key Name</label>
                            <input v-model="backupAzureAccessKeyName">

                            <label for="backupAzureAccessKey">Access Key</label>
                            <input v-model="backupAzureAccessKey"

                            <label for="backupAzureBufferSize">Buffer Size</label>
                            <input v-model="backupAzureBufferSize">

                            <label for="backupAzureMaxBuffers">Max Buffers</label>
                            <input v-model="backupAzureMaxBuffers">
                        </div>

                        <button @click="createBackupConfig">Create Configuration</button> <button @click="hideFields('.backup-config')" class="border">Cancel</button>
                    </div>
                </div>

                <button @click="createCluster">Create Cluster</button> <button @click="cancel" class="border">Cancel</button>
            </div>                
		</div>`,
	data: function() {
		return {
            name: '',
            namespace: '',
            pgVersion: '',
            instances: '',
            resourceProfile: '',
            pgConfig: '',
            storageClass: '',
            volumeSize: '',
            volumeUnit: '',
            connPooling: false,
            connectionPoolingConfig: '',
            restore: false,
            restoreBackup: '',
            backupConfig: '',
            pgUtils: false,
            profileName: '',
            profileNamespace: '',
            profileCPU: '',
            profileRAM: '',
            profileRAMUnit: '',
            pgConfigName: '',
            pgConfigNamespace: '',
            pgConfigParams: '',
            pgConfigVersion: '',
            poolConfigName: '',
            poolConfigNamespace: '',
            poolConfigParams: '',
            backupConfigName: '',
            backupConfigNamespace: '',
            backupConfigCompressionMethod: 'lz4',
            backupConfigFullSchedule: '*/1 * * * *',
            backupConfigFullWindow: 1,
            backupConfigRetention: 5,
            backupConfigTarSizeThreshold: 1,
            backupConfigTarSizeThresholdUnit: 1073741824,
            backupConfigUploadDiskConcurrency: 1,
            backupConfigStorageType: '',
            backupS3Prefix: '',
            backupS3AccessKeyName: '',
            backupS3AccessKey: '',
            backupS3SecretKeyName: '',
            backupS3SecretKey: '',
            backupS3Region: '',
            backupS3Endpoint: '',
            backupS3ForcePathStyle: '',
            backupS3StorageClass: '',
            backupS3sse: '',
            backupS3sseKmsId: '',
            backupS3cseKmsId: '',
            backupS3cseKmsRegion: '',
            backupGCSPrefix: '',
            backupGCSKeyName: '',
            backupGCSKey: '',
            backupAzurePrefix: '',
            backupAzureAccountName: '',
            backupAzureAccountKey: '',
            backupAzureAccessKeyName: '',
            backupAzureAccessKey: '',
            backupAzureBufferSize: '',
            backupAzureMaxBuffers: '',
		}
	},
	computed: {

		currentNamespace () {
			return store.state.currentNamespace
        },
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
        shortPGVersion () {
            return this.pgVersion.substring(0,2)
        }

    },
    methods: {

        createCluster: function(e) {
            e.preventDefault();

            let sidecars = [];
            let fromBackup = {};

            if(this.connPooling)
                sidecars.push('connection-pooling');

            if(this.pgUtils)
                sidecars.push('postgres-util');

            var cluster = { 
                "metadata": {
                    "name": this.name,
                    "namespace": this.namespace
                },
                "spec": {
                    "instances": this.instances,
                    "pgVersion": this.pgVersion,
                    "pgConfig": this.pgConfig,
                    "resourceProfile": this.resourceProfile,
                    "restore": {
                        "fromBackup": this.restoreBackup
                    },
                    "backupConfig": this.backupConfig,
                    "connectionPoolingConfig": this.connectionPoolingConfig,
                    "volumeSize": this.volumeSize+this.volumeUnit,
                    "storageClass": this.storageClass,
                    //"prometheusAutobind": "true",
                    "sidecars": sidecars
                }
            }

            const res = axios
            .post(
                apiURL+'cluster/', 
                cluster 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> created successfully', 'message');

                store.commit('updateClusters', { 
                    name: cluster.metadata.name,
                    data: item
                });
            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        createProfile: function(e) {
            e.preventDefault();

            var profile = { 
                "metadata": {
                    "name": this.profileName,
                    "namespace": this.profileNamespace
                },
                "spec": {
                    "cpu": this.profileCPU,
                    "memory": this.profileRAM+this.profileRAMUnit,
                }
            }

            const res = axios
            .post(
                apiURL+'profile/', 
                profile 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Profile <strong>"'+profile.metadata.name+'"</strong> created successfully', 'message');

                store.commit('updateProfiles', { 
                    name: profile.metadata.name,
                    data: profile
                });

                setTimeout(function(){
                    $(".resourceProfile").val(profile.metadata.name).change().focus();
                    hideFields('.profile');
                },1000);                

            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        createPGConfig: function(e) {
            e.preventDefault();

            var config = { 
                "metadata": {
                    "name": this.pgConfigName,
                    "namespace": this.pgConfigNamespace
                },
                "spec": {
                    "pgVersion": this.pgConfigVersion,
                    "postgresql.conf": getJSON(this.pgConfigParams)
                }
            }

            const res = axios
            .post(
                apiURL+'pgconfig/', 
                config 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Postgres configuration <strong>"'+config.metadata.name+'"</strong> created successfully', 'message');

                store.commit('updatePGConfig', { 
                    name: config.metadata.name,
                    data: config
                });

                setTimeout(function(){
                    $(".pgConfig").val(config.metadata.name).change().focus();
                    hideFields('.pg-config');
                },1000);                
            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        createPoolConfig: function(e) {
            e.preventDefault();

            var config = { 
                "metadata": {
                    "name": this.poolConfigName,
                    "namespace": this.poolConfigNamespace
                },
                "spec": {
                    "pgbouncer.ini": getJSON(this.poolConfigParams)
                }
            }

            const res = axios
            .post(
                apiURL+'connpoolconfig/', 
                config 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Connection pooling configuration <strong>"'+config.metadata.name+'"</strong> created successfully', 'message');

                store.commit('updatePoolConfig', { 
                    name: config.metadata.name,
                    data: config
                });

                setTimeout(function(){
                    $(".connectionPoolingConfig").val(config.metadata.name).change().focus();
                    hideFields('.pool-config');
                },1000);               
            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        createBackupConfig: function(e) {
            e.preventDefault();

            let storage = {};

            switch(this.backupConfigStorageType) {
                
                case 's3':
                    storage['s3'] = {
                        "credentials": {
                            "accessKey": {
                                "key": this.backupS3AccessKey,
                                "name": this.backupS3AccessKeyName
                            },
                            "secretKey": {
                                "key": this.backupS3SecretKey,
                                "name": this.backupS3SecretKeyName
                            }
                        },
                        "endpoint": this.backupS3Endpoint,
                        "forcePathStyle": this.backupS3ForcePathStyle,
                        "prefix": this.backupS3Prefix,
                        "region": this.backupS3Region,
                        "storageClass": this.backupS3StorageClass,
                        "sse": this.backupS3sse,
                        "sseKmsId": this.backupS3sseKmsId,
                        "cseKmsId": this.backupS3cseKmsId,
                        "cseKmsRegion": this.backupS3cseKmsRegion
                    };
                    storage['type'] = 's3';
                    break;

                case 'gcs':
                    storage['gcs'] = {
                        "prefix": this.backupGCSPrefix,
                        "credentials": {
                            "serviceAccountJsonKey": {
                                name: this.backupGCSKeyName,
                                key: this.backupGCSKey
                            }
                        }
                    }
                    storage['type'] = 'gcs';

                    break;

                case 'azureblob':
                    storage['azureblob'] = {
                        "prefix": this.backupAzurePrefix,
                        "credentials": {
                            "account": {
                                "name": this.backupAzureAccountName,
                                "key": this.backupAzureAccountKey
                            },
                            "accessKey": {
                                "key": this.backupAzureAccessKey,
                                "name": this.backupAzureAccessKeyName
                            }
                        },
                        "bufferSize": this.backupAzureBufferSize,
                        "maxBuffers": this.backupAzureMaxBuffers,
                    }
                    storage['type'] = 'azureblob';

                    break;

            }

            let config = { 
                "metadata": {
                    "name": this.backupConfigName,
                    "namespace": this.backupConfigNamespace
                },
                "spec": {
                    "compressionMethod": this.backupConfigCompressionMethod,
                    "fullSchedule": this.backupConfigFullSchedule,
                    "fullWindow": this.backupConfigFullWindow,
                    "retention": this.backupConfigRetention,
                    "tarSizeThreshold": this.backupConfigTarSizeThreshold * this.backupConfigTarSizeThresholdUnit,
                    "storage": storage
                }
            }

            const res = axios
            .post(
                apiURL+'backupconfig/', 
                config 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Backup configuration <strong>"'+config.metadata.name+'"</strong> created successfully', 'message');

                store.commit('updateBackupConfig', { 
                    name: config.metadata.name,
                    data: config
                });

                setTimeout(function(){
                    $(".backupConfig").val(config.metadata.name).change().focus();
                    hideFields('.backup-config');
                },1000);
            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        cancel: function() {
            router.push('/overview/'+store.state.currentNamespace);
        },

        showFields: function( fields ) {
            $(fields).slideDown();
        },

        hideFields: function( fields ) {
            $(fields).slideUp();
        }

    }
})