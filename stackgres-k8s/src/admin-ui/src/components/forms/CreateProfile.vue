<template>
    <form id="create-profile" v-if="loggedIn && isReady && !notFound" @submit.prevent>
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(config).length > 0"></template>

        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li>
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><g transform="translate(0 -242)"><path d="M19.649,256.971l-1.538-1.3a.992.992,0,1,0-1.282,1.514l.235.2-6.072,2.228v-4.373l.266.154a.974.974,0,0,0,.491.132.99.99,0,0,0,.862-.506,1.012,1.012,0,0,0-.369-1.372l-1.75-1.013a.983.983,0,0,0-.984,0l-1.75,1.013a1.012,1.012,0,0,0-.369,1.372.985.985,0,0,0,1.353.374l.266-.154v4.353l-6.07-2.21.233-.2a.992.992,0,1,0-1.282-1.514l-1.538,1.3a.992.992,0,0,0-.337.925l.342,1.987a.992.992,0,0,0,.977.824.981.981,0,0,0,.169-.015.992.992,0,0,0,.81-1.145l-.052-.3,7.4,2.694A1.011,1.011,0,0,0,10,262c.01,0,.02,0,.03-.005s.02.005.03.005a1,1,0,0,0,.342-.061l7.335-2.691-.051.3a.992.992,0,0,0,.811,1.145.953.953,0,0,0,.168.015.992.992,0,0,0,.977-.824l.341-1.987A.992.992,0,0,0,19.649,256.971Z" fill="#00adb5"/><path d="M20,246.25a.99.99,0,0,0-.655-.93l-9-3.26a1,1,0,0,0-.681,0l-9,3.26a.99.99,0,0,0-.655.93.9.9,0,0,0,.016.1c0,.031-.016.057-.016.089v5.886a1.052,1.052,0,0,0,.992,1.1,1.052,1.052,0,0,0,.992-1.1v-4.667l7.676,2.779a1.012,1.012,0,0,0,.681,0l7.675-2.779v4.667a1,1,0,1,0,1.984,0v-5.886c0-.032-.014-.058-.016-.089A.9.9,0,0,0,20,246.25Zm-10,2.207L3.9,246.25l6.1-2.206,6.095,2.206Z" fill="#00adb5"/></g></svg>
                <router-link :to="'/' + $route.params.namespace + '/sginstanceprofiles/'" title="SGInstanceProfiles">SGInstanceProfiles</router-link>
                </li>
                <li v-if="editMode">
                    <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + $route.params.name" title="SGInstanceProfile Details">{{ $route.params.name }}</router-link
                </li>
                <li class="action">
                    {{ $route.name == 'EditProfile' ? 'Edit' : 'Create' }}
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sginstanceprofile/" target="_blank" title="SGInstanceProfile Documentation">SGInstanceProfile Documentation</a>
            </div>
        </header>

        <div class="form">
            <div class="header">
                <h2>Instance Profile Details</h2>
            </div>

            <label for="metadata.name">Profile Name <span class="req">*</span></label>
            <input v-model="profileName" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
            <a class="help" @click="showTooltip( 'sgprofile', 'metadata.name')"></a>

            <span class="warning" v-if="nameColission && !editMode">
                There's already a <strong>SGInstanceProfile</strong> with the same name on this namespace. Please specify a different name or create the profile on another namespace
            </span>

            
            <div class="unit-select">
                <label for="spec.memory">RAM <span class="req">*</span></label>
                <input v-model="profileRAM" class="size" required data-field="spec.memory" type="number" min="0">

                <select v-model="profileRAMUnit" class="unit" required data-field="spec.memory">
                    <option value="Mi">MiB</option>
                    <option value="Gi" selected>GiB</option>
                </select>
                <a class="help" @click="showTooltip( 'sgprofile', 'spec.memory')"></a>
            </div>

            <div class="unit-select">
                <label for="spec.cpu">CPU <span class="req">*</span></label>
                <input v-model="profileCPU" class="size" required data-field="spec.cpu" type="number" min="0">

                <select v-model="profileCPUUnit" class="unit" required data-field="spec.cpu">
                    <option selected>CPU</option>
                    <option value="m">millicpu</option>
                </select>
                <a class="help" @click="showTooltip( 'sgprofile', 'spec.cpu')"></a>
            </div>
                                

            <template v-if="editMode">
                <template v-if="profileClusters.length">
                    <br/><br/>
                    <span class="warning">Please, be aware that any changes made to this instance profile will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance on the following {{ (profileClusters.length > 1) ? 'clusters' : 'cluster' }}: <strong>{{ profileClusters.join(", ") }}</strong> </span>
                </template>

                <button class="btn" @click="createProfile">Update Profile</button>
            </template>
            <template v-else>
                <button class="btn" @click="createProfile">Create Profile</button>
            </template>

            <button @click="cancel" class="btn border">Cancel</button>
        </div>
        <div id="help" class="form">
            <div class="header">
                <h2>Help</h2>
            </div>
            
            <div class="info">
                <h3 class="title"></h3>
                <vue-markdown :source=tooltipsText :breaks=false></vue-markdown>
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
        name: 'CreateProfile',

        mixins: [mixin],
        
        data: function() {

            const vm = this;

            return {
                editMode: (vm.$route.name === 'EditProfile'),
                editReady: false,
                profileName: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                profileNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                profileCPU: '',
                profileCPUUnit: 'CPU',
                profileRAM: '',
                profileRAMUnit: 'Gi',
                profileClusters: []
            }
                
            
        },
        computed: {
            allNamespaces () {
                return store.state.allNamespaces
            },

            tooltipsText() {
                return store.state.tooltipsText
            },

            nameColission() {
                const vc = this;
                var nameColission = false;
                
                store.state.profiles.forEach(function(item, index) {
                    if( (item.name == vc.profileName) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },

            config() {
                var vm = this;
                var config = {};
                
                if( vm.editMode && !vm.editReady ) {
                    store.state.profiles.forEach(function( conf ){
                        if( (conf.data.metadata.name === vm.$route.params.name) && (conf.data.metadata.namespace === vm.$route.params.namespace) ) {
                            vm.profileCPU = conf.data.spec.cpu.match(/\d+/g)[0];
                            vm.profileCPUUnit = (conf.data.spec.cpu.match(/[a-zA-Z]+/g) !== null) ? conf.data.spec.cpu.match(/[a-zA-Z]+/g)[0] : 'CPU';
                            vm.profileRAM = conf.data.spec.memory.match(/\d+/g)[0];
                            vm.profileRAMUnit = conf.data.spec.memory.match(/[a-zA-Z]+/g)[0];
                            vm.profileClusters = [...conf.data.status.clusters]
                            config = conf;

                            vm.editReady = true
                            return false
                        }
                    });
                }
            
                return config
            }
        },
        methods: {

            createProfile: function(e) {
                const vc = this;

                if(vc.checkRequired()) {

                    var profile = { 
                        "metadata": {
                            "name": this.profileName,
                            "namespace": this.profileNamespace
                        },
                        "spec": {
                            "cpu": (this.profileCPUUnit !== 'CPU')? this.profileCPU+this.profileCPUUnit : this.profileCPU,
                            "memory": this.profileRAM+this.profileRAMUnit,
                        }
                    }

                    if(this.editMode) {
                        axios
                        .put(
                            '/stackgres/sginstanceprofiles', 
                            profile 
                        )
                        .then(function (response) {
                            vc.notify('Profile <strong>"'+profile.metadata.name+'"</strong> updated successfully', 'message','sginstanceprofiles');

                            vc.fetchAPI('sginstanceprofile');
                            router.push('/' + profile.metadata.namespace + '/sginstanceprofile/' + profile.metadata.name);

                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error','sginstanceprofiles');
                        });

                    } else {
                        axios
                        .post(
                            '/stackgres/sginstanceprofiles', 
                            profile 
                        )
                        .then(function (response) {

                            vc.notify('Profile <strong>"'+profile.metadata.name+'"</strong> created successfully', 'message','sginstanceprofiles');
                            vc.fetchAPI('sginstanceprofile');
                            router.push('/' + profile.metadata.namespace + '/sginstanceprofiles');
            
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error','sginstanceprofiles');
                        });

                    }
        
                    
                }

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