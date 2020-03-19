var CreateCluster = Vue.component("create-cluster", {
    template: `
        <form id="create-cluster">
            <header>
                <ul class="breadcrumbs">
                    <li class="namespace">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                        <router-link :to="'/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
                    </li>
                    <li class="action">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
                        {{ $route.params.action }} cluster
                    </li>
                    <li v-if="editMode">
                        {{ $route.params.name }}
                    </li>
                </ul>
            </header>
            
            <div class="form">
                
                <div class="header">
                    <h2>Cluster Details</h2>
                    <label for="advancedMode" :class="(advancedMode) ? 'active' : ''">
                        <input v-model="advancedMode" type="checkbox" id="advancedMode" name="advancedMode" />
                        <span>Advanced</span>
                    </label>
                </div>

                <label for="name">Cluster Name <span class="req">*</span></label>
                <input v-model="name" :disabled="(editMode)" required>

                <div>
                    
                    <label for="pgVersion">Postgres Version <span class="req">*</span></label>
                    <select v-model="pgVersion" :disabled="(editMode)" required>
                        <option disabled value="">Select Version</option>
                        <option value="12.2" selected>Latest</option>
                        <option value="12">12</option>
                        <option value="11">11</option>
                        <option value="12.2">12.2</option>
                        <option value="11.7">11.7</option>
                    </select>

                    <div class="row-50">
                        <div class="col">
                            <label for="instances">Number of Instances <span class="req">*</span></label>
                            <select v-model="instances" required>    
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
                        </div>
                        <div class="col">
                            <label for="resourceProfile">Instance Profile</label>  
                            <select v-model="resourceProfile" class="resourceProfile" :disabled="(editMode)">
                                <option selected value="">Default (Cores: 1, RAM: 2GiB)</option>
                                <option v-for="prof in profiles" v-if="prof.data.metadata.namespace == namespace" :value="prof.name">{{ prof.name }} (Cores: {{ prof.data.spec.cpu }}, RAM: {{ prof.data.spec.memory }}B)</option>
                            </select>
                        </div>
                    </div>

                    <div class="unit-select">
                        <label for="volumeSize">Volume Size <span class="req">*</span></label>  
                        <input v-model="volumeSize" class="size" required  :disabled="(editMode)" >
                        <select v-model="volumeUnit" class="unit" required :disabled="(editMode)" >
                            <option disabled value="">Select Unit</option>
                            <option value="Mi">MiB</option>
                            <option value="Gi">GiB</option>
                            <option value="Ti">TiB</option>   
                        </select>
                    </div>

                    <fieldset v-if="(!editMode && advancedMode)">
                        <label>Cluster Data Initialization</label>  
                        <label for="restore" class="switch yes-no">Restore from an existing Backup <input type="checkbox" id="restore" v-model="restore" data-switch="OFF"></label>

                        <label for="restoreBackup">Backup Selection</label>
                        <select v-model="restoreBackup" :disabled="(!restore)">
                            <option disabled value="">Select a Backup</option>
                            <option v-for="backup in backups" v-if="( (backup.data.metadata.namespace == namespace) && (backup.data.status.phase === 'Completed') )" :value="backup.data.metadata.uid">{{ backup.name }} | {{ backup.data.status.time }}</option>
                        </select>
                    </fieldset>

                    <template v-if="advancedMode">
                        <label for="storageClass">Storage Class</label>
                        <select v-model="storageClass" :disabled="(editMode)">
                            <option value="">Select Storage Class</option>
                            <option v-for="sClass in storageClasses">{{ sClass }}</option>
                        </select>
                        
                        <label for="pgConfig">PostgreSQL Configuration</label>
                        <select v-model="pgConfig" class="pgConfig" :disabled="(editMode)" >
                            <option value="" selected>Default</option>
                            <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.pgVersion == shortPGVersion) )">{{ conf.name }}</option>
                        </select>

                        <fieldset>
                            <label>Enable Connection Pooling</label>  
                            <label for="connPooling" class="switch" :disabled="(editMode)">Connection Pooling <input type="checkbox" id="connPooling" v-model="connPooling" data-switch="OFF" :disabled="(editMode)" ></label>
                            
                            <label for="connectionPoolingConfig">Connection Pooling Configuration</label>
                            <select v-model="connectionPoolingConfig" class="connectionPoolingConfig" :disabled="(editMode || !connPooling)" >
                                <option value="" selected>Default</option>
                                <option v-for="conf in connPoolConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                            </select>
                        </fieldset>

                        <!--<label>Enable Postgres Utils</label>  
                        <label for="pgUtils" class="switch">Postgres Utils <input type="checkbox" id="pgUtils" v-model="pgUtils" data-switch="OFF"></label>-->

                        <label for="backupConfig">Backup Configuration</label>
                        <select v-model="backupConfig" class="backupConfig">
                            <option disabled value="">Select Backup Configuration</option>
                            <option v-for="conf in backupConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                        </select>

                        <label>Prometheus Autobind</label>  
                        <label for="prometheusAutobind" class="switch">Prometheus Autobind <input type="checkbox" id="prometheusAutobind" v-model="prometheusAutobind" data-switch="OFF"></label>

                        <fieldset>
                            <div class="header">
                                <h3>Non Production</h3>  
                            </div>
                            <label for="disableClusterPodAntiAffinity" class="switch yes-no">disableClusterPodAntiAffinity <input type="checkbox" id="disableClusterPodAntiAffinity" v-model="disableClusterPodAntiAffinity" data-switch="NO"></label>
                        </fieldset>
                    </template>
                    
                    <template v-if="editMode">
                        <button @click="createCluster" type="submit">Update Cluster</button>
                    </template>
                    <template v-else>
                        <button @click="createCluster" type="submit">Create Cluster</button>
                    </template>

                    <button @click="cancel" class="border">Cancel</button>
                </div>   
            </div>             
		</form>`,
	data: function() {

        if (vm.$route.params.action == 'create') {
            return {
                advancedMode: false,
                cluster: {},
                editMode: false,
                name: '',
                namespace: store.state.currentNamespace,
                pgVersion: '',
                instances: '',
                resourceProfile: '',
                pgConfig: '',
                storageClass: '',
                volumeSize: '',
                volumeUnit: '',
                connPooling: true,
                connectionPoolingConfig: '',
                restore: false,
                restoreBackup: '',
                backupConfig: '',
                prometheusAutobind: false,
                disableClusterPodAntiAffinity: false,
                pgUtils: true,
            }
        } else if (vm.$route.params.action == 'edit') {

            let volumeSize = store.state.currentCluster.spec.volumeSize.match(/\d+/g);
            let volumeUnit = store.state.currentCluster.spec.volumeSize.match(/[a-zA-Z]+/g);

            return {
                advancedMode: false,
                cluster: {},
                editMode: true,
                name: vm.$route.params.name,
                namespace: store.state.currentNamespace,
                pgVersion: store.state.currentCluster.spec.pgVersion,
                instances: store.state.currentCluster.spec.instances,
                resourceProfile: store.state.currentCluster.spec.resourceProfile,
                pgConfig: store.state.currentCluster.spec.pgConfig,
                storageClass: store.state.currentCluster.spec.storageClass,
                volumeSize: volumeSize,
                volumeUnit: ''+volumeUnit,
                connPooling: (store.state.currentCluster.spec.connectionPoolingConfig !== undefined),
                connectionPoolingConfig: (store.state.currentCluster.spec.connectionPoolingConfig !== undefined) ? store.state.currentCluster.spec.connectionPoolingConfig : '',
                restore: false,
                restoreBackup: '',
                backupConfig: (store.state.currentCluster.spec.backupConfig !== undefined) ? store.state.currentCluster.spec.backupConfig : '',
                prometheusAutobind:  (store.state.currentCluster.spec.prometheusAutobind !== undefined) ? store.state.currentCluster.spec.prometheusAutobind : false,
                disableClusterPodAntiAffinity: (store.state.currentCluster.spec.disableClusterPodAntiAffinity !== undefined) ? store.state.currentCluster.spec.disableClusterPodAntiAffinity : false,
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
        },
        storageClasses() {
            return store.state.storageClasses
        }

    },

    methods: {

        createCluster: function(e) {
            //e.preventDefault();

            let isValid = true;
            
            $('input:required, select:required').each(function() {
                if ($(this).val() === '') {
                    isValid = false;
                    return false;
                }
                    
            });

            if(isValid) {
                let sidecars = [];
                let fromBackup = {};
                let nonProduction = {}

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
                        ...(this.pgConfig.length && ( {"pgConfig": this.pgConfig }) ),
                        ...(this.resourceProfile.length && ( {"resourceProfile": this.resourceProfile }) ),
                        ...(this.restore && ( {"restore": { "fromBackup": this.restoreBackup } }) ),
                        ...(this.backupConfig.length && ( {"backupConfig": this.backupConfig }) ),
                        ...(this.connPooling && ( {"connectionPoolingConfig": this.connectionPoolingConfig }) ),
                        "volumeSize": this.volumeSize+this.volumeUnit,
                        ...( ( (this.storageClass !== undefined) && (this.storageClass.length ) ) && ( {"storageClass": this.storageClass }) ),
                        ...(this.prometheusAutobind && ( {"prometheusAutobind": this.prometheusAutobind }) ),
                        ...(this.disableClusterPodAntiAffinity && ( {"nonProduction": { "disableClusterPodAntiAffinity": this.disableClusterPodAntiAffinity } }) ),
                        "sidecars": sidecars
                    }
                }  
                
                console.log(cluster);

                if(this.editMode) {
                    const res = axios
                    .put(
                        apiURL+'cluster/', 
                        cluster 
                    )
                    .then(function (response) {
                        console.log("GOOD");
                        notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> updated successfully', 'message');

                        vm.fetchAPI();
                        router.push('/status/'+cluster.metadata.namespace+'/'+cluster.metadata.name);
                        
                    })
                    .catch(function (error) {
                        console.log(error.response);
                        notify(error.response.data.message,'error');
                    });
                } else {
                    const res = axios
                    .post(
                        apiURL+'cluster/', 
                        cluster 
                    )
                    .then(function (response) {
                        console.log("GOOD");
                        notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> created successfully', 'message');

                        vm.fetchAPI();
                        router.push('/status/'+cluster.metadata.namespace+'/'+cluster.metadata.name);
                        
                        /* store.commit('updateClusters', { 
                            name: cluster.metadata.name,
                            data: item
                        });
 */
                    })
                    .catch(function (error) {
                        console.log(error.response);
                        notify(error.response.data.message,'error');
                    });
                }

            }

        },

        cancel: function() {
            if(this.editMode)
                router.push('/status/'+store.state.currentNamespace+'/'+store.state.currentCluster.name);
            else
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