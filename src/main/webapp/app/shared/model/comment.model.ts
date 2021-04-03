import { IUser } from '../../core';
import { Moment } from 'moment';

export interface IComment {
    id?: number;
    topicId?: number;
    author?: IUser;
    postedDate?: Moment;
    content?: string;
}

export class Comment implements IComment {
    constructor(public id?: number, public topicId?: number, public authorId?: number, public postedDate?: Moment, public text?: string) {}
}
