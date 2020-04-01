var Logs = Vue.component("sg-logs", {
	template: `
		<div id="sg-logs">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>
						<template v-if="typeof $route.params.name !== 'undefined'">
							{{ $route.params.name }}
						</template>
						<template v-else>
							Centralized Logs
						</template>
					</li>
				</ul>
				<!-- <router-link :to="'/crd/create/backup/'+currentNamespace" class="add">Add New</router-link> -->
			</header>

			<div class="content">
				<div id="log">
					
					<table class="logs">
						<thead>
							<th>Log Time</th>
							<th>Owner</th>
						</thead>
						<tbody>
							<tr v-for="log in logs">
								<td>
									{{  log.logTime }}
								</td>
								<td>
									{{  log.owner }}
								</td>
							</tr>
						</tbody>
					</table>

					
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			currentSort: 'data.status.process.timing.stored',
			currentSortDir: 'desc',
			clusterName: '',
			keyword: '',
			isPermanent: [],
			phase: [],
			postgresVersion: [],
			tested: [],
		}
	},
	computed: {

		backups () {
			//return store.state.backups
			return sortTable( store.state.backups, this.currentSort, this.currentSortDir )
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

		allClusters () {
			return store.state.clusters
		},

		currentCluster () {
			return store.state.currentCluster
		},

		isCluster() {
			return  vm.$route.params.cluster !== undefined
        },
        
        logs() {
            
            /* Logs Data */
            axios
            .get('js/data/logs')
            .then( function(response){
                store.commit('setLogs', response.data)
            }).catch(function(err) {
                console.log(err);
			});
			
			return store.state.logs

        }

	},
	methods: {

		filterTable: function() {

			let bk = this;

			$("table tr.base").each(function () {

				let show = true;
				let r = $(this);
				let checkFilters = ['isPermanent', 'phase', 'postgresVersion']; // 'tested' is out for now

				// Filter by Keyword
				if(bk.keyword.length && (r.text().toLowerCase().indexOf(bk.keyword.toLowerCase()) === -1) )
					show = false;

				checkFilters.forEach(function(f){

					if(bk[f].length){
						let hasClass = 0;

						bk[f].forEach(function(c){
							if(r.children('.'+c).length)
								hasClass++;
						});

						if(!hasClass)
							show = false;
					}
					
				})

				/* //Filter by isPermanent
				if(bk.subjectToRetentionPolicy.length && (!r.children(".subjectToRetentionPolicy."+bk.subjectToRetentionPolicy).length))
					show = false;

				//Filter by phase
				if(bk.phase.length && (!r.children(".phase."+bk.phase).length))
					show = false;

				//Filter by postgresVersion
				if(bk.postgresVersion.length){

					let hasClass = 0;
					
					bk.postgresVersion.forEach(function(item){
						if(r.children('.'+item).length)
							hasClass++;
					});

					if(hasClass < 1)
						show = false;

				}

				//Filter by tested
				if(bk.tested.length && (!r.children(".tested."+bk.tested).length))
					show = false; */

				//Filter by clusterName
				if(bk.clusterName.length && (!r.children(".clusterName."+bk.clusterName).length))
					show = false;

				if(!show)
					r.addClass("not-found");
				else
					r.removeClass("not-found");
				
				if(!$("tr.base:not(.not-found)").length)
					$("tr.no-results").show();
				else
					$("tr.no-results").hide();

			});
			
		}
	}
})
