/*
 * Copyright (C) 2017 hu
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package spider.base.okHttp;

import logUtils.Log4jUtil;
import okhttp3.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * @author hu
 */

/**
 * 执行流程：
 * 封装Request。
 * 封装RealCall提交到调度器Dispatcher去执行
 * 将结果返回
 */
public class OkHttpRequesterV1 {

    protected OkHttpClient client;
    protected HashSet<Integer> successCodeSet;
    protected OkConfiguration okConfiguration = OkConfiguration.getDefault();

    public OkHttpRequesterV1 addSuccessCode(int successCode) {
        successCodeSet.add(successCode);
        return this;
    }

    public OkHttpRequesterV1 removeSuccessCode(int successCode) {
        successCodeSet.remove(successCode);
        return this;
    }

    protected HashSet<Integer> createSuccessCodeSet() {
        /**
         * 响应码见：HttpURLConnection
         */
        HashSet<Integer> result = new HashSet<>();
        /**
         * 200 301 302 404
         */
        result.add(HttpURLConnection.HTTP_OK);
        result.add(HttpURLConnection.HTTP_MOVED_PERM);
        result.add(HttpURLConnection.HTTP_MOVED_TEMP);
        result.add(HttpURLConnection.HTTP_NOT_FOUND);
        /**
         * An {@code int} representing the three digit HTTP Status-Code.
         * <ul>
         * <li> 1xx: Informational
         * <li> 2xx: Success
         * <li> 3xx: Redirection
         * <li> 4xx: Client Error
         * <li> 5xx: Server Error
         * </ul>
         */
        return result;
    }


    public OkHttpClient.Builder createOkHttpClientBuilder() {
        /**
         HTTPS配置：
         HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory(证书的inputstream, null, null);
         OkHttpClient okHttpClient = new OkHttpClient.Builder()
         .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager))
         //其他配置
         .build();
         */
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(okConfiguration.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(okConfiguration.getReadTimeout(), TimeUnit.MILLISECONDS);
        return builder;
    }

    //    public Request.Builder createRequestBuilder(CrawlDatum crawlDatum) {
//        Request.Builder builder = new Request.Builder()
//                .header("User-Agent", okConfiguration.getDefaultUserAgent())
//                .url(crawlDatum.url());
//
//        String defaultCookie = okConfiguration.getDefaultCookie();
//        if (defaultCookie != null) {
//            builder.header("Cookie", defaultCookie);
//        }
//        return builder;
//    }

    /**
     * 其他请求方式
     */
    public Request.Builder createRequestBuilderV2(String url) {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", "")
                .build();
        Request.Builder builder = createRequestBuilder(url).post(requestBody);
        return builder;
    }

    public Request.Builder createRequestBuilder(String url) {

        Request.Builder builder = new Request.Builder()
                .header("User-Agent", okConfiguration.getDefaultUserAgent())
                .url(url);
        String defaultCookie = okConfiguration.getDefaultCookie();
        if (defaultCookie != null) {
            builder.header("Cookie", defaultCookie);
        }
        return builder;
    }

    public OkHttpRequesterV1() {
        successCodeSet = createSuccessCodeSet();
        client = createOkHttpClientBuilder().build();
    }

    public OkHttpRequesterV1(String url) {
        successCodeSet = createSuccessCodeSet();
        client = createOkHttpClientBuilder().build();
    }

    /**
     * @param [datum, callback]
     * @return spider.base.okHttp.HttpResponseV1
     * @description AsyncCall 异步
     * @author
     * @when 2020/9/30 10:49
     * @see getResponse
     */
    public HttpResponseV1 getResponse(String datum, Callback callback) {
        Request request = createRequestBuilder(datum).build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        HttpResponseV1 page = new HttpResponseV1();

        return page;
    }

    /**
     * 同步
     */
    public HttpResponseV1 getResponse(String datum) throws Exception {
        Request request = createRequestBuilder(datum).build();

        Call call = client.newCall(request);
        Response response = call.execute();

        String contentType = null;
//        byte[] contentAt = null;
        String charset = null;
        String html = null;
        ResponseBody responseBody = response.body();
        try {
            int code = response.code();
            //设置重定向地址
//            datum.code(code);
//            datum.location(response.header("Location"));

            if (!successCodeSet.contains(code)) {
//            throw new IOException(String.format("Server returned HTTP response code: %d for URL: %s (CrawlDatum: %s)", code,crawlDatum.url(), crawlDatum.key()));
//            throw new IOException(String.format("Server returned HTTP response code: %d for %s", code, crawlDatum.briefInfo()));
                throw new IOException(String.format("Server returned HTTP response code: %d", code));
            }
            if (responseBody != null) {
                //                contentAt = responseBody.bytes();
                html = responseBody.string();
                MediaType mediaType = responseBody.contentType();
                if (mediaType != null) {
                    contentType = mediaType.toString();
                    Charset responseCharset = mediaType.charset();
                    if (responseCharset != null) {
                        charset = responseCharset.name();
                    }
                }
            }
            HttpResponseV1 page = new HttpResponseV1();
            page.setHtml(html);
            page.setCharset(charset);
            page.setObj(response);
            page.setContentType(contentType);
            return page;
        } catch (IOException e) {
            Log4jUtil.error(OkHttpRequesterV1.class, "IOException...可能会重试...");
            Log4jUtil.error(OkHttpRequesterV1.class, e);
            throw e;
        } finally {
            if (responseBody != null) {
                responseBody.close();
            }
        }
    }

    public HashSet<Integer> getSuccessCodeSet() {
        return successCodeSet;
    }

    public void setSuccessCodeSet(HashSet<Integer> successCodeSet) {
        this.successCodeSet = successCodeSet;
    }
}
