
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

  it('unregistered users should be able to browse problems but not submit', () => {
    // 1. Make sure user is not logged in.
    cy.get('#account-menu').click();
    cy.get('#login').should('be.visible');

    // 2. Select a problem from catalog
    cy.get('[data-testid=nav-catalog]').click();

    // Breadcrumb: Задачи
    cy.get('[data-testid=breadcrumb]').should('contain', 'Задачи');

    // Navigate to Есенен турнир
    cy.get('[data-testid="competition-link-Есенен турнир"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'Есенен турнир');

    // Navigate to 2017
    cy.get('[data-testid="competition-link-2017"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', '2017');

    // Navigate to group B
    cy.get('[data-testid="competition-link-B"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'B');

    // Select problem 'bridges'
    cy.get('[data-testid="competition-link-bridges"], [data-testid="problem-link-bridges"]').click();

    cy.get('h2').should('contain', 'bridges');
    cy.get('[data-testid=statement-link]').should('be.visible');
    cy.get('[data-testid=solution-form]').should('not.exist');

    // Follow breadcrumbs back
    cy.get('[data-testid="breadcrumb-link-2017"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', '2017');

    cy.get('[data-testid="breadcrumb-link-Есенен турнир"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'Есенен турнир');

    cy.get('[data-testid="breadcrumb-link-Задачи"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'Задачи');
  });

  it('should allow jumping levels in catalog navigation', () => {
    cy.get('[data-testid=nav-catalog]').click();

    // Jump from root to 2017 directly (2017 is rendered as a grandchild in Есенен турнир's row)
    cy.get('[data-testid="competition-link-Есенен турнир"]').closest('tr').within(() => {
      cy.get('[data-testid="competition-link-2017"]').click();
    });
    cy.get('[data-testid=breadcrumb]').should('contain', '2017');

    // Jump from 2017 to bridges directly (bridges is rendered as a grandchild in group B's row)
    cy.get('[data-testid="competition-link-B"]').closest('tr').within(() => {
      cy.get('[data-testid="problem-link-bridges"]').click();
    });

    cy.get('h2').should('contain', 'bridges');
    cy.get('[data-testid=breadcrumb]').should('contain', 'B');
  });
});
