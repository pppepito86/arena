
-- Export the time limits. The file needs to be in one of the dirs from 
-- SHOW VARIABLES LIKE "secure_file_priv" so it can be loaded in SQL.
--
--   cd arena/workdir/problems/
--   /tmp/export_time_limits.sh > /var/lib/mysql-files/time_limits.csv

CREATE TEMPORARY TABLE real_time_limits(
    problem_id INT PRIMARY KEY,
    time_limit DECIMAL(5,3)
);

LOAD DATA INFILE '/var/lib/mysql-files/time_limits.csv' 
INTO TABLE real_time_limits 
FIELDS TERMINATED BY ',' 
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;


-- Check what's loaded
select count(*)
from real_time_limits;

-- Real time limits joined with competition_problems
select
    id as competition_problem_id,
    problem_id,
    competition_id,
    time_limit as real_time_limit
from competition_problem
left join real_time_limits using (problem_id)
limit 10;


-- DROP TABLE IF EXISTS author_times;

-- Extract stats for the timing of the first 3 author
-- solutions for each problem.
create temporary table author_times as
with ordered_submissions as (
    select
        competition_problem_id,
        user_id, 
        id as submission_id,
        time_in_millis,
        ROW_NUMBER() OVER (
         PARTITION BY competition_problem_id ORDER BY id
        ) AS local_submission_number
    from submission
    where user_id = 4 -- author
)
select competition_problem_id,
    AVG(time_in_millis) / 1000 as avg_time_millis,
    MAX(time_in_millis) / 1000 as max_time_millis
from ordered_submissions
-- Take first 3 submissions per problem
where local_submission_number <= 3
group by competition_problem_id;

-- Join with the real time limits.
select title,
    problem_id,
    competition_problem_id,
    competition_problem.competition_id,
    avg_time_millis,
    max_time_millis,
    time_limit,
    time_limit / avg_time_millis as tl_over_avg,
    time_limit / max_time_millis as tl_over_max
from author_times
left join competition_problem
  on competition_problem.id = author_times.competition_problem_id
left join real_time_limits using (problem_id)
left join problem on problem.id = problem_id
-- Exclude super fast solutions, which have the default time limit of 0.1
where not (max_time_millis < 0.01 and time_limit = 0.1)
order by tl_over_avg 
limit 50;






