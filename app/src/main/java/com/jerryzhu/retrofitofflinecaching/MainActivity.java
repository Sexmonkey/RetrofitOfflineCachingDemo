package com.jerryzhu.retrofitofflinecaching;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jerryzhu.retrofitofflinecaching.bean.BannerData;
import com.jerryzhu.retrofitofflinecaching.bean.BaseResponse;
import com.jerryzhu.retrofitofflinecaching.bean.FeedArticleListData;
import com.jerryzhu.retrofitofflinecaching.net.APIService;
import com.jerryzhu.retrofitofflinecaching.util.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import static com.jerryzhu.retrofitofflinecaching.net.APIService.BASE_URL;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    TextView textView;
    Button btnGetRandomJoke;

    APIService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        btnGetRandomJoke = findViewById(R.id.button);

        setupRetrofitAndOkHttp();

        btnGetRandomJoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getResponseFromAPI();

            }
        });

    }

    private void setupRetrofitAndOkHttp() {

        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        File httpCacheDirectory = new File(getCacheDir(), "offlineCache");

        //10 MB
        Cache cache = new Cache(httpCacheDirectory, 10 * 1024 * 1024);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(httpLoggingInterceptor)
                .addNetworkInterceptor(REWRITE_RESPONSE_INTERCEPTOR)
                .addInterceptor(REWRITE_RESPONSE_INTERCEPTOR_OFFLINE)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .client(httpClient)
                .baseUrl(BASE_URL)
                .build();

        apiService = retrofit.create(APIService.class);

    }

    public void getResponseFromAPI() {
        Observable<BaseResponse<FeedArticleListData>> observable = apiService.getRandomJoke(1);
        observable.subscribeOn(Schedulers.newThread()).
                observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<BaseResponse<FeedArticleListData>>() {
            @Override
            public void accept(BaseResponse<FeedArticleListData> feedArticleListDataBaseResponse) throws Exception {
                Log.d(TAG, "accept:  feedArticleListDataBaseResponse = "+feedArticleListDataBaseResponse.getData().getDatas().get(1).getTitle());
                textView.setText(feedArticleListDataBaseResponse.getData().getDatas().get(1).getTitle());
            }
        });

        Observable<BaseResponse<List<BannerData>>> bannerData = apiService.getBannerData();
        bannerData.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<BaseResponse<List<BannerData>>>() {
            @Override
            public void accept(BaseResponse<List<BannerData>> listBaseResponse) throws Exception {

                Log.d(TAG, "accept: listBaseResponse = "+listBaseResponse.getData().get(1).getTitle());
                textView.setText(listBaseResponse.getData().get(1).getTitle());
            }
        });
    }

    private Interceptor provideCacheInterceptor() {

        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response originalResponse = chain.proceed(request);
                String cacheControl = originalResponse.header("Cache-Control");

                if (cacheControl == null || cacheControl.contains("no-store") || cacheControl.contains("no-cache") ||
                        cacheControl.contains("must-revalidate") || cacheControl.contains("max-stale=0")) {


                    CacheControl cc = new CacheControl.Builder()
                            .maxStale(1, TimeUnit.DAYS)
                            .build();

                    /*return originalResponse.newBuilder()
                            .header("Cache-Control", "public, max-stale=" + 60 * 60 * 24)
                            .build();*/


                    request = request.newBuilder()
                            .cacheControl(cc)
                            .build();

                    return chain.proceed(request);

                } else {
                    return originalResponse;
                }
            }
        };

    }


    private Interceptor provideOfflineCacheInterceptor() {

        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                try {
                    return chain.proceed(chain.request());
                } catch (Exception e) {


                    CacheControl cacheControl = new CacheControl.Builder()
                            .onlyIfCached()
                            .maxStale(1, TimeUnit.DAYS)
                            .build();

                    Request offlineRequest = chain.request().newBuilder()
                            .cacheControl(cacheControl)
                            .build();
                    return chain.proceed(offlineRequest);
                }
            }
        };
    }

    private final Interceptor REWRITE_RESPONSE_INTERCEPTOR = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            okhttp3.Response originalResponse = chain.proceed(chain.request());
            String cacheControl = originalResponse.header("Cache-Control");
            if (cacheControl == null || cacheControl.contains("no-store") || cacheControl.contains("no-cache") ||
                    cacheControl.contains("must-revalidate") || cacheControl.contains("max-age=0")) {
                return originalResponse.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, max-age=" + 5000)
                        .build();
            } else {
                return originalResponse;
            }
        }
    };

    private final Interceptor REWRITE_RESPONSE_INTERCEPTOR_OFFLINE = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            if (!CommonUtils.isNetworkConnected(getApplicationContext())) {
                request = request.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, only-if-cached")
                        .build();
            }
            return chain.proceed(request);
        }
    };

}
