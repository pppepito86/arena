import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ICompetitionProblem } from 'app/shared/model/competition-problem.model';
import { CompetitionProblemService } from './competition-problem.service';

@Component({
    selector: 'jhi-competition-problem-update',
    templateUrl: './competition-problem-update.component.html'
})
export class CompetitionProblemUpdateComponent implements OnInit {
    competitionProblem: ICompetitionProblem;
    isSaving: boolean;

    constructor(private competitionProblemService: CompetitionProblemService, private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ competitionProblem }) => {
            this.competitionProblem = competitionProblem;
        });
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.competitionProblem.id !== undefined) {
            this.subscribeToSaveResponse(this.competitionProblemService.update(this.competitionProblem));
        } else {
            this.subscribeToSaveResponse(this.competitionProblemService.create(this.competitionProblem));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<ICompetitionProblem>>) {
        result.subscribe((res: HttpResponse<ICompetitionProblem>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError() {
        this.isSaving = false;
    }
}
