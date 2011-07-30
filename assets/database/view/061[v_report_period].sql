create view v_report_period AS 
select 
		0 as _id,
       null as name,    
       t.from_account_currency_id as currency_id,          
       t.from_amount as amount,
       t.datetime as datetime,
       t.is_transfer as is_transfer
from v_blotter_for_account t;
	
