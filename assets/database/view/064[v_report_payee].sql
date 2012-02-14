create view v_report_payee AS
select 
	   p._id as _id,
       p.title as name,    
       t.datetime as datetime,
       t.from_account_currency_id as from_account_currency_id,
       t.from_amount as from_amount,
       t.to_account_currency_id as to_account_currency_id,
       t.to_amount as to_amount,
       t.is_transfer as is_transfer
from payee p
inner join v_blotter_for_account t on t.payee_id=p._id
where p._id != 0;
