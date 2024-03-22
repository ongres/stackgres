<template>
    <div id="SGUser" v-if="iCanLoad">
        <div class="content">
            <h2>
                User Details
                <router-link
                    v-if="havePermissionsTo.patch.users"
                    :to="'/manage/user/' + $route.params.name + '/edit'"
                    title="Edit User"
                    class="floatRight"
                >
                    EDIT
                </router-link>
            </h2>
            <template v-if="(typeof crd.data !== 'undefined')">
                <CRDSummary :crd="crd" kind="SGUser" :details="true"></CRDSummary>
            </template>
        </div>
    </div>
</template>

<script>
    import sgApi from '../../api/sgApi';
    import store from '../../store';
    import CRDSummary from '../forms/summary/CRDSummary.vue';
    import { mixin } from '../mixins/mixin';
    
    export default {
        name: 'SGUser',

        mixins: [mixin],

        components: {
            CRDSummary
        },

        computed: {

            crd() {
                return {
                    data: store.state.users.find( user => (
                            (user.metadata.name === this.$route.params.name)
                        )
                    )
                }
            }

        },

        mounted() {
            if(typeof this.crd.data === 'undefined') {
                this.initData();
            }
        },

        methods: {

            initData() {
                const vc = this;
                
                sgApi
                .get('users')
                .then( (response) => {
                    store.commit('setUsers', response.data)

                   let user = response.data.find( user => (
                        (user.metadata.name === vc.$route.params.name)
                    ));

                    if(typeof user === 'undefined') {
                        store.commit('notFound', true);
                    }
                })
                .catch(function(err) {
                    console.log(err);
                    vc.checkAuthError(err);
                });
            }

        }
        
    }
</script>