export interface ICompetition {
    id?: number;
    label?: string;
    description?: string;
    order?: number;
    parentId?: number;
    competitionProblemId?: number;
}

export class Competition implements ICompetition {
    constructor(
        public id?: number,
        public label?: string,
        public description?: string,
        public order?: number,
        public parentId?: number,
        public competitionProblemId?: number
    ) {}
}
