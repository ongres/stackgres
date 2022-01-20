<template>
    <div id="create-dbops" v-if="loggedIn && isReady && !notFound">
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><g fill="#36A8FF"><path d="M17.1 20c-.6 0-1-.5-1-1 0-1.6-1.3-2.8-2.8-2.8H6.6c-1.6 0-2.8 1.3-2.8 2.8 0 .6-.5 1-1 1s-1-.5-1-1c0-2.7 2.2-4.8 4.8-4.8h6.7c2.7 0 4.8 2.2 4.8 4.8.1.5-.4 1-1 1zM9.9 9.4c-1.4 0-2.5-1.1-2.5-2.5s1.1-2.5 2.5-2.5 2.5 1.1 2.5 2.5c.1 1.4-1.1 2.5-2.5 2.5zm0-3.3c-.4 0-.8.3-.8.8 0 .4.3.8.8.8.5-.1.8-.4.8-.8 0-.5-.3-.8-.8-.8z"/><path d="M10 13.7h-.2c-1-.1-1.8-.8-1.8-1.8v-.1h-.1l-.1.1c-.8.7-2.1.6-2.8-.2s-.7-1.9 0-2.6l.1-.1H5c-1.1 0-2-.8-2.1-1.9 0-1.2.8-2.1 1.8-2.2H5v-.1c-.7-.8-.7-2 .1-2.8.8-.7 1.9-.7 2.7 0 .1 0 .1 0 .2-.1 0-.6.3-1.1.7-1.4.8-.7 2.1-.6 2.8.2.2.3.4.7.4 1.1v.1h.1c.8-.7 2.1-.6 2.8.2.6.7.6 1.9 0 2.6l-.1.1v.1h.1c.5 0 1 .1 1.4.5.8.7.9 2 .2 2.8-.3.4-.8.6-1.4.7h-.3c.4.4.6 1 .6 1.5-.1 1.1-1 1.9-2.1 1.9-.4 0-.9-.2-1.2-.5l-.1-.1v.1c0 1.1-.9 1.9-1.9 1.9zM7.9 10c1 0 1.8.8 1.8 1.7 0 .1.1.2.2.2s.2-.1.2-.2c0-1 .8-1.8 1.8-1.8.5 0 .9.2 1.3.5.1.1.2.1.3 0s.1-.2 0-.3c-.7-.7-.7-1.8 0-2.5.3-.3.8-.5 1.3-.5h.1c.1 0 .2 0 .2-.1 0 0 .1-.1.1-.2s0-.1-.1-.2c0 0-.1-.1-.2-.1h-.2c-.7 0-1.4-.4-1.6-1.1 0-.1 0-.1-.1-.2-.2-.6-.1-1.3.4-1.8.1-.1.1-.2 0-.3s-.2-.1-.3 0c-.3.3-.8.5-1.2.5-1 0-1.8-.8-1.8-1.8 0-.1-.1-.2-.2-.2s-.1 0-.2.1c.1.1 0 .2 0 .3 0 .7-.4 1.4-1.1 1.7-.1 0-.1 0-.2.1-.6.2-1.3 0-1.8-.4-.1-.1-.2-.1-.3 0-.1.1-.1.2 0 .3.3.3.5.7.5 1.2.1 1-.7 1.9-1.7 1.9h-.2c-.1 0-.1 0-.2.1 0-.1 0 0 0 0 0 .1.1.2.2.2h.2c1 0 1.8.8 1.8 1.8 0 .5-.2.9-.5 1.2-.1.1-.1.2 0 .3s.2.1.3 0c.3-.2.7-.4 1.1-.4h.1z"/></g></svg>
                    <router-link :to="'/' + $route.params.namespace + '/sgdbops'" title="SGDbOps">SGDbOps</router-link>
                </li>
                <li class="action">
                    Create
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgdbops/" target="_blank" title="SGDbOps Documentation">SGDbOps Documentation</a>
            </div>
        </header>
        <form id="createDbops" class="form dbopsForm" @submit.prevent>
            <div class="header">
                <h2>Database Operation Details</h2>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Operation Name <span class="req">*</span></label>
                    <input v-model="name" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.metadata.name')"></span>
                </div>

                <div class="col">
                    <label for="spec.sgCluster">Target Cluster <span class="req">*</span></label>
                    <select v-model="sgCluster" required data-field="spec.sgCluster">
                        <option disabled value="">Choose a Cluster</option>
                        <template v-for="cluster in allClusters">
                            <option v-if="cluster.data.metadata.namespace == $route.params.namespace">{{ cluster.data.metadata.name }}</option>
                        </template>
                    </select>
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.sgCluster')"></span>
                </div>

                <span class="warning" v-if="nameColission">
                    There's already a <strong>SGDbOps</strong> with the same name on this namespace. Please specify a different name or create the operation on another namespace.
                </span>
            </div>

            <hr/>

            <div class="row-100">
                <h3 class="sectionTitle">Database Operation <span class="req">*</span></h3>

                <div class="col">
                    <div class="optionBoxes">
                        <template v-for="operation in ['benchmark', 'vacuum', 'repack', 'securityUpgrade', 'minorVersionUpgrade', 'majorVersionUpgrade', 'restart']">
                            <label class="dbopIcon" :class="[operation, ( (op == operation) && 'active' )]" :for="operation" data-field="spec.op">
                                {{ splitUppercase(operation) }}
                                <input type="radio" v-model="op" data-field="spec.op" :value="operation" :id="operation">
                            </label>
                        </template>
                    </div>
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.op')"></span>
                </div>
            </div>

            <hr/>
            
            <div class="row-50">
                <h3 class="sectionTitle">General Operation Details</h3>

                <div class="col">            
                    <label for="spec.runAt">Run At</label>
                    <input class="datePicker" autocomplete="off" placeholder="YYYY-MM-DD HH:MM:SS" :value="runAtTimezone" data-field="spec.runAt">
                    <span class="helpTooltip" :data-tooltip="(timezone == 'local') ? getTooltip('sgdbops.spec.runAt').replace('UTC ','') : getTooltip('sgdbops.spec.runAt')"></span>
                </div>

                <div class="col">        
                    <label for="spec.timeout">Timeout</label>
                    <div class="timeSelect">
                        <select v-model="timeout.d" class="round dayselect" data-field="spec.timeout.days">
                            <option disabled selected value="0">Days</option><option value="0">0</option><option value="1">1</option><option value="2">2</option><option value="3">3</option><option value="4">4</option><option value="5">5</option><option value="6">6</option><option value="7">7</option><option value="8">8</option><option value="9">9</option><option value="10">10</option>
                        </select>
                        <select v-model="timeout.h" class="round hourselect" data-field="spec.timeout.hours">
                            <option disabled selected value="0">Hours</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option>
                        </select> 
                        <select v-model="timeout.m" class="round minuteselect" data-field="spec.timeout.minutes">
                            <option disabled selected value="0">Minutes</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option><option value="24">24</option><option value="25">25</option><option value="26">26</option><option value="27">27</option><option value="28">28</option><option value="29">29</option><option value="30">30</option><option value="31">31</option><option value="32">32</option><option value="33">33</option><option value="34">34</option><option value="35">35</option><option value="36">36</option><option value="37">37</option><option value="38">38</option><option value="39">39</option><option value="40">40</option><option value="41">41</option><option value="42">42</option><option value="43">43</option><option value="44">44</option><option value="45">45</option><option value="46">46</option><option value="47">47</option><option value="48">48</option><option value="49">49</option><option value="50">50</option><option value="51">51</option><option value="52">52</option><option value="53">53</option><option value="54">54</option><option value="55">55</option><option value="56">56</option><option value="57">57</option><option value="58">58</option><option value="59">59</option>
                        </select>
                        <select v-model="timeout.s" class="round secondselect" data-field="spec.timeout.seconds">
                            <option disabled selected value="0">Seconds</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option><option value="24">24</option><option value="25">25</option><option value="26">26</option><option value="27">27</option><option value="28">28</option><option value="29">29</option><option value="30">30</option><option value="31">31</option><option value="32">32</option><option value="33">33</option><option value="34">34</option><option value="35">35</option><option value="36">36</option><option value="37">37</option><option value="38">38</option><option value="39">39</option><option value="40">40</option><option value="41">41</option><option value="42">42</option><option value="43">43</option><option value="44">44</option><option value="45">45</option><option value="46">46</option><option value="47">47</option><option value="48">48</option><option value="49">49</option><option value="50">50</option><option value="51">51</option><option value="52">52</option><option value="53">53</option><option value="54">54</option><option value="55">55</option><option value="56">56</option><option value="57">57</option><option value="58">58</option><option value="59">59</option>
                        </select>
                    </div>
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.timeout')"></span>
                </div>

                <template  v-if="( tzOffset && ( timezone == 'utc' ) )">
                    <div class="warning">
                        Bear in mind <strong>"Run At"</strong> times are expressed in UTC (Coordinated Universal Time). That's {{ tzOffset }} your current timezone.
                    </div>

                    <div class="clearfix"></div>
                </template>
        
                <div class="col">
                    <label for="spec.maxRetries">Max Retries</label>
                    <select v-model="maxRetries" data-field="spec.maxRetries">
                        <option v-for="val in 11">{{ val - 1 }}</option>
                    </select>
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.maxRetries')"></span>
                </div>
            </div>

            <hr/>

            <fieldset v-if="op == 'vacuum'">
                <div class="header open">
                    <h3>Vacuum Details</h3>
                </div>

                <div class="row-50">
                    <div class="col">
                        <label for="spec.vacuum.full">Full Vacuum</label>
                        <label for="fullVacuum" class="switch yes-no" data-field="spec.vacuum.full">
                            Enable
                            <input type="checkbox" id="fullVacuum" v-model="vacuum.full" data-switch="ON">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.full')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.vacuum.freeze">Freeze</label>
                        <label for="freezeVacuum" class="switch yes-no" data-field="spec.vacuum.freeze">
                            Enable
                            <input type="checkbox" id="freezeVacuum" v-model="vacuum.freeze" data-switch="ON">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.freeze')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.vacuum.analyze">Analyze</label>
                        <label for="analyzeVacuum" class="switch yes-no" data-field="spec.vacuum.analyze">
                            Enable
                            <input type="checkbox" id="analyzeVacuum" v-model="vacuum.analyze" data-switch="ON">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.analyze')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.vacuum.disablePageSkipping">Page Skipping</label>
                        <label for="disablePageSkippingVacuum" class="switch yes-no reverse" data-field="spec.vacuum.disablePageSkipping">
                            Enable
                            <input type="checkbox" id="disablePageSkippingVacuum" v-model="vacuum.disablePageSkipping" data-switch="ON"></label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.disablePageSkipping').replace('By default false', 'Enabled by default')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.vacuum.databases">Database Specific Options</label>
                        <label for="vacuumPerDbs" class="switch" data-field="spec.vacuum.databases">
                            Enable
                            <input type="checkbox" id="vacuumPerDbs" v-model="vacuumPerDbs" data-switch="ON">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.databases')"></span>
                    </div>
                </div>

                <template v-if="vacuumPerDbs">
                    <hr style="margin: -10px 0 25px;" />

                    <fieldset>
                        <div class="header">
                            <h4 for="spec.vacuum.databases">
                                Databases
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.databases')"></span>
                            </h4>
                        </div>
                        
                        <div class="vacuumDbs repeater">
                            <fieldset v-for="(db, index) in vacuumDbs">
                                <div class="header">
                                    <h5>Database #{{ index+1 }} <template v-if="db.hasOwnProperty('name') && db.name.length">–</template> <span class="scriptTitle">{{ db.name }}</span></h5>
                                    <a class="addRow" @click="spliceArray('vacuumDbs', index)">Delete</a>
                                </div>    
                                <div class="row row-50">
                                    <div class="col">
                                        <label for="spec.vacuum.databases.name">Name <span class="req">*</span></label>
                                        <input v-model="db.name" placeholder="Type a name..." required autocomplete="off" data-field="spec.vacuum.databases.name">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.databases.name')"></span>
                                    </div>

                                    <div class="col">
                                        <label for="spec.vacuum.databases.full">Full</label>
                                        <select v-model="db.full" data-field="spec.vacuum.databases.full">
                                            <option value="inherit" selected>Inherit from global settings</option>
                                            <option :value="true">ON</option>
                                            <option :value="false">OFF</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.databases.full')"></span>
                                    </div>

                                    <div class="col">
                                        <label for="spec.vacuum.databases.freeze">Freeze</label>
                                        <select v-model="db.freeze" data-field="spec.vacuum.databases.freeze">
                                            <option value="inherit" selected>Inherit from global settings</option>
                                            <option :value="true">ON</option>
                                            <option :value="false">OFF</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.databases.freeze')"></span>
                                    </div>

                                    <div class="col">
                                        <label for="spec.vacuum.databases.analyze">Analyze</label>
                                        <select v-model="db.analyze" data-field="spec.vacuum.databases.analyze">
                                            <option value="inherit" selected>Inherit from global settings</option>
                                            <option :value="true">ON</option>
                                            <option :value="false">OFF</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.databases.analyze')"></span>
                                    </div>

                                    <div class="col">
                                        <label for="spec.vacuum.databases.disablePageSkipping">Page Skipping</label>
                                        <select v-model="db.disablePageSkipping" data-field="spec.vacuum.databases.disablePageSkipping">
                                            <option value="inherit" selected>Inherit from global settings</option>
                                            <option :value="false">Enable</option>
                                            <option :value="true">Disable</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.databases.disablePageSkipping')"></span>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="!vacuumDbs.length && 'topBorder'">
                                <a class="addRow" @click="pushDatabase('vacuum')">Add Database</a>
                            </div>
                        </div>
                    </fieldset>
                </template>
            </fieldset>

            <fieldset v-else-if="op == 'restart'">
                <div class="header open">
                    <h3>Restart Details</h3>
                </div>

                <div class="row-50">
                    <div class="col">
                        <label for="spec.restart.method">Method</label>
                        <select v-model="restart.method" data-field="spec.restart.method">
                            <option value="InPlace">In Place</option>
                            <option value="ReducedImpact">Reduced Impact</option>
                        </select>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.restart.method')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.restart.onlyPendingRestart">Pods to Restart</label>
                        <label for="usePreparedStatements" class="switch yes-no" data-field="spec.restart.onlyPendingRestart">
                            Pending Pods Only
                            <input type="checkbox" id="usePreparedStatements" v-model="restart.onlyPendingRestart" data-switch="NO">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.restart.onlyPendingRestart')"></span>
                    </div>
                </div>
            </fieldset>

            <fieldset class="securityUpgrade" v-else-if="op == 'securityUpgrade'">
                <div class="header open">
                    <h3>Security Upgrade Details</h3>
                </div>

                <div class="row-50">
                    <div class="col">
                        <label for="spec.securityUpgrade.method">Method</label>
                        <select v-model="securityUpgrade.method" data-field="spec.securityUpgrade.method">
                            <option value="InPlace">In Place</option>
                            <option value="ReducedImpact">Reduced Impact</option>
                        </select>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.securityUpgrade.method')"></span>
                    </div>
                </div>
            </fieldset>
            
            <fieldset class="minorVersionUpgrade" v-else-if="op == 'minorVersionUpgrade'">
                <template v-if="sgCluster.length">
                    <template v-for="cluster in allClusters" v-if="sgCluster == cluster.name">
                        <template v-if="(typeof (postgresVersionsList[cluster.data.spec.postgres.version.substring(0,2)].find(v => v > cluster.data.spec.postgres.version)) != 'undefined')">
                            <div class="header open">
                                <h3>Minor Version Upgrade Details</h3>
                            </div>

                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.minorVersionUpgrade.method">Method</label>
                                    <select v-model="minorVersionUpgrade.method" data-field="spec.minorVersionUpgrade.method">
                                        <option value="InPlace">In Place</option>
                                        <option value="ReducedImpact">Reduced Impact</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.minorVersionUpgrade.method')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.minorVersionUpgrade.postgresVersion">Target Postgres Version <span class="req">*</span></label>
                                    <select v-model="minorVersionUpgrade.postgresVersion" required data-field="spec.minorVersionUpgrade.postgresVersion">
                                        <option disabled value="">Choose version...</option>
                                        <option v-for="version in postgresVersionsList[cluster.data.spec.postgres.version.substring(0,2)]" v-if="version > cluster.data.spec.postgres.version">{{ version }}</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.minorVersionUpgrade.postgresVersion')"></span>
                                </div>
                            </div>
                        </template>
                        <template v-else>
                            <p class="warning">
                                Your cluster is already set to use <strong>Postgres {{ cluster.data.spec.postgres.version }}</strong> which is the latest minor version available for <strong>Postgres {{ cluster.data.spec.postgres.version.substring(0,2) }}</strong>.

                                <template v-if="(typeof Object.keys(postgresVersionsList).find(v => v > cluster.data.spec.postgres.version.substring(0,2)) !== 'undefined')">
                                    <br/><br/>
                                    If you prefer, you could perform a Major Version Upgrade to <strong>Postgres {{ parseInt(cluster.data.spec.postgres.version.substring(0,2)) + 1 }}</strong>.
                                </template>
                            </p>
                        </template>
                    </template>
                </template>
                <p class="warning" v-else>
                    You must first choose a target cluster to be able to set the upgrade details
                </p>
            </fieldset>

            <fieldset class="majorVersionUpgrade" v-else-if="op == 'majorVersionUpgrade'">
                <template v-if="sgCluster.length">
                    <template v-for="cluster in allClusters" v-if="sgCluster == cluster.name">
                        <template v-if="Object.keys(postgresVersionsList).filter(v => v > cluster.data.spec.postgres.version.substring(0,2)).length">
                            <div class="header open">
                                <h3>Major Version Upgrade Details</h3>
                            </div>

                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.majorVersionUpgrade.link">Hard Link Files</label>
                                    <label for="majorVersionUpgradeLink" class="switch yes-no" data-field="spec.majorVersionUpgrade.link">
                                        Enable
                                        <input type="checkbox" id="majorVersionUpgradeLink" v-model="majorVersionUpgrade.link" data-switch="ON">
                                    </label>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.link')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.majorVersionUpgrade.clone">Clone Files</label>
                                    <label for="majorVersionUpgradeClone" class="switch yes-no" data-field="spec.majorVersionUpgrade.clone">
                                        Enable
                                        <input type="checkbox" id="majorVersionUpgradeClone" v-model="majorVersionUpgrade.clone" data-switch="ON">
                                    </label>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.clone')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.majorVersionUpgrade.check">Check Clusters</label>
                                    <label for="majorVersionUpgradeCheck" class="switch yes-no" data-field="spec.majorVersionUpgrade.check">
                                        Enable
                                        <input type="checkbox" id="majorVersionUpgradeCheck" v-model="majorVersionUpgrade.check" data-switch="ON">
                                    </label>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.check')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.majorVersionUpgrade.postgresVersion">Target Postgres Version <span class="req">*</span></label>
                                    <select v-model="majorVersionUpgrade.postgresVersion" required data-field="spec.majorVersionUpgrade.postgresVersion">
                                        <option disabled value="">Choose version...</option>
                                        <option v-for="version in postgresVersionsList[parseInt(cluster.data.spec.postgres.version.substring(0,2)) + 1]">{{ version }}</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.minorVersionUpgrade.postgresVersion')"></span>
                                </div>

                                <div class="col" v-if="majorVersionUpgrade.postgresVersion.length">
                                    <label for="spec.majorVersionUpgrade.sgPostgresConfig">Target Postgres Configuration <span class="req">*</span></label>
                                    <select v-model="majorVersionUpgrade.sgPostgresConfig" required data-field="spec.majorVersionUpgrade.sgPostgresConfig">
                                        <option disabled value="">Choose config...</option>
                                        <option v-for="config in pgConfigs" v-if="config.data.spec.postgresVersion == majorVersionUpgrade.postgresVersion.substring(0,2)">{{ config.name }}</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.sgPostgresConfig')"></span>
                                </div>
                            </div>
                        </template>
                        <template v-else>
                            <p class="warning">
                                Your cluster is already set to use <strong>Postgres {{ cluster.data.spec.postgres.version.substring(0,2) }}</strong> which is the latest major version available.
                            </p>
                        </template>
                    </template>
                </template>
                <p class="warning" v-else>
                    You must first choose a target cluster to be able to set the upgrade details
                </p>
            </fieldset>

            <fieldset v-else-if="op == 'benchmark'">
                <div class="header open">
                    <h3>Benchmark Details</h3>
                </div>

                <div class="row-50">
                    <div class="col">                
                        <label for="spec.benchmark.type">Type <span class="req">*</span></label>
                        <select v-model="benchmark.type">
                            <option v-for="type in benchmarkTypes">{{ type }}</option>
                        </select>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.type')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.benchmark.connectionType">Connection Type</label>
                        <select v-model="benchmark.connectionType" data-field="spec.benchmark.connectionType">
                            <option value="primary-service">Connect to the Primary Service</option>
                            <option value="replicas-service">Connect to the Replicas Service</option>
                        </select>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.connectionType')"></span>
                    </div>
                </div>

                <template v-if="benchmark.type == 'pgbench'">
                    <div class="row-50">
                        <div class="col">
                            <div class="unit-select">
                                <label for="spec.benchmark.pgbench.databaseSize">Database Size <span class="req">*</span></label>  
                                <input v-model="benchmark.pgbench.databaseSize.size" class="size" required data-field="spec.benchmark.pgbench.databaseSize" type="number" min="1">
                                <select v-model="benchmark.pgbench.databaseSize.unit" class="unit" required data-field="spec.benchmark.pgbench.databaseSize" >
                                    <option disabled value="">Select Unit</option>
                                    <option value="Mi">MiB</option>
                                    <option value="Gi">GiB</option>
                                    <option value="Ti">TiB</option>   
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.databaseSize')"></span>
                            </div>
                        </div>

                        <div class="col">
                            <label for="spec.benchmark.pgbench.usePreparedStatements">Prepared Statements</label>
                            <label for="usePreparedStatements" class="switch yes-no" data-field="spec.benchmark.pgbench.usePreparedStatements">
                                Enable
                                <input type="checkbox" id="usePreparedStatements" v-model="benchmark.pgbench.usePreparedStatements" data-switch="NO">
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.usePreparedStatements')"></span>
                        </div>

                        <div class="col">                                
                            <label for="spec.benchmark.pgbench.concurrentClients">Concurrent Clients</label>  
                            <input v-model="benchmark.pgbench.concurrentClients" class="size" data-field="spec.benchmark.pgbench.concurrentClients" type="number" min="1">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.concurrentClients')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.benchmark.pgbench.threads">Threads</label>  
                            <input v-model="benchmark.pgbench.threads" class="size" data-field="spec.benchmark.pgbench.threads" type="number" min="1">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.threads')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.benchmark.pgbench.duration">Duration <span class="req">*</span></label>
                            <div class="timeSelect reqFieldset">
                                <select v-model="benchmark.pgbench.duration.d" class="round dayselect" data-field="spec.benchmark.pgbench.duration.days">
                                    <option disabled selected value="">Days</option><option value="0">0</option><option value="1">1</option><option value="2">2</option><option value="3">3</option><option value="4">4</option><option value="5">5</option><option value="6">6</option><option value="7">7</option><option value="8">8</option><option value="9">9</option><option value="10">10</option>
                                </select>
                                <select v-model="benchmark.pgbench.duration.h" class="round hourselect" data-field="spec.benchmark.pgbench.duration.hours">
                                    <option disabled selected value="">Hours</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option>
                                </select>
                                <select v-model="benchmark.pgbench.duration.m" class="round minuteselect" data-field="spec.benchmark.pgbench.duration.minutes">
                                    <option disabled selected value="">Minutes</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option><option value="24">24</option><option value="25">25</option><option value="26">26</option><option value="27">27</option><option value="28">28</option><option value="29">29</option><option value="30">30</option><option value="31">31</option><option value="32">32</option><option value="33">33</option><option value="34">34</option><option value="35">35</option><option value="36">36</option><option value="37">37</option><option value="38">38</option><option value="39">39</option><option value="40">40</option><option value="41">41</option><option value="42">42</option><option value="43">43</option><option value="44">44</option><option value="45">45</option><option value="46">46</option><option value="47">47</option><option value="48">48</option><option value="49">49</option><option value="50">50</option><option value="51">51</option><option value="52">52</option><option value="53">53</option><option value="54">54</option><option value="55">55</option><option value="56">56</option><option value="57">57</option><option value="58">58</option><option value="59">59</option>
                                </select>
                                <select v-model="benchmark.pgbench.duration.s" class="round secondselect" data-field="spec.benchmark.pgbench.duration.seconds">
                                    <option disabled selected value="">Seconds</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option><option value="24">24</option><option value="25">25</option><option value="26">26</option><option value="27">27</option><option value="28">28</option><option value="29">29</option><option value="30">30</option><option value="31">31</option><option value="32">32</option><option value="33">33</option><option value="34">34</option><option value="35">35</option><option value="36">36</option><option value="37">37</option><option value="38">38</option><option value="39">39</option><option value="40">40</option><option value="41">41</option><option value="42">42</option><option value="43">43</option><option value="44">44</option><option value="45">45</option><option value="46">46</option><option value="47">47</option><option value="48">48</option><option value="49">49</option><option value="50">50</option><option value="51">51</option><option value="52">52</option><option value="53">53</option><option value="54">54</option><option value="55">55</option><option value="56">56</option><option value="57">57</option><option value="58">58</option><option value="59">59</option>
                                </select>
                            </div>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.duration')"></span>
                            <span class="warning">The pgbench Benchmark Duration <strong>does not include database preparation</strong> that, depending on the selected size and environment, may take from some seconds to hours.</span>
                        </div>
                    </div>
                </template>
            </fieldset>

            <fieldset v-if="op == 'repack'">
                <div class="header open">
                    <h3>Repack Details</h3>
                </div>

                <div class="row-50">
                    <div class="col">
                        <label for="spec.repack.noOrder">No Order</label>
                        <label for="repackNoOrder" class="switch yes-no" data-field="spec.repack.noOrder">
                            Enable
                            <input type="checkbox" id="repackNoOrder" v-model="repack.noOrder" data-switch="ON">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noOrder')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.repack.noKillBackend">No Kill Backend</label>
                        <label for="repackNoKillBackend" class="switch yes-no" data-field="spec.repack.noKillBackend">
                            Enable
                            <input type="checkbox" id="repackNoKillBackend" v-model="repack.noKillBackend" data-switch="ON">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noKillBackend')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.repack.noAnalyze">No Analyze</label>
                        <label for="repackNoAnalyze" class="switch yes-no" data-field="spec.repack.noAnalyze">
                            Enable
                            <input type="checkbox" id="repackNoAnalyze" v-model="repack.noAnalyze" data-switch="ON">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noAnalyze')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.repack.excludeExtension">Exclude Extension</label>
                        <label for="repackExcludeExtension" class="switch yes-no" data-field="spec.repack.excludeExtension">
                            Enable
                            <input type="checkbox" id="repackExcludeExtension" v-model="repack.excludeExtension" data-switch="ON">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.excludeExtension')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.repack.waitTimeout">Wait Timeout</label>
                        <div class="timeSelect">
                            <select v-model="repack.waitTimeout.d" class="round dayselect" data-field="spec.repack.waitTimeout.days">
                                <option disabled selected value="0">Days</option><option value="0">0</option><option value="1">1</option><option value="2">2</option><option value="3">3</option><option value="4">4</option><option value="5">5</option><option value="6">6</option><option value="7">7</option><option value="8">8</option><option value="9">9</option><option value="10">10</option>
                            </select>
                            <select v-model="repack.waitTimeout.h" class="round hourselect" data-field="spec.repack.waitTimeout.hours">
                                <option disabled selected value="0">Hours</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option>
                            </select>
                            <select v-model="repack.waitTimeout.m" class="round minuteselect" data-field="spec.repack.waitTimeout.minutes">
                                <option disabled selected value="0">Minutes</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option><option value="24">24</option><option value="25">25</option><option value="26">26</option><option value="27">27</option><option value="28">28</option><option value="29">29</option><option value="30">30</option><option value="31">31</option><option value="32">32</option><option value="33">33</option><option value="34">34</option><option value="35">35</option><option value="36">36</option><option value="37">37</option><option value="38">38</option><option value="39">39</option><option value="40">40</option><option value="41">41</option><option value="42">42</option><option value="43">43</option><option value="44">44</option><option value="45">45</option><option value="46">46</option><option value="47">47</option><option value="48">48</option><option value="49">49</option><option value="50">50</option><option value="51">51</option><option value="52">52</option><option value="53">53</option><option value="54">54</option><option value="55">55</option><option value="56">56</option><option value="57">57</option><option value="58">58</option><option value="59">59</option>
                            </select>
                            <select v-model="repack.waitTimeout.s" class="round secondselect" data-field="spec.repack.waitTimeout.seconds">
                                <option disabled selected value="0">Seconds</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option><option value="24">24</option><option value="25">25</option><option value="26">26</option><option value="27">27</option><option value="28">28</option><option value="29">29</option><option value="30">30</option><option value="31">31</option><option value="32">32</option><option value="33">33</option><option value="34">34</option><option value="35">35</option><option value="36">36</option><option value="37">37</option><option value="38">38</option><option value="39">39</option><option value="40">40</option><option value="41">41</option><option value="42">42</option><option value="43">43</option><option value="44">44</option><option value="45">45</option><option value="46">46</option><option value="47">47</option><option value="48">48</option><option value="49">49</option><option value="50">50</option><option value="51">51</option><option value="52">52</option><option value="53">53</option><option value="54">54</option><option value="55">55</option><option value="56">56</option><option value="57">57</option><option value="58">58</option><option value="59">59</option>
                            </select>
                        </div>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.waitTimeout')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.repack.databases">Database Specific Options</label>
                        <label for="repackPerDbs" class="switch yes-no" data-field="spec.repack.databases">
                            Enable
                            <input type="checkbox" id="repackPerDbs" v-model="repackPerDbs" data-switch="ON">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases')"></span>
                    </div>
                </div>

                <template v-if="repackPerDbs">
                    <hr style="margin: -10px 0 25px;" />
                    
                    <fieldset>
                        <div class="header">
                            <h3 for="spec.vacuum.databases">
                                Databases
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases')"></span>
                            </h3>
                        </div>
                        
                        <div class="repackDbs repeater">
                            <fieldset v-for="(db, index) in repackDbs">
                                <div class="header">
                                    <h3>Database #{{ index+1 }} <template v-if="db.hasOwnProperty('name') && db.name.length">–</template> <span class="scriptTitle">{{ db.name }}</span></h3>
                                    <a class="addRow" @click="spliceArray('repackDbs', index)">Delete</a>
                                </div>    
                                <div class="row row-50">
                                    <div class="col">
                                        <label for="spec.repack.databases.name">Name <span class="req">*</span></label>
                                        <input v-model="db.name" placeholder="Type a name..." required autocomplete="off" data-field="spec.repack.databases.name">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases.name')"></span>
                                    </div>

                                    <div class="col">
                                        <label for="spec.repack.databases.noOrder">No Order</label>
                                        <select v-model="db.noOrder" data-field="spec.repack.databases.noOrder">
                                            <option value="inherit" selected>Inherit from global settings</option>
                                            <option :value="true">ON</option>
                                            <option :value="false">OFF</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases.noOrder')"></span>
                                    </div>

                                    <div class="col">                                
                                        <label for="spec.repack.databases.noKillBackend">No Kill Backend</label>
                                        <select v-model="db.noKillBackend" data-field="spec.repack.databases.noKillBackend">
                                            <option value="inherit" selected>Inherit from global settings</option>
                                            <option :value="true">ON</option>
                                            <option :value="false">OFF</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases.noKillBackend')"></span>
                                    </div>

                                    <div class="col">
                                        <label for="spec.repack.databases.noAnalyze">No Analyze</label>
                                        <select v-model="db.noAnalyze" data-field="spec.repack.databases.noAnalyze">
                                            <option value="inherit" selected>Inherit from global settings</option>
                                            <option :value="true">ON</option>
                                            <option :value="false">OFF</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases.noAnalyze')"></span>
                                    </div>

                                    <div class="col">
                                        <label for="spec.repack.databases.excludeExtension">Exclude Extension</label>
                                        <select v-model="db.excludeExtension" data-field="spec.repack.databases.excludeExtension">
                                            <option value="inherit" selected>Inherit from global settings</option>
                                            <option :value="true">ON</option>
                                            <option :value="false">OFF</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases.excludeExtension')"></span>
                                    </div>

                                    <div class="col">
                                        <label for="spec.repack.databases.waitTimeout">Wait Timeout</label>
                                        <select v-model="db.inheritTimeout" data-field="spec.repack.databases.waitTimeout">
                                            <option :value="true">Inherit from global settings</option>
                                            <option :value="false">Set custom timeout</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases.waitTimeout')"></span>
                                    </div>

                                    <div class="col" v-if="!db.inheritTimeout">
                                        <label for="spec.repack.databases.waitTimeout">Custom Wait Timeout</label>
                                        <div class="timeSelect">
                                            <select v-model="db.waitTimeout.d" class="round dayselect" data-field="spec.repack.databases.waitTimeout.days">
                                                <option disabled selected value="inherit">Days</option><option value="inherit">Inherit from global</option><option value="0">0</option><option value="1">1</option><option value="2">2</option><option value="3">3</option><option value="4">4</option><option value="5">5</option><option value="6">6</option><option value="7">7</option><option value="8">8</option><option value="9">9</option><option value="10">10</option>
                                            </select>
                                            <select v-model="db.waitTimeout.h" class="round hourselect" data-field="spec.repack.databases.waitTimeout.hours">
                                                <option disabled selected value="inherit">Hours</option><option value="inherit">Inherit from global</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option>
                                            </select>
                                            <select v-model="db.waitTimeout.m" class="round minuteselect" data-field="spec.repack.databases.waitTimeout.minutes">
                                                <option disabled selected value="inherit">Minutes</option><option value="inherit">Inherit from global</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option><option value="24">24</option><option value="25">25</option><option value="26">26</option><option value="27">27</option><option value="28">28</option><option value="29">29</option><option value="30">30</option><option value="31">31</option><option value="32">32</option><option value="33">33</option><option value="34">34</option><option value="35">35</option><option value="36">36</option><option value="37">37</option><option value="38">38</option><option value="39">39</option><option value="40">40</option><option value="41">41</option><option value="42">42</option><option value="43">43</option><option value="44">44</option><option value="45">45</option><option value="46">46</option><option value="47">47</option><option value="48">48</option><option value="49">49</option><option value="50">50</option><option value="51">51</option><option value="52">52</option><option value="53">53</option><option value="54">54</option><option value="55">55</option><option value="56">56</option><option value="57">57</option><option value="58">58</option><option value="59">59</option>
                                            </select>
                                            <select v-model="db.waitTimeout.s" class="round secondselect" data-field="spec.repack.databases.waitTimeout.seconds">
                                                <option disabled selected value="inherit">Seconds</option><option value="inherit">Inherit from global</option><option value="0">00</option><option value="1">01</option><option value="2">02</option><option value="3">03</option><option value="4">04</option><option value="5">05</option><option value="6">06</option><option value="7">07</option><option value="8">08</option><option value="9">09</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option><option value="24">24</option><option value="25">25</option><option value="26">26</option><option value="27">27</option><option value="28">28</option><option value="29">29</option><option value="30">30</option><option value="31">31</option><option value="32">32</option><option value="33">33</option><option value="34">34</option><option value="35">35</option><option value="36">36</option><option value="37">37</option><option value="38">38</option><option value="39">39</option><option value="40">40</option><option value="41">41</option><option value="42">42</option><option value="43">43</option><option value="44">44</option><option value="45">45</option><option value="46">46</option><option value="47">47</option><option value="48">48</option><option value="49">49</option><option value="50">50</option><option value="51">51</option><option value="52">52</option><option value="53">53</option><option value="54">54</option><option value="55">55</option><option value="56">56</option><option value="57">57</option><option value="58">58</option><option value="59">59</option>
                                            </select>
                                        </div>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases.waitTimeout')"></span>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="!repackDbs.length && 'topBorder'">
                                <a class="addRow" @click="pushDatabase('repack')">Add Database</a>
                            </div>
                        </div>
                    </fieldset>
                </template>
            </fieldset>

            <button class="btn" type="submit" @click="createDbOps()">Create Operation</button>
            
            <button @click="cancel()" class="btn border">Cancel</button>
        
            <button type="button" class="btn floatRight" @click="createDbOps(true)">View Summary</button>
        </form>

        <CRDSummary :crd="previewCRD" kind="SGDbOps" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import axios from 'axios'
    import moment from 'moment'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateDbOps',

        mixins: [mixin],

        components: {
            CRDSummary
        },

        data: function() {

            return {
                previewCRD: {},
                showSummary: false,
                name: '',
                sgCluster: '',
                runAt: '',
                timeout: {
                    d: 0,
                    h: 0,
                    m: 0,
                    s: 0
                },
                maxRetries: 0,
                op: '',
                vacuum: {
                    full: false,
                    freeze: false,
                    analyze: true,
                    disablePageSkipping: false
                },
                vacuumPerDbs: false,
                vacuumDbs: [{
                    name: '',
                    full: 'inherit',
                    freeze: 'inherit',
                    analyze: 'inherit',
                    disablePageSkipping: 'inherit'
                }],
                restart: {
                    method: 'InPlace',
                    onlyPendingRestart: false
                },
                minorVersionUpgrade: {
                    method: 'InPlace',
                    postgresVersion: ''
                },
                securityUpgrade: {
                    method: 'InPlace'
                },
                majorVersionUpgrade: {
                    link: false,
                    clone: false,
                    check: false,
                    postgresVersion: '',
                    sgPostgresConfig: ''
                },
                benchmark: {
                    type: 'pgbench',
                    connectionType: 'primary-service',
                    pgbench: {
                        databaseSize: {
                            size: '',
                            unit: ''
                        },
                        duration: {
                            d: '',
                            h: '',
                            m: '',
                            s: ''
                        },
                        usePreparedStatements: false,
                        concurrentClients: 1,
                        threads: 1
                    }
                },
                benchmarkTypes: ['pgbench'],
                repack: {
                    noOrder: false,
                    waitTimeout: {
                        d: 0,
                        h: 0,
                        m: 0,
                        s: 0
                    },
                    noKillBackend: false,
                    noAnalyze: false,
                    excludeExtension: false,
                },
                repackPerDbs: false,
                repackDbs: [{
                    name: '',
                    noOrder: 'inherit',
                    inheritTimeout: true,
                    waitTimeout: {
                        d: 0,
                        h: 0,
                        m: 0,
                        s: 0
                    },
                    noKillBackend: 'inherit',
                    noAnalyze: 'inherit',
                    excludeExtension: 'inherit'
                }]
            }

        },
        
        computed: {

            allClusters() {
                return store.state.clusters
            },
            
            nameColission() {

                const vc = this;
                var nameColission = false;
                
                store.state.dbOps.forEach(function(item, index){
                    if( (item.name == vc.name) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },

            isReady() {
                return store.state.ready
            },

            tzOffset() {
                let offset = (new Date().getTimezoneOffset())/60;

                if(offset == 0) {
                    return false
                } else if (offset > 0) {
                    return offset.toString().substring(1) + ( (offset > 1) ? ' hours' : 'hour') + ' ahead';
                } else if (offset < 0) {
                    return offset.toString().substring(1) + ( (offset < -1) ? ' hours' : 'hour') + ' behind';
                }

            },

            runAtTimezone() {
                return this.runAt.length ? moment.utc(this.runAt).local().format('YYYY-MM-DD HH:mm:ss') : ''
            },

            timezone () {
                return store.state.timezone
            },

            postgresVersionsList() {
                return store.state.postgresVersions.vanilla
            },

            pgConfigs() {
                return store.state.pgConfig.filter(pgconfig => (pgconfig.data.metadata.namespace == this.$route.params.namespace))
            }

        },

        created: function() {
            $(document).on('click', '.opList label', function(){    
				$(this).addClass('active');
                $('.opList label').not($(this)).removeClass('active');
                $('.opList label').removeClass('notValid');
			});
        },

        methods: {

            getIsoDuration( duration ) {
                let isoD = 'P' + (duration.d.length ? duration.d : '0') + 'DT' + (duration.h.length ? duration.h : '0') + 'H' + (duration.m.length ? duration.m : '0') + 'M' + (duration.s.length ? duration.s : '0') + 'S';
                return (isoD == 'P0DT0H0M0S') ? null : isoD
            },

            createDbOps(preview = false) {
                const vc = this

                let isValid = vc.checkRequired();

                if(vc.op == 'benchmark') {
                
                    let reqFieldsetValid = false;
                    $('.reqFieldset select').each( function(index, item) {
                        if(item.value.length) {
                            reqFieldsetValid = true;
                            return false;
                        }
                    })

                    if(!reqFieldsetValid) {
                        $('.reqFieldset select').addClass('notValid');
                    }

                    isValid = isValid && reqFieldsetValid;

                }
                    
                if(isValid) {

                    let dbOps = {
                        metadata: {
                            name: vc.name,
                            namespace: vc.$route.params.namespace
                        },
                        spec: {
                            sgCluster: vc.sgCluster,
                            op: vc.op,
                            ...(vc.runAt.length && ( { runAt: vc.runAt }) ), 
                            ...(vc.getIsoDuration(vc.timeout) && ( { timeout: vc.getIsoDuration(vc.timeout) }) ),
                            maxRetries: vc.maxRetries
                        }
                    }

                    switch(vc.op) {
                        case 'vacuum':
                            if(vc.vacuumPerDbs) {
                                vc.vacuumDbs.forEach(function(db){
                                    Object.keys(db).forEach(function(key){
                                        if(db[key] == 'inherit')
                                            db[key] = vc.vacuum[key]
                                    })
                                })
                                vc.vacuum['databases'] = [...vc.vacuumDbs]
                            }

                            dbOps.spec['vacuum'] = vc.vacuum
                            break;

                        case 'repack':
                            let repack = {
                                noOrder: vc.repack.noOrder,
                                waitTimeout: vc.getIsoDuration(vc.repack.waitTimeout),
                                noKillBackend: vc.repack.noKillBackend,
                                noAnalyze: vc.repack.noAnalyze,
                                excludeExtension: vc.repack.excludeExtension,
                            }

                            if(vc.repackPerDbs) {
                                vc.repackDbs.forEach(function(db){
                                    Object.keys(db).forEach(function(key){
                                        if(db[key] == 'inherit')
                                            db[key] = vc.repack[key]
                                        
                                        if (key == 'waitTimeout') {
                                            let wT = db.inheritTimeout ? vc.getIsoDuration(vc.repack.waitTimeout) : vc.getIsoDuration(db.waitTimeout)
                                            delete db.waitTimeout
                                            delete db.inheritTimeout
                                            db['waitTimeout'] = wT
                                        }
                                    })
                                })
                                repack['databases'] = [...vc.repackDbs]
                            }

                            dbOps.spec['repack'] = repack
                            break;

                        case 'benchmark':
                            let benchmark = {
                                type: vc.benchmark.type,
                                connectionType: vc.benchmark.connectionType
                            }

                            benchmark[vc.benchmark.type] = {
                                databaseSize: vc.benchmark[vc.benchmark.type].databaseSize.size + vc.benchmark[vc.benchmark.type].databaseSize.unit,
                                duration: vc.getIsoDuration(vc.benchmark[vc.benchmark.type].duration),
                                usePreparedStatements: vc.benchmark[vc.benchmark.type].usePreparedStatements,
                                concurrentClients: vc.benchmark[vc.benchmark.type].concurrentClients,
                                threads: vc.benchmark[vc.benchmark.type].threads
                            }
                            dbOps.spec['benchmark'] = benchmark
                            break;

                        default:
                            dbOps.spec[vc.op] = vc[vc.op]
                            break;
                    }

                    if(preview) {                  

                        vc.previewCRD = {};
                        vc.previewCRD['data'] = dbOps;
                        vc.showSummary = true;

                    } else {
                        axios
                        .post(
                            '/stackgres/sgdbops', 
                            dbOps 
                        )
                        .then(function (response) {
                            vc.notify('Database operation "' + dbOps.metadata.name + '" created successfully', 'message', 'sgdbops');

                            vc.fetchAPI('sgdbops');
                            router.push('/' + vc.$route.params.namespace + '/sgdbops');
                            
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error','sgdbops');
                        });
                    }

                }
            },

            spliceArray: function( prop, index ) {
                this[prop].splice( index, 1 )
            },
            
            pushDatabase( op ) {
                const vc = this

                switch(op) {
                    case 'vacuum':
                        vc.vacuumDbs.push({
                            name: '',
                            full: 'inherit',
                            freeze: 'inherit',
                            analyze: 'inherit',
                            disablePageSkipping: 'inherit'
                        })
                        break;
                    case 'repack':
                        vc.repackDbs.push({
                            name: '',
                            noOrder: 'inherit',
                            waitTimeout: {
                                d: 0,
                                h: 0,
                                m: 0,
                                s: 0
                            },
                            noKillBackend: 'inherit',
                            noAnalyze: 'inherit',
                            excludeExtension: 'inherit'
                        })
                        break;
                }

            },

        },

        mounted: function() {
            
            // Load datepicker
			require('daterangepicker');

            const vc = this
            
            $(document).ready(function(){

                let minDate = new Date(new Date().getTime())
                
                $(document).on('focus', '.datePicker', function() {

                    if(!$(this).val()) {
                        $('.daterangepicker').remove()
                        $('.datePicker').daterangepicker({
                            "autoApply": false,
                            "singleDatePicker": true,
                            "timePicker": true,
                            "opens": "right",
                            "minDate": minDate,
                            "timePicker24Hour": true,
                            "timePickerSeconds": true,
                            "autoUpdateInput": false,
                            locale: {
                                cancelLabel: "Clear",
                                format: 'YYYY-MM-DD HH:mm:ss'
                            }
                        }, function(start, end, label) {
                            vc.runAt = (store.state.timezone == 'local') ? start.utc().format() : ( start.format('YYYY-MM-DDTHH:mm:ss') + 'Z' )
                        });
                    }
                });

                

                $(document).on('click','.daterangepicker .cancelBtn', function() {
                    $('.datePicker').val('');
                    vc.runAt = '';
                })
                
            })
        }
        
    }
</script>

<style scoped>
    .sectionTitle {
        display: block;
        margin-bottom: 20px;
    }

    .optionBoxes > label {
        height: 100px;
        padding: 0 10px;
    }

    .dbopIcon:before {
        display: block;
        content: "";
        height: 25px;
        width: 25px;
        position: relative;
        margin: 15px auto 10px;
    }

    .dbopIcon.active:before {
        filter: brightness(0) invert(1);
    }
    
    .darkmode .dbopIcon.active:before {
        filter: brightness(0.15);
    }

    .optionBoxes + .helpTooltip {
        transform: translate(20px, -100px);
    }

    .timeSelect {
        display: flex;
    }

    .timeSelect .round {
        margin-left: 10px;
        background-position: 90%;
    }

    .timeSelect .round.dayselect {
        margin-left: 0;
    }

    .repeater a.help {
        right: -88px;
    }

    #createDbops > fieldset {
        margin-top: 30px;
    }

    .fieldsetFooter {
        margin-bottom: 25px;
    }

    .majorVersionUpgrade, .securityUpgrade, .minorVersionUpgrade {
        padding-bottom: 15px;
    }

    fieldset > .warning:first-child:last-child {
       margin-bottom: 5px;
    }

</style>