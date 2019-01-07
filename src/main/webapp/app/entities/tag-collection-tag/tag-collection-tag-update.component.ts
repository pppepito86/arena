import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JhiAlertService } from 'ng-jhipster';

import { ITagCollectionTag } from 'app/shared/model/tag-collection-tag.model';
import { TagCollectionTagService } from './tag-collection-tag.service';
import { ITagCollection } from 'app/shared/model/tag-collection.model';
import { TagCollectionService } from 'app/entities/tag-collection';
import { ITag } from 'app/shared/model/tag.model';
import { TagService } from 'app/entities/tag';

@Component({
    selector: 'jhi-tag-collection-tag-update',
    templateUrl: './tag-collection-tag-update.component.html'
})
export class TagCollectionTagUpdateComponent implements OnInit {
    tagCollectionTag: ITagCollectionTag;
    isSaving: boolean;

    tagcollections: ITagCollection[];

    tags: ITag[];

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected tagCollectionTagService: TagCollectionTagService,
        protected tagCollectionService: TagCollectionService,
        protected tagService: TagService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ tagCollectionTag }) => {
            this.tagCollectionTag = tagCollectionTag;
        });
        this.tagCollectionService.query().subscribe(
            (res: HttpResponse<ITagCollection[]>) => {
                this.tagcollections = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
        this.tagService.query().subscribe(
            (res: HttpResponse<ITag[]>) => {
                this.tags = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.tagCollectionTag.id !== undefined) {
            this.subscribeToSaveResponse(this.tagCollectionTagService.update(this.tagCollectionTag));
        } else {
            this.subscribeToSaveResponse(this.tagCollectionTagService.create(this.tagCollectionTag));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<ITagCollectionTag>>) {
        result.subscribe((res: HttpResponse<ITagCollectionTag>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    protected onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    protected onSaveError() {
        this.isSaving = false;
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }

    trackTagCollectionById(index: number, item: ITagCollection) {
        return item.id;
    }

    trackTagById(index: number, item: ITag) {
        return item.id;
    }
}
