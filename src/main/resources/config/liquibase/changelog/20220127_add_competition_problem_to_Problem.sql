--liquibase formatted sql
--changeset shalamanov:20220127

ALTER TABLE problem
ADD canonical_competition_problem bigint;

ALTER TABLE problem
ADD CONSTRAINT FK_comp_problem
FOREIGN KEY (canonical_competition_problem) REFERENCES competition_problem(id);

--rollback ALTER TABLE problem DROP FOREIGN KEY FK_comp_problem;
--rollback ALTER TABLE problem DROP COLUMN canonical_competition_problem;


