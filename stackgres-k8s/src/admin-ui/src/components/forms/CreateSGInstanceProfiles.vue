<template>
    <div id="create-profile" class="createProfile noSubmit" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(config).length > 0"></template>

        <form id="createProfile" class="form" @submit.prevent>
            <div class="header stickyHeader">
                <h2>
                    {{ editMode ? 'Edit' : 'Create' }} Instance Profile
                </h2>
            </div>
            <div class="stepsContainer">
                <ul class="steps">
                    <button type="button" class="btn arrow prev" @click="currentStep = formSteps[(currentStepIndex - 1)]" :disabled="( currentStepIndex == 0 )"></button>
            
                    <template v-for="(step, index) in formSteps">
                        <li @click="currentStep = step; checkValidSteps(_data, 'steps')" :class="[( (currentStep == step) && 'active'), ( !index && 'basic' ), (errorStep.includes(step) && 'notValid')]" v-if="!( editMode && (step == 'initialization') && !restoreBackup.length )" :data-step="step">
                            {{ splitUppercase(step) }}
                        </li>
                    </template>

                    <button type="button" class="btn arrow next" @click="currentStep = formSteps[(currentStepIndex + 1)]" :disabled="(currentStepIndex == (formSteps.length - 1))"></button>
                </ul>
            </div>

            <div class="clearfix"></div>

            <fieldset class="step" :class="(currentStep == 'profile') && 'active'" data-fieldset="profile">
                <div class="header">
                    <h2>
                        Profile Information
                    </h2>
                </div>
                <div class="row-50">
                    <div class="col">
                        <label for="metadata.name">Profile Name <span class="req">*</span></label>
                        <input v-model="profileName" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.metadata.name')"></span>
                    </div>
                
                    <div class="col">
                        <div class="unit-select">
                            <label for="spec.memory">RAM <span class="req">*</span></label>
                            <input v-model="profileRAM" class="size" required data-field="spec.memory" type="number" min="0">

                            <select v-model="profileRAMUnit" class="unit" required data-field="spec.memory">
                                <option value="Mi">MiB</option>
                                <option value="Gi" selected>GiB</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.memory')"></span>
                        </div>
                    </div>

                    <span class="warning topLeft" v-if="nameColission && !editMode">
                        There's already a <strong>SGInstanceProfile</strong> with the same name on this namespace. Please specify a different name or create the profile on another namespace
                    </span>

                    <div class="col">
                        <div class="unit-select">
                            <label for="spec.cpu">CPU <span class="req">*</span></label>
                            <input v-model="profileCPU" class="size" required data-field="spec.cpu" type="number" min="0">

                            <select v-model="profileCPUUnit" class="unit" required data-field="spec.cpu">
                                <option selected>CPU</option>
                                <option value="m">millicpu</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.cpu')"></span>
                        </div>
                    </div>

                    <div class="header">
                        <h2>
                            Huge Pages Specs
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.hugePages')"></span>
                        </h2>
                    </div>

                    <div class="col">
                        <div class="unit-select">
                            <label for="spec.hugePages.hugepages-2Mi">Huge Pages 2Mi</label>
                            <input v-model="hugePages2Mi" class="size" data-field="spec.hugePages.hugepages-2Mi" type="number" min="0">

                            <select v-model="hugePages2MiUnit" class="unit" data-field="spec.hugePages.hugepages-2Mi">
                                <option value="Mi">MiB</option>
                                <option value="Gi" selected>GiB</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.hugePages.hugepages-2Mi')"></span>
                        </div>
                    </div>
                    
                    <div class="col">
                        <div class="unit-select">
                            <label for="spec.hugePages.hugepages-1Gi">Huge Pages 1Gi</label>
                            <input v-model="hugePages1Gi" class="size" data-field="spec.hugePages.hugepages-1Gi" type="number" min="0">

                            <select v-model="hugePages1GiUnit" class="unit" data-field="spec.hugePages.hugepages-1Gi">
                                <option value="Mi">MiB</option>
                                <option value="Gi" selected>GiB</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.hugePages.hugepages-1Gi')"></span>
                        </div>
                    </div>
                </div>

            </fieldset>

            
            <fieldset class="step" :class="(currentStep == 'containers') && 'active'" data-fieldset="containers">
                <div class="header">
                    <h2>
                        Containers
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.containers')"></span>
                    </h2>
                </div>
                <div class="repeater">
                    <fieldset v-for="(container, index) in containers">
                        <div class="header">
                            <h3>
                                Container #{{ (index + 1) + (container.name.length && (': ' + container.name) ) }}
                            </h3>
                            <div class="addRow">
                                <a @click="spliceArray(containers, index)">Delete Container</a>
                            </div>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.containers">Container Name</label>
                                <input v-model="container.name" :required="( (container.RAM.length > 0) || (container.CPU.length > 0) )" :data-field="'spec.containers[' + index + '].name'">
                            </div>
                        </div>
                        <br/><br/>
                        <div class="header">
                            <h4>
                                Container Specs
                            </h4>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.containers.memory">RAM</label>
                                    <input v-model="container.RAM" :required="( (container.name.length > 0) || (container.CPU.length > 0) )" class="size" :data-field="'spec.containers[' + index + '].memory'" type="number" min="0">

                                    <select v-model="container.RAMUnit" class="unit" :required="(container.RAM.length > 0)" :data-field="'spec.containers[' + index + '].memoryUnit'">
                                        <option value="Mi">MiB</option>
                                        <option value="Gi" selected>GiB</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.containers.additionalProperties.properties.memory')"></span>
                                </div>
                            </div>

                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.containers.cpu">CPU</label>
                                    <input v-model="container.CPU" :required="( (container.name.length > 0) || (container.RAM.length > 0) )" class="size" :data-field="'spec.containers[' + index + '].cpu'" type="number" min="0">

                                    <select v-model="container.CPUUnit" class="unit" :required="(container.CPU.length > 0)" :data-field="'spec.containers[' + index + '].cpuUnit'">
                                        <option selected>CPU</option>
                                        <option value="m">millicpu</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.containers.additionalProperties.properties.cpu')"></span>
                                </div>
                            </div>
                        </div>

                        <div class="header">
                            <h4>
                                Container Huge Pages Specs
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.containers.additionalProperties.properties.hugePages')"></span>
                            </h4>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.containers.hugePages.hugepages-2Mi">Huge Pages 2Mi</label>
                                    <input v-model="container.hugePages2Mi" class="size" :data-field="'spec.containers[' + index + '].hugePages.hugepages-2Mi'" type="number" min="0">

                                    <select v-model="container.hugePages2MiUnit" class="unit" :required="container.hugePages2Mi.length" :data-field="'spec.containers[' + index + '].hugePages.hugepages-2MiUnit'">
                                        <option value="Mi">MiB</option>
                                        <option value="Gi" selected>GiB</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.containers.additionalProperties.properties.hugePages.properties.hugepages-2Mi')"></span>
                                </div>
                            </div>
                            
                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.containers.hugePages.hugepages-1Gi">Huge Pages 1Gi</label>
                                    <input v-model="container.hugePages1Gi" class="size" :data-field="'spec.containers[' + index + '].hugePages.hugepages-1Gi'" type="number" min="0">

                                    <select v-model="container.hugePages1GiUnit" class="unit" :required="container.hugePages1Gi.length" :data-field="'spec.containers[' + index + '].hugePages.hugepages-1GiUnit'">
                                        <option value="Mi">MiB</option>
                                        <option value="Gi" selected>GiB</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.containers.additionalProperties.properties.hugePages.properties.hugepages-1Gi')"></span>
                                </div>
                            </div>
                        </div>
                    </fieldset>
                    <div class="fieldsetFooter" :class="(!containers.length && 'topBorder')">
                        <a class="addRow"
                            @click="containers.push({
                                name: '',
                                CPU: '',
                                CPUUnit: 'CPU',
                                RAM: '',
                                RAMUnit: 'Gi',
                                hugePages1Gi: '',
                                hugePages1GiUnit: 'Gi',
                                hugePages2Mi: '',
                                hugePages2MiUnit: 'Gi',
                            })">
                            Add Container
                        </a>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'initContainers') && 'active'" data-fieldset="initContainers">
                <div class="header">
                    <h2>
                        Init Containers
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.initContainers')"></span>
                    </h2>
                </div>
                <div class="repeater">
                    <fieldset v-for="(container, index) in initContainers">
                        <div class="header">
                            <h3>
                                Container #{{ (index + 1) + (container.name.length && (': ' + container.name) ) }}
                            </h3>
                            <div class="addRow">
                                <a @click="spliceArray(initContainers, index)">Delete Container</a>
                            </div>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.initContainers">Container Name</label>
                                <input v-model="container.name" :required="( (container.RAM.length > 0) || (container.CPU.length > 0) )" :data-field="'spec.initContainers[' + index + '].name'">
                            </div>
                        </div>
                        <br/><br/>
                        <div class="header">
                            <h4>
                                Container Specs
                            </h4>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.initContainers.memory">RAM</label>
                                    <input v-model="container.RAM" :required="( (container.name.length > 0) || (container.CPU.length > 0) )" class="size" :data-field="'spec.initContainers[' + index + '].memory'" type="number" min="0">

                                    <select v-model="container.RAMUnit" class="unit" :required="(container.RAM.length > 0)" :data-field="'spec.initContainers[' + index + '].memoryUnit'">
                                        <option value="Mi">MiB</option>
                                        <option value="Gi" selected>GiB</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.initContainers.additionalProperties.properties.memory')"></span>
                                </div>
                            </div>

                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.initContainers.cpu">CPU</label>
                                    <input v-model="container.CPU" :required="( (container.name.length > 0) || (container.RAM.length > 0) )" class="size" :data-field="'spec.initContainers[' + index + '].cpu'" type="number" min="0">

                                    <select v-model="container.CPUUnit" class="unit" :required="(container.CPU.length > 0)" :data-field="'spec.initContainers[' + index + '].cpuUnit'">
                                        <option selected>CPU</option>
                                        <option value="m">millicpu</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.initContainers.additionalProperties.properties.cpu')"></span>
                                </div>
                            </div>
                        </div>

                        <div class="header">
                            <h4>
                                Container Huge Pages Specs
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.initContainers.additionalProperties.properties.hugePages')"></span>
                            </h4>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.initContainers.hugePages.hugepages-2Mi">Huge Pages 2Mi</label>
                                    <input v-model="container.hugePages2Mi" class="size" :data-field="'spec.initContainers[' + index + '].hugePages.hugepages-2Mi'" type="number" min="0">

                                    <select v-model="container.hugePages2MiUnit" class="unit" :required="container.hugePages2Mi.length" :data-field="'spec.initContainers[' + index + '].hugePages.hugepages-2MiUnit'">
                                        <option value="Mi">MiB</option>
                                        <option value="Gi" selected>GiB</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.initContainers.additionalProperties.properties.hugePages.properties.hugepages-2Mi')"></span>
                                </div>
                            </div>
                            
                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.initContainers.hugePages.hugepages-1Gi">Huge Pages 1Gi</label>
                                    <input v-model="container.hugePages1Gi" class="size" :data-field="'spec.initContainers[' + index + '].hugePages.hugepages-1Gi'" type="number" min="0">

                                    <select v-model="container.hugePages1GiUnit" class="unit" :required="container.hugePages1Gi.length" :data-field="'spec.initContainers[' + index + '].hugePages.hugepages-1GiUnit'">
                                        <option value="Mi">MiB</option>
                                        <option value="Gi" selected>GiB</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.initContainers.additionalProperties.properties.hugePages.properties.hugepages-1Gi')"></span>
                                </div>
                            </div>
                        </div>
                    </fieldset>
                    <div class="fieldsetFooter" :class="(!initContainers.length && 'topBorder')">
                        <a class="addRow"
                            @click="initContainers.push({
                                name: '',
                                CPU: '',
                                CPUUnit: 'CPU',
                                RAM: '',
                                RAMUnit: 'Gi',
                                hugePages1Gi: '',
                                hugePages1GiUnit: 'Gi',
                                hugePages2Mi: '',
                                hugePages2MiUnit: 'Gi',
                            })">
                            Add Container
                        </a>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'requests') && 'active'" data-fieldset="requests">
                <div class="header">
                    <h2>
                        Requests
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.requests')"></span>
                    </h2>
                </div>
                <div class="row-50">
                    <div class="col">
                        <div class="unit-select">
                            <label for="spec.requests.memory">RAM</label>
                            <input v-model="requests.RAM" class="size" data-field="spec.requests.memory" type="number" min="0">

                            <select v-model="requests.RAMUnit" class="unit" :required="requests.RAM.length" data-field="spec.requests.memory">
                                <option value="Mi">MiB</option>
                                <option value="Gi" selected>GiB</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.requests.memory')"></span>
                        </div>
                    </div>

                    <div class="col">
                        <div class="unit-select">
                            <label for="spec.requests.cpu">CPU</label>
                            <input v-model="requests.CPU" class="size" data-field="spec.requests.cpu" type="number" min="0">

                            <select v-model="requests.CPUUnit" class="unit" :required="requests.CPU.length" data-field="spec.requests.cpu">
                                <option selected>CPU</option>
                                <option value="m">millicpu</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.requests.cpu')"></span>
                        </div>
                    </div>
                </div>

                <div class="header">
                    <h2>
                        Containers Specs
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.requests.containers')"></span>
                    </h2>
                </div>

                <div class="repeater">
                    <fieldset v-for="(container, index) in requests.containers" class="containers">
                        <div class="header">
                            <h3>
                                Container #{{ (index + 1) + (container.name.length && (': ' + container.name) ) }}
                            </h3>
                            <div class="addRow">
                                <a @click="spliceArray(requests.containers, index)">Delete Container</a>
                            </div>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.requests.containers">Container Name</label>
                                <input v-model="container.name" :required="( (container.RAM.length > 0) || (container.CPU.length > 0) )" :data-field="'spec.requests.containers[' + index + '].name'">
                            </div>
                        </div>
                        <br/><br/>
                        <div class="header">
                            <h4>
                                Container Specs
                            </h4>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.requests.containers.memory">RAM</label>
                                    <input v-model="container.RAM" :required="( (container.name.length > 0) || (container.CPU.length > 0) )" class="size" :data-field="'spec.requests.containers[' + index + '].memory'" type="number" min="0">

                                    <select v-model="container.RAMUnit" class="unit" :required="(container.RAM.length > 0)" :data-field="'spec.requests.containers[' + index + '].memoryUnit'">
                                        <option value="Mi">MiB</option>
                                        <option value="Gi" selected>GiB</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.requests.containers.additionalProperties.properties.memory')"></span>
                                </div>
                            </div>

                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.requests.containers.cpu">CPU</label>
                                    <input v-model="container.CPU" :required="( (container.name.length > 0) || (container.RAM.length > 0) )" class="size" :data-field="'spec.requests.containers[' + index + '].cpu'" type="number" min="0">

                                    <select v-model="container.CPUUnit" class="unit" :required="(container.CPU.length > 0)" :data-field="'spec.requests.containers[' + index + '].cpuUnit'">
                                        <option selected>CPU</option>
                                        <option value="m">millicpu</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.requests.containers.additionalProperties.properties.cpu')"></span>
                                </div>
                            </div>
                        </div>
                    </fieldset>
                    <div class="fieldsetFooter" :class="(!requests.containers.length && 'topBorder')">
                        <a class="addRow"
                            @click="requests.containers.push({
                                name: '',
                                CPU: '',
                                CPUUnit: 'CPU',
                                RAM: '',
                                RAMUnit: 'Gi'
                            })">
                            Add Container
                        </a>
                    </div>
                </div>

                <br/><br/>

                <div class="header">
                    <h2>
                        Init Containers Specs
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.requests.initContainers')"></span>
                    </h2>
                </div>

                <div class="repeater">
                    <fieldset v-for="(container, index) in requests.initContainers" class="initContainers">
                        <div class="header">
                            <h3>
                                Container #{{ (index + 1) + (container.name.length && (': ' + container.name) ) }}
                            </h3>
                            <div class="addRow">
                                <a @click="spliceArray(requests.initContainers, index)">Delete Container</a>
                            </div>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.requests.initContainers">Container Name</label>
                                <input v-model="container.name" :required="( (container.RAM.length > 0) || (container.CPU.length > 0) )" :data-field="'spec.requests.initContainers[' + index + '].name'">
                            </div>
                        </div>
                        <br/><br/>
                        <div class="header">
                            <h4>
                                Container Specs
                            </h4>
                        </div>
                        <div class="row-50">
                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.requests.initContainers.memory">RAM</label>
                                    <input v-model="container.RAM" :required="( (container.name.length > 0) || (container.CPU.length > 0) )" class="size" :data-field="'spec.requests.initContainers[' + index + '].memory'" type="number" min="0">

                                    <select v-model="container.RAMUnit" class="unit" :required="(container.RAM.length > 0)" :data-field="'spec.requests.initContainers[' + index + '].memoryUnit'">
                                        <option value="Mi">MiB</option>
                                        <option value="Gi" selected>GiB</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.requests.initContainers.additionalProperties.properties.memory')"></span>
                                </div>
                            </div>

                            <div class="col">
                                <div class="unit-select">
                                    <label for="spec.requests.initContainers.cpu">CPU</label>
                                    <input v-model="container.CPU" :required="( (container.name.length > 0) || (container.RAM.length > 0) )" class="size" :data-field="'spec.requests.initContainers[' + index + '].cpu'" type="number" min="0">

                                    <select v-model="container.CPUUnit" class="unit" :required="(container.CPU.length > 0)" :data-field="'spec.requests.initContainers[' + index + '].cpuUnit'">
                                        <option selected>CPU</option>
                                        <option value="m">millicpu</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgprofile.spec.requests.initContainers.additionalProperties.properties.cpu')"></span>
                                </div>
                            </div>
                        </div>
                    </fieldset>
                    <div class="fieldsetFooter" :class="(!requests.initContainers.length && 'topBorder')">
                        <a class="addRow"
                            @click="requests.initContainers.push({
                                name: '',
                                CPU: '',
                                CPUUnit: 'CPU',
                                RAM: '',
                                RAMUnit: 'Gi'
                            })">
                            Add Container
                        </a>
                    </div>
                </div>
                
            </fieldset>
                                
            <hr/>
            
            <template v-if="editMode">
                <template v-if="profileClusters.length">
                    <br/><br/>
                    <span class="warning">Please, be aware that any changes made to this instance profile will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance on the following {{ (profileClusters.length > 1) ? 'clusters' : 'cluster' }}: <strong>{{ profileClusters.join(", ") }}</strong> </span>
                </template>

                <button class="btn" type="submit" @click="createProfile()">Update Profile</button>
            </template>
            <template v-else>
                <button class="btn" type="submit" @click="createProfile()">Create Profile</button>
            </template>

            <button @click="cancel" class="btn border">Cancel</button>

            <button type="button" class="btn floatRight" @click="createProfile(true)">View Summary</button>
        </form>
        <CRDSummary :crd="previewCRD" kind="SGInstanceProfile" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateSGInstanceProfiles',

        mixins: [mixin],

        components: {
            CRDSummary
        },

        data: function() {

            const vm = this;

            return {
                formSteps: ['profile', 'containers', 'initContainers', 'requests'],
                currentStep: 'profile',
                errorStep: [],
                editMode: (vm.$route.name === 'EditProfile'),
                editReady: false,
                previewCRD: {},
                showSummary: false,
                profileName: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                profileNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                profileCPU: '',
                profileCPUUnit: 'CPU',
                profileRAM: '',
                profileRAMUnit: 'Gi',
                hugePages1Gi: '',
                hugePages1GiUnit: 'Gi',
                hugePages2Mi: '',
                hugePages2MiUnit: 'Gi',
                containers: [
                    {
                        name: '',
                        CPU: '',
                        CPUUnit: 'CPU',
                        RAM: '',
                        RAMUnit: 'Gi',
                        hugePages1Gi: '',
                        hugePages1GiUnit: 'Gi',
                        hugePages2Mi: '',
                        hugePages2MiUnit: 'Gi',
                    }
                ],
                initContainers: [
                    {
                        name: '',
                        CPU: '',
                        CPUUnit: 'CPU',
                        RAM: '',
                        RAMUnit: 'Gi',
                        hugePages1Gi: '',
                        hugePages1GiUnit: 'Gi',
                        hugePages2Mi: '',
                        hugePages2MiUnit: 'Gi',
                    }
                ],
                requests: {
                    CPU: '',
                    CPUUnit: 'CPU',
                    RAM: '',
                    RAMUnit: 'Gi',
                    containers: [
                        {
                            name: '',
                            CPU: '',
                            CPUUnit: 'CPU',
                            RAM: '',
                            RAMUnit: 'Gi',
                            hugePages1Gi: '',
                            hugePages1GiUnit: 'Gi',
                            hugePages2Mi: '',
                            hugePages2MiUnit: 'Gi',
                        }
                    ],
                    initContainers: [
                        {
                            name: '',
                            CPU: '',
                            CPUUnit: 'CPU',
                            RAM: '',
                            RAMUnit: 'Gi',
                            hugePages1Gi: '',
                            hugePages1GiUnit: 'Gi',
                            hugePages2Mi: '',
                            hugePages2MiUnit: 'Gi',
                        }
                    ],
                },
                profileClusters: []
            }
                
            
        },
        computed: {
            allNamespaces () {
                return store.state.allNamespaces
            },

            tooltipsText() {
                return store.state.tooltipsText
            },

            nameColission() {
                const vc = this;
                var nameColission = false;
                
                store.state.sginstanceprofiles.forEach(function(item, index) {
                    if( (item.name == vc.profileName) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },

            config() {
                var vm = this;
                var config = {};
                
                if( vm.editMode && !vm.editReady ) {
                    store.state.sginstanceprofiles.forEach(function( conf ){
                        if( (conf.data.metadata.name === vm.$route.params.name) && (conf.data.metadata.namespace === vm.$route.params.namespace) ) {
                            vm.profileCPU = conf.data.spec.cpu.match(/\d+/g)[0];
                            vm.profileCPUUnit = (conf.data.spec.cpu.match(/[a-zA-Z]+/g) !== null) ? conf.data.spec.cpu.match(/[a-zA-Z]+/g)[0] : 'CPU';
                            vm.profileRAM = conf.data.spec.memory.match(/\d+/g)[0];
                            vm.profileRAMUnit = conf.data.spec.memory.match(/[a-zA-Z]+/g)[0];
                            vm.hugePages1Gi = vm.hasProp(conf, 'data.spec.hugePages.hugepages-1Gi') ? conf.data.spec.hugePages['hugepages-1Gi'].match(/\d+/g)[0] : '';
                            vm.hugePages1GiUnit = vm.hasProp(conf, 'data.spec.hugePages.hugepages-1Gi') ? conf.data.spec.hugePages['hugepages-1Gi'].match(/[a-zA-Z]+/g)[0] : 'Gi';
                            vm.hugePages2Mi = vm.hasProp(conf, 'data.spec.hugePages.hugepages-2Mi') ? conf.data.spec.hugePages['hugepages-2Mi'].match(/\d+/g)[0] : '';
                            vm.hugePages2MiUnit = vm.hasProp(conf, 'data.spec.hugePages.hugepages-2Mi') ? conf.data.spec.hugePages['hugepages-2Mi'].match(/[a-zA-Z]+/g)[0] : 'Gi';
                            vm.containers = vm.hasProp(conf, 'data.spec.containers') ? vm.unparseContainers(conf.data.spec.containers) : [];
                            vm.initContainers = vm.hasProp(conf, 'data.spec.initContainers') ? vm.unparseContainers(conf.data.spec.initContainers) : [];
                            vm.requests = {
                                CPU: vm.hasProp(conf, 'data.spec.requests.cpu') ? conf.data.spec.requests.cpu.match(/\d+/g)[0] : '',
                                CPUUnit: ( ( vm.hasProp(conf, 'data.spec.requests.cpu') && (conf.data.spec.requests.cpu.match(/[a-zA-Z]+/g) !== null) ) ? conf.data.spec.requests.cpu.match(/[a-zA-Z]+/g)[0] : 'CPU' ),
                                RAM: ( vm.hasProp(conf, 'data.spec.requests.memory') ? conf.data.spec.requests.memory.match(/\d+/g)[0] : '' ),
                                RAMUnit: ( vm.hasProp(conf, 'data.spec.requests.memory') ? conf.data.spec.requests.memory.match(/[a-zA-Z]+/g)[0] : 'Gi' ),
                                containers: ( vm.hasProp(conf, 'data.spec.requests.containers') ? vm.unparseContainers(conf.data.spec.requests.containers) : [
                                        {
                                            name: '',
                                            CPU: '',
                                            CPUUnit: 'CPU',
                                            RAM: '',
                                            RAMUnit: 'Gi',
                                            hugePages1Gi: '',
                                            hugePages1GiUnit: 'Gi',
                                            hugePages2Mi: '',
                                            hugePages2MiUnit: 'Gi',
                                        }
                                    ]
                                ),
                                initContainers: ( vm.hasProp(conf, 'data.spec.requests.initContainers') ? vm.unparseContainers(conf.data.spec.requests.initContainers) : [
                                        {
                                            name: '',
                                            CPU: '',
                                            CPUUnit: 'CPU',
                                            RAM: '',
                                            RAMUnit: 'Gi',
                                            hugePages1Gi: '',
                                            hugePages1GiUnit: 'Gi',
                                            hugePages2Mi: '',
                                            hugePages2MiUnit: 'Gi',
                                        }
                                    ]
                                ),
                            }
                            vm.profileClusters = [...conf.data.status.clusters]
                            config = conf;

                            vm.editReady = true
                            return false
                        }
                    });
                }
            
                return config
            },

            currentStepIndex() {
                return this.formSteps.indexOf(this.currentStep)
            }
        },
        methods: {

            createProfile(preview = false, previous) {
                const vc = this;

                if(!vc.checkRequired()) {
                    return;
                }

                if (!previous && vc.editMode) {
                    sgApi
                    .getResourceDetails('sginstanceprofiles', this.profileNamespace, this.profileName)
                    .then(function (response) {
                        vc.createProfile(preview, response.data);
                    })
                    .catch(function (error) {
                        if ( error.hasOwnProperty('response') && (error.response.status != 404) ) {
                          console.log(error.response);
                          vc.notify(error.response.data,'error', 'sginstanceprofiles');
                          return;
                        }
                        vc.createProfile(preview, {});
                    });
                    return;
                }

                var profile = {
                    "metadata": {
                        ...(this.hasProp(previous, 'metadata') && previous.metadata),
                        "name": this.profileName,
                        "namespace": this.profileNamespace
                    },
                    "spec": {
                        ...(this.hasProp(previous, 'spec') && previous.spec),
                        "cpu": (this.profileCPUUnit !== 'CPU') ? (this.profileCPU + this.profileCPUUnit) : this.profileCPU,
                        "memory": (this.profileRAM + this.profileRAMUnit),
                        ...( (this.hugePages1Gi.length || this.hugePages2Mi.length) && {
                            "hugePages": {
                                ...( this.hugePages2Mi.length && {
                                    "hugepages-2Mi": this.hugePages2Mi + this.hugePages2MiUnit
                                }),
                                ...( this.hugePages1Gi.length && {
                                    "hugepages-1Gi": this.hugePages1Gi + this.hugePages1GiUnit
                                }),
                            }
                        } || {"hugePages": null} ),
                        "containers": this.parseContainers(this.containers),
                        "initContainers": this.parseContainers(this.initContainers),
                        ...( ( this.requests.RAM.length || this.requests.CPU.length || (this.parseContainers(this.requests.containers) != null) || (this.parseContainers(this.requests.initContainers) != null) ) && {
                            "requests": {
                                ...( this.requests.CPU.length && {
                                    "cpu": (this.requests.CPUUnit !== 'CPU') ? (this.requests.CPU + this.requests.CPUUnit) : this.requests.CPU
                                }),
                                ...( this.requests.RAM.length && {
                                    "memory": (this.requests.RAM + this.requests.RAMUnit),
                                }),
                                "containers": this.parseContainers(this.requests.containers),
                                "initContainers": this.parseContainers(this.requests.initContainers),
                            }
                        } || {"requests": null} )
                    }
                }

                if(preview) {

                    vc.previewCRD = {};
                    vc.previewCRD['data'] = profile;
                    vc.showSummary = true;

                } else {

                    if(this.editMode) {
                        sgApi
                        .update('sginstanceprofiles', profile)
                        .then(function (response) {
                            vc.notify('Profile <strong>"'+profile.metadata.name+'"</strong> updated successfully', 'message','sginstanceprofiles');

                            vc.fetchAPI('sginstanceprofile');
                            router.push('/' + profile.metadata.namespace + '/sginstanceprofile/' + profile.metadata.name);

                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error','sginstanceprofiles');
                        });

                    } else {
                        sgApi
                        .create('sginstanceprofiles', profile)
                        .then(function (response) {

                            var urlParams = new URLSearchParams(window.location.search);
                            if(urlParams.has('newtab')) {
                                opener.fetchParentAPI('sginstanceprofile');
                                vc.notify('Profile <strong>"'+profile.metadata.name+'"</strong> created successfully.<br/><br/> You may now close this window and choose your profile from the list.', 'message','sginstanceprofiles');
                            } else {
                                vc.notify('Profile <strong>"'+profile.metadata.name+'"</strong> created successfully', 'message','sginstanceprofiles');
                            }

                            vc.fetchAPI('sginstanceprofiles');
                            router.push('/' + profile.metadata.namespace + '/sginstanceprofiles');
            
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error','sginstanceprofiles');
                        });

                    }
                }

            },

            parseContainers(containers) {

                let result = {};
                
                containers.forEach( (container) => {
                    if( container.name.length && (container.RAM.length || container.CPU.length || container.hugePages1Gi.length || container.hugePages2Mi.length) ) {
                        result[container.name] = {
                            ...( container.CPU.length && {
                                "cpu": (container.CPUUnit !== 'CPU') ? (container.CPU + container.CPUUnit) : container.CPU
                            }),
                            ...( container.RAM.length && {
                                "memory": container.RAM+container.RAMUnit,
                            }),
                            ...( ( (container.hasOwnProperty('hugePages1Gi') && container.hugePages1Gi.length) || (container.hasOwnProperty('hugePages2Mi') && container.hugePages2Mi.length) ) && {
                                "hugePages": {
                                    ...( (container.hasOwnProperty('hugePages2Mi') && container.hugePages2Mi.length) && {
                                        "hugepages-2Mi": container.hugePages2Mi + container.hugePages2MiUnit
                                    }),
                                    ...( (container.hasOwnProperty('hugePages1Gi') && container.hugePages1Gi.length) && {
                                        "hugepages-1Gi": container.hugePages1Gi + container.hugePages1GiUnit
                                    }),
                                }
                            })
                        }
                    }
                })
                
                return ( Object.keys(result).length ? result : null )

            },

            unparseContainers(containers) {
                let result = [];

                if(Object.keys(containers).length) {
                
                    Object.keys(containers).forEach( (container) => {
                        result.push({
                            name: container,
                            CPU: containers[container].hasOwnProperty('cpu') ? containers[container].cpu.match(/\d+/g)[0] : '',
                            CPUUnit: ( ( containers[container].hasOwnProperty('cpu') && (containers[container].cpu.match(/[a-zA-Z]+/g) !== null) ) ? containers[container].cpu.match(/[a-zA-Z]+/g)[0] : 'CPU' ),
                            RAM: ( containers[container].hasOwnProperty('memory') ? containers[container].memory.match(/\d+/g)[0] : '' ),
                            RAMUnit: ( containers[container].hasOwnProperty('memory') ? containers[container].memory.match(/[a-zA-Z]+/g)[0] : 'Gi' ),
                            hugePages1Gi: ( this.hasProp(containers[container], 'hugePages.hugepages-1Gi') ? containers[container].hugePages['hugepages-1Gi'].match(/\d+/g)[0] : '' ),
                            hugePages1GiUnit: ( this.hasProp(containers[container], 'hugePages.hugepages-1Gi') ? containers[container].hugePages['hugepages-1Gi'].match(/[a-zA-Z]+/g)[0] : 'Gi' ),
                            hugePages2Mi: ( this.hasProp(containers[container], 'hugePages.hugepages-2Mi') ? containers[container].hugePages['hugepages-2Mi'].match(/\d+/g)[0] : '' ),
                            hugePages2MiUnit: ( this.hasProp(containers[container], 'hugePages.hugepages-2Mi') ? containers[container].hugePages['hugepages-2Mi'].match(/[a-zA-Z]+/g)[0] : 'Gi' )
                        })
                    })

                    return result

                } else {
                    return [ {
                            name: '',
                            CPU: '',
                            CPUUnit: 'CPU',
                            RAM: '',
                            RAMUnit: 'Gi',
                            hugePages1Gi: '',
                            hugePages1GiUnit: 'Gi',
                            hugePages2Mi: '',
                            hugePages2MiUnit: 'Gi',
                        } ]
                }
                
            }

        }

    }
</script>
