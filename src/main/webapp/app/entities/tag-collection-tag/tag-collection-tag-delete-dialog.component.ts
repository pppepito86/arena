import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { ITagCollectionTag } from 'app/shared/model/tag-collection-tag.model';
import { TagCollectionTagService } from './tag-collection-tag.service';

@Component({
    selector: 'jhi-tag-collection-tag-delete-dialog',
    templateUrl: './tag-collection-tag-delete-dialog.component.html'
})
export class TagCollectionTagDeleteDialogComponent {
    tagCollectionTag: ITagCollectionTag;

    constructor(
        protected tagCollectionTagService: TagCollectionTagService,
        public activeModal: NgbActiveModal,
        protected eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.tagCollectionTagService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'tagCollectionTagListModification',
                content: 'Deleted an tagCollectionTag'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-tag-collection-tag-delete-popup',
    template: ''
})
export class TagCollectionTagDeletePopupComponent implements OnInit, OnDestroy {
    protected ngbModalRef: NgbModalRef;

    constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ tagCollectionTag }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(TagCollectionTagDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.tagCollectionTag = tagCollectionTag;
                this.ngbModalRef.result.then(
                    result => {
                        this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
                        this.ngbModalRef = null;
                    },
                    reason => {
                        this.router.navigate([{ outlets: { popup: null } }], { replaceUrl: true, queryParamsHandling: 'merge' });
                        this.ngbModalRef = null;
                    }
                );
            }, 0);
        });
    }

    ngOnDestroy() {
        this.ngbModalRef = null;
    }
}
