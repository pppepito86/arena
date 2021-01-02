--liquibase formatted sql

create table topic (
    id bigint(20) not null auto_increment,
    title varchar(255) not null,
    created_date datetime not null,
    creator_id bigint(20),
    primary key (id),
    foreign key (creator_id) references jhi_user(id)
);

create table comment (
    id bigint(20) not null auto_increment,
    topic_id bigint(20) not null,
    content varchar(1024) not null,
    author_id bigint(20) not null,
    posted_date datetime not null,
    reviewed boolean not null default 0,
    censured boolean not null default 0,
    censured_by bigint(20),
    censured_date datetime,
    primary key (id),
    foreign key (topic_id) references topic(id),
    foreign key (author_id) references jhi_user(id),
    foreign key (censured_by) references jhi_user(id)
);

CREATE INDEX reviewed_comments
ON comment (reviewed);

create table topic_subscriptions (
    user_id bigint(20) not null,
    topic_id bigint(20) not null,
    primary key (user_id, topic_id),
    foreign key (user_id) references jhi_user(id),
    foreign key (topic_id) references topic(id)
);

create table problem_topic (
    competition_problem_id bigint(20) not null,
    topic_id bigint(20) not null,
    primary key (competition_problem_id),
    foreign key (topic_id) references topic(id),
    foreign key (competition_problem_id) references competition_problem(id)
);

--rollback drop table problem_topic;
--rollback drop table topic_subscriptions;
--rollback drop table comment;
--rollback drop table topic;

