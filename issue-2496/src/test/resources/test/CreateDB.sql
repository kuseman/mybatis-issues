DROP TABLE IF EXISTS users;

create table users (
  id int,
  name varchar(20)
);

insert into users (id, name) values
(1, 'User1'),
(2, 'User2');


create or alter procedure queryUsers
(

	@pUser_id AS INT = -1
)
as

	select id, name
	from users
	where id = @pUser_id

	return(0)
;

create or alter procedure queryUsersWithDebugSelect
(
	@pUser_id AS INT = -1
)
as
	-- Set this has no effect when selecting
	-- that is however a workaround when dealing with JDBC update-calls
	-- set nocount on

	-- Debug some stuff in a procedure temporarily (unknowingly that it's used in mybatis)
	-- to investigate some issue by running the procedure inside Management studio etc.
	select *
	from users

	-- The real query that the procedure was built to do
	select id, name
	from users
	where id = @pUser_id

	return(0)
;
