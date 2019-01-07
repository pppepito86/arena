import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiAlertService } from 'ng-jhipster';

import { ITagCollection } from 'app/shared/model/tag-collection.model';
import { AccountService } from 'app/core';
import { TagCollectionService } from './tag-collection.service';

@Component({
    selector: 'jhi-tag-collection',
    templateUrl: './tag-collection.component.html'
})
export class TagCollectionComponent implements OnInit, OnDestroy {
    tagCollections: ITagCollection[];
    currentAccount: any;
    eventSubscriber: Subscription;

    constructor(
        protected tagCollectionService: TagCollectionService,
        protected jhiAlertService: JhiAlertService,
        protected eventManager: JhiEventManager,
        protected accountService: AccountService
    ) {}

    loadAll() {
        this.tagCollectionService.query().subscribe(
            (res: HttpResponse<ITagCollection[]>) => {
                this.tagCollections = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    ngOnInit() {
        this.loadAll();
        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
        this.registerChangeInTagCollections();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: ITagCollection) {
        return item.id;
    }

    registerChangeInTagCollections() {
        this.eventSubscriber = this.eventManager.subscribe('tagCollectionListModification', response => this.loadAll());
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
