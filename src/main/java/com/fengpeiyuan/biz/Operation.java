package com.fengpeiyuan.biz;

import com.fengpeiyuan.util.BitUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.fengpeiyuan.dao.redis.shard.RedisShard;
import com.fengpeiyuan.dao.redis.shard.exception.RedisAccessException;

public class Operation {
	private RedisShard redisShard;

	/**
	 * initialization amount
	 * @param goodsId
	 * @param initAmount
	 * @return
	 * @throws Exception
     */
	public Integer stockInit(String goodsId,Integer initAmount) throws Exception {
		if(initAmount<0)
			return 0;

		try {
			byte[] amountByte = BitUtil.getBytesByAmount(initAmount);

		/* debug print
		for(int i=0;i<amountByte.length;i++) {
			char[] ou = BitUtil.printCharArrayFromByte(amountByte[i]);
			System.out.println(ou);
		}
		 */

			this.getRedisShard().set(goodsId, amountByte);
			return this.getRedisShard().bitcount(goodsId).intValue();

		}catch (Exception e){
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * query remain
	 * @param goodsId
	 * @return
	 * @throws Exception
     */
	public Integer stockRemain(String goodsId) throws Exception {
		return this.getRedisShard().bitcount(goodsId).intValue();
	}


	/**
	 * deduct
	 * @param goodsId
	 * @return
	 * @throws Exception
     */
	public Integer stockDeductOne(String goodsId) {
		Boolean ori = Boolean.FALSE;
		Integer offset = 0;
		try {
			long remain = this.getRedisShard().bitcount(goodsId);
			if(remain<=0)
				return -1;
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

	/**
	 * rockback opration
	 * @param goodsId
	 * @param offset
	 * @return
	 * @throws Exception
     */
	public Boolean stockSendbackOne(String goodsId,Integer offset) throws Exception {
		Boolean ori = this.getRedisShard().setbit(goodsId, offset, Boolean.TRUE);
		if(ori==Boolean.TRUE){
			
			return Boolean.FALSE;
		}

		return Boolean.TRUE;
	}


	/**
	 * clean to zero
	 * @param goodsId
	 * @return
	 * @throws RedisAccessException
     */
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
