var CreatePGConfig = Vue.component("create-pgconfig", {
    template: `
        <div id="create-pgconfig" class="form">
            <header>
                <h2 class="title">{{ $route.params.action }} PostgreSQL Configuration</h2>
            </header>

            <label for="pgConfigNamespace">Configuration Namespace</label>
            <select v-model="pgConfigNamespace" :disabled="(editMode)">
                <option disabled value="">Choose a Namespace</option>
                <option v-for="namespace in allNamespaces">{{ namespace }}</option>
            </select>

            <label for="pgConfigName">Configuration Name</label>
            <input v-model="pgConfigName" :disabled="(editMode)">

            <label for="pgConfigVersion">PostgreSQL Version</label>
            <select v-model="pgConfigVersion" :disabled="(editMode)">
                <option disabled value="">Select PostgreSQL Version</option>
                <option value="11">11</option>
                <option value="12">12</option>
            </select>

            <label for="pgConfigParams">Parameters</label>
            <textarea v-model="pgConfigParams" placeholder="parameter = value"></textarea>

            <template v-if="editMode">
                <button @click="updatePGConfig">Update Configuration</button>
            </template>
            <template v-else>
                <button @click="createPGConfig">Create Configuration</button>
            </template>
            
            <button @click="cancel" class="border">Cancel</button>
		</div>`,
	data: function() {

        if (vm.$route.params.action == 'create') {
            return {
                editMode: false,
                pgConfigName: '',
                pgConfigNamespace: '',
                pgConfigParams: '',
                pgConfigVersion: '',
            }
        } else if (vm.$route.params.action == 'edit') {
            
            var configVersion, configParams = '';
            
            store.state.pgConfig.forEach(function( conf ){
                if( (conf.data.metadata.name === vm.$route.params.name) && (conf.data.metadata.namespace === vm.$route.params.namespace) ) {
                    configVersion = conf.data.spec.pgVersion;

                    $.each( conf.data.spec["postgresql.conf"], function( index, value ){
                        configParams += index+' = '+value+'\n';
                    });
                    return false;
                }
            });
            
            return {
                editMode: true,
                pgConfigName: vm.$route.params.name,
                pgConfigNamespace: vm.$route.params.namespace,
                pgConfigParams: configParams,
                pgConfigVersion: configVersion,
            }
        
        }
	},
	computed: {
        allNamespaces () {
            return store.state.allNamespaces
        }
    },
    methods: {

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
         
            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        updatePGConfig: function(e) {
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
            .put(
                apiURL+'pgconfig/', 
                config 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Postgres configuration <strong>"'+config.metadata.name+'"</strong> updated successfully', 'message');
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