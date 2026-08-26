// /problem renders every problem in one Angular Material table and does its
// paging and filtering client-side (ProblemComponent fetches with size=100000),
// so the whole catalogue is loaded before the first assertion here can pass.
const rows = '[data-testid=problem-row]';
const PAGE_SIZE = 20; // first entry of the paginator's pageSizeOptions

describe('Problems table', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.login();
    // The Entities menu that links here is admin-only, but the route itself
    // only needs ROLE_USER.
    cy.visit('/#/problem');
    cy.get(rows, { timeout: 60000 }).should('have.length', PAGE_SIZE);
  });

  it('pages through the problems', () => {
    cy.get('.mat-paginator-range-label').invoke('text').should('match', /1\s*[–-]\s*20 of \d+/);

    cy.get('[data-testid=problem-cell-id]')
      .first()
      .invoke('text')
      .then(idOnPage1 => {
        cy.get('.mat-paginator-navigation-next').click();

        cy.get(rows).should('have.length', PAGE_SIZE);
        cy.get('.mat-paginator-range-label').invoke('text').should('match', /21\s*[–-]\s*40 of \d+/);
        cy.get('[data-testid=problem-cell-id]').first().invoke('text').should('not.eq', idOnPage1);

        cy.get('.mat-paginator-navigation-previous').click();
        cy.get('[data-testid=problem-cell-id]').first().invoke('text').should('eq', idOnPage1);
      });
  });

  it('changes the page size', () => {
    cy.get('mat-paginator mat-select').click();
    cy.get('mat-option').contains('50').click();

    cy.get(rows).should('have.length', 50);
    cy.get('.mat-paginator-range-label').invoke('text').should('match', /1\s*[–-]\s*50 of \d+/);
  });

  it('filters on free text', () => {
    // The filter only applies on Enter (see the keyup.enter binding).
    cy.get('[data-testid=problem-filter]').type('sign{enter}');

    cy.get(rows).should('have.length.lessThan', PAGE_SIZE);
    cy.get(rows).should('have.length.greaterThan', 0);
    cy.get('[data-testid=problem-cell-title]').should('contain', 'sign');

    // Clearing it brings the full page back.
    cy.get('[data-testid=problem-filter]').clear().type('{enter}');
    cy.get(rows).should('have.length', PAGE_SIZE);
  });

  it('filters on the group prefix', () => {
    // "г:" (Cyrillic) and "g:" both select the group column.
    cy.get('[data-testid=problem-filter]').type('г:A{enter}');

    cy.get(rows).should('have.length', PAGE_SIZE);
    cy.get('[data-testid=problem-cell-group]').each($cell => {
      expect($cell.text().trim()).to.eq('A');
    });
  });

  it('reports no matches for a filter nothing satisfies', () => {
    cy.get('[data-testid=problem-filter]').type('zzz-no-such-problem-zzz{enter}');

    cy.get(rows).should('have.length', 0);
  });
});
