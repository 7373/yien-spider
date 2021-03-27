package spider.base;

import logUtils.Log4jUtil;
import logUtils.LogUtils2;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StopWatch;

/**
 * @description:
 * @author:
 * @create: 2020-07-24 14:48
 **/
public abstract class BaseSpiderRun implements TaskFace {

    protected Logger lOGGER = Log4jUtil.getLogger(BaseSpiderRun.class);
//    protected LogUtils2 lOGGER = new LogUtils2(BaseSpiderRun.class,false, true);



    @Override
    public void run() {
        StopWatch sw = new StopWatch("爬取线程,id：" + Thread.currentThread().getId());
        sw.start("BaseSpiderRun task");
//        lOGGER.info(Thread.currentThread().getName() + "爬取线程运行开始::");
//        long startTime = System.currentTimeMillis();
        this.execute();
//        long endTime = System.currentTimeMillis();
//        float seconds = (endTime - startTime) / 1000F;
        sw.stop();
//        lOGGER.info(Thread.currentThread().getName() + "爬取线程运行时间：" + seconds + "秒");
        lOGGER.info(sw.prettyPrint());
//        System.gc();
    }

    protected abstract void execute();

}
