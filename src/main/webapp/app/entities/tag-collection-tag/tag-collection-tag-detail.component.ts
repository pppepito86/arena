import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ITagCollectionTag } from 'app/shared/model/tag-collection-tag.model';

@Component({
    selector: 'jhi-tag-collection-tag-detail',
    templateUrl: './tag-collection-tag-detail.component.html'
})
export class TagCollectionTagDetailComponent implements OnInit {
    tagCollectionTag: ITagCollectionTag;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ tagCollectionTag }) => {
            this.tagCollectionTag = tagCollectionTag;
        });
    }

    previousState() {
        window.history.back();
    }
}
