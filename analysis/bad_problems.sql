-- Problems ID with multiple competition problems associated to them.
select problem_id, title, 
    GROUP_CONCAT(competition_problem.id) as competition_problem_id,
    GROUP_CONCAT(competition_problem.competition_id) as competition_id
from competition_problem 
left join problem on competition_problem.problem_id = problem.id
group by problem_id
having count(competition_problem.id) > 1;


-- Competition problems without a competitions. This should be empty.
select *
from competition_problem 
where competition_id is null;

-- Delete these
delete from competition_problem 
where competition_id is null;

-- List submissions, which are associated with compeition problem IDs, which don't have a competition.
-- These are dead submissions which should not exist. 
select submission.id, competition_problem_id, user_id, first_name, last_name, points
from submission
join competition_problem
  on submission.competition_problem_id = competition_problem.id
join jhi_user
  on jhi_user.id = submission.user_id
where competition_problem.competition_id is null;

-- Delete them
delete s
from submission s
join competition_problem
  on s.competition_problem_id = competition_problem.id
where competition_problem.competition_id is null;

-- Problems, whose canonical competition problem doesn't have a compeition.
-- Should be empty.
select problem.*
from problem
join competition_problem
  on competition_problem.id = problem.canonical_competition_problem
where competition_problem.competition_id is null;
