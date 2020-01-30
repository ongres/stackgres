var CreateCluster = Vue.component("create-cluster", {
	template: `
        <div id="create-cluster" class="form">

            <label for="name">Cluster Name</label>
            <input v-model="name">

            <label for="namespace">K8S Namespace</label>
            <input v-model="namespace">

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
            <select v-model="resourceProfile">
                <option disabled value="">Select Instance Profile</option>
                <option v-for="prof in profiles">{{ prof.name }}</option>
            </select>

            <label for="storageClass">Storage Class</label>
            <input v-model="storageClass">

            <div class="unit-select">
                <label for="volumeSize">Volume Size</label>  
                <select v-model="volumeSize" class="size">
                    <option disabled value="">Select Size</option>
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
                <select v-model="volumeUnit" class="unit">
                    <option disabled value="">Select Unit</option>
                    <option>Ki</option>
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
            <select v-model="pgConfig">
                <option disabled value="">Select PostgreSQL Configuration</option>
                <option v-for="conf in pgConf">{{ conf.name }}</option>
            </select>

            <label>Enable Connection Pooling</label>  
            <label for="connPooling" class="switch">Connection Pooling <input type="checkbox" id="connPooling" v-model="connPooling" data-switch="OFF"></label>

            <button v-on:click="createCluster">Create Cluster</button> <button v-on:click="cancel" class="border">Cancel</button>
            
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
            connPooling: false
		}
	},
	computed: {

		currentNamespace () {
			return store.state.currentNamespace
        },        
        profiles () {
            return store.state.profiles
        },
        pgConf () {
            return store.state.pgConfig
        }

    },
    methods: {

        createCluster: function(e) {
            e.preventDefault();

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
                    // "backupConfig": "backupconf",
                    // "connectionPoolingConfig": "pgbouncerconf",
                    "volumeSize": this.volumeSize+this.volumeUnit,
                    "storageClass": this.storageClass,
                    //"prometheusAutobind": "true",
                    // "sidecars": [
                    //     "connection-pooling",
                    //     "postgres-util",
                    //     "prometheus-postgres-exporter"
                    // ]
                }
            }

            axios
            .post(
                apiURL+'cluster/', 
                cluster 
            )
            .then((response) => {
                notify(response.data,'message')
            })
            .catch((error)=>{
                notify(error,'error')
            });
            
            //console.log(cluster);

        },

        cancel: function() {
            router.push('/overview/'+store.state.currentNamespace);
        }

    }
})