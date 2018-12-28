package com.jerryzhu.retrofitofflinecaching.net;

import com.jerryzhu.retrofitofflinecaching.bean.BannerData;
import com.jerryzhu.retrofitofflinecaching.bean.BaseResponse;
import com.jerryzhu.retrofitofflinecaching.bean.FeedArticleListData;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface APIService {


    String BASE_URL = "http://www.wanandroid.com/";

    @GET("article/list/{num}/json")
    Observable<BaseResponse<FeedArticleListData>> getRandomJoke(@Path("num") int num);


    @GET("banner/json")
    Observable<BaseResponse<List<BannerData>>> getBannerData();
}

