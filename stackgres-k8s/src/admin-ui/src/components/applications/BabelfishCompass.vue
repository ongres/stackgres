<template>
	<div id="babelfish-compass" v-if="loggedIn && isReady && !notFound">        
        <form
            id="babelfishCompass"
            class="form"
            :class="processingFile && 'loading'"
            @submit.prevent>

            <div class="header step">
                <h2>Babelfish Compass</h2>
                <p>Babelfish Compass is a compatibility assessment tool for Babelfish for PostgreSQL. With Babelfish Compass, users can quickly analyze T-SQL DDL/SQL scripts for compatibility with Babelfish.</p>
                <p>The assessment report output file provides a detailed summary of the supported and unsupported SQL features in Babelfish for the analyzed SQL Server script(s).</p>
            </div>

           
                <label for="reportName">
                    Report Name
                    <span class="req">*</span>
                </label>
                <input 
                    v-model="reportName"
                    class="reportName"
                    data-field="reportName"
                    required
                >
                <span class="helpTooltip" data-tooltip="The name used by the Babelfish Compass tool to generate the report"></span>
                
                
                <div class="repeater">
                    <div class="header">
                        <h3>
                            SQL files
                            <span class="req">*</span>
                            <span class="helpTooltip" data-tooltip="One or more SQL/DDL scripts to be analyzed"></span>
                        </h3>
                    </div>
                    <fieldset v-if="sqlFilesLength.length">
                        <template v-for="(item, index) in sqlFilesLength">
                            <div class="header">
                                File #{{ index+1 }}
                                <a class="addRow" @click="sqlFilesLength.splice(index, 1)">Delete</a>
                            </div>
                            <input 
                                type="file" 
                                name="sqlFiles"
                                :id="item"
                                :ref="item"
                                :required="(!index)"
                                @change="uploadFile" 
                            >
                        </template>
                    </fieldset>
                    <div class="fieldsetFooter" :class="!sqlFilesLength.length && 'topBorder'">
                        <a class="addRow" @click="addNewFile()">Add File</a>
                    </div>
                </div>
            <br/>
            <hr/>
            <button
                class="btn"
                type="submit"
                @click="sendFiles()"
                :disabled="processingFile"
                >
                SEND
            </button>
        </form>

        <div 
            v-if="Object.keys(result).length"
            class="compassReport"
            >
            <div class="header">
                <h2>Report Results</h2>
            </div>
            <br/>
            <p class="warningText">
                <strong>Notice:</strong> <br/>
                This report contains an assessment based on the resources you scanned with the
                Babelfish Compass tool. The information contained in this report, including whether
                or not a feature is 'supported' or 'not supported', is made available 'as is',
                and may be incomplete, incorrect, and subject to interpretation.
                You should not base decisions on the information in this report without independently
                validating it against the actual SQL/DDL code on which this report is based.
            </p>
           
            <table class="setup clusterConfig">
                <tbody>
                    <tr>
                        <td class="label">
                            Report Name
                        </td>
                        <td class="value" colspan="2">
                            {{ reportName }}
                        </td>
                    </tr>
                    <tr v-for="(value, key, index) in result.setup">
                        <td 
                            v-if="!index"
                            class="label" 
                            :rowspan="Object.keys(result.setup).length">
                            Report Setup
                        </td>
                        <td class="label">
                            {{ key }}
                        </td>
                        <td class="value">
                            {{ value }}
                        </td>
                    </tr>
                    <tr v-for="(value, key, index) in result.metrics">
                        <td 
                            v-if="!index"
                            class="label" 
                            :rowspan="Object.keys(result.metrics).length">
                            Report Metrics
                        </td>
                        <td class="label">
                            {{ key }}
                        </td>
                        <td class="value">
                            {{ value }}
                        </td>
                    </tr>
                    <template v-for="(fileName, index) in result.files">
                        <tr>
                            <td 
                                v-if="!index"
                                class="label" 
                                :rowspan="Object.keys(result.files).length">
                                Files Processed
                            </td>
                            <td class="value" colspan="2">
                                {{ fileName }}
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
           
            <pre 
                id="toc"
                class="monoFont"
                v-html="result.report"
            ></pre>
        </div>
	</div>
</template>

