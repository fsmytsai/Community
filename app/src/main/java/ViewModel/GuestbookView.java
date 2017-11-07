package ViewModel;

import java.util.Date;
import java.util.List;

/**
 * Created by user on 2016/10/6.
 */

public class GuestbookView {
    public List<Data> DataList;
    public List<String> CTime;
    public List<String> RTime;
    public String Search;
    public Paging Paging;

    public static class Data {
        public int Id;
        public String Account;
        public String Content;
        public String CreateTime;
        public String ReplyAccount;
        public String Reply;
        public String ReplyTime;
        public String ImgUrl;
    }

    public static class Paging {
        public int NowPage;
        public int MaxPage;
        public int ItemNum;
    }

}
