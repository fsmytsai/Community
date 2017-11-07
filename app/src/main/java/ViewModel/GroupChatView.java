package ViewModel;

import java.util.List;

/**
 * Created by user on 2016/10/25.
 */

public class GroupChatView {
    public List<ChatData> GroupChatList;
    public List<String> CTime;
    public Paging Paging;
    public static class ChatData {
        public int ChatId;
        public String SendAccount;
        public String Content;
        public String CreateTime;
    }
    public static class Paging {
        public int NowPage;
        public int MaxPage;
        public int ItemNum;
    }
}
