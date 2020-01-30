var Create = Vue.component("sg-create", {
	template: `
		<div id="sg-create">
			<header>
				<h2 class="title">Create New {{ $route.params.name }}</h2>
				<!-- <h3 class="subtitle">Namespace: {{ currentNamespace }} </h3> -->
			</header>

			<div class="content">
				<template v-if="$route.params.name == 'cluster'">
					<create-cluster></create-cluster>
				</template>
			</div>
		</div>`,
	data: function() {

		return {
			
		}
	},
	computed: {

		currentNamespace () {
			return store.state.currentNamespace
		},

    }
})