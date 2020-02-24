var SGProfiles = Vue.component("sg-profile", {
	template: `
		<div id="sg-profile">
			<header>
				<h2 class="title">PostgreSQL instance profiles</h2>
				<h3 class="subtitle">Namespace: {{ currentNamespace }} </h3>
			</header>

			<div class="content">
				<div class="profiles boxes">
					<div v-for="prof in profiles" v-bind:id="prof.name+'-'+prof.data.metadata.namespace" class="box" v-bind:class="[($route.params.name == prof.name) ? 'show' : '']" v-if="prof.data.metadata.namespace == currentNamespace">
						<h4>{{ prof.name }}</h4>
						<div class="table no-margin">
							<div class="head row no-border">
								<div class="col text">
									<span>Namespace</span>
									{{ prof.data.metadata.namespace }}
								</div>
								<div class="col">
									<span>RAM</span>
									{{ prof.data.spec.memory }}
								</div>
								<div class="col">
									<span>CPU</span>
									{{ prof.data.spec.cpu }}
								</div>
							</div>
						</div>
						<div class="form">
							<router-link :to="'/crd/edit/profile/'+$route.params.namespace+'/'+prof.name" class="btn">Edit Profile</router-link> 
							<button @click="deleteProfile(prof.name, prof.data.metadata.namespace)" class="border">Delete Profile</button>
						</div>
					</div>
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			
		}
	},
	computed: {

		profiles () {
			return store.state.profiles
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

	},
	methods: {
		deleteProfile: function(profName, profNamespace) {
			//e.preventDefault();

			let confirmDelete = confirm("DELETE ITEM\nAre you sure you want to delete this item?")

			if(confirmDelete) {
				const profile = {
					name: profName,
					namespace: profNamespace
				}
	
				const res = axios
				.delete(
					apiURL+'profile/', 
					{
						data: {
							"metadata": {
								"name": profile.name,
								"namespace": profile.namespace
							}
						}
					}
				)
				.then(function (response) {
					console.log("DELETED");
					//console.log(response);
					notify('Profile <strong>"'+profName+'"</strong> deleted successfully', 'message');
					$('#'+profName+'-'+profNamespace).addClass("deleted");
					//router.push('/overview/'+store.state.currentNamespace);
				})
				.catch(function (error) {
					console.log(error.response);
					notify(error.response.data.message,'error');
				});
			}

		}	
	}
})
