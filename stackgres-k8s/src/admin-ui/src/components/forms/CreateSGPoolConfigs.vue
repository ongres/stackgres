<template>
    <div id="create-poolConfig" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(config).length > 0"></template>
        
        <form id="createPoolConfig" class="form" @submit.prevent v-if="!editMode || editReady">
            <div class="header">
                <h2>
                    <span>{{ editMode ? 'Edit' : 'Create' }} Connection Pooling Configuration</span>
                </h2>
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

            <template v-if="Object.keys(defaultParams).length">
                <div class="paramDetails">
                    <hr/>
                    <h2>Default Parameters</h2><br/>
                    <p>StackGres has set some default parameters to your configuration. If no value is specifically set for them, they will remain with the following default values:</p><br/><br/>
                
                    <table class="defaultParams">
                        <tbody>
                            <tr v-for="param in poolConfigParamsObj" v-if="(defaultParams.hasOwnProperty(param.parameter) && (defaultParams[param.parameter] == param.value) )">
                                <td class="label">
                                    {{ param.parameter }}
                                </td>
                                <td class="paramValue">
                                    {{ param.value }}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </template>

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
                configClusters: [],
                poolConfigParamsObj: null,
                defaultParams: {},
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
                            vm.poolConfigParams = vm.getParams(conf);
                            vm.poolConfigParamsObj = conf.data.status.pgBouncer["pgbouncer.ini"];
                            vm.defaultParams = conf.data.status.pgBouncer["defaultParameters"];
                            vm.configClusters = [...conf.data.status.clusters];
                            config = conf;

                            vm.editReady = true;
                            return false;
                        }
                    });    
                }
                
                return config
            }
        },
        methods: {

            createPoolConfig(preview = false, previous) {
                const vc = this;

                if(!vc.checkRequired()) {
                    return;
                }

                if (!previous) {
                    sgApi
                    .getResourceDetails('sgpoolconfigs', this.poolConfigNamespace, this.poolConfigName)
                    .then(function (response) {
                        vc.createPoolConfig(preview, response.data);
                    })
                    .catch(function (error) {
                        if (error.response.status != 404) {
                          console.log(error.response);
                          vc.notify(error.response.data,'error', 'sgpoolconfigs');
                          return;
                        }
                        vc.createPoolConfig(preview, {});
                    });
                    return;
                }

                var config = {
                    "kind": "StackGresConnectionPoolingConfig",
                    "metadata": {
                        ...(this.hasProp(previous, 'metadata') && previous.metadata),
                        "name": this.poolConfigName,
                        "namespace": this.poolConfigNamespace
                    },
                    "spec": {
                        ...(this.hasProp(previous, 'spec') && previous.spec),
                        "pgBouncer": {
                            "pgbouncer.ini": this.editMode ? vc.unifyParams() : this.poolConfigParams
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
            },

            getParams(conf) {
                const vc = this;
                const customParams = [];

                var configParams = conf.data.status.pgBouncer["pgbouncer.ini"];
                var defaultParams = conf.data.status.pgBouncer["defaultParameters"];

                configParams.forEach(function(item) {
                    if(defaultParams.hasOwnProperty(item.parameter)) {
                        if(item.value !== defaultParams[item.parameter]) {
                            customParams.push(item.parameter + "=" + item.value)
                        }
                    } else {
                        customParams.push(item.parameter + "=" + item.value)
                    }
                });

                return customParams.join('\n')
            }, 

            unifyParams() {
                const vc = this;
                const inputParamsObj = {};
                const initialParamsObj = {};
                const finalParamsArr = [];
                
                var inputParams = vc.poolConfigParams.split('\n');
                var initialParams = vc.poolConfigParamsObj;
                var defaultParams = vc.defaultParams;

                inputParams.forEach(function(item) {
                    if(item.length && (item != " ")) {
                        const indexOfEqual = item.indexOf('=');
                        const key = item.substring(0, indexOfEqual);
                        var value = (item.substring(indexOfEqual+1, item.length));
                        
                        if(value.startsWith("'")) {
                            value = value.substring(1)
                        }

                        if(value.endsWith("'")) {
                            value = value.substring(0, value.length-1)
                        }

                        inputParamsObj[key] = value
                    }
                });

                initialParams.forEach(function(item) {
                    if(!inputParamsObj.hasOwnProperty(item.parameter)) {
                        if(defaultParams.hasOwnProperty(item.parameter)) {
                            inputParamsObj[item.parameter] = defaultParams[item.parameter]
                        }
                    }
                });

                Object.keys(inputParamsObj).forEach( (key) => {
                    finalParamsArr.push(key + "=" + inputParamsObj[key])
                });

                return finalParamsArr.join('\n')
            }

        }

    }
</script>

<style scoped>
    form.form label + .helpTooltip {
        transform: translate(20px, 15px);
    }
</style>
