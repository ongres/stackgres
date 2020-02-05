var CreateCluster = Vue.component("create-cluster", {
	template: `
        <div id="create-cluster" class="form">

            <label for="namespace">K8S Namespace</label>
            <div class="list-create">
                <select v-model="namespace" @change="showFields()">
                    <option disabled value="">Choose a Namespace</option>
                    <option v-for="namespace in allNamespaces">{{ namespace }}</option>
                </select>
                <button>New Namespace</button>
            </div>
            
            <label for="name">Cluster Name</label>
            <input v-model="name">

            <div class="hide">
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
                    <select v-model="resourceProfile">
                        <option disabled value="">Select Instance Profile</option>
                        <option v-for="prof in profiles" v-if="prof.data.metadata.namespace == namespace">{{ prof.name }}</option>
                    </select>
                    <button>New Instance Profile</button>
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
                    <select v-model="pgConfig">
                        <option disabled value="">Select PostgreSQL Configuration</option>
                        <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.pgVersion == shortPGVersion) )">{{ conf.name }}</option>
                    </select>
                    <button>New Configuration</button>
                </div>

                <label>Enable Connection Pooling</label>  
                <label for="connPooling" class="switch">Connection Pooling <input type="checkbox" id="connPooling" v-model="connPooling" data-switch="OFF"></label>

                <template v-if="connPooling">
                    <label for="connectionPoolingConfig">Connection Pooling Configuration</label>
                    <div class="list-create">
                        <select v-model="connectionPoolingConfig">
                            <option disabled value="">Select Configuration</option>
                            <option v-for="conf in connPoolConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                        </select>
                        <button>New Configuration</button>
                    </div>
                </template>

                <label for="backupConfig">Backup Configuration</label>
                <div class="list-create">
                    <select v-model="backupConfig">
                        <option disabled value="">Select Backup Configuration</option>
                        <option v-for="conf in backupConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                    </select>
                    <button>New Configuration</button>
                </div>

                <label>Enable Postgres Utils</label>  
                <label for="pgUtils" class="switch">Postgres Utils <input type="checkbox" id="pgUtils" v-model="pgUtils" data-switch="OFF"></label>

                <button v-on:click="createCluster">Create Cluster</button> <button v-on:click="cancel" class="border">Cancel</button>
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
            backupConfig: '',
            pgUtils: false
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
        shortPGVersion () {
            return this.pgVersion.substring(0,2)
        }

    },
    methods: {

        createCluster: function(e) {
            e.preventDefault();

            let sidecars = [];

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
                    // "restore": {
                    //     "fromBackup": "d7e660a9-377c-11ea-b04b-0242ac110004"
                    // },
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
                notify('Cluster <strong>'+cluster.metadata.name+'</strong> created successfully', 'message');
            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        cancel: function() {
            router.push('/overview/'+store.state.currentNamespace);
        },

        showFields: function() {
            $('.hide').slideDown();
        }

    }
})