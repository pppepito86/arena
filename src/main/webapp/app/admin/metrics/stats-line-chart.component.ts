import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { IPeriodDelta, StatsGranularity } from './metrics.model';

export interface IChartPoint {
    index: number;
    x: number;
    y: number;
    value: number;
    shortLabel: string;
    fullLabel: string;
    isInProgress: boolean;
    hitX: number;
    hitWidth: number;
}

export interface IYAxisTick {
    value: number;
    y: number;
    label: string;
}

export interface IXAxisTick {
    x: number;
    label: string;
    anchor: 'start' | 'middle' | 'end';
}

const MONTH_SHORT = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const MONTH_LONG = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December'
];
const DAY_SHORT = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

interface IParsedCalendarDate {
    year: number;
    month: number; // 0-based
    day: number;
    epochMs: number;
}

@Component({
    selector: 'jhi-stats-line-chart',
    templateUrl: './stats-line-chart.component.html',
    styleUrls: ['./stats-line-chart.component.scss']
})
export class StatsLineChartComponent implements OnChanges {
    private static nextGradientId = 1;

    @Input() title = '';
    @Input() periods: IPeriodDelta[] = [];
    @Input() color = '#3e8acc';
    @Input() unitLabel = '';
    @Input() granularity: StatsGranularity = 'day';

    readonly viewWidth = 640;
    readonly viewHeight = 230;
    readonly padTop = 18;
    readonly padRight = 20;
    readonly padBottom = 34;
    readonly padLeft = 48;

    readonly gradientId: string;

    viewMode: 'chart' | 'table' = 'chart';
    points: IChartPoint[] = [];
    yTicks: IYAxisTick[] = [];
    xTicks: IXAxisTick[] = [];
    completedLinePath = '';
    inProgressLinePath = '';
    areaPath = '';
    baselineY = 0;

    latestValue = 0;
    averageValue = 0;
    peakValue = 0;
    peakLabel = '';

    hoveredIndex: number | null = null;

