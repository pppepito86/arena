/** One field an admin changed, as stored in the details column. */
export interface IFieldChange {
    field: string;
    old: string;
    new: string;
}

/** The parsed contents of AdminAuditLog.details. */
export interface IAuditDetails {
    note?: string;
    changes?: IFieldChange[];
}

export class AdminAuditLog {
    constructor(
        public id?: number,
        public adminId?: number,
        public adminLogin?: string,
        public action?: string,
        public entityType?: string,
        public entityId?: number,
        public entityLabel?: string,
        public details?: string,
        public performedDate?: string
    ) {}
}
