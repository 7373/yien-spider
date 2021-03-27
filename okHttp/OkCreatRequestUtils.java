package spider.base.okHttp;


import com.squareup.okhttp.FormEncodingBuilder;
import okhttp3.*;
import org.apache.commons.collections.MapUtils;
import org.assertj.core.util.Maps;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author:
 * @create: 2020-11-02 10:32
 **/
public class OkCreatRequestUtils {


    /**
     * 构造post请求  FormEncodingBuilder 表单
     *
     * @param url    请求url
     * @param params 请求的参数
     * @return 返回 Request
     */
    public static Request buildPostRequest(final Map<String, String> headers, final String url, final Map<String, String> params
    ) {
//        FormEncodingBuilder builder = new FormEncodingBuilder();
//
//        for (Map.Entry<String, String> param : params.entrySet()) {
//            builder.add(param.getKey(), param.getValue());
//        }
        MultipartBody.Builder multiBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        for (Map.Entry<String, String> param : params.entrySet()) {
            multiBuilder.addFormDataPart(param.getKey(), param.getValue());
        }
        RequestBody requestBody = multiBuilder.build();
        /**
         * 请求头
         */
        Headers header;
        if (MapUtils.isEmpty(headers)) {
            header = Headers.of(OkConfiguration.getDefault().data2);
        } else {
            header = Headers.of(headers);
        }
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .headers(header)
                .build();
    }


    private static final MediaType CONTENT_TYPE =
            MediaType.parse("application/x-www-form-urlencoded");
    public static final Charset UTF_8 = StandardCharsets.UTF_8;


    public static Request buildPostRequest(final Map<String, String> headers, final String url,
                                            final String content) {

        /**
         * RequestBody创建方式1  RequestBody.create
         * 方式2  new MultipartBody.Builder()
         */
        /*** UTF_8 ***/
        RequestBody requestBody = RequestBody.create(CONTENT_TYPE, content.getBytes(UTF_8));
        /**
         * 请求头 new MultipartBody.Builder()
         */
        Headers header;
        if (MapUtils.isEmpty(headers)) {
            Map<String, String> headerss= OkConfiguration.getDefault().data2;
//            headerss.put("cookie","tk=78397966927eb7a869cab39a65abfdd7198527ea;uid=281713746206825088;lon=120.2127460241039;idfa=00000000-0000-0000-0000-000000000000;brand=Apple;PPU=\"TT=b56b25c9bedb3a1eec6a2eefc40bdd453bc40503&UID=281713746206825088&SF=ZHUANZHUAN&SCT=1604138892967&V=1&ET=1606727292967\"; Version=1; Domain=zhuanzhuan.com; Max-Age=2592000; Expires=Mon, 30-Nov-2020 09:48:57 GMT;;lat=30.18355878772119;t=16;sts=1604134757000;model=iPhone10%2C1;v=8.1.6;zz_t=16;");
            header = Headers.of(headerss);
        } else {
            header = Headers.of(headers);
        }
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .headers(header)
                .build();
    }

    /**
     * 其他请求方式
     */
    public okhttp3.Request.Builder createRequestBuilderV2(String url) {

        okhttp3.RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", "")
                .build();

        /**
         * FormBody
         * FileRequestBody
         * MultipartBody
         */
        okhttp3.RequestBody requestBody2 = new FormBody.Builder()

                .add("", "")
                .build();


        okhttp3.Request.Builder builder = createRequestBuilder(url).post(requestBody);
        return builder;
    }

    public okhttp3.Request.Builder createRequestBuilder(String url) {

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .header("User-Agent", "")
                .url(url);
        String defaultCookie = " ";
        if (defaultCookie != null) {
            builder.header("Cookie", defaultCookie);
        }
        return builder;
    }


    private OkCreatRequestUtils() {
    }
}
