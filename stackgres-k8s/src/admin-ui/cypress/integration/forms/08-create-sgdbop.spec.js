describe('Create SGDbOp', () => {

    const namespace = Cypress.env('namespace')
    let resourceName;
    let clusterName;

    before( () => {
        cy.login()

        generateRandomString = () => Cypress._.random(0, 1e6)

        resourceName = generateRandomString()
        clusterName = 'cluster-' + resourceName;
        pgConfigName = 'pgconfig-' + resourceName;

        cy.createCRD('sgclusters', {
            metadata: {
                name: clusterName,
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
                    version: "13.3",
                    extensions: [{
                        name: "pg_repack",
                        version: "1.4.7",
                        publisher: "com.ongres",
                        repository: "https://extensions.stackgres.io/postgres/repository"
                    }],
                    flavor: "vanilla"
                }
            }  
        });

        cy.createCRD('sgpgconfigs', {
            metadata: {
                name: pgConfigName,
                namespace: namespace
            },
            spec: {
                postgresVersion: "14",
                'postgresql.conf':""
            }
        })
    });

    beforeEach( () => {
        Cypress.Cookies.preserveOnce('sgToken')
        cy.visit(namespace + '/sgdbops/new')
    });

    after( () => {
        cy.deleteCluster(namespace, clusterName);

        cy.deleteCRD('sgpgconfigs', {
            metadata: {
                name: pgConfigName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgdbops', {
            metadata: {
                name: 'benchmark-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgdbops', {
            metadata: {
                name: 'vacuum-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgdbops', {
            metadata: {
                name: 'sec-upg-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgdbops', {
            metadata: {
                name: 'minor-upg-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgdbops', {
            metadata: {
                name: 'major-upg-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgdbops', {
            metadata: {
                name: 'restart-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgdbops', {
            metadata: {
                name: 'repack-' + resourceName,
                namespace: namespace
            }
        });
    });

    it('Create SGDbOps form should be visible', () => {
        cy.get('form#createDbops')
            .should('be.visible')
    });  

    it('Creating a Benchmark SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .clear()
            .type('benchmark-' + resourceName)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select(clusterName)
        
        // Test Benchmark input
        cy.get('label[for="benchmark"]')
            .click()

        // Test Benchmark input
        cy.get('label[for="benchmark"]')
            .click()

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')
        cy.get('.daterangepicker button.applyBtn')
            .click()

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
                expect($notification).contain('Database operation "benchmark-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdbops')
    }); 

    it('Creating a Vacuum SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .clear()
            .type('vacuum-' + resourceName)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select(clusterName)

        // Test Vaccum input
        cy.get('label[for="vacuum"]')
            .click()

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')
        cy.get('.daterangepicker button.applyBtn')
            .click()

        // Test Vaccum specs
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
            .type(resourceName)

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
                expect($notification).contain('Database operation "vacuum-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdbops')
    });

    it('Creating a Security Upgrade SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .clear()
            .type('sec-upg-' + resourceName)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select(clusterName)

        // Test Security Upgrade input
        cy.get('label[for="securityUpgrade"]')
            .click()

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')
        cy.get('.daterangepicker button.applyBtn')
            .click()

        // Test Security Upgrade specs
        cy.get('select[data-field="spec.securityUpgrade.method"]')
            .select('ReducedImpact')

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "sec-upg-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdbops')
    });

    it('Creating a Minor Version Upgrade SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .clear()
            .type('minor-upg-' + resourceName)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select(clusterName)
        
        // Test Minor Version Upgrade input
        cy.get('label[for="minorVersionUpgrade"]')
            .click()
    
        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')
        cy.get('.daterangepicker button.applyBtn')
            .click()

        // Test Minor Version Upgrade specs
        cy.get('select[data-field="spec.minorVersionUpgrade.method"]')
            .select('ReducedImpact')

        cy.get('select[data-field="spec.minorVersionUpgrade.postgresVersion"]')
            .select('13.4')

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "minor-upg-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdbops')
    });
   
    it('Creating a Major Version Upgrade SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .clear()
            .type('major-upg-' + resourceName)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select(clusterName)

        // Test Minor Version Upgrade input
        cy.get('label[for="majorVersionUpgrade"]')
            .click()

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')
        cy.get('.daterangepicker button.applyBtn')
            .click()

        // Test Minor Version Upgrade specs
        cy.get('label[data-field="spec.majorVersionUpgrade.link"]')
            .click()
        
        cy.get('label[data-field="spec.majorVersionUpgrade.check"]')
            .click()

        cy.get('select[data-field="spec.majorVersionUpgrade.postgresVersion"]')
            .select('14.0')

        cy.get('select[data-field="spec.majorVersionUpgrade.sgPostgresConfig"]')
            .select(pgConfigName)

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "major-upg-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdbops')
    });

    it('Creating a Restart SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .clear()
            .type('restart-' + resourceName)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select(clusterName)

        // Test Restart input
        cy.get('label[for="restart"]')
            .click()

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')    
        cy.get('.daterangepicker button.applyBtn')
            .click()

        // Test Restart specs
        cy.get('select[data-field="spec.restart.method"]')
            .select('ReducedImpact')

        cy.get('label[data-field="spec.restart.onlyPendingRestart"]')
            .click()

        // Test Submit form
        cy.get('form#createDbops button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Database operation "restart-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdbops')
    });
  
    it('Creating a Repack SGDbOps should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .clear()
            .type('repack-' + resourceName)

        // Test target SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select(clusterName)

        // Test Repack input
        cy.get('label[for="repack"]')
            .click()

        // Test runAt
        cy.get('input[data-field="spec.runAt"]')
            .type('9999-01-01 00:00:00')
        cy.get('.daterangepicker button.applyBtn')
            .click()

        // Test Repack specs
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
            .type(resourceName)

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
                expect($notification).contain('Database operation "repack-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgdbops')
    });
  })