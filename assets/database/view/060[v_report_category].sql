create view v_report_category AS 
select 
	   c._id as _id,
	   c.parent_id as parent_id,
       c.title as name,    
       t.from_account_currency_id as currency_id,          
       t.from_amount as amount,
       t.datetime datetime
from v_category_list c
inner join v_blotter_for_currency t on t.category_left between c.left and c.right
where c._id != 0 and t.from_amount > 0
union all
select 
	   c._id as _id,
	   c.parent_id as parent_id,
       c.title as name,    
       t.from_account_currency_id as currency_id,          
       t.from_amount as amount,
       t.datetime datetime
from v_category_list c
inner join v_blotter_for_currency t on t.category_left between c.left and c.right
where c._id != 0 and t.from_amount < 0;
	
