<template>
    <div>
        <ul class="section">
            <li>
                <button></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults">
                        <button></button>
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.metadata.namespace')"></span>
                        <span class="value"> : {{ crd.data.metadata.namespace }}</span>
                    </li>
                    <li>
                        <button></button>
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.metadata.name')"></span>
                        <span class="value"> : {{ crd.data.metadata.name }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section">
            <li>
                <button></button>
                <strong class="sectionTitle">Specs </strong>
                <ul>
                    <li>
                        <strong class="label">Postgres Version</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.spec.postgresVersion')"></span>
                        <span class="value"> : {{ crd.data.spec.postgresVersion }}</span>
                    </li>

                    <!--CRD Detilas-->
                    <template v-if="crd.data.hasOwnProperty('status')">
                        <template v-if="hasParamsSet(crd)">
                            <li>
                                <button></button>
                                <strong class="label">Parameters</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.spec.postgresql.conf')"></span>
                                <ul class="paramUl">
                                    <li v-for="param in crd.data.status['postgresql.conf']" v-if="(!crd.data.status.defaultParameters.hasOwnProperty(param.parameter) || (crd.data.status.defaultParameters.hasOwnProperty(param.parameter) && (crd.data.status.defaultParameters[param.parameter] != param.value)) )">
                                        <strong class="label">
                                            {{ param.parameter }}
                                            <a v-if="(typeof param.documentationLink !== 'undefined')" :href="param.documentationLink" target="_blank" :title="'Read documentation about ' + param.parameter" class="paramDoc">
                                            </a>
                                        </strong>
                                        <span>
                                            : {{ param.value }}
                                        </span>
                                    </li>
                                </ul>
                            </li>
                        </template>

                        <template v-if="Object.keys(crd.data.status.defaultParameters).length">
                            <li>
                                <button></button>
                                <strong class="label">Default Parameters</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.status.defaultParameters')"></span>
                                <ul class="paramUl">
                                    <li v-for="param in crd.data.status['postgresql.conf']" v-if="( crd.data.status.defaultParameters.hasOwnProperty(param.parameter) && (crd.data.status.defaultParameters[param.parameter] == param.value))">
                                        <strong class="label">
                                            {{ param.parameter }}
                                            <a v-if="(typeof param.documentationLink !== 'undefined')" :href="param.documentationLink" target="_blank" :title="'Read documentation about ' + param.parameter" class="paramDoc"></a>
                                        </strong>
                                        <span class="paramValue">
                                            : {{ param.value }}
                                        </span>
                                    </li>
                                </ul>
                            </li>
                        </template>
                    </template>

                    <!--Form Summary-->
                    <template v-else>
                        <li v-if="crd.data.spec['postgresql.conf'].length">
                            <button></button>
                            <strong class="label">Parameters</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.spec.postgresql.conf')"></span>
                            <ul>
                                <li v-for="param in (crd.data.spec['postgresql.conf'].split(/\r?\n/))" v-if="param.length">
                                    <strong class="label" :set="p = getParam(param)">
                                        {{ p.param }}:
                                    </strong>
                                    <span class="value">
                                        {{ p.value }}
                                    </span>
                                </li>
                            </ul>
                        </li>
                    </template>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(crd.data.hasOwnProperty('status') && (crd.data.status.clusters.length || logsClusters.length))">
            <li>
                <button></button>
                <strong class="sectionTitle">Status </strong>
                <ul>
                    <li class="usedOn">
                        <button></button>
                        <strong class="label">Used on</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.status.clusters').replace('[SGClusters](https://stackgres.io/doc/latest/04-postgres-cluster-management/01-postgres-clusters/)', 'resources')"></span>
                        <ul>
                            <template v-if="crd.data.status.clusters.length">
                                <li>
                                    <button></button>
                                    <strong class="label">SGClusters</strong>
                                    <ul>
                                        <li v-for="cluster in crd.data.status.clusters">
                                            <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster" title="Cluster Details">
                                                {{ cluster }}
                                                <span class="eyeIcon"></span>
                                            </router-link>
                                        </li>
                                    </ul>
                                </li>
                            </template>
                            <template v-if="logsClusters.length">
                                <li>
                                    <button></button>
                                    <strong class="label">SGDistributedLogs</strong>
                                    <ul>
                                        <li v-for="lcluster in logsClusters">
                                            <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + lcluster.name" title="Logs Server Details">
                                                {{ lcluster.name }}
                                                <span class="eyeIcon"></span>
                                            </router-link>
                                        </li>
                                    </ul>
                                </li>
                            </template>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
    import { mixin } from './../../mixins/mixin'
    import store from '../../../store'

    export default {
        name: 'SGPostgresConfigSummary',

        mixins: [mixin],

        props: ['crd', 'showDefaults'],

        computed: {
			logsClusters(){
                return store.state.sgdistributedlogs.filter(c => (c.data.metadata.namespace == this.$route.params.namespace) && (c.data.spec.configurations.sgPostgresConfig == this.$route.params.name))
            }              
		},

        methods: {

            getParam(paramString) {
                let eqIndex = paramString.indexOf('=');

                return {
                    param: paramString.substr(0, eqIndex),
                    value: paramString.substr(eqIndex + 1, paramString.legth)
                };
            },

            hasParamsSet(crd) {
				let setParam = crd.data.status['postgresql.conf'].find(p => ( (crd.data.status.defaultParameters[p.parameter] != p.value) ))
				return (typeof setParam != 'undefined')
			}

        }
	}
</script>