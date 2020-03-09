var CreateBackup = Vue.component("create-backup", {
    template: `
        <form id="create-backup">
            <header>
                <ul class="breadcrumbs">
                    <li class="namespace">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                        {{ currentNamespace }}
                    </li>
                    <li>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 27.1 20"><path d="M15.889 13.75a2.277 2.277 0 011.263.829 2.394 2.394 0 01.448 1.47 2.27 2.27 0 01-.86 1.885 3.721 3.721 0 01-2.375.685h-3.449a.837.837 0 01-.6-.213.8.8 0 01-.22-.6v-7.795a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.306a3.679 3.679 0 012.306.648 2.165 2.165 0 01.836 1.815 2.159 2.159 0 01-.395 1.3 2.254 2.254 0 01-1.089.79zm-4.118-.585h2.179q1.779 0 1.778-1.323a1.143 1.143 0 00-.441-.989 2.267 2.267 0 00-1.337-.321h-2.179zm2.407 4.118a2.219 2.219 0 001.363-.335 1.242 1.242 0 00.428-1.042 1.271 1.271 0 00-.435-1.056 2.155 2.155 0 00-1.356-.348h-2.407v2.781zm8.929 1.457a3.991 3.991 0 01-2.941-1 3.968 3.968 0 01-1-2.927V9.984a.854.854 0 01.227-.622.925.925 0 011.23 0 .854.854 0 01.227.622V14.9a2.623 2.623 0 00.573 1.838 2.18 2.18 0 001.684.622 2.153 2.153 0 001.671-.628 2.624 2.624 0 00.573-1.832V9.984a.85.85 0 01.228-.622.924.924 0 011.229 0 .85.85 0 01.228.622v4.826a3.969 3.969 0 01-1 2.92 3.95 3.95 0 01-2.929 1.01zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>
                        {{ $route.params.action }} backup
                    </li>
                    <li v-if="editMode">
                        {{ $route.params.name }}
                    </li>
                </ul>
            </header>
                    
            <div class="form">
                <label for="backupConfigNamespace">Backup Namespace</label>
                <select v-model="backupNamespace" :disabled="(editMode)" required>
                    <option disabled value="">Choose a Namespace</option>
                    <option v-for="namespace in allNamespaces">{{ namespace }}</option>
                </select>

                <label for="backupCluster">Backup Cluster</label>
                <select v-model="backupCluster" :disabled="(editMode)" required>
                    <option disabled value="">Choose a Cluster</option>
                    <template v-for="cluster in allClusters">
                        <option v-if="cluster.data.metadata.namespace == backupNamespace">{{ cluster.data.metadata.name }}</option>
                    </template>
                </select>

                <label for="backupName">Backup Name</label>
                <input v-model="backupName" :disabled="(editMode)" required>

                <label>Is Permanent</label>  
                <label for="permanent" class="switch">Permanent <input type="checkbox" id="permanent" v-model="isPermanent" data-switch="NO"></label>

                <template v-if="editMode">
                    <button @click="createBackup">Update Backup</button>
                </template>
                <template v-else>
                    <button @click="createBackup">Create Backup</button>
                </template>

                <button @click="cancel" class="border">Cancel</button>
            </div>
        </form>`,
	data: function() {
        
        if (vm.$route.params.action == 'create') {
            
            return {
                editMode: false,
                backupName: '',
                backupNamespace: vm.$route.params.namespace,
                backupCluster: '',
                isPermanent: false
            }

        } else if (vm.$route.params.action == 'edit') {
            
            return {
                editMode: true,
                backupName: vm.$route.params.name,
                backupNamespace: vm.$route.params.namespace,
                backupCluster: vm.$route.params.cluster,
                isPermanent: false
            }
        }
	},
	computed: {
        allNamespaces () {
            return store.state.allNamespaces
        },

        allClusters () {
            return store.state.clusters
        },

        currentNamespace() {
            return store.state.currentNamespace
        }
    },
    methods: {

        
        createBackup: function(e) {
            //e.preventDefault();

            let isValid = true;
            
            $('input:required, select:required').each(function() {
                if ($(this).val() === '') {
                    isValid = false;
                    return false;
                }
                    
            });

            if(isValid) {

                let backup = {
                    "metadata": {
                        "name": this.backupName,
                        "namespace": this.backupNamespace
                    },
                    "spec": {
                        "cluster": this.backupCluster,
                        "isPermanent": this.isPermanent
                    },
                    "status": {}
                };

                if(this.editMode) {
                    const res = axios
                    .put(
                        apiURL+'backup/', 
                        backup 
                    )
                    .then(function (response) {
                        console.log("GOOD");
                        notify('Backup <strong>"'+backup.metadata.name+'"</strong> updated successfully', 'message');

                        vm.fetchAPI();
                        router.push('/backups/'+backup.metadata.namespace);
                    })
                    .catch(function (error) {
                        console.log(error.response);
                        notify(error.response.data.message,'error');
                    });

                } else {
                    const res = axios
                    .post(
                        apiURL+'backup/', 
                        backup 
                    )
                    .then(function (response) {
                        console.log("GOOD");
                        notify('Backup <strong>"'+backup.metadata.name+'"</strong> created successfully', 'message');

                        vm.fetchAPI();
                        router.push('/backups/'+backup.metadata.namespace);
                        

                        /* store.commit('updateBackupConfig', { 
                            name: config.metadata.name,
                            data: config
                        }); */
                    })
                    .catch(function (error) {
                        console.log(error.response);
                        notify(error.response.data.message,'error');
                    });
                }

            }

        },

        cancel: function() {
            router.push('/backups/'+store.state.currentNamespace);
        },

        showFields: function( fields ) {
            $(fields).slideDown();
        },

        hideFields: function( fields ) {
            $(fields).slideUp();
        }

    }
})