describe('Create SGObjectStorage', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;
    
    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6)
    });

    beforeEach( () => {
      cy.gc()
      cy.login()
      cy.setCookie('sgReload', '0')
      cy.setCookie('sgTimezone', 'utc')
      cy.visit(namespace + '/sgobjectstorages/new')
    });

    after( () => {
        cy.login()

        cy.deleteCRD('sgobjectstorages', {
            metadata: {
                name: 's3-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgobjectstorages', {
            metadata: {
                name: 's3compatible-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgobjectstorages', {
            metadata: {
                name: 'gcs-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgobjectstorages', {
            metadata: {
                name: 'azure-' + resourceName,
                namespace: namespace
            }
        });
    });
    
    it('Create SGObjectStorage form should be visible', () => {
        cy.get('form#createObjectStorage')
            .should('be.visible')
    });
        
    // Amazon S3
    it('Creating an Amazon S3 SGObjectStorage should be possible', () => {
        
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('s3-' + resourceName)

        // Storage Details
        cy.get('[for="s3"]')
            .click()

        cy.get('form#createObjectStorage input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.s3.bucket"]')
            .type(resourceName)

        cy.get('[data-field="spec.s3.region"]')
            .type(resourceName)

        cy.get('[data-field="spec.s3.awsCredentials.secretKeySelectors.accessKeyId"]')
            .type(resourceName)

        cy.get('[data-field="spec.s3.awsCredentials.secretKeySelectors.secretAccessKey"]')
            .type(resourceName)
        
        // Submit
        cy.get('form#createObjectStorage button[type="submit"]')
            .click()

        // Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Object storage configuration "s3-' + resourceName + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgobjectstorages')
        
    });


    // Amazon S3 Compatible
    it('Creating an Amazon S3 Compatible SGObjectStorage should be possible', () => {

        // Name
        cy.get('[data-field="metadata.name"]')
            .type('s3compatible-' + resourceName)
        
        // Storage Details
        cy.get('[for="s3Compatible"]')
            .click()

        cy.get('form#createObjectStorage input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.s3Compatible.bucket"]')
            .type(resourceName)

        cy.get('[data-field="spec.s3Compatible.endpoint"]')
            .type('https://' + resourceName)

        cy.get('[data-field="spec.s3Compatible.region"]')
            .type(resourceName)

        cy.get('[data-field="spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId"]')
            .type(resourceName)

        cy.get('[data-field="spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey"]')
            .type(resourceName)

        cy.get('#enablePathStyleAddressing')
            .click()
        
        // Submit
        cy.get('form#createObjectStorage button[type="submit"]')
            .click()

        // Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Object storage configuration "s3compatible-' + resourceName + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgobjectstorages')
    });


    // Google Cloud Storage
    it('Creating a GCS SGObjectStorage should be possible', () => {
        
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('gcs-' + resourceName) 
        
        // Storage Details
        cy.get('[for="gcs"]')
            .click()
        
        cy.get('[data-field="spec.gcs.bucket"]')
            .type(resourceName)

        cy.get('#fetchGCSCredentials')
            .click()
        
        // Submit
        cy.get('form#createObjectStorage button[type="submit"]')
            .click()

        // Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Object storage configuration "gcs-' + resourceName + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgobjectstorages')
    });


    // Azure Blob
    it('Creating an Azure Blob SGObjectStorage should be possible', () => {
        
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('azure-' + resourceName)
        
        // Storage Details
        cy.get('[for="azureBlob"]')
            .click()
        
        cy.get('[data-field="spec.azureBlob.bucket"]')
            .type(resourceName)

        cy.get('[data-field="spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount"]')
            .type(resourceName)

        cy.get('[data-field="spec.azureBlob.azureCredentials.secretKeySelectors.accessKey"]')
            .type(resourceName)
        
        // Submit
        cy.get('form#createObjectStorage button[type="submit"]')
            .click()

        // Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Object storage configuration "azure-' + resourceName + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgobjectstorages')
    });

    it('notValid class should be added if no Type is selected and removed once Type is selected', () => {
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('not-valid-' + resourceName)
        
        // Submit
        cy.get('form#createObjectStorage button[type="submit"]')
            .click()

        // Error notification should appear
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Please fill every mandatory field in the form')
        })

        // notValid class should be added to Type boxes
        cy.get('.optionBoxes label')
            .should('have.class', 'notValid')

        // notValid class should be removed when Type is selected
        cy.get('[for="azureBlob"]')
            .click()

        cy.get('.optionBoxes label')
            .should('not.have.class', 'notValid')
    });
})
    