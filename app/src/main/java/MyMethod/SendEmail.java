package MyMethod;

import android.app.Application;
import android.util.Log;

import com.tsai.community.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by user on 2016/10/15.
 */

public class SendEmail extends Application {
    private OkHttpClient client = new OkHttpClient();
    private String Account;

    public SendEmail(String account) {
        this.Account = account;
        this.Send();
    }

    private void Send() {
        Request request = new Request.Builder()
                .url(getApplicationContext().getString(R.string.server_url)+"Api/MemberApi/SendEmail?Account=" + Account)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("FailMSG", call.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
    }
}
