package spider.base;

import algorithmUtils.lru.LruLinkedHashSet;
import com.enniu.crawler.encreeper.common.util.StringUtils;
import com.google.common.collect.Maps;
import lockUtils.ReentrantLockUtils;
import logUtils.Log4jUtil;
import logUtils.LogUtils2;
import lombok.NoArgsConstructor;
import okhttp3.*;
import org.apache.commons.codec.digest.DigestUtils;
import spider.base.okHttp.HttpResponseV1;
import spider.base.okHttp.OkConfiguration;
import spider.base.okHttp.OkHttpRequesterV1;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @description: 爬虫构建器V2 版本 通过okHttp
 * @author: yien
 * @create: 2020-05-21 10:45
 **/
@NoArgsConstructor
@NotThreadSafe
public final class HttpSpiderBuilderV2 {
    protected OkHttpClient okHttpClient;
    protected HashSet<Integer> successCodeSet;

    protected Request request;
    private Request defualtRequestBase;
    private Boolean filter = Boolean.FALSE;
    private Class<?> nowClass;
    private LogUtils2 lOGGER = new LogUtils2(HttpSpiderBuilderV2.class, false, true);
    private ThreadLocal<String> activeContext = new ThreadLocal<>();
    /**
     * V1  private static Set<String> DEFAULT_FILTER_KEY_SET = Sets.newConcurrentHashSet();
     * v2 避免无限增长
     * v3 不同的过滤集
     */
    private static final LruLinkedHashSet<String> DEFAULT_FILTER_KEY_SET = new LruLinkedHashSet<>(6000);
    private static final Map<String, LruLinkedHashSet<String>> FILTER_KEY_MAP = Maps.newConcurrentMap();

    private OkConfiguration okConfiguration = OkConfiguration.getDefault();

    /**
     * @param key
     * @return
     * @description
     * @author ahran
     * @when 2020/5/21 10:49
     * @see
     */
    private static LruLinkedHashSet<String> getFilterKeySet(String key) {
        /*** 以下操作：避免重复插入  或者用双重验证锁***/
        ReentrantLock lock = ReentrantLockUtils.getLock(HttpSpiderBuilderV2.class, "getFilterKeySet");
        LruLinkedHashSet<String> result;
        lock.lock();
        try {
            result = FILTER_KEY_MAP.get(key);
            if (null == result) {
                result = new LruLinkedHashSet<>(3000);
                FILTER_KEY_MAP.put(key, result);
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    private static LruLinkedHashSet<String> getFilterKeySet(Class<?> key) {
        if (null == key || null == key.getName()) {
            return DEFAULT_FILTER_KEY_SET;
        } else {
            return getFilterKeySet(key.getName());
        }
    }


    public static HttpSpiderBuilderV2 create() {
        return new HttpSpiderBuilderV2().setSuccessCodeSet(createSuccessCodeSet());
    }


    /**
     * @param requestBase
     * @return
     * @description
     * @author ahran
     * @when 2020/5/21 10:52
     * @see
     */
    public HttpResponseV1 execute(Request requestBase, final Boolean filter) {
        if (null == requestBase) {
            requestBase = defualtRequestBase;
        }
        String url = null;
        if (requestBase != null) {
            url = requestBase.url().toString().toLowerCase();
        }
        if (StringUtils.isBlank(url)) {
            return null;
        }
        url = url.trim();
        LruLinkedHashSet<String> filterSet = null;
        if (null != filter && filter) {
            /*** 取出唯一的集合 ***/
            filterSet = getFilterKeySet(this.nowClass);
            if (filterSet.contains(DigestUtils.md5Hex(url))) {
                return null;
            }
        }
        HttpResponseV1 contentResult = null;
        try {
            if (null == this.okHttpClient) {
                this.setOkHttpClient(createOkHttpClientBuilder().build());
            }
            contentResult = this.excuteRequest(this.okHttpClient, requestBase);
            if (null != contentResult) {
                /**
                 * 空着
                 */
//                activeContext.set(contentResult);
            }
            /**
             * 增加从连接池取出httpclient
             */
            if (null != filterSet) {
                filterSet.put(DigestUtils.md5Hex(url));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log4jUtil.error(this, e);
            activeContext.remove();
        }
        return contentResult;
    }


    public HttpResponseV1 execute(final Boolean filter) {
        return HttpSpiderBuilderV2.this.execute(this.request, filter);
    }

    public HttpResponseV1 execute(final Request requestBase) {
        return HttpSpiderBuilderV2.this.execute(requestBase, this.filter);
    }

    public HttpResponseV1 execute() {
        return HttpSpiderBuilderV2.this.execute(this.request, this.filter);
    }

    public HttpSpiderBuilderV2 setNowClass(final Class<?> nowClass) {
        this.nowClass = nowClass;
        return HttpSpiderBuilderV2.this;

    }


    public HttpSpiderBuilderV2 setRequest(final Request request) {
        this.request = request;
        return HttpSpiderBuilderV2.this;
    }


    public HttpSpiderBuilderV2 setSuccessCodeSet(HashSet<Integer> successCodeSet) {
        this.successCodeSet = successCodeSet;
        return HttpSpiderBuilderV2.this;
    }

    public HttpSpiderBuilderV2 setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
        return HttpSpiderBuilderV2.this;
    }

    public HttpSpiderBuilderV2 setFilter(final Boolean filter) {
        this.filter = filter;
        /**
         *
         * ①当在一个类的内部类中，如果需要访问外部类的方法或者成员域的时候，如果使用
         * this.成员域(与 内部类.this.成员域 没有分别) 调用的显然是内部类的域 ， 如果我们想要访问外部类的域的时候，就要必须使用  外部类.this.成员域
         */
        return HttpSpiderBuilderV2.this;
    }


    /**
     * 执行请求入口 同步
     *
     * @param httpClient
     * @param request
     * @return
     */
    public HttpResponseV1 excuteRequest(final OkHttpClient httpClient, final Request request) throws Exception {


        Call call = httpClient.newCall(request);
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
            page.setCharset(charset);
            page.setHtml(html);
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

    /**
     * 执行请求入口 异步
     *
     * @param httpClient
     * @param request
     * @return
     */
    public HttpResponseV1 excuteRequest(final OkHttpClient httpClient, final Request request, final Callback callback) throws Exception {

        Call call = httpClient.newCall(request);
        call.enqueue(callback);
        HttpResponseV1 page = new HttpResponseV1();
        page.setStatus(200);
        return page;
    }

    protected OkHttpClient.Builder createOkHttpClientBuilder() {
        /**
         HTTPS配置：
         HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory(证书的inputstream, null, null);
         OkHttpClient okHttpClient = new OkHttpClient.Builder()
         .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager))
         //其他配置
         .build();
         */
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8888));

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(okConfiguration.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(okConfiguration.getReadTimeout(), TimeUnit.MILLISECONDS)
                /**
                 *         String proxyHost = "proxy.abuyun.com";
                 *         int proxyPort = 9020;
                 *         Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                 */

//                .proxy(proxy)


                ;

        return builder;
    }

    protected static HashSet<Integer> createSuccessCodeSet() {
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
        return result;
    }
}
