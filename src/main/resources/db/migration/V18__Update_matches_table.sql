alter table matches add column gender varchar(255) null;
alter table matches add column is_paid_event boolean default false;
alter table matches add column price_per_player decimal(10, 2) default 0;
alter table matches add column currency varchar(3) default 'GBP';
alter table matches add column is_refundable boolean default true;
alter table matches add column max_players integer default null;
alter table matches add column latitude decimal(10, 8) default null;
alter table matches add column longitude decimal(11, 8) default null;
alter table matches add column address varchar(255) null;