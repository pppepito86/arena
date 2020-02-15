pushd ~/workspace/workdir/problems

find . -name tests | while read test_dir; do
    echo "Deleting '$test_dir'"
    rm -r $test_dir
done

popd

