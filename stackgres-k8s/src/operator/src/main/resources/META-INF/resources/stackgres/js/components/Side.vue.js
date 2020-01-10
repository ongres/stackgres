var Side = Vue.component("sg-side", {
	template: `
		<aside id="side">
			<div id="logo">
				<h1>STACK<strong>GRES</strong></h1>
			</div>

			<div id="namespaces">
				<strong>SELECT NAMESPACE</strong>
				<div id="selected--zg-ul-select">{{ currentNamespace }}</div>
				<ul id="be-select" tabindex="0" class="zg-ul-select">
					<template v-for="namespace in namespaces">
						<li v-bind:class="{'active':(namespace == currentNamespace)}">
							<router-link :to="'/overview/'+namespace" class="item namespace" :class="namespace">{{ namespace }}</router-link>
						</li>
					</template>
				</ul>
			</div>

			<div id="sets">
				<div class="set clu">
					<h3>Stackgres Clusters</h3>
					<a href="#" class="addnew">+</a>

					<template v-for="cluster in clusters">
						<template v-if="cluster.data.metadata.namespace == currentNamespace">
							<router-link :to="'/information/'+cluster.data.metadata.namespace+'/'+cluster.name" class="item" :class="cluster.name">{{ cluster.name }}</router-link>
						</template>
					</template>
				</div>
				<div class="set conf">
					<h3>Configurations</h3>

					<div class="item pg">
						<router-link :to="'/configurations/postgresql/'+currentNamespace"><h4>PostgreSQL</h4></router-link>
						<a href="#" class="addnew">+</a>

						<template v-for="config in pgConfig">
							<router-link :to="'/configurations/postgresql/'+config.data.metadata.namespace+'/'+config.name" class="item" :class="config.name" v-if="config.data.metadata.namespace == currentNamespace">{{ config.name }}</router-link>
						</template>
					</div>

					<div class="item pool">
						<router-link :to="'/configurations/connectionpooling/'+currentNamespace"><h4>Connection Pooling</h4></router-link>
						<a href="#" class="addnew">+</a>

						<template v-for="config in poolConfig">
							<router-link :to="'/configurations/connectionpooling/'+config.data.metadata.namespace+'/'+config.name" class="item" :class="config.name" v-if="config.data.metadata.namespace == currentNamespace">{{ config.name }}</router-link>
						</template>
					</div>

					<div class="item backup">
						<router-link :to="'/configurations/backup/'+currentNamespace"><h4>Backup</h4></router-link>
						<a href="#" class="addnew">+</a>

						<template v-for="config in bkConfig">
							<router-link :to="'/configurations/backup/'+config.data.metadata.namespace+'/'+config.name" class="item" :class="config.name" v-if="config.data.metadata.namespace == currentNamespace">{{ config.name }}</router-link>
						</template>
					</div>
				</div>
				<div class="set prof">
					<h3>Instance Profiles</h3>
					<a href="#" class="addnew">+</a>

					<template v-for="profile in profiles">
						<router-link :to="'/profiles/'+profile.data.metadata.namespace+'/'+profile.name" class="item" :class="profile.name" v-if="profile.data.metadata.namespace == currentNamespace">{{ profile.name }}</router-link>
					</template>
				</div>
			</div>

			<footer id="credits">
				<span>STACK<strong>GRES</strong></span> coded with <span class="hearth">❤</span>︎ by Ongres
			</footer>
		</aside>`,
	data: function() {

		return {
			//clusters: null,
		}

	},

	computed: {
		namespaces () {
			return store.state.namespaces
		},

		clusters () {
			return store.state.clusters
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

		pgConfig () {
			return store.state.pgConfig;
		},

		poolConfig () {
			return store.state.poolConfig;
		},

		bkConfig () {
			return store.state.backupConfig;
		},

		profiles () {
			return store.state.profiles;
		}
	}
})