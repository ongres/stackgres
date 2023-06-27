Cypress.Commands.add('login', () => {
    cy.request({
        method: 'POST',
        url: Cypress.env('api') + '/auth/login',
        body: {
            username: Cypress.env('username'),
            password: Cypress.env('password')
        }
    })
    .then( (resp) => {
        cy.setCookie('sgToken', resp.body.access_token);
    })
});

Cypress.Commands.add('gc', () => {
  cy.window().then((win) => {
    win.location.href = 'about:blank'
  })
  if (Cypress.isBrowser('firefox')) {
      // run gc multiple times in an attempt to force a major GC between tests
      Cypress.backend('firefox:force:gc')
      Cypress.backend('firefox:force:gc')
      Cypress.backend('firefox:force:gc')
      Cypress.backend('firefox:force:gc')
      Cypress.backend('firefox:force:gc')
  }
  if (Cypress.isBrowser('chrome')) {
      cy.window().then(win => {
        // window.gc is enabled with --js-flags=--expose-gc chrome flag
        if (typeof win.gc === 'function') {
          // run gc multiple times in an attempt to force a major GC between tests
          win.gc();
          win.gc();
          win.gc();
          win.gc();
          win.gc();
        }
      });
  }
});

Cypress.Commands.add('createCRD', (kind, crd) => {
    cy.getCookie('sgToken').then((cookie) => {
        cy.request({
            method: 'POST',
            url: Cypress.env('api') + '/' + kind,
            headers: {
                Authorization: 'Bearer ' + cookie.value,
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            body: crd
        })   
    })    
});

Cypress.Commands.add('deleteCRD', (kind, crd) => {
    cy.getCookie('sgToken').then((cookie) => {
        cy.request({
            method: 'DELETE',
            url: Cypress.env('api') + '/' + kind,
            headers: {
                Authorization: 'Bearer ' + cookie.value,
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            body: crd
        })   
    }) 
});

function getResources(kind) {
    cy.getCookie('sgToken').then((cookie) => {
        cy.request({
            method: 'GET',
            url: Cypress.env('api') + '/' + kind,
            headers: {
                Authorization: 'Bearer ' + cookie.value,
                'Content-Type': 'application/json',
                Accept: 'application/json'
            }
        }).then((resp)=> {
            return cy.wrap(resp.body)
        })   
    }) 
};

Cypress.Commands.add('getResources', getResources);

Cypress.Commands.add('deleteCluster', (namespace, clusterName) => {
    cy.getResources('sgclusters').then(resp => {
        let cluster = resp.find(el => (el.metadata.namespace === namespace) && (el.metadata.name === clusterName))
        if (cluster) {
            cy.deleteCRD('sgclusters', {
                metadata: {
                    name: cluster.metadata.name,
                    namespace: cluster.metadata.namespace
                }
            })
            .then( () => {
                cy.deleteCRD('sginstanceprofiles', {
                    metadata: {
                        name: cluster.spec.sgInstanceProfile,
                        namespace: cluster.metadata.namespace
                    }
                });
                cy.deleteCRD('sgpgconfigs', {
                    metadata: {
                        name: cluster.spec.configurations.sgPostgresConfig,
                        namespace: cluster.metadata.namespace
                    }
                });
                cy.deleteCRD('sgpoolconfigs', {
                    metadata: {
                        name: cluster.spec.configurations.sgPoolingConfig,
                        namespace: cluster.metadata.namespace
                    }
                });
            })
        }
    })
});

Cypress.Commands.add('deleteShardedCluster', (namespace, clusterName) => {
    cy.getResources('sgshardedclusters').then(resp => {
        let cluster = resp.find(el => (el.metadata.namespace === namespace) && (el.metadata.name === clusterName));
        if (cluster) {
            cy.deleteCRD('sgshardedclusters', {
                metadata: {
                    name: cluster.metadata.name,
                    namespace: cluster.metadata.namespace
                }
            })
            .then( () => {
                cluster.status.clusters.forEach(name => {
                    cy.deleteCRD('sgclusters', {
                        metadata: {
                            name: name,
                            namespace: cluster.metadata.namespace
                        }
                    });
                });
                cy.deleteCRD('sginstanceprofiles', {
                    metadata: {
                        name: cluster.spec.coordinator.sgInstanceProfile,
                        namespace: cluster.metadata.namespace
                    }
                });
                cy.deleteCRD('sgpgconfigs', {
                    metadata: {
                        name: cluster.spec.coordinator.configurations.sgPostgresConfig,
                        namespace: cluster.metadata.namespace
                    }
                });
                cy.deleteCRD('sgpoolconfigs', {
                    metadata: {
                        name: cluster.spec.coordinator.configurations.sgPoolingConfig,
                        namespace: cluster.metadata.namespace
                    }
                });
                cy.deleteCRD('sginstanceprofiles', {
                    metadata: {
                        name: cluster.spec.shards.sgInstanceProfile,
                        namespace: cluster.metadata.namespace
                    }
                });
                cy.deleteCRD('sgpgconfigs', {
                    metadata: {
                        name: cluster.spec.shards.configurations.sgPostgresConfig,
                        namespace: cluster.metadata.namespace
                    }
                });
                cy.deleteCRD('sgpoolconfigs', {
                    metadata: {
                        name: cluster.spec.shards.configurations.sgPoolingConfig,
                        namespace: cluster.metadata.namespace
                    }
                });
            });
        }
    })
});
