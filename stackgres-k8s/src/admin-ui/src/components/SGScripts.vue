<template>
	<div id="script-config" v-if="iCanLoad">
		<div class="content">
			<template v-if="!$route.params.hasOwnProperty('name')">
				<table id="script" class="configurations script resizable fullWidth" v-columns-resizable>
					<thead class="sort">
						<th class="sorted asc name hasTooltip">
							<span @click="sort('data.metadata.name')" title="Name">
								Name
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgscript.metadata.name')"></span>
						</th>
                        <th class="sorted asc hasTooltip">
							<span @click="sort('data.spec.managedVersions')" title="Managed Versions">
                                Managed Versions
                            </span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions')"></span>
						</th>
                        <th class="sorted asc hasTooltip">
							<span @click="sort('data.spec.continueOnError')" title="Continue on Error">
                                Continue on Error
                            </span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError')"></span>
						</th>
						<th class="hasTooltip notSortable textRight">
							<span title="Script Count">
                                Script Count
                            </span>
							<span class="helpTooltip" data-tooltip="The total count of entries associated to this script"></span>
						</th>
                        <th class="hasTooltip notSortable">
							<span title="Default Script">
                                Default Script
                            </span>
							<span class="helpTooltip" data-tooltip="Indicates if it's a default script which has been created by StackGres"></span>
						</th>
                        <th class="actions"></th>
					</thead>
					<tbody>
						<template v-if="!scripts.length">
							<tr class="no-results">
								<td colspan="999" v-if="iCan('create','sgscripts',$route.params.namespace)">
									No script configurations have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgscripts/new'" title="Add New Script Configuration">create a new one?</router-link>
								</td>
								<td v-else colspan="3">
									No script configurations have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
						</template>
						<template v-for="(baseScript, index) in scripts">
							<template  v-if="(index >= pagination.start) && (index < pagination.end)">
								<tr class="base">
									<td class="hasTooltip">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
												{{ baseScript.name }}
											</router-link>
										</span>
									</td>
                                    <td class="center icon" :class="hasProp(baseScript,'data.spec.managedVersions') ? baseScript.data.spec.managedVersions.toString() : 'true'" :data-val="hasProp(baseScript, 'data.spec.managedVersions') ? baseScript.data.spec.managedVersions : 'true'">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
												<span></span>
											</router-link>
										</span>
									</td>
                                    <td class="center icon" :class="hasProp(baseScript,'data.spec.continueOnError') ? baseScript.data.spec.continueOnError.toString() : 'false'" :data-val="hasProp(baseScript, 'data.spec.continueOnError') ? baseScript.data.spec.continueOnError : 'false'">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
												<span></span>
											</router-link>
										</span>
									</td>
                                    <td class="hasTooltip textRight">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
												{{ baseScript.data.spec.scripts.length }}
											</router-link>
										</span>
									</td>
                                    <td class="center icon" :class="isDefaultScript(baseScript) ? 'true' : 'false'" :data-val="isDefaultScript(baseScript)">
                                        <router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
                                            <span></span>
                                        </router-link>
                                    </td>
									<td class="actions">
										<router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" target="_blank" class="newTab"></router-link>
										<router-link v-if="iCan('patch','sgscripts',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name + '/edit'" title="Edit Script" class="editCRD" :class="isDefaultScript(baseScript) && 'disabled'"></router-link>
										<a v-if="iCan('create','sgscripts',$route.params.namespace)" @click="cloneCRD('SGScripts', $route.params.namespace, baseScript.name)" class="cloneCRD" title="Clone Script"></a>
										<a v-if="iCan('delete','sgscripts',$route.params.namespace)" @click="deleteCRD('sgscripts',$route.params.namespace, baseScript.name)" class="delete deleteCRD" title="Delete Script" :class="isDefaultScript(baseScript) && 'disabled'"></a>
									</td>
								</tr>
							</template>
						</template>
					</tbody>
				</table>
				<v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < scripts.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="scripts .length" @page-change="pageChange" align="center" ref="page"></v-page>
				<div id="nameTooltip">
					<div class="info"></div>
				</div>
			</template>
			<template v-else>
				<h2>Script Details</h2>
				<template v-for="baseScript in scripts" v-if="baseScript.name == $route.params.name">
					<div class="configurationDetails">
						<table class="crdDetails">
							<tbody>
								<tr>
									<td class="label">
                                        Name 
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.metadata.name')"></span>
                                    </td>
									<td>
                                        {{ baseScript.name }}
                                    </td>
								</tr>
                                 <tr>
									<td class="label">
                                        Continue on Error 
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                    </td>
									<td>
                                        {{ baseScript.data.spec.hasOwnProperty('continueOnError') ? isEnabled(baseScript.data.spec.continueOnError) : 'Disabled' }}
                                    </td>
								</tr>	
								<tr>
									<td class="label">
                                        Managed Versions 
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions').replace(/true/g, 'Enabled')"></span>
                                    </td>
									<td>
                                        {{ baseScript.data.spec.hasOwnProperty('managedVersions') ? isEnabled(baseScript.data.spec.managedVersions) : 'Enabled' }}
                                    </td>
								</tr>
                                <tr v-if="baseScript.data.status.clusters.length">
									<td class="label">
                                        Used on
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.status.clusters')"></span>
                                    </td>
									<td class="usedOn" colspan="2">
										<ul>
											<li v-for="cluster in baseScript.data.status.clusters">
												<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster" title="Cluster Details">
													{{ cluster }}
													<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
												</router-link>
											</li>
										</ul>
									</td>
								</tr>
							</tbody>
						</table>
					</div>

                    <div class="configurationDetails">
                        <h2>Entries <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts')"></span></h2>

                        <table class="crdDetails" v-for="(script, index) in baseScript.data.spec.scripts">
                            <tbody>
                                <template>
                                    <tr>
                                        <td :rowspan="999">
                                            Script #{{ index + 1 }}
                                        </td>
                                        <td class="label">
                                            ID
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.id')"></span>
                                        </td>
                                        <td class="textRight">
                                            {{ script.id }}
                                        </td>
                                    </tr>
                                    <tr v-if="script.hasOwnProperty('name')">
                                        <td class="label">
                                            Name
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                        </td>
                                        <td>
                                            {{ script.name }}
                                        </td>
                                    </tr>
                                    <tr v-if="script.hasOwnProperty('version')">
                                        <td class="label">
                                            Version
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                        </td>
                                        <td class="textRight">
                                            {{ script.version }}
                                        </td>
                                    </tr>
                                    <tr v-if="script.hasOwnProperty('database')">
                                        <td class="label">
                                            Database
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                        </td>
                                        <td>
                                            {{ script.database }}
                                        </td>
                                    </tr>
                                    <tr v-if="script.hasOwnProperty('user')">
                                        <td class="label">
                                            User
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.user')"></span>
                                        </td>
                                        <td>
                                            {{ script.user }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Retry on Error
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError').replace(/false/g, 'Disabled').replace(/true/g, 'Enabled')"></span>
                                        </td>
                                        <td>
                                            {{ script.hasOwnProperty('retryOnError') ? isEnabled(script.retryOnError) : 'Disabled' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Store Status in Database
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase').replace(/false/g, 'Disabled').replace(/true/g, 'Enabled')"></span>
                                        </td>
                                        <td>
                                            {{ script.hasOwnProperty('storeStatusInDatabase') ? isEnabled(script.storeStatusInDatabase) : 'Disabled' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Wrap in Transaction
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                                        </td>
                                        <td class="upper">
                                            {{ script.hasOwnProperty('wrapInTransaction') ? script.wrapInTransaction : 'Disabled' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Script Details 
                                            <span class="helpTooltip" :data-tooltip="( script.hasOwnProperty('scriptFrom') ? getTooltip('sgscript.spec.scripts.scriptFrom') : getTooltip('sgscript.spec.scripts.script') )"></span>
                                        </td>
                                        <td>
                                            <template v-if="hasProp(script, 'scriptFrom.secretKeyRef')">
                                                <a @click="setContentTooltip('#script-'+index)"> 
                                                    View Details
                                                    <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                                </a>
                                                <div :id="'script-'+index" class="hidden">
                                                    <strong>Name</strong>: {{  script.scriptFrom.secretKeyRef.name }}<br/><br/>
                                                    <strong>Key</strong>: {{  script.scriptFrom.secretKeyRef.key }}
                                                </div>
                                            </template>
                                            <template v-else>
                                                <a @click="setContentTooltip('#script-'+index)"> 
                                                    View Script
                                                    <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                                </a>
                                                <div :id="'script-'+index" class="hidden">
                                                    <pre v-if="script.hasOwnProperty('script')">{{ script.script }}</pre>
                                                    <pre v-else-if="hasProp(script, 'scriptFrom.configMapScript')">{{ script.scriptFrom.configMapScript }}</pre>
                                                </div>
                                            </template>
                                        </td>
                                    </tr>
                                </template>
                            </tbody>
                        </table>
                    </div>				
				</template>
			</template>
		</div>
	</div>
</template>

<script>
	import { mixin } from './mixins/mixin'
	import store from '../store'

    export default {
        name: 'SGScripts',

		mixins: [mixin],

        data: function() {
            return {
                currentSort: {
					param: 'data.metadata.name',
					type: 'alphabetical'
				},
				currentSortDir: 'asc',
            }
        },

        computed: {

			scripts () {
				return this.sortTable( [...(store.state.sgscripts.filter(s => (s.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			},

		},

        methods: {

            isDefaultScript(script) {
                return ( script.data.status.clusters.length && (script.name == (script.data.status.clusters[0] + '-default') ) )
            }

        }
	}

</script>