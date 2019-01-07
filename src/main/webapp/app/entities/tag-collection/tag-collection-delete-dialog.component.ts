import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { ITagCollection } from 'app/shared/model/tag-collection.model';
import { TagCollectionService } from './tag-collection.service';

@Component({
    selector: 'jhi-tag-collection-delete-dialog',
    templateUrl: './tag-collection-delete-dialog.component.html'
})
export class TagCollectionDeleteDialogComponent {
    tagCollection: ITagCollection;

    constructor(
        protected tagCollectionService: TagCollectionService,
        public activeModal: NgbActiveModal,
        protected eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.tagCollectionService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'tagCollectionListModification',
                content: 'Deleted an tagCollection'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-tag-collection-delete-popup',
    template: ''
})
export class TagCollectionDeletePopupComponent implements OnInit, OnDestroy {
    protected ngbModalRef: NgbModalRef;

    constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ tagCollection }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(TagCollectionDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.tagCollection = tagCollection;
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
