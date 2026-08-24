// In-process "library" grader (compiled together with the contestant's
// submitted .h file into a single binary via HCompileStep -- no separate
// process/pipe is involved; the contestant calls ask() as a plain function).
#include <iostream>
#include <vector>
using namespace std;

static vector<int> arr;

int ask(int i) {
    return arr[i];
}

#include "solution.h"

int main() {
    int n;
    cin >> n;
    arr.resize(n);
    for (int i = 0; i < n; i++) cin >> arr[i];

    cout << solve(n) << endl;
    return 0;
}
