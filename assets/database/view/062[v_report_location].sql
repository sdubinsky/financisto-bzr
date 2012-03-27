create view v_report_location AS 
select 
	   l._id as _id,
       l.name as name,    
       t.datetime as datetime,
       t.from_account_currency_id as from_account_currency_id,
       t.from_amount as from_amount,
       t.to_account_currency_id as to_account_currency_id,
       t.to_amount as to_amount,
       t.is_transfer as is_transfer
from locations l
inner join v_blotter_for_account t on t.location_id=l._id
where l._id != 0 and from_account_is_include_into_totals=1;
