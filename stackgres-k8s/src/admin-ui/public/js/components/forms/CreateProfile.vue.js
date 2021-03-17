var CreateProfile = Vue.component("CreateProfile", {
    template: `
        <form id="create-profile" v-if="loggedIn && isReady">
            <!-- Vue reactivity hack -->
            <template v-if="Object.keys(config).length > 0"></template>

            <header>
                <ul class="breadcrumbs">
                    <li class="namespace">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                        <router-link :to="'/admin/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
                    </li>
                    <li>
					<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><g transform="translate(0 -242)"><path d="M19.649,256.971l-1.538-1.3a.992.992,0,1,0-1.282,1.514l.235.2-6.072,2.228v-4.373l.266.154a.974.974,0,0,0,.491.132.99.99,0,0,0,.862-.506,1.012,1.012,0,0,0-.369-1.372l-1.75-1.013a.983.983,0,0,0-.984,0l-1.75,1.013a1.012,1.012,0,0,0-.369,1.372.985.985,0,0,0,1.353.374l.266-.154v4.353l-6.07-2.21.233-.2a.992.992,0,1,0-1.282-1.514l-1.538,1.3a.992.992,0,0,0-.337.925l.342,1.987a.992.992,0,0,0,.977.824.981.981,0,0,0,.169-.015.992.992,0,0,0,.81-1.145l-.052-.3,7.4,2.694A1.011,1.011,0,0,0,10,262c.01,0,.02,0,.03-.005s.02.005.03.005a1,1,0,0,0,.342-.061l7.335-2.691-.051.3a.992.992,0,0,0,.811,1.145.953.953,0,0,0,.168.015.992.992,0,0,0,.977-.824l.341-1.987A.992.992,0,0,0,19.649,256.971Z" fill="#00adb5"/><path d="M20,246.25a.99.99,0,0,0-.655-.93l-9-3.26a1,1,0,0,0-.681,0l-9,3.26a.99.99,0,0,0-.655.93.9.9,0,0,0,.016.1c0,.031-.016.057-.016.089v5.886a1.052,1.052,0,0,0,.992,1.1,1.052,1.052,0,0,0,.992-1.1v-4.667l7.676,2.779a1.012,1.012,0,0,0,.681,0l7.675-2.779v4.667a1,1,0,1,0,1.984,0v-5.886c0-.032-.014-.058-.016-.089A.9.9,0,0,0,20,246.25Zm-10,2.207L3.9,246.25l6.1-2.206,6.095,2.206Z" fill="#00adb5"/></g></svg>
					<router-link :to="'/admin/profiles/'+currentNamespace" title="SGInstanceProfiles">SGInstanceProfiles</router-link>
					</li>
                    <li v-if="editMode">
                        <router-link :to="'/admin/profiles/'+currentNamespace+'/'+$route.params.name" title="SGInstanceProfile Details">{{ $route.params.name }}</router-link
                    </li>
                    <li class="action">
                        {{ $route.params.action }}
                    </li>
                </ul>

                <div class="actions">
                    <a class="documentation" href="https://stackgres.io/doc/latest/04-postgres-cluster-management/03-instance-profiles/" target="_blank" title="SGInstanceProfile Documentation">SGInstanceProfile Documentation</a>
                </div>
            </header>

            <div class="form">
                <div class="header">
                    <h2>Instance Profile Details</h2>
                </div>

                <label for="metadata.name">Profile Name <span class="req">*</span></label>
                <input v-model="profileName" :disabled="(editMode)" required data-field="metadata.name">
                <a class="help" @click="showTooltip( 'SGInstanceProfile', 'metadata.name')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                </a>

                <span class="warning" v-if="nameColission && !editMode">
                    There's already a <strong>SGInstanceProfile</strong> with the same name on this namespace. Please specify a different name or create the profile on another namespace
                </span>

                
                <div class="unit-select">
                    <label for="spec.memory">RAM <span class="req">*</span></label>
                    <input v-model="profileRAM" class="size" required data-field="spec.memory" type="number" min="0">

                    <select v-model="profileRAMUnit" class="unit" required data-field="spec.memory">
                        <option value="Mi">MiB</option>
                        <option value="Gi" selected>GiB</option>
                    </select>
                    <a class="help" @click="showTooltip( 'SGInstanceProfile', 'spec.memory')">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                    </a>
                </div>

                <div class="unit-select">
                    <label for="spec.cpu">CPU <span class="req">*</span></label>
                    <input v-model="profileCPU" class="size" required data-field="spec.cpu" type="number" min="0">

                    <select v-model="profileCPUUnit" class="unit" required data-field="spec.cpu">
                        <option selected>CPU</option>
                        <option value="m">millicpu</option>
                    </select>
                    <a class="help" @click="showTooltip( 'SGInstanceProfile', 'spec.cpu')">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                    </a>
                </div>
                                    

                <template v-if="editMode">
                    <a class="btn" @click="createProfile">Update Profile</a>
                </template>
                <template v-else>
                    <a class="btn" @click="createProfile">Create Profile</a>
                </template>

                <a @click="cancel" class="btn border">Cancel</a>
            </div>
            <div id="help" class="form">
                <div class="header">
                    <h2>Help</h2>
                </div>
                
                <div class="info">
                    <h3 class="title"></h3>
                    <vue-markdown :source=tooltips></vue-markdown>
                </div>
            </div>
        </form>`,
	data: function() {

        const vm = this;

        return {
            editMode: (vm.$route.params.action === 'edit'),
            profileName: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
            profileNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
            profileCPU: '',
            profileCPUUnit: 'CPU',
            profileRAM: '',
            profileRAMUnit: 'Gi',
        }
            
        
	},
	computed: {
        allNamespaces () {
            return store.state.allNamespaces
        },

        currentNamespace () {
            return store.state.currentNamespace
        },

        tooltips() {
            return store.state.tooltips.description
        },

        nameColission() {
            const vc = this;
            var nameColission = false;
            
            store.state.profiles.forEach(function(item, index) {
				if( (item.name == vc.profileName) && (item.data.metadata.namespace == store.state.currentNamespace ) )
					nameColission = true
			})

			return nameColission
        },

        config() {
            var vm = this;
            var config = {};
            
            if(vm.$route.params.action === 'edit') {
                store.state.profiles.forEach(function( conf ){
                    if( (conf.data.metadata.name === vm.$route.params.name) && (conf.data.metadata.namespace === vm.$route.params.namespace) ) {
                        vm.profileCPU = conf.data.spec.cpu.match(/\d+/g)[0];
                        vm.profileCPUUnit = (conf.data.spec.cpu.match(/[a-zA-Z]+/g) !== null) ? conf.data.spec.cpu.match(/[a-zA-Z]+/g)[0] : 'CPU';
                        vm.profileRAM = conf.data.spec.memory.match(/\d+/g)[0];
                        vm.profileRAMUnit = conf.data.spec.memory.match(/[a-zA-Z]+/g)[0];
                        config = conf;
                        return false;
                    }
                });
            }
        
            return config
        }
    },
    methods: {

        createProfile: function(e) {
            //e.preventDefault();

            let isValid = true;
            
            $('input:required, select:required').each(function() {
                if ($(this).val() === '') {
                    isValid = false;
                    return false;
                }
                    
            });

            if(isValid) {

                var profile = { 
                    "metadata": {
                        "name": this.profileName,
                        "namespace": this.profileNamespace
                    },
                    "spec": {
                        "cpu": (this.profileCPUUnit !== 'CPU')? this.profileCPU+this.profileCPUUnit : this.profileCPU,
                        "memory": this.profileRAM+this.profileRAMUnit,
                    }
                }

                if(this.editMode) {
                    const res = axios
                    .put(
                        apiURL+'sginstanceprofile/', 
                        profile 
                    )
                    .then(function (response) {
                        notify('Profile <strong>"'+profile.metadata.name+'"</strong> updated successfully', 'message','profile');

                        vm.fetchAPI();
                        router.push('/admin/profiles/'+profile.metadata.namespace+'/'+profile.metadata.name);

                    })
                    .catch(function (error) {
                        console.log(error.response);
                        notify(error.response.data,'error','profile');
                    });

                } else {
                    const res = axios
                    .post(
                        apiURL+'sginstanceprofile/', 
                        profile 
                    )
                    .then(function (response) {
                        notify('Profile <strong>"'+profile.metadata.name+'"</strong> created successfully', 'message','profile');
        
                        /* store.commit('updateProfiles', { 
                            name: profile.metadata.name,
                            data: profile
                        }); */

                        vm.fetchAPI();
                        router.push('/admin/profiles/'+profile.metadata.namespace+'/'+profile.metadata.name);
        
                    })
                    .catch(function (error) {
                        console.log(error.response);
                        notify(error.response.data,'error','profile');
                    });

                }
    
                
            }

        },

        cancel: function() {
            router.push('/admin/profiles/'+store.state.currentNamespace);
        },

        showFields: function( fields ) {
            $(fields).slideDown();
        },

        hideFields: function( fields ) {
            $(fields).slideUp();
        }

    },
    created: function() {
        
        this.loadTooltips('SGInstanceProfile');

    },

    mounted: function() {
       
    },

    beforeDestroy: function() {
        store.commit('setTooltipDescription','Click on a question mark to get help and tips about that field.');
    }
})