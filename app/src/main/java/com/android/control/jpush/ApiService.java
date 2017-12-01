package com.android.control.jpush;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Created by dulong on 2017/11/28.
 */

public interface ApiService {
    @POST
    @FormUrlEncoded
    Observable<String> onLine(@Url String url, @Field("userMsg") String userMsg);
}
