package ViewModel;

import java.util.List;

/**
 * Created by user on 2016/10/22.
 */

public class GalleryView {
    public List<Gallery> GalleryList;
    public static class Gallery {
        public int PicId;
        public String Account;
        public String ImgUrl;
        public String ImgDescription;
    }
}
