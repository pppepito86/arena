import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ITag } from 'app/shared/model/tag.model';
import { TagService } from './tag.service';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';
import { ISubmission } from '../../shared/model/submission.model';
import { zip } from 'rxjs';
import { getPointsColor } from 'app/shared/util/points-color';

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
    getPointsColor = getPointsColor;

    constructor(protected activatedRoute: ActivatedRoute, private tagService: TagService) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ tag }) => {
            this.tag = tag;
            this.loadData();
        });
    }

    loadData() {
        zip(this.tagService.findProblems(this.tag.id), this.tagService.findProblemsTaggedByUsers(this.tag.id)).subscribe(
            ([officialProblemsRes, userProblemsRes]: HttpResponse<ICompetitionProblem[]>[]) => {
                this.problems = officialProblemsRes.body;
                for (const problem of this.problems) {
                    if (problem.path) {
                        // Remove the root
                        problem.path.shift();
                    }
                    this.problemsWithOfficialTag.add(problem.id);
                }

                this.userTaggedProblems = [];
                for (const problem of userProblemsRes.body) {
                    if (problem.path) {
                        // Remove the root
                        problem.path.shift();
                    }
                    if (!this.problemsWithOfficialTag.has(problem.id)) {
                        this.userTaggedProblems.push(problem);
                    }
                }

                this.sortProblems(this.problems);
                this.sortProblems(this.userTaggedProblems);
            },
            error => this.handleError(error)
        );

        this.tagService.findSubmissions(this.tag.id).subscribe((res: HttpResponse<ICompetitionProblem[]>) => (this.submissions = res.body));
    }

    private handleError(error) {
        console.log(error);
    }

    previousState() {
        window.history.back();
    }

    getPathAtPos(p: ICompetitionProblem, pos: number) {
        if (!p.path) return '';
        while (pos < 0) pos += p.path.length;
        if (pos >= p.path.length) pos %= p.path.length;
        return p.path[pos];
    }

    compareAtPos(a: ICompetitionProblem, b: ICompetitionProblem, pos: number) {
        return this.getPathAtPos(a, pos).localeCompare(this.getPathAtPos(b, pos));
    }

    sortProblems(problems: ICompetitionProblem[]) {
        problems.sort(
            (a: ICompetitionProblem, b: ICompetitionProblem) =>
                this.compareAtPos(a, b, -1) || // group
                this.compareAtPos(a, b, 0) || // comp name
                this.compareAtPos(a, b, -2) // year
        );
    }
}
