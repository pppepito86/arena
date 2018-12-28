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
    testDetails: any;

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
                this.submission.details = JSON.parse(res.body.details);
                this.parseTestDetails();
                this.submissionId = this.submission.id;

                if (!this.isJudged(this.submission)) {
                    this.refreshInterval = setInterval(() => {
                        this.submissionService.find(this.submissionId).subscribe(res => {
                            this.submission = res.body;
                            this.submission.details = JSON.parse(res.body.details);
                            this.parseTestDetails();
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

    parseTestDetails() {
        if (!this.submission || !this.submission.details || !this.submission.details['scoreSteps']) {
            return;
        }

        this.testDetails = [];
        for (let i = 1; ; i++) {
            let property = `Test${i}`;
            if (this.submission.details['scoreSteps'].hasOwnProperty(property)) {
                let val = this.submission.details['scoreSteps'][property];
                if (!val.output) val.output = '';
                if (val.output.length > 70) val.output = val.output.substring(0, 70) + '...';
                this.testDetails.push({
                    key: property,
                    value: val
                });
            } else {
                break;
            }
        }
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
