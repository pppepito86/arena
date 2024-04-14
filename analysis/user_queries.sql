-- users with a lot of submissions per day
select user_id, max(c) 
from (
    select user_id, CAST(upload_date as date) as date, count(*) as c 
    from submission 
    where user_id != 4  
    group by 1,2
) as t 
group by 1 
order by max(c) desc
limit 20;

-- users with submissions to many problems per day
select user_id, max(c) from (
    select 
        user_id, 
        CAST(upload_date as date) as date, 
        count(distinct  competition_problem_id) as c 
    from submission 
    where user_id != 4 
    group by 1,2
) 
as t group by 1 
order by max(c) desc 
limit 20;



--submissions per user
select id,upload_date,competition_problem_id
from submission
where user_id=123;

-- submissions per day per user
select
    user_id, 
    CAST(upload_date as date) as date, 
    count(distinct competition_problem_id) as c 
from submission
where user_id = 123
group by 1,2
having c > 30;


-- most banned users
select 
    first_name, last_name, user_id, 
    sum(if(verdict="banned", 1, 0)) as num_banned, 
    count(*) as total, 
    100 * sum(if(verdict="banned", 1, 0))/count(*) as percent_banned 
from submission 
join jhi_user on jhi_user.id=submission.user_id 
group by 1,2,3
order by 4 desc
limit 20;
