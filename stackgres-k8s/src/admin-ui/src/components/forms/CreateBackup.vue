<template>
    <div id="create-backup" v-if="loggedIn && isReady && !notFound">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(backup).length > 0"></template>
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <template v-if="$route.params.hasOwnProperty('cluster')">
                    <li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						<router-link :to="'/' + $route.params.namespace + '/sgclusters'" title="SGClusters">SGClusters</router-link>
					</li>
                    <li>
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.cluster" title="Status">{{ $route.params.cluster }}</router-link>
					</li>
                    <li>
                        <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.cluster + '/sgbackups'" title="Backups">Backups</router-link>
                    </li>
                </template>
                <template v-else>
                    <li>
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>
                        <router-link :to="'/' + $route.params.namespace + '/sgbackups'" title="SGBackups">SGBackups</router-link>
                    </li>
                </template>
                <li v-if="editMode">
                    <router-link :to="'/' + $route.params.namespace + '/sgbackup/' + $route.params.backupname" title="Backup Details">{{ $route.params.backupname }}</router-link>
                </li>
                <li class="action">
                    {{ $route.name.includes('Edit') ? 'Edit' : 'Create' }}
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgbackup/" target="_blank" title="SGBackup Documentation">SGBackup Documentation</a>
            </div>
        </header>
                
        <form id="createBackup" class="form" @submit.prevent>
            <div class="header">
                <h2>Backup Details</h2>
            </div>
            
            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Backup Name <span class="req">*</span></label>
                    <input v-model="backupName" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off" @keyup="nameEdited = true">
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackup.metadata.name')"></span>
                </div>

                <div class="col">
                    <label for="spec.sgCluster">Backup Cluster <span class="req">*</span></label>
                    <select v-model="backupCluster" :disabled="(editMode)" required data-field="spec.sgCluster" @change="setDefaultName()">
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
                    <label for="permanent" class="switch yes-no" data-field="spec.managedLifecycle">Managed 
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
    import axios from 'axios'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateBackup',

        mixins: [mixin],

        components: {
            CRDSummary
        },

        data: function() {

            const vm = this;

            return {
                nameEdited: false,
                editMode: vm.$route.name.includes('Edit'),
                editReady: false,
                advancedMode: false,
                previewCRD: {},
                showSummary: false,
                backupName: 'sgbackup-' + vm.getDateString(),
                backupNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                backupCluster: (vm.$route.params.hasOwnProperty('cluster')) ? vm.$route.params.cluster : '',
                managedLifecycle: false
            }
        },
        computed: {

            allClusters () {
                return store.state.clusters
            },

            nameColission() {

                const vc = this;
                var nameColission = false;
                
                store.state.backups.forEach(function(item, index){
                    if( (item.name == vc.backupName) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },

            backup() {
                var vm = this;
                var backup = {};
                
                if( vm.editMode && !vm.editReady ) {
                    store.state.backups.forEach(function( bk ){
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
                            const res = axios
                            .put(
                                '/stackgres/sgbackups', 
                                backup 
                            )
                            .then(function (response) {
                                vc.notify('Backup <strong>"'+backup.metadata.name+'"</strong> updated successfully', 'message', 'sgbackups');

                                vc.fetchAPI('sgbackup');

                                // If edited backup is not in Pending state, redirect to its details
                                let bk = store.state.backups.find(b => (b.data.metadata.namespace == backup.metadata.namespace) && (b.data.metadata.name == backup.metadata.name) && vc.hasProp(b, 'data.status.process.status') && (b.data.status.process.status != 'Pending'))
                                
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
                            const res = axios
                            .post(
                                '/stackgres/sgbackups', 
                                backup 
                            )
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

            },

            setDefaultName() {
                const vc = this;

                if(!vc.nameEdited) {
                    vc.backupName = 'sgbackup-' + vc.backupCluster;
                    vc.backupName += ('-' + vc.getDateString());
                    vc.backupName = vc.backupName.substring(0,55);
                } 
            }
        }

    }

</script>