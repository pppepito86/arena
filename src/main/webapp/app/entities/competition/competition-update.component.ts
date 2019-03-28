import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JhiAlertService } from 'ng-jhipster';

import { ICompetition } from 'app/shared/model/competition.model';
import { CompetitionService } from './competition.service';
import { ProblemService } from '../problem/problem.service';
import { IProblem, Problem } from '../../shared/model/problem.model';
import { CompetitionProblem, ICompetitionProblem } from '../../shared/model/competition-problem.model';
import { CompetitionProblemService } from '../competition-problem';

@Component({
    selector: 'jhi-competition-update',
    templateUrl: './competition-update.component.html'
})
export class CompetitionUpdateComponent implements OnInit {
    competition: ICompetition;
    isSaving: boolean;

    competitions: ICompetition[];
    children_competitions: ICompetition[];
    children_problems: ICompetitionProblem[];

    newSubCompetitionId: number;
    newSubProblemId: number;

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected competitionService: CompetitionService,
        protected competitionProblemService: CompetitionProblemService,
        protected problemService: ProblemService,
        protected activatedRoute: ActivatedRoute,
        protected router: Router
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ competition }) => {
            this.competition = competition;

            // override ParentID if a URL parameter is set
            this.activatedRoute.queryParams.subscribe(params => {
                if (params['parent'] !== undefined) {
                    this.competition.parentId = params['parent'];
                }
            });
        });

        this.competitionService.query().subscribe(
            (res: HttpResponse<ICompetition[]>) => {
                this.competitions = res.body;
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );

        if (this.competition.id !== undefined) {
            this.competitionService
                .findChildren(this.competition.id, {
                    page: 0,
                    size: 10000
                })
                .subscribe(
                    (res: HttpResponse<ICompetition[]>) => {
                        this.children_competitions = res.body;
                        this.children_competitions.sort((a, b) => a.order - b.order);
                    },
                    (res: HttpErrorResponse) => this.onError(res.message)
                );
            this.competitionService
                .findProblems(this.competition.id, {
                    page: 0,
                    size: 10000
                })
                .subscribe(
                    (res: HttpResponse<ICompetitionProblem[]>) => {
                        this.children_problems = res.body;
                        this.children_problems.sort((a, b) => a.order - b.order);
                    },
                    (res: HttpErrorResponse) => this.onError(res.message)
                );
        }
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.competition.id !== undefined) {
            this.subscribeToSaveResponse(this.competitionService.update(this.competition));
        } else {
            this.subscribeToSaveResponse(this.competitionService.create(this.competition));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<ICompetition>>) {
        result.subscribe(
            (res: HttpResponse<ICompetition>) => {
                this.competition.id = res.body.id;
                this.uploadOrders();
                this.onSaveSuccess();
            },
            (res: HttpErrorResponse) => this.onSaveError()
        );
    }

    uploadOrders() {
        this.competitionService
            .updateSubCompetitions(this.competition.id, this.children_competitions)
            .subscribe(() => {}, error => this.onSaveError());
        this.competitionService
            .updateSubProblems(this.competition.id, this.children_problems)
            .subscribe(() => {}, error => this.onSaveError());
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

    trackCompetitionById(index: number, item: ICompetition) {
        return item.id;
    }

    onSubCompetitionAdd(subCompetitionId: number) {
        // console.log("addign ", this.newSubCompetitionId);
        this.competitionService.find(subCompetitionId).subscribe(
            (res: HttpResponse<ICompetition>) => {
                this.children_competitions.push(res.body);
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    onSubCompetitionRemove(subCompetitionId: number) {
        for (let i = 0; i < this.children_competitions.length; i++) {
            if (this.children_competitions[i].id === subCompetitionId) {
                this.children_competitions.splice(i, 1);
                break;
            }
        }
    }

    onSubProblemAdded(subProblemId: number) {
        // console.log("addign ", this.newSubCompetitionId);
        this.problemService.find(subProblemId).subscribe(
            (res: HttpResponse<IProblem>) =>
                this.children_problems.push({
                    problemId: res.body.id,
                    competitionId: this.competition.id,
                    title: res.body.title
                }),
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    onSubProblemRemove(subProblemId: number) {
        console.log(this.children_problems);
        for (let i = 0; i < this.children_problems.length; i++) {
            if (this.children_problems[i].problemId === subProblemId) {
                this.children_problems.splice(i, 1);
                break;
            }
        }
    }

    onSubCompetitionCreate() {
        this.router.navigate(['/competition', 'new'], { queryParams: { parent: this.competition.id } });
    }

    onSubProblemCreate() {
        // 1. Creates Problem
        this.problemService.create(new Problem()).subscribe(
            (res: HttpResponse<IProblem>) => {
                // 2. Creates Competition Problem
                const problemId = res.body.id;
                const competitionProblem = new CompetitionProblem();
                competitionProblem.problemId = problemId;
                competitionProblem.competitionId = this.competition.id;
                this.competitionProblemService.create(competitionProblem).subscribe(
                    value => {
                        // 3. Redirects to the edit page of the created Problem
                        this.router.navigate(['/problem', problemId, 'edit']);
                    },
                    (err: HttpErrorResponse) => this.onError(err.message)
                );
            },
            (err: HttpErrorResponse) => this.onError(err.message)
        );
    }
}
