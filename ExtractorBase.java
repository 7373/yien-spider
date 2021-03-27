package spider.base;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import logUtils.LogUtils2;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

@Data
@Deprecated
public class ExtractorBase<E> implements TaskFace {
    protected Object content;
    LogUtils2 LOG = new LogUtils2(ExtractorBase.class);

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        this.etractor(content);
        long endTime = System.currentTimeMillis();
        float seconds = (endTime - startTime) / 1000F;
        LOG.displayFile(Thread.currentThread().getName() + "解析线程运行时间：" + seconds + "秒", false);
    }

    protected List<E> etractor(final Object content) {
        List<E> goodsList = null;
        if (content instanceof String) {
            try {
                JSONArray jsonObject = JSONObject.parseObject(((String) content).trim()).getJSONObject("data").getJSONArray("list");
                goodsList = (List<E>) jsonObject.toJavaList(getClass());
            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            }
            LOG.displayFile(goodsList);
            return goodsList;
        }
        return goodsList;
    }


    protected <T> T etractor(Object V,Function<Object, T> etract){
        return etract.apply(V);
    }


}
