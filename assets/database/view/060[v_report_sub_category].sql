create view v_report_sub_category AS 
select 
	   c._id as _id,
	   c.left as left,
	   c.right as right,
       c.title as name,    
       t.from_account_currency_id as currency_id,          
       t.from_amount as amount,
       t.datetime as datetime,
       t.is_transfer as is_transfer
from v_category c
inner join v_blotter_for_account_with_splits t on t.category_left between c.left and c.right
where c._id > 0;
	
