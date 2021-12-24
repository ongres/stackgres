describe('Create SGDbOp', () => {

    const host = Cypress.env('host')
    const resourcename = Cypress.env('resourcename')

    beforeEach( () => {
        cy.login()
        cy.visit(host + '/default/sgdbops/new')
    })

    it('Create SGDbOps form should be visible', () => {
        cy.get('form#createDbops')
            .should('be.visible')
    });  

    it('Creating a Benchmark SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('benchmark-' + resourcename)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select('advanced-' + resourcename)

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')

        // Test timeout
        cy.get('select[data-field="spec.timeout.days"]')
         .select('10')
        cy.get('select[data-field="spec.timeout.hours"]')
         .select('20')
        cy.get('select[data-field="spec.timeout.minutes"]')
         .select('30')
        cy.get('select[data-field="spec.timeout.seconds"]')
         .select('40')

        // Test max retries
        cy.get('select[data-field="spec.maxRetries"]')
         .select('10')

        // Test Benchmark specs
        cy.get('select[data-field="spec.op"]')
         .select('benchmark')

        cy.get('select[data-field="spec.benchmark.connectionType"]')
            .select('replicas-service')

        cy.get('input[data-field="spec.benchmark.pgbench.databaseSize"]')
            .type('100')
        cy.get('select[data-field="spec.benchmark.pgbench.databaseSize"]')
            .select('Mi')

        cy.get('label[data-field="spec.benchmark.pgbench.usePreparedStatements"]')
            .click()

        cy.get('input[data-field="spec.benchmark.pgbench.concurrentClients"]')
            .clear()
            .type('10')
        
        cy.get('input[data-field="spec.benchmark.pgbench.threads"]')
            .clear()
            .type('10')

        cy.get('select[data-field="spec.benchmark.pgbench.duration.days"]')
            .select('10')
        cy.get('select[data-field="spec.benchmark.pgbench.duration.hours"]')
            .select('20')
        cy.get('select[data-field="spec.benchmark.pgbench.duration.minutes"]')
            .select('30')
        cy.get('select[data-field="spec.benchmark.pgbench.duration.seconds"]')
            .select('40')

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "benchmark-' + resourcename + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgdbops')
    }); 

    it('Creating a Vacuum SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('vacuum-' + resourcename)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select('advanced-' + resourcename)

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')

        // Test Vaccum specs
        cy.get('select[data-field="spec.op"]')
         .select('vacuum')

        cy.get('label[data-field="spec.vacuum.full"]')
            .click()
        cy.get('label[data-field="spec.vacuum.freeze"]')
            .click()
        cy.get('label[data-field="spec.vacuum.analyze"]')
            .click()
        cy.get('label[data-field="spec.vacuum.disablePageSkipping"]')
            .click()
        
        // Test per-database Vacuum specs
        cy.get('label[data-field="spec.vacuum.databases"]')
            .click()

        cy.get('input[data-field="spec.vacuum.databases.name"]')
            .type(resourcename)

        cy.get('select[data-field="spec.vacuum.databases.full"]')
            .select('false')
        cy.get('select[data-field="spec.vacuum.databases.freeze"]')
            .select('false')
        cy.get('select[data-field="spec.vacuum.databases.analyze"]')
            .select('true')
        cy.get('select[data-field="spec.vacuum.databases.disablePageSkipping"]')
            .select('true')

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "vacuum-' + resourcename + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgdbops')
    });

    /* 
    // To-Do: once backend dependencies have been set
    it('Creating a Repack SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('repack-' + resourcename)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select('advanced-' + resourcename)

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')

        // Test Repack specs
        cy.get('select[data-field="spec.op"]')
         .select('repack')

        cy.get('label[data-field="spec.repack.noOrder"]')
            .click()
        cy.get('label[data-field="spec.repack.noKillBackend"]')
            .click()
        cy.get('label[data-field="spec.repack.noAnalyze"]')
            .click()
        cy.get('label[data-field="spec.repack.excludeExtension"]')
            .click()

        cy.get('select[data-field="spec.repack.waitTimeout.days"]')
            .select('10')
        cy.get('select[data-field="spec.repack.waitTimeout.hours"]')
            .select('20')
        cy.get('select[data-field="spec.repack.waitTimeout.minutes"]')
            .select('30')
        cy.get('select[data-field="spec.repack.waitTimeout.seconds"]')
            .select('40')
        
        // Test per-database Repack specs
        cy.get('label[data-field="spec.repack.databases"]')
            .click()

        cy.get('input[data-field="spec.repack.databases.name"]')
            .type(resourcename)

        cy.get('select[data-field="spec.repack.databases.noOrder"]')
            .select('false')
        cy.get('select[data-field="spec.repack.databases.noKillBackend"]')
            .select('false')
        cy.get('select[data-field="spec.repack.databases.noAnalyze"]')
            .select('false')
        cy.get('select[data-field="spec.repack.databases.excludeExtension"]')
            .select('false')
        
        cy.get('select[data-field="spec.repack.databases.waitTimeout"]')
            .select('false')
        cy.get('select[data-field="spec.repack.databases.waitTimeout.days"]')
            .select('10')
        cy.get('select[data-field="spec.repack.databases.waitTimeout.hours"]')
            .select('20')
        cy.get('select[data-field="spec.repack.databases.waitTimeout.minutes"]')
            .select('30')
        cy.get('select[data-field="spec.repack.databases.waitTimeout.seconds"]')
            .select('40')

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "repack-' + resourcename + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgdbops')
    }); */

    it('Creating a Security Upgrade SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('sec-upg-' + resourcename)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select('advanced-' + resourcename)

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')

        // Test Security Upgrade specs
        cy.get('select[data-field="spec.op"]')
            .select('securityUpgrade')

        cy.get('select[data-field="spec.securityUpgrade.method"]')
            .select('ReducedImpact')

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "sec-upg-' + resourcename + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgdbops')
    });

    /*
    // To-Do: once backend dependencies have been set
    it('Creating a Minor Version Upgrade SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('minor-upg-' + resourcename)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select('advanced-' + resourcename)

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')

        // Test Minor Version Upgrade specs
        cy.get('select[data-field="spec.op"]')
            .select('minorVersionUpgrade')

        cy.get('select[data-field="spec.minorVersionUpgrade.method"]')
            .select('ReducedImpact')

        cy.get('select[data-field="spec.minorVersionUpgrade.postgresVersion"]')
            .select(1)

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "minor-upg-' + resourcename + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgdbops')
    }); */


    /*
    // To-Do: once backend dependencies have been set
    it('Creating a Major Version Upgrade SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('major-upg-' + resourcename)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select('advanced-' + resourcename)

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')

        // Test Minor Version Upgrade specs
        cy.get('select[data-field="spec.op"]')
            .select('majorVersionUpgrade')

        cy.get('label[data-field="spec.majorVersionUpgrade.link"]')
            .click()
        
        cy.get('label[data-field="spec.majorVersionUpgrade.check"]')
            .click()

        cy.get('select[data-field="spec.majorVersionUpgrade.postgresVersion"]')
            .select(1)

        cy.get('select[data-field="spec.majorVersionUpgrade.sgPostgresConfig"]')
            .select(1)

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "major-upg-' + resourcename + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgdbops')
    }); */


    it('Creating a Restart SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('restart-' + resourcename)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select('advanced-' + resourcename)

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')

        // Test Security Upgrade specs
        cy.get('select[data-field="spec.op"]')
            .select('restart')

        cy.get('select[data-field="spec.restart.method"]')
            .select('ReducedImpact')

        cy.get('label[data-field="spec.restart.onlyPendingRestart"]')
            .click()

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "restart-' + resourcename + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgdbops')
    });

  })