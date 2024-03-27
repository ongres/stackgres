<template>
    <div v-if="iCanLoad">        
        <form id="CreateRole" class="form" @submit.prevent v-if="!editMode || editReady">
            <div class="header">
                <h2>
                    <span>{{ editMode ? 'Edit' : 'Create' }} {{ splitUppercase(roleKind).substring(0, (splitUppercase(roleKind).length - 1)) }}</span>
                </h2>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Role Name <span class="req">*</span></label>
                    <input v-model="role.metadata.name" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgrole.metadata.name')"></span>
                </div>
                <div class="col" v-if="roleKind === 'Roles'">
                    <label for="metadata.name">Role Namespace <span class="req">*</span></label>
                    <select
                        required
                        autocomplete="off"
                        :disabled="(editMode)"
                        data-field="metadata.namespace"
                        v-model="role.metadata.namespace" 
                    >
                        <option value="" disabled>
                            Choose one...
                        </option>
                        <template v-for="namespace in namespaces">
                            <option :key="'role-namespace-' + namespace">
                                {{ namespace }}
                            </option>
                        </template>
                    </select>
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgrole.metadata.namespace')"></span>
                </div>
            </div>

             <span class="warning topLeft" v-if="nameColission && !editMode">
                There's already a <strong>Role</strong> with the same name on this namespace. Please specify a different name or create the role on another namespace
            </span>

            <div class="header">
                <h3>
                    Rules
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgrole.rules')"></span>
                </h3>
            </div>

            <template v-if="!isNull(role.rules)">
                <div class="repeater">
                    <template v-for="(ruleSet, ruleSetIndex) in role.rules">
                        <fieldset
                            :key="'rules[' + ruleSetIndex + ']'"
                            :data-fieldset="'rules-' + ruleSetIndex"
                            class="noMargin"
                        >
                            <div class="header">
                                <h3>
                                    Rule #{{ ruleSetIndex + 1 }}
                                </h3>
                                <div class="addRow delete">
                                    <button
                                        type="button"
                                        class="plain"
                                        @click="
                                            spliceArray(role.rules, ruleSetIndex);
                                            !role.rules.length && $set(role, 'rules', null)
                                        "
                                    >
                                        Delete Rule
                                    </button>
                                </div>
                            </div>
                            <div class="ruleSets">
                                <template v-for="(ruleName, ruleIndex) in roleRuleSpecs">
                                    <div
                                        class="ruleSet"
                                        :class="ruleName"
                                        :key="'rules-' + ruleSetIndex + '-' + ruleName"
                                    >
                                        <fieldset
                                            :data-fieldset="'rules[' + ruleSetIndex + '].' + ruleName"
                                            class="noMargin"
                                        >
                                            <div
                                                class="header"
                                                :class="isNull(ruleSet[ruleName]) && 'noBorder'"
                                            >
                                                <h3 for="spec.metadata.labels">
                                                    {{ splitUppercase(ruleName).replace(/([A-Z]) /g, '$1').trim() }}
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgrole.rules.' + ruleName)"></span> 
                                                </h3>
                                            </div>

                                            <div class="repeater">
                                                <template v-for="(el, elIndex) in ruleSet[ruleName]">
                                                    <div
                                                        :key="'rules[' + ruleSetIndex + '].' + ruleName + '[' + elIndex + ']'"
                                                        class="inputContainer"
                                                    >
                                                        <button
                                                            type="button"
                                                            class="addRow delete plain inline"
                                                            @click="
                                                                spliceArray(ruleSet[ruleName], elIndex);
                                                                !ruleSet[ruleName].length && $set(ruleSet, ruleName, null)
                                                            "
                                                        >
                                                            Delete
                                                        </button>
                                                        <input
                                                            v-model="ruleSet[ruleName][elIndex]"
                                                            :data-field="'rules[' + ruleSetIndex + '].' + ruleName + '[' + elIndex + ']'"
                                                        />
                                                    </div>
                                                </template>
                                            </div>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="isNull(role.rules) && 'topBorder'">
                                            <a class="addRow" :data-field="'add-' + ruleName" @click="addRuleElement(ruleSet, ruleName, '')">Add New</a>
                                        </div>
                                        <br/>
                                    </div>
                                </template>
                            </div>
                        </fieldset>
                    </template>
                </div>
            </template>
            <div class="fieldsetFooter" :class="isNull(role.rules) && 'topBorder'">
                <a class="addRow" data-field="add-rule" @click="addRuleSet()">Add Rule</a>
            </div>

            <hr/>

            <template v-if="editMode">
                <button type="submit" class="btn" @click="createRole()">Update Role</button>
            </template>
            <template v-else>
                <button type="submit" class="btn" @click="createRole()">Create Role</button>
            </template>
            
            <button @click="cancel()" class="btn border">Cancel</button>
            
            <button type="button" class="btn floatRight" @click="createRole(true)">View Summary</button>
        </form>
        <CRDSummary :crd="previewCRD" kind="SGRole" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateRole',

        mixins: [mixin],

        components: {
            CRDSummary
        },
        
        data: function() {

            const vc = this;

            return {
                editMode: ['EditRole', 'EditClusterRole'].includes(vc.$route.name),
                editReady: false,
                previewCRD: {},
                showSummary: false,
                role: {
                    metadata: {
                        namespace: '',
                        name: '',
                    },
                    rules: null,
                },
                roleRuleSpecs: ['apiGroups', 'nonResourceURLs', 'resourceNames', 'resources', 'verbs']
            }

        },
        computed: {

            // Define kind (role or clusterrole) according to the route's component name
            roleKind() {
                return this.$route.meta.componentName + 's';
            },

            roleData() {
                return store.state[this.roleKind.toLowerCase()].find( role => (
                        (role.metadata.name === this.role.metadata.name) &&
                        (
                            (this.roleKind === 'ClusterRoles') ||
                            (role.metadata.namespace === this.role.metadata.namespace)
                        )
                    ))
            },

            nameColission() {
                return !this.editMode && (typeof this.roleData !== 'undefined');
            },

            namespaces() {
                let action = this.editMode ? 'patch' : 'create';
                return store.state.allNamespaces.filter( ns => this.iCan(action, 'roles', ns))
            }
        },

        mounted() {
            this.initForm();
        },

        methods: {
            
            initForm() {
                const vc = this;
                
                sgApi
                .get(vc.roleKind.toLowerCase())
                .then( (response) => {
                    store.commit('set' + vc.roleKind, response.data)

                    if(vc.editMode && !vc.editReady ) {
                        let role = response.data.find( role => 
                            (role.metadata.name === vc.$route.params.name)
                        );

                        if(typeof role !== 'undefined') {
                            vc.role = JSON.parse(JSON.stringify(role))
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
            },
        

            createRole(preview = false) {
                const vc = this;

                if(!vc.checkRequired()) {
                    return;
                }

                if(preview) {

                    vc.previewCRD = {};
                    vc.previewCRD['data'] = vc.role;
                    vc.showSummary = true;

                } else {
                    let shortRoleKind = vc.roleKind.substring(0, vc.roleKind.length - 1);

                    if(this.editMode) {
                        sgApi
                        .update(vc.roleKind.toLowerCase(), vc.role)
                        .then(function (response) {
                            vc.notify( vc.splitUppercase(shortRoleKind) + ' <strong>"' + vc.role.metadata.name + '"</strong> updated successfully', 'message', vc.roleKind.toLowerCase());

                            vc.fetchAPI(vc.roleKind.toLowerCase());
                            router.push('/manage/' + shortRoleKind.toLowerCase() + '/' + vc.role.metadata.name);
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data, 'error', vc.roleKind.toLowerCase());
                        });

                    } else {
                        sgApi
                        .create(vc.roleKind.toLowerCase(), vc.role)
                        .then(function (response) {
                            vc.notify(vc.splitUppercase(shortRoleKind) + ' <strong>"' + vc.role.metadata.name + '"</strong> created successfully', 'message', vc.roleKind.toLowerCase());
                            router.push('/manage/users');
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data, 'error', vc.roleKind.toLowerCase());
                        });
                    }
                }
            },

            addRuleSet() {
                const vc = this;
                if(!vc.hasProp(vc.role, 'rules') || (vc.role.rules === null)) {
                    vc.$set(vc.role, 'rules', []);
                }

                vc.role.rules.push({
                    apiGroups: null,
                    nonResourceURLs: null,
                    resourceNames: null,
                    resources: null,
                    verbs: null
                });
            },

            addRuleElement(ruleSet, ruleName, el) {
                const vc = this;
                if(!vc.hasProp(ruleSet, ruleName) || (ruleSet[ruleName] === null)) {
                    vc.$set(ruleSet, ruleName, []);
                }

                ruleSet[ruleName].push(el);
            },

        }

    }
</script>

<style scoped>
    .ruleSets {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        -moz-column-gap: 20px;
        column-gap: 20px;
    }

    .ruleSet.apiGroups {
        grid-column: 1 / -1;
    }
</style>