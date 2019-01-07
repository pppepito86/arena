export interface ITagCollection {
    id?: number;
}

export class TagCollection implements ITagCollection {
    constructor(public id?: number) {}
}
