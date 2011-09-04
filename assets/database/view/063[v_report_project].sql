create view v_report_project AS 
select 
	   p._id as _id,
       p.title as name,    
       t.from_account_currency_id as currency_id,          
       t.from_amount as amount,
       t.datetime as datetime,
       t.is_transfer as is_transfer
from project p
inner join v_blotter_for_account t on t.project_id=p._id
where p._id != 0;
