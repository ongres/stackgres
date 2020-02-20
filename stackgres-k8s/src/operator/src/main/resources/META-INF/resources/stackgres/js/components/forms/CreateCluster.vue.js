var CreateCluster = Vue.component("create-cluster", {
    template: `
        <div id="create-cluster" class="form">
            <header>
                <h2 class="title">Create New Cluster</h2>
            </header>
            
            <label for="namespace">K8S Namespace</label>
            <!-- <div class="list-create"> -->
                <select v-model="namespace" @change="showFields('.form > .hide')">
                    <option disabled value="">Choose a Namespace</option>
                    <option v-for="namespace in allNamespaces" :selected="(namespace === currentNamespace)">{{ namespace }}</option>
                </select>
            <!-- <button>New Namespace</button> -->
            <!-- </div> -->
            
            <label for="name">Cluster Name</label>
            <input v-model="name">

            <div :class="(editMode) ? '' : 'hide'">
                
                <template v-if="!editMode">
                    <label>Restore from Backup</label>  
                    <label for="restore" class="switch">Restore from Backup <input type="checkbox" id="restore" v-model="restore" data-switch="OFF"></label>
                </template>

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
                <select v-model="resourceProfile" class="resourceProfile">
                    <option disabled value="">Select Instance Profile</option>
                    <option v-for="prof in profiles" v-if="prof.data.metadata.namespace == namespace" :value="prof.name">{{ prof.name }}</option>
                </select>

                <template v-if="!editMode">
                    <label for="storageClass">Storage Class</label>
                    <input v-model="storageClass">
                </template>

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
                <select v-model="pgConfig" class="pgConfig">
                    <option disabled value="">Select PostgreSQL Configuration</option>
                    <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.pgVersion == shortPGVersion) )">{{ conf.name }}</option>
                </select>

                <label>Enable Connection Pooling</label>  
                <label for="connPooling" class="switch">Connection Pooling <input type="checkbox" id="connPooling" v-model="connPooling" data-switch="OFF"></label>

                <template v-if="connPooling">
                    <label for="connectionPoolingConfig">Connection Pooling Configuration</label>
                    <select v-model="connectionPoolingConfig" class="connectionPoolingConfig">
                        <option disabled value="">Select Configuration</option>
                        <option v-for="conf in connPoolConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                    </select>
                </template>

                <!--<label>Enable Postgres Utils</label>  
                <label for="pgUtils" class="switch">Postgres Utils <input type="checkbox" id="pgUtils" v-model="pgUtils" data-switch="OFF"></label>-->

                <label for="backupConfig">Backup Configuration</label>
                <select v-model="backupConfig" class="backupConfig">
                    <option disabled value="">Select Backup Configuration</option>
                    <option v-for="conf in backupConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                </select>
                
                <template v-if="editMode">
                    <button @click="updateCluster">Update Cluster</button>
                </template>
                <template v-else>
                    <button @click="createCluster">Create Cluster</button>
                </template>

                <button @click="cancel" class="border">Cancel</button>
            </div>                
		</div>`,
	data: function() {

        if (vm.$route.params.action == 'create') {
            return {
                cluster: {},
                editMode: false,
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
                pgUtils: true,
            }
        } else if (vm.$route.params.action == 'edit') {

            let volumeSize = store.state.currentCluster.spec.volumeSize.match(/\d+/g);
            let volumeUnit = store.state.currentCluster.spec.volumeSize.match(/[a-zA-Z]+/g);

            return {
                cluster: {},
                editMode: true,
                name: vm.$route.params.name,
                namespace: vm.$route.params.namespace,
                pgVersion: store.state.currentCluster.spec.pgVersion,
                instances: store.state.currentCluster.spec.instances,
                resourceProfile: store.state.currentCluster.spec.resourceProfile,
                pgConfig: store.state.currentCluster.spec.pgConfig,
                storageClass: store.state.currentCluster.spec.storageClass,
                volumeSize: volumeSize,
                volumeUnit: ''+volumeUnit,
                connPooling: store.state.currentCluster.spec.connectionPoolingConfig.length,
                connectionPoolingConfig: store.state.currentCluster.spec.connectionPoolingConfig,
                restore: false,
                restoreBackup: '',
                backupConfig: store.state.currentCluster.spec.backupConfig,
                pgUtils: true,
            }
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
        },
        currentCluster() {
            return store.state.currentCluster
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

        updateCluster: function(e) {
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
            .put(
                apiURL+'cluster/', 
                cluster 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> updated successfully', 'message');
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