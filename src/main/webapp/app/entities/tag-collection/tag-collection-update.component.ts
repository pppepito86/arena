import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ITagCollection } from 'app/shared/model/tag-collection.model';
import { TagCollectionService } from './tag-collection.service';

@Component({
    selector: 'jhi-tag-collection-update',
    templateUrl: './tag-collection-update.component.html'
})
export class TagCollectionUpdateComponent implements OnInit {
    tagCollection: ITagCollection;
    isSaving: boolean;

    constructor(protected tagCollectionService: TagCollectionService, protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ tagCollection }) => {
            this.tagCollection = tagCollection;
        });
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.tagCollection.id !== undefined) {
            this.subscribeToSaveResponse(this.tagCollectionService.update(this.tagCollection));
        } else {
            this.subscribeToSaveResponse(this.tagCollectionService.create(this.tagCollection));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<ITagCollection>>) {
        result.subscribe((res: HttpResponse<ITagCollection>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    protected onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    protected onSaveError() {
        this.isSaving = false;
    }
}
