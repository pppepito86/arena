import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { ISubmission } from 'app/shared/model/submission.model';
import { SubmissionService } from './submission.service';
import { CompetitionProblemService } from '../competition-problem';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';
import { JhiAlertService } from 'ng-jhipster';

@Component({
    styleUrls: ['./submission-detail.css'],
    selector: 'jhi-submission-detail',
    templateUrl: './submission-detail.component.html'
})
export class SubmissionDetailComponent implements OnInit {
    submission: ISubmission;
    refreshIntervalMs = 2000;
    refreshInterval: any;
    submissionId: number;
    competitionProblem: ICompetitionProblem;
    securityKey: string;
    testDetails: any;
    submissionDetails: any;
    tags = [];
    autocompleteTags = [
        'Basic / Рисуване на фигури',
        'Basic / Дати и време',
        'Basic / Мерни единици',
        'Basic / Масиви',
        'Basic / Цикли',
        'Basic / Низове (string)',
        'Basic / Char',
        'Префиксни суми (prefix array)',
        'Нестандартен вход / изход',
        'Сортиране с броене (counting sort)',
        'Броене с масив (counting)',
        'Разделяне на последователности',
        'Метод на показалките / 2-pointers',
        'Сортиране (sorting)',
        'Дълги числа (Big numbers)',
        'Двумерни и многомерни масиви',
        'Рекурсия (recursion)',
        'Търсене с връщане (backtracking)',
        'Сложна имплементация',
        'Игри',
        'Побитови операции',
        'Двоично търсене (binary search)',
        'Троично търсене (ternary search)',
        'Двоичо дърво (binary tree)',
        'Комбинаторни конфигурации и преброяване',
        'Аритметчни изрази',
        'Числа на Каталан',
        'Биномни коефициенти',
        'Редици на Грей',
        'Математика',
        'Математика / НОД - Алгоритъм на Евклид',
        'Математика / Прости числа',
        'Математика / Решето на Ератостен',
        'Математика / Степенуване',
        'Математика / Дроби',
        'Математика / lg(N) Степенуване',
        'Математика / Ератостен',
        'Математика / Бройни системи',
        'Математика / Разширен алгоритъм на Евклид',
        'Математика / Теория на числата',
        'Математика / Системи линейни уравнения',
        'Математика / Матрици',
        'Математика / Китайска теорема за остатъците',
        'STL',
        'STL / vector',
        'STL / bitset',
        'STL / Стек (queue)',
        'STL / Опашка (queue)',
        'STL / Приоритетна опашка (priority_queue)',
        'STL / Set',
        'STL / Map',
        'Greedy',
        'Bruteforce',
        'Precomputing',
        'Динамично програмиране (Dynamic programming)',
        'Двоично търсене по отговора (binary search)',
        'Разделяй и владей (divide and conquer)',
        'Meet in the middle',
        'Геометрия',
        'Геометрия / Ориентирани лица',
        'Геометрия / Изпъкнала обвивка',
        'Геометрия / Пресичане на прави',
        'Геометрия / Метяща права (sweep line)',
        'Геометрия / Ротации',
        'Графи',
        'Графи / Търсене в ширина (BFS)',
        'Графи / Търсене в дълбочина (DFS)',
        'Графи / Дейкстра (Dijkstra)',
        'Графи / Floyd Warshall',
        'Графи / Bellman-Ford',
        'Графи / Топологично сортиране',
        'Графи / Потоци (Flow)',
        'Графи / Планарни графи',
        'Графи / Хамилтонови цикли',
        'Графи / Ойлерови цикли',
        'Графи / Силна свързаност',
        'Графи / Matching / двойкосъчетания',
        'Графи / Покриващо дърво (Spanning tree)',
        'Advanced / Дърво на Фенуик (Fenwick, BIT)',
        'Advanced / Сегментно дърво',
        'Advanced / Интервално дърво',
        'Advanced / Rabin Karp (hashing)',
        'Advanced / KMP',
        'Advanced / Z функция',
        'Advanced / Aho Corasick',
        'Advanced / Suffix Tree',
        'Advanced / Kодове на Хафман',
        'Advanced / Формални граматики, автомати',
        'Advanced / Диаграми на Вороной',
        'Advanced / SQRT Decomposition',
        'Advanced / Непрекъснати задачи',
        'Advanced / Рандомизиран алгоритъм',
        'Advanced / Sparse Table',
        'Advanced / Union Find / Disjoint Set Union',
        'Advanced / Пирамида (heap)',
        'Advanced / FFT, NTT',
        'Advanced / 2-sat',
        'Advanced / Trie',
        'Advanced / Treap',
        'Advanced / RMQ',
        'Advanced / LCA',
        'Advanced / Heavy-light decompozition',
        'Динамично по профил (dynamic programming)',
        'Advanced / Хеширане (hashing)'
    ];

    constructor(
        protected activatedRoute: ActivatedRoute,
        private submissionService: SubmissionService,
        private competitionProblemService: CompetitionProblemService,
        protected jhiAlertService: JhiAlertService
    ) {}

    ngOnInit() {
        this.securityKey = this.activatedRoute.snapshot.queryParams['securityKey'];
        if (this.securityKey === undefined) {
            this.securityKey = '';
        }
        this.submissionId = this.activatedRoute.snapshot.params['id'];

        this.submissionService.find(this.submissionId, this.securityKey).subscribe(
            res => {
                this.submission = res.body;
                this.submissionDetails = JSON.parse(res.body.details);
                this.parseTestDetails();
                this.submissionId = this.submission.id;

                if (!this.isJudged(this.submission)) {
                    this.refreshInterval = setInterval(() => {
                        this.submissionService.find(this.submissionId).subscribe(res => {
                            this.submission = res.body;
                            this.submissionDetails = JSON.parse(res.body.details);
                            this.parseTestDetails();
                            if (this.isJudged(this.submission)) {
                                clearInterval(this.refreshInterval);
                            }
                        });
                    }, this.refreshIntervalMs);
                }
            },
            (res: HttpErrorResponse) => {
                this.submission = null;
                this.jhiAlertService.error(res.message, null, null);
            }
        );
    }

    parseTestDetails() {
        if (!this.submission || !this.submissionDetails || !this.submissionDetails['scoreSteps']) {
            return;
        }

        this.testDetails = [];
        for (let i = 1; ; i++) {
            let property = `Test${i}`;
            if (this.submissionDetails['scoreSteps'].hasOwnProperty(property)) {
                let val = this.submissionDetails['scoreSteps'][property];
                if (!val.output) {
                    val.output = '';
                }
                if (val.output.length > 70) {
                    val.output = val.output.substring(0, 70) + '...';
                }
                this.testDetails.push({
                    key: property,
                    value: val
                });
            } else {
                break;
            }
        }
    }

    isJudged(submission: ISubmission): boolean {
        return (
            submission.verdict != null && submission.verdict.toLowerCase() !== 'waiting' && submission.verdict.toLowerCase() !== 'judging'
        );
    }

    previousState() {
        window.history.back();
    }

    onTagsChanged() {
        console.log('change');
    }
}
