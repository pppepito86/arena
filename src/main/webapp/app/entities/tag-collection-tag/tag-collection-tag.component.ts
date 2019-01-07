import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiAlertService } from 'ng-jhipster';

import { ITagCollectionTag } from 'app/shared/model/tag-collection-tag.model';
import { AccountService } from 'app/core';
import { TagCollectionTagService } from './tag-collection-tag.service';

@Component({
    selector: 'jhi-tag-collection-tag',
    templateUrl: './tag-collection-tag.component.html'
})
export class TagCollectionTagComponent implements OnInit, OnDestroy {
    tagCollectionTags: ITagCollectionTag[];
    currentAccount: any;
    eventSubscriber: Subscription;

    constructor(
        protected tagCollectionTagService: TagCollectionTagService,
        protected jhiAlertService: JhiAlertService,
        protected eventManager: JhiEventManager,
        protected accountService: AccountService
    ) {}

    loadAll() {
        this.tagCollectionTagService.query().subscribe(
            (res: HttpResponse<ITagCollectionTag[]>) => {
                this.tagCollectionTags = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    ngOnInit() {
        this.loadAll();
        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
        this.registerChangeInTagCollectionTags();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: ITagCollectionTag) {
        return item.id;
    }

    registerChangeInTagCollectionTags() {
        this.eventSubscriber = this.eventManager.subscribe('tagCollectionTagListModification', response => this.loadAll());
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
