package ru.orangesoftware.financisto.utils;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/25/11 7:16 PM
 */
public class AndroidUtils {

    private AndroidUtils(){}

    public static boolean isSupportedApiLevel() {
        String version = android.os.Build.VERSION.SDK;
        return Integer.parseInt(version) >= 4; // supports at least Donut
    }

}
