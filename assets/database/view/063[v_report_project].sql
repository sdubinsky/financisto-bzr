create view v_report_project AS 
select 
	   p._id as _id,
       p.title as name,    
       t.datetime as datetime,
       t.from_account_currency_id as from_account_currency_id,
       t.from_amount as from_amount,
       t.to_account_currency_id as to_account_currency_id,
       t.to_amount as to_amount,
       t.is_transfer as is_transfer
from project p
inner join v_blotter_for_account t on t.project_id=p._id
where p._id != 0 and from_account_is_include_into_totals=1;
