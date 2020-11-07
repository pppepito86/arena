
set @source_comp = 7;

set @source_name = (select jhi_label from competition where id = @source_comp);

update  competition c 
join competition p on p.id=c.parent_id 
set c.jhi_label = concat(c.jhi_label, '-', p.jhi_label) 
where p.parent_id = @source_comp;


insert into competition (jhi_label, parent_id) 
values (@source_name, 1);

set @comp = (select max(id) from competition);


set @group = 'A';

insert into competition (jhi_label, parent_id) 
values (@group, @comp);

set @last_comp  = (select max(id) from competition);

update  competition c 
join competition p on p.id=c.parent_id 
set c.parent_id = @last_comp, c.jhi_order = (2020 - CONVERT(SUBSTRING(c.jhi_label, 3, 4), SIGNED))
where c.jhi_label like (concat(@group, '%') collate utf8mb4_unicode_ci) and p.parent_id=7;

update  competition c 
join competition p on p.id=c.parent_id 
set c.jhi_label=SUBSTRING(c.jhi_label, 3, 4) 
where c.parent_id=@last_comp;

-- -------------------------------------


set @group = 'B';

insert into competition (jhi_label, parent_id) 
values (@group, @comp);

set @last_comp  = (select max(id) from competition);

update  competition c 
join competition p on p.id=c.parent_id 
set c.parent_id = @last_comp, c.jhi_order = (2020 - CONVERT(SUBSTRING(c.jhi_label, 3, 4), SIGNED))
where c.jhi_label like (concat(@group, '%') collate utf8mb4_unicode_ci) and p.parent_id=7;

update  competition c 
join competition p on p.id=c.parent_id 
set c.jhi_label=SUBSTRING(c.jhi_label, 3, 4) 
where c.parent_id=@last_comp;

-- -------------------------------------


set @group = 'C';

insert into competition (jhi_label, parent_id) 
values (@group, @comp);

set @last_comp  = (select max(id) from competition);

update  competition c 
join competition p on p.id=c.parent_id 
set c.parent_id = @last_comp, c.jhi_order = (2020 - CONVERT(SUBSTRING(c.jhi_label, 3, 4), SIGNED))
where c.jhi_label like (concat(@group, '%') collate utf8mb4_unicode_ci) and p.parent_id=7;

update  competition c 
join competition p on p.id=c.parent_id 
set c.jhi_label=SUBSTRING(c.jhi_label, 3, 4) 
where c.parent_id=@last_comp;

-- -------------------------------------


set @group = 'D';

insert into competition (jhi_label, parent_id) 
values (@group, @comp);

set @last_comp  = (select max(id) from competition);

update  competition c 
join competition p on p.id=c.parent_id 
set c.parent_id = @last_comp, c.jhi_order = (2020 - CONVERT(SUBSTRING(c.jhi_label, 3, 4), SIGNED))
where c.jhi_label like (concat(@group, '%') collate utf8mb4_unicode_ci) and p.parent_id=7;

update  competition c 
join competition p on p.id=c.parent_id 
set c.jhi_label=SUBSTRING(c.jhi_label, 3, 4) 
where c.parent_id=@last_comp;

-- -------------------------------------

set @group = 'E';

insert into competition (jhi_label, parent_id) 
values (@group, @comp);

set @last_comp  = (select max(id) from competition);

update  competition c 
join competition p on p.id=c.parent_id 
set c.parent_id = @last_comp, c.jhi_order = (2020 - CONVERT(SUBSTRING(c.jhi_label, 3, 4), SIGNED))
where c.jhi_label like (concat(@group, '%') collate utf8mb4_unicode_ci) and p.parent_id=7;

update  competition c 
join competition p on p.id=c.parent_id 
set c.jhi_label=SUBSTRING(c.jhi_label, 3, 4) 
where c.parent_id=@last_comp;

-- -------------------------------------
