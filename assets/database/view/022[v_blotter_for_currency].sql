CREATE VIEW v_blotter_for_currency AS
select 
	t.*
from v_blotter_for_account_with_splits t
where is_template=0;