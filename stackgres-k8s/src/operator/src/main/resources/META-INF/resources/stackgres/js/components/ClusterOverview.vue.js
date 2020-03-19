var ClusterOverview = Vue.component("cluster-overview", {
	template: `
		<div id="cluster-overview">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						StackGres Clusters
					</li>
				</ul>
				<router-link to="/crd/create/cluster/" class="btn">Create New Cluster</router-link>
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
								<router-link :to="'/status/'+currentNamespace+'/'+cluster.name" title="Cluster Status" class="no-color">{{ cluster.name }}</router-link>
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
								{{ cluster.data.podsReady }} / {{ cluster.data.spec.instances }}
							</div>
						</div>
					</template>
				</div>
				<!-- <div class="cta">
					<a href="#" class="btn">Create New Cluster</a> 
				</div>-->
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
