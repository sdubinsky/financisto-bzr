CREATE VIEW v_blotter_for_currency AS
select 
	t.*
from v_blotter_for_account t;