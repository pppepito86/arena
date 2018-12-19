import { IProblem } from 'app/shared/model//problem.model';
import { ICompetition } from 'app/shared/model//competition.model';

export interface ICompetitionProblem {
    id?: number;
    order?: number;
    problems?: IProblem[];
    competitions?: ICompetition[];
}

export class CompetitionProblem implements ICompetitionProblem {
    constructor(public id?: number, public order?: number, public problems?: IProblem[], public competitions?: ICompetition[]) {}
}
