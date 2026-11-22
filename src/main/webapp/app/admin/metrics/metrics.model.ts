export interface IPeriodDelta {
    name: string;
    timestamp: string;
    delta: number;
}

export interface IStatsMetric {
    total: number;
    periods: IPeriodDelta[][];
}

export interface IStatsProblems {
    total: number;
    submitable: number;
}

export interface IQueuedSubmission {
    id: number;
    author?: string;
    problemName?: string;
    verdict?: string;
}

export interface IStatsQueue {
    submissionIds: number[];
    submissions?: IQueuedSubmission[];
    ageMins: number;
}

export interface IStats {
    users?: IStatsMetric;
    activeUsers?: IStatsMetric;
    submissions?: IStatsMetric;
    problems?: IStatsProblems;
    queue?: IStatsQueue;
    badSubmissionIds?: number[];
}

export type StatsGranularity = 'day' | 'month';
