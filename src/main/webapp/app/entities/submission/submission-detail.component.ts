import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ISubmission } from 'app/shared/model/submission.model';
import { SubmissionService } from './submission.service';
import { CompetitionProblemService } from '../competition-problem';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';

@Component({
    styleUrls: ['./submission-detail.css'],
    selector: 'jhi-submission-detail',
    templateUrl: './submission-detail.component.html'
})
export class SubmissionDetailComponent implements OnInit {
    submission: ISubmission;
    refreshIntervalMs = 2000;
    refreshInterval: any;
    submissionId: number;
    competitionProblem: ICompetitionProblem;

    constructor(
        protected activatedRoute: ActivatedRoute,
        private submissionService: SubmissionService,
        private competitionProblemService: CompetitionProblemService
    ) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ submission }) => {
            this.submission = submission;
            this.submissionId = this.submission.id;

            this.competitionProblemService.find(this.submission.competitionProblemId).subscribe(
                res => {
                    console.log(res);
                    this.competitionProblem = res.body;
                },
                error => {
                    console.log(error);
                }
            );

            if (!this.isJudged(this.submission)) {
                this.refreshInterval = setInterval(() => {
                    this.submissionService.find(this.submissionId).subscribe(res => {
                        this.submission = res.body;
                        if (this.isJudged(this.submission)) {
                            clearInterval(this.refreshInterval);
                        }
                    });
                }, this.refreshIntervalMs);
            }
        });
    }

    isJudged(submission: ISubmission): boolean {
        return submission.verdict != null;
    }

    previousState() {
        window.history.back();
    }
}
