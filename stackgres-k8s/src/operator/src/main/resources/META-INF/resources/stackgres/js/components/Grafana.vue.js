var Grafana = Vue.component("grafana", {
	template: `
		<div id="grafana">
			<header>
				<h2 class="title">Grafana Report</h2>
				<h3 class="subtitle">{{ $route.params.pod }}</h3>

				<ul class="selector">
					<li><strong>Select a node:</strong></li>
					<li v-for="pod in pods">
						<router-link :to="'/grafana/'+$route.params.name+'/'+pod.ip+':9187'" class="item">{{ pod.name }}</router-link>
					</li>
				</ul>
			</header>

			<div class="content grafana">
				<iframe :src="grafana"></iframe>
			</div>
		</div>`,
	data: function() {

		return {
			grafana: ""
		}
	},
	created: function() {
		this.fetchData()
	},
	mounted: {

		pods () {
			return store.currentCluster.data.status.pods
		}

	},
	methods: {
		fetchData () {
			// Grafana service
			let vc = this;

			$.get("/grafana", function(data) {
				let url = data;
				url += (url.includes('?') ? '&' : '?') + 'theme=light&kiosk';

				if(vc.$route.params.pod && vc.$route.params.pod.length)
					url = url+'&var-instance='+vc.$route.params.pod;
				else
					url = url+'&var-instance='+vc.pods[0].ip+':9187';
				vc.grafana = url;
				$(".grafana iframe").prop("src", url);
			});
		}
	}
})