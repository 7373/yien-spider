package spider.base.okHttp;

/**
 * @description:
 * @author:
 * @create: 2020-09-29 16:36
 **/
public class OkHttpUtils {

    private static OkHttpRequesterV1 okHttpRequester = new OkHttpRequesterV1();

    public static HttpResponseV1 getResponse(String datum) throws Exception {
        return okHttpRequester.getResponse(datum);
    }

    private OkHttpUtils() {
    }
}
