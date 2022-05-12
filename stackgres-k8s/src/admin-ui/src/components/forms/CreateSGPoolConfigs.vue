<template>
    <div id="create-poolConfig" v-if="loggedIn && isReady && !notFound">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(config).length > 0"></template>
        
        <form id="createPoolConfig" class="form" @submit.prevent>
            <div class="header">
                <h2>Connection Pooling Configuration Details</h2>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Configuration Name <span class="req">*</span></label>
                    <input v-model="poolConfigName" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgpoolingconfig.metadata.name')"></span>
                </div>
            </div>

             <span class="warning topLeft" v-if="nameColission && !editMode">
                There's already a <strong>SGPoolingConfig</strong> with the same name on this namespace. Please specify a different name or create the configuration on another namespace
            </span>

            <div class="row-100">
                <div class="col">
                    <label for="spec.pgBouncer.pgbouncer\.ini">PgBouncer Parameters</label>
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgpoolingconfig.spec.pgBouncer.pgbouncer.ini')"></span>
                    <textarea v-model="poolConfigParams" placeholder="parameter = value" data-field="spec.pgBouncer.pgbouncer\.ini"></textarea>
                </div>
            </div>

            <hr/>

            <template v-if="editMode">
                <template v-if="configClusters.length">
                    <br/><br/>
                    <span class="warning">Please, be aware that some changes made to this configuration might require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance on the following {{ (configClusters.length > 1) ? 'clusters' : 'cluster' }}: <strong>{{ configClusters.join(", ") }}</strong> </span>
                </template>

                <button type="submit" class="btn" @click="createPoolConfig()">Update Configuration</button>
            </template>
            <template v-else>
                <button type="submit" class="btn" @click="createPoolConfig()">Create Configuration</button>
            </template>
            
            <button @click="cancel()" class="btn border">Cancel</button>
            
            <button type="button" class="btn floatRight" @click="createPoolConfig(true)">View Summary</button>
        </form>
        <CRDSummary :crd="previewCRD" kind="SGPoolingConfig" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateSGPoolConfigs',

        mixins: [mixin],

        components: {
            CRDSummary
        },
        
        data: function() {

            const vm = this;

            return {
                editMode: (vm.$route.name === 'EditPoolConfig'),
                editReady: false,
                previewCRD: {},
                showSummary: false,
                poolConfigName: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                poolConfigNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                poolConfigParams: '',
                configClusters: []
            }

        },
        computed: {

            nameColission() {
                const vc = this;
                var nameColission = false;
                
                store.state.sgpoolconfigs.forEach(function(item, index) {
                    if( (item.name == vc.poolConfigName) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },

            config() {
                var vm = this;
                var config = {};

                if( vm.editMode && !vm.editReady ) {
                    store.state.sgpoolconfigs.forEach(function( conf ){
                        if( (conf.data.metadata.name === vm.$route.params.name) && (conf.data.metadata.namespace === vm.$route.params.namespace) ) {
                            vm.poolConfigParams = conf.data.spec.pgBouncer["pgbouncer.ini"].replace('[pgbouncer]\n','');
                            vm.configClusters = [...conf.data.status.clusters]
                            config = conf;

                            vm.editReady = true
                            return false;
                        }
                    });    
                }
                
                return config
            }
        },
        methods: {

            createPoolConfig(preview = false) {
                const vc = this;

                if(vc.checkRequired()) {

                    var config = { 
                        "kind": "StackGresConnectionPoolingConfig",
                        "metadata": {
                            "name": this.poolConfigName,
                            "namespace": this.poolConfigNamespace
                        },
                        "spec": {
                            "pgBouncer": {
                                "pgbouncer.ini": this.poolConfigParams
                            }
                        }
                    }

                    if(preview) {                  

                        vc.previewCRD = {};
                        vc.previewCRD['data'] = config;
                        vc.showSummary = true;

                    } else {

                        if(this.editMode) {
                            sgApi
                            .update('sgpoolconfigs', config)
                            .then(function (response) {
                                vc.notify('Connection pooling configuration <strong>"'+config.metadata.name+'"</strong> updated successfully', 'message','sgpoolconfigs');

                                vc.fetchAPI('sgpoolconfig');
                                router.push('/' + config.metadata.namespace + '/sgpoolconfig/' + config.metadata.name);
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error','sgpoolconfigs');
                            });

                        } else {
                            sgApi
                            .create('sgpoolconfigs', config)
                            .then(function (response) {
                                
                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sgpoolconfig');
                                    vc.notify('Connection pooling configuration <strong>"'+config.metadata.name+'"</strong> created successfully.<br/><br/> You may now close this window and choose your configuration from the list.', 'message','sgpoolconfigs');
                                } else {
                                    vc.notify('Connection pooling configuration <strong>"'+config.metadata.name+'"</strong> created successfully', 'message','sgpoolconfigs');
                                }

                                vc.fetchAPI('sgpoolconfig');
                                router.push('/' + config.metadata.namespace + '/sgpoolconfigs');
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error','sgpoolconfigs');
                            });
                        }
                    }
                }

            }
        }

    }
</script>

<style scoped>
    form.form label + .helpTooltip {
        transform: translate(20px, 15px);
    }
</style>
