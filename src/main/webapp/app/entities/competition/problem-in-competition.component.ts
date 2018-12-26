import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse, HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import { IProblem } from 'app/shared/model/problem.model';
import { CompetitionService } from './competition.service';
import { ICompetition } from '../../shared/model/competition.model';
import { JhiAlertService } from 'ng-jhipster';
import { Title } from '@angular/platform-browser';

@Component({
    selector: 'jhi-problem-in-competition',
    templateUrl: './problem-in-competition.component.html'
})
export class ProblemInCompetitionComponent implements OnInit {
    problem: IProblem;
    competitionId: number;
    competitionProblemId: number;
    isSubmitting: boolean;
    solution: string = '#include <bits/stdc++.h>\n' + 'using namespace std;\n' + '\n' + 'int main() {\n' + '\n' + '    return 0;\n' + '}';

    constructor(
        protected activatedRoute: ActivatedRoute,
        protected competitionService: CompetitionService,
        protected jhiAlertService: JhiAlertService,
        private titleService: Title,
        private http: HttpClient
    ) {}

    ngOnInit() {
        const params = this.activatedRoute.snapshot.paramMap;
        this.competitionId = Number(params.get('id'));
        this.competitionProblemId = Number(params.get('compProb'));

        this.competitionService.findProblem(this.competitionId, this.competitionProblemId).subscribe(
            (res: HttpResponse<IProblem>) => {
                this.problem = res.body;
                this.titleService.setTitle(this.problem.title);
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }

    submit() {
        this.isSubmitting = true;
        this.competitionService.submitSolution(this.competitionId, this.competitionProblemId, this.solution).subscribe(
            (res: HttpResponse<IProblem>) => {
                this.isSubmitting = false;
            },
            (res: HttpErrorResponse) => {
                this.isSubmitting = false;
                this.onError(res.message);
            }
        );
    }
}
