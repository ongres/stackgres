<template>
    <div id="create-profile" class="createProfile noSubmit" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(config).length > 0"></template>

        <form id="createProfile" class="form" @submit.prevent>
            <div class="header">
                <h2>
                    <span>{{ editMode ? 'Edit' : 'Create' }} Instance Profile</span>
                </h2>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Profile Name <span class="req">*</span></label>
                    <input v-model="profileName" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.metadata.name')"></span>
                </div>
            
                <div class="col">
                    <div class="unit-select">
                        <label for="spec.memory">RAM <span class="req">*</span></label>
                        <input v-model="profileRAM" class="size" required data-field="spec.memory" type="number" min="0">

                        <select v-model="profileRAMUnit" class="unit" required data-field="spec.memory">
                            <option value="Mi">MiB</option>
                            <option value="Gi" selected>GiB</option>
                        </select>
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.memory')"></span>
                    </div>
                </div>

                <span class="warning topLeft" v-if="nameColission && !editMode">
                    There's already a <strong>SGInstanceProfile</strong> with the same name on this namespace. Please specify a different name or create the profile on another namespace
                </span>

                <div class="col">
                    <div class="unit-select">
                        <label for="spec.cpu">CPU <span class="req">*</span></label>
                        <input v-model="profileCPU" class="size" required data-field="spec.cpu" type="number" min="0">

                        <select v-model="profileCPUUnit" class="unit" required data-field="spec.cpu">
                            <option selected>CPU</option>
                            <option value="m">millicpu</option>
                        </select>
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.cpu')"></span>
                    </div>
                </div>

                <div class="header">
                    <h2>Huge Pages Specs</h2>
                </div>

                <div class="col">
                    <div class="unit-select">
                        <label for="spec.hugePages.hugepages-2Mi">Huge Pages 2Mi</label>
                        <input v-model="hugePages2Mi" class="size" data-field="spec.hugePages.hugepages-2Mi" type="number" min="0" :max="profileRAM - hugePages1Gi">

                        <select v-model="hugePages2MiUnit" class="unit" data-field="spec.hugePages.hugepages-2Mi">
                            <option value="Mi">MiB</option>
                            <option value="Gi" selected>GiB</option>
                        </select>
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.hugePages.hugepages-2Mi')"></span>
                    </div>
                </div>
                
                <div class="col">
                    <div class="unit-select">
                        <label for="spec.hugePages.hugepages-1Gi">Huge Pages 1Gi</label>
                        <input v-model="hugePages1Gi" class="size" data-field="spec.hugePages.hugepages-1Gi" type="number" min="0" :max="profileRAM - hugePages2Mi">

                        <select v-model="hugePages1GiUnit" class="unit" data-field="spec.hugePages.hugepages-1Gi">
                            <option value="Mi">MiB</option>
                            <option value="Gi" selected>GiB</option>
                        </select>
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.hugePages.hugepages-1Gi')"></span>
                    </div>
                </div>
            </div>
                                
            <hr/>
            
            <template v-if="editMode">
                <template v-if="profileClusters.length">
                    <br/><br/>
                    <span class="warning">Please, be aware that any changes made to this instance profile will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance on the following {{ (profileClusters.length > 1) ? 'clusters' : 'cluster' }}: <strong>{{ profileClusters.join(", ") }}</strong> </span>
                </template>

                <button class="btn" type="submit" @click="createProfile()">Update Profile</button>
            </template>
            <template v-else>
                <button class="btn" type="submit" @click="createProfile()">Create Profile</button>
            </template>

            <button @click="cancel" class="btn border">Cancel</button>

            <button type="button" class="btn floatRight" @click="createProfile(true)">View Summary</button>
        </form>
        <CRDSummary :crd="previewCRD" kind="SGInstanceProfile" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateSGInstanceProfiles',

        mixins: [mixin],

        components: {
			CRDSummary
		},
        
        data: function() {

            const vm = this;

            return {
                editMode: (vm.$route.name === 'EditProfile'),
                editReady: false,
                previewCRD: {},
                showSummary: false,
                profileName: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                profileNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                profileCPU: '',
                profileCPUUnit: 'CPU',
                profileRAM: '',
                profileRAMUnit: 'Gi',
                hugePages1Gi: '',
                hugePages1GiUnit: 'Gi',
                hugePages2Mi: '',
                hugePages2MiUnit: 'Gi',
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
                
                store.state.sginstanceprofiles.forEach(function(item, index) {
                    if( (item.name == vc.profileName) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },

            config() {
                var vm = this;
                var config = {};
                
                if( vm.editMode && !vm.editReady ) {
                    store.state.sginstanceprofiles.forEach(function( conf ){
                        if( (conf.data.metadata.name === vm.$route.params.name) && (conf.data.metadata.namespace === vm.$route.params.namespace) ) {
                            vm.profileCPU = conf.data.spec.cpu.match(/\d+/g)[0];
                            vm.profileCPUUnit = (conf.data.spec.cpu.match(/[a-zA-Z]+/g) !== null) ? conf.data.spec.cpu.match(/[a-zA-Z]+/g)[0] : 'CPU';
                            vm.profileRAM = conf.data.spec.memory.match(/\d+/g)[0];
                            vm.profileRAMUnit = conf.data.spec.memory.match(/[a-zA-Z]+/g)[0];
                            vm.hugePages1Gi = vm.hasProp(conf, 'data.spec.hugePages.hugepages-1Gi') ? conf.data.spec.hugePages['hugepages-1Gi'].match(/\d+/g)[0] : '';
                            vm.hugePages1GiUnit = vm.hasProp(conf, 'data.spec.hugePages.hugepages-1Gi') ? conf.data.spec.hugePages['hugepages-1Gi'].match(/[a-zA-Z]+/g)[0] : 'Gi';
                            vm.hugePages2Mi = vm.hasProp(conf, 'data.spec.hugePages.hugepages-2Mi') ? conf.data.spec.hugePages['hugepages-2Mi'].match(/\d+/g)[0] : '';
                            vm.hugePages2MiUnit = vm.hasProp(conf, 'data.spec.hugePages.hugepages-2Mi') ? conf.data.spec.hugePages['hugepages-2Mi'].match(/[a-zA-Z]+/g)[0] : 'Gi';
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

            createProfile(preview = false) {
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
                            ...( (this.hugePages1Gi.length || this.hugePages2Mi.length) && {
                                "hugePages": {
                                    ...( this.hugePages2Mi.length && {
                                        "hugepages-2Mi": this.hugePages2Mi + this.hugePages2MiUnit
                                    }),
                                    ...( this.hugePages1Gi.length && {
                                        "hugepages-1Gi": this.hugePages1Gi + this.hugePages1GiUnit
                                    }),
                                }
                            })
                        }
                    }

                    if(preview) {                  

                        vc.previewCRD = {};
                        vc.previewCRD['data'] = profile;
                        vc.showSummary = true;

                    } else {

                        if(this.editMode) {
                            sgApi
                            .update('sginstanceprofiles', profile)
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
                            sgApi
                            .create('sginstanceprofiles', profile)
                            .then(function (response) {

                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sginstanceprofile');
                                    vc.notify('Profile <strong>"'+profile.metadata.name+'"</strong> created successfully.<br/><br/> You may now close this window and choose your profile from the list.', 'message','sginstanceprofiles');
                                } else {
                                    vc.notify('Profile <strong>"'+profile.metadata.name+'"</strong> created successfully', 'message','sginstanceprofiles');
                                }

                                vc.fetchAPI('sginstanceprofiles');
                                router.push('/' + profile.metadata.namespace + '/sginstanceprofiles');
                
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error','sginstanceprofiles');
                            });

                        }
                    }        
                    
                }

            }
        }

    }
</script>