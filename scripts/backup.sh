curr_date=`date '+%Y-%m-%d'`
pushd ~/arena/workdir/problems
aws s3 sync . s3://backup.arena.olimpiici.com/problems_${curr_date}/problems --exclude "*" --include "*.zip"
popd

pushd ~/arena
mysqldump -u root arena_dev > database.sql      # password required
file=backup_${curr_date}.zip
zip -r $file database.sql workdir/submissions
aws s3 cp $file s3://backup.arena.olimpiici.com/
rm database.sql
rm $file
popd

