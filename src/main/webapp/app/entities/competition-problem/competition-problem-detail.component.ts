import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ICompetitionProblem } from 'app/shared/model/competition-problem.model';

@Component({
    selector: 'jhi-competition-problem-detail',
    templateUrl: './competition-problem-detail.component.html'
})
export class CompetitionProblemDetailComponent implements OnInit {
    competitionProblem: ICompetitionProblem;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ competitionProblem }) => {
            this.competitionProblem = competitionProblem;
        });
    }

    previousState() {
        window.history.back();
    }
}
