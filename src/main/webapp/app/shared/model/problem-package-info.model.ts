export interface IProblemPackageInfo {
    hasPackage?: boolean;
    propertiesPath?: string;
    timeMs?: number;
    memoryMb?: number;
    extension?: string;
    allowedExtensions?: string[];
    statementPath?: string;
    authorSolutionPath?: string;
    checkerPath?: string;
    graderPath?: string;
    managerPath?: string;
    testCount?: number;
    groupCount?: number;
    testsCleanedUp?: boolean;
    warnings?: string[];
}
