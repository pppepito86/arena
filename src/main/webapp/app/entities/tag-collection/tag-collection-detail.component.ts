import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ITagCollection } from 'app/shared/model/tag-collection.model';

@Component({
    selector: 'jhi-tag-collection-detail',
    templateUrl: './tag-collection-detail.component.html'
})
export class TagCollectionDetailComponent implements OnInit {
    tagCollection: ITagCollection;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ tagCollection }) => {
            this.tagCollection = tagCollection;
        });
    }

    previousState() {
        window.history.back();
    }
}
