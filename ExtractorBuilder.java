package spider.base;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import logUtils.LogUtils2;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @description: 解析创建者模式
 * 泛型+构造器模式
 * lombok中使用@Builder
 * @author:
 * @create: 2020-06-16 16:13
 **/


public class ExtractorBuilder<IN, OUT> {
    /*** 初始化类型 ***/
//    private Class<IN> clazzIn;
//    private Class<OUT> clazzOut;
    /*** 输入参数 ***/
    private IN pageResult;
    /*** 转换函数 ***/
    private Function<? super IN, ? super OUT> convert;
    /*** 过滤结果集函数 ***/
    private Function<? super OUT, ? super OUT> filter;
    /*** 传输函数：数据持久化处理等 ***/
    private Consumer<? super OUT> pipeline;
    private LogUtils2 LOGGER = new LogUtils2(ExtractorBuilder.class, false, true);

//    @Deprecated
//    private ExtractorBuilder(Class<IN> clazzIn, Class<OUT> clazzOut) {
//        this.clazzIn = clazzIn;
//        this.clazzOut = clazzOut;
//    }

    private ExtractorBuilder() {
    }

    public static <IN, OUT> ExtractorBuilder<IN, OUT> create() {
        return new ExtractorBuilder<IN, OUT>();
    }

    public static <IN, OUT> ExtractorBuilder<IN, OUT> create(IN in, OUT out) {
        return new ExtractorBuilder<IN, OUT>();
    }

//    @Deprecated
//    private static <IN, OUT> ExtractorBuilder<IN, OUT> create(Class<IN> in, Class<OUT> out) {
//        return new ExtractorBuilder<>(in, out);
//    }

    public ExtractorBuilder<IN, OUT> setPageResult(IN pageResult) {
        this.pageResult = pageResult;
        return this;
    }

    public ExtractorBuilder<IN, OUT> setConvert(Function<? super IN, ? super OUT> convert) {
        this.convert = convert;
        return this;
    }


    public ExtractorBuilder<IN, OUT>  setFilter(Function<? super OUT, ? super OUT> filter) {
        this.filter = filter;
        return this;
    }

    public ExtractorBuilder<IN, OUT> setPipeline(Consumer<? super OUT> pipeline) {
        this.pipeline = pipeline;
        return this;
    }


    /**
     * @param pageResult
     * @param excuteIn2out
     * @param pipeline
     * @return
     * @description 最终执行函数
     * @when 2020/6/16 16:36
     * @see
     */
    @SuppressWarnings("unchecked")
    public OUT extractor(final IN pageResult,final  Function<? super IN, ? super OUT> excuteIn2out,final Function<? super OUT, ? super OUT> filter, Consumer<? super OUT> pipeline) {
        OUT out = null;
        if (null == pageResult) {
            return (OUT) null;
        }
        /**
         * 清洗
         */
        if (null != excuteIn2out) {
            out = (OUT) excuteIn2out.apply(pageResult);
        }
        /**
         * 过滤
         */
        if (null != filter) {
            out = (OUT) filter.apply(out);
        }
        /**
         * 持久化
         */
        if (null != pipeline) {
            pipeline.accept(out);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public OUT extractor() {
        return extractor(this.pageResult, this.convert,this.filter, this.pipeline);
    }

    @SuppressWarnings("unchecked")
    public OUT extractor(final IN pageResult) {
        return extractor(pageResult, this.convertFunction, this.filter,this.pipeline);

    }


    public OUT extractor(final IN pageResult, final Function<IN, OUT> excuteIn2out) {
        return extractor(pageResult, excuteIn2out, this.filter,this.pipeline);

    }
    public OUT extractor(final IN pageResult,final  Function<IN, OUT> excuteIn2out,final Function<? super OUT, ? super OUT> filter) {
        return extractor(pageResult, excuteIn2out, filter,this.pipeline);

    }
    /**
     * 默认函数
     */

    /*** 转换函数 ***/
    @SuppressWarnings("unchecked")
    public Function<IN, OUT> convertFunction = in -> {
        /*** List<E> : OUT ***/
        OUT goodsList = null;
        if (in instanceof String) {
            try {
                JSONArray jsonObject = JSONObject.parseObject(((String) in).trim()).getJSONObject("data").getJSONArray("list");
                goodsList = (OUT) jsonObject.toJavaList(in.getClass());
            } catch (Throwable e) {
                e.printStackTrace();
                LOGGER.error(e);
                return null;
            }
            return goodsList;
        }
        return goodsList;
    };
    /*** 持久化函数 ***/
    public Consumer<OUT> pipelineConsumer = System.out::println;

}
