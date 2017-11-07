package ViewModel;

import java.util.List;

/**
 * Created by user on 2016/11/19.
 */

public class CommentView {

    public List<Comments> CommentList;
    public List<String> CTime;
    public AppendForComment AppendComment;

    public static class Comments {
        public int ComId;
        public int PostId;
        public String Account;
        public String Content;
        public String CreateTime;
    }

    public static class AppendForComment {
        public int NowAppendTimes;
        public int MaxAppendTimes;
        public int ItemNum;
    }
}
