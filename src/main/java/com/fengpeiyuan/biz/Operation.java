package com.fengpeiyuan.biz;

import com.fengpeiyuan.util.BitUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.fengpeiyuan.dao.redis.shard.RedisShard;
import com.fengpeiyuan.dao.redis.shard.exception.RedisAccessException;

public class Operation {
	private RedisShard redisShard;

	public String stockInit(String goodsId,Integer initAmount) throws RedisAccessException {
		System.out.println("initAmount:"+initAmount);
		byte[] amountByte = BitUtil.getBytesByAmount(initAmount);

		/* debug print
		for(int i=0;i<amountByte.length;i++) {
			char[] ou = BitUtil.printCharArrayFromByte(amountByte[i]);
			System.out.println(ou);
		}
		 */

		String ret = this.getRedisShard().set(goodsId,amountByte);
		return ret;
	}

	public Long stockRemain(String goodsId) throws RedisAccessException {
		Long ret = this.getRedisShard().bitcount(goodsId);
		return ret;
	}


	public Long stockDeductOne(String goodsId) throws RedisAccessException {
		Boolean ori = Boolean.FALSE;
		long offset = 0L;
		try {
			long amount = this.getRedisShard().bitcount(goodsId);
			while (!ori) {
				offset = this.getRedisShard().bitpos(goodsId,Boolean.TRUE);
				if(amount == offset)
					offset = 0L;
				ori = this.getRedisShard().setbit(goodsId, offset, Boolean.FALSE);
			}
		}catch (Throwable t){
			t.printStackTrace();
			return -1L;
		}
		return offset;
	}

	public Boolean stockSendbackOne(String goodsId,Long offset) throws Exception {
		Boolean ori = this.getRedisShard().setbit(goodsId, offset, Boolean.TRUE);
		if(ori==Boolean.TRUE)
			throw new Exception("cannot sendback because not deduct at this position");
		return Boolean.TRUE;
	}

	public Long stockClear(String goodsId) throws RedisAccessException {
		Long ret = this.getRedisShard().del(goodsId);
		return ret;
	}


	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"spring-config-redis.xml"});
        context.start();
		String goodsId = "a";
        Operation operation = (Operation)context.getBean("operation");

		operation.stockInit(goodsId,10);

		Long remain = operation.stockRemain(goodsId);
        System.out.println("remain:"+remain);

		Long pos = operation.stockDeductOne(goodsId);
		System.out.println("decuct one pos:"+pos);

		Long remain2 = operation.stockRemain(goodsId);
		System.out.println("remain:"+remain2);


		operation.stockSendbackOne(goodsId,pos);

		Long remain3 = operation.stockRemain(goodsId);
		System.out.println("remain:"+remain3);


	}


	
	public RedisShard getRedisShard() {
		return redisShard;
	}

	public void setRedisShard(RedisShard redisShard) {
		this.redisShard = redisShard;
	}
	
	
	
	
}
