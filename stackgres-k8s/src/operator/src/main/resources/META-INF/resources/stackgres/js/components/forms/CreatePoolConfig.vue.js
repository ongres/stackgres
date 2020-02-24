var CreatePoolConfig = Vue.component("create-poolconfig", {
    template: `
        <div id="create-poolConfig" class="form">
            <header>
                <h2 class="title">{{ $route.params.action }} Connection Pooling Configuration</h2>
            </header>
            
            <label for="poolConfigNamespace">Configuration Namespace</label>
            <select v-model="poolConfigNamespace" :disabled="(editMode)">
                <option disabled value="">Choose a Namespace</option>
                <option v-for="namespace in allNamespaces">{{ namespace }}</option>
            </select>

            <label for="poolConfigName">Configuration Name</label>
            <input v-model="poolConfigName" :disabled="(editMode)">

            <label for="poolConfigParams">Parameters</label>
            <textarea v-model="poolConfigParams" placeholder="parameter = value"></textarea>

            <template v-if="editMode">
                <button @click="updatePoolConfig">Update Configuration</button>
            </template>
            <template v-else>
                <button @click="createPoolConfig">Create Configuration</button>
            </template>
            
            <button @click="cancel" class="border">Cancel</button>
		</div>`,
	data: function() {
            
        if (vm.$route.params.action == 'create') {
            return {
                editMode: false,
                poolConfigName: '',
                poolConfigNamespace: '',
                poolConfigParams: '',
            }
        } else if (vm.$route.params.action == 'edit') {
            
            var configParams = '';
            
            store.state.poolConfig.forEach(function( conf ){
                if( (conf.data.metadata.name === vm.$route.params.name) && (conf.data.metadata.namespace === vm.$route.params.namespace) ) {
                    $.each( conf.data.spec["pgbouncer.ini"], function( index, value ){
                        configParams += index+' = '+value+'\n';
                    });
                    return false;
                }
            });
            
            return {
                editMode: true,
                poolConfigName: vm.$route.params.name,
                poolConfigNamespace: vm.$route.params.namespace,
                poolConfigParams: configParams,
            }
        
        }
	},
	computed: {
        allNamespaces () {
            return store.state.allNamespaces
        }
    },
    methods: {

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
            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        updatePoolConfig: function(e) {
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
            .put(
                apiURL+'connpoolconfig/', 
                config 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Connection pooling configuration <strong>"'+config.metadata.name+'"</strong> updated successfully', 'message');
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