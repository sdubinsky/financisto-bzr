create view v_budget AS 
SELECT
	b._id as _id,
	b.title as title,
	b.amount as amount,
	b.start_date as start_date,
	b.end_date as end_date,
	b.repeat as repeat,
	b.include_subcategories as include_subcategories,
	b.recur as recur,
	b.recur_num as recur_num,
	b.is_current as is_current,
	b.parent_budget_id as parent_budget_id,
	cat._id as category_id,
	cat.title as category_title,
	cat.left as category_left,
	cat.right as category_right,
	cur._id as currency_id
FROM
	budget as b,
	category as cat,
	currency as cur
WHERE
	cat._id=b.category_id
	AND cur._id=b.currency_id;
	
