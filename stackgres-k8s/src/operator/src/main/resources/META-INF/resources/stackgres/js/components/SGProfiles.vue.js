var SGProfiles = Vue.component("sg-profile", {
	template: `
		<div id="sg-profile">
			<header>
				<h2 class="title">PostgreSQL instance profiles</h2>
				<h3 class="subtitle">Namespace: {{ currentNamespace }} </h3>
			</header>

			<div class="content">
				<div class="profiles boxes">
					<div v-for="prof in profiles" class="box" v-bind:class="{'show':($route.params.name == prof.name)}" v-if="prof.data.metadata.namespace == currentNamespace">
						<h4>{{ prof.name }}</h4>
						<span>Namespace</span>
						{{ prof.data.metadata.namespace }}
						<hr>
						<span>RAM</span>
						{{ prof.data.spec.memory }}
						<hr>
						<span>CPU</span>
						{{ prof.data.spec.cpu }}
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

	}
})
