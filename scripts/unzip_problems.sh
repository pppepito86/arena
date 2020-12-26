#!/bin/bash
set -e

pushd ~/arena/workdir/problems
for ((i=1259;i<=1300;i++));
do
    rm -f ~/arena/workdir/problems/$i/problem/task.pdf
    unzip -o ~/arena/workdir/problems/$i/problem.zip -d ~/arena/workdir/problems/$i/problem
    cp ~/arena/workdir/problems/$i/problem/*.pdf ~/arena/workdir/problems/$i/problem/task.pdf
done

popd

