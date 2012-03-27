create view v_report_period AS 
select 
       0 as _id,
       null as name,
       t.datetime as datetime,
       t.from_account_currency_id as from_account_currency_id,
       t.from_amount as from_amount,
       t.to_account_currency_id as to_account_currency_id,
       t.to_amount as to_amount,
       t.is_transfer as is_transfer
from v_blotter_for_account_with_splits t
where t.category_id != -1 and from_account_is_include_into_totals=1;
	
