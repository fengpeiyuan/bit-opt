package com.fengpeiyuan.biz;

import com.fengpeiyuan.util.BitUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.fengpeiyuan.dao.redis.shard.RedisShard;
import com.fengpeiyuan.dao.redis.shard.exception.RedisAccessException;

public class Operation {
	private RedisShard redisShard;

	public Integer stockInit(String goodsId,Integer initAmount) throws Exception {
		byte[] amountByte = BitUtil.getBytesByAmount(initAmount);

		/* debug print
		for(int i=0;i<amountByte.length;i++) {
			char[] ou = BitUtil.printCharArrayFromByte(amountByte[i]);
			System.out.println(ou);
		}
		 */

		this.getRedisShard().set(goodsId,amountByte);
		Integer remain = this.getRedisShard().bitcount(goodsId).intValue();
		return remain;
	}

	public Integer stockRemain(String goodsId) throws Exception {
		return this.getRedisShard().bitcount(goodsId).intValue();
	}


	public Integer stockDeductOne(String goodsId) throws Exception {
		Boolean ori = Boolean.FALSE;
		Integer offset = 0;
		try {
			long remain = this.getRedisShard().bitcount(goodsId);
			while (!ori) {
				offset = this.getRedisShard().bitpos(goodsId,Boolean.TRUE).intValue();
				if(remain == offset)
					offset = 0;
				ori = this.getRedisShard().setbit(goodsId, offset, Boolean.FALSE);
			}
		}catch (Throwable t){
			t.printStackTrace();
			return -1;
		}
		return offset;
	}

	public Boolean stockSendbackOne(String goodsId,Integer offset) throws Exception {
		Boolean ori = this.getRedisShard().setbit(goodsId, offset, Boolean.TRUE);
		if(ori==Boolean.TRUE)
			throw new Exception("cannot sendback because not deduct at this position");
		return Boolean.TRUE;
	}

	public Boolean stockClear(String goodsId) throws RedisAccessException {
		Long ret = this.getRedisShard().del(goodsId);
		if(ret>0)
			return Boolean.TRUE;
		return Boolean.FALSE;
	}


	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"spring-config-redis.xml"});
        context.start();
		String goodsId = "a";
        Operation operation = (Operation)context.getBean("operation");

		operation.stockInit(goodsId,10);

		Integer remain = operation.stockRemain(goodsId);
        System.out.println("remain:"+remain);

		Integer pos = operation.stockDeductOne(goodsId);
		System.out.println("decuct one pos:"+pos);

		Integer remain2 = operation.stockRemain(goodsId);
		System.out.println("remain:"+remain2);

		operation.stockSendbackOne(goodsId,pos);

		Integer remain3 = operation.stockRemain(goodsId);
		System.out.println("remain:"+remain3);

		operation.stockClear(goodsId);

		Integer remain4 = operation.stockRemain(goodsId);
		System.out.println("remain:"+remain4);
	}


	
	public RedisShard getRedisShard() {
		return redisShard;
	}

	public void setRedisShard(RedisShard redisShard) {
		this.redisShard = redisShard;
	}
	
	
	
	
}
