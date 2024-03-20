<template>
    <header id="header" v-if="loggedIn && !notFound" :class="(($route.meta.componentName == 'SGCluster') && ((!$route.name.includes('Create') && (!$route.name.includes('Edit')) && ($route.name != 'ClusterOverview')))? 'clusterHeader' : '')">
        <ul class="breadcrumbs" v-if="!['SGConfig', 'User', 'Role', 'ClusterRole'].includes($route.meta.componentName)">

            <!--Namespace-->
            <li class="namespace" v-if="$route.name.includes('GlobalDashboard') || $route.params.hasOwnProperty('namespace')">
                <template v-if="$route.name.includes('GlobalDashboard')">
                    Namespaces Overview
                </template>
                <template v-else>
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <template v-if="$route.name == 'NamespaceOverview'">
                        {{ currentPath.namespace }}
                    </template>
                    <template v-else>
                        <router-link :to="'/' + currentPath.namespace" title="Namespace Overview">
                            {{ currentPath.namespace }}
                        </router-link>
                    </template>
                </template>
            </li>

            <template v-if="!notFound && !$route.params.hasOwnProperty('namespace') && iCan('create', 'namespaces') && ($route.name !== 'CreateNamespace')">
                <router-link to="/namespaces/new" class="floatRight add" data-field="CreateNamespace">
                    Create Namespace
                </router-link>
            </template>
            <template v-else-if="$route.name === 'CreateNamespace'">
                Namespaces
            </template>

            <template v-if="$route.params.hasOwnProperty('namespace') && $route.meta.hasOwnProperty('componentName')">
                <!--Kind-->
                <template v-if="!['NamespaceOverview', 'GlobalDashboard'].includes($route.name)">
                    <li>
                        <span class="component" :class="$route.meta.componentName.toLowerCase()"></span>

                        <template v-if="($route.meta.componentName !== 'SGConfig') && (currentPath.name || $route.name.startsWith('Create') || $route.params.hasOwnProperty('backupname'))">
                            <template v-if="( ( (kind === 'users') && havePermissionsTo.get.users ) || iCan('list', kind, $route.params.namespace))">
                                <router-link :to="'/' + currentPath.namespace + '/' + kind" :title="$route.meta.componentName + 's'">
                                    {{ $route.meta.hasOwnProperty('customComponentName') ? $route.meta.customComponentName + 's' : $route.meta.componentName + 's' }}
                                </router-link>
                            </template>
                            <template v-else>
                                {{ $route.meta.hasOwnProperty('customComponentName') ? $route.meta.customComponentName + 's' : $route.meta.componentName + 's' }}
                            </template>
                        </template>
                        <template v-else>
                            {{ $route.meta.hasOwnProperty('customComponentName') ? $route.meta.customComponentName + 's' : $route.meta.componentName + 's' }}
                        </template>
                    </li>
                </template>
                
                <!--CRD Name-->
                <template v-if="currentPath.hasOwnProperty('name') && currentPath.name.length">
                    <li>
                        <template v-if="(currentPath.component.startsWith('Edit')) || ($route.meta.componentName == 'SGCluster') || ($route.meta.componentName == 'SGShardedCluster')">
                            <router-link :to="'/' + currentPath.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + currentPath.name" :title="currentPath.name">
                                {{ currentPath.name }}
                            </router-link>
                        </template>
                        <template v-else>
                            <span>{{ currentPath.name }}</span>
                        </template>
                    </li>
                </template>

                <!--Cluster name on Backups Tab-->
                <template v-if="$route.params.hasOwnProperty('name') && !currentPath.name">
                    <li>
                        <template v-if="currentPath.component.startsWith('Edit') || $route.name == 'CreateClusterBackup'">
                            <router-link :to="'/' + currentPath.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.name" :title="$route.params.name">
                                {{ $route.params.name }}
                            </router-link>
                        </template>
                        <template v-else>
                            <span>{{ $route.params.name }}</span>
                        </template>
                    </li>
                </template>

                <!--Cluster Tabs-->
                <template v-if="['SGCluster', 'SGShardedCluster'].includes($route.meta.componentName) && !['ClusterOverview', 'EditCluster', 'CreateCluster', 'ShardedClusterOverview', 'EditShardedCluster', 'CreateShardedCluster'].includes($route.name)">
                    <li class="back">
                        <template v-if="$route.name.includes('Backup') && $route.name != 'ClusterBackups'">
                            <router-link :to="'/' + currentPath.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + ($route.params.hasOwnProperty('name') ? $route.params.name : currentPath.name) + '/sgbackups'" title="Backups">
                                Backups
                            </router-link>
                        </template>

                        <template v-else>
                            <span>
                                <template v-if="$route.name.endsWith('Status')">Status</template>
                                <template v-else-if="$route.name.endsWith('Info') || $route.name.endsWith('Config')">Configuration</template>
                                <template v-else-if="$route.name.includes('Backup')">Backups</template>
                                <template v-else-if="$route.name.endsWith('Logs')">Logs</template>
                                <template v-else-if="$route.name.includes('Monitor')">Monitoring</template>
                                <template v-else-if="$route.name.includes('Events')">Events</template>
                            </span>
                        </template>
                    </li>
                </template>

                <!--Backup Name-->
                <template v-if="$route.params.hasOwnProperty('backupname')">
                    <li>
                        <template v-if="currentPath.component.startsWith('Edit')">
                            <template v-if="$route.params.hasOwnProperty('name')">
                                <router-link :to="'/' + currentPath.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.name + '/sgbackup/' + $route.params.backupname" :title="$route.params.backupname">
                                    {{ $route.params.backupname }}
                                </router-link>
                            </template>

                            <template v-else>
                                <router-link :to="'/' + currentPath.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.backupname" :title="$route.params.backupname">
                                    {{ $route.params.backupname }}
                                </router-link>
                            </template>   
                        </template>
                        
                        <template v-else>
                            <span>{{ $route.params.backupname }}</span>
                        </template>
                    </li>
                </template>

                <!--Create / Edit-->
                <template v-if="currentPath.component.startsWith('Edit') || currentPath.component.startsWith('Create')">
                    <li>
                        <template v-if="currentPath.component.startsWith('Edit')">
                            <span>Edit</span>
                        </template>
                        <template v-else>
                            <span>Create</span>
                        </template>
                    </li>
                </template>

                <!--Babelfish Compass-->
                <template v-if="$route.meta.componentName == 'Application'">
                    <li>
                        <span>Babelfish Compass</span>
                    </li>
                </template>
            </template>
        </ul>

        <template v-if="$route.params.hasOwnProperty('namespace') && $route.meta.hasOwnProperty('componentName')">
            <div class="actions" v-if="!['User', 'Role', 'ClusterRole'].includes($route.meta.componentName)">
                <!--Docs Links-->
                <template v-if="currentPath.component == 'BabelfishCompass'">
                    <a class="documentation" href="https://github.com/babelfish-for-postgresql/babelfish_compass/" target="_blank" title="Babelfish Compass Documentation">Babelfish Compass Documentation</a>
                </template>
                <template v-else-if="($route.meta.componentName == 'SGDistributedLog') || ($route.meta.componentName == 'SGDbOp')">
                    <a class="documentation" :href="'https://stackgres.io/doc/latest/reference/crd/' + $route.meta.componentName.toLowerCase() + 's'" target="_blank" :title="$route.meta.componentName + 's Documentation'">{{ $route.meta.componentName }}s Documentation</a>
                </template>
                <template v-else-if="$route.params.hasOwnProperty('namespace') && (kind !== 'users')">
                    <a class="documentation" :href="'https://stackgres.io/doc/latest/reference/crd/' + ($route.meta.customComponentName == 'SGPoolingConfig' ? $route.meta.customComponentName.toLowerCase() : $route.meta.componentName.toLowerCase())" target="_blank" :title="$route.meta.hasOwnProperty('customComponentName') ? $route.meta.customComponentName + ' Documentation': $route.meta.componentName + ' Documentation'">{{ $route.meta.hasOwnProperty('customComponentName') ? $route.meta.customComponentName : $route.meta.componentName }} Documentation</a>
                </template>

                <!--Actions-->
                <div class="crdActionLinks" v-if="!['Create', 'Edit'].includes($route.name)">
                    <template v-if="!$route.params.hasOwnProperty('name') && !$route.params.hasOwnProperty('backupname') && $route.name != 'BabelfishCompass'">
                        <router-link 
                            v-if="(kind !== 'users') && iCan('create', kind, $route.params.namespace)"
                            :to="'/' + $route.params.namespace + '/' + kind + '/new'" class="add" :title="'Add New ' + getSuffix($route.meta.componentName)">
                            Add New
                        </router-link>
                    </template>
                    <template v-if="($route.params.hasOwnProperty('name') || $route.params.hasOwnProperty('backupname'))">
                        <template v-if="($route.name == 'SingleClusterBackups')">
                            <router-link v-if="iCan('patch', kind, $route.params.namespace)" :to="'/' + $route.params.namespace + '/sgcluster/' +  $route.params.name + '/sgbackup/' + $route.params.backupname + '/edit'" :title="'Edit ' + getSuffix($route.meta.componentName)" :class="$route.name.includes('Script') && isDefaultScript($route.params.name) && 'disabled'">
                                Edit
                            </router-link>
                        </template>
                        <template v-else-if="($route.name == 'SingleBackups')">
                            <router-link v-if="iCan('patch', kind, $route.params.namespace)" :to="'/' + $route.params.namespace + '/sgbackup/' + $route.params.backupname + '/edit'" :title="'Edit ' + getSuffix($route.meta.componentName)" :class="$route.name.includes('Script') && isDefaultScript($route.params.name) && 'disabled'">
                                Edit
                            </router-link>
                        </template>
                        <template v-else-if="!$route.name.includes('DbOp')">
                            <router-link v-if="( ( (kind === 'users') && havePermissionsTo.patch.users ) || iCan('patch', kind, $route.params.namespace) )" :to="'/' + $route.params.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.name + '/edit'" :title="'Edit ' + getSuffix($route.meta.componentName)" :class="$route.name.includes('Script') && isDefaultScript($route.params.name) && 'disabled'">
                                Edit
                            </router-link>
                        </template>
                        <template v-if="!$route.name.includes('DbOp') && ($route.name != 'SingleBackups')">
                            <a v-if="( ( (kind === 'users') && havePermissionsTo.create.users ) || iCan('create', kind, $route.params.namespace) )" @click="cloneCRD((($route.meta.hasOwnProperty('customComponentName')) ? ($route.meta.customComponentName + 's') : ($route.meta.componentName + 's') ), $route.params.namespace, $route.params.name)" class="cloneCRD" :title="(($route.meta.componentName == 'SGCluster') ? ('Clone ' + getSuffix($route.meta.componentName) + ' Configuration') : ('Clone ' + getSuffix($route.meta.componentName)))">
                                Clone
                            </a>
                        </template>
                        <a v-if="( ( (kind === 'users') && havePermissionsTo.delete.users ) || iCan('delete', kind, $route.params.namespace) )" @click="deleteCRD(kind, $route.params.namespace, $route.params.name, '/' + $route.params.namespace + '/' + kind)" class="deleteCRD" :title="'Delete ' + getSuffix($route.meta.componentName)" :class="!isDeletable ? 'disabled' : ''">
                            Delete
                        </a>
                        <template v-if="$route.meta.componentName == 'SGCluster'">
                            <a @click="setRestartCluster($route.params.namespace, $route.params.name)" class="restartCluster lastItem" title="Restart Cluster">
                                Restart
                            </a>
                        </template>
                        
                        <router-link :to="'/' + $route.params.namespace + '/' + kind" :title="$route.meta.hasOwnProperty('customComponentName') ? 'Go to ' + $route.meta.customComponentName +'s List' : 'Go to ' + $route.meta.componentName + 's List'" class="lastItem">
                            Go to {{ $route.meta.hasOwnProperty('customComponentName') ? $route.meta.customComponentName : $route.meta.componentName }}s List
                        </router-link>
                    </template>
                </div>
            </div>
        </template>

        <!--Cluster Tabs-->
        <template v-if="['SGCluster', 'SGShardedCluster'].includes($route.meta.componentName) && !['ClusterOverview', 'ShardedClusterOverview', 'EditCluster', 'CreateCluster', 'EditShardedCluster', 'CreateShardedCluster'].includes($route.name)">
            <ul class="tabs">
                <li>
                    <router-link :to="'/' + $route.params.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.name" title="Status" class="status">Status</router-link>
                </li>
                <li>
                    <router-link :to="'/' + $route.params.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.name + '/config'" title="Configuration" class="info">Configuration</router-link>
                </li>
                <li v-if="iCan('list','sgbackups',$route.params.namespace) && ($route.meta.componentName == 'SGCluster')" :class="$route.name.includes('Backup') && 'active'">
                    <router-link :to="'/' + $route.params.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.name + '/sgbackups'" title="Backups" class="backups">Backups</router-link>
                </li>
                <li v-if="iCan('list','sgdistributedlogs',$route.params.namespace) && ($route.meta.componentName == 'SGCluster') && hasLogs">
                    <router-link :to="'/' + $route.params.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.name + '/logs'" title="Distributed Logs" class="logs">Logs</router-link>
                </li>
                <li v-if="hasMonitoring" :class="$route.name.includes('Monitor') && 'active'">
                    <router-link id="grafana-btn" :to="'/' + $route.params.namespace + '/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.name + '/monitor'" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
                </li>
                <li v-if="($route.meta.componentName == 'SGCluster')" :class="$route.name == 'SingleClusterEvents' && 'active'">
                    <router-link :to="'/' + $route.params.namespace +'/' + $route.meta.componentName.toLowerCase() + '/' + $route.params.name + '/events'" title="Events" class="events">Events</router-link>
                </li>
            </ul>
        </template>
    </header>
</template>

<script>
    import store from '../../store'
	import { mixin } from '../mixins/mixin'

    export default {
        name: 'HeaderSection',

        mixins: [mixin],

        computed: {

            kind () {
                return (this.$route.meta.componentName.toLowerCase() + 's');
            },

			currentPath () {
				return store.state.currentPath
			},

			notFound () {
				return store.state.notFound
			},

            isDeletable () {
                const vc = this;
                let c = '';
                let l = ''

                switch(vc.$route.meta.componentName) {
                    
                    case 'SGObjectStorage':
                        // Looks for a cluster that depends on this resource
                        c = store.state.sgclusters.find(c => (c.data.metadata.namespace == vc.$route.params.namespace) && ( vc.hasProp(c, 'data.spec.configurations.backups.sgObjectStorage') &&  (c.data.spec.configurations.backups.sgObjectStorage == vc.$route.params.name)))
                        // If there is any then it can't be deleted
                        return (typeof c == 'undefined')
                    case 'SGDistributedLog':
                        c = store.state.sgclusters.find(c => (c.data.metadata.namespace == vc.$route.params.namespace) && ((c.data.spec.hasOwnProperty('distributedLogs')) &&  (c.data.spec.distributedLogs.sgDistributedLogs == vc.$route.params.name)))
                        return (typeof c == 'undefined')
                    case 'SGInstanceProfile':
                        c = store.state.sgclusters.find(c => (c.data.metadata.namespace == vc.$route.params.namespace) && (c.data.spec.sgInstanceProfile == vc.$route.params.name));
                        l = store.state.sgdistributedlogs.find(l => (l.data.metadata.namespace == vc.$route.params.namespace) && (l.data.spec.sgInstanceProfile == vc.$route.params.name))
                        return ((typeof c == 'undefined') && (typeof l == 'undefined'))
                    case 'SGPgConfig':
                        c = store.state.sgclusters.find(c => (c.data.metadata.namespace == vc.$route.params.namespace) && (c.data.spec.configurations.sgPostgresConfig == vc.$route.params.name));
                        l = store.state.sgdistributedlogs.find(l => (l.data.metadata.namespace == vc.$route.params.namespace) && (l.data.spec.configurations.sgPostgresConfig == vc.$route.params.name))
                        return ((typeof c == 'undefined') && (typeof l == 'undefined'))
                    case 'SGPoolConfig':
                        c = store.state.sgclusters.find(c => (c.data.metadata.namespace == vc.$route.params.namespace) && (c.data.spec.configurations.sgPoolingConfig == vc.$route.params.name))
                        return (typeof c == 'undefined') 
                    case 'SGScript':
                        return !vc.isDefaultScript(vc.$route.params.name)
                }

                return true;
            },
            
            hasLogs () {
                const vc = this;

                let cluster = store.state.sgclusters.filter(c => (c.data.metadata.namespace == vc.$route.params.namespace) && (c.name == vc.$route.params.name))

                if((cluster.length > 0) && (cluster[0].data.spec.hasOwnProperty('distributedLogs')))
                    return true
                else
                    return false
            },

            hasMonitoring () {
                const vc = this;

                let cluster = store.state[vc.kind].find(c => (c.data.metadata.namespace == vc.$route.params.namespace) && (c.name == vc.$route.params.name) && c.data.hasOwnProperty('grafanaEmbedded') && c.data.grafanaEmbedded)

                return (typeof cluster !== 'undefined');
            }
		}, 

        methods: {

            getSuffix(crd) {

                let suffixes = {
                    'SGCluster': 'Cluster',
                    'SGShardedCluster': 'Sharded Cluster',
					'SGInstanceProfile': 'Profile',
                    'SGPgConfig': 'Configuration',
                    'SGPoolConfig': 'Configuration',
                    'SGDistributedLog': 'Logs Server',
                    'SGBackup': 'Backup',
                    'SGDbOp': 'Operation',
                    'SGObjectStorage': 'Object Storage',
                    'SGScript': 'Script'
				}

				return suffixes[crd];
            },

            isDefaultScript(scriptName) {
                const vc = this;
                let script = store.state.sgscripts.find( s => (s.data.metadata.namespace == vc.$route.params.namespace) && (s.data.metadata.name == scriptName) );

                return ( (typeof script != 'undefined') && ( script.data.status.clusters.length && (script.name == (script.data.status.clusters[0] + '-default') ) ) )
            }
        }
	}

</script>

<style scoped>

    ul.breadcrumbs {
        margin-top: 0;
    }

    .component {
        width: 20px;
        height: 20px;
        display: inline-block;
        margin-right: 10px;
        margin-top: 2px;
        background-repeat: no-repeat !important;
        background-size: contain;
    }

    .component.sgcluster {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+PHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTEwIDBDNC45IDAgLjkgMi4yMTguOSA1LjA1djExLjQ5Qy45IDE5LjI3MiA2LjYyMSAyMCAxMCAyMHM5LjEtLjcyOCA5LjEtMy40NlY1LjA1QzE5LjEgMi4yMTggMTUuMSAwIDEwIDB6bTcuMSAxMS45MDdjMCAxLjQ0NC0yLjkxNyAzLjA1Mi03LjEgMy4wNTJzLTcuMS0xLjYwOC03LjEtMy4wNTJ2LS4zNzVhMTIuODgzIDEyLjg4MyAwIDAwNy4xIDEuODIzIDEyLjg5MSAxMi44OTEgMCAwMDcuMS0xLjgyNHptMC0zLjZjMCAxLjQ0My0yLjkxNyAzLjA1Mi03LjEgMy4wNTJzLTcuMS0xLjYxLTcuMS0zLjA1M3YtLjA2OEExMi44MDYgMTIuODA2IDAgMDAxMCAxMC4xYTEyLjc5NCAxMi43OTQgMCAwMDcuMS0xLjg2MnpNMTAgOC4xYy00LjE4NSAwLTcuMS0xLjYwNy03LjEtMy4wNVM1LjgxNSAyIDEwIDJzNy4xIDEuNjA4IDcuMSAzLjA1MVMxNC4xODUgOC4xIDEwIDguMXptLTcuMSA4LjQ0di0xLjQwN2ExMi44OSAxMi44OSAwIDAwNy4xIDEuODIzIDEyLjg3NCAxMi44NzQgMCAwMDcuMTA2LTEuODI3bC4wMDYgMS4zNDVDMTYuOTU2IDE2Ljg5NCAxNC41MzEgMTggMTAgMThjLTQuODIyIDAtNi45OS0xLjE5MS03LjEtMS40NnoiLz48L3N2Zz4=);
        transform: scale(.8) translateY(-3px);
    }

    .component.sgshardedcluster {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+PGcgZmlsbD0iIzM2QThGRiI+PHBhdGggZD0ibTE5IDE1LjMtMS40LTEuMmMtLjQtLjMtMS0uMy0xLjMuMS0uMy40LS4zIDEgLjEgMS4zaC4xbC4yLjItNS42IDIuMXYtNC4xbC4yLjFjLjEuMS4zLjEuNS4xLjMgMCAuNi0uMi44LS41LjMtLjQuMS0xLS4zLTEuM2wtMS42LS45Yy0uMy0uMi0uNi0uMi0uOSAwbC0xLjYuOWMtLjQuMy0uNi44LS4zIDEuMy4yLjQuOC42IDEuMi40bC4yLS4xdjRsLTUuNi0yLjEuMi0uMmMuNC0uMy41LS45LjItMS4zcy0uOS0uNS0xLjMtLjJMMSAxNS4zYy0uMi4yLS40LjUtLjMuOUwxIDE4Yy4xLjUuNi44IDEuMS44LjQtLjEuOC0uNS44LS45di0uNWw2LjkgMi41YzAgLjEuMS4xLjIuMWguMWMuMSAwIC4yIDAgLjMtLjFsNi44LTIuNXYuM2MtLjEuNS4zIDEgLjggMS4xaC4yYy40IDAgLjgtLjMuOS0uOGwuMy0xLjhjLS4xLS4zLS4yLS43LS40LS45Ii8+PHBhdGggZD0iTTEwIDBDNC45IDAgLjkgMi4yLjkgNS4xdjYuM2MwIC42LjQgMSAxIDFoLjJjLjQgMCAuOC0uMy44LS44LjEuMS4yLjEuNC4yaC4xYy4xIDAgLjEuMS4yLjEuMS4xLjMuMS40LjIuMSAwIC4xLjEuMi4xcy4xIDAgLjIuMWguMWMuMS4xLjIuMS4zLjEuMSAwIC4yLjEuMy4xLjQgMCAuOC0uMy45LS42IDAtLjEgMC0uMS4xLS4yLjEtLjUtLjItLjktLjYtMS4xLS4yLS4xLS40LS4yLS42LS4yLS4zLS4xLS42LS4zLS45LS41LS4xLS4xLS4yLS4xLS4yLS4ybC0uMS0uMWMtLjItLjEtLjQtLjMtLjUtLjUtLjItLjItLjItLjQtLjMtLjZ2LS4zYzIuMSAxLjMgNC42IDIgNy4xIDEuOSAyLjUuMSA1LS42IDcuMS0xLjl2LjJjMCAuMi0uMS41LS4zLjctLjEuMi0uMy40LS41LjVsLS4xLjFjLS4xLjEtLjIuMS0uMy4yLS4zLjItLjYuMy0uOS41LS4yLjEtLjQuMi0uNi4yLS40LjItLjcuNi0uNiAxLjEgMCAuMSAwIC4xLjEuMi4xLjQuNS42LjkuNi4xIDAgLjIgMCAuNC0uMS4xLS4xLjItLjEuNC0uMWguMWMuMSAwIC4xLS4xLjItLjFzLjEtLjEuMi0uMWMuMS0uMS4yLS4xLjQtLjIuMSAwIC4xLS4xLjItLjFoLjFjLjEtLjEuMy0uMS40LS4yIDAgLjQuMy44LjguOGguMmMuNiAwIDEtLjQgMS0xVjUuMUMxOS4xIDIuMiAxNS4xIDAgMTAgMG0wIDguMUM1LjggOC4xIDIuOSA2LjUgMi45IDVTNS44IDIgMTAgMnM3LjEgMS42IDcuMSAzLjEtMi45IDMtNy4xIDMiLz48L2c+PC9zdmc+);
        transform: scale(.8) translateY(-3px);
    }

    .component.sginstanceprofile {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+PHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTEwIDBoLjNsOC44IDNjLjUuMi44LjcuNiAxLjItLjEuMy0uMy41LS42LjZsLTguOCAyLjljLS4yLjEtLjQuMS0uNiAwTC45IDQuOUMuNCA0LjcuMSA0LjEuMyAzLjZjLjEtLjMuMy0uNS42LS42TDkuNy4xYy4xLS4xLjItLjEuMy0uMXptNS43IDMuOUwxMCAyIDQuMyAzLjkgMTAgNS44bDUuNy0xLjl6TTEuMiA2LjJjLjEgMCAuMiAwIC4zLjFsNy4zIDIuNGMuNC4xLjcuNS43LjlWMTljMCAuNS0uNCAxLTEgMS0uMSAwLS4yIDAtLjMtLjFMLjkgMTcuNWMtLjQtLjEtLjctLjUtLjctLjlWNy4yYzAtLjYuNC0xIDEtMXptNi4yIDQuMUwyLjEgOC42djcuM2w1LjMgMS44di03LjR6bTExLjQtNC4xYy41IDAgMSAuNCAxIDF2OS40YzAgLjQtLjMuOC0uNy45bC03LjMgMi40Yy0uNS4yLTEuMS0uMS0xLjItLjYgMC0uMS0uMS0uMi0uMS0uM1Y5LjZjMC0uNC4zLS44LjctLjlsNy4zLTIuNGMuMS0uMS4yLS4xLjMtLjF6bS0uOSA5LjdWOC42bC01LjMgMS44djcuM2w1LjMtMS44eiIvPjwvc3ZnPg==);
        transform: scale(.8) translateY(-3px);
    }

    .component.sgpgconfig {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNi43IDIwIj48cGF0aCBmaWxsPSIjMzZBOEZGIiBkPSJNMTAuOTQ2IDE4LjdhLjg0MS44NDEgMCAwIDEtLjYyMi0uMjM0Ljg2Mi44NjIgMCAwIDEtLjIzNC0uNjM1di03LjgxN2EuOC44IDAgMCAxIC4yMjEtLjYuODM0LjgzNCAwIDAgMSAuNjA4LS4yMTRoMy4yOWEzLjQgMy40IDAgMCAxIDIuMzUzLjc1NSAyLjcgMi43IDAgMCAxIC44NDMgMi4xMiAyLjcyIDIuNzIgMCAwIDEtLjg0MyAyLjEyNiAzLjM3OSAzLjM3OSAwIDAgMS0yLjM1My43NjRoLTIuMzk0djIuODc1YS44LjggMCAwIDEtLjg2OS44Njd6TTE0IDEzLjYzN3ExLjc3OCAwIDEuNzc4LTEuNTUxVDE0IDEwLjUzNWgtMi4xOHYzLjF6bTExLjk2OC0uMTA3YS42ODMuNjgzIDAgMCAxIC40OTQuMTgxLjYyNS42MjUgMCAwIDEgLjE5MS40Nzd2Mi44NzVhMS43MTcgMS43MTcgMCAwIDEtLjE2Ljg3IDEuMTc0IDEuMTc0IDAgMCAxLS42NTUuNDE0IDYuODgyIDYuODgyIDAgMCAxLTEuMjQyLjI5NCA5LjAyMyA5LjAyMyAwIDAgMS0xLjM2NC4xMDcgNS4yNTIgNS4yNTIgMCAwIDEtMi41MjctLjU3MyAzLjg4MyAzLjg4MyAwIDAgMS0xLjYzOC0xLjY2NSA1LjU0OCA1LjU0OCAwIDAgMS0uNTY5LTIuNiA1LjUgNS41IDAgMCAxIC41NjktMi41NzUgMy45NjQgMy45NjQgMCAwIDEgMS42MTEtMS42NzEgNC45NjUgNC45NjUgMCAwIDEgMi40NTUtLjU5IDQuNjIgNC42MiAwIDAgMSAzLjA4OSAxLjAxNiAxLjA1OCAxLjA1OCAwIDAgMSAuMjM0LjI5NC44NTQuODU0IDAgMCAxLS4wODcuODQzLjQ3OS40NzkgMCAwIDEtLjM4OC4yLjczNy43MzcgMCAwIDEtLjI2Ny0uMDQ3IDEuNSAxLjUgMCAwIDEtLjI4MS0uMTUzIDQuMjMyIDQuMjMyIDAgMCAwLTEuMS0uNTgyIDMuNjQ4IDMuNjQ4IDAgMCAwLTEuMTQ2LS4xNjcgMi43NDcgMi43NDcgMCAwIDAtMi4yLjg1OSAzLjgzNCAzLjgzNCAwIDAgMC0uNzQyIDIuNTYxcTAgMy40NzcgMy4wNDkgMy40NzdhNi43NTIgNi43NTIgMCAwIDAgMS44MTUtLjI1NHYtMi4zNmgtMS41MTdhLjczNy43MzcgMCAwIDEtLjUtLjE2MS42NjQuNjY0IDAgMCAxIDAtLjkwOS43MzIuNzMyIDAgMCAxIC41LS4xNjF6TS45NTUgNC43NjJoMTAuNWEuOTUzLjk1MyAwIDEgMCAwLTEuOUguOTU1YS45NTMuOTUzIDAgMSAwIDAgMS45ek0xNC44IDcuNjE5YS45NTQuOTU0IDAgMCAwIC45NTUtLjk1MlY0Ljc2Mmg0LjNhLjk1My45NTMgMCAxIDAgMC0xLjloLTQuM1YuOTUyYS45NTUuOTU1IDAgMCAwLTEuOTA5IDB2NS43MTVhLjk1My45NTMgMCAwIDAgLjk1NC45NTJ6TS45NTUgMTAuOTUyaDQuM3YxLjlhLjk1NS45NTUgMCAwIDAgMS45MDkgMFY3LjE0M2EuOTU1Ljk1NSAwIDAgMC0xLjkwOSAwdjEuOWgtNC4zYS45NTMuOTUzIDAgMSAwIDAgMS45em02LjY4MSA0LjI4NkguOTU1YS45NTMuOTUzIDAgMSAwIDAgMS45MDVoNi42ODFhLjk1My45NTMgMCAxIDAgMC0xLjkwNXoiLz48L3N2Zz4=);
    }

    .component.sgpoolconfig {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNi41IDIwIj48cGF0aCBmaWxsPSIjMzZBOEZGIiBkPSJNMTQuMzA1IDE4Ljc0OWE0LjcgNC43IDAgMCAxLTIuMzg4LS41ODkgMy45MSAzLjkxIDAgMCAxLTEuNTcxLTEuNjg1IDUuNjY4IDUuNjY4IDAgMCAxLS41NDYtMi41NjggNS42MzkgNS42MzkgMCAwIDEgLjU0OC0yLjU2MSAzLjkxNiAzLjkxNiAwIDAgMSAxLjU3MS0xLjY3OCA0LjcxNSA0LjcxNSAwIDAgMSAyLjM4OC0uNTkzIDUuMTg5IDUuMTg5IDAgMCAxIDEuNjU4LjI2MSA0LjMyNCA0LjMyNCAwIDAgMSAxLjM3OC43NTYuNzU4Ljc1OCAwIDAgMSAuMjQuMjgxLjg1OS44NTkgMCAwIDEgLjA2Ny4zNjEuNzY4Ljc2OCAwIDAgMS0uMTYuNDk1LjQ3OS40NzkgMCAwIDEtLjM4OC4yLjk4NC45ODQgMCAwIDEtLjU0OC0uMTkxIDQgNCAwIDAgMC0xLjA3LS41OTUgMy40MDUgMy40MDUgMCAwIDAtMS4xLS4xNjcgMi41NzEgMi41NzEgMCAwIDAtMi4xMDYuODY5IDMuOTQzIDMuOTQzIDAgMCAwLS43MiAyLjU2MiAzLjk2MyAzLjk2MyAwIDAgMCAuNzE2IDIuNTY4IDIuNTY4IDIuNTY4IDAgMCAwIDIuMTA2Ljg2OSAzLjE0NyAzLjE0NyAwIDAgMCAxLjA2My0uMTczIDUuMTEyIDUuMTEyIDAgMCAwIDEuMS0uNTg5IDIuMDE4IDIuMDE4IDAgMCAxIC4yNjctLjEzNC43NTEuNzUxIDAgMCAxIC4yOS0uMDQ4LjQ3Ny40NzcgMCAwIDEgLjM4OC4yLjc2Ny43NjcgMCAwIDEgLjE2LjQ5NC44NjMuODYzIDAgMCAxLS4wNjcuMzU1LjczOS43MzkgMCAwIDEtLjI0LjI4NiA0LjMwOCA0LjMwOCAwIDAgMS0xLjM3OC43NTcgNS4xNjEgNS4xNjEgMCAwIDEtMS42NTguMjU3em01LjcxLS4wNGEuODQxLjg0MSAwIDAgMS0uNjIyLS4yMzQuODU2Ljg1NiAwIDAgMS0uMjM0LS42MzZ2LTcuODI0YS44LjggMCAwIDEgLjIyLS42LjgzNS44MzUgMCAwIDEgLjYwOS0uMjE0aDMuMjlhMy40IDMuNCAwIDAgMSAyLjM1NC43NTUgMi43IDIuNyAwIDAgMSAuODQyIDIuMTIgMi43MjUgMi43MjUgMCAwIDEtLjg0MiAyLjEyNyAzLjM4NiAzLjM4NiAwIDAgMS0yLjM1NC43NjRoLTIuMzkzdjIuODc1YS44LjggMCAwIDEtLjg3Ljg2OHptMy4wNS01LjA2OXExLjc3OSAwIDEuNzc5LTEuNTUydC0xLjc3OS0xLjU1MWgtMi4xOHYzLjF6TS45NTUgNC43NjJoMTAuNWEuOTUzLjk1MyAwIDEgMCAwLTEuOUguOTU1YS45NTMuOTUzIDAgMSAwIDAgMS45ek0xNC44IDcuNjE5YS45NTQuOTU0IDAgMCAwIC45NTUtLjk1MlY0Ljc2Mmg0LjNhLjk1My45NTMgMCAxIDAgMC0xLjloLTQuM1YuOTUyYS45NTUuOTU1IDAgMCAwLTEuOTA5IDB2NS43MTVhLjk1My45NTMgMCAwIDAgLjk1NC45NTJ6TS45NTUgMTAuOTUyaDQuM3YxLjlhLjk1NS45NTUgMCAwIDAgMS45MDkgMFY3LjE0M2EuOTU1Ljk1NSAwIDAgMC0xLjkwOSAwdjEuOWgtNC4zYS45NTMuOTUzIDAgMSAwIDAgMS45em02LjY4MSA0LjI4NkguOTU1YS45NTMuOTUzIDAgMSAwIDAgMS45MDVoNi42ODFhLjk1My45NTMgMCAxIDAgMC0xLjkwNXoiLz48L3N2Zz4=);
    }

    .component.sgdistributedlog {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+PGcgZmlsbD0iIzM2QThGRiI+PHBhdGggZD0iTTE5IDE1SDVjLS42IDAtMS0uNC0xLTFzLjQtMSAxLTFoMTRjLjYgMCAxIC40IDEgMXMtLjQgMS0xIDF6TTEgMTVjLS42IDAtMS0uNC0xLTFzLjQtMSAxLTEgMSAuNCAxIDEtLjQgMS0xIDF6TTE5IDExSDVjLS42IDAtMS0uNC0xLTFzLjQtMSAxLTFoMTRjLjYgMCAxIC40IDEgMXMtLjQgMS0xIDF6TTEgMTFjLS42IDAtMS0uNC0xLTFzLjQtMSAxLTEgMSAuNCAxIDEtLjQgMS0xIDF6TTE5IDdINWMtLjYgMC0xLS40LTEtMXMuNC0xIDEtMWgxNGMuNiAwIDEgLjQgMSAxcy0uNCAxLTEgMXpNMSA3Yy0uNiAwLTEtLjQtMS0xcy40LTEgMS0xIDEgLjQgMSAxLS40IDEtMSAxeiIvPjwvZz48L3N2Zz4=);
        transform: translateY(-2px);
    }

    .component.sgbackup {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+PGcgZmlsbD0iIzM2QThGRiI+PHBhdGggZD0iTTEwLjU1LjU1QTkuNDU0IDkuNDU0IDAgMCAwIDEuMTI1IDkuNUguNDc5YS40NTguNDU4IDAgMCAwLS4yMTQuMDUzLjUxLjUxIDAgMCAwLS4yMTQuNjcxbDEuNjIxIDMuMzgyYS40OS40OSAwIDAgMCAuMjEzLjIyMy40NzEuNDcxIDAgMCAwIC42NDQtLjIyM2wxLjYyLTMuMzgyQS41MS41MSAwIDAgMCA0LjIgMTBhLjQ5LjQ5IDAgMCAwLS40NzktLjVIMy4xYTcuNDcgNy40NyAwIDEgMSA3LjQ0OSA3Ljk3NCA3LjM5MiA3LjM5MiAwIDAgMS0zLjMzMi0uNzgxLjk4OC45ODggMCAwIDAtLjg4MyAxLjc2NyA5LjM1NiA5LjM1NiAwIDAgMCA0LjIxNS45OSA5LjQ1IDkuNDUgMCAwIDAgMC0xOC45eiIvPjxwYXRoIGQ9Ik0xMy41NTQgMTBhMyAzIDAgMSAwLTMgMyAzIDMgMCAwIDAgMy0zeiIvPjwvZz48L3N2Zz4=);
        transform: scale(.8);
        margin-top: -3px;
    }

    .component.sgdbop {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+PGcgZmlsbD0iIzM2QThGRiI+PHBhdGggZD0iTTE3LjEgMjBjLS42IDAtMS0uNS0xLTEgMC0xLjYtMS4zLTIuOC0yLjgtMi44SDYuNmMtMS42IDAtMi44IDEuMy0yLjggMi44IDAgLjYtLjUgMS0xIDFzLTEtLjUtMS0xYzAtMi43IDIuMi00LjggNC44LTQuOGg2LjdjMi43IDAgNC44IDIuMiA0LjggNC44LjEuNS0uNCAxLTEgMXpNOS45IDkuNGMtMS40IDAtMi41LTEuMS0yLjUtMi41czEuMS0yLjUgMi41LTIuNSAyLjUgMS4xIDIuNSAyLjVjLjEgMS40LTEuMSAyLjUtMi41IDIuNXptMC0zLjNjLS40IDAtLjguMy0uOC44IDAgLjQuMy44LjguOC41LS4xLjgtLjQuOC0uOCAwLS41LS4zLS44LS44LS44eiIvPjxwYXRoIGQ9Ik0xMCAxMy43aC0uMmMtMS0uMS0xLjgtLjgtMS44LTEuOHYtLjFoLS4xbC0uMS4xYy0uOC43LTIuMS42LTIuOC0uMnMtLjctMS45IDAtMi42bC4xLS4xSDVjLTEuMSAwLTItLjgtMi4xLTEuOSAwLTEuMi44LTIuMSAxLjgtMi4ySDV2LS4xYy0uNy0uOC0uNy0yIC4xLTIuOC44LS43IDEuOS0uNyAyLjcgMCAuMSAwIC4xIDAgLjItLjEgMC0uNi4zLTEuMS43LTEuNC44LS43IDIuMS0uNiAyLjguMi4yLjMuNC43LjQgMS4xdi4xaC4xYy44LS43IDIuMS0uNiAyLjguMi42LjcuNiAxLjkgMCAyLjZsLS4xLjF2LjFoLjFjLjUgMCAxIC4xIDEuNC41LjguNy45IDIgLjIgMi44LS4zLjQtLjguNi0xLjQuN2gtLjNjLjQuNC42IDEgLjYgMS41LS4xIDEuMS0xIDEuOS0yLjEgMS45LS40IDAtLjktLjItMS4yLS41bC0uMS0uMXYuMWMwIDEuMS0uOSAxLjktMS45IDEuOXpNNy45IDEwYzEgMCAxLjguOCAxLjggMS43IDAgLjEuMS4yLjIuMnMuMi0uMS4yLS4yYzAtMSAuOC0xLjggMS44LTEuOC41IDAgLjkuMiAxLjMuNS4xLjEuMi4xLjMgMHMuMS0uMiAwLS4zYy0uNy0uNy0uNy0xLjggMC0yLjUuMy0uMy44LS41IDEuMy0uNWguMWMuMSAwIC4yIDAgLjItLjEgMCAwIC4xLS4xLjEtLjJzMC0uMS0uMS0uMmMwIDAtLjEtLjEtLjItLjFoLS4yYy0uNyAwLTEuNC0uNC0xLjYtMS4xIDAtLjEgMC0uMS0uMS0uMi0uMi0uNi0uMS0xLjMuNC0xLjguMS0uMS4xLS4yIDAtLjNzLS4yLS4xLS4zIDBjLS4zLjMtLjguNS0xLjIuNS0xIDAtMS44LS44LTEuOC0xLjggMC0uMS0uMS0uMi0uMi0uMnMtLjEgMC0uMi4xYy4xLjEgMCAuMiAwIC4zIDAgLjctLjQgMS40LTEuMSAxLjctLjEgMC0uMSAwLS4yLjEtLjYuMi0xLjMgMC0xLjgtLjQtLjEtLjEtLjItLjEtLjMgMC0uMS4xLS4xLjIgMCAuMy4zLjMuNS43LjUgMS4yLjEgMS0uNyAxLjktMS43IDEuOWgtLjJjLS4xIDAtLjEgMC0uMi4xIDAtLjEgMCAwIDAgMCAwIC4xLjEuMi4yLjJoLjJjMSAwIDEuOC44IDEuOCAxLjggMCAuNS0uMi45LS41IDEuMi0uMS4xLS4xLjIgMCAuM3MuMi4xLjMgMGMuMy0uMi43LS40IDEuMS0uNGguMXoiLz48L2c+PC9zdmc+);
        transform: scale(.8) translateY(-3px);
    }

    .component.sgobjectstorage {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBkYXRhLXYtM2VjYTcxODg9IiIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB2aWV3Qm94PSIwIDAgMjYuNSAxOC44Ij48ZyBkYXRhLXYtM2VjYTcxODg9IiIgZmlsbD0iIzM2QThGRiI+PHBhdGggZGF0YS12LTNlY2E3MTg4PSIiIGQ9Ik0xIDQuOGgxMC41Yy41IDAgMS0uNCAxLTFzLS40LTEtMS0xSDFjLS41IDAtMSAuNC0xIDFzLjUgMSAxIDF6TTE0LjggNy42Yy41IDAgMS0uNCAxLTFWNC44aDQuM2MuNSAwIDEtLjQgMS0xcy0uNC0xLTEtMWgtNC4zVjFjMC0uNS0uNC0xLTEtMXMtMSAuNC0xIDF2NS43Yy4xLjUuNS45IDEgLjl6TTEgMTFoNC4zdjEuOWMwIC41LjQgMSAxIDFzMS0uNCAxLTFWNy4xYzAtLjUtLjQtMS0xLTFzLTEgLjQtMSAxVjlIMWMtLjUgMC0xIC41LTEgMXMuNC45IDEgMWMtLjEgMCAwIDAgMCAwek03LjcgMTUuM0gxYy0uNSAwLTEgLjQtMSAuOXMuNCAxIC45IDFoNi44Yy41IDAgMS0uNCAxLS45IDAtLjYtLjQtMS0xLTF6Ij48L3BhdGg+PGcgZGF0YS12LTNlY2E3MTg4PSIiPjxwYXRoIGRhdGEtdi0zZWNhNzE4OD0iIiBkPSJNMTQuMjc1IDE4LjdjLS44LjEtMS42LS4xLTIuMy0uNi0uNy0uNC0xLjItMS0xLjUtMS43LS40LS44LS42LTEuNi0uNi0yLjUgMC0uOS4yLTEuOC41LTIuNi4zLS43LjktMS4zIDEuNS0xLjcuNy0uNCAxLjUtLjYgMi4zLS42LjggMCAxLjYuMiAyLjMuNi43LjQgMS4yIDEgMS41IDEuNy41LjguNyAxLjcuNyAyLjYgMCAuOS0uMiAxLjgtLjUgMi42LS40LjctLjkgMS4yLTEuNiAxLjYtLjcuNS0xLjUuNy0yLjMuNnptMC0xLjZjLjcgMCAxLjQtLjMgMS44LS44LjUtLjcuNy0xLjYuNi0yLjQuMS0uOS0uMi0xLjctLjYtMi40LS41LS42LTEuMS0uOS0xLjgtLjlzLTEuNC4zLTEuOC45Yy0uNC43LS43IDEuNS0uNiAyLjQtLjEuOC4yIDEuNy42IDIuNC40LjUgMS4xLjggMS44Ljh6TTIyLjg3NSAxOC43Yy0uNiAwLTEuMy0uMS0xLjktLjItLjUtLjEtMS0uNC0xLjQtLjcgMC0uMS0uMS0uMi0uMi0uMy0uMS0uMi0uMS0uMy0uMS0uNXMuMS0uNC4yLS42Yy4xLS4yLjMtLjIuNC0uM2guM2MuMSAwIC4yLjEuMy4yLjMuMi43LjQgMS4xLjUuNS4zLjkuMyAxLjMuM3MuOS0uMSAxLjMtLjNjLjMtLjIuNS0uNS40LS45IDAtLjMtLjItLjUtLjQtLjctLjUtLjItMS0uNC0xLjUtLjUtLjYtLjEtMS4zLS4zLTEuOS0uNi0uNC0uMi0uOC0uNS0xLS45LS4yLS4zLS4zLS44LS4zLTEuMiAwLS41LjItMS4xLjUtMS41LjMtLjUuOC0uOCAxLjMtMS4xLjYtLjMgMS4yLS40IDEuOC0uNCAxLjEgMCAyLjEuMyAzIDFsLjMuM2MuMS4xLjEuMy4xLjQgMCAuMi0uMS40LS4yLjYtLjEuMi0uMy4yLS40LjNoLS4zYy0uMSAwLS4yLS4xLS4zLS4yLS4zLS4yLS42LS40LTEtLjUtLjQtLjEtLjctLjItMS4xLS4yLS40IDAtLjkuMS0xLjIuMy0uMy4yLS41LjUtLjQuOSAwIC4yLjEuNC4yLjUuMi4yLjQuMy42LjQuMS4xLjUuMi45LjMuOS4yIDEuNy41IDIuNSAxIC41LjQuOCAxLjEuOCAxLjcgMCAuNS0uMSAxLjEtLjQgMS41LS4zLjUtLjguOC0xLjMgMS0uNy4zLTEuMy41LTIgLjR6Ij48L3BhdGg+PC9nPjwvZz48L3N2Zz4=)
    }

    .component.sgscript {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNS42MjgiIGhlaWdodD0iMTguNjE3IiB2aWV3Qm94PSIwIDAgMjUuNjI4IDE4LjYxNyI+PHBhdGggZD0iTS05Ni43NDUsMzQuNzYyaDEwLjVhLjk1NC45NTQsMCwwLDAsLjk1NC0uOTUzLjk1My45NTMsMCwwLDAtLjk1NC0uOTUyaC0xMC41YS45NTMuOTUzLDAsMCwwLS45NTUuOTUyQS45NTQuOTU0LDAsMCwwLTk2Ljc0NSwzNC43NjJaIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSg5Ny43IC0zMCkiIGZpbGw9IiMzNkE4RkYiLz48cGF0aCBkPSJNLTgyLjksMzcuNjE5YS45NTQuOTU0LDAsMCwwLC45NTUtLjk1MnYtMS45aDQuMjk1YS45NTQuOTU0LDAsMCwwLC45NTUtLjk1My45NTMuOTUzLDAsMCwwLS45NTUtLjk1MkgtODEuOTV2LTEuOUEuOTUzLjk1MywwLDAsMC04Mi45LDMwYS45NTMuOTUzLDAsMCwwLS45NTQuOTUydjUuNzE1QS45NTMuOTUzLDAsMCwwLTgyLjksMzcuNjE5WiIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoOTcuNyAtMzApIiBmaWxsPSIjMzZBOEZGIi8+PHBhdGggZD0iTS05Ni43NDUsNDAuOTUyaDQuM3YxLjlhLjk1My45NTMsMCwwLDAsLjk1NS45NTMuOTUzLjk1MywwLDAsMCwuOTU0LS45NTNWMzcuMTQzYS45NTQuOTU0LDAsMCwwLS45NTQtLjk1My45NTQuOTU0LDAsMCwwLS45NTUuOTUzdjEuOWgtNC4zQS45NTQuOTU0LDAsMCwwLTk3LjcsNDAsLjk1My45NTMsMCwwLDAtOTYuNzQ1LDQwLjk1MloiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDk3LjcgLTMwKSIgZmlsbD0iIzM2QThGRiIvPjxwYXRoIGQ9Ik0tOTAuMDY0LDQ1LjIzOGgtNi42ODFhLjk1My45NTMsMCwwLDAtLjk1NS45NTMuOTUzLjk1MywwLDAsMCwuOTU1Ljk1Mmg2LjY4MWEuOTUzLjk1MywwLDAsMCwuOTU1LS45NTJBLjk1My45NTMsMCwwLDAtOTAuMDY0LDQ1LjIzOFoiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDk3LjcgLTMwKSIgZmlsbD0iIzM2QThGRiIvPjxwYXRoIGQ9Ik00LjE2LjExN0E2LjU2NCw2LjU2NCwwLDAsMSwyLjI2OS0uMTQ5LDQuMTY0LDQuMTY0LDAsMCwxLC44MTktLjg3MS43My43MywwLDAsMSwuNTItMS41LjczMy43MzMsMCwwLDEsLjY4My0xLjk3YS40ODEuNDgxLDAsMCwxLC4zODMtLjIsMS4wNTYsMS4wNTYsMCwwLDEsLjUzMy4xODIsMy45LDMuOSwwLDAsMCwxLjE3LjU4NSw0LjcyNCw0LjcyNCwwLDAsMCwxLjM1Mi4xODIsMi42NjIsMi42NjIsMCwwLDAsMS40NTYtLjMzMSwxLjA4NSwxLjA4NSwwLDAsMCwuNTA3LS45NjkuODMzLjgzMywwLDAsMC0uNDc1LS43NkE2LjI4Myw2LjI4MywwLDAsMCw0LjAzLTMuOGE5LjM1Niw5LjM1NiwwLDAsMS0xLjg1OS0uNTcyQTIuNjI1LDIuNjI1LDAsMCwxLDEuMDkyLTUuMiwyLjE1MiwyLjE1MiwwLDAsMSwuNzI4LTYuNDg3LDIuNDIyLDIuNDIyLDAsMCwxLDEuMTgzLTcuOTNhMywzLDAsMCwxLDEuMjY4LS45OTUsNC40OSw0LjQ5LDAsMCwxLDEuODI2LS4zNTcsNC41MzQsNC41MzQsMCwwLDEsMy4wMjkuOTg4Ljk3Ni45NzYsMCwwLDEsLjI0MS4yOC43MzguNzM4LDAsMCwxLC4wNzIuMzQ1LjczMy43MzMsMCwwLDEtLjE2My40NzQuNDgxLjQ4MSwwLDAsMS0uMzgzLjIuNzE4LjcxOCwwLDAsMS0uMjQxLS4wMzksMS45MjcsMS45MjcsMCwwLDEtLjI5Mi0uMTQzLDQuMzkyLDQuMzkyLDAsMCwwLTEuMDM0LS41NzksMy41LDMuNSwwLDAsMC0xLjIyOC0uMTg4LDIuNDA5LDIuNDA5LDAsMCwwLTEuNC4zNTcsMS4xNDUsMS4xNDUsMCwwLDAtLjUxMy45OTQuOTEyLjkxMiwwLDAsMCwuNDU1LjgxOSw1LjMzMyw1LjMzMywwLDAsMCwxLjU0Ny41MzMsMTAuNDA5LDEwLjQwOSwwLDAsMSwxLjg3OC41NzgsMi43ODYsMi43ODYsMCwwLDEsMS4xMDUuODEyLDEuOTUzLDEuOTUzLDAsMCwxLC4zODQsMS4yMzUsMi4zNTksMi4zNTksMCwwLDEtLjQ0OSwxLjQyNCwyLjkyMywyLjkyMywwLDAsMS0xLjI2MS45NjJBNC43NDMsNC43NDMsMCwwLDEsNC4xNi4xMTdabTkuMjE3LDBhNC41NzIsNC41NzIsMCwwLDEtMi4zMjEtLjU3MkEzLjgsMy44LDAsMCwxLDkuNTI5LTIuMDkzLDUuNTE2LDUuNTE2LDAsMCwxLDktNC41ODlhNS40NzUsNS40NzUsMCwwLDEsLjUzMy0yLjQ5QTMuODExLDMuODExLDAsMCwxLDExLjA1Ni04LjcxYTQuNTcyLDQuNTcyLDAsMCwxLDIuMzIxLS41NzIsNS4wMjYsNS4wMjYsMCwwLDEsMS42MTIuMjUzLDQuMTg4LDQuMTg4LDAsMCwxLDEuMzM5LjczNC43NDYuNzQ2LDAsMCwxLC4yMzQuMjczLjg0Ni44NDYsMCwwLDEsLjA2NS4zNTEuNzQ2Ljc0NiwwLDAsMS0uMTU2LjQ4MS40NjguNDY4LDAsMCwxLS4zNzcuMTk1Ljk2MS45NjEsMCwwLDEtLjUzMy0uMTgyLDMuODU1LDMuODU1LDAsMCwwLTEuMDQtLjU3OSwzLjMxNSwzLjMxNSwwLDAsMC0xLjA2Ni0uMTYzLDIuNSwyLjUsMCwwLDAtMi4wNDguODQ1LDMuODI5LDMuODI5LDAsMCwwLS43LDIuNDgzLDMuODUyLDMuODUyLDAsMCwwLC43LDIuNSwyLjUsMi41LDAsMCwwLDIuMDQ4Ljg0NSwzLjA2LDMuMDYsMCwwLDAsMS4wMzMtLjE2OSw0Ljk1OCw0Ljk1OCwwLDAsMCwxLjA3My0uNTcyLDEuODE5LDEuODE5LDAsMCwxLC4yNi0uMTMuNzI4LjcyOCwwLDAsMSwuMjczLS4wNTIuNDY4LjQ2OCwwLDAsMSwuMzc3LjIuNzQ2Ljc0NiwwLDAsMSwuMTU2LjQ4MS44NTcuODU3LDAsMCwxLS4wNjUuMzQ0LjcyOC43MjgsMCwwLDEtLjIzNC4yOCw0LjE4OCw0LjE4OCwwLDAsMS0xLjMzOS43MzVBNS4wMjYsNS4wMjYsMCwwLDEsMTMuMzc3LjExN1oiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDkuMDAxIDE4LjUpIiBmaWxsPSIjMzZBOEZGIi8+PC9zdmc+)
    }

    .component.application {
        background-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxOSAxOSI+PHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTEzLjcgMTlIOC44Yy0uNSAwLS45LS40LTEtLjkgMC0uMyAwLS43LjEtMSAwLS4xLjEtLjIuMS0uMi4yLS4zLjQtLjcuNC0xLjEgMC0uMy0uMS0uNi0uMy0uOC0uMi0uMi0uNS0uMy0uOC0uMy0uMyAwLS42LjEtLjkuMy0uMi4yLS4zLjQtLjMuNy4xLjQuMi43LjQgMS4xLjEuMS4xLjIuMS4zLjEuMy4xLjYuMS45IDAgLjUtLjUgMS0xIDFIMWMtLjYgMC0xLS40LTEtMVY1LjNjMC0uNi40LTEgMS0xaDMuNWMtLjEtLjMtLjItLjYtLjMtMXYtLjFjMC0uOC4zLTEuNi45LTIuMi42LS43IDEuNC0xIDIuMi0xIC45IDAgMS43LjMgMi4zLjkuNi42LjkgMS40LjkgMi4zdi4xYy0uMS4zLS4xLjctLjMgMWgzLjVjLjYgMCAxIC40IDEgMXYzLjVjLjMtLjEuNi0uMiAxLS4zaC4zYy44IDAgMS42LjQgMi4yIDFzLjkgMS40LjggMi4yYzAgLjgtLjQgMS42LTEgMi4yLS42LjYtMS40LjktMi4yLjhoLS4xYy0uMy0uMS0uNi0uMS0xLS4zVjE4YzAgLjYtLjQgMS0xIDF6bS0zLjUtMmgyLjV2LTMuOWMwLS41LjQtMSAxLTEgLjMgMCAuNiAwIC45LjEuMSAwIC4yLjEuMy4xLjMuMi43LjQgMS4xLjQuMyAwIC41LS4xLjctLjMuMi0uMi4zLS41LjQtLjggMC0uMy0uMS0uNi0uMy0uOC0uMi0uMi0uNS0uMy0uOC0uNC0uMy4xLS43LjItMS4xLjQtLjEuMS0uMi4xLS4yLjEtLjMuMS0uNi4xLTEgLjEtLjUgMC0uOS0uNS0uOS0xVjYuMmgtNGMtLjUgMC0xLS40LTEtMSAwLS4zIDAtLjYuMS0uOSAwIDAgLjEtLjEuMS0uMi4zLS4zLjQtLjcuNS0xLjEgMC0uMy0uMS0uNS0uMy0uNy0uMi0uMi0uNS0uMy0uOC0uMy0uNCAwLS43LjEtLjkuMy0uMi4yLS4zLjUtLjMuNy4xLjQuMi43LjQgMS4xLjEuMS4xLjIuMS4yLjEuMy4xLjcuMSAxIDAgLjUtLjUuOS0xIC45SDJWMTdoMi41Yy0uMS0uMy0uMi0uNi0uMy0xdi0uMWMwLS44LjMtMS42LjktMi4yLjYtLjYgMS40LS45IDIuMy0uOS44IDAgMS42LjMgMi4yLjkuNi42LjkgMS40LjkgMi4zdi4xbC0uMy45eiIvPjwvc3ZnPg==);
    }

    .component.sgconfig {
        background-image: url("data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+PHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTEzLjE5MyAxMEEzLjE5MyAzLjE5MyAwIDEwMTAgMTMuMmEzLjIgMy4yIDAgMDAzLjE5My0zLjJ6bS0xLjgwOSAwQTEuMzg0IDEuMzg0IDAgMTExMCA4LjYxNCAxLjM4NiAxLjM4NiAwIDAxMTEuMzg0IDEweiIgY2xhc3M9ImEiPjwvcGF0aD48cGF0aCBmaWxsPSIjMzZBOEZGIiBkPSJNMTYuOTYxIDEyLjgzNWEuNDQzLjQ0MyAwIDAxLjQ0LS4yNDYgMi42IDIuNiAwIDAwMC01LjJoLS4xMzZhLjQuNCAwIDAxLS4zMTgtLjE1Ny45ODguOTg4IDAgMDAtLjA1NS0uMTY0LjQyNy40MjcgMCAwMS4xMjItLjQ4NkEyLjYgMi42IDAgMTAxMy4zIDIuOTM3YS40MTQuNDE0IDAgMDEtLjI4Ny4xMTYuNC40IDAgMDEtLjI5Mi0uMTIuNDU1LjQ1NSAwIDAxLS4xMjMtLjM1NyAyLjU5MSAyLjU5MSAwIDAwLS43NjItMS44NCAyLjY1OSAyLjY1OSAwIDAwLTMuNjc1IDAgMi42IDIuNiAwIDAwLS43NiAxLjg0di4xMzdhLjQwNi40MDYgMCAwMS0uMTU4LjMxOCAxLjA3OCAxLjA3OCAwIDAwLS4xNjMuMDU1LjQxLjQxIDAgMDEtLjQ2NS0uMWwtLjA3Ni0uMDc3YTIuNSAyLjUgMCAwMC0xLjg1My0uNzI5IDIuNTc2IDIuNTc2IDAgMDAtMS44MjIuOCAyLjYzMiAyLjYzMiAwIDAwLjEgMy43MS40MzQuNDM0IDAgMDEuMDU4LjUuNDIzLjQyMyAwIDAxLS40MjIuMjY1IDIuNiAyLjYgMCAwMDAgNS4yaC4xMzNhLjQxLjQxIDAgMDEuMjg1LjExNy40My40MyAwIDAxLS4wMzUuNjI5bC0uMDc5LjA3OXYuMDA1QTIuNjEgMi42MSAwIDAwMyAxNy4xMzVhMi40NzkgMi40NzkgMCAwMDEuODUzLjcyOCAyLjYxNCAyLjYxNCAwIDAwMS44NDctLjgyNy40MjkuNDI5IDAgMDEuNS0uMDU3LjQxOS40MTkgMCAwMS4yNjQuNDIgMi42IDIuNiAwIDEwNS4yIDB2LS4xMzJhLjQxNC40MTQgMCAwMS4xMTYtLjI4NC40MjEuNDIxIDAgMDEuMy0uMTI2LjM1Ni4zNTYgMCAwMS4yNzguMTEzbC4xLjFhMi43MzEgMi43MzEgMCAwMDEuODUyLjcyOCAyLjYgMi42IDAgMDAyLjU1LTIuNjUgMi42MTEgMi42MTEgMCAwMC0uODI1LTEuODU3LjQuNCAwIDAxLS4wODEtLjQ0NHptLTYuMiA0LjQyMnYuMTQzYS42OTEuNjkxIDAgMDEtLjY5LjY5MS43MTguNzE4IDAgMDEtLjY5Mi0uNzg4IDIuMjg5IDIuMjg5IDAgMDAtMS40NTctMi4wOTUgMi4yNzQgMi4yNzQgMCAwMC0uOTE5LS4yIDIuNDI3IDIuNDI3IDAgMDAtMS43LjcyOC43LjcgMCAwMS0uNS4yMTMuNjUyLjY1MiAwIDAxLS40ODItLjE5NC42NzYuNjc2IDAgMDEtLjIwOC0uNDc3Ljc0OS43NDkgMCAwMS4yMTctLjUzbC4wNjQtLjA2NGEyLjMyMyAyLjMyMyAwIDAwLTEuNjU0LTMuOTM4SDIuNmEuNjkyLjY5MiAwIDAxLS40ODktMS4xOC43NTUuNzU1IDAgMDEuNTg3LS4yQTIuMjg2IDIuMjg2IDAgMDA0Ljc4OCA3LjlhMi4zMDYgMi4zMDYgMCAwMC0uNDY3LTIuNTU2bC0uMDY5LS4wNjlhLjY5My42OTMgMCAwMS40NzgtMS4xOTEuNjU1LjY1NSAwIDAxLjUuMjEzbC4wNjkuMDcxYTIuMjU3IDIuMjU3IDAgMDAyLjMzNC41MzYuOTIuOTIgMCAwMC4yNy0uMDcxIDIuMzEyIDIuMzEyIDAgMDAxLjQtMi4xMjF2LS4xMzRhLjY4Ny42ODcgMCAwMS4yLS40ODkuNzA1LjcwNSAwIDAxLjk3NyAwIC43NTEuNzUxIDAgMDEuMi41NzEgMi4zIDIuMyAwIDAwLjcwNSAxLjY0IDIuMzMxIDIuMzMxIDAgMDAxLjY0OS42NjUgMi4zNjkgMi4zNjkgMCAwMDEuNjUyLS43MTMuNjkxLjY5MSAwIDAxMS4xODEuNDg4Ljc1My43NTMgMCAwMS0uMjU5LjU0NyAyLjI1MyAyLjI1MyAwIDAwLS41MzggMi4zMzQuOTMyLjkzMiAwIDAwLjA3Mi4yNzQgMi4zMTMgMi4zMTMgMCAwMDIuMTE5IDEuNGguMTM5YS42OTEuNjkxIDAgMDEuNjkuNjkyLjcxNy43MTcgMCAwMS0uNzY4LjY5MSAyLjMxMiAyLjMxMiAwIDAwLTIuMTEzIDEuMzk1IDIuMzQ1IDIuMzQ1IDAgMDAuNTMzIDIuNjE5LjY5My42OTMgMCAwMS0uNDUgMS4xOTIuNzQ5Ljc0OSAwIDAxLS41MDYtLjE5bC0uMS0uMWEyLjQgMi40IDAgMDAtMS42NTMtLjY1NCAyLjMyNSAyLjMyNSAwIDAwLTIuMjgzIDIuMzEyek01LjUgNC4xNzd6IiBjbGFzcz0iYSI+PC9wYXRoPjwvc3ZnPg==");
    }

    .component.user, .component.role, .component.clusterrole {
        background-image: url("data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxOSIgaGVpZ2h0PSIxNS45MDkiPjxnIGZpbGw9IiMzNmE4ZmYiPjxwYXRoIGQ9Ik0xMy4zNjQgMTUuOTA5YTEgMSAwIDAgMS0xLTF2LTEuNTQ1YTIuMDkzIDIuMDkzIDAgMCAwLTIuMDkxLTIuMDkxSDQuMDkxQTIuMDkgMi4wOSAwIDAgMCAyIDEzLjM2NHYxLjU0NWExIDEgMCAwIDEtMiAwdi0xLjU0NWE0LjA5IDQuMDkgMCAwIDEgNC4wOTEtNC4wOTFoNi4xODJhNC4xIDQuMSAwIDAgMSA0LjA5MSA0LjA5MXYxLjU0NWExIDEgMCAwIDEtMSAxTTcuMTgyIDBhNC4wOTEgNC4wOTEgMCAxIDEtNC4wOTEgNC4wOTFBNC4xIDQuMSAwIDAgMSA3LjE4MiAwbTAgNi4xODJhMi4wOTEgMi4wOTEgMCAxIDAtMi4wOTEtMi4wOTEgMi4wOTMgMi4wOTMgMCAwIDAgMi4wOTEgMi4wOTFNMTggMTUuOTA5YTEgMSAwIDAgMS0xLTF2LTEuNTQ2YTIuMDkgMi4wOSAwIDAgMC0xLjU2OC0yLjAyMiAxIDEgMCAxIDEgLjUtMS45MzZBNC4wOSA0LjA5IDAgMCAxIDE5IDEzLjM2M3YxLjU0NmExIDEgMCAwIDEtMSAxIi8+PHBhdGggZD0iTTEyLjU5MSA4LjA4OWExIDEgMCAwIDEtLjI0Ny0xLjk2OSAyLjA5MSAyLjA5MSAwIDAgMCAwLTQuMDUxIDEgMSAwIDEgMSAuNS0xLjkzNyA0LjA5MSA0LjA5MSAwIDAgMSAwIDcuOTI2IDEgMSAwIDAgMS0uMjUzLjAzMSIvPjwvZz48L3N2Zz4=");
    }

</style>