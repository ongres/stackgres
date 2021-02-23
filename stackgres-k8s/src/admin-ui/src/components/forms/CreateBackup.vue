<template>
    <form id="create-backup" v-if="loggedIn && isReady" @submit.prevent="createBackup()">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(backup).length > 0"></template>
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/overview/'+$route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li class="action">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>
                    <router-link :to="'/backups/'+$route.params.namespace" title="SGBackups">SGBackups</router-link>
                </li>
                <li v-if="editMode">
                    <router-link :to="'/backups/'+$route.params.namespace+'/'+$route.params.cluster+'/'+$route.params.uid" title="Backup Details">{{ $route.params.uid }}</router-link>
                </li>
                <li class="action">
                    {{ $route.params.action }}
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/04-postgres-cluster-management/04-backups/" target="_blank" title="SGBackup Documentation">SGBackup Documentation</a>
            </div>
        </header>
                
        <div class="form">
            <div class="header">
                <h2>Backup Details</h2>
            </div>
            
            <label for="spec.sgCluster">Backup Cluster <span class="req">*</span></label>
            <select v-model="backupCluster" :disabled="(editMode)" required data-field="spec.sgCluster">
                <option disabled value="">Choose a Cluster</option>
                <template v-for="cluster in allClusters">
                    <option v-if="cluster.data.metadata.namespace == backupNamespace">{{ cluster.data.metadata.name }}</option>
                </template>
            </select>
            <a class="help" @click="showTooltip( 'sgbackup', 'spec.sgCluster')">
                <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
            </a>

            <label for="metadata.name">Backup Name <span class="req">*</span></label>
            <input v-model="backupName" :disabled="(editMode)" required data-field="metadata.name">
            <a class="help" @click="showTooltip( 'sgbackup', 'metadata.name')">
                <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
            </a>

            <span class="warning" v-if="nameColission && !editMode">
                There's already a <strong>SGBackup</strong> with the same name on this namespace. Please specify a different name or create the backup on another namespace
            </span>

            <label for="spec.managedLifecycle">Managed Lifecycle</label>  
            <label for="permanent" class="switch yes-no" data-field="spec.managedLifecycle">Managed <input type="checkbox" id="permanent" v-model="managedLifecycle" data-switch="NO"></label>
            <a class="help" @click="showTooltip( 'sgbackup', 'spec.managedLifecycle')">
                <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
            </a>
            
            <template v-if="editMode">
                <a class="btn" @click="createBackup">Update Backup</a>
            </template>
            <template v-else>
                <a class="btn" @click="createBackup">Create Backup</a>
            </template>

            <a class="btn border" @click="cancel">Cancel</a>
        </div>
        <div id="help" class="form">
            <div class="header">
                <h2>Help</h2>
            </div>
            
            <div class="info">
                <h3 class="title"></h3>
                <vue-markdown :source=tooltipsText></vue-markdown>
            </div>
        </div>
    </form>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import axios from 'axios'

    export default {
        name: 'CreateBackup',

        mixins: [mixin],

        data: function() {

            const vm = this;

            return {
                editMode: (vm.$route.params.action === 'edit'),
                advancedMode: false,
                backupName: '',
                backupNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                backupCluster: (vm.$route.params.hasOwnProperty('cluster')) ? vm.$route.params.cluster : '',
                managedLifecycle: ''
            }
        },
        computed: {
            allNamespaces () {
                return store.state.allNamespaces
            },

            allClusters () {
                return store.state.clusters
            },

            tooltipsText() {
                return store.state.tooltipsText
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
                
                if(vm.$route.params.action === 'edit') {
                    store.state.backups.forEach(function( bk ){
                        if( (bk.data.metadata.uid === vm.$route.params.uid) && (bk.data.metadata.namespace === vm.$route.params.namespace) ) {
                            vm.backupName = bk.name;
                            vm.backupCluster = bk.data.spec.sgCluster;
                            vm.managedLifecycle = bk.data.spec.managedLifecycle
                            backup = bk;
                            return false;
                        }
                    });    
                }

                return backup
            }
        },
        methods: {

            
            createBackup: function(e) {
                const vc = this;

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
                            "sgCluster": this.backupCluster,
                            "managedLifecycle": this.managedLifecycle
                        },
                        "status": {}
                    };

                    if(this.editMode) {
                        const res = axios
                        .put(
                            '/stackgres/sgbackup/', 
                            backup 
                        )
                        .then(function (response) {
                            vc.notify('Backup <strong>"'+backup.metadata.name+'"</strong> updated successfully', 'message', 'sgbackup');

                            vc.fetchAPI('sgbackup');
                            router.push('/backups/'+backup.metadata.namespace);
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error', 'sgbackup');
                        });

                    } else {
                        const res = axios
                        .post(
                            '/stackgres/sgbackup/', 
                            backup 
                        )
                        .then(function (response) {
                            vc.notify('Backup <strong>"'+backup.metadata.name+'"</strong> started successfully.', 'message', 'sgbackup');

                            vc.fetchAPI('sgbackup');
                            router.push('/backups/'+backup.metadata.namespace);
                            

                            /* store.commit('updateBackupConfig', { 
                                name: config.metadata.name,
                                data: config
                            }); */
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error', 'sgbackup');
                        });
                    }

                }

            },

            cancel: function() {
                const vc = this
                router.push('/backups/'+vc.$route.params.namespace);
            },

            showFields: function( fields ) {
                $(fields).slideDown();
            },

            hideFields: function( fields ) {
                $(fields).slideUp();
            }

        },
        created: function() {
            

        },

        mounted: function() {
        
        },

        beforeDestroy: function() {
            store.commit('setTooltipsText','Click on a question mark to get help and tips about that field.');
        }
    }

</script>