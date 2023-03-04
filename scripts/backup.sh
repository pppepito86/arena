curr_date=`date '+%Y-%m-%d'`

mkdir ~/arena/workdir/problems_to_backup

pushd ~/arena/workdir/problems
find . -mindepth 1 -maxdepth 1 -mtime -60 -exec ln -s $(pwd)/{} ../problems_to_backup/{} \;
popd

pushd ~/arena/workdir/problems_to_backup
aws s3 sync . s3://backup.arena.olimpiici.com/problems_${curr_date} --exclude "*" --include "*.zip"
popd

rm -r ~/arena/workdir/problems_to_backup


pushd ~/arena
mysqldump -u root arena_dev > database.sql      # password required
file=backup_${curr_date}.zip
zip -r $file database.sql workdir/submissions
aws s3 cp $file s3://backup.arena.olimpiici.com/
rm database.sql
rm $file
popd

