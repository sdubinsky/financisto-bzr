create view v_report_location AS 
select 
	   l._id as _id,
       l.name as name,    
       t.from_account_currency_id as currency_id,          
       t.from_amount as amount,
       t.datetime datetime
from locations l
inner join v_blotter_for_currency t on t.location_id=l._id
where t.from_amount > 0 and l._id != 0
union all
select 
	   l._id as _id,
       l.name as name,    
       t.from_account_currency_id as currency_id,          
       t.from_amount as amount,
       t.datetime datetime
from locations l
inner join v_blotter_for_currency t on t.location_id=l._id
where t.from_amount < 0 and l._id != 0;	
