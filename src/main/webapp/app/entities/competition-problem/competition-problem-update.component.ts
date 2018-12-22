import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JhiAlertService } from 'ng-jhipster';

import { ICompetitionProblem } from 'app/shared/model/competition-problem.model';
import { CompetitionProblemService } from './competition-problem.service';
import { IProblem } from 'app/shared/model/problem.model';
import { ProblemService } from 'app/entities/problem';
import { ICompetition } from 'app/shared/model/competition.model';
import { CompetitionService } from 'app/entities/competition';

@Component({
    selector: 'jhi-competition-problem-update',
    templateUrl: './competition-problem-update.component.html'
})
export class CompetitionProblemUpdateComponent implements OnInit {
    competitionProblem: ICompetitionProblem;
    isSaving: boolean;

    problems: IProblem[];

    competitions: ICompetition[];

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected competitionProblemService: CompetitionProblemService,
        protected problemService: ProblemService,
        protected competitionService: CompetitionService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ competitionProblem }) => {
            this.competitionProblem = competitionProblem;
        });
        this.problemService.query().subscribe(
            (res: HttpResponse<IProblem[]>) => {
                this.problems = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
        this.competitionService.query().subscribe(
            (res: HttpResponse<ICompetition[]>) => {
                this.competitions = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
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

    protected subscribeToSaveResponse(result: Observable<HttpResponse<ICompetitionProblem>>) {
        result.subscribe((res: HttpResponse<ICompetitionProblem>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
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

    trackProblemById(index: number, item: IProblem) {
        return item.id;
    }

    trackCompetitionById(index: number, item: ICompetition) {
        return item.id;
    }
}
