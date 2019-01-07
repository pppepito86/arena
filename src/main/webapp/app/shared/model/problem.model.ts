export interface IProblem {
    id?: number;
    title?: string;
    directory?: string;
    version?: number;
    tagsId?: number;
}

export class Problem implements IProblem {
    constructor(public id?: number, public title?: string, public directory?: string, public version?: number, public tagsId?: number) {}
}
