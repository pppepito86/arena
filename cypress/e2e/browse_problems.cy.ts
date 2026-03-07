
describe('Browse Problems', () => {
  beforeEach(() => {
    // Fail the test if any API request fails with status >= 400
    // but ignore 401 which are expected for guests on certain endpoints
    cy.intercept('**/api/**', (req) => {
      req.continue((res) => {
        if (res.statusCode >= 400) {
          const isExpected401 = res.statusCode === 401 && (
            req.url.endsWith('/api/account') || 
            req.url.includes('/api/tags') ||
            req.url.includes('/api/competition-problems/')
          );
          
          if (isExpected401) {
            return;
          }
          throw new Error(`API request failed: ${req.method} ${req.url} returned ${res.statusCode}`);
        }
      });
    });

    cy.visit('/');
  });

  const waitForLoading = () => {
    // Fixed wait as requested because rows appear sequentially without a spinner
    cy.wait(2000);
  };

  it('unregistered users should be able to browse problems but not submit', () => {
    // 1. Make sure user is not logged in.
    cy.get('#account-menu').click();
    cy.get('#login').should('be.visible');

    // 2. Select a problem from catalog
    cy.contains('Задачи').click();
    waitForLoading();
    
    // Breadcrumb: Задачи
    cy.get('jhi-competition-path').should('contain', 'Задачи');

    // Navigate to Есенен турнир
    cy.get('jhi-competition-children a').contains('Есенен турнир').click();
    waitForLoading();
    cy.get('jhi-competition-path').should('contain', 'Есенен турнир');
    
    // Navigate to 2017
    cy.get('jhi-competition-children a').contains('2017').click();
    waitForLoading();
    cy.get('jhi-competition-path').should('contain', '2017');

    // Navigate to group B. Exact match check.
    cy.get('jhi-competition-children table td:first-child a').filter((i, el) => {
       return el.innerText.trim() === 'B';
    }).click();
    
    waitForLoading();
    cy.get('jhi-competition-path').should('contain', 'B');
    
    // Select problem 'bridges'
    cy.contains('a', 'bridges', { matchCase: false }).click();
    waitForLoading();
    
    cy.get('h2').should('contain', 'bridges');
    cy.contains('Условие').should('be.visible');
    cy.contains('Решение').should('not.exist');
    cy.contains('Изпрати').should('not.exist');

    // Follow breadcrumbs back
    cy.get('jhi-competition-path').contains('2017').click();
    waitForLoading();
    cy.get('jhi-competition-path').should('contain', '2017');

    cy.get('jhi-competition-path').contains('Есенен турнир').click();
    waitForLoading();
    cy.get('jhi-competition-path').should('contain', 'Есенен турнир');

    cy.get('jhi-competition-path').contains('Задачи').click();
    waitForLoading();
    cy.get('jhi-competition-path').should('contain', 'Задачи');
  });

  it('should allow jumping levels in catalog navigation', () => {
    cy.contains('Задачи').click();
    waitForLoading();

    // Jump from root to 2017 directly (2017 is rendered in the row of Есенен турнир)
    cy.get('jhi-competition-children table').contains('tr', 'Есенен турнир').within(() => {
       cy.contains('a', '2017').click();
    });
    waitForLoading();
    cy.get('jhi-competition-path').should('contain', '2017');

    // Jump from 2017 to bridges directly (bridges is rendered in the row of Group B)
    cy.get('jhi-competition-children table').contains('tr', 'B').within(() => {
       cy.contains('a', 'bridges', { matchCase: false }).click();
    });
    waitForLoading();
    
    cy.get('h2').should('contain', 'bridges');
    cy.get('jhi-competition-path').should('contain', 'B');
  });
});
