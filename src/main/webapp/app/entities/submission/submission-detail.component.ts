import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { ISubmission } from 'app/shared/model/submission.model';
import { SubmissionService } from './submission.service';
import { CompetitionProblemService } from '../competition-problem';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';
import { JhiAlertService } from 'ng-jhipster';
import { ITag } from '../../shared/model/tag.model';
import { TagService } from '../tag';

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
    submissionDetails: any;
    tags = [];
    autocompleteTags: ITag[] = [];
    tagStatus = 0;
    tagStatusTimeout;
    Math = Math; // Make the Math library visible in html

    constructor(
        protected activatedRoute: ActivatedRoute,
        private submissionService: SubmissionService,
        private tagService: TagService,
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

                if (res.body.details) {
                    this.submissionDetails = JSON.parse(res.body.details);
                }
                this.parseTestDetails();
                this.submissionId = this.submission.id;

                if (!this.isJudged(this.submission)) {
                    this.refreshInterval = setInterval(() => {
                        this.submissionService.find(this.submissionId).subscribe(res => {
                            this.submission = res.body;
                            if (res.body.details) {
                                this.submissionDetails = JSON.parse(res.body.details);
                            }
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

        this.tagService
            .query(true)
            .subscribe(
                (res: HttpResponse<ITag[]>) => (this.autocompleteTags = res.body),
                error => this.jhiAlertService.error(error.message, null, null)
            );

        this.submissionService
            .getTags(this.submissionId)
            .subscribe(
                (res: HttpResponse<ITag[]>) => (this.tags = res.body),
                error => this.jhiAlertService.error(error.message, null, null)
            );
    }

    parseTestDetails() {
        if (!this.submission || !this.submissionDetails || !this.submissionDetails['scoreSteps']) {
            return;
        }

        this.testDetails = [];
        for (let i = 1; ; i++) {
            const property = `Test${i}`;
            if (this.submissionDetails['scoreSteps'].hasOwnProperty(property)) {
                const val = this.submissionDetails['scoreSteps'][property];
                if (!val.output) {
                    val.output = '';
                }
                if (val.output.length > 70) {
                    val.output = val.output.substring(0, 70) + '...';
                }
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

    onTagsChanged() {
        clearTimeout(this.tagStatusTimeout);
        this.tagStatus = 1;

        this.submissionService.updateTags(this.submissionId, this.tags).subscribe(
            res => {
                this.tagStatus = 2;
                this.tagStatusTimeout = setTimeout(() => (this.tagStatus = 0), 3000);
            },
            err => (this.tagStatus = 3)
        );
    }
}
