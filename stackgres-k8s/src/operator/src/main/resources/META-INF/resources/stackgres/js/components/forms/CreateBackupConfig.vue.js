var CreateBackupConfig = Vue.component("create-backup-config", {
    template: `
        <div id="create-backup-config" class="form">
            <header>
                <h2 class="title">{{ $route.params.action }} Backup Configuration</h2>
            </header>
            
            <label for="backupConfigNamespace">Configuration Namespace</label>
            <select v-model="backupConfigNamespace" :disabled="(editMode)">
                <option disabled value="">Choose a Namespace</option>
                <option v-for="namespace in allNamespaces">{{ namespace }}</option>
            </select>

            <label for="backupConfigName">Configuration Name</label>
            <input v-model="backupConfigName" :disabled="(editMode)">

            <label for="backupConfigCompressionMethod">Compression Method</label>
            <select v-model="backupConfigCompressionMethod">
                <option disabled value="">Select a method</option>
                <option value="lz4">LZ4</option>
                <option value="lzma">LZMA</option>
                <option value="brotli">Brotli</option>
            </select>

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
            <select v-model="backupConfigStorageType" :disabled="(editMode)">
                <option disabled value="">Select Storage Type</option>
                <option value="s3">Amazon S3</option>
                <option value="gcs">Google Storage</option>
                <option value="azureblob">Microsoft Azure</option>
            </select>

            <fieldset class="fieldset" v-if="backupConfigStorageType === 's3'" :disabled="(editMode)">
                <h3>Amazon S3 Configuration</h3>

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
            </fieldset>

            <fieldset class="fieldset" v-if="backupConfigStorageType === 'gcs'" :disabled="(editMode)">
                <h3>Google Cloud Storage Configuration</h3>
                
                <label for="backupGCSPrefix">Prefix</label>
                <input v-model="backupGCSPrefix">

                <label for="backupGCSKeyName">Service Account Key Name</label>
                <input v-model="backupGCSKeyName">

                <label for="backupGCSKey">Service Account Key</label>
                <input v-model="backupGCSKey">
            </fieldset>

            <fieldset class="fieldset" v-if="backupConfigStorageType === 'azureblob'" :disabled="(editMode)">
                <h3>Microsoft Azure Configuration</h3>

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
            </fieldset>

            <button @click="createBackupConfig">Create Configuration</button> <button @click="cancel" class="border">Cancel</button>
		</div>`,
	data: function() {
        
        if (vm.$route.params.action == 'create') {
            
            return {
                editMode: false,
                backupConfigName: vm.$route.params.name,
                backupConfigNamespace: vm.$route.params.namespace,
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
        } else if (vm.$route.params.action == 'edit') {
            
            let config = {};
            
            store.state.backupConfig.forEach(function( conf ){
                if( (conf.data.metadata.name === vm.$route.params.name) && (conf.data.metadata.namespace === vm.$route.params.namespace) ) {
                    config = conf;
                    return false;
                }
            });

            
            let tresholdSize = formatBytes(config.data.spec.tarSizeThreshold);
            let tresholdUnit = '';            
            let unitSizes = {
                "Ki": 1024, 
                "Mi": 1048576,
                "Gi": 1073741824,
                "Ti": 1099511627776,
                "Pi": 1125899906842624,
                "Ei": 1152921504606846976,
                "Zi": 1180591620717411303424,
                "Yi": 1208925819614629174706176
            };
            
            $.each( unitSizes, function( index, value ){
                if( tresholdSize.match(/[a-zA-Z]+/g)[0] === index ) {
                    tresholdUnit = value;
                    return false;
                }
            });
            
            // console.log(tresholdSize);
            // console.log(tresholdSize.match(/[a-zA-Z]+/g));
            // console.log(tresholdSize.match(/[a-zA-Z]+/g)[0]);

            return {
                editMode: true,
                backupConfigName: vm.$route.params.name,
                backupConfigNamespace: vm.$route.params.namespace,
                backupConfigCompressionMethod: config.data.spec.compressionMethod,
                backupConfigFullSchedule: config.data.spec.fullSchedule,
                backupConfigFullWindow: config.data.spec.fullWindow,
                backupConfigRetention: config.data.spec.retention,
                backupConfigTarSizeThreshold: tresholdSize.match(/\d+/g),
                backupConfigTarSizeThresholdUnit: tresholdUnit,
                backupConfigUploadDiskConcurrency: config.data.spec.uploadDiskConcurrency,
                backupConfigStorageType: config.data.spec.storage.type,
                backupS3Prefix: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.prefix : '',
                backupS3AccessKeyName: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.credentials.accessKey.name : '',
                backupS3AccessKey: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.credentials.accessKey.key : '',
                backupS3SecretKeyName: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.credentials.secretKey.name : '',
                backupS3SecretKey: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.credentials.secretKey.key : '',
                backupS3Region: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.credentials.region : '',
                backupS3Endpoint: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.credentials.endpoint : '',
                backupS3ForcePathStyle: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.credentials.forcePathStyle : '',
                backupS3StorageClass: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.storageClass : '',
                backupS3sse: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.sse : '',
                backupS3sseKmsId: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.sseKmsId : '',
                backupS3cseKmsId: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.cseKmsId : '',
                backupS3cseKmsRegion: ( config.data.spec.storage.type === 's3' ) ? config.data.spec.storage.s3.cseKmsRegion : '',
                backupGCSPrefix:  ( config.data.spec.storage.type === 'gcs' ) ? config.data.spec.storage.gcs.prefix : '',
                backupGCSKeyName: ( config.data.spec.storage.type === 'gcs' ) ? config.data.spec.storage.gcs.credentials.serviceAccountJsonKey.name : '',
                backupGCSKey: ( config.data.spec.storage.type === 'gcs' ) ? config.data.spec.storage.gcs.credentials.serviceAccountJsonKey.key : '',
                backupAzurePrefix: ( config.data.spec.storage.type === 'azureblob' ) ? config.data.spec.storage.azureblob.prefix : '',
                backupAzureAccountName: ( config.data.spec.storage.type === 'azureblob' ) ? config.data.spec.storage.azureblob.credentials.account.name : '',
                backupAzureAccountKey: ( config.data.spec.storage.type === 'azureblob' ) ? config.data.spec.storage.azureblob.credentials.account.key : '',
                backupAzureAccessKeyName: ( config.data.spec.storage.type === 'azureblob' ) ? config.data.spec.storage.azureblob.credentials.accessKey.name : '',
                backupAzureAccessKey: ( config.data.spec.storage.type === 'azureblob' ) ? config.data.spec.storage.azureblob.credentials.accessKey.key : '',
                backupAzureBufferSize: ( config.data.spec.storage.type === 'azureblob' ) ? config.data.spec.storage.azureblob.bufferSize : '',
                backupAzureMaxBuffers: ( config.data.spec.storage.type === 'azureblob' ) ? config.data.spec.storage.azureblob.maxBuffers : '',
            }
        }
	},
	computed: {
        allNamespaces () {
            return store.state.allNamespaces
        }
    },
    methods: {

        
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

        updateBackupConfig: function(e) {
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
            .put(
                apiURL+'backupconfig/', 
                config 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Backup configuration <strong>"'+config.metadata.name+'"</strong> updated successfully', 'message');
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