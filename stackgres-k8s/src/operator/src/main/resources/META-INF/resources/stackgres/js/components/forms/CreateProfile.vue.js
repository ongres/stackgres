var CreateProfile = Vue.component("create-profile", {
    template: `
        <div id="create-profile" class="form">
            <header>
                <h2 class="title">{{ $route.params.action }} Instance Profile</h2>
            </header>
            
            <label for="profileNamespace">K8S Namespace</label>
            <select v-model="profileNamespace" :disabled="(editMode)">
                <option disabled value="">Choose a Namespace</option>
                <option v-for="namespace in allNamespaces">{{ namespace }}</option>
            </select>

            <label for="profileName">Profile Name</label>
            <input v-model="profileName" :disabled="(editMode)">

            
            <div class="unit-select">
                <label for="profileRAM">RAM</label>
                <input v-model="profileRAM" class="size">

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

            <template v-if="editMode">
                <button @click="updateProfile">Update Profile</button>
            </template>
            <template v-else>
                <button @click="createProfile">Create Profile</button>
            </template>

            <button @click="cancel" class="border">Cancel</button>
		</div>`,
	data: function() {

        if (vm.$route.params.action == 'create') {
            return {
                editMode: false,
                profileName: '',
                profileNamespace: '',
                profileCPU: '',
                profileRAM: '',
                profileRAMUnit: '',
            }
        } else if (vm.$route.params.action == 'edit') {

            var cpu, ram, unit;
            
            store.state.profiles.forEach(function( profile ){
                if( (profile.data.metadata.name === vm.$route.params.name) && (profile.data.metadata.namespace === vm.$route.params.namespace) ) {
                    console.log(profile);
                    cpu = profile.data.spec.cpu;
                    ram = profile.data.spec.memory.match(/\d+/g);
                    unit = profile.data.spec.memory.match(/[a-zA-Z]+/g);
                    return false;
                }
            });
            
            return {
                editMode: true,
                profileName: vm.$route.params.name,
                profileNamespace: vm.$route.params.namespace,
                profileCPU: cpu,
                profileRAM: ''+ram,
                profileRAMUnit: ''+unit
            }
        }
	},
	computed: {
        allNamespaces () {
            return store.state.allNamespaces
        }
    },
    methods: {

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

            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        updateProfile: function(e) {
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
            .put(
                apiURL+'profile/', 
                profile 
            )
            .then(function (response) {
                console.log("GOOD");
                notify('Profile <strong>"'+profile.metadata.name+'"</strong> updated successfully', 'message');
            })
            .catch(function (error) {
                console.log(error.response);
                notify(error.response.data.message,'error');
            });

        },

        cancel: function() {
            router.push('/profiles/'+store.state.currentNamespace);
        },

        showFields: function( fields ) {
            $(fields).slideDown();
        },

        hideFields: function( fields ) {
            $(fields).slideUp();
        }

    }
})