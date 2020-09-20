package tk.gushizone.spring.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import tk.gushizone.spring.SpringDemoApplication;
import tk.gushizone.spring.async.service.AsyncService;
import tk.gushizone.spring.async.service.SyncService;
import tk.gushizone.spring.transaction.propagation.service.ItemService;
import tk.gushizone.spring.transaction.propagation.service.ItemStockService;
import tk.gushizone.spring.transaction.propagation.service.StockService;

import javax.annotation.Resource;
import java.util.List;

/**
 * Async 测试
 *
 * @author gushizone@gmail.com
 * @date 2020-01-30 18:16
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringDemoApplication.class)
public class AsyncTest {

    @Resource
    SyncService syncService;

    @Test
    public void test() {

        List<?> results = syncService.test();
        System.out.println(results);
    }

}
