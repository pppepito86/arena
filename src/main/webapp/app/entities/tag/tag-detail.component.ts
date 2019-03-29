import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ITag } from 'app/shared/model/tag.model';
import { TagService } from './tag.service';
import { IProblem } from '../../shared/model/problem.model';
import { HttpResponse } from '@angular/common/http';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';
import { ISubmission } from '../../shared/model/submission.model';

@Component({
    selector: 'jhi-tag-detail',
    templateUrl: './tag-detail.component.html'
})
export class TagDetailComponent implements OnInit {
    tag: ITag;
    problems: ICompetitionProblem[];
    problemsWithOfficialTag: Set<number> = new Set<number>();
    userTaggedProblems: ICompetitionProblem[];
    submissions: ISubmission[];

    constructor(protected activatedRoute: ActivatedRoute, private tagService: TagService) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ tag }) => {
            this.tag = tag;
            this.loadData();
        });
    }

    loadData() {
        this.tagService.findProblems(this.tag.id).subscribe((res: HttpResponse<ICompetitionProblem[]>) => {
            this.problems = res.body;
            for (let problem of this.problems) {
                this.problemsWithOfficialTag.add(problem.id);
            }

            // Get the problems tagged by users
            this.tagService.findProblemsTaggedByUsers(this.tag.id).subscribe((res: HttpResponse<ICompetitionProblem[]>) => {
                this.userTaggedProblems = [];
                for (let problem of res.body) {
                    if (!this.problemsWithOfficialTag.has(problem.id)) {
                        this.userTaggedProblems.push(problem);
                    }
                }
            });
        });

        this.tagService.findSubmissions(this.tag.id).subscribe((res: HttpResponse<ICompetitionProblem[]>) => (this.submissions = res.body));
    }

    previousState() {
        window.history.back();
    }
}