    constructor() {
        this.gradientId = 'stats-chart-grad-' + StatsLineChartComponent.nextGradientId++;
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['periods'] || changes['granularity']) {
            this.hoveredIndex = null;
            this.buildChart();
        }
    }

    get hoveredPoint(): IChartPoint | null {
        if (this.hoveredIndex === null || this.hoveredIndex < 0 || this.hoveredIndex >= this.points.length) {
            return null;
        }
        return this.points[this.hoveredIndex];
    }

    get latestCaption(): string {
        return this.granularity === 'day' ? 'Today' : 'This mo';
    }

    get tooltipLeftPercent(): number {
        const pt = this.hoveredPoint;
        if (!pt) {
            return 0;
        }
        return (pt.x / this.viewWidth) * 100;
    }

    get tooltipTopPercent(): number {
        const pt = this.hoveredPoint;
        if (!pt) {
            return 0;
        }
        return (pt.y / this.viewHeight) * 100;
    }

    get tooltipAlignClass(): string {
        const pct = this.tooltipLeftPercent;
        if (pct < 22) {
            return 'align-left';
        }
        if (pct > 78) {
            return 'align-right';
        }
        return 'align-center';
    }

    setHover(index: number) {
        this.hoveredIndex = index;
    }

    clearHover() {
        this.hoveredIndex = null;
    }

    setViewMode(mode: 'chart' | 'table') {
        this.viewMode = mode;
        this.hoveredIndex = null;
    }

    private buildChart() {
        const plotWidth = this.viewWidth - this.padLeft - this.padRight;
        const plotHeight = this.viewHeight - this.padTop - this.padBottom;
        this.baselineY = this.padTop + plotHeight;

        if (!this.periods || this.periods.length === 0) {
            this.points = [];
            this.yTicks = [];
            this.xTicks = [];
            this.completedLinePath = '';
            this.inProgressLinePath = '';
            this.areaPath = '';
            this.latestValue = 0;
            this.averageValue = 0;
            this.peakValue = 0;
            this.peakLabel = '';
            return;
        }

        const parsed = this.periods.map((p, idx) => ({
            raw: p,
            originalIndex: idx,
            cal: this.parseCalendarDate(p.timestamp)
        }));

        // Sort chronologically ascending (oldest left -> newest right)
        parsed.sort((a, b) => {
            if (a.cal && b.cal) {
                return a.cal.epochMs - b.cal.epochMs;
            }
            // Backend returns newest first, so reverse original order as fallback
            return b.originalIndex - a.originalIndex;
        });

        const n = parsed.length;
        const maxVal = parsed.reduce((acc, item) => Math.max(acc, item.raw.delta || 0), 0);
        const { yMax, ticks } = this.computeNiceYScale(maxVal);

        this.yTicks = ticks.map(v => ({
            value: v,
            y: this.padTop + plotHeight - (v / yMax) * plotHeight,
            label: this.formatCompactNumber(v)
        }));

        const stepX = n > 1 ? plotWidth / (n - 1) : 0;

        this.points = parsed.map((item, i) => {
            const value = item.raw.delta || 0;
            const x = n === 1 ? this.padLeft + plotWidth / 2 : this.padLeft + i * stepX;
            const y = this.padTop + plotHeight - (value / yMax) * plotHeight;
            const isInProgress = i === n - 1;
            const shortLabel = this.formatShortLabel(item.raw.name, item.cal, this.granularity);
            const fullLabel = this.formatFullLabel(item.raw.name, item.cal, this.granularity, isInProgress);

            const leftEdge = n === 1 ? this.padLeft : i === 0 ? this.padLeft : x - stepX / 2;
            const rightEdge = n === 1 ? this.padLeft + plotWidth : i === n - 1 ? this.padLeft + plotWidth : x + stepX / 2;

            return {
                index: i,
                x,
                y,
                value,
                shortLabel,
                fullLabel,
                isInProgress,
                hitX: leftEdge,
                hitWidth: Math.max(rightEdge - leftEdge, 1)
            };
        });

        // Build solid line for completed periods and dashed segment for the final in-progress period
        if (n >= 2) {
            const completed = this.points.slice(0, n - 1);
            this.completedLinePath =
                completed.length >= 2
                    ? completed.map((pt, i) => `${i === 0 ? 'M' : 'L'} ${pt.x.toFixed(2)} ${pt.y.toFixed(2)}`).join(' ')
                    : '';
            const prev = this.points[n - 2];
            const last = this.points[n - 1];
            this.inProgressLinePath = `M ${prev.x.toFixed(2)} ${prev.y.toFixed(2)} L ${last.x.toFixed(2)} ${last.y.toFixed(2)}`;

            const first = this.points[0];
            const lineSegments = this.points.map((pt, i) => `${i === 0 ? 'M' : 'L'} ${pt.x.toFixed(2)} ${pt.y.toFixed(2)}`).join(' ');
            this.areaPath = `${lineSegments} L ${last.x.toFixed(2)} ${this.baselineY.toFixed(2)} L ${first.x.toFixed(
                2
            )} ${this.baselineY.toFixed(2)} Z`;
        } else {
            this.completedLinePath = '';
            this.inProgressLinePath = '';
            this.areaPath = '';
        }

        // Compute evenly spaced X-axis ticks
        this.xTicks = this.computeXTicks(this.points);

        // Compute summary KPIs
        this.latestValue = this.points[n - 1].value;
        const completedForAvg = n > 1 ? this.points.slice(0, n - 1) : this.points;
        const sumCompleted = completedForAvg.reduce((acc, pt) => acc + pt.value, 0);
        this.averageValue = Math.round(sumCompleted / completedForAvg.length);

        let peak = this.points[0];
        for (const pt of this.points) {
            if (pt.value >= peak.value) {
                peak = pt;
            }
        }
        this.peakValue = peak.value;
        this.peakLabel = peak.shortLabel;
    }

    private computeXTicks(points: IChartPoint[]): IXAxisTick[] {
        const n = points.length;
        if (n === 0) {
            return [];
        }
        if (n === 1) {
            return [{ x: points[0].x, label: points[0].shortLabel, anchor: 'middle' }];
        }

        const maxLabels = 7;
        const step = Math.max(1, Math.ceil((n - 1) / (maxLabels - 1)));
        const indices: number[] = [];
        for (let i = 0; i < n; i += step) {
            indices.push(i);
        }
        const lastIdx = indices[indices.length - 1];
        if (lastIdx !== n - 1) {
            if (n - 1 - lastIdx >= Math.max(1, Math.floor(step * 0.6))) {
                indices.push(n - 1);
            } else {
                indices[indices.length - 1] = n - 1;
            }
        }

        return indices.map((idx, pos) => {
            let anchor: 'start' | 'middle' | 'end' = 'middle';
            if (pos === 0 && idx === 0) {
                anchor = 'start';
            } else if (pos === indices.length - 1 && idx === n - 1) {
                anchor = 'end';
            }
            return {
                x: points[idx].x,
                label: points[idx].shortLabel,
                anchor
            };
        });
    }

    private computeNiceYScale(maxVal: number): { yMax: number; ticks: number[] } {
        if (maxVal <= 0) {
            return { yMax: 4, ticks: [0, 1, 2, 3, 4] };
        }

        const targetIntervals = 4;
        const rawStep = maxVal / targetIntervals;
        const magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        const residual = rawStep / magnitude;

        let niceMultiplier: number;
        if (residual <= 1) {
            niceMultiplier = 1;
        } else if (residual <= 2) {
            niceMultiplier = 2;
        } else if (residual <= 2.5) {
            niceMultiplier = 2.5;
        } else if (residual <= 5) {
            niceMultiplier = 5;
        } else {
            niceMultiplier = 10;
        }

        let step = niceMultiplier * magnitude;
        if (step < 1) {
            step = 1;
        }
        // Keep step an integer for discrete counts
        if (!Number.isInteger(step)) {
            step = Math.ceil(step);
        }

        const yMax = Math.ceil(maxVal / step) * step;
        const ticks: number[] = [];
        for (let v = 0; v <= yMax; v += step) {
            ticks.push(v);
        }
        return { yMax, ticks };
    }

    private parseCalendarDate(timestamp: string | number | null | undefined): IParsedCalendarDate | null {
        if (timestamp === null || timestamp === undefined) {
            return null;
        }
        if (typeof timestamp === 'string') {
            // Preserve the server's calendar date directly from ISO-8601 prefix so browser timezone never shifts midnight backwards
            const isoMatch = /^(\d{4})-(\d{2})-(\d{2})/.exec(timestamp);
            if (isoMatch) {
                const year = parseInt(isoMatch[1], 10);
                const month = parseInt(isoMatch[2], 10) - 1;
                const day = parseInt(isoMatch[3], 10);
                return { year, month, day, epochMs: Date.UTC(year, month, day) };
            }
            const ms = Date.parse(timestamp);
            if (!isNaN(ms)) {
                const d = new Date(ms);
                return { year: d.getUTCFullYear(), month: d.getUTCMonth(), day: d.getUTCDate(), epochMs: ms };
            }
            return null;
        }
        if (typeof timestamp === 'number') {
            const ms = timestamp < 1e12 ? timestamp * 1000 : timestamp;
            const d = new Date(ms);
            return { year: d.getUTCFullYear(), month: d.getUTCMonth(), day: d.getUTCDate(), epochMs: ms };
        }
        return null;
    }

    private formatShortLabel(rawName: string, cal: IParsedCalendarDate | null, granularity: StatsGranularity): string {
        if (cal) {
            const mon = MONTH_SHORT[cal.month] || '';
            if (granularity === 'day') {
                return `${cal.day} ${mon}`;
            }
            const shortYear = String(cal.year).slice(-2);
            return `${mon} '${shortYear}`;
        }
        return rawName || '';
    }

    private formatFullLabel(
        rawName: string,
        cal: IParsedCalendarDate | null,
        granularity: StatsGranularity,
        isInProgress: boolean
    ): string {
        let base = rawName || '';
        if (cal) {
            if (granularity === 'day') {
                const utcDate = new Date(Date.UTC(cal.year, cal.month, cal.day));
                const weekday = DAY_SHORT[utcDate.getUTCDay()];
                base = `${weekday}, ${cal.day} ${MONTH_SHORT[cal.month]} ${cal.year}`;
            } else {
                base = `${MONTH_LONG[cal.month]} ${cal.year}`;
            }
        }
        if (isInProgress) {
            return base + (granularity === 'day' ? ' (today)' : ' (in progress)');
        }
        return base;
    }

    private formatCompactNumber(n: number): string {
        if (n >= 1000000) {
            const v = (n / 1000000).toFixed(1).replace(/\.0$/, '');
            return `${v}M`;
        }
        if (n >= 10000) {
            return `${Math.round(n / 1000)}k`;
        }
        if (n >= 1000) {
            const v = (n / 1000).toFixed(1).replace(/\.0$/, '');
            return `${v}k`;
        }
        return String(n);
    }
}
