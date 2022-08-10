import { IUser } from '../../core';

export interface IUserPoints {
    userId?: any;
    user?: IUser;
    points?: number;
    perProblemJson?: string;
    perProblem?: any;
}

export class UserPoints implements IUserPoints {
    constructor(public user?: IUser, public points?: number) {}
}
