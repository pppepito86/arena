export interface ITag {
    id?: number;
    title?: string;
    keywords?: string;
    visible?: boolean;
    popularity?: number;
}

export class Tag implements ITag {
    constructor(public id?: number, public title?: string, public keywords?: string, public visible?: boolean, public popularity?: number) {
        this.visible = this.visible || false;
    }
}
