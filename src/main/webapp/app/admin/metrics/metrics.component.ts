import { Component, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { JhiMetricsMonitoringModalComponent } from './metrics-modal.component';
import { JhiMetricsService } from './metrics.service';
import { IPeriodDelta, IStats, IStatsMetric, StatsGranularity } from './metrics.model';

export interface IStatsChartConfig {
    title: string;
    periods: IPeriodDelta[];
    color: string;
    unitLabel: string;
    granularity: StatsGranularity;
}

@Component({
    selector: 'jhi-metrics',
    templateUrl: './metrics.component.html',
    styleUrls: ['./metrics.component.scss']
})
export class JhiMetricsMonitoringComponent implements OnInit {
    metrics: any = {};
    stats: IStats = null;
    cachesStats: any = {};
    servicesStats: any = {};
    updatingMetrics = true;
    updatingStats = true;
    JCACHE_KEY: string;

    statsCharts: IStatsChartConfig[] = [];
    todaySubmissions: number | null = null;
    todayActiveUsers: number | null = null;

    constructor(private modalService: NgbModal, private metricsService: JhiMetricsService) {
        this.JCACHE_KEY = 'jcache.statistics';
    }

    ngOnInit() {
        this.refresh();
    }

    refresh() {
        this.updatingMetrics = true;
        this.updatingStats = true;
        this.metricsService.getMetrics().subscribe(metrics => {
            this.metrics = metrics;
            this.updatingMetrics = false;
            this.servicesStats = {};
            this.cachesStats = {};
            Object.keys(metrics.timers).forEach(key => {
                const value = metrics.timers[key];
                if (key.includes('web.rest') || key.includes('service')) {
                    this.servicesStats[key] = value;
                }
            });
            Object.keys(metrics.gauges).forEach(key => {
                if (key.includes('jcache.statistics')) {
                    const value = metrics.gauges[key].value;
                    // remove gets or puts
                    const index = key.lastIndexOf('.');
                    const newKey = key.substr(0, index);

                    // Keep the name of the domain
                    this.cachesStats[newKey] = {
                        name: this.JCACHE_KEY.length,
                        value
                    };
                }
            });
        });
        this.metricsService.getStats().subscribe(
            stats => {
                this.stats = stats;
                this.buildStatsCharts(stats);
                this.updatingStats = false;
            },
            () => {
                this.updatingStats = false;
            }
        );
    }

    refreshThreadDumpData() {
        this.metricsService.threadDump().subscribe(data => {
            const modalRef = this.modalService.open(JhiMetricsMonitoringModalComponent, { size: 'lg' });
            modalRef.componentInstance.threadDump = data.threads;
            modalRef.result.then(
                result => {
                    // Left blank intentionally, nothing to do here
                },
                reason => {
                    // Left blank intentionally, nothing to do here
                }
            );
        });
    }

    filterNaN(input) {
        if (isNaN(input)) {
            return 0;
        }
        return input;
    }

    sendMail() {
        this.metricsService.sendMail().subscribe();
    }

    trackByChartTitle(index: number, chart: IStatsChartConfig): string {
        return chart.title;
    }

    get badProblemsCount(): number {
        if (!this.stats || !this.stats.problems) {
            return 0;
        }
        return this.stats.problems.total - this.stats.problems.submitable;
    }

    get queueCardAccentClass(): string {
        if (!this.stats || !this.stats.queue) {
            return 'kpi-success';
        }
        const count = this.stats.queue.submissionIds ? this.stats.queue.submissionIds.length : 0;
        if (count === 0) {
            return 'kpi-success';
        }
        return this.stats.queue.ageMins >= 5 ? 'kpi-danger' : 'kpi-warning';
    }

    private buildStatsCharts(stats: IStats) {
        const charts: IStatsChartConfig[] = [];
        this.todaySubmissions = null;
        this.todayActiveUsers = null;

        if (!stats) {
            this.statsCharts = charts;
            return;
        }

        this.appendMetricCharts(charts, stats.submissions, 'Submissions', 'submissions', '#3e8acc', '#2b6cb0', daily => {
            this.todaySubmissions = this.getLatestDelta(daily);
        });

        this.appendMetricCharts(charts, stats.activeUsers, 'Active Users', 'active users', '#28a745', '#1e7e34', daily => {
            this.todayActiveUsers = this.getLatestDelta(daily);
        });

        this.appendMetricCharts(charts, stats.users, 'New Users', 'new users', '#17a2b8', '#117a8b');

        this.statsCharts = charts;
    }

    private appendMetricCharts(
        target: IStatsChartConfig[],
        metric: IStatsMetric | undefined,
        baseTitle: string,
        unitLabel: string,
        dailyColor: string,
        monthlyColor: string,
        onDaily?: (periods: IPeriodDelta[]) => void
    ) {
        if (!metric || !metric.periods || metric.periods.length === 0) {
            return;
        }

        const classified = metric.periods
            .filter(series => series && series.length > 0)
            .map(series => ({
                series,
                granularity: this.detectGranularity(series)
            }));

        // Always order Daily before Monthly for consistent side-by-side comparison
        classified.sort((a, b) => {
            if (a.granularity === b.granularity) {
                return 0;
            }
            return a.granularity === 'day' ? -1 : 1;
        });

        for (const item of classified) {
            const isDaily = item.granularity === 'day';
            if (isDaily && onDaily) {
                onDaily(item.series);
            }
            target.push({
                title: isDaily ? `${baseTitle} (Daily)` : `${baseTitle} (Monthly)`,
                periods: item.series,
                color: isDaily ? dailyColor : monthlyColor,
                unitLabel,
                granularity: item.granularity
            });
        }
    }

    private detectGranularity(periods: IPeriodDelta[]): StatsGranularity {
        if (periods.length >= 2) {
            const t0 = Date.parse(periods[0].timestamp);
            const t1 = Date.parse(periods[1].timestamp);
            if (!isNaN(t0) && !isNaN(t1)) {
                return Math.abs(t0 - t1) <= 2 * 24 * 3600 * 1000 ? 'day' : 'month';
            }
        }
        if (periods.length > 0 && /^\d+\s/.test(periods[0].name || '')) {
            return 'day';
        }
        return 'month';
    }

    private getLatestDelta(periods: IPeriodDelta[]): number | null {
        if (!periods || periods.length === 0) {
            return null;
        }
        let latest = periods[0];
        let latestMs = Date.parse(latest.timestamp);
        for (let i = 1; i < periods.length; i++) {
            const ms = Date.parse(periods[i].timestamp);
            if (!isNaN(ms) && (isNaN(latestMs) || ms > latestMs)) {
                latest = periods[i];
                latestMs = ms;
            }
        }
        return latest.delta;
    }
}
