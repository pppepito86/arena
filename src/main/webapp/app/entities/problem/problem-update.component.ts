import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { IProblem } from 'app/shared/model/problem.model';
import { ProblemService } from './problem.service';

@Component({
    selector: 'jhi-problem-update',
    templateUrl: './problem-update.component.html'
})
export class ProblemUpdateComponent implements OnInit {
    problem: IProblem;
    isSaving: boolean;

    constructor(protected problemService: ProblemService, protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ problem }) => {
            this.problem = problem;
        });
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.problem.id !== undefined) {
            this.subscribeToSaveResponse(this.problemService.update(this.problem));
        } else {
            this.subscribeToSaveResponse(this.problemService.create(this.problem));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<IProblem>>) {
        result.subscribe((res: HttpResponse<IProblem>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    protected onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    protected onSaveError() {
        this.isSaving = false;
    }
}
