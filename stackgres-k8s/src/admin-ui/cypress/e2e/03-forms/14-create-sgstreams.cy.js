describe('Create SGStream', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6)

        // Create SGCluster dependency for spec.source.sgCluster.name
        cy.createCRD('sgclusters', {
            metadata: {
                name: 'source-sgcluster-' + resourceName, 
                namespace: namespace
            },
            spec: {
                instances: 1, 
                pods: {
                    persistentVolume: {
                        size: "128Mi"
                    }
                },
                nonProductionOptions: {
                    disableClusterPodAntiAffinity: true
                },
                postgres: {
                    version: "latest",
                    flavor: "vanilla"
                }
            }
        });

        // Create SGCluster dependency for spec.target.sgCluster.name
        cy.createCRD('sgclusters', {
            metadata: {
                name: 'target-sgcluster-' + resourceName, 
                namespace: namespace
            },
            spec: {
                instances: 1, 
                pods: {
                    persistentVolume: {
                        size: "128Mi"
                    }
                },
                nonProductionOptions: {
                    disableClusterPodAntiAffinity: true
                },
                postgres: {
                    version: "latest",
                    flavor: "vanilla"
                }
            }
        });

    });

    beforeEach( () => {
        cy.gc()
        cy.login()
        cy.setCookie('sgReload', '0')
        cy.setCookie('sgTimezone', 'utc')
        cy.visit(namespace + '/sgstreams/new')
        cy.get('form#createStream li[data-step="stream"]')
            .should('have.class', 'active')
    });

    after( () => {
        cy.login()

        cy.deleteCluster(namespace, 'source-sgcluster-' + resourceName);

        cy.deleteCluster(namespace, 'target-sgcluster-' + resourceName);

        cy.deleteCRD('sgstreams', {
            metadata: {
                name: 'sgcluster2sgcluster-stream-' + resourceName,
                namespace: namespace
            }
        });
    });

    it('Create SGStream form should be visible', () => {
        cy.get('form#createStream')
            .should('be.visible')
    });

    it('Creating an SGStream between two SGClusters with all specs set should be possible', () => {

        // Test Stream Information
        cy.get('[data-field="metadata.name"]')
            .type('sgcluster2sgcluster-stream-' + resourceName)

        cy.get('[data-field="spec.maxRetries"')
            .clear()
            .type('10')

        cy.get('input[data-field="spec.pods.persistentVolume.size"]')
            .clear()
            .type('512')

        cy.get('select[data-field="spec.pods.persistentVolume.size"]')
            .select('Mi')

        cy.get('[data-field="spec.pods.persistentVolume.storageClass"]')
            .select(1)

        // Test Source configuration
        cy.get('form#createStream li[data-step="source"]')
            .click()

        cy.get('[data-field="spec.source.type"]')
            .select('SGCluster')

        cy.get('[data-field="spec.source.sgCluster.name"]')
            .select('source-sgcluster-' + resourceName)

        cy.get('[data-field="spec.source.sgCluster.database"]')
            .clear()
            .type('postgres')

        cy.get('[data-field="spec.source.sgCluster.username.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.source.sgCluster.username.key"]')
            .clear()
            .type('key')

        cy.get('[data-field="spec.source.sgCluster.password.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.source.sgCluster.password.key"]')
            .clear()
            .type('key')

        cy.get('[data-field="add-includes"]')
            .click()

        cy.get('[data-field="spec.source.sgCluster.includes[0]"]')
            .clear()
            .type('include')

        cy.get('[data-field="add-excludes"]')
            .click()

        cy.get('[data-field="spec.source.sgCluster.excludes[0]"]')
            .clear()
            .type('exclude')

        // Test Target configuration
        cy.get('form#createStream li[data-step="target"]')
            .click()

        cy.get('[data-field="spec.target.type"]')
            .select('SGCluster')

        cy.get('[data-field="spec.target.sgCluster.name"]')
            .select('source-sgcluster-' + resourceName)

        cy.get('[data-field="spec.target.sgCluster.database"]')
            .clear()
            .type('postgres')

        cy.get('[data-field="spec.target.sgCluster.username.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.target.sgCluster.username.key"]')
            .clear()
            .type('key')

        cy.get('[data-field="spec.target.sgCluster.password.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.target.sgCluster.password.key"]')
            .clear()
            .type('key')

        cy.get('[data-field="spec.target.sgCluster.skipDdlImport"]')
            .click()

        cy.get('[data-field="spec.target.sgCluster.ddlImportRoleSkipFilter"]')
            .clear()
            .type('filter')

        // Test Target configuration
        cy.get('form#createStream li[data-step="pods"]')
            .click()

        cy.get('[data-field="add-claim"]')
            .click()
        cy.get('[data-field="crd.spec.pods.resources.claims[0].name"]')
            .type('claim')

        cy.get('[data-field="add-limit"]')
            .click()
        cy.get('[data-field="spec.pods.resources.limits[0].limit"]')
            .type('limit')
        cy.get('[data-field="spec.pods.resources.limits[0].value"]')
            .type('value')

        cy.get('[data-field="add-request"]')
            .click()
        cy.get('[data-field="spec.pods.resources.requests[0].request"]')
            .type('request')
        cy.get('[data-field="spec.pods.resources.requests[0].value"]')
            .type('value')

        cy.get('[data-field="spec.pods.resources.scheduling.priorityClassName"]')
            .clear()
            .type('class')

        cy.get('[data-field="add-node-selector"]')
            .click()
        cy.get('[data-field="spec.pods.resources.scheduling.nodeSelector[0].label"]')
            .type('label')
        cy.get('[data-field="spec.pods.resources.scheduling.nodeSelector[0].value"]')
            .type('value')

        cy.get('[data-field="add-toleration"]')
            .click()
        cy.get('[data-field="spec.pods.resources.scheduling.tolerations[0].key"]')
            .type('key')        
        cy.get('[data-field="spec.pods.resources.scheduling.tolerations[0].value"]')
            .type('value')        
        cy.get('[data-field="spec.pods.resources.scheduling.tolerations[0].effect"]')
            .select('NoExecute')        
        cy.get('[data-field="spec.pods.resources.scheduling.tolerations[0].tolerationSeconds"]')
            .type('10')

        cy.get('[data-field="add-required-affinity-term"]')
            .click()
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key"]')
            .type('key')
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator"]')
            .select('In')
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values"]')
            .type('value')

        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key"]')
            .type('key')
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator"]')
            .select('In')
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values"]')
            .type('value')

        cy.get('[data-field="add-preferred-affinity-term"]')
            .click()
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key"]')
            .type('key')
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator"]')
            .select('In')
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values"]')
            .type('value')
        
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key"]')
            .type('key')
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator"]')
            .select('In')
        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values"]')
            .type('value')

        cy.get('[data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight"]')
            .clear()
            .type('10')

        // Test Submit form
        cy.get('form#createStream button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Stream "sgcluster2sgcluster-stream-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgstreams')

    });

    it('Creating an SGStream from an SGCluster to a CloudEvent should be possible', () => {

        // Test Stream Name
        cy.get('[data-field="metadata.name"]')
            .type('sgcluster2cloudevent-stream-' + resourceName)

        // Test Source configuration
        cy.get('form#createStream li[data-step="source"]')
            .click()

        cy.get('[data-field="spec.source.type"]')
            .select('SGCluster')

        cy.get('[data-field="spec.source.sgCluster.name"]')
            .select('source-sgcluster-' + resourceName)

        cy.get('[data-field="spec.source.sgCluster.database"]')
            .clear()
            .type('postgres')

        cy.get('[data-field="spec.source.sgCluster.username.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.source.sgCluster.username.key"]')
            .clear()
            .type('key')

        cy.get('[data-field="spec.source.sgCluster.password.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.source.sgCluster.password.key"]')
            .clear()
            .type('key')

        cy.get('[data-field="add-includes"]')
            .click()

        cy.get('[data-field="spec.source.sgCluster.includes[0]"]')
            .clear()
            .type('include')

        cy.get('[data-field="add-excludes"]')
            .click()

        cy.get('[data-field="spec.source.sgCluster.excludes[0]"]')
            .clear()
            .type('exclude')

        // Test Target configuration
        cy.get('form#createStream li[data-step="target"]')
            .click()

        cy.get('[data-field="spec.target.type"]')
            .select('CloudEvent')

        cy.get('[data-field="spec.target.cloudEvent.binding"]')
            .select('http')
        
        cy.get('[data-field="spec.target.cloudEvent.format"]')
            .select('json')

        cy.get('[data-field="spec.target.cloudEvent.http.url"]')
            .type('https://cloud.event.url')

        cy.get('[data-field="spec.target.cloudEvent.http.connectTimeout"]')
            .type('P1DT1H1M1S')

        cy.get('[data-field="spec.target.cloudEvent.http.readTimeout"]')
            .type('P1DT1H1M1S')

        cy.get('[data-field="spec.target.cloudEvent.http.retryBackoffDelay"]')
            .type('10')

        cy.get('[data-field="spec.target.cloudEvent.http.retryLimit"]')
            .type('10')

        cy.get('[data-field="spec.target.cloudEvent.http.skipHostnameVerification"]')
            .click()

        cy.get('[data-field="add-http-header"]')
            .click()

        cy.get('[data-field="spec.target.cloudEvent.http.headers[0].header"]')
            .type('header')
        
        cy.get('[data-field="spec.target.cloudEvent.http.headers[0].value"]')
            .type('value')

        // Test Submit form
        cy.get('form#createStream button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Stream "sgcluster2cloudevent-stream-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgstreams')

    });

    it('Creating an SGStream from an SGCluster to a PgLambda should be possible', () => {

        // Test Stream Name
        cy.get('[data-field="metadata.name"]')
            .type('sgcluster2pglambda-stream-' + resourceName)

        // Test Source configuration
        cy.get('form#createStream li[data-step="source"]')
            .click()

        cy.get('[data-field="spec.source.type"]')
            .select('SGCluster')

        cy.get('[data-field="spec.source.sgCluster.name"]')
            .select('source-sgcluster-' + resourceName)

        // Test Target configuration
        cy.get('form#createStream li[data-step="target"]')
            .click()

        cy.get('[data-field="spec.target.type"]')
            .select('PgLambda')

        cy.get('[data-field="spec.target.pgLambda.scriptSource"]')
            .select('createNewScript')

        cy.get('[data-field="spec.target.pgLambda.script"]')
            .type('test script;')

        cy.get('[data-field="spec.target.pgLambda.knative.http.url"]')
            .type('https://pglambda.knative.url')

        cy.get('[data-field="spec.target.pgLambda.knative.http.connectTimeout"]')
            .type('P1DT1H1M1S')

        cy.get('[data-field="spec.target.pgLambda.knative.http.readTimeout"]')
            .type('P1DT1H1M1S')

        cy.get('[data-field="spec.target.pgLambda.knative.http.retryBackoffDelay"]')
            .type('10')

        cy.get('[data-field="spec.target.pgLambda.knative.http.retryLimit"]')
            .type('10')

        cy.get('[data-field="spec.target.pgLambda.knative.http.skipHostnameVerification"]')
            .click()

        cy.get('[data-field="add-http-header"]')
            .click()

        cy.get('[data-field="spec.target.pgLambda.knative.http.headers[0].header"]')
            .type('header')
        
        cy.get('[data-field="spec.target.pgLambda.knative.http.headers[0].value"]')
            .type('value')

        // Test Submit form
        cy.get('form#createStream button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Stream "sgcluster2pglambda-stream-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgstreams')

    });

    it('Creating an SGStream from a Postgres instance to an SGCluster should be possible', () => {

        // Test Stream Name
        cy.get('[data-field="metadata.name"]')
            .type('postgres2sgcluster-stream-' + resourceName)

        // Test Source configuration
        cy.get('form#createStream li[data-step="source"]')
            .click()

        cy.get('[data-field="spec.source.type"]')
            .select('Postgres')

        cy.get('[data-field="spec.source.postgres.host"]')
            .clear()
            .type('http://localhost')

        cy.get('[data-field="spec.source.postgres.port"]')
            .clear()
            .type('5432')

        cy.get('[data-field="spec.source.postgres.database"]')
            .clear()
            .type('postgres')

        cy.get('[data-field="spec.source.postgres.username.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.source.postgres.username.key"]')
            .clear()
            .type('key')

        cy.get('[data-field="spec.source.postgres.password.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.source.postgres.password.key"]')
            .clear()
            .type('key')

        // Test Target configuration
        cy.get('form#createStream li[data-step="target"]')
            .click()

        cy.get('[data-field="spec.target.type"]')
            .select('SGCluster')

        cy.get('[data-field="spec.target.sgCluster.name"]')
            .select('source-sgcluster-' + resourceName)

        cy.get('[data-field="spec.target.sgCluster.database"]')
            .clear()
            .type('postgres')

        cy.get('[data-field="spec.target.sgCluster.username.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.target.sgCluster.username.key"]')
            .clear()
            .type('key')

        cy.get('[data-field="spec.target.sgCluster.password.name"]')
            .clear()
            .type('name')
        
        cy.get('[data-field="spec.target.sgCluster.password.key"]')
            .clear()
            .type('key')

        cy.get('[data-field="spec.target.sgCluster.skipDdlImport"]')
            .click()

        cy.get('[data-field="spec.target.sgCluster.ddlImportRoleSkipFilter"]')
            .clear()
            .type('filter')

        // Test Submit form
        cy.get('form#createStream button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Stream "postgres2sgcluster-stream-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgstreams')

    });

    it('Creating an SGStream from a Postgres instance to a CloudEvent should be possible', () => {

        // Test Stream Name
        cy.get('[data-field="metadata.name"]')
            .type('postgres2cloudevent-stream-' + resourceName)

        // Test Source configuration
        cy.get('form#createStream li[data-step="source"]')
            .click()

        cy.get('[data-field="spec.source.type"]')
            .select('Postgres')

        cy.get('[data-field="spec.source.postgres.host"]')
            .type('http://localhost')

        // Test Target configuration
        cy.get('form#createStream li[data-step="target"]')
            .click()

        cy.get('[data-field="spec.target.type"]')
            .select('CloudEvent')

        cy.get('[data-field="spec.target.cloudEvent.binding"]')
            .select('http')
        
        cy.get('[data-field="spec.target.cloudEvent.format"]')
            .select('json')

        cy.get('[data-field="spec.target.cloudEvent.http.url"]')
            .type('https://cloud.event.url')

        cy.get('[data-field="spec.target.cloudEvent.http.connectTimeout"]')
            .type('P1DT1H1M1S')

        cy.get('[data-field="spec.target.cloudEvent.http.readTimeout"]')
            .type('P1DT1H1M1S')

        cy.get('[data-field="spec.target.cloudEvent.http.retryBackoffDelay"]')
            .type('10')

        cy.get('[data-field="spec.target.cloudEvent.http.retryLimit"]')
            .type('10')

        cy.get('[data-field="spec.target.cloudEvent.http.skipHostnameVerification"]')
            .click()

        cy.get('[data-field="add-http-header"]')
            .click()

        cy.get('[data-field="spec.target.cloudEvent.http.headers[0].header"]')
            .type('header')
        
        cy.get('[data-field="spec.target.cloudEvent.http.headers[0].value"]')
            .type('value')

        // Test Submit form
        cy.get('form#createStream button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Stream "postgres2cloudevent-stream-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgstreams')

    });

    it('Creating an SGStream from a Postgres instance to a PgLambda should be possible', () => {

        // Test Stream Name
        cy.get('[data-field="metadata.name"]')
            .type('postgres2pglambda-stream-' + resourceName)

        // Test Source configuration
        cy.get('form#createStream li[data-step="source"]')
            .click()

        cy.get('[data-field="spec.source.type"]')
            .select('Postgres')

        cy.get('[data-field="spec.source.postgres.host"]')
            .type('http://localhost')

        // Test Target configuration
        cy.get('form#createStream li[data-step="target"]')
            .click()

        cy.get('[data-field="spec.target.type"]')
            .select('PgLambda')

        cy.get('[data-field="spec.target.pgLambda.scriptSource"]')
            .select('createNewScript')

        cy.get('[data-field="spec.target.pgLambda.script"]')
            .type('test script;')

        cy.get('[data-field="spec.target.pgLambda.knative.http.url"]')
            .type('https://pglambda.knative.url')

        cy.get('[data-field="spec.target.pgLambda.knative.http.connectTimeout"]')
            .type('P1DT1H1M1S')

        cy.get('[data-field="spec.target.pgLambda.knative.http.readTimeout"]')
            .type('P1DT1H1M1S')

        cy.get('[data-field="spec.target.pgLambda.knative.http.retryBackoffDelay"]')
            .type('10')

        cy.get('[data-field="spec.target.pgLambda.knative.http.retryLimit"]')
            .type('10')

        cy.get('[data-field="spec.target.pgLambda.knative.http.skipHostnameVerification"]')
            .click()

        cy.get('[data-field="add-http-header"]')
            .click()

        cy.get('[data-field="spec.target.pgLambda.knative.http.headers[0].header"]')
            .type('header')
        
        cy.get('[data-field="spec.target.pgLambda.knative.http.headers[0].value"]')
            .type('value')

        // Test Submit form
        cy.get('form#createStream button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Stream "postgres2pglambda-stream-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgstreams')

    });

});