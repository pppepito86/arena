int solve(int n) {
    int sum = 0;
    for (int i = 0; i < n; i++) sum += ask(i);
    return sum;
}
