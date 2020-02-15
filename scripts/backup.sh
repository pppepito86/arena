pushd ~/workspace/workdir/problems
aws s3 sync . s3://backup.arena.olimpiici.com/problems --exclude "*" --include "*.zip"
popd

pushd ~/workspace
mysqldump -u root -ppassword arena_dev > database.sql      # password required
file=backup_`date '+%Y-%m-%d'`.zip
zip -r $file database.sql workdir/submissions
aws s3 cp $file s3://backup.arena.olimpiici.com/
rm database.sql
rm $file
popd

