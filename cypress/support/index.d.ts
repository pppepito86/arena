/// <reference types="cypress" />

declare namespace Cypress {
    interface Chainable {
        /**
         * Logs in through the login modal and waits until the account menu
         * reflects the logged-in state.
         */
        login(username?: string, password?: string): Chainable<void>;
    }
}
