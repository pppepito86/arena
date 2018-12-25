export interface ICompetitionProblem {
    id?: number;
    order?: number;
    problemId?: number;
    competitionId?: number;
    title?: string;
}

export class CompetitionProblem implements ICompetitionProblem {
    constructor(
        public id?: number,
        public order?: number,
        public problemId?: number,
        public competitionId?: number,
        public titile?: string
    ) {}
}
