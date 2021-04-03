import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

import { IProblem } from 'app/shared/model/problem.model';
import { CompetitionService } from './competition.service';
import { JhiAlertService } from 'ng-jhipster';
import { Title } from '@angular/platform-browser';
import { ISubmission } from '../../shared/model/submission.model';
import { ProblemService } from '../problem';
import { CompetitionProblemService } from '../competition-problem';
import { AccountService } from '../../core';
import { ITopic } from 'app/shared/model/topic.model';
import { IComment } from 'app/shared/model/comment.model';

@Component({
    selector: 'jhi-discussion',
    styleUrls: ['././discussion.component.css'],
    templateUrl: './discussion.component.html'
})
export class DiscussionComponent implements OnInit {
    problem: IProblem;
    currentAccount: any;
    competitionId: number;
    competitionProblemId: number;
    newComment = '';
    isSubmitting: boolean;
    topicId: number;
    comments: IComment[];

    constructor(
        private router: Router,
        protected activatedRoute: ActivatedRoute,
        protected competitionService: CompetitionService,
        protected jhiAlertService: JhiAlertService,
        protected accountService: AccountService,
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
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );

        this.competitionService.getTopic(this.competitionProblemId).subscribe(
            (res: HttpResponse<ITopic>) => {
                this.topicId = res.body.id;
                this.loadComments();
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );

        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }

    loadComments() {
        this.competitionService.getComments(this.topicId).subscribe(
            (res: HttpResponse<IComment[]>) => {
                this.comments = res.body;
                this.comments.reverse();
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    onPostComment() {
        console.log(this.newComment);
        this.competitionService.postComment(this.topicId, this.newComment).subscribe(
            (res: HttpResponse<ISubmission>) => {
                this.isSubmitting = false;
                this.loadComments();
            },
            (res: HttpErrorResponse) => {
                this.isSubmitting = false;
                this.onError(res.message);
            }
        );
    }
}
