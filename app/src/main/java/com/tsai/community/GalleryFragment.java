package com.tsai.community;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import MyMethod.FileChooser;
import MyMethod.PersistentCookieStore;
import ViewModel.GalleryView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;


/**
 * A simple {@link Fragment} subclass.
 */
public class GalleryFragment extends Fragment {

    private static final int REQUEST_EXTERNAL_STORAGE = 18;
    //避免重複Toast
    private Toast toast = null;
    private FileChooser fileChooser;

    private OkHttpClient client;
    private GalleryView galleryView;
    private List<GalleryView.Gallery> galleryList = new ArrayList<GalleryView.Gallery>();

    //第一次進入resume才會創建ListView
    private boolean isFirstLoad = true;
    private GalleryAdapter galleryAdapter;

    private int maxMemory = (int) Runtime.getRuntime().maxMemory();
    private int cacheSizes = maxMemory / 3;

    public GalleryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        SetCookies();
        Refresh();
        Button bt_UploadImage = (Button) view.findViewById(R.id.bt_UploadImage);
        bt_UploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UploadImage(v);
            }
        });
        return view;
    }

    private void SetCookies() {
        client = new OkHttpClient().newBuilder()
                .cookieJar(new CookieJar() {
                    private final PersistentCookieStore cookieStore = new PersistentCookieStore(getActivity());

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        if (cookies != null && cookies.size() > 0) {
                            for (Cookie item : cookies) {
                                cookieStore.add(url, item);
                            }
                        }
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url);
                        return cookies;
                    }
                })
                .build();
    }

    public void Refresh() {
        if (!isFirstLoad) {
            galleryList = new ArrayList<GalleryView.Gallery>();
            //避免上傳圖片後不自動上網抓取剛上傳的圖片
            galleryAdapter.isFirstIn = true;
            galleryAdapter.notifyDataSetChanged();
        }
        GetGallery();

    }

    private void GetGallery() {
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "Api/GuestbookApi/GetGallery")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showTextToast("請檢察網路連線");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            Gson gson = new Gson();
                            galleryView = new GalleryView();
                            galleryView = gson.fromJson(ResMsg, GalleryView.class);
                            if (galleryView.GalleryList.size() == 0) {
                                showTextToast("您的圖庫是空的!!");
                                return;
                            }
                            galleryList.addAll(galleryView.GalleryList);
                            if (isFirstLoad) {
                                isFirstLoad = false;
                                ListView imageListView = (ListView) getActivity().findViewById(R.id.lv_Gallery);
                                galleryAdapter = new GalleryAdapter(getActivity(), imageListView);
                                imageListView.setOnItemClickListener(new imageClick());
                                imageListView.setOnItemLongClickListener(new imageLongClick());
                                imageListView.setAdapter(galleryAdapter);
                            } else {
                                galleryAdapter.notifyDataSetChanged();
                            }

                        } else if (StatusCode == 400) {
                            Gson gson = new Gson();
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("錯誤訊息")
                                    .setMessage(gson.fromJson(ResMsg, String.class))
                                    .setPositiveButton("知道了", null)
                                    .show();
                        } else {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("錯誤訊息")
                                    .setMessage(String.valueOf(StatusCode))
                                    .setPositiveButton("知道了", null)
                                    .show();
                        }
                    }
                });
            }
        });
    }

    class imageClick implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            if (galleryList.get(position).ImgDescription == null) {
                showEditDialog(position, 1);
            } else {
                new AlertDialog.Builder(getActivity())
                        .setTitle("圖片簡介")
                        .setMessage(galleryList.get(position).ImgDescription)
                        .setNeutralButton("關閉", null)
                        .setPositiveButton("修改簡介", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showEditDialog(position, 2);
                            }
                        })
                        .show();
            }

        }
    }

    private void showEditDialog(final int position, int Type) {
        final View editView = LayoutInflater.from(getActivity()).inflate(R.layout.edit_view, null);
        final EditText et_EditContent = (EditText) editView.findViewById(R.id.et_EditContent);
        String Title, PositiveButton;

        if (Type == 2) {
            et_EditContent.setText(galleryList.get(position).ImgDescription);
            et_EditContent.setSelection(et_EditContent.length());
            Title = "修改圖片簡介";
            PositiveButton = "修改";
        } else {
            Title = "新增圖片簡介";
            PositiveButton = "新增";
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(Title)
                .setView(editView)
                .setNeutralButton("取消", null)
                .setPositiveButton(PositiveButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RequestBody formBody = new FormBody.Builder()
                                .add("PicId", String.valueOf(galleryList.get(position).PicId))
                                .add("ImgDescription", et_EditContent.getText().toString())
                                .build();

                        Request request = new Request.Builder()
                                .url(getString(R.string.server_url) + "Api/GuestbookApi/EditImgDescription")
                                .post(formBody)
                                .build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showTextToast("請檢察網路連線");
                                    }
                                });
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                final int StatusCode = response.code();
                                final String ResMsg = response.body().string();

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (StatusCode == 200) {
                                            showTextToast("成功");
                                            galleryList.get(position).ImgDescription = et_EditContent.getText().toString();
                                            galleryAdapter.notifyDataSetChanged();
                                        } else if (StatusCode == 400) {
                                            new AlertDialog.Builder(getActivity())
                                                    .setTitle("錯誤訊息")
                                                    .setMessage(ResMsg)
                                                    .setPositiveButton("知道了", null)
                                                    .show();
                                        }
                                    }
                                });
                            }
                        });
                    }
                })
                .show();
    }

    class imageLongClick implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            new AlertDialog.Builder(getActivity())
                    .setMessage("刪除雲端圖片會一併刪除包含此圖片的留言!\n您確定要刪除嗎?")
                    .setNeutralButton("取消", null)
                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Request request = new Request.Builder()
                                    .url(getString(R.string.server_url) + "Api/GuestbookApi/DeleteImage?PicId=" + galleryList.get(position).PicId)
                                    .build();
                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showTextToast("請檢察網路連線");
                                        }
                                    });
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    final int StatusCode = response.code();
                                    final Gson gson = new Gson();
                                    final String ResMsg = response.body().string();

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (StatusCode == 200) {
                                                showTextToast("刪除成功");
                                                Refresh();
                                            } else if (StatusCode == 400) {
                                                new AlertDialog.Builder(getActivity())
                                                        .setTitle("錯誤訊息")
                                                        .setMessage(gson.fromJson(ResMsg, String.class))
                                                        .setPositiveButton("知道了", null)
                                                        .show();
                                            } else {
                                                new AlertDialog.Builder(getActivity())
                                                        .setTitle("錯誤訊息")
                                                        .setMessage(String.valueOf(StatusCode))
                                                        .setPositiveButton("知道了", null)
                                                        .show();
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    })
                    .show();
            return true;
        }
    }

    public class GalleryAdapter extends BaseAdapter implements AbsListView.OnScrollListener {
        private LayoutInflater mInflater;
        private ListView mListView;
        private boolean isFirstIn;
        private ImageLoader imageLoader;
        private int mStart;
        private int mEnd;

        public GalleryAdapter(Context context, ListView listView) {

            mInflater = LayoutInflater.from(context);
            mListView = listView;
            isFirstIn = true;

            imageLoader = new ImageLoader(mListView);
            mListView.setOnScrollListener(this);
        }

        @Override
        public int getCount() {
            return galleryList.size();
        }

        @Override
        public Object getItem(int position) {
            return galleryList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return galleryList.get(position).PicId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View IL = convertView;
            final Holder holder;
            if (IL == null) {
                IL = mInflater.inflate(R.layout.image_block, null);
                holder = new GalleryAdapter.Holder();
                holder.imageView = (ImageView) IL.findViewById(R.id.iv_GalleryImage);
                IL.setTag(holder);

            } else {
                holder = (Holder) IL.getTag();
            }

            holder.imageView.setTag(galleryList.get(position).ImgUrl);
            imageLoader.showImage(holder.imageView, galleryList.get(position).ImgUrl);
            return IL;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == SCROLL_STATE_IDLE) {
                imageLoader.loadImages(mStart, mEnd);
            } else {
                imageLoader.cancelAllAsyncTask();
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            mStart = firstVisibleItem;
            mEnd = firstVisibleItem + visibleItemCount;

            if (isFirstIn && visibleItemCount > 0) {
                imageLoader.loadImages(mStart, mEnd);
                isFirstIn = false;
            }
        }

        class Holder {
            ImageView imageView;
        }
    }

    private class ImageLoader {
        Set<LoadImgAsyncTask> mTasks = new HashSet<>();
        private ListView imageListView;


        public ImageLoader(ListView listView) {
            imageListView = listView;
        }

        public void showImage(ImageView imageView, String url) {

            Bitmap bitmap = getBitmapFromLrucache(url);
            if (bitmap == null) {
                imageView.setImageDrawable(null);
            } else {
                imageView.setImageBitmap(bitmap);
            }
        }

        public void loadImages(int start, int end) {

            for (int i = start; i < end; i++) {
                String loadUrl = galleryList.get(i).ImgUrl;
                if (loadUrl != null) {
                    ImageView imageView = (ImageView) imageListView.findViewWithTag(loadUrl);
                    if (getBitmapFromLrucache(loadUrl) != null) {
                        imageView.setImageBitmap(getBitmapFromLrucache(loadUrl));
                    } else {
                        LoadImgAsyncTask imgAsyncTask = new LoadImgAsyncTask(imageView, loadUrl);
                        mTasks.add(imgAsyncTask);
                        imgAsyncTask.execute(loadUrl);
                    }
                }

            }
        }

        public void cancelAllAsyncTask() {
            if (mTasks != null) {
                for (LoadImgAsyncTask imgAsyncTask : mTasks) {
                    imgAsyncTask.cancel(false);
                }
            }
        }
    }

    public class LoadImgAsyncTask extends AsyncTask<String, Void, Bitmap> {
        private String mUrl;
        private ImageView dataImageView;

        public LoadImgAsyncTask(ImageView imageView, String url) {
            mUrl = url;
            dataImageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... params) {

            String url = params[0];
            Bitmap bitmap;

            bitmap = getBitmapFromUrl(url);
            if (bitmap != null) {
                addBitmapToLrucaches(url, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (dataImageView.getTag().equals(mUrl)) {
                dataImageView.setImageBitmap(bitmap);
            }
        }

        public Bitmap getBitmapFromUrl(String urlString) {
            Bitmap bitmap;
            InputStream is = null;
            try {
                URL mUrl = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) mUrl.openConnection();
                is = new BufferedInputStream(connection.getInputStream());
                bitmap = BitmapFactory.decodeStream(is);
                connection.disconnect();
                return bitmap;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

    }


    private LruCache<String, Bitmap> mMemoryCaches = new LruCache<String, Bitmap>(cacheSizes) {
        @SuppressLint("NewApi")
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    public Bitmap getBitmapFromLrucache(String url) {
        return mMemoryCaches.get(url);
    }

    public void addBitmapToLrucaches(String url, Bitmap bitmap) {
        if (getBitmapFromLrucache(url) == null) {
            mMemoryCaches.put(url, bitmap);
        }
    }

    public void clearLruCache() {
        if (mMemoryCaches != null) {
            if (mMemoryCaches.size() > 0) {
                mMemoryCaches.evictAll();
            }
        }
    }

    public void UploadImage(View v) {
        int permission = ActivityCompat.checkSelfPermission(getActivity(), READ_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            fileChooser = new FileChooser(getActivity(), this);
            if (!fileChooser.showFileChooser("image/*")) {
                showTextToast("您沒有適合的檔案選取器");
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fileChooser = new FileChooser(getActivity(), this);
                    if (!fileChooser.showFileChooser("image/*")) {
                        showTextToast("您沒有適合的檔案選取器");
                    }
                } else {
                    showTextToast("您拒絕選取檔案");
                }
                return;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 9973:
                if (fileChooser.onActivityResult(requestCode, resultCode, data)) {
                    showTextToast("圖片上傳中...");
                    File[] files = fileChooser.getChosenFiles();
                    UploadFile(files[0]);
                }
        }
    }

    private void UploadFile(File file) {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("image/png"), file))
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "Api/GuestbookApi/Upload")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showTextToast("請檢察網路連線");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 201) {
                            showTextToast("上傳成功");
                            Refresh();
                        } else if (StatusCode == 400) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("錯誤訊息")
                                    .setMessage(ResMsg)
                                    .setPositiveButton("知道了", null)
                                    .show();
                        }
                    }
                });

            }
        });
    }

    //避免重複Toast
    private void showTextToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }

}
