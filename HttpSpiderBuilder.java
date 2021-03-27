package spider.base;

import algorithmUtils.lru.LruLinkedHashSet;
import cn.hutool.crypto.digest.DigestUtil;
import com.enniu.crawler.encreeper.common.util.StringUtils;
import com.google.common.collect.Maps;
import lockUtils.ReentrantLockUtils;
import logUtils.Log4jUtil;
import logUtils.LogUtils2;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @description: 爬虫构建器
 * @author: yien
 * @create: 2020-05-21 10:45
 **/
@NoArgsConstructor
@NotThreadSafe
public final class HttpSpiderBuilder {
    private HttpClient httpClient;
    private HttpRequestBase requestBase;
    private String url;
    private HttpRequestBase defualtRequestBase;
    private Boolean filter = Boolean.FALSE;
    private Class<?> nowClass;
    private LogUtils2 LOGGER = new LogUtils2(HttpSpiderBuilder.class, false, true);
    private ThreadLocal<String> activeContext = new ThreadLocal<>();
    /**
     * V1  private static Set<String> DEFAULT_FILTER_KEY_SET = Sets.newConcurrentHashSet();
     * v2 避免无限增长
     * v3 不同的过滤集
     */
    private static final LruLinkedHashSet<String> DEFAULT_FILTER_KEY_SET = new LruLinkedHashSet<>(6000);
    private static final Map<String, LruLinkedHashSet<String>> FILTER_KEY_MAP = Maps.newConcurrentMap();

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
        ReentrantLock lock = ReentrantLockUtils.getLock(HttpSpiderBuilder.class, "getFilterKeySet");
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

    /*** 考虑连接池怎么用 ***/
    private HttpSpiderBuilder(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static HttpSpiderBuilder create() {
        return new HttpSpiderBuilder();
    }

    /**
     * @param
     * @return
     * @description
     * @author ahran
     * @when 2020/5/21 10:48
     * @see
     */
    public static HttpSpiderBuilder createHttp() {
        return createHttp(true);
    }

    public static HttpSpiderBuilder createHttp(boolean needRetry) {
        return new HttpSpiderBuilder(HttpClientCreateUtil.getHttpClient(needRetry));
    }


    /**
     * @param
     * @return
     * @description
     * @author ahran
     * @when 2020/5/21 10:49
     * @see
     */
    public static HttpSpiderBuilder createHttps() {
        return new HttpSpiderBuilder(HttpClientCreateUtil.getHttpsClient());
    }


    /**
     * @param requestBase
     * @return
     * @description
     * @author ahran
     * @when 2020/5/21 10:52
     * @see
     */
    public String execute(HttpRequestBase requestBase, final Boolean filter) {
        if (null == requestBase) {
            requestBase = defualtRequestBase;
        }
        String url = requestBase.getURI().toString().toLowerCase();
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
        String contentResult = null;
        try {
            if (null == this.httpClient) {
                if (url.startsWith(HTTPS)) {
                    this.setHttpClient(HttpClientCreateUtil.getHttpsClient());
                } else {
                    this.setHttpClient(HttpClients.createDefault());
                }
            }
            contentResult = this.excuteRequest(this.httpClient, requestBase);
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
            LOGGER.error(e);
            activeContext.remove();
        }
        return contentResult;
    }

    private static final String HTTPS = "https";

    public String execute(final Boolean filter) {
        return HttpSpiderBuilder.this.execute(this.requestBase, filter);
    }

    public String execute(final HttpRequestBase requestBase) {
        return HttpSpiderBuilder.this.execute(requestBase, this.filter);
    }

    public String execute() {
        return HttpSpiderBuilder.this.execute(this.requestBase, this.filter);
    }

    public HttpSpiderBuilder setNowClass(final Class<?> nowClass) {
        this.nowClass = nowClass;
        return HttpSpiderBuilder.this;

    }

    public HttpSpiderBuilder setRequestBaseGet(final String url) {
        HttpGet httpGet = HttpClientUtils.createHttpGet(url, null);
        return this.setRequestBase(httpGet);
    }

    public HttpSpiderBuilder setHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
        return HttpSpiderBuilder.this;
    }

    public HttpSpiderBuilder setRequestBase(final HttpRequestBase requestBase) {
        this.requestBase = requestBase;
        return HttpSpiderBuilder.this;
    }

    public HttpSpiderBuilder setUrl(String url) {
        this.url = url;
        if (null != this.requestBase) {
            this.requestBase.setURI(URI.create(url));
        } else if (null == this.defualtRequestBase) {
            this.defualtRequestBase = HttpClientUtils.createHttpGet(url, null);
        } else {
            this.defualtRequestBase.setURI(URI.create(url));
        }
        return HttpSpiderBuilder.this;
    }

    public HttpSpiderBuilder setFilter(final Boolean filter) {
        this.filter = filter;
        /**
         *
         * ①当在一个类的内部类中，如果需要访问外部类的方法或者成员域的时候，如果使用
         * this.成员域(与 内部类.this.成员域 没有分别) 调用的显然是内部类的域 ， 如果我们想要访问外部类的域的时候，就要必须使用  外部类.this.成员域
         */
        return HttpSpiderBuilder.this;
    }

    private static final Charset EN_CODING = StandardCharsets.UTF_8;

    /**
     * 执行请求入口
     *
     * @param httpClient
     * @param requestBase
     * @return
     */
    public String excuteRequest(final HttpClient httpClient, final HttpRequestBase requestBase) {
        HttpEntity httpEntity = null;
        String response = null;
        try {
            HttpResponse htpResponse = httpClient.execute(requestBase);
            httpEntity = htpResponse
                    .getEntity();
            if (null != httpEntity) {
                //按指定编码转换结果实体为String类型
                response = EntityUtils.toString(httpEntity, EN_CODING);
            }
        } catch (SSLHandshakeException e) {
            Log4jUtil.error(HttpClientUtils.class, "握手失败/更换套接字协议........");
            Log4jUtil.error(HttpClientUtils.class, e);
            e.printStackTrace();
//            throw e;
        } catch (UnknownHostException e) {
            Log4jUtil.error(HttpClientUtils.class, "Unknown host error....");
            Log4jUtil.error(HttpClientUtils.class, e);
            e.printStackTrace();
//            throw e;
        } catch (SocketException e) {
            if (e instanceof HttpHostConnectException) {
                System.out.println("代理不可用");
                Log4jUtil.error(HttpClientUtils.class, "代理不可用...");
            }
            e.printStackTrace();
            Log4jUtil.error(HttpClientUtils.class, "Socket连接断开...重试3次...");
            Log4jUtil.error(HttpClientUtils.class, e);
        } catch (IOException e) {
            e.printStackTrace();
            Log4jUtil.error(HttpClientUtils.class, "IOException...可能会重试...");
            Log4jUtil.error(HttpClientUtils.class, e);
        } catch (Throwable e) {
            e.printStackTrace();
            Log4jUtil.error(HttpClientUtils.class, e);
//            throw e;
        }
//        finally {
//            System.gc();
        //
//            Log4jUtil.debug(this,"结束请求{}",requestBase.getURI().toString());
//        }
        return response;
    }

}
