var CRD = Vue.component("sg-crd", {
	template: `
		<div id="sg-create">
			<header>
				<h2 class="title">{{ $route.params.action }} {{ $route.params.type }}</h2>
			</header>

			<div class="content">
				<template v-if="($route.params.type == 'cluster')">
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