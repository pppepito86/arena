
-- Tags by popularity
-- This should give the same order and popularity as in the UI.
select tag.id,
    left(tag.title, 30), -- trim long title
    tag.visible,
    count(tag_collection_tag.id) as popularity
from tag
left join tag_collection_tag
  on tag_collection_tag.tag_id = tag.id
group by tag.id
order by 4 desc;



-- Most popular tags, which aren't visible. Should we make some
-- of them visible?
select tag.id,
    left(tag.title, 30) as title, -- trim long title
    tag.visible,
    count(tag_collection_tag.id) as popularity
from tag
left join tag_collection_tag
  on tag_collection_tag.tag_id = tag.id
group by tag.id
having visible is null or visible <> 1
order by 4 desc
limit 20;


-- Invisible tags with 0 popularity. This can be deleted
-- straight away.
select tag.id,
    left(tag.title, 30) as title, -- trim long title
    tag.visible,
    count(tag_collection_tag.id) as popularity
from tag
left join tag_collection_tag
  on tag_collection_tag.tag_id = tag.id
where (visible is null or visible <> 1)
group by tag.id
having count(tag_collection_tag.id) = 0;

-- Delete them.
delete from tag
where id in
(
  select id 
  from (
    select tag.id
    from tag
    left join tag_collection_tag
      on tag_collection_tag.tag_id = tag.id
    where (tag.visible is null or tag.visible <> 1)
    group by tag.id
    having count(tag_collection_tag.id) = 0
  ) t
);


-- Tags with popularity 1, by user who added them.
-- Can identify spammers or at least group the tags so the
-- nonsense it clustered.
select submission.id, submission.user_id, first_name, last_name, 
  left(tag.title, 20) as title
from submission
join tag_collection_tag
  on submission.tags_id = tag_collection_tag.collection_id
join tag
 on tag_collection_tag.tag_id = tag.id
join jhi_user
  on jhi_user.id = submission.user_id
where tag.id in
(
  select id 
  from (
    select tag.id,
        left(tag.title, 30) as title,
        tag.visible,
        count(tag_collection_tag.id) as popularity
    from tag
    left join tag_collection_tag
      on tag_collection_tag.tag_id = tag.id
    where visible is null or visible <> 1
    group by tag.id
    having count(tag_collection_tag.id) = 1
    order by 4 desc
  ) t
)
order by 2;



-- List all tags for a given user.
select submission.id, submission.user_id, first_name, last_name, 
  left(tag.title, 20) as title
from submission
join tag_collection_tag
  on submission.tags_id = tag_collection_tag.collection_id
join tag
 on tag_collection_tag.tag_id = tag.id
join jhi_user
  on jhi_user.id = submission.user_id
where tag.id in
(
  select id 
  from (
    select tag.id,
        left(tag.title, 30) as title,
        tag.visible,
        count(tag_collection_tag.id) as popularity
    from tag
    left join tag_collection_tag
      on tag_collection_tag.tag_id = tag.id
    where visible is null or visible <> 1
    group by tag.id
    order by 4 desc
  ) t
)
and submission.user_id = 776
order by 2;
