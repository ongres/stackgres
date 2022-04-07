describe('Create SGBackupConfig', () => {

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;
    
    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6)
    });

    beforeEach( () => {
        Cypress.Cookies.preserveOnce('sgToken')
        cy.visit(namespace + '/sgbackupconfigs/new')
    });

    after( () => {
        cy.deleteCRD('sgbackupconfigs', {
            metadata: {
                name: 's3-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgbackupconfigs', {
            metadata: {
                name: 's3compatible-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgbackupconfigs', {
            metadata: {
                name: 'gcs-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgbackupconfigs', {
            metadata: {
                name: 'azure-' + resourceName,
                namespace: namespace
            }
        });
    });
    
    it('Create SGBackupConfig form should be visible', () => {
        cy.get('form#createBackupConfig')
            .should('be.visible')
    });
        
    // Amazon S3
    it('Creating a Amazon S3 SGBackupConfig should be possible', () => {
        //Enable Advances Options
        cy.get('form#createBackupConfig input#advancedMode')
            .click()

        
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('s3-' + resourceName)
        
        
        // Backup Schedule
        cy.get('#backupConfigFullScheduleMin')
            .clear()
            .type('1')
        
        cy.get('#backupConfigFullScheduleHour')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOM')
            .clear()    
            .type('1')
        
        cy.get('#backupConfigFullScheduleMonth')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOW')
            .clear()    
            .type('1')


        // Base Backup Details
        cy.get('[data-field="spec.baseBackups.retention"]')
            .clear()    
            .type('3')
        
        cy.get('[data-field="spec.baseBackups.compression"]')
            .select('LZ4')


        //Performance Details
        cy.get('[data-field="spec.baseBackups.performance.maxNetworkBandwitdh"]')
            .type('1024')

        cy.get('[data-field="spec.baseBackups.performance.maxDiskBandwitdh"]')
            .type('1024')
        
        cy.get('[data-field="spec.baseBackups.performance.uploadDiskConcurrency"]')
            .clear()    
            .type('2')
        

        //Storage Details
        cy.get('[data-field="spec.storage.type.s3"]')
            .click()

        cy.get('form#createBackupConfig input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.storage.s3.bucket"]')
            .type(resourceName)

        cy.get('[data-field="spec.storage.s3.region"]')
            .type(resourceName)

        cy.get('[data-field="spec.storage.s3.awsCredentials.secretKeySelectors.accessKeyId"]')
            .type(resourceName)

        cy.get('[data-field="spec.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey"]')
            .type(resourceName)
        
        
        // Submit
        cy.get('form#createBackupConfig button[type="submit"]')
            .click()

        //Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup configuration "s3-' + resourceName + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgbackupconfigs')
        
    });


    // Amazon S3 Compatible
    it('Creating a Amazon S3 Compatible SGBackupConfig should be possible', () => {
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('s3compatible-' + resourceName)
        
        
        // Backup Schedule
        cy.get('#backupConfigFullScheduleMin')
            .clear()
            .type('1')
        
        cy.get('#backupConfigFullScheduleHour')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOM')
            .clear()    
            .type('1')
        
        cy.get('#backupConfigFullScheduleMonth')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOW')
            .clear()    
            .type('1')

        
        //Storage Details
        cy.get('[data-field="spec.storage.type.s3Compatible"]')
            .click()

        cy.get('form#createBackupConfig input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.storage.s3Compatible.bucket"]')
            .type(resourceName)

        cy.get('[data-field="spec.storage.s3Compatible.endpoint"]')
            .type('https://' + resourceName)

        cy.get('[data-field="spec.storage.s3Compatible.region"]')
            .type(resourceName)

        cy.get('[data-field="spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId"]')
            .type(resourceName)

        cy.get('[data-field="spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey"]')
            .type(resourceName)

        cy.get('#enablePathStyleAddressing')
            .click()
        
        
        // Submit
        cy.get('form#createBackupConfig button[type="submit"]')
            .click()

        //Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup configuration "s3compatible-' + resourceName + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgbackupconfigs')
    });


    // Google Cloud Storage
    it('Creating a GCS SGBackupConfig should be possible', () => {
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('gcs-' + resourceName)
        
        
        // Backup Schedule
        cy.get('#backupConfigFullScheduleMin')
            .clear()
            .type('1')
        
        cy.get('#backupConfigFullScheduleHour')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOM')
            .clear()    
            .type('1')
        
        cy.get('#backupConfigFullScheduleMonth')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOW')
            .clear()    
            .type('1')

        
        //Storage Details
        cy.get('[data-field="spec.storage.type.gcs"]')
            .click()
        
        cy.get('form#createBackupConfig input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.storage.gcs.bucket"]')
            .type(resourceName)

        cy.get('#fetchGCSCredentials')
            .click()
       
        
        // Submit
        cy.get('form#createBackupConfig button[type="submit"]')
            .click()

        //Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup configuration "gcs-' + resourceName + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgbackupconfigs')
    });


    // Azure Blob
    it('Creating a Azure Blob SGBackupConfig should be possible', () => {
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('azure-' + resourceName)
        
        
        // Backup Schedule
        cy.get('#backupConfigFullScheduleMin')
            .clear()
            .type('1')
        
        cy.get('#backupConfigFullScheduleHour')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOM')
            .clear()    
            .type('1')
        
        cy.get('#backupConfigFullScheduleMonth')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOW')
            .clear()    
            .type('1')

        
        //Storage Details
        cy.get('[data-field="spec.storage.type.azureBlob"]')
            .click()
        
        cy.get('form#createBackupConfig input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.storage.azureBlob.bucket"]')
            .type(resourceName)

        cy.get('[data-field="spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount"]')
            .type(resourceName)

        cy.get('[data-field="spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey"]')
            .type(resourceName)
       
        
        // Submit
        cy.get('form#createBackupConfig button[type="submit"]')
            .click()

        //Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup configuration "azure-' + resourceName + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgbackupconfigs')
    });
})
    