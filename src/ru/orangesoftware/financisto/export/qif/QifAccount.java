package ru.orangesoftware.financisto.export.qif;

import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/7/11 8:03 PM
 */
public class QifAccount {

    public String type;
    public String memo;

    public static QifAccount fromAccount(Account account) {
        QifAccount qifAccount = new QifAccount();
        qifAccount.type = decodeAccountType(account.type);
        qifAccount.memo = account.title;
        return qifAccount;
    }

    /*
    !Type:Bank	Bank account transactions
    !Type:Cash	Cash account transactions
    !Type:CCard	Credit card account transactions
    !Type:Invst	Investment account transactions
    !Type:Oth A	Asset account transactions
    !Type:Oth L	Liability account transactions
    */
    private static String decodeAccountType(String type) {
        AccountType t = AccountType.valueOf(type);
        switch (t) {
            case BANK:
                return "Bank";
            case CASH:
                return "Cash";
            case CREDIT_CARD:
                return "CCard";
            case ASSET:
                return "Oth A";
            default:
                return "Oth L";
        }
    }

    public void writeTo(QifBufferedWriter bw) throws IOException {
        bw.writeAccountsHeader();
        bw.write("N").write(memo).newLine();
        bw.write("T").write(type).newLine();
        bw.end();
    }

}
