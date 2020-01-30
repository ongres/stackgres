var ClusterOverview = Vue.component("cluster-overview", {
	template: `
		<div id="cluster-overview">
			<header>
				<h2 class="title">OVERVIEW</h2>
				<h3 class="subtitle">Namespace: {{ currentNamespace }}</h3>
			</header>

			<div class="content">
				<div class="table">
					<div class="head row">
						<div class="col text">
							<h4>StackGres Cluster</h4>
						</div>

						<div class="col">
							<h4>Instances</h4>
						</div>

						<div class="col">
							<h4>CPU</h4>
						</div>

						<div class="col">
							<h4>Memory</h4>
						</div>

						<div class="col">
							<h4>Disk</h4>
						</div>

						<div class="col">
							<h4>Health</h4>
						</div>
					</div>
					<template v-for="cluster in clusters">
						<div class="row" v-if="cluster.data.metadata.namespace == currentNamespace">
							<div class="col text">
								{{ cluster.name }}
							</div>
							<div class="col">
								{{ cluster.data.spec.instances }}
							</div>
							
							<template v-for="profile in profiles">
								<template v-if="(profile.data.metadata.namespace == currentNamespace) && (cluster.data.spec.resourceProfile == profile.name)">
									<div class="col">
										{{ profile.data.spec.cpu }}
									</div>
									<div class="col">
										{{ profile.data.spec.memory }}
									</div>
								</template>
							</template>

							<div class="col">
								{{ cluster.data.spec.volumeSize }}
							</div>
							<div class="col">
								<!-- {{ cluster.data.status.pods_ready }} -->*** / {{ cluster.data.spec.instances }}
							</div>
						</div>
					</template>
				</div>
				<div class="cta">
					<!-- <a href="#" class="btn">Create New Cluster</a> -->
					<router-link :to="'/create/cluster/'+currentNamespace" class="btn">Create New Cluster</router-link>
				</div>
			</div>
		</div>`,
	data: function() {
		
		return {
	      //clusters: null
	    }
	},
	computed: {
		clusters () {
			return store.state.clusters
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

		profiles () {
			return store.state.profiles
		}
	},
	/*mounted: function() {
		
		setTimeout(function(){
			$('#sets .set').each(function(){
	        if($(this).find('a.item').length)
	          	$(this).slideDown();
	        else
	        	$(this).slideUp();
	    });
		},500);

	}*/
})
