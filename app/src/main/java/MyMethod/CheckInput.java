package MyMethod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by user on 2016/10/19.
 */

public class CheckInput {
    public boolean CheckBlank(String str) {
        //回傳true表示通過
        Pattern pattern = Pattern.compile("[\\s]+");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            return false;
        }
        if (str.equals("")) return false;
        return true;
    }

    public boolean CheckEmpty(String str) {
        //回傳true表示通過
        if (str.trim().equals("")) return false;
        return true;
    }
}
