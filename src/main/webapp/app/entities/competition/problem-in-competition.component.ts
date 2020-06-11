import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse, HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

import { IProblem } from 'app/shared/model/problem.model';
import { CompetitionService } from './competition.service';
import { ICompetition } from '../../shared/model/competition.model';
import { JhiAlertService } from 'ng-jhipster';
import { Title } from '@angular/platform-browser';
import { ISubmission } from '../../shared/model/submission.model';
import { TagService } from '../tag';
import { ProblemService } from '../problem';
import { ITag } from '../../shared/model/tag.model';
import { CompetitionProblemService } from '../competition-problem';
import { AccountService } from '../../core';

@Component({
    selector: 'jhi-problem-in-competition',
    templateUrl: './problem-in-competition.component.html'
})
export class ProblemInCompetitionComponent implements OnInit {
    problem: IProblem;
    currentAccount: any;
    competitionId: number;
    competitionProblemId: number;
    isSubmitting: boolean;
    solution = '';
    tags = [];
    autocompleteTags: ITag[] = [];
    tagStatus = 0;
    tagStatusTimeout;

    constructor(
        private router: Router,
        protected activatedRoute: ActivatedRoute,
        protected competitionService: CompetitionService,
        protected jhiAlertService: JhiAlertService,
        protected accountService: AccountService,
        protected tagService: TagService,
        protected problemService: ProblemService,
        private titleService: Title,
        private competitionProblemService: CompetitionProblemService
    ) {}

    ngOnInit() {
        const params = this.activatedRoute.snapshot.paramMap;
        this.competitionId = Number(params.get('id'));
        this.competitionProblemId = Number(params.get('compProb'));

        this.competitionService.findProblem(this.competitionId, this.competitionProblemId).subscribe(
            (res: HttpResponse<IProblem>) => {
                this.problem = res.body;
                this.titleService.setTitle(this.problem.title);
                this.loadTags();
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );

        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });

        this.tagService
            .query(true)
            .subscribe(
                (res: HttpResponse<ITag[]>) => (this.autocompleteTags = res.body),
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    private loadTags() {
        this.problemService
            .getTags(this.problem.id)
            .subscribe((res: HttpResponse<ITag[]>) => (this.tags = res.body), (res: HttpErrorResponse) => this.onError(res.message));
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }

    submit() {
        this.isSubmitting = true;
        this.competitionService.submitSolution(this.competitionId, this.competitionProblemId, this.solution).subscribe(
            (res: HttpResponse<ISubmission>) => {
                this.isSubmitting = false;
                const submission = res.body;
                this.router.navigate(['submission', submission.id, 'view'], {
                    queryParams: { securityKey: submission.securityKey }
                });
            },
            (res: HttpErrorResponse) => {
                this.isSubmitting = false;
                this.onError(res.message);
            }
        );
    }

    onTagsChanged() {
        clearTimeout(this.tagStatusTimeout);
        this.tagStatus = 1;
        this.problemService.updateTags(this.problem.id, this.tags).subscribe(
            res => {
                this.tagStatus = 2;
                this.tagStatusTimeout = setTimeout(() => (this.tagStatus = 0), 3000);
            },
            err => (this.tagStatus = 3)
        );
    }

    onAutoSetTimeLimit() {
        if (confirm('Are you sure? This will change the time limit.')) {
            this.competitionProblemService
                .autoSetTimeLimit(this.competitionProblemId)
                .subscribe(value => console.log('OK'), (res: HttpErrorResponse) => this.onError(res.message));
        }
    }

    onSubmitAuthorSolution() {
        this.competitionProblemService
            .submitAuthorSolution(this.competitionProblemId)
            .subscribe(value => console.log('OK'), (res: HttpErrorResponse) => this.onError(res.message));
    }
}
