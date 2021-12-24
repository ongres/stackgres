describe('Create SGBackupConfig', () => {
   
    const host = Cypress.env('host')
    const resourcename = Cypress.env('resourcename')
    
    beforeEach( () => {
        cy.login()
        cy.visit(host + '/default/sgbackupconfigs/new')
    })
    
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
            .type('s3-' + resourcename)
        
        
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
        cy.get('[data-field="spec.storage.type"]')
            .select('s3')

        cy.get('form#createBackupConfig input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.storage.s3.bucket"]')
            .type(resourcename)

        cy.get('[data-field="spec.storage.s3.path"]')
            .type('//' + resourcename)

        cy.get('[data-field="spec.storage.s3.region"]')
            .type(resourcename)

        cy.get('[data-field="spec.storage.s3.awsCredentials.secretKeySelectors.accessKeyId"]')
            .type(resourcename)

        cy.get('[data-field="spec.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey"]')
            .type(resourcename)
        
        
        // Submit
        cy.get('form#createBackupConfig button[type="submit"]')
            .click()

        //Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup configuration "s3-' + resourcename + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgbackupconfigs')
        
    });


    // Amazon S3 Compatible
    it('Creating a Amazon S3 Compatible SGBackupConfig should be possible', () => {
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('s3compatible-' + resourcename)
        
        
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
        cy.get('[data-field="spec.storage.type"]')
            .select('s3Compatible')

        cy.get('form#createBackupConfig input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.storage.s3Compatible.bucket"]')
            .type(resourcename)

        cy.get('[data-field="spec.storage.s3Compatible.path"]')
            .type('//' + resourcename)

        cy.get('[data-field="spec.storage.s3Compatible.endpoint"]')
            .type('https://' + resourcename)

        cy.get('[data-field="spec.storage.s3Compatible.region"]')
            .type(resourcename)

        cy.get('[data-field="spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId"]')
            .type(resourcename)

        cy.get('[data-field="spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey"]')
            .type(resourcename)

        cy.get('#enablePathStyleAddressing')
            .click()
        
        
        // Submit
        cy.get('form#createBackupConfig button[type="submit"]')
            .click()

        //Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup configuration "s3compatible-' + resourcename + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgbackupconfigs')
    });


    // Google Cloud Storage
    it('Creating a GCS SGBackupConfig should be possible', () => {
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('gcs-' + resourcename)
        
        
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
        cy.get('[data-field="spec.storage.type"]')
            .select('gcs')
        
        cy.get('form#createBackupConfig input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.storage.gcs.bucket"]')
            .type(resourcename)

        cy.get('[data-field="spec.storage.gcs.path"]')
            .type('//' + resourcename)

        cy.get('#fetchGCSCredentials')
            .click()
       
        
        // Submit
        cy.get('form#createBackupConfig button[type="submit"]')
            .click()

        //Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup configuration "gcs-' + resourcename + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgbackupconfigs')
    });


    // Azure Blob
    it('Creating a Azure Blob SGBackupConfig should be possible', () => {
        // Name
        cy.get('[data-field="metadata.name"]')
            .type('azure-' + resourcename)
        
        
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
        cy.get('[data-field="spec.storage.type"]')
            .select('azureBlob')
        
        cy.get('form#createBackupConfig input#advancedModeStorage')
            .click()

        cy.get('[data-field="spec.storage.azureBlob.bucket"]')
            .type(resourcename)

        cy.get('[data-field="spec.storage.azureBlob.path"]')
            .type('//' + resourcename)

        cy.get('[data-field="spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount"]')
            .type(resourcename)

        cy.get('[data-field="spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey"]')
            .type(resourcename)
       
        
        // Submit
        cy.get('form#createBackupConfig button[type="submit"]')
            .click()

        //Notification appears
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup configuration "azure-' + resourcename + '" created successfully')
        })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgbackupconfigs')
    });


})
    