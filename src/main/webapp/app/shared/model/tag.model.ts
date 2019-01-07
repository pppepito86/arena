export interface ITag {
    id?: number;
    title?: string;
    keywords?: string;
    visible?: boolean;
}

export class Tag implements ITag {
    constructor(public id?: number, public title?: string, public keywords?: string, public visible?: boolean) {
        this.visible = this.visible || false;
    }
}
