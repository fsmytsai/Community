package ViewModel;

import java.util.List;

/**
 * Created by user on 2016/11/18.
 */

public class PostView {

    public List<Posts> PostList;
    public List<String> CTime;
    public List<Boolean> IsLike;
    public List<Integer> CommentNum;
    public String Search;

    public static class Posts {
        public int PostId;
        public String Account;
        public String Content;
        public String CreateTime;
        public int LikeNum;
        public String LatestTime;
        public String ImgUrl;
        public boolean IsTop;
        public int GroupId;
    }
}
