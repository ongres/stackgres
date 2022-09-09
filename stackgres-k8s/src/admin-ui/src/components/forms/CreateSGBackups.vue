<template>
    <div id="create-backup" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(backup).length > 0"></template>
                
        <form id="createBackup" class="form" @submit.prevent>
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

                <span class="warning" v-if="nameColission && !editMode">
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
        </form>
       
        <CRDSummary :crd="previewCRD" kind="SGBackup" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
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

            nameColission() {

                const vc = this;
                var nameColission = false;
                
                store.state.sgbackups.forEach(function(item, index){
                    if( (item.name == vc.backupName) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },

            backup() {
                var vm = this;
                var backup = {};
                
                if( vm.editMode && !vm.editReady ) {
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

                if(vc.checkRequired()) {

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

                    } else {

                        if(this.editMode) {
                            sgApi
                            .update('sgbackups', backup)
                            .then(function (response) {
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
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error', 'sgbackups');
                            });

                        } else {
                            sgApi
                            .create('sgbackups', backup)
                            .then(function (response) {

                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sgbackups');
                                    vc.notify('Backup <strong>"'+backup.metadata.name+'"</strong> started successfully! You must wait for the backup to be completed before you can choose it from the list.<br/><br/>You may now close this window.', 'message','sgbackups');
                                } else {
                                    vc.notify('Backup <strong>"'+backup.metadata.name+'"</strong> started successfully.', 'message', 'sgbackups');
                                }

                                vc.fetchAPI('sgbackup');
                                router.push('/' + backup.metadata.namespace + '/sgbackups');
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error', 'sgbackups');
                            });
                        }
                    }

                }

            }
        }
    }

</script>