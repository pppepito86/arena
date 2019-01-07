export interface ITagCollectionTag {
    id?: number;
    collectionId?: number;
    tagId?: number;
}

export class TagCollectionTag implements ITagCollectionTag {
    constructor(public id?: number, public collectionId?: number, public tagId?: number) {}
}
