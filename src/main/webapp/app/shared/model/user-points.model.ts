import { IUser } from '../../core';

export interface IUserPoints {
    userId?: any;
    user?: IUser;
    points?: number;
    perProblemJson?: string;
    perProblem?: any;
    firstName?: string;
    lastName?: string;
}

export class UserPoints implements IUserPoints {
    constructor(public user?: IUser, public points?: number) {}
}
