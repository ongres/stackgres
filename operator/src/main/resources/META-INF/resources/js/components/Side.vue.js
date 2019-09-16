var Side = Vue.component("sg-side", {
	template: `
		<aside id="side">
			<div id="logo">
				<h1>STACK<strong>GRES</strong></h1>
			</div>

			<div id="cluster-id">
				<span>K8S CLUSTER</span>
				<h2>255.255.255.255</h2>
			</div>

			<div id="sets">
				<div class="set clu">
					<h3>Stackgres Clusters</h3>
					<a href="#" class="addnew">+</a>

					<div v-for="cluster in clusters">
						<router-link :to="'/information/'+cluster.name" class="item">{{ cluster.name }}</router-link>
					</div>
				</div>
				<div class="set conf">
					<h3>Configurations</h3>

					<div class="item pg">
						<router-link :to="'/configurations/postgresql'"><h4>PostgreSQL</h4></router-link>
						<a href="#" class="addnew">+</a>

						<template v-for="config in conf.pg">
							<router-link :to="'/configurations/postgresql/'+config.name" class="item">{{ config.name }}</router-link>
						</template>
					</div>

					<div class="item pool">
						<router-link :to="'/configurations/connectionpooling/'"><h4>Connection Pooling</h4></router-link>
						<a href="#" class="addnew">+</a>

						<template v-for="config in conf.pool">
							<router-link :to="'/configurations/connectionpooling/'+config.name" class="item">{{ config.name }}</router-link>
						</template>
					</div>
				</div>
				<div class="set prof">
					<h3>Instance Profiles</h3>
					<a href="#" class="addnew">+</a>

					<div v-for="profile in prof">
						<router-link :to="'/profiles/'+profile.name" class="item">{{ profile.name }}</router-link>
					</div>
				</div>
			</div>

			<footer id="credits">
				<span>STACK<strong>GRES</strong></span> coded with <span class="hearth">❤</span>︎ by Ongres
			</footer>
		</aside>`,
	data: function() {

		return {
			clusters: null,
			conf: {
				pg: null,
				pool: null
			},
			prof: null
		}
	},
	created: function() {

		let vm = this;

		vm.clusters = clustersList;
		vm.conf.pg = pgConf;
		vm.conf.pool = poolConf;
		vm.prof = profiles;

	}
})