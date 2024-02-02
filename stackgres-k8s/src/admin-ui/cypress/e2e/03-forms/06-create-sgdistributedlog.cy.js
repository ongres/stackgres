describe('Create SGDistributedLog', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;
    let profileName;
    let pgConfigName;

    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6)
        profileName = 'profile-' + resourceName;
        pgConfigName = 'pgconfig-' + resourceName;

        // Create dependencies
        cy.createCRD('sginstanceprofiles', {
            metadata: {
                namespace: namespace,
                name: profileName,
            },
            spec: {
                cpu: "500m",
                memory: "512Mi"
            }
        });

        cy.createCRD('sgpgconfigs', {
            metadata: {
                name: pgConfigName,
                namespace: namespace
            },
            spec: {
                postgresVersion: "12",
                'postgresql.conf':""
            }
        })
    });

    beforeEach( () => {
      cy.gc()
      cy.login()
      cy.setCookie('sgReload', '0')
      cy.setCookie('sgTimezone', 'utc')
      cy.visit(namespace + '/sgdistributedlogs/new')
    });

    after( () => {
        cy.login()

        cy.deleteCRD('sgdistributedlogs', {
            metadata: {
                name: 'basic-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgdistributedlogs', {
          metadata: {
              name: 'advanced-' + resourceName,
              namespace: namespace
          }
        });

        cy.deleteCRD('sgdistributedlogs', {
          metadata: {
              name: 'resources-' + resourceName,
              namespace: namespace
          }
        });

        cy.deleteCRD('sgpgconfigs', {
            metadata: {
                name: pgConfigName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgpgconfigs', {
            metadata: {
                name: profileName,
                namespace: namespace
            }
        });

    });

    it('Create SGDistributedLog form should be visible', () => {
        cy.get('form#createLogsServer')
            .should('be.visible')
    });  

    it('Creating a basic SGDistributedLog should be possible', () => {
        // Test SGDistributedLog Name
        cy.get('[data-field="metadata.name"]')
            .type('basic-' + resourceName)

        // Test Submit form
        cy.get('form#createLogsServer button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Logs server "basic-' + resourceName + '" created successfully')
            })
        
            // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdistributedlogs')
    });

    it('Creating an advanced SGDistributedLog should be possible', () => {

        // Enable advanced options
        cy.get('form#createLogsServer input#advancedMode')
            .click()
        
        // Test SGDistributedLog Name
        cy.get('input[data-field="metadata.name"]')
            .type('advanced-' + resourceName)

        // Test Profile
        cy.get('select[data-field="spec.profile"]')
            .select('testing')
        
        // Test Volume Size
        cy.get('input[data-field="spec.persistentVolume.size"]')
            .clear()
            .type('2')

        // Test Postgres Services types
        cy.get('form#createLogsServer li[data-step="services"]')
            .click()

        cy.get('select[data-field="spec.postgresServices.primary.type"]')
            .select('LoadBalancer')
        
        cy.get('input[data-field="spec.postgresServices.primary.loadBalancerIP"]')
            .clear()
            .type('1.2.3.4')
        
        cy.get('select[data-field="spec.postgresServices.replicas.type"]')
            .select('NodePort')
        
        cy.get('input[data-field="spec.postgresServices.replicas.loadBalancerIP"]')
            .clear()
            .type('1.2.3.4')

        // Test Metadata
        cy.get('form#createLogsServer li[data-step="metadata"]')
            .click()
        
        cy.get('input[data-field="spec.metadata.annotations.allResources[0].annotation"]')
            .type('annotation')
        cy.get('input[data-field="spec.metadata.annotations.allResources[0].value"]')
            .type('value')

        cy.get('input[data-field="spec.metadata.annotations.pods[0].annotation"]')
            .type('annotation')
        cy.get('input[data-field="spec.metadata.annotations.pods[0].value"]')
            .type('value')

        cy.get('input[data-field="spec.metadata.annotations.services[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.metadata.annotations.services[0].value"]')
            .type('value')
        
        // Tests Node Selectors
        cy.get('form#createLogsServer li[data-step="scheduling"]')
            .click()

        cy.get('input[data-field="spec.scheduling.nodeSelector[0].label"]')
            .type('key')
        cy.get('input[data-field="spec.scheduling.nodeSelector[0].value"]')
            .type('value')

        // Tests Node Tolerations
        cy.get('input[data-field="spec.scheduling.tolerations[0].key"]')
            .type('key')
        cy.get('input[data-field="spec.scheduling.tolerations[0].value"]')
            .type('value')
        cy.get('select[data-field="spec.scheduling.tolerations[0].effect"]')
            .select('NoSchedule')
        
        // Test Non Production Options
        cy.get('form#createLogsServer li[data-step="non-production"]')
            .click()

        cy.get('select[data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity"]')
            .select('Disable')
        
        cy.get('select[data-field="spec.nonProductionOptions.disablePatroniResourceRequirements"]')
            .select('Disable')
        
        cy.get('select[data-field="spec.nonProductionOptions.disableClusterResourceRequirements"]')
            .select('Disable')

        // Test Submit form
        cy.get('form#createLogsServer button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Logs server "advanced-' + resourceName + '" created successfully')
            })

        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdistributedlogs')
    }); 

    it('Creating a SGDistributedLog with a SGInstanceProfile and a SGPostgresConfig should be possible', () => {
        // Test SGDistributedLog Name
        cy.get('[data-field="metadata.name"]')
            .type('resources-' + resourceName)
        
        // Test SGInstanceProfile
        cy.get('select[data-field="spec.sgInstanceProfile"]') 
            .select(profileName) 

        // Test SGPostgresConfig
        cy.get('select[data-field="spec.configurations.sgPostgresConfig"]') 
            .select(pgConfigName) 

        // Test Submit form
        cy.get('form#createLogsServer button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Logs server "resources-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdistributedlogs')
    });
  })