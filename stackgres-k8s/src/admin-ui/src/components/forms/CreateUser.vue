<template>
    <div v-if="iCanLoad">        
        <form id="CreateUser" class="form" @submit.prevent v-if="!editMode || editReady">
            <div class="header">
                <h2>
                    <span>{{ editMode ? 'Edit' : 'Create' }} User</span>
                </h2>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">User Name <span class="req">*</span></label>
                    <input v-model="user.metadata.name" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgrole.metadata.name')"></span>
                </div>
            </div>

            <span class="warning topLeft" v-if="nameCollision && !editMode">
                There's already a <strong>User</strong> with the same name on this instance, please specify a different name for this user.
            </span>

            <div class="header">
                <h3>
                    User Information
                </h3>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="k8sUsername">K8s Username <span class="req">*</span></label>
                    <input v-model="user.k8sUsername" required data-field="k8sUsername" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip('sguser.k8sUsername')"></span>
                </div>

                <div class="col">
                    <template v-if="!editMode">
                        <label for="password">Password <span class="req">*</span></label>
                        <input v-model="user.password" type="password" required data-field="password" autocomplete="off">
                        <span class="helpTooltip" :data-tooltip="getTooltip('sguser.password')"></span>
                    </template>
                </div>

                <div class="col">
                    <label for="apiUsernameNotBlank">
                        Mandatory API Username
                    </label>  
                    <label for="apiUsernameNotBlank" class="switch yes-no" data-field="apiUsernameNotBlank">
                        Enable
                        <input type="checkbox" id="apiUsernameNotBlank" :value="!user.apiUsernameNotBlank" data-switch="NO" @change="user.apiUsernameNotBlank = !user.apiUsernameNotBlank">
                    </label>
                    <span class="helpTooltip" :data-tooltip="getTooltip('sguser.apiUsernameNotBlank')"></span>
                </div>

                <div class="col">
                    <label for="apiUsername">
                        API Username
                        <template v-if="user.apiUsernameNotBlank">
                            <span class="req">*</span>
                        </template>
                    </label>
                    <input v-model="user.apiUsername" :required="user.apiUsernameNotBlank" data-field="apiUsername" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip('sguser.apiUsername')"></span>
                </div>
            </div>

            <div class="header">
                <h3>
                    Role Binding
                </h3>
            </div>

            <template v-for="roleKind in ['roles', 'clusterRoles']">
                <fieldset
                    :key="'rolebinding-' + roleKind"
                    :data-fieldset="roleKind.toLowerCase()"
                >
                    <div class="header">
                        {{ splitUppercase(roleKind) }}
                    </div>
                    <div class="toolbar">
                        <div class="searchBar roles">
                            <label :for="roleKind + 'Keyword'">Search {{ splitUppercase(roleKind) }}</label>
                            <input :id="roleKind + 'Keyword'" v-model="searchRole[roleKind]" class="search" placeholder="Enter text..." autocomplete="off">
                            <button 
                                type="button"
                                @click="searchRole[roleKind] = ''"
                                class="btn clear border keyword"
                                v-if="searchRole[roleKind].length"
                            >
                                CLEAR
                            </button>
                        </div>
                    </div>

                    <div class="extHead">
                        <span class="install">Bind</span>
                        <span class="name">Name</span>
                        <template v-if="roleKind === 'roles'">
                            <span class="name">Namespace</span>
                        </template>
                        <span class="apiGroups">API Groups</span>
                        <span class="resources">Resources</span>
                        <span class="verbs">Verbs</span>
                    </div>

                    <ul class="extensionsList">
                        <template v-if="!rolesList[roleKind].length">
                            <li class="extension notFound">
                                {{ 
                                    searchRole[roleKind].length
                                    ? 'No roles match your search terms'
                                    : 'No ' + splitUppercase(roleKind) + ' have been found' + (roleKind === 'roles' ? ' on this namespace' : '')
                                }}
                            </li>
                        </template>
                        <template v-for="role in rolesList[roleKind]" >
                            <li 
                                class="extension"
                                :key="roleKind + '-' + role.metadata.name"
                            >
                                <label>
                                    <input
                                        type="checkbox"
                                        class="plain enableRole"
                                        @change="toggleRoleBinding(roleKind, role)"
                                        :checked="roleIsSet(roleKind, role.metadata.name)"
                                        :data-field="roleKind.toLowerCase() + '-' + role.metadata.name"
                                    />
                                </label>
                                <span class="extInfo">
                                    <span class="hasTooltip roleName">
                                        <span class="name">
                                            {{ role.metadata.name }}
                                        </span>
                                    </span>
                                    <template v-if="roleKind === 'roles'">
                                        <span class="hasTooltip roleNamespace">
                                            <span class="name">
                                                {{ role.metadata.namespace }}
                                            </span>
                                        </span>
                                    </template>
                                    <span class="hasTooltip apiGroups">
                                        <span>
                                            [
                                                <template v-for="rule in role.rules">
                                                    {{ rule.apiGroups.join(', ') }}
                                                </template>
                                            ]
                                        </span>
                                    </span>
                                    <span class="hasTooltip resources">
                                        <span>
                                            [
                                                <template v-for="rule in role.rules">
                                                    {{ rule.resources.join(', ') }}
                                                </template>
                                            ]
                                        </span>
                                    </span>
                                    <span class="hasTooltip verbs">
                                        <span>
                                            [
                                                <template v-for="rule in role.rules">
                                                    {{ rule.verbs.join(', ') }}
                                                </template>
                                            ]
                                        </span>
                                    </span>
                                </span>
                            </li>
                        </template>
                    </ul>
                </fieldset>
            </template>

            <hr/>

            <button type="submit" class="btn" @click="createUser()">{{ editMode ? 'Update' : 'Create' }} User</button>
            
            <button @click="cancel()" class="btn border">Cancel</button>
            
            <button type="button" class="btn floatRight" @click="createUser(true)">View Summary</button>
        </form>
        <CRDSummary :crd="previewCRD" kind="SGUser" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>

        <div id="nameTooltip">
            <div class="info"></div>
        </div>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateUser',

        mixins: [mixin],

        components: {
            CRDSummary
        },
        
        data: function() {

            const vc = this;

            return {
                editMode: (vc.$route.name === 'EditUser'),
                editReady: false,
                previewCRD: {},
                showSummary: false,
                user: {
                    metadata: {
                        namespace: '',
                        name: '',
                    },
                    k8sUsername: null,
                    password: null,
                    apiUsername: null,
                    apiUsernameNotBlank: false,
                    roles: null,
                    clusterRoles: null
                    
                },
                searchRole: {
                    roles: '',
                    clusterRoles: ''
                }
            }

        },
        computed: {

            userData() {
                return store.state.users.find( user => (
                        (user.metadata.name === this.user.metadata.name)
                    ))
            },

            nameCollision() {
                return !this.editMode && (typeof this.userData !== 'undefined');
            },
            
            rolesList() {
                return {
                    clusterRoles: store.state.clusterroles.filter( (role) => role.metadata.name.includes(this.searchRole.clusterRoles)),
                    roles: store.state.roles.filter( (role) => role.metadata.name.includes(this.searchRole.roles)),
                }
            },

            namespaces() {
                return store.state.allNamespaces;
            }
        },

        mounted() {
            this.initForm();
        },

        methods: {
            
            initForm() {
                const vc = this;

                sgApi
                .get('users')
                .then( (response) => {
                    store.commit('setUsers', response.data)

                    if(vc.editMode && !vc.editReady ) {
                        let user = response.data.find( user => (
                            (user.metadata.name === this.$route.params.name)
                        ));

                        if(typeof user !== 'undefined') {
                            vc.user = JSON.parse(JSON.stringify(user))
                        } else {
                            store.commit('notFound', true);
                        }
                        vc.editReady = true;
                    }
                })
                .catch(function(err) {
                    console.log(err);
                    vc.checkAuthError(err);
                });

                ['Roles', 'ClusterRoles'].forEach( (roleKind) => {
                    sgApi
                    .get(roleKind.toLowerCase())
                    .then( (response) => {
                        store.commit('set' + roleKind, response.data)
                    })
                    .catch(function(err) {
                        console.log(err);
                        vc.checkAuthError(err);
                    });
                })
            },
            
            roleIsSet(roleKind, roleName) {
                return (
                    !this.isNull(this.user[roleKind]) &&
                    ( typeof this.user[roleKind].find( (role) => 
                        (role.name === roleName)
                    ) !== 'undefined')
                )
            },

            toggleRoleBinding(roleKind, role) {
                let newRole = {
                    name: role.metadata.name,
                    ...( role.metadata.hasOwnProperty('namespace') && {
                        namespace: role.metadata.namespace
                    })
                };
                
                if(
                    !this.isNull(this.user[roleKind]) && 
                    (typeof this.user[roleKind].find( (r) => r.name === role.metadata.name) === 'undefined')
                ) {
                    this.user[roleKind].push(newRole);
                } else if(this.isNull(this.user[roleKind])) {
                    this.user[roleKind] = [newRole];
                } else {
                    this.user[roleKind] = this.user[roleKind].filter( (r) => r.name !== role.metadata.name);
                    
                    if(!this.user[roleKind].length) {
                        this.user[roleKind] = null;
                    }
                }
            },

            createUser(preview = false) {
                const vc = this;

                if(!vc.checkRequired()) {
                    return;
                }

                if(preview) {
                    vc.previewCRD = {};
                    vc.previewCRD['data'] = vc.user;
                    vc.showSummary = true;

                } else {
                    if(this.editMode) {
                        sgApi
                        .update('users', vc.user)
                        .then(function (response) {
                            vc.notify('User <strong>"' + vc.user.metadata.name + '"</strong> updated successfully', 'message', 'users');

                            vc.fetchAPI('users');
                            router.push('/manage/user/' + vc.user.metadata.name);
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data, 'error', 'users');
                        });

                    } else {
                        sgApi
                        .create('users', vc.user)
                        .then(function (response) {
                            vc.notify('User <strong>"' + vc.user.metadata.name + '"</strong> created successfully', 'message', 'users');
                            vc.fetchAPI('users');
                            router.push('/manage/users');
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data, 'error', 'users');
                        });
                    }
                }
            },

        }

    }
</script>

<style scoped>
    .searchBar.roles #keyword {
        display: inline-block;
        width: 100%;
        max-width: 100%;
        font-size: 100%;
        height: 38px;
    }

    .extHead {
        width: 100%;
        display: inline-block;
    }

    .extHead .install {
        margin-right: 20px;
    }

    .extHead .apiGroups, .extHead .resources, .extHead .verbs {
        width: 220px;
        display: inline-block;
    } 
    
    .extInfo .apiGroups, .extInfo .resources, .extInfo .verbs {
        width: 220px;
        display: inline-block;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        padding-right: 15px;
        box-sizing: border-box;
    }

    li.extension.notFound {
        text-align: center;
    }

    .extensionsList .hasTooltip > span {
        top: 3px;
    }

    [data-fieldset="roles"] .extHead span.name, [data-fieldset="roles"] .extensionsList span.name {
        width: 100px;
    }

    [data-fieldset="roles"] .apiGroups, [data-fieldset="roles"] .resources, [data-fieldset="roles"] .verbs {
        max-width: 215px;
    }
</style>