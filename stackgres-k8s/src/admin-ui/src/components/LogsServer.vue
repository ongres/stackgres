<template>
    <div id="logs-cluster" v-if="loggedIn && isReady && !notFound">
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/overview/'+$route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M19,15H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,14.6,19.6,15,19,15z"/><path class="a" d="M1,15L1,15c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,14.6,1.6,15,1,15z"/><path class="a" d="M19,11H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,10.6,19.6,11,19,11z"/><path class="a" d="M1,11L1,11c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,10.6,1.6,11,1,11z"/><path class="a" d="M19,7H5C4.4,7,4,6.6,4,6v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,6.6,19.6,7,19,7z"/><path d="M1,7L1,7C0.4,7,0,6.6,0,6v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,6.6,1.6,7,1,7z"/></svg>
                    SGDistributedLogs
                </li>
                <li v-if="(typeof $route.params.name !== 'undefined')">
                    {{ $route.params.name }}
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgdistributedlogs/" target="_blank" title="SGDistributedLogs Documentation">SGDistributedLogs Documentation</a>
                <div>
                    <router-link v-if="iCan('create','sgdistributedlogs',$route.params.namespace)" :to="'/crd/create/logs/'+$route.params.namespace" class="add">Add New</router-link>
                </div>	
            </div>		
        </header>

        <div class="content">
            <table id="logs" class="logsCluster pgConfig resizable" v-columns-resizable>
                <thead class="sort">
                    <th class="sorted desc name hasTooltip">
                        <span @click="sort('data.metadata.name')" title="Name">
                            Name
                        </span>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.metadata.name')"></span>
                    </th>
                    <th class="desc volumeSize hasTooltip">
                        <span @click="sort('data.spec.persistentVolume.size', 'memory')" title="Volume Size">
                            Volume Size
                        </span>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume.size')"></span>
                    </th>
                    <th class="actions"></th>
                </thead>
                <tbody>
                    <tr class="no-results">
                        <td :colspan="3" v-if="iCan('create','sgdistributedlogs',$route.params.namespace)">
                            No logs clusters have been found, would you like to <router-link :to="'/crd/create/logs/'+$route.params.namespace" title="Add New Logs Cluster">create a new one?</router-link>
                        </td>
                        <td v-else colspan="3">
                            No configurations have been found. You don't have enough permissions to create a new one
                        </td>
                    </tr>
                    <template v-for="(cluster, index) in clusters">
                        <tr class="base" :class="[ $route.params.name == cluster.data.metadata.name ? 'open' : '', 'logs-'+cluster.data.metadata.namespace+'-'+cluster.data.metadata.name, ( (index < pagination.start) || (index >= pagination.end) ? 'hide' : '' )]" :data-name="cluster.data.metadata.name">
                            <td class="hasTooltip clusterName">
                                <span>{{ cluster.name }}</span>
                            </td>
                            <td class="volumeSize fontZero">
                                {{ cluster.data.spec.persistentVolume.size }}
                            </td>
                            <td class="actions">
                                <router-link v-if="iCan('patch','sgdistributedlogs',$route.params.namespace)" :to="'/crd/edit/logs/'+$route.params.namespace+'/'+cluster.data.metadata.name" title="Edit Configuration">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 14 14"><path d="M90,135.721v2.246a.345.345,0,0,0,.345.345h2.246a.691.691,0,0,0,.489-.2l8.042-8.041a.346.346,0,0,0,0-.489l-2.39-2.389a.345.345,0,0,0-.489,0L90.2,135.232A.691.691,0,0,0,90,135.721Zm13.772-8.265a.774.774,0,0,0,0-1.095h0l-1.82-1.82a.774.774,0,0,0-1.095,0h0l-1.175,1.176a.349.349,0,0,0,0,.495l2.421,2.421a.351.351,0,0,0,.5,0Z" transform="translate(-90 -124.313)"/></svg>
                                </router-link>
                                <a v-if="iCan('create','sgdistributedlogs',$route.params.namespace)" v-on:click="cloneCRD('SGDistributedLogs', $route.params.namespace, cluster.data.metadata.name)" class="cloneCRD" title="Clone Logs Cluster"><svg xmlns="http://www.w3.org/2000/svg" width="13.9" height="16" viewBox="0 0 20 20"><g><path fill="#00ADB5" d="M2.5,20c-0.5,0-1-0.4-1-1V5c0-0.5,0.4-1,1-1c0.6,0,1,0.4,1,1v12.4c0,0.3,0.3,0.6,0.6,0.6h9.4c0.5,0,1,0.4,1,1c0,0.5-0.4,1-1,1H2.5z"/><path fill="#00ADB5" d="M6.5,16c-0.5,0-0.9-0.4-0.9-0.9V0.9C5.6,0.4,6,0,6.5,0h11.1c0.5,0,0.9,0.4,0.9,0.9v14.1c0,0.5-0.4,0.9-0.9,0.9H6.5z M8,1.8c-0.3,0-0.6,0.3-0.6,0.6v11.2c0,0.3,0.3,0.6,0.6,0.6h8.1c0.3,0,0.6-0.3,0.6-0.6V2.4c0-0.3-0.3-0.6-0.6-0.6H8z"/><path fill="#00ADB5" d="M14.1,5.3H10c-0.5,0-0.9-0.4-0.9-0.9v0c0-0.5,0.4-0.9,0.9-0.9h4.1c0.5,0,0.9,0.4,0.9,0.9v0C15,4.9,14.6,5.3,14.1,5.3z"/><path fill="#00ADB5" d="M14.1,8.8H10C9.5,8.8,9.1,8.4,9.1,8v0c0-0.5,0.4-0.9,0.9-0.9h4.1C14.6,7.1,15,7.5,15,8v0C15,8.4,14.6,8.8,14.1,8.8z"/><path fill="#00ADB5" d="M14.1,12.4H10c-0.5,0-0.9-0.4-0.9-0.9v0c0-0.5,0.4-0.9,0.9-0.9h4.1c0.5,0,0.9,0.4,0.9,0.9v0C15,12,14.6,12.4,14.1,12.4z"/></g></svg></a>
                                <a v-if="iCan('delete','sgdistributedlogs',$route.params.namespace)" v-on:click="deleteCRD('sgdistributedlogs',$route.params.namespace, cluster.data.metadata.name, '/logs/' + $route.params.namespace)" class="delete" title="Delete Configuration" :class="cluster.data.status.clusters.length ? 'disabled' : ''">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
                                </a>
                            </td>
                        </tr>
                        <tr :class="[$route.params.name == cluster.data.metadata.name ? 'open details logsCluster pgConfig' : 'details logsCluster pgConfig', ( (index < pagination.start) || (index >= pagination.end) ? 'hide' : '' )]">
                            <td colspan="3">
                                <div class="configurationDetails">
                                    <table>
                                        <thead>
                                            <th colspan="2" class="label">
                                            Log Details
                                            </th>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td class="label">
                                                    Volume Size
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume.size')"></span>
                                                </td>
                                                <td>{{ cluster.data.spec.persistentVolume.size }}</td>
                                            </tr>
                                            <tr v-if="cluster.data.spec.persistentVolume.hasOwnProperty('storageClass')">
                                                <td class="label">
                                                    Storage Class
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume.storageClass')"></span>
                                                </td>
                                                <td>{{ cluster.data.spec.persistentVolume.storageClass }}</td>
                                            </tr>
                                            <template v-if="cluster.data.status.clusters.length">
                                                <tr>
                                                    <td class="label">
                                                        Used on
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.status.clusters')"></span>
                                                    </td>
                                                    <td class="usedOn">
                                                        <ul>
                                                            <li v-for="c in cluster.data.status.clusters">
                                                                {{ c }}
                                                                <router-link :to="'/cluster/status/'+$route.params.namespace+'/'+c" title="Cluster Details">
                                                                    <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                                                </router-link>
                                                            </li>
                                                        </ul>
                                                    </td>
                                                </tr>
                                            </template>
                                        </tbody>
                                    </table>

                                    <template v-if="hasProp(cluster, 'data.spec.metadata.annotations')">
                                        <table>
                                            <thead>
                                                <th colspan="3" class="label">
                                                    Resources Metadata
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations')"></span>
                                                </th>
                                            </thead>
                                            <tbody>
                                                <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.allResources)" v-if="hasProp(cluster, 'data.spec.metadata.annotations.allResources')">
                                                    <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.allResources).length">
                                                        All Resources
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.allResources')"></span>
                                                    </td>
                                                    <td class="label">
                                                        {{ item.annotation }}
                                                    </td>
                                                    <td colspan="2">
                                                        {{ item.value }}
                                                    </td>
                                                </tr>
                                                <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.pods)" v-if="hasProp(cluster, 'data.spec.metadata.annotations.pods')">
                                                    <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.pods).length">
                                                        Pods
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.pods')"></span>
                                                    </td>
                                                    <td class="label">
                                                        {{ item.annotation }}
                                                    </td>
                                                    <td colspan="2">
                                                        {{ item.value }}
                                                    </td>
                                                </tr>
                                                <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.services)" v-if="hasProp(cluster, 'data.spec.metadata.annotations.services')">
                                                    <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.services).length">
                                                        Services
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.services')"></span>
                                                    </td>
                                                    <td class="label">
                                                        {{ item.annotation }}
                                                    </td>
                                                    <td colspan="2">
                                                        {{ item.value }}
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </template>	
                                </div>

                                <div class="configurationDetails">
                                    <table v-if="hasProp(cluster, 'data.spec.scheduling.nodeSelector')">
                                        <thead>
                                            <th colspan="2" class="label">
                                                Node Selector
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.nodeSelector')"></span>
                                            </th>
                                        </thead>
                                        <tbody>
                                            <tr v-for="(item, index) in unparseProps(cluster.data.spec.scheduling.nodeSelector)">
                                                <td class="label">
                                                    {{ item.annotation }}
                                                </td>
                                                <td colspan="2">
                                                    {{ item.value }}
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                
                                    <table v-if="hasProp(cluster, 'data.spec.scheduling.tolerations')">
                                        <thead>
                                            <th colspan="3" class="label">
                                                Tolerations
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations')"></span>
                                            </th>
                                        </thead>
                                        <tbody>
                                            <template v-for="(item, index) in cluster.data.spec.scheduling.tolerations">
                                                <tr v-for="(value, prop, i) in item">
                                                    <td class="label" :rowspan="Object.keys(item).length" v-if="!i">
                                                        Toleration #{{ index+1 }}
                                                    </td>
                                                    <td class="label">
                                                        {{ prop }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations[prop]')"></span>
                                                    </td>
                                                    <td colspan="2">
                                                        {{ value }}
                                                    </td>
                                                </tr>
                                            </template>
                                        </tbody>
                                    </table>
                                
                                </div>
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>
        <v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < clusters.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="clusters.length" @page-change="pageChange" align="center" ref="page"></v-page>
        <div id="nameTooltip">
            <div class="info"></div>
        </div>
    </div>
</template>

<script>
    import { mixin } from './mixins/mixin'
    import router from '../router'
    import store from '../store'

    export default {
        name: 'LogsServer',

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
        methods: {
            
            unparseProps ( props, key = 'annotation' ) {
                var propsArray = [];
                if(!$.isEmptyObject(props)) {
                    Object.entries(props).forEach(([k, v]) => {
                        var prop = {};
                        prop[key] = k;
                        prop['value'] = v;
                        propsArray.push(prop)
                    });
                }
                
                return propsArray
            },

        },
        
        mounted: function() {

            const vc = this

            // Show configurations details when the row is clicked
			$(document).on('click', 'table.logsCluster tr.base > td:not(.actions)', function(){    
				const table = $(this).parents('table');
				if(!$(this).parent().hasClass('open')) {
					if(table.hasClass('configurations'))
						router.push('/configurations/'+table.prop('id')+'/'+vc.$route.params.namespace+'/'+$(this).parent().data('name'))
					else
						router.push('/'+table.prop('id')+'/'+vc.$route.params.namespace+'/'+$(this).parent().data('name'))
				} else {
					if(table.hasClass('configurations'))
						router.push('/configurations/'+table.prop('id')+'/'+vc.$route.params.namespace)
					else
						router.push('/'+$(this).parents('table').prop('id')+'/'+vc.$route.params.namespace)
				}
			})

        },
        computed: {

            clusters () {
                return this.sortTable([...(store.state.logsClusters.filter(cluster => (cluster.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
            },
            
            tooltips() {
                return store.state.tooltips
            }
        }
    }
</script>