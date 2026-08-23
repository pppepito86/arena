/* the checker has three arguments:
   - first - name of the input file
   - second - name of the correct output file
   - third - name of the contestant output file */
#include <bits/stdc++.h>

using namespace std;

int correct(){
    cout<<"1\n";
    cerr<<"translate:success\n";
    return 0;
}
int checker_broken(){
    cout<<"1\n";
    cerr<<"Checker is broken\n";
    return 0;
}
int wrong_answer(){
    cout<<"0\n";
    cerr<<"wrong\n";
    return 0;
}
int invalid_error(){
    cout<<"0\n";
    cerr<<"Output is invalid\n";
    return 0;
}


int main (int argc, char** argv) {
    ifstream in(argv[1]);
    ifstream generated_out(argv[2]);
    ifstream contestant_out(argv[3]);

    // Here we assume that the generated_out will be the same as the input.
    // The point is to test that the generated_out is correctly passed.
    int k;
    generated_out >> k;

    int multiple;
    contestant_out >> multiple;

    if (multiple % k == 0) correct();
    else wrong_answer();
}