<template>
    <div id="create-pgconfig" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(config).length > 0"></template>

        <form id="createPgConfig" class="form" @submit.prevent v-if="!editMode || editReady">
            <div class="header">
                <h2>
                    <span>{{ editMode ? 'Edit' : 'Create' }} Postgres Configuration</span>
                </h2>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Configuration Name <span class="req">*</span></label>
                    <input v-model="pgConfigName" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgpostgresconfig.metadata.name')"></span>
                </div>

                <div class="col">
                    <label for="spec.postgresVersion">Postgres Version <span class="req">*</span></label>
                    <select
                        v-model="pgConfigVersion"
                        :disabled="(editMode)"
                        required data-field="spec.postgresVersion"
                        @change="loadingDefaults = true; dryRun = true; createPGConfig();"
                    >
                        <option disabled value="">Select Major Postgres Version</option>
                        <option v-for="version in postgresVersions">{{ version }}</option>
                    </select>
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgpostgresconfig.spec.postgresVersion')"></span>
                </div>

                <span class="warning topLeft" v-if="nameColission && !editMode">
                    There's already a <strong>SGPostgresConfig</strong> with the same name on this namespace. Please specify a different name or create the configuration on another namespace
                </span>
            </div>

            <div class="row-100">
                <div class="col">
                    <label for="spec.postgresql\.conf">Parameters</label>
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgpostgresconfig.spec.postgresql.conf')"></span>
                    <textarea v-model="pgConfigParams" placeholder="parameter = value" data-field="spec.postgresql\.conf"></textarea>
                </div>
            </div>

            <template v-if="Object.keys(defaultParams).length">
                <div class="paramDetails">
                    <hr/>
                    <h2>Default Parameters</h2><br/>
                    <p>StackGres sets some default parameters for your configuration. If no value is specifically set for them, they will remain with the following default values:</p><br/><br/>
                
                    <table class="defaultParams loadingContainer" :class="loadingDefaults && 'loading'">
                        <tbody>
                            <tr v-for="param in pgConfigParamsObj" v-if="(defaultParams.hasOwnProperty(param.parameter) && (defaultParams[param.parameter] == param.value) )">
                                <td class="label">
                                    {{ param.parameter }}
                                    <a v-if="(typeof param.documentationLink !== 'undefined')" :href="param.documentationLink" target="_blank" :title="'Read documentation about ' + param.parameter" class="paramDoc"></a>
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

                <button class="btn" type="submit" @click="createPGConfig()">Update Configuration</button>
            </template>
            <template v-else>
                <button class="btn" type="submit" @click="createPGConfig()">Create Configuration</button>
            </template>
            
            <button class="btn border" @click="cancel()">Cancel</button>

            <button type="button" class="btn floatRight" @click="createPGConfig(true)">View Summary</button>
            <button
                type="button"
                class="btn border floatRight"
                title="Dry run mode helps to evaluate a request through the typical request stages without any storage persistance or resource allocation."
                @click="
                    dryRun = true;
                    createPGConfig();
                "
            >
                Dry Run
            </button>
        </form>
        <CRDSummary
            v-if="showSummary"
            :crd="previewConfig"
            :dryRun="dryRun"
            kind="SGPostgresConfig"
            @closeSummary="
                showSummary = false;
                dryRun = false;
                previewConfig = {};
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
        name: 'CreateSGPgConfigs',

        mixins: [mixin],

        components: {
            CRDSummary
        },
        
        data: function() {

            const vm = this;

            return {
                editMode: (vm.$route.name === 'EditPgConfig'),
                editReady: false,
                dryRun: false,
                previewConfig: {},
                showSummary: false,
                pgConfigName: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                pgConfigNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                pgConfigParams: '',
                pgConfigParamsObj: null,
                defaultParams: {},
                loadingDefaults: false,
                pgConfigVersion: '',
                configClusters: []
            }
            
        },
        computed: {
            
            nameColission() {
                const vc = this;
                var nameColission = false;
                
                store.state.sgpgconfigs.forEach(function(item, index) {
                    if( (item.name == vc.pgConfigName) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },

            config() {
                var vm = this;
                var config = {};

                if( vm.editMode && !vm.editReady ) {
                    store.state.sgpgconfigs.forEach(function( conf ){
                        if( (conf.data.metadata.name === vm.$route.params.name) && (conf.data.metadata.namespace === vm.$route.params.namespace) ) {
                            vm.pgConfigVersion = conf.data.spec.postgresVersion;
                            vm.pgConfigParams = vm.getParams(conf);
                            vm.pgConfigParamsObj = conf.data.status["postgresql.conf"];
                            vm.defaultParams = conf.data.status["defaultParameters"];
                            vm.configClusters = [...conf.data.status.clusters];
                            config = conf;
                            
                            vm.editReady = true;
                            return false;
                        }
                    });    
                }

                return config
            },

            postgresVersions() {
                return Object.keys(store.state.postgresVersions.vanilla).reverse()
            }
        },
        methods: {

            createPGConfig(preview = false, previous) {
                const vc = this;

                if (!vc.loadingDefaults && !vc.checkRequired()) {
                    return;
                }

                store.commit('loading', !vc.loadingDefaults);

                if (!previous) {
                    sgApi
                    .getResourceDetails('sgpgconfigs', this.pgConfigNamespace, this.pgConfigName)
                    .then(function (response) {
                        vc.createPGConfig(preview, response.data);
                    })
                    .catch(function (error) {
                        if (error.response.status != 404) {
                          console.log(error.response);
                          vc.notify(error.response.data,'error', 'sgpgconfigs');
                          return;
                        }
                        vc.createPGConfig(preview, {});
                    });
                    return;
                }

                var config = {
                    "metadata": {
                        ...(this.hasProp(previous, 'metadata') && previous.metadata),
                        "name": this.pgConfigName,
                        "namespace": this.pgConfigNamespace
                    },
                    "spec": {
                        ...(this.hasProp(previous, 'spec') && previous.spec),
                        "postgresVersion": this.pgConfigVersion,
                        "postgresql.conf": this.editMode ? vc.unifyParams() : this.pgConfigParams
                    }
                }

                if(preview) {

                    vc.previewConfig = {};
                    vc.previewConfig['data'] = config;
                    vc.showSummary = true;
                    store.commit('loading', false);

                } else {

                    if(this.editMode) {
                        sgApi
                        .update('sgpgconfigs', config, vc.dryRun)
                        .then(function (response) {
                            if(vc.dryRun) {
                                vc.showSummary = !vc.loadingDefaults;
                                vc.validateDryRun(response.data);
                            } else {
                                vc.notify('Postgres configuration <strong>"'+config.metadata.name+'"</strong> updated successfully', 'message', 'sgpgconfigs');

                                vc.fetchAPI('sgpgconfig');
                                router.push('/' + config.metadata.namespace + '/sgpgconfig/' + config.metadata.name);
                            }
                            store.commit('loading', false);
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error', 'sgpgconfigs');
                            store.commit('loading', false);
                        });
                    } else {

                        sgApi
                        .create('sgpgconfigs', config, vc.dryRun)
                        .then(function (response) {

                            if(vc.dryRun) {
                                vc.showSummary = !vc.loadingDefaults;
                                vc.validateDryRun(response.data);
                            } else {
                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sgpgconfigs');
                                    vc.notify('Postgres configuration <strong>"'+config.metadata.name+'"</strong> created successfully.<br/><br/> You may now close this window and choose your configuration from the list.', 'message','sgpgconfigs');
                                } else {
                                    vc.notify('Postgres configuration <strong>"'+config.metadata.name+'"</strong> created successfully', 'message', 'sgpgconfigs');
                                }

                                vc.fetchAPI('sgpgconfigs');
                                router.push('/' + config.metadata.namespace + '/sgpgconfigs');
                            }
                            store.commit('loading', false);
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error', 'sgpgconfigs');
                            store.commit('loading', false);
                        });
                    }
                }
            },

            getParams(conf) {
                const vc = this;
                const customParams = [];

                var configParams = conf.data.status["postgresql.conf"];
                var defaultParams = conf.data.status["defaultParameters"];

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
                
                var inputParams = vc.pgConfigParams.split('\n');
                var initialParams = vc.pgConfigParamsObj;
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
                    finalParamsArr.push(key + "='" + inputParamsObj[key] + "'")
                });

                return finalParamsArr.join('\n')
            },

            validateDryRun(data) {
                const vc = this;

                vc.defaultParams = data.status.defaultParameters;
                vc.pgConfigParamsObj = data.status['postgresql.conf'];
                vc.loadingDefaults = false;

                if(vc.showSummary) {
                    vc.previewConfig = {};
                    vc.previewConfig['data'] = data;
                } else {
                    vc.dryRun = false;
                }
                store.commit('loading', false);
            }
        }
    }
</script>

<style scoped>
    form.form label + .helpTooltip {
        transform: translate(20px, 15px);
    }

    .defaultParams.loadingContainer.loading:after {
        position: absolute;
    }
</style>
