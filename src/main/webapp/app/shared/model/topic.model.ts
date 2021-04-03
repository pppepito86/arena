import { IUser } from '../../core';

export interface ITopic {
    id?: number;
}

export class Topic implements ITopic {
    constructor(public id?: number) {}
}
