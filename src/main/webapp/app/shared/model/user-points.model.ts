import { IUser } from '../../core';

export interface IUserPoints {
    user?: IUser;
    points?: number;
}

export class UserPoints implements IUserPoints {
    constructor(public user?: IUser, public points?: number) {}
}
