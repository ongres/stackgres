var CreateLogsServer = Vue.component("CreateLogsServer", {
	template: `
        <form id="create-logs-server" class="noSubmit" v-if="loggedIn && isReady">
            <!-- Vue reactivity hack -->
            <template v-if="Object.keys(cluster).length > 0"></template>

            <header>
                <ul class="breadcrumbs">
                    <li class="namespace">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                        <router-link :to="'/admin/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
                    </li>
                    <li>
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M19,15H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,14.6,19.6,15,19,15z"/><path class="a" d="M1,15L1,15c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,14.6,1.6,15,1,15z"/><path class="a" d="M19,11H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,10.6,19.6,11,19,11z"/><path class="a" d="M1,11L1,11c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,10.6,1.6,11,1,11z"/><path class="a" d="M19,7H5C4.4,7,4,6.6,4,6v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,6.6,19.6,7,19,7z"/><path d="M1,7L1,7C0.4,7,0,6.6,0,6v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,6.6,1.6,7,1,7z"/></svg>
						<router-link :to="'/admin/overview/'+currentNamespace" title="Namespace Overview">SGDistributedLogs</router-link>
					</li>
					<li v-if="editMode">
						<router-link :to="'/admin/logs/'+$route.params.namespace+'/'+$route.params.name" title="Logs Server Details">{{ $route.params.name }}</router-link>
					</li>
                    <li class="action">
                        {{ $route.params.action }}
                    </li>
                </ul>

                <div class="actions">
                    <a class="documentation" href="https://stackgres.io/doc/latest/04-postgres-cluster-management/06-distributed-logs/" target="_blank" title="SGDistributedLogs Documentation">SGDistributedLogs Documentation</a>
                </div>
            </header>
            <div class="form">
                <div class="header">
                    <h2>Logs Server Details</h2>
                    <label for="advancedMode" :class="(advancedMode) ? 'active' : ''">
                        <input v-model="advancedMode" type="checkbox" id="advancedMode" name="advancedMode" />
                        <span>Advanced</span>
                    </label>
                </div>

                <label for="metadata.name">Server Name <span class="req">*</span></label>
                <input v-model="name" :disabled="(editMode)" required data-field="metadata.name">
                <a class="help" @click="showTooltip( 'SGDistributedLogs', 'metadata.name')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                </a>

                <span class="warning" v-if="nameColission && !editMode">
                    There's already a <strong>SGDistributedLogs</strong> with the same name on this namespace. Please specify a different name or create the server on another namespace
                </span>

                <div>
                    <div class="unit-select">
                        <label for="spec.persistentVolume.size">Volume Size <span class="req">*</span></label>  
                        <input v-model="volumeSize" class="size" required  :disabled="(editMode)" data-field="spec.persistentVolume.size" type="number">
                        <select v-model="volumeUnit" class="unit" required :disabled="(editMode)" data-field="spec.persistentVolume.size" >
                            <option disabled value="">Select Unit</option>
                            <option value="Mi">MiB</option>
                            <option value="Gi">GiB</option>
                            <option value="Ti">TiB</option>   
                        </select>
                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.persistentVolume.size')">
                            <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                        </a>
                    </div>

                    <template v-if="advancedMode">                        
                        <template v-if="hasStorageClass">
                            <label for="spec.persistentVolume.storageClass">Storage Class</label>
                            <select v-model="storageClass" :disabled="(editMode)" data-field="spec.persistentVolume.storageClass">
                                <option value="">Select Storage Class</option>
                                <option v-for="sClass in storageClasses">{{ sClass }}</option>
                            </select>
                            <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.persistentVolume.storageClass')">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                            </a>
                        </template>
                        
                        <fieldset data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity">
                            <div class="header">
                                <h3>Non Production Settings</h3>  
                            </div>
                            <label for="spec.nonProductionOptions.disableClusterPodAntiAffinity" class="switch yes-no">disableClusterPodAntiAffinity <input type="checkbox" id="disableClusterPodAntiAffinity" v-model="disableClusterPodAntiAffinity" data-switch="NO"></label>
                            <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.nonProductionOptions.disableClusterPodAntiAffinity')">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                            </a>
                        </fieldset>

                        <fieldset class="podsScheduling">
                            <div class="header">
                                <h3 for="spec.pods.scheduling">Pods Scheduling</h3>
                                <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.pods.scheduling')">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                </a> 
                            </div>
                    
                            <fieldset class="nodeSelectors">
                                <div class="header">
                                    <h3 for="spec.pods.scheduling.nodeSelector">Node Selectors</h3>
                                    <a class="addRow" @click="pushLabel('nodeSelector')">Add Node Selector</a>
                                    <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.pods.scheduling.nodeSelector')">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                    </a> 
                                </div>
                        
                                <div class="scheduling repeater" v-if="nodeSelector.length">
                                    <div class="row" v-for="(field, index) in nodeSelector">
                                        <label>Key</label>
                                        <input class="label" v-model="field.label">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="labelValue" v-model="field.value">
                                        
                                        <a class="addRow" @click="spliceArray('nodeSelector', index)">Delete</a>
                                        
                                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.pods.scheduling.nodeSelector')">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                    </a>
                                    </div>
                                </div>
                            </fieldset>

                            <fieldset class="nodeTolerations">
                                <div class="header">
                                    <h3 for="spec.pods.scheduling.tolerations">Node Tolerations</h3>
                                    <a class="addRow" @click="pushToleration()">Add Toleration</a>
                                    <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.pods.scheduling.tolerations')">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                    </a> 
                                </div>
                        
                                <div class="scheduling repeater" v-if="tolerations.length">
                                    <fieldset v-for="(field, index) in tolerations">
                                        <div class="header">
                                            <h3 for="spec.pods.scheduling.tolerations">Toleration #{{ index+1 }}</h3>
                                            <a class="addRow del" @click="spliceArray('tolerations', index)">Delete</a>
                                        </div>
                                        <label for="spec.pods.scheduling.tolerations.key">Key</label>
                                        <input v-model="field.key">
                                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.pods.scheduling.tolerations.key')">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                        </a>

                                        <label for="spec.pods.scheduling.tolerations.operator">Operator</label>
                                        <select v-model="field.operator" @change="(field.operator == 'Exists') ? (field.value = null) : null">
                                            <option>Equal</option>
                                            <option>Exists</option>
                                        </select>
                                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.pods.scheduling.tolerations.operator')">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                        </a>

                                        <label for="spec.pods.scheduling.tolerations.value">Value</label>
                                        <input v-model="field.value" :disabled="(field.operator == 'Exists')" :title="(field.operator == 'Exists') ? 'When the selected operator is Exists, this value must be empty' : ''">
                                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.pods.scheduling.tolerations.value')">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                        </a>

                                        <label for="spec.pods.scheduling.tolerations.effect">Effect</label>
                                        <select v-model="field.effect">
                                            <option :value="nullVal">MatchAll</option>
                                            <option>NoSchedule</option>
                                            <option>PreferNoSchedule</option>
                                            <option>NoExecute</option>
                                        </select>
                                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.pods.scheduling.tolerations.effect')">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                        </a>

                                        <label for="spec.pods.scheduling.tolerations.tolerationSeconds">Toleration Seconds</label>
                                        <input type="number" min="0" v-model="field.tolerationSeconds">
                                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.pods.scheduling.tolerations.tolerationSeconds')">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                        </a>
                                    </fieldset>
                                </div>
                            </fieldset>

                            <span class="warning" v-if="editMode">Please, be aware that any changes made to the <code>Scheduling</code> will require a <a href="https://stackgres.io/doc/0.9/install/restart/" target="_blank">restart opration</a> on the logs server</span>
                        </fieldset>

                        <fieldset class="resourcesMetadata" v-if="!editMode || (editMode && (annotationsAll.length || annotationsPods.length || annotationsServices.length))">
                            <div class="header">
                                <h3 for="spec.metadata.annotations">Resources Metadata</h3>
                                <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.metadata.annotations')">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                </a> 
                            </div>

                            <template v-if="annotationsAll.length">
                                <fieldset>
                                    <div class="header">
                                        <h3 for="spec.metadata.annotations.allResources">All Resources</h3>
                                        <a v-if="!editMode" class="addRow" @click="pushAnnotation('annotationsAll')">Add Annotation</a>
                                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.metadata.annotations.allResources')">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                        </a>    
                                    </div>
                                    <div class="annotation repeater">
                                        <div class="row" v-for="(field, index) in annotationsAll">
                                            <label>Annotation</label>
                                            <input class="annotation" v-model="field.annotation" :disabled="editMode">

                                            <span class="eqSign"></span>

                                            <label>Value</label>
                                            <input class="annotationValue" v-model="field.value" :disabled="editMode">

                                            <a v-if="!editMode" class="addRow" @click="spliceArray('annotationsAll', index)">Delete</a>
                                        </div>
                                    </div>
                                </fieldset>
                            </template>

                            <template v-if="annotationsPods.length">
                                <fieldset>
                                    <div class="header">
                                        <h3 for="spec.metadata.annotations.pods">Pods</h3>
                                        <a v-if="!editMode" class="addRow" @click="pushAnnotation('annotationsPods')">Add Annotation</a>
                                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.metadata.annotations.pods')">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                        </a>    
                                    </div>
                                    <div class="annotation repeater">
                                        <div class="row" v-for="(field, index) in annotationsPods">
                                            <label>Annotation</label>
                                            <input class="annotation" v-model="field.annotation" :disabled="editMode">

                                            <span class="eqSign"></span>

                                            <label>Value</label>
                                            <input class="annotationValue" v-model="field.value" :disabled="editMode">

                                            <a v-if="!editMode" class="addRow" @click="spliceArray('annotationsPods', index)">Delete</a>
                                        </div>
                                    </div>
                                </fieldset>
                            </template>

                            <template v-if="annotationsServices.length">
                                <fieldset>
                                    <div class="header">
                                        <h3 for="spec.metadata.annotations.services">Services</h3>
                                        <a v-if="!editMode" class="addRow" @click="pushAnnotation('annotationsServices')">Add Annotation</a>
                                        <a class="help" @click="showTooltip( 'SGDistributedLogs', 'spec.metadata.annotations.services')">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                        </a>  
                                    </div>
                                    <div class="annotation repeater">
                                        <div class="row" v-for="(field, index) in annotationsServices">
                                            <label>Annotation</label>
                                            <input class="annotation" v-model="field.annotation" :disabled="editMode">

                                            <span class="eqSign"></span>

                                            <label>Value</label>
                                            <input class="annotationValue" v-model="field.value" :disabled="editMode">

                                            <a v-if="!editMode" class="addRow" @click="spliceArray('annotationsServices', index)">Delete</a>
                                        </div>
                                    </div>
                                </fieldset>
                            </template>
                        </fieldset>
                    </template>
                    
                    <template v-if="editMode">
                        <a class="btn" @click="createCluster">Update Server</a>
                    </template>
                    <template v-else>
                        <a class="btn" @click="createCluster">Create Server</a>
                    </template>

                    <a @click="cancel" class="btn border">Cancel</a>
                </div>   
            </div>
            <div id="help" class="form">
                <div class="header">
                    <h2>Help</h2>
                </div>
                
                <div class="info">
                    <h3 class="title"></h3>
                    <vue-markdown :source=tooltips></vue-markdown>
                </div>
            </div>
		</form>`,
	data: function() {

        const vm = this;

        return {
            editMode: (vm.$route.params.action === 'edit'),
            editReady: false,
            help: 'Click on a question mark to get help and tips about that field.',
            nullVal: null,
            advancedMode: false,
            name: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
            namespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
            storageClass: '',
            volumeSize: '',
            volumeUnit: 'Gi',
            disableClusterPodAntiAffinity: false,
            hasStorageClass: true,
            nodeSelector: [ { label: '', value: ''} ],
            tolerations: [ { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null } ],
            annotationsAll: [ { annotation: '', value: '' } ],
            annotationsAllText: '',
            annotationsPods: [ { annotation: '', value: '' } ],
            annotationsServices: [ { annotation: '', value: '' } ],
        }

    },
    
	computed: {

		currentNamespace () {
			return store.state.currentNamespace
        },
        allNamespaces () {
            return store.state.allNamespaces
        },
        storageClasses() {
            return store.state.storageClasses
        },
        tooltips() {
            return store.state.tooltips.description
        },
        nameColission() {

            const vc = this;
            var nameColission = false;
            
            store.state.logsClusters.forEach(function(item, index){
				if( (item.data.metadata.name == vc.$route.params.name) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
					nameColission = true
			})

			return nameColission
        },
        isReady() {
            return store.state.ready
        },
        cluster () {

            var vm = this;
            var cluster = {};
            
            if( (vm.$route.params.action === 'edit') && !vm.editReady) {
                vm.advancedMode = true;
                store.state.logsClusters.forEach(function( c ){
                    if( (c.data.metadata.name === vm.$route.params.name) && (c.data.metadata.namespace === vm.$route.params.namespace) ) {
                      
                        let volumeSize = c.data.spec.persistentVolume.size.match(/\d+/g);
                        let volumeUnit = c.data.spec.persistentVolume.size.match(/[a-zA-Z]+/g);

                        vm.storageClass = c.data.spec.persistentVolume.hasOwnProperty('storageClass') ? c.data.spec.persistentVolume.storageClass : '';

                        if(!vm.storageClass.length)
                            vm.hasStorageClass = false

                        vm.volumeSize = volumeSize;
                        vm.volumeUnit = ''+volumeUnit;
                        vm.disableClusterPodAntiAffinity = ( (typeof c.data.spec.nonProductionOptions !== 'undefined') && (typeof c.data.spec.nonProductionOptions.disableClusterPodAntiAffinity !== 'undefined') ) ? c.data.spec.nonProductionOptions.disableClusterPodAntiAffinity : false;
                        vm.nodeSelector = hasProp(c, 'data.spec.scheduling.nodeSelector') ? vm.unparseProps(c.data.spec.scheduling.nodeSelector, 'label') : [];
                        vm.tolerations = hasProp(c, 'data.spec.scheduling.tolerations') ? c.data.spec.scheduling.tolerations : [];
                        vm.annotationsAll = hasProp(c, 'data.spec.metadata.annotations.allResources') ? vm.unparseProps(c.data.spec.metadata.annotations.allResources) : [];
                        vm.annotationsPods = hasProp(c, 'data.spec.metadata.annotations.pods') ? vm.unparseProps(c.data.spec.metadata.annotations.pods) : [];
                        vm.annotationsServices = hasProp(c, 'data.spec.metadata.annotations.services') ? vm.unparseProps(c.data.spec.metadata.annotations.services) : [];
                        vm.editReady = true

                    }
                });
            }

            return cluster
        }

    },

    methods: {

        createCluster: function(e) {
            //e.preventDefault();

            let isValid = true;
            
            $('input:required, select:required').each(function() {
                if ($(this).val() === '') {
                    isValid = false;
                    return false;
                }
                    
            });

            if(isValid) {
                
                var cluster = { 
                    "metadata": {
                        "name": this.name,
                        "namespace": this.namespace
                    },
                    "spec": {
                        "persistentVolume": {
                            "size": this.volumeSize+this.volumeUnit,
                            ...( ( (this.storageClass !== undefined) && (this.storageClass.length ) ) && ( {"storageClass": this.storageClass }) )
                        },
                        ...(this.disableClusterPodAntiAffinity && ( {"nonProductionOptions": { "disableClusterPodAntiAffinity": this.disableClusterPodAntiAffinity } }) ),
                        ...( (!jQuery.isEmptyObject(this.parseProps(this.nodeSelector, 'label')) || this.hasTolerations() ) && ({
                            "scheduling": {
                                ...(!jQuery.isEmptyObject(this.parseProps(this.nodeSelector, 'label')) && ({"nodeSelector": this.parseProps(this.nodeSelector, 'label')})),
                                ...(this.hasTolerations() && ({"tolerations": this.tolerations}))
                            }
                        }) ),
                        ...( (!jQuery.isEmptyObject(this.parseProps(this.annotationsAll)) || !jQuery.isEmptyObject(this.parseProps(this.annotationsPods)) || !jQuery.isEmptyObject(this.parseProps(this.annotationsServices))) && ({
                            "metadata": {
                                "annotations": {
                                    ...(!jQuery.isEmptyObject(this.parseProps(this.annotationsAll)) && ( {"allResources": this.parseProps(this.annotationsAll) }) ),
                                    ...(!jQuery.isEmptyObject(this.parseProps(this.annotationsPods)) && ( {"pods": this.parseProps(this.annotationsPods) }) ),
                                    ...(!jQuery.isEmptyObject(this.parseProps(this.annotationsServices)) && ( {"services": this.parseProps(this.annotationsServices) }) ),
                                }
                            }
                        }) ),
                    },
                }  
                
                if(this.editMode) {
                    const res = axios
                    .put(
                        apiURL+'sgdistributedlogs/', 
                        cluster 
                    )
                    .then(function (response) {
                        notify('Logs server <strong>"'+cluster.metadata.name+'"</strong> updated successfully', 'message', 'sgcluster');

                        vm.fetchAPI('sgdistributedlogs');
                        router.push('/admin/logs/'+cluster.metadata.namespace);
                        
                    })
                    .catch(function (error) {
                        console.log(error.response);
                        notify(error.response.data,'error', 'sgdistributedlogs');
                    });
                } else {
                    const res = axios
                    .post(
                        apiURL+'sgdistributedlogs/', 
                        cluster 
                    )
                    .then(function (response) {
                        notify('Logs server <strong>"'+cluster.metadata.name+'"</strong> created successfully', 'message', 'sgcluster');

                        vm.fetchAPI('sgdistributedlogs');
                        router.push('/admin/logs/'+cluster.metadata.namespace);
                        
                        /* store.commit('updateClusters', { 
                            name: cluster.data.metadata.name,
                            data: item
                        });*/
                        
                    })
                    .catch(function (error) {
                        console.log(error);
                        notify(error.response.data,'error','sgdistributedlogs');
                    });
                }

            }

        },

        cancel: function() {
            if(this.$route.params.action == 'create')
                router.push('/admin/logs/'+this.$route.params.namespace);
            else
                router.push('/admin/logs/'+this.$route.params.namespace+'/'+this.$route.params.name);
        },  

        showFields: function( fields ) {
            $(fields).slideDown();
        },

        hideFields: function( fields ) {
            $(fields).slideUp();
        },

        parseProps ( props, key = 'annotation' ) {
            var jsonString = '{';
            props.forEach(function(p, i){
                if(p[key].length && p.value.length) {                    
                    if(i)
                        jsonString += ','
                    
                    jsonString += '"'+sanitizeString(p[key])+'":"'+sanitizeString(p.value)+'"'
                }                
            })
            jsonString += '}'

			return JSON.parse(jsonString)
        },
        
        unparseProps ( props, key = 'annotation' ) {
            var propsArray = [];

            Object.entries(props).forEach(([k, v]) => {
                var prop = {};
                prop[key] = k;
                prop['value'] = v;

                propsArray.push(prop)
            });
            return propsArray
		},

        hasTolerations () {
            const vc = this
            let t = [...vc.tolerations]

            t.forEach(function(item, index) {
                if(JSON.stringify(item) == '{"key":"","operator":"Equal","value":null,"effect":null,"tolerationSeconds":null}') {
                    console.log('empty tol')
                    vc.tolerations.splice( index, 1 )
                }
            })
            
            return t.length
        },

        pushLabel: function( prop ) {
            this[prop].push( { label: '', value: '' } )
        },

        pushToleration () {
            this.tolerations.push({ key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null })
        },

        spliceArray: function( prop, index ) {
            this[prop].splice( index, 1 )
        },

        pushAnnotation: function( prop ) {
            this[prop].push( { annotation: '', value: '' } )
        },

    },

    created: function() {
        
        this.loadTooltips('SGDistributedLogs');

    },

    mounted: function() {
        
    },

    beforeDestroy: function() {
        store.commit('setTooltipDescription','Click on a question mark to get help and tips about that field.');
    }
})