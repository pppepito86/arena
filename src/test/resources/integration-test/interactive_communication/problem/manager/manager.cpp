// Two-process interactive manager (piper protocol).
//
// Own stdin/stdout are the test's .in file and the graded output (wired up
// by SandboxExecutor around the whole "piper" invocation). argv[1]/argv[2]
// are raw fds piped to/from the contestant's process (piper.cpp forks it
// with its stdin/stdout dup2'd onto the other ends of these same pipes).
//
// Protocol: manager picks a secret number (read from its own stdin). The
// contestant repeatedly prints a guess; manager replies -1/0/1 (low/equal/
// high) over the pipe. Once the contestant hits 0, or after too many
// guesses, or if the contestant stops responding, the manager prints the
// final verdict (1 = solved, 0 = not) to its own stdout.
#include <cstdio>
#include <cstdlib>

int main(int argc, char** argv) {
    int readFd = atoi(argv[1]);
    int writeFd = atoi(argv[2]);
    FILE* in = fdopen(readFd, "r");
    FILE* out = fdopen(writeFd, "w");

    int secret;
    if (scanf("%d", &secret) != 1) {
        printf("0\n");
        return 0;
    }

    bool solved = false;
    for (int query = 0; query < 20; query++) {
        int guess;
        if (fscanf(in, "%d", &guess) != 1) break;

        int reply = (guess < secret) ? -1 : (guess > secret) ? 1 : 0;
        fprintf(out, "%d\n", reply);
        fflush(out);

        if (reply == 0) {
            solved = true;
            break;
        }
    }

    printf("%d\n", solved ? 1 : 0);
    return 0;
}
