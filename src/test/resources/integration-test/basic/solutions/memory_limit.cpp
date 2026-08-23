#include <iostream>
#include <vector>
using namespace std;

int main() {
    int a, b;
    if (cin >> a >> b) {
        // Problem's memory limit is 64 MB; allocate and touch ~100 MB so the
        // sandbox measures actual usage past the limit (not just reserved
        // virtual memory), triggering an ML verdict.
        const size_t count = 100LL * 1024 * 1024 / sizeof(int);
        vector<int> big(count, 1);
        cout << a + b + big[count - 1] << endl;
    }
    return 0;
}
