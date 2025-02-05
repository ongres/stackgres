<template>
    <div id="create-backup" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(backup).length > 0"></template>

        <template v-if="editMode && !editReady">
            <span class="warningText">
                Loading data...
            </span>
        </template>
                
        <form id="createBackup" class="form" @submit.prevent v-if="!editMode || editReady">
            <div class="header">
                <h2>
                    <span>{{ editMode ? 'Edit' : 'Create' }} Backup</span>
                </h2>
            </div>
            
            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Backup Name <span class="req">*</span></label>
                    <input v-model="backupName" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackup.metadata.name')"></span>
                </div>

                <div class="col">
                    <label for="spec.sgCluster">Backup Cluster <span class="req">*</span></label>
                    <select v-model="backupCluster" :disabled="(editMode)" required data-field="spec.sgCluster">
                        <option disabled value="">Choose a Cluster</option>
                        <template v-for="cluster in allClusters">
                            <option v-if="cluster.data.metadata.namespace == backupNamespace">{{ cluster.data.metadata.name }}</option>
                        </template>
                    </select>
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackup.spec.sgCluster')"></span>
                </div>

                <span class="warning" v-if="nameCollision && !editMode">
                    There's already a <strong>SGBackup</strong> with the same name on this namespace. Please specify a different name or create the backup on another namespace
                </span>

                <div class="col">
                    <label for="spec.managedLifecycle">Managed Lifecycle</label>  
                    <label for="permanent" class="switch yes-no" data-field="spec.managedLifecycle">Enable 
                        <input type="checkbox" id="permanent" v-model="managedLifecycle" data-switch="NO">
                    </label>
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackup.spec.managedLifecycle')"></span>
                </div>
            </div>

            <hr/>
            
            <template v-if="editMode">
                <button type="submit" class="btn" @click="createBackup()">Update Backup</button>
            </template>
            <template v-else>
                <button type="submit" class="btn" @click="createBackup()">Create Backup</button>
            </template>

            <button class="btn border" @click="cancel()">Cancel</button>
            
            <button type="button" class="btn floatRight" @click="createBackup(true)">View Summary</button>
            <button
                data-field="dryRun"
                type="button"
                class="btn border floatRight"
                title="Dry run mode helps to evaluate a request through the typical request stages without any storage persistance or resource allocation."
                @click="
                    dryRun = true;
                    createBackup();
                "
            >
                Dry Run
            </button>
        </form>

        <CRDSummary
            v-if="showSummary"
            :crd="previewCRD"
            :dryRun="dryRun"
            kind="SGBackup"
            @closeSummary="
                showSummary = false;
                dryRun = false;
                previewCRD = {};
            "
        ></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateSGBackups',

        mixins: [mixin],

        components: {
            CRDSummary
        },

        data: function() {

            const vm = this;

            return {
                editMode: vm.$route.name.includes('Edit'),
                editReady: false,
                dryRun: false,
                advancedMode: false,
                previewCRD: {},
                showSummary: false,
                backupName: 'bk' + vm.getDateString(),
                backupNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                backupCluster: (vm.$route.params.hasOwnProperty('name')) ? vm.$route.params.name : '',
                managedLifecycle: false
            }
        },
        computed: {

            allClusters () {
                return store.state.sgclusters
            },

            nameCollision() {

                if(store.state.sgbackups !== null) {
                    const vc = this;
                    var nameCollision = false;
                    
                    store.state.sgbackups.forEach(function(item, index){
                        if( (item.name == vc.backupName) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                            nameCollision = true
                    })

                    return nameCollision
                } else {
                    return false;
                }
            },

            backup() {
                var vm = this;
                var backup = {};
                
                if( vm.editMode && !vm.editReady && (store.state.sgbackups !== null) ) {
                    store.state.sgbackups.forEach(function( bk ){
                        if( (bk.data.metadata.name === vm.$route.params.backupname) && (bk.data.metadata.namespace === vm.$route.params.namespace) ) {
                            vm.backupName = bk.name;
                            vm.backupCluster = bk.data.spec.sgCluster;
                            vm.managedLifecycle = bk.data.spec.managedLifecycle
                            backup = bk;
                            vm.editReady = true;
                            return false;
                        }
                    });
                }

                return backup
            }
        },
        
        methods: {

            createBackup(preview = false) {
                const vc = this;

                if(!vc.checkRequired()) {
                    vc.dryRun = false;
                    vc.showSummary = false;
                    return;
                }

                store.commit('loading', true);

                let backup = {
                    "metadata": {
                        "name": this.backupName,
                        "namespace": this.backupNamespace
                    },
                    "spec": {
                        "sgCluster": this.backupCluster,
                        "managedLifecycle": this.managedLifecycle
                    },
                    "status": {}
                };

                if(preview) {

                    vc.previewCRD = {};
                    vc.previewCRD['data'] = backup;
                    vc.showSummary = true;
                    store.commit('loading', false);

                } else {

                    if(this.editMode) {
                        sgApi
                        .update('sgbackups', backup, vc.dryRun)
                        .then(function (response) {

                            if(vc.dryRun) {
                                vc.showSummary = true;
                                vc.validateDryRun(response.data);
                            } else {
                                vc.notify('Backup <strong>"'+backup.metadata.name+'"</strong> updated successfully', 'message', 'sgbackups');

                                vc.fetchAPI('sgbackup');

                                // If edited backup is not in Pending state, redirect to its details
                                let bk = store.state.sgbackups.find(b => (b.data.metadata.namespace == backup.metadata.namespace) && (b.data.metadata.name == backup.metadata.name) && vc.hasProp(b, 'data.status.process.status') && (b.data.status.process.status != 'Pending'))
                                
                                if( typeof bk != 'undefined') {
                                    if(vc.isCluster) {
                                        router.push('/' + backup.metadata.namespace + '/sgcluster/' + backup.spec.sgCluster + '/sgbackup/' + backup.metadata.name);
                                    } else {
                                        router.push('/' + backup.metadata.namespace + '/sgbackup/' + backup.metadata.name);
                                    }
                                } else {
                                    if(vc.isCluster) {
                                        router.push('/' + backup.metadata.namespace + '/sgcluster/' + backup.spec.sgCluster + '/sgbackups');
                                    } else {
                                        router.push('/' + backup.metadata.namespace + '/sgbackup/');
                                    }
                                }
                            }

                            store.commit('loading', false);
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error', 'sgbackups');
                            store.commit('loading', false);
                        });

                    } else {
                        sgApi
                        .create('sgbackups', backup, vc.dryRun)
                        .then(function (response) {

                            if(vc.dryRun) {
                                vc.showSummary = true;
                                vc.validateDryRun(response.data);
                            } else {
                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sgbackups');
                                    vc.notify('Backup <strong>"'+backup.metadata.name+'"</strong> started successfully! You must wait for the backup to be completed before you can choose it from the list.<br/><br/>You may now close this window.', 'message','sgbackups');
                                } else {
                                    vc.notify('Backup <strong>"'+backup.metadata.name+'"</strong> started successfully.', 'message', 'sgbackups');
                                }

                                vc.fetchAPI('sgbackup');
                                router.push('/' + backup.metadata.namespace + '/sgbackups');
                            }
                            
                            store.commit('loading', false);
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error', 'sgbackups');
                            store.commit('loading', false);
                        });
                    }
                }

            }
        }
    }

</script>