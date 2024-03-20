<template>
    <div v-if="iCanLoad">
        <div class="content">
            <h2>
                Roles
                <router-link 
                    title="Add New Role"
                    class="add floatRight"
                    v-if="iCan('create', 'roles', 'any')"
                    to="/manage/roles/new"
                    data-field="CreateRole"
                >
                    Add New
                </router-link>
            </h2>
            <template v-if="iCan('list', 'roles', 'all')">
                <div class="tableContainer">
                    <table id="roles" class="roles resizable fullWidth" v-columns-resizable>
                        <thead class="sort">
                            <th class="sorted desc name hasTooltip">
                                <span @click="sort('metadata.name')" title="Name">
                                    Name
                                </span>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgrole.metadata.name')"></span>
                            </th>
                            <th class="sorted desc namespace hasTooltip">
                                <span @click="sort('metadata.namespace')" title="Name">
                                    Namespace
                                </span>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgrole.metadata.namespace')"></span>
                            </th>                            
                            <template v-for="ruleSpec in roleRuleSpecs">
                                <th
                                    :key="'roleRuleSpecHeader-' + ruleSpec"
                                    class="notSortable hasTooltip"
                                    :class="ruleSpec"
                                >
                                    <span :title="splitUppercase(ruleSpec).replace(/([A-Z]) /g, '$1').trim()">
                                        {{ splitUppercase(ruleSpec).replace(/([A-Z]) /g, '$1').trim() }}
                                    </span>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgrole.rules.' + ruleSpec)"></span>
                                </th>
                            </template>
                            <th class="actions"></th>
                        </thead>
                        <tbody>
                            <template v-if="!rolesList.length">
                                <tr class="no-results">
                                    <td colspan="999">
                                        <template v-if="iCan('create', 'roles', $route.params.namespace)">
                                            No Roles have been found, would you like to <router-link to="/manage/roles/new" title="Add New Role">create a new one?</router-link>
                                        </template>
                                        <template v-else>
                                            No Roles have been found. You don't have enough permissions to create a new one
                                        </template>
                                    </td>
                                </tr>
                            </template>
                            <template v-for="(role, index) in rolesList">
                                <template v-if="role.hasOwnProperty('rules') && !isNull(role.rules)">
                                    <template v-for="(rule, ruleIndex) in role.rules">
                                        <tr
                                            class="base"
                                            :key="'role-' + index + '-' + ruleIndex"
                                        >
                                            <template v-if="!ruleIndex">
                                                <td
                                                    class="hasTooltip name"
                                                    :rowspan="role.rules.length"
                                                >
                                                    <span>
                                                        <router-link :to="'/manage/role/' + role.metadata.name" class="noColor">
                                                            {{ role.metadata.name }}
                                                        </router-link>
                                                    </span>
                                                </td>
                                                <td
                                                    class="hasTooltip namespace"
                                                    :rowspan="role.rules.length"
                                                >
                                                    <span>
                                                        <router-link :to="'/manage/role/' + role.metadata.name" class="noColor">
                                                            {{ role.metadata.namespace }}
                                                        </router-link>
                                                    </span>
                                                </td>
                                            </template>
                                            <template v-for="ruleSpec in roleRuleSpecs">
                                                <td
                                                    :key="'roleRuleSpec-' + ruleSpec"
                                                    class="hasTooltip"
                                                    :class="ruleSpec"
                                                >
                                                    <span>
                                                        <router-link :to="'/manage/role/' + role.metadata.name" class="noColor">
                                                            {{ hasProp(rule, ruleSpec) ? rule[ruleSpec].join(', ') : '' }}
                                                        </router-link>
                                                    </span>
                                                </td>
                                            </template>
                                            <td v-if="!ruleIndex"
                                                class="actions"
                                                :rowspan="role.rules.length"
                                            >
                                                <router-link v-if="iCan('patch', 'roles', role.metadata.namespace)" :to="'/manage/role/' + role.metadata.name + '/edit'" title="Edit Role" class="editCRD" :data-crd-name="role.metadata.name"></router-link>
                                                <!-- TO-DO: Enable roles cloning -->
                                                <!-- <a v-if="iCan('create', 'roles', $route.params.namespace)" @click="cloneCRD('Roles', $route.params.namespace, role.metadata.name)" title="Clone Role" class="cloneCRD"></a> -->
                                                <a v-if="iCan('delete', 'roles', role.metadata.namespace)" @click="deleteCRD('roles', role.metadata.namespace, role.metadata.name)" title="Delete Role" class="deleteCRD"></a>
                                            </td>
                                        </tr>
                                    </template>
                                </template>
                                <template v-else>
                                    <tr
                                        class="base"
                                        :key="'role-' + role.metadata.namespace + '-' + role.metadata.name"
                                    >
                                        <td
                                            class="hasTooltip name"
                                        >
                                            <span>
                                                <router-link :to="'/manage/role/' + role.metadata.name" class="noColor">
                                                    {{ role.metadata.name }}
                                                </router-link>
                                            </span>
                                        </td>
                                        <td
                                            class="hasTooltip namespace"
                                            colspan="1"
                                        >
                                            <span>
                                                <router-link :to="'/manage/role/' + role.metadata.name" class="noColor">
                                                    {{ role.metadata.namespace }}
                                                </router-link>
                                            </span>
                                        </td>
                                        <td colspan="5">
                                            <router-link :to="'/manage/role/' + role.metadata.name" class="noColor"></router-link>
                                        </td>
                                        <td class="actions">
                                            <router-link v-if="iCan('patch', 'roles', role.metadata.namespace)" :to="'/manage/role/' + role.metadata.name + '/edit'" title="Edit Role" class="editCRD" :data-crd-name="role.metadata.name"></router-link>
                                            <!-- TO-DO: Enable roles cloning -->
                                            <!-- <a v-if="iCan('create', 'roles', $route.params.namespace)" @click="cloneCRD('Roles', $route.params.namespace, role.metadata.name)" title="Clone Role" class="cloneCRD"></a> -->
                                            <a v-if="iCan('delete', 'roles', role.metadata.namespace)" @click="deleteCRD('roles', role.metadata.namespace, role.metadata.name)" title="Delete Role" class="deleteCRD"></a>
                                        </td>
                                    </tr>
                                </template>
                            </template>
                        </tbody>
                    </table>
                </div>
            </template>
            <template v-else>
                <span class="warningText textCenter">
                    <p>You don't have enough permissions to access roles data in this namespace</p>
                </span>
            </template>

            <br/><br/>

            <h2>
                Cluster Roles
                <router-link 
                    title="Add New Role"
                    class="add floatRight"
                    to="/manage/clusterroles/new"
                    v-if="iCan('create', 'clusterroles')"
                    data-field="CreateClusterRole"
                >
                    Add New
                </router-link>
            </h2>
            <template v-if="iCan('list', 'clusterroles')">
                <div class="tableContainer">
                    <table id="clusterroles" class="roles resizable fullWidth" v-columns-resizable>
                        <thead class="sort">
                            <th class="sorted desc name hasTooltip">
                                <span @click="sort('metadata.name')" title="Name">
                                    Name
                                </span>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterrole.metadata.name')"></span>
                            </th>
                            <template v-for="ruleSpec in roleRuleSpecs">
                                <th
                                    :key="'roleRuleSpecHeader-' + ruleSpec"
                                    class="notSortable hasTooltip"
                                    :class="ruleSpec"
                                >
                                    <span :title="splitUppercase(ruleSpec).replace(/([A-Z]) /g, '$1').trim()">
                                        {{ splitUppercase(ruleSpec).replace(/([A-Z]) /g, '$1').trim() }}
                                    </span>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterrole.rules.' + ruleSpec)"></span>
                                </th>
                            </template>
                            <th class="actions"></th>
                        </thead>
                        <tbody>
                            <template v-if="!clusterRolesList.length">
                                <tr class="no-results">
                                    <td colspan="999">
                                        <template v-if="iCan('create', 'clusterroles')">
                                            No Cluster Roles have been found, would you like to <router-link to="/manage/clusterroles/new" title="Add New Role">create a new one?</router-link>
                                        </template>
                                        <template v-else>
                                            No Cluster Roles have been found. You don't have enough permissions to create a new one
                                        </template>
                                    </td>
                                </tr>
                            </template>
                            <template v-for="(role, index) in clusterRolesList">
                                <template v-if="role.hasOwnProperty('rules') && !isNull(role.rules)">
                                    <template v-for="(rule, ruleIndex) in role.rules">
                                        <tr
                                            class="base"
                                            :key="'clusterrole-' + index + '-' + ruleIndex"
                                        >
                                            <td
                                                v-if="!ruleIndex"
                                                class="hasTooltip name"
                                                :rowspan="role.rules.length"
                                            >
                                                <span>
                                                    <router-link :to="'/manage/clusterrole/' + role.metadata.name" class="noColor">
                                                        {{ role.metadata.name }}
                                                    </router-link>
                                                </span>
                                            </td>
                                            <template v-for="ruleSpec in roleRuleSpecs">
                                                <td
                                                    :key="'roleRuleSpec-' + ruleSpec"
                                                    class="hasTooltip"
                                                    :class="ruleSpec"
                                                >
                                                    <span>
                                                        <router-link :to="'/manage/clusterrole/' + role.metadata.name" class="noColor">
                                                            {{ hasProp(rule, ruleSpec) ? rule[ruleSpec].join(', ') : '' }}
                                                        </router-link>
                                                    </span>
                                                </td>
                                            </template>
                                            <td v-if="!ruleIndex"
                                                class="actions"
                                                :rowspan="role.rules.length"
                                            >
                                                <router-link v-if="iCan('patch', 'clusterroles')" :to="'/manage/clusterrole/' + role.metadata.name + '/edit'" title="Edit Cluster Role" class="editCRD" :data-crd-name="role.metadata.name"></router-link>
                                                <!-- TO-DO: Enable cluster roles cloning -->
                                                <!-- <a v-if="iCan('create', 'clusterroles')" @click="cloneCRD('ClusterRoles', $route.params.namespace, role.metadata.name)" title="Clone Cluster Role" class="cloneCRD"></a>-->
                                                <a v-if="iCan('delete', 'clusterroles')" @click="deleteCRD('clusterroles', null, role.metadata.name)" title="Delete Cluster Role" class="deleteCRD" :data-crd-name="role.metadata.name"></a>
                                            </td>
                                        </tr>
                                    </template>
                                </template>
                                <template v-else>
                                    <tr
                                        class="base"
                                        :key="'role-' + role.metadata.namespace + '-' + role.metadata.name"
                                    >
                                        <td
                                            class="hasTooltip name"
                                        >
                                            <span>
                                                <router-link :to="'/manage/clusterrole/' + role.metadata.name" class="noColor">
                                                    {{ role.metadata.name }}
                                                </router-link>
                                            </span>
                                        </td>
                                        <td
                                            class="hasTooltip namespace"
                                            colspan="1"
                                        >
                                            <span>
                                                <router-link :to="'/manage/role/' + role.metadata.name" class="noColor">
                                                    {{ role.metadata.namespace }}
                                                </router-link>
                                            </span>
                                        </td>
                                        <td colspan="4">
                                            <router-link :to="'/manage/clusterrole/' + role.metadata.name" class="noColor"></router-link>
                                        </td>
                                        <td class="actions">
                                            <router-link v-if="iCan('patch', 'clusterroles')" :to="'/manage/clusterrole/' + role.metadata.name + '/edit'" title="Edit Cluster Role" class="editCRD" :data-crd-name="role.metadata.name"></router-link>
                                                <!-- TO-DO: Enable cluster roles cloning -->
                                                <!-- <a v-if="iCan('create', 'clusterroles')" @click="cloneCRD('ClusterRoles', $route.params.namespace, role.metadata.name)" title="Clone Cluster Role" class="cloneCRD"></a>-->
                                                <a v-if="iCan('delete', 'clusterroles')" @click="deleteCRD('clusterroles', null, role.metadata.name)" title="Delete Cluster Role" class="deleteCRD" :data-crd-name="role.metadata.name"></a>
                                        </td>
                                    </tr>
                                </template>
                            </template>
                        </tbody>
                    </table>
                </div>
            </template>
            <template v-else>
                <span class="warningText textCenter">
                    <p>You don't have enough permissions to access roles data in this namespace</p>
                </span>
            </template>

            <br/><br/>

            <h2>
                Users
                <router-link 
                    title="Add New user"
                    class="add floatRight"
                    to="/manage/users/new"
                    v-if="havePermissionsTo.create.users"
                    data-field="CreateUser"
                >
                    Add New
                </router-link>
            </h2>
            <template v-if="havePermissionsTo.get.users">
                <div class="tableContainer">
                    <table id="users" class="resizable fullWidth" v-columns-resizable>
                        <thead class="sort">
                            <th class="sorted desc name hasTooltip">
                                <span @click="sort('metadata.name')" title="Name">
                                    Name
                                </span>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sguser.metadata.name')"></span>
                            </th>
                            <th class="sorted desc k8sUsername hasTooltip">
                                <span @click="sort('k8sUsername')" title="K8s Username">
                                    K8s Username
                                </span>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sguser.k8sUsername')"></span>
                            </th>
                            <th class="sorted desc apiUsername hasTooltip">
                                <span @click="sort('apiUsername')" title="API Username">
                                    API Username
                                </span>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sguser.apiUsername')"></span>
                            </th>
                            <th class="roles hasTooltip notSortable">
                                <span title="Roles">
                                    Roles
                                </span>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sguser.roles')"></span>
                            </th>
                            <th class="clusterRoles hasTooltip notSortable">
                                <span title="Cluster Roles">
                                    Cluster Roles
                                </span>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sguser.clusterRoles')"></span>
                            </th>
                            <th class="actions"></th>
                        </thead>
                        <tbody>
                            <template v-if="!usersList.length">
                                <tr class="no-results">
                                    <td colspan="999">
                                        <template v-if="iCan('create','sgpgconfigs',$route.params.namespace)">
                                            No Users have been found, would you like to <router-link to="/manage/users/new" title="Add New User">create a new one?</router-link>
                                        </template>
                                        <template v-else>
                                            No Users have been found. You don't have enough permissions to create a new one
                                        </template>
                                    </td>
                                </tr>
                            </template>
                            <template v-for="(user, index) in usersList">
                                <tr
                                    class="base"
                                    :key="'user-' + index"
                                >
                                    <td class="hasTooltip name">
                                        <span>
                                            <router-link :to="'/manage/user/' + user.metadata.name" class="noColor">
                                                {{ user.metadata.name }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="hasTooltip k8sUsername">
                                        <span>
                                            <router-link :to="'/manage/user/' + user.metadata.name" class="noColor">
                                                {{ hasProp(user, 'k8sUsername') ? user.k8sUsername : '' }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="hasTooltip apiUsername">
                                        <span>
                                            <router-link :to="'/manage/user/' + user.metadata.name" class="noColor">
                                                {{ hasProp(user, 'apiUsername') ? user.apiUsername : '' }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="hasTooltip roles">
                                        <span>
                                            <router-link :to="'/manage/user/' + user.metadata.name" class="noColor">
                                                {{ hasProp(user, 'roles') && user.roles.map(r => r.name).join(', ') }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="hasTooltip clusterRoles">
                                        <span>
                                            <router-link :to="'/manage/user/' + user.metadata.name" class="noColor">
                                                {{ hasProp(user, 'clusterRoles') && user.clusterRoles.map(r => r.name).join(', ') }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="actions">
                                        <router-link v-if="havePermissionsTo.patch.users" :to="'/manage/user/' + user.metadata.name + '/edit'" title="Edit User" class="editCRD" :data-crd-name="user.metadata.name"></router-link>
                                        <!-- TO-DO: Enable users cloning -->
                                        <!-- <a v-if="havePermissionsTo.create.users" @click="cloneCRD('Users', $route.params.namespace, user.metadata.name)" title="Clone User" class="cloneCRD"></a>-->
                                        <a v-if="havePermissionsTo.delete.users" @click="deleteCRD('users', user.metadata.namespace, user.metadata.name)" title="Delete User" class="deleteCRD"></a>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
            </template>
            <template v-else>
                <span class="warningText textCenter">
                    <p>You don't have enough permissions to access users data in this namespace</p>
                </span>
            </template>
            <div id="nameTooltip">
                <div class="info"></div>
            </div>
		</div>
    </div>
</template>

<script>
    import sgApi from '../../api/sgApi'
    import store from '@/store'
    import { mixin } from '../mixins/mixin'
    
    export default {
        name: 'UserManagement',

        mixins: [mixin],

        data() {
            return {
                currentSort: {
					param: 'metadata.name',
					type: 'alphabetical'
				},
				currentSortDir: 'asc',
                roleRuleSpecs: ['apiGroups', 'nonResourceURLs', 'resourceNames', 'resources', 'verbs']
            }
        },

        mounted() {
            const vc = this;

            ['Users', 'Roles', 'ClusterRoles'].forEach( (kind) => {
                sgApi
                .get(kind.toLowerCase())
                .then( (response) => {
                    store.commit('set' + kind, response.data)
                })
                .catch(function(err) {
                    console.log(err);
                    vc.checkAuthError(err);
                });
            })
        },

        computed: {

            rolesList() {
                return store.state.roles
            },

            clusterRolesList() {
                return store.state.clusterroles
            },

            usersList() {
                return store.state.users
            }
        }
        
    }
</script>

<style scoped>
    table.roles td.actions {
        vertical-align: top;
        padding-top: 13px;
    }

    .tableContainer {
        max-height: 28vh;
    }

    th.actions, td.actions {
        width: 75px !important;
        min-width: 75px;
        max-width: 75px;
    }
</style>