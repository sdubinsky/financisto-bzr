CREATE VIEW v_blotter_for_currency AS
select t.*
from v_blotter_for_account t
where not (t.from_amount+ifnull(t.to_amount, 0)=0 and t.from_account_currency_id=ifnull(t.to_account_currency_id, -1));