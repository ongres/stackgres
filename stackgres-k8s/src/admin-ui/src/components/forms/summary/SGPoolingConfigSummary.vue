<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults">
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.metadata.namespace')"></span>
                        <span class="value"> : {{ crd.data.metadata.namespace }}</span>
                    </li>
                    <li>
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.metadata.name')"></span>
                        <span class="value"> : {{ crd.data.metadata.name }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section">
            <!--CRD Details-->
            <template v-if="crd.data.hasOwnProperty('status') && crd.data.status.pgBouncer['pgbouncer.ini'].length">
                <li>
                    <button class="toggleSummary"></button>
                    <strong class="sectionTitle">Specs </strong>
                    <ul>
                        <li v-if="hasParamsSet(crd)">
                            <button class="toggleSummary"></button>
                            <strong class="label">Parameters</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.spec.pgBouncer.pgbouncer.ini')"></span>
                            <ul class="paramUl">
                                <li v-for="param in crd.data.status.pgBouncer['pgbouncer.ini']" v-if="(!crd.data.status.pgBouncer.defaultParameters.hasOwnProperty(param.parameter) || (crd.data.status.pgBouncer.defaultParameters.hasOwnProperty(param.parameter) && (crd.data.status.pgBouncer.defaultParameters[param.parameter] != param.value)) )">
                                    <strong class="label">{{ param.parameter }}: </strong>
                                    <span>{{ param.value }}</span>
                                </li>
                            </ul>
                        </li>

                        <li v-if="hasDefaults(crd)">
                            <button class="toggleSummary"></button>
                            <strong class="label">Default Parameters</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.status.pgBouncer.defaultParameters')"></span>
                            <ul class="paramUl">
                                <li v-for="param in crd.data.status.pgBouncer['pgbouncer.ini']" v-if="( crd.data.status.pgBouncer.defaultParameters.hasOwnProperty(param.parameter) && (crd.data.status.pgBouncer.defaultParameters[param.parameter] == param.value))">
                                    <strong class="label">{{ param.parameter }}: </strong>
                                    <span>{{ param.value }}</span>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </li>
            </template>

            <!--Form Summary-->
            <template v-else-if="crd.data.spec.pgBouncer['pgbouncer.ini'].length">
                <li>
                    <button class="toggleSummary"></button>
                    <strong class="sectionTitle">Specs </strong>
                    <ul>
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="label">Parameters</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.spec.pgBouncer.pgbouncer.ini')"></span>
                            <ul>
                                <li v-for="param in (crd.data.spec.pgBouncer['pgbouncer.ini'].split(/\r?\n/))" v-if="param.length">
                                    <strong class="label" :set="p = getParam(param)">
                                        {{ p.param }}: 
                                    </strong>
                                    <span class="value">
                                        {{ p.value }}
                                    </span>
                                </li>
                            </ul>                     
                        </li>
                    </ul>
                </li>
            </template>
        </ul>

        <ul class="section" v-if="crd.data.hasOwnProperty('status') && crd.data.status.clusters.length">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Status </strong>
                <ul>
                    <li class="usedOn">
                        <button class="toggleSummary"></button>
                        <strong class="label">Used on</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.status.clusters')"></span>
                        <ul>    
                            <li v-for="cluster in crd.data.status.clusters">
                                <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster" title="Cluster Details">
                                    {{ cluster }}
                                    <span class="eyeIcon"></span>
                                </router-link>
                            </li>
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
        name: 'SGPoolingConfigSummary',

        mixins: [mixin],

        props: ['crd', 'showDefaults'],

        methods: {

            getParam(paramString) {
                let eqIndex = paramString.indexOf('=');

                return {
                    param: paramString.substr(0, eqIndex),
                    value: paramString.substr(eqIndex + 1, paramString.legth)
                };
            },

            hasParamsSet(crd) {
				let setParam = crd.data.status.pgBouncer['pgbouncer.ini'].find(p => ( (crd.data.status.pgBouncer.defaultParameters[p.parameter] != p.value) ))
				return (typeof setParam != 'undefined')
			},

            hasDefaults(crd) {
                let defaultParam = crd.data.status.pgBouncer['pgbouncer.ini'].find(p => ( crd.data.status.pgBouncer.defaultParameters.hasOwnProperty(p.parameter) && (crd.data.status.pgBouncer.defaultParameters[p.parameter] == p.value)))
                return (typeof defaultParam != 'undefined')
            }

        }
	}
</script>