import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IProblem } from 'app/shared/model/problem.model';

@Component({
    selector: 'jhi-problem-detail',
    templateUrl: './problem-detail.component.html'
})
export class ProblemDetailComponent implements OnInit {
    problem: IProblem;

    constructor(private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ problem }) => {
            this.problem = problem;
        });
    }

    previousState() {
        window.history.back();
    }
}
