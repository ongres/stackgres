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
				<div v-if="hasClusters" class="set clu">
					<h3>Stackgres Clusters</h3>
					<router-link :to="'/create/cluster/'+currentNamespace" class="addnew">+</router-link>

					<template v-for="cluster in clusters">
						<template v-if="cluster.data.metadata.namespace == currentNamespace">
							<router-link :to="'/information/'+cluster.data.metadata.namespace+'/'+cluster.name" class="item" :class="cluster.name">{{ cluster.name }}</router-link>
						</template>
					</template>
				</div>
				<!--<div class="set back">
					<h3>Backups</h3>
					<a href="#" class="addnew">+</a>

					<template v-for="backup in backups">
						<router-link :to="'/backups/'+backup.data.metadata.namespace+'/'+backup.name" class="item" :class="backup.name" v-if="backup.data.metadata.namespace == currentNamespace">{{ backup.name }}</router-link>
					</template>
				</div>-->
				<div v-if="hasPGConfig || hasPoolConfig || hasBackupConfig" class="set conf">
					<h3>Configurations</h3>

					<div v-if="hasPGConfig" class="item pg">
						<router-link :to="'/configurations/postgresql/'+currentNamespace"><h4>PostgreSQL</h4></router-link>
						<!-- <a href="#" class="addnew">+</a> -->

						<template v-for="config in pgConfig">
							<router-link :to="'/configurations/postgresql/'+config.data.metadata.namespace+'/'+config.name" class="item" :class="config.name" v-if="config.data.metadata.namespace == currentNamespace">{{ config.name }}</router-link>
						</template>
					</div>

					<div v-if="hasPoolConfig" class="item pool">
						<router-link :to="'/configurations/connectionpooling/'+currentNamespace"><h4>Connection Pooling</h4></router-link>
						<!-- <a href="#" class="addnew">+</a> -->

						<template v-for="config in poolConfig">
							<router-link :to="'/configurations/connectionpooling/'+config.data.metadata.namespace+'/'+config.name" class="item" :class="config.name" v-if="config.data.metadata.namespace == currentNamespace">{{ config.name }}</router-link>
						</template>
					</div>

					<div v-if="hasBackupConfig" class="item backup">
						<router-link :to="'/configurations/backup/'+currentNamespace"><h4>Backup</h4></router-link>
						<!-- <a href="#" class="addnew">+</a> -->

						<template v-for="config in bkConfig">
							<router-link :to="'/configurations/backup/'+config.data.metadata.namespace+'/'+config.name" class="item" :class="config.name" v-if="config.data.metadata.namespace == currentNamespace">{{ config.name }}</router-link>
						</template>
					</div>
				</div>
				<div v-if="hasProfiles" class="set prof">
					<h3>Instance Profiles</h3>
					<!-- <a href="#" class="addnew">+</a> -->

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

		backups () {
			return store.state.backups;
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
		},

		hasClusters () {
			let index = store.state.clusters.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		},

		hasPGConfig () {
			let index = store.state.pgConfig.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		},

		hasPoolConfig () {
			let index = store.state.poolConfig.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		},

		hasBackupConfig () {
			let index = store.state.backupConfig.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		},

		hasProfiles () {
			let index = store.state.profiles.find(c => (store.state.currentNamespace == c.data.metadata.namespace) );

			if(typeof index !== "undefined")
				return true
			else
				return false
		}
	}
})