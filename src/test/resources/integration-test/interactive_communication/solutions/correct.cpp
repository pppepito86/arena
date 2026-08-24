#include <iostream>
using namespace std;

int main() {
    int lo = 1, hi = 100;
    while (lo <= hi) {
        int mid = (lo + hi) / 2;
        cout << mid << endl;

        int reply;
        if (!(cin >> reply)) break;
        if (reply == 0) break;
        else if (reply < 0) lo = mid + 1;
        else hi = mid - 1;
    }
    return 0;
}
