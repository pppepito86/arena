import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AccountService } from 'app/core';

@Component({
    selector: 'jhi-catalog',
    templateUrl: './catalog.component.html'
})
export class CatalogComponent implements OnInit {
    id: number;
    currentAccount: any;

    constructor(protected activatedRoute: ActivatedRoute, protected accountService: AccountService) {}

    ngOnInit() {
        this.id = this.activatedRoute.snapshot.params['id'];

        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
    }
}
