import { Moment } from 'moment';

export interface ISubmission {
    id?: number;
    file?: string;
    verdict?: string;
    details?: string;
    points?: number;
    timeInMillis?: number;
    memoryInBytes?: number;
    uploadDate?: Moment;
    securityKey?: string;
    userId?: number;
    competitionProblemId?: number;
    code?: string;
    userFirstName?: string;
    userLastName?: string;
    problemName?: string;
    competitionName?: string;
    competitionId?: number;
}

export class Submission implements ISubmission {
    constructor(
        public id?: number,
        public file?: string,
        public verdict?: string,
        public details?: string,
        public points?: number,
        public timeInMillis?: number,
        public memoryInBytes?: number,
        public uploadDate?: Moment,
        public securityKey?: string,
        public userId?: number,
        public competitionProblemId?: number,
        public code?: string,
        public userFirstName?: string,
        public userLastName?: string,
        public problemName?: string,
        public competitionName?: string,
        public competitionId?: number
    ) {}
}
