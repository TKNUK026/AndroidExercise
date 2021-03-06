package com.bigfat.gankio_ca.data.net;

import com.bigfat.gankio_ca.data.entity.DataEntity;
import com.bigfat.gankio_ca.data.entity.DayEntity;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/**
 * api文档: http://gank.io/api
 * Created by yueban on 17:42 23/2/16.
 * Email: fbzhh007@gmail.com
 * QQ: 343278606
 */
public interface GankApi {
    /**
     * 1.分类数据: http://gank.avosapps.com/api/data/数据类型/请求个数/第几页
     *
     * 数据类型： 福利 | Android | iOS | 休息视频 | 拓展资源 | 前端 | all
     * 请求个数： 数字，大于0
     * 第几页：数字，大于0
     * 例：
     * http://gank.avosapps.com/api/data/Android/10/1
     * http://gank.avosapps.com/api/data/福利/10/1
     * http://gank.avosapps.com/api/data/iOS/20/2
     * http://gank.avosapps.com/api/data/all/20/2
     *
     *
     * 2.每日数据： http://gank.avosapps.com/api/day/年/月/日
     * 例：
     * http://gank.avosapps.com/api/day/2015/08/06
     *
     *
     * 3.随机数据：http://gank.avosapps.com/api/random/data/分类/个数
     *
     * 数据类型：福利 | Android | iOS | 休息视频 | 拓展资源 | 前端
     * 个数： 数字，大于0
     * 例：
     * http://gank.avosapps.com/api/random/data/Android/20
     */

    String URL_BASE = "http://gank.avosapps.com/api/";

    @GET("data/{type}/{pageSize}/{pageIndex}")
    Observable<DataEntity> data(@Path(value = "type", encoded = true) String type, @Path("pageSize") int pageSize,
        @Path("pageIndex") int pageIndex);

    @GET("day/{date}")
    Observable<DayEntity> day(@Path(value = "date", encoded = true) String date);

    class Type {
        public static final String BENEFIT = "福利";
        public static final String ANDROID = "Android";
        public static final String IOS = "iOS";
        public static final String REST = "休息视频";
        public static final String EXTEND = "拓展资源";
        public static final String FRONTPAEG = "前端";
        public static final String ALL = "all";
    }
}
