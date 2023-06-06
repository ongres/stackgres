<template>
    <div>
        <ul class="section">
            <li>
                <button></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults">
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.metadata.namespace')"></span>
                        <span class="value"> : {{ crd.data.metadata.namespace }}</span>
                    </li>
                    <li>
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.metadata.name')"></span>
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
                    
                    <li v-if="showDefaults || crd.data.spec.continueOnError">
                        <strong class="label">Continue on Error</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                        <span class="value"> : {{ isEnabled(crd.data.spec.continueOnError) }}</span>
                    </li>
                    <li v-if="showDefaults || !crd.data.spec.managedVersions">
                        <strong class="label">Managed Versions</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions').replace(/true/g, 'Enabled')"></span>
                        <span class="value"> : {{ isEnabled(crd.data.spec.managedVersions) }}</span>
                    </li>
                    <li>
                        <button></button>
                        <strong class="label">Script Entries</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts')"></span>
                        <ul>
                            <li v-for="(script, index) in crd.data.spec.scripts">
                                <button></button>
                                <strong class="label">Script #{{ index + 1 }}</strong>

                                <ul>
                                    <li v-if="hasProp(script, 'id')">
                                        <strong class="label">ID</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.id')"></span>
                                        <span class="value"> : {{ script.id }}</span>
                                    </li>
                                    <li v-if="hasProp(script, 'name')">
                                        <strong class="label">Name</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                        <span class="value"> : {{ script.name }}</span>
                                    </li>
                                    <li v-if="hasProp(script, 'version')">
                                        <strong class="label">Version</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                        <span class="value"> : {{ script.version }}</span>
                                    </li>
                                    <li v-if="showDefaults || hasProp(script, 'database')">
                                        <strong class="label">Database</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                        <span class="value"> : {{ script.hasOwnProperty('database') ? script.database : 'postgres' }}</span>
                                    </li>
                                    <li v-if="showDefaults || hasProp(script, 'user')">
                                        <strong class="label">User</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.user')"></span>
                                        <span class="value"> : {{ script.hasOwnProperty('user') ? script.database : 'postgres' }}</span>
                                    </li>
                                    <li v-if="showDefaults || script.hasOwnProperty('wrapInTransaction')">
                                        <strong class="label">Wrap in Transaction</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                                        <span class="value"> : {{ script.hasOwnProperty('wrapInTransaction') ? script.wrapInTransaction : 'Disabled' }}</span>
                                    </li>
                                    <li v-if="showDefaults || (script.hasOwnProperty('storeStatusInDatabase') && script.storeStatusInDatabase)">
                                        <strong class="label">Store Status in Database</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase').replace(/false/g, 'Disabled').replace(/true/g, 'Enabled')"></span>
                                        <span class="value"> : {{ script.hasOwnProperty('storeStatusInDatabase') ? isEnabled(script.storeStatusInDatabase) : 'Disabled' }}</span>
                                    </li>
                                    <li v-if="showDefaults || (script.hasOwnProperty('retryOnError') && script.retryOnError)">
                                        <strong class="label">Retry on Error</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError').replace(/false/g, 'Disabled').replace(/true/g, 'Enabled')"></span>
                                        <span class="value"> : {{ script.hasOwnProperty('retryOnError') ? isEnabled(script.retryOnError) : 'Disabled' }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Script Source</strong>
                                        <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'"></span>
                                        <span class="value"> : {{ hasProp(script, 'script') ? 'Raw Script' : (hasProp(script, 'scriptFrom.secretKeyRef') ? 'Secret Key' : "Config Map") }}</span>
                                    </li>
                                    <li v-if="hasProp(script, 'script')">
                                        <strong class="label">Script</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.script')"></span>
                                        <span class="value script">
                                            <span> : <a @click="setContentTooltip('#script-' + index)">View Script</a></span>
                                            <div :id="'script-' + index" class="hidden">
                                                <pre>{{ script.script }}</pre>
                                            </div>
                                        </span>
                                    </li>
                                    <li v-else-if="hasProp(script, 'scriptFrom.secretKeyRef')">
                                        <button></button>
                                        <strong class="label">Secret Key Reference</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.secretKeyRef')"></span>
                                        <ul>
                                            <li>
                                                <strong class="label">Name</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.secretKeyRef.name')"></span>
                                                <span class="value"> : {{ script.scriptFrom.secretKeyRef.name }}</span>
                                            </li>
                                            <li>
                                                <strong class="label">Key</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.secretKeyRef.key')"></span>
                                                <span class="value"> : {{ script.scriptFrom.secretKeyRef.key }}</span>
                                            </li>
                                        </ul>
                                    </li>
                                    <li v-else-if="hasProp(script, 'scriptFrom.configMapKeyRef')">
                                        <button></button>
                                        <strong class="label">Config Map Key Reference</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.configMapKeyRef')"></span>
                                        <ul>
                                            <li>
                                                <strong class="label">Name</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.configMapKeyRef.name')"></span>
                                                <span class="value"> : {{ script.scriptFrom.configMapKeyRef.name }}</span>
                                            </li>
                                            <li>
                                                <strong class="label">Key</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.configMapKeyRef.key')"></span>
                                                <span class="value"> : {{ script.scriptFrom.configMapKeyRef.key }}</span>
                                            </li>                                                                            
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="crd.data.hasOwnProperty('status') && crd.data.status.clusters.length">
            <li>
                <button></button>
                <strong class="sectionTitle">Status </strong>
                <ul>
                    <li class="usedOn">
                        <button></button>
                        <strong class="label">Used on</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.status.clusters')"></span>
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
	import { mixin } from '../../mixins/mixin'

    export default {
        name: 'SGScriptSummary',

		mixins: [mixin],

        props: ['crd', 'showDefaults']

	}
</script>
