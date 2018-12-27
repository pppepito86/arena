import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { ISubmission } from 'app/shared/model/submission.model';
import { SubmissionService } from './submission.service';
import { CompetitionProblemService } from '../competition-problem';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';
import { JhiAlertService } from 'ng-jhipster';

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
    securityKey: string;

    constructor(
        protected activatedRoute: ActivatedRoute,
        private submissionService: SubmissionService,
        private competitionProblemService: CompetitionProblemService,
        protected jhiAlertService: JhiAlertService
    ) {}

    ngOnInit() {
        this.securityKey = this.activatedRoute.snapshot.queryParams['securityKey'];
        if (this.securityKey === undefined) {
            this.securityKey = '';
        }
        this.submissionId = this.activatedRoute.snapshot.params['id'];

        this.submissionService.find(this.submissionId, this.securityKey).subscribe(
            res => {
                this.submission = res.body;
                this.submissionId = this.submission.id;

                this.loadProblem();

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
            },
            (res: HttpErrorResponse) => {
                this.submission = null;
                this.jhiAlertService.error(res.message, null, null);
            }
        );
    }

    loadProblem() {
        this.competitionProblemService.find(this.submission.competitionProblemId).subscribe(
            res => {
                console.log(res);
                this.competitionProblem = res.body;
            },
            error => {
                console.log(error);
            }
        );
    }

    isJudged(submission: ISubmission): boolean {
        return (
            submission.verdict != null && submission.verdict.toLowerCase() !== 'waiting' && submission.verdict.toLowerCase() !== 'judging'
        );
    }

    previousState() {
        window.history.back();
    }
}
