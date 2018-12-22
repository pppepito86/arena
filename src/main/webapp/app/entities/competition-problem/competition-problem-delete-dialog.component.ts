import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { ICompetitionProblem } from 'app/shared/model/competition-problem.model';
import { CompetitionProblemService } from './competition-problem.service';

@Component({
    selector: 'jhi-competition-problem-delete-dialog',
    templateUrl: './competition-problem-delete-dialog.component.html'
})
export class CompetitionProblemDeleteDialogComponent {
    competitionProblem: ICompetitionProblem;

    constructor(
        private competitionProblemService: CompetitionProblemService,
        public activeModal: NgbActiveModal,
        private eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.competitionProblemService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'competitionProblemListModification',
                content: 'Deleted an competitionProblem'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-competition-problem-delete-popup',
    template: ''
})
export class CompetitionProblemDeletePopupComponent implements OnInit, OnDestroy {
    private ngbModalRef: NgbModalRef;

    constructor(private activatedRoute: ActivatedRoute, private router: Router, private modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ competitionProblem }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(CompetitionProblemDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.competitionProblem = competitionProblem;
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
