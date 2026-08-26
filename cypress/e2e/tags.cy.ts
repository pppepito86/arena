describe('Tags', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('is reachable from the navbar without logging in', () => {
    cy.get('[data-testid=nav-tags]').click();

    cy.hash().should('eq', '#/tag');
    cy.get('[data-testid=tag-row]').should('have.length.greaterThan', 20);
  });

  it('filters the list by title as you type', () => {
    cy.get('[data-testid=nav-tags]').click();
    cy.get('[data-testid=tag-row]').should('have.length.greaterThan', 20);

    cy.get('[data-testid=tag-row]').then($allRows => {
      const total = $allRows.length;

      // The search box matches on the tag title, case-insensitively.
      cy.get('[data-testid=tag-search]').type('граф');

      cy.get('[data-testid=tag-row]').should('have.length.lessThan', total);
      cy.get('[data-testid=tag-row]').should('have.length.greaterThan', 0);
      cy.get('[data-testid=tag-row]').each($row => {
        expect($row.text().toLowerCase()).to.include('граф');
      });

      // Clearing the box restores the full list.
      cy.get('[data-testid=tag-search]').clear();
      cy.get('[data-testid=tag-row]').should('have.length', total);
    });
  });

  it('opens a tag from the list', () => {
    cy.get('[data-testid=nav-tags]').click();
    cy.get('[data-testid=tag-search]').type('граф');

    cy.get('[data-testid=tag-row]').first().find('a').click();

    cy.hash().should('match', /^#\/tag\/\d+\/view/);
  });
});
