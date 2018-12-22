import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ICompetition } from 'app/shared/model/competition.model';

@Component({
    selector: 'jhi-competition-detail',
    templateUrl: './competition-detail.component.html'
})
export class CompetitionDetailComponent implements OnInit {
    competition: ICompetition;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ competition }) => {
            this.competition = competition;
        });
    }

    previousState() {
        window.history.back();
    }
}
