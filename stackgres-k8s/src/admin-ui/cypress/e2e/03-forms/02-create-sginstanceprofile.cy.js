describe('Create SGInstanceProfile', () => {
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
        cy.visit(namespace + '/sginstanceprofiles/new')
    });

    it('Create SGInstanceProfile form should be visible', () => {
        cy.get('form#createProfile')
            .should('be.visible')
    });  

    after( () => {
        cy.deleteCRD('sginstanceprofiles', {
            metadata: {
                name: 'profile-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sginstanceprofiles', {
            metadata: {
                name: 'advanced-' + resourceName,
                namespace: namespace
            }
        });
    });

    it('Creating a Basic SGInstanceProfile should be possible', () => {
        // Test Config Name
        cy.get('[data-field="metadata.name"]')
            .type('profile-' + resourceName)
        
        // Test Memory
        cy.get('input[data-field="spec.memory"]')
            .type('2')
        
        // Test CPU
        cy.get('input[data-field="spec.cpu"]')
            .type('1')

        // Test Submit form
        cy.get('form#createProfile button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Profile "profile-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sginstanceprofiles')
    });

    it('Creating an advanced SGInstanceProfile should be possible', () => {
        // Test Config Name
        cy.get('[data-field="metadata.name"]')
            .type('advanced-' + resourceName)
        
        // Test Memory
        cy.get('input[data-field="spec.memory"]')
            .type('10')
        
        // Test CPU
        cy.get('input[data-field="spec.cpu"]')
            .type('4')

        // Test Huge Pages
        cy.get('input[data-field="spec.hugePages.hugepages-2Mi"]')
            .type('1')
        cy.get('input[data-field="spec.hugePages.hugepages-1Gi"]')
            .type('1')

        // Test Containers
        cy.get('form#createProfile li[data-step="containers"]')
            .click()

        cy.get('input[data-field="spec.containers[0].name"]')
            .type('test.container')
        cy.get('input[data-field="spec.containers[0].memory"]')
            .type('1')
        cy.get('input[data-field="spec.containers[0].cpu"]')
            .type('1')
        cy.get('input[data-field="spec.containers[0].hugePages.hugepages-2Mi"]')
            .type('1')
        cy.get('input[data-field="spec.containers[0].hugePages.hugepages-1Gi"]')
            .type('1')
        
        cy.get('fieldset[data-fieldset="containers"] .fieldsetFooter a.addRow')
            .click()

        cy.get('input[data-field="spec.containers[1].name"]')
            .type('test.container2')
        cy.get('input[data-field="spec.containers[1].memory"]')
            .type('1')
        cy.get('input[data-field="spec.containers[1].cpu"]')
            .type('1')
        cy.get('input[data-field="spec.containers[1].hugePages.hugepages-2Mi"]')
            .type('1')
        cy.get('input[data-field="spec.containers[1].hugePages.hugepages-1Gi"]')
            .type('1')

        // Test Init Containers
        cy.get('form#createProfile li[data-step="initContainers"]')
            .click()

        cy.get('input[data-field="spec.initContainers[0].name"]')
            .type('test.init-container')
        cy.get('input[data-field="spec.initContainers[0].memory"]')
            .type('1')
        cy.get('input[data-field="spec.initContainers[0].cpu"]')
            .type('1')
        cy.get('input[data-field="spec.initContainers[0].hugePages.hugepages-2Mi"]')
            .type('1')
        cy.get('input[data-field="spec.initContainers[0].hugePages.hugepages-1Gi"]')
            .type('1')

        cy.get('fieldset[data-fieldset="initContainers"] .fieldsetFooter a.addRow')
        .click()

        cy.get('input[data-field="spec.initContainers[1].name"]')
            .type('test.init-container2')
        cy.get('input[data-field="spec.initContainers[1].memory"]')
            .type('1')
        cy.get('input[data-field="spec.initContainers[1].cpu"]')
            .type('1')
        cy.get('input[data-field="spec.initContainers[1].hugePages.hugepages-2Mi"]')
            .type('1')
        cy.get('input[data-field="spec.initContainers[1].hugePages.hugepages-1Gi"]')
            .type('1')

        // Test Requests
        cy.get('form#createProfile li[data-step="requests"]')
            .click()

        cy.get('input[data-field="spec.requests.memory"]')
            .type('1')
        cy.get('input[data-field="spec.requests.cpu"]')
            .type('1')

        cy.get('input[data-field="spec.requests.containers[0].name"]')
            .type('test.container')
        cy.get('input[data-field="spec.requests.containers[0].memory"]')
            .type('1')
        cy.get('input[data-field="spec.requests.containers[0].cpu"]')
            .type('1')

        cy.get('fieldset[data-fieldset="requests"] fieldset.containers + .fieldsetFooter a.addRow')
            .click()
        
        cy.get('input[data-field="spec.requests.containers[1].name"]')
            .type('test.container2')
        cy.get('input[data-field="spec.requests.containers[1].memory"]')
            .type('1')
        cy.get('input[data-field="spec.requests.containers[1].cpu"]')
            .type('1')

        cy.get('input[data-field="spec.requests.initContainers[0].name"]')
            .type('test.init-container')
        cy.get('input[data-field="spec.requests.initContainers[0].memory"]')
            .type('1')
        cy.get('input[data-field="spec.requests.initContainers[0].cpu"]')
            .type('1')

        cy.get('fieldset[data-fieldset="requests"] fieldset.initContainers + .fieldsetFooter a.addRow')
            .click()
        
        cy.get('input[data-field="spec.requests.initContainers[1].name"]')
            .type('test.init-container2')
        cy.get('input[data-field="spec.requests.initContainers[1].memory"]')
            .type('1')
        cy.get('input[data-field="spec.requests.initContainers[1].cpu"]')
            .type('1')
            
        // Test Dry Run
        cy.get('form#createProfile button[data-field="dryRun"]')
            .click()

        cy.get('#crdSummary')
            .should('be.visible')

        cy.get('#crdSummary span.close')
            .click()
        
        cy.get('#crdSummary')
            .should('not.exist')

        // Test Submit form
        cy.get('form#createProfile button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Profile "advanced-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sginstanceprofiles')
    });
})