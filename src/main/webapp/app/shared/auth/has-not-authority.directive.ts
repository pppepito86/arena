import { Directive, Input, TemplateRef, ViewContainerRef } from '@angular/core';
import { AccountService } from 'app/core/auth/account.service';

@Directive({
    selector: '[jhiHasNotAuthority]'
})
export class HasNotAuthorityDirective {
    private authorities: string[];

    constructor(
        private accountService: AccountService,
        private templateRef: TemplateRef<any>,
        private viewContainerRef: ViewContainerRef
    ) {}

    @Input()
    set jhiHasNotAuthority(value: string) {
        this.authorities = [value];
        this.updateView();
        // Get notified each time authentication state changes.
        this.accountService.getAuthenticationState().subscribe(identity => this.updateView());
    }

    private updateView(): void {
        const hasAnyAuthority = this.accountService.hasAnyAuthority(this.authorities);
        this.viewContainerRef.clear();
        if (!hasAnyAuthority) {
            this.viewContainerRef.createEmbeddedView(this.templateRef);
        }
    }
}
