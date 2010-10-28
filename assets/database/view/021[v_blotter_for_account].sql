CREATE VIEW v_blotter_for_account AS 
SELECT
	t._id as _id,
	a._id as from_account_id,		
	a.title as from_account_title,
	a.is_include_into_totals as from_account_is_include_into_totals,
	c._id as from_account_currency_id,
	a2._id as to_account_id,		
	a2.title as to_account_title,
	a2.currency_id as to_account_currency_id,
	cat._id as category_id,
	cat.title as category_title,
	cat.left as category_left,
	cat.right as category_right,
	p._id as project_id,
	p.title as project,
	loc._id as location_id,
	loc.name as location,
	t.payee as payee,
	t.note as note,
	t.from_amount as from_amount,
	t.to_amount as to_amount,
	t.datetime as datetime,
	t.is_template as is_template,
	t.template_name as template_name,
	t.recurrence as recurrence,
	t.notification_options as notification_options,
	t.status as status,
	t.to_account_id as is_transfer
FROM 
	transactions as t		
	INNER JOIN account as a ON a._id=t.from_account_id
	INNER JOIN currency as c ON c._id=a.currency_id
	INNER JOIN category as cat ON cat._id=t.category_id
	LEFT OUTER JOIN account as a2 ON a2._id=t.to_account_id
	LEFT OUTER JOIN locations as loc ON loc._id=t.location_id
	LEFT OUTER JOIN project as p ON p._id=t.project_id
WHERE is_template=0
UNION ALL
SELECT
	t._id as _id,
	a._id as from_account_id,		
	a.title as from_account_title,
	a.is_include_into_totals as from_account_is_include_into_totals,
	c._id as from_account_currency_id,
	a2._id as to_account_id,		
	a2.title as to_account_title,
	a2.currency_id as to_account_currency_id,
	cat._id as category_id,
	cat.title as category_title,
	cat.left as category_left,
	cat.right as category_right,
	p._id as project_id,
	p.title as project,
	loc._id as location_id,
	loc.name as location,
	t.payee as payee,
	t.note as note,
	t.to_amount as from_amount,
	t.from_amount as to_amount,
	t.datetime as datetime,
	t.is_template as is_template,
	t.template_name as template_name,
	t.recurrence as recurrence,
	t.notification_options as notification_options,
	t.status as status,
	1 as is_transfer
FROM 
	transactions as t		
	INNER JOIN account as a ON a._id=t.to_account_id
	INNER JOIN currency as c ON c._id=a.currency_id
	INNER JOIN category as cat ON cat._id=t.category_id
	LEFT OUTER JOIN account as a2 ON a2._id=t.from_account_id
	LEFT OUTER JOIN locations as loc ON loc._id=t.location_id
	LEFT OUTER JOIN project as p ON p._id=t.project_id
WHERE is_template=0;