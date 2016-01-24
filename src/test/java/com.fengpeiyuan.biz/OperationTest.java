package com.fengpeiyuan.biz;


import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OperationTest {
    private ClassPathXmlApplicationContext context;

    @Before
    public void setup(){
        context = new ClassPathXmlApplicationContext(new String[] {"spring-config-redis.xml"});
        context.start();
    }

    @Test
    public void testStockInit(){
        String goodsId = "a";
        Integer num = 10;
        Operation operation = (Operation)context.getBean("operation");
        Integer ret = operation.stockInit(goodsId,num);
        assertEquals(ret,num);
    }


    @Test
    public void testStockRemain(){
        String goodsId = "a";
        Operation operation = (Operation)context.getBean("operation");
        Integer ret = operation.stockRemain(goodsId);
        assertTrue(ret>=0);
    }

    @Test
    public void testStockDeductOne(){
        String goodsId = "a";
        Operation operation = (Operation)context.getBean("operation");
        Integer ret = operation.stockDeductOne(goodsId);
        assertTrue(ret>=0);
    }

    @Test
    public void testStockDeductOneInLua(){
        String goodsId = "a";
        Operation operation = (Operation)context.getBean("operation");
        Integer ret = operation.stockDeductOneInLua(goodsId);
        assertTrue(ret>=0);
        Integer ret2 = operation.stockDeductOneInLua(goodsId);
        assertTrue(ret2>=0);
    }

    @Test
    public void testStockRefillByNumberInLua(){
        String goodsId = "a";
        Operation operation = (Operation)context.getBean("operation");
        Integer ret = operation.stockRefillByNumber(goodsId,1);
        assertTrue(ret>=0);

    }


    @Test
    public void testStockSendbackOne(){
        String goodsId = "a";
        Operation operation = (Operation)context.getBean("operation");
        Integer initNum = 10;
        Integer initRet = operation.stockInit(goodsId,initNum);
        assertTrue(initRet==initNum);

        Integer deductPos = operation.stockDeductOne(goodsId);
        assertTrue(deductPos>=0);

        Integer remainNum = operation.stockRemain(goodsId);
        assertEquals(remainNum,(Integer)(initNum-1));

        Boolean sendbackRet = operation.stockSendbackOne(goodsId,deductPos);
        assertTrue(sendbackRet);

        Integer remainNum2 = operation.stockRemain(goodsId);
        assertEquals(remainNum2,(Integer)(initNum));
    }


    @Test
    public void testStockClear(){
        String goodsId = "a";
        Operation operation = (Operation)context.getBean("operation");
        Boolean ret = operation.stockClear(goodsId);
        assertTrue(ret);

        Integer remainNum = operation.stockRemain(goodsId);
        assertEquals(remainNum,(Integer) 0);
    }

}
