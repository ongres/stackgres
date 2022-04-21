<template>
    <div id="create-pgconfig" v-if="loggedIn && isReady && !notFound">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(config).length > 0"></template>
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li class="action">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.7 20"><path d="M10.946 18.7a.841.841 0 01-.622-.234.862.862 0 01-.234-.635v-7.817a.8.8 0 01.221-.6.834.834 0 01.608-.214h3.29a3.4 3.4 0 012.353.755 2.7 2.7 0 01.843 2.12 2.72 2.72 0 01-.843 2.126 3.379 3.379 0 01-2.353.764h-2.394v2.875a.8.8 0 01-.869.867zM14 13.637q1.778 0 1.778-1.551T14 10.535h-2.18v3.1zm11.968-.107a.683.683 0 01.494.181.625.625 0 01.191.477v2.875a1.717 1.717 0 01-.16.87 1.174 1.174 0 01-.655.414 6.882 6.882 0 01-1.242.294 9.023 9.023 0 01-1.364.107 5.252 5.252 0 01-2.527-.573 3.883 3.883 0 01-1.638-1.665 5.548 5.548 0 01-.569-2.6 5.5 5.5 0 01.569-2.575 3.964 3.964 0 011.611-1.671 4.965 4.965 0 012.455-.59 4.62 4.62 0 013.089 1.016 1.058 1.058 0 01.234.294.854.854 0 01-.087.843.479.479 0 01-.388.2.737.737 0 01-.267-.047 1.5 1.5 0 01-.281-.153 4.232 4.232 0 00-1.1-.582 3.648 3.648 0 00-1.146-.167 2.747 2.747 0 00-2.2.859 3.834 3.834 0 00-.742 2.561q0 3.477 3.049 3.477a6.752 6.752 0 001.815-.254v-2.36h-1.517a.737.737 0 01-.5-.161.664.664 0 010-.909.732.732 0 01.5-.161zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z" class="a"></path></svg>
                    <router-link :to="'/' + $route.params.namespace + '/sgpgconfigs'" title="SGPostgresConfigs">SGPostgresConfigs</router-link>
                </li>
                <li v-if="editMode">
                    <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + $route.params.name" title="Configuration Details">{{ $route.params.name }}</router-link>
                </li>
                <li class="action">
                    {{ $route.name == 'EditPgConfig' ? 'Edit' : 'Create' }}
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgpgconfig/" target="_blank" title="SGPostgresConfig Documentation">SGPostgresConfig Documentation</a>
            </div>
        </header>

        <form id="cretaePgConfig" class="form" @submit.prevent>
            <div class="header">
                <h2>Postgres Configuration Details</h2>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Configuration Name <span class="req">*</span></label>
                    <input v-model="pgConfigName" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgpostgresconfig.metadata.name')"></span>
                </div>

                <div class="col">
                    <label for="spec.postgresVersion">Postgres Version <span class="req">*</span></label>
                    <select v-model="pgConfigVersion" :disabled="(editMode)" required data-field="spec.postgresVersion">
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
                    <label for="spec.postgresql.conf">Parameters</label>
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgpostgresconfig.spec.postgresql.conf')"></span>
                    <textarea v-model="pgConfigParams" placeholder="parameter = value" data-field="spec.postgresql.conf"></textarea>
                </div>
            </div>

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
        </form>
        <CRDSummary :crd="previewConfig" kind="SGPostgresConfig" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import axios from 'axios'
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
                previewConfig: {},
                showSummary: false,
                pgConfigName: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                pgConfigNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                pgConfigParams: '',
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
                            vm.pgConfigParams = conf.data.spec["postgresql.conf"];
                            vm.configClusters = [...conf.data.status.clusters]
                            config = conf;
                            
                            vm.editReady = true
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

            createPGConfig(preview = false) {
                const vc = this;

                let isValid = vc.checkRequired();
                
                if(isValid) {
                    var config = { 
                        "metadata": {
                            "name": this.pgConfigName,
                            "namespace": this.pgConfigNamespace
                        },
                        "spec": {
                            "postgresVersion": this.pgConfigVersion,
                            "postgresql.conf": this.pgConfigParams
                        }
                    }

                    if(preview) {                  

                        vc.previewConfig = {};
                        vc.previewConfig['data'] = config;
                        vc.showSummary = true;

                    } else {

                        if(this.editMode) {
                            const res = axios
                            .put(
                                '/stackgres/sgpgconfigs', 
                                config 
                            )
                            .then(function (response) {
                                vc.notify('Postgres configuration <strong>"'+config.metadata.name+'"</strong> updated successfully', 'message', 'sgpgconfigs');

                                vc.fetchAPI('sgpgconfig');
                                router.push('/' + config.metadata.namespace + '/sgpgconfig/' + config.metadata.name);
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error', 'sgpgconfigs');
                            });
                        } else {
                            const res = axios
                            .post(
                                '/stackgres/sgpgconfigs', 
                                config 
                            )
                            .then(function (response) {
                                
                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sgpgconfigs');
                                    vc.notify('Postgres configuration <strong>"'+config.metadata.name+'"</strong> created successfully.<br/><br/> You may now close this window and choose your configuration from the list.', 'message','sgpgconfigs');
                                } else {
                                    vc.notify('Postgres configuration <strong>"'+config.metadata.name+'"</strong> created successfully', 'message', 'sgpgconfigs');
                                }
                
                                vc.fetchAPI('sgpgconfigs');
                                router.push('/' + config.metadata.namespace + '/sgpgconfigs');                    
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error', 'sgpgconfigs');
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