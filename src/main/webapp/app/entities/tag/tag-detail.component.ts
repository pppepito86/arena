import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ITag } from 'app/shared/model/tag.model';
import { TagService } from './tag.service';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';
import { ISubmission } from '../../shared/model/submission.model';
import { CompetitionService } from '../competition';
import { ICompetition } from '../../shared/model/competition.model';

@Component({
    selector: 'jhi-tag-detail',
    templateUrl: './tag-detail.component.html',
    styleUrls: ['./tag-detail.component.css']
})
export class TagDetailComponent implements OnInit {
    tag: ITag;
    problems: ICompetitionProblem[];
    problemsWithOfficialTag: Set<number> = new Set<number>();
    userTaggedProblems: ICompetitionProblem[];
    submissions: ISubmission[];
    competitionIdToPath: any = {};

    constructor(protected activatedRoute: ActivatedRoute, private tagService: TagService, private competitionService: CompetitionService) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ tag }) => {
            this.tag = tag;
            this.loadData();
        });
    }

    loadData() {
        this.tagService.findProblems(this.tag.id).subscribe(
            (res: HttpResponse<ICompetitionProblem[]>) => {
                this.problems = res.body;
                for (const problem of this.problems) {
                    this.problemsWithOfficialTag.add(problem.id);
                }

                // Get the problems tagged by users
                this.tagService.findProblemsTaggedByUsers(this.tag.id).subscribe(
                    (res: HttpResponse<ICompetitionProblem[]>) => {
                        this.userTaggedProblems = [];
                        for (const problem of res.body) {
                            if (!this.problemsWithOfficialTag.has(problem.id)) {
                                this.userTaggedProblems.push(problem);
                            }
                        }
                    },
                    error => this.handleError(error),
                    () => this.resolveCompetitionNames()
                );
            },
            error => this.handleError(error)
        );

        this.tagService.findSubmissions(this.tag.id).subscribe((res: HttpResponse<ICompetitionProblem[]>) => (this.submissions = res.body));
    }

    private resolveCompetitionNames() {
        for (const problem of this.problems) {
            this.resolveCompetitionName(problem.competitionId);
        }
        for (const problem of this.userTaggedProblems) {
            this.resolveCompetitionName(problem.competitionId);
        }
    }

    private resolveCompetitionName(competitionId: number) {
        if (this.competitionIdToPath[competitionId] === undefined) {
            this.competitionService.findPath(competitionId).subscribe(
                (res: HttpResponse<ICompetition[]>) => {
                    this.competitionIdToPath[competitionId] = res.body;
                    this.competitionIdToPath[competitionId].shift();
                },
                (res: HttpErrorResponse) => this.handleError(res.message)
            );
        }
    }

    private handleError(error) {
        console.log(error);
    }

    previousState() {
        window.history.back();
    }
}