<script>
    import sgApi from '../../api/sgApi'
	import { mixin } from '../mixins/mixin'

    export default {
        name: 'BabelfishCompass',

		mixins: [mixin],

		data: function() {

			return {
                reportName: '',
				sqlFiles: [],
                sqlFilesLength: ['file-0'],
                result: {},
                processingFile: false
			}
		},

        methods: {
            
            addNewFile() {
                this.sqlFilesLength.push('file-' + (this.sqlFilesLength.length + 1) );
                setTimeout(() => {
                    document.getElementById('file-' + this.sqlFilesLength.length).click();
                }, 100);
            },

            uploadFile(e) {
                this.sqlFiles.push(e.target.files[0]);
            },
            
            clearFile(index) {
                this.$refs['file-' + index][0].value = null;
                this.sqlFiles.splice(index, 1);
            },

            sendFiles() {
                let vc = this;

                if(!vc.checkRequired()) {
                    return;
                }

                vc.processingFile = true;
                vc.result = {};

                const formData = new FormData();
                formData.append('reportName', vc.reportName)
                vc.sqlFiles.forEach((file) => {
                    formData.append('sqlFiles', file)
                })

                sgApi
                .createCustomResource('/applications/com.ongres/babelfish-compass', formData)
                .then( function(response){
                    let parser = new DOMParser();
                    let htmlDoc = parser.parseFromString(response.data.report, 'text/html');
                    vc.result = vc.parseCompassReport(htmlDoc.getElementsByTagName('pre')[0].innerHTML);
                    vc.cleanUp();
                }).catch(function(err) {
                    vc.notify('<p>There was an error when trying to process your file. Please try again!</p>', 'error');
                    console.log(err);
                    vc.cleanUp();
                });
            },

            parseCompassReport(content) {
                let result = {};
               
                // Truncate first part
                content = content.substring(
                    content.indexOf('BabelfishFeatures.cfg file'), 
                    content.length
                );
                
                // Get compass setup
                result['setup'] = {};
                let setup = content.substring(
                    0, 
                    content.indexOf('================================================================================')
                ).split('\n');

                for(let i = 0; i < setup.length; i+=2) {
                    let prop = setup[i].split(' : ');

                    if(prop[0].length && !prop[1].includes('/home/babelfish/')) {
                        result.setup[prop[0]] = prop[1];
                    }
                }

                // Get report
                result['report'] = content.substring(
                    content.indexOf('--------------------------------------------------------------------------------'),
                    content.indexOf('--- Run Metrics')
                );

                // Get metrics
                result['metrics'] = {};
                let metrics = content.substring(
                    content.indexOf('--- Run Metrics ----------------------------------------------------------------') + 80,
                    content.lastIndexOf('================================================================================')
                ).split('\n');

                for(let i = 0; i < metrics.length; i+=2) {
                    let prop = metrics[i].split(' : ');
                    
                    if(prop[0].length && !prop[1].includes('/home/babelfish/')) {
                        result.metrics[prop[0]] = prop[1];
                    }
                }
                
                result['files'] = [];
                this.sqlFiles.forEach( (file) => {
                    result['files'].push(file.name)
                });
                
                return result
            },

            cleanUp() {
                const vc = this;

                vc.sqlFiles.splice(0, vc.sqlFiles.length);
                vc.sqlFilesLength.splice(0, vc.sqlFilesLength.length);
                vc.name = '';
                vc.processingFile = false;
            }
        }
	}
</script>

<style scoped>
    #babelfishCompass {
        width: calc(35% - 40px);
        margin-right: 40px;
    }

    #babelfishCompass.loading > *{
        opacity: .4;
    }

    #babelfishCompass.loading:after {
        display: block;
        content: " ";
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: url('/assets/img/loader.gif') center center no-repeat;
        background-size: 40px;
    }


    #babelfishCompass h2 {
        margin-bottom: 10px;
    }

    input.reportName + .helpTooltip {
        transform: translate(20px, -50px);
    }

    input[type="file"] {
        padding: 7px 10px;
        margin-bottom: 20px;
        margin-top: -5px;
    }

    input[type="file"]:last-of-type {
        margin-bottom: 0;
    }

    .repeater > .header {
        margin-bottom: 10px;
    }

    .compassReport {
        display: inline-block;
        width: 65%;
        padding: 40px;
        border-left: 1px solid var(--borderColor);
        margin-top: -15px;
        overflow-y: auto;
        overflow-x: hidden;
        height: calc(100vh - 220px);
        scroll-behavior: smooth;
    }

    .compassReport pre {
        width: 100%;
        overflow-x: auto;
        border: 1px solid var(--borderColor);
        padding: 20px;
        margin-top: 30px;
        border-radius: 5px;
    }
</style>

<style>

    #toc [target="_blank"] {
        pointer-events: none;
    }

</style>