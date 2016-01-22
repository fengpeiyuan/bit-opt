package com.fengpeiyuan.biz;

import com.fengpeiyuan.util.BitUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.fengpeiyuan.dao.redis.shard.RedisShard;
import org.apache.log4j.Logger;

public class Operation {
	private RedisShard redisShard;
	final static Logger logger = Logger.getLogger(Operation.class);

	/**
	 * initialization amount
	 * @param goodsId
	 * @param initAmount
	 * @return
	 *
     */
	public Integer stockInit(String goodsId,Integer initAmount) {
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
			logger.error("exception in stock init,",e);
			return -1;
		}
	}

	/**
	 * query remain
	 * @param goodsId
	 * @return
     */
	public Integer stockRemain(String goodsId) {
		try {
			return this.getRedisShard().bitcount(goodsId).intValue();
		}catch (Exception e){
			logger.error("exception when query stock remaink",e);
			return -1;
		}
	}


	/**
	 * deduct
	 * @param goodsId
	 * @return position decuct
	 *
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
		}catch (Exception t){
			logger.error("error happend when deduct",t);
			return -1;
		}
		return offset;
	}

	public static String shaDeductOneInLua;
	public static String scriptDeductOneInLua = "";

	/**
	 *
	 * @param goodsId
	 * @return
     */
	public Integer stockDeductOneInLua(String goodsId) {
		Integer result = -1;
		try {
			if(null == Operation.shaDeductOneInLua){
				Operation.shaDeductOneInLua = this.getRedisShard().scriptLoadSingleShard(goodsId, Operation.scriptDeductOneInLua);
				result = (Integer) this.getRedisShard().evalshaSingleShard(goodsId,Operation.shaDeductOneInLua,0,new String[0]);
			}else{
				result = (Integer) this.getRedisShard().evalshaSingleShard(goodsId,Operation.shaDeductOneInLua,0,new String[0]);
			}

		}catch (Exception t){
			logger.error("error happend when deduct lua",t);
			return -1;
		}
		return result;
	}


	/**
	 * rockback opration
	 * @param goodsId
	 * @param offset
	 * @return
	 *
     */
	public Boolean stockSendbackOne(String goodsId,Integer offset) {
		if(offset<0)
			return Boolean.FALSE;
		try {
			Boolean ori = this.getRedisShard().setbit(goodsId, offset, Boolean.TRUE);
			if (ori == Boolean.TRUE)
				throw new Exception("cannot sendback because not deduct at this position");
			return Boolean.TRUE;
		}catch (Exception e){
			logger.error("exception when stock sendback",e);
			return Boolean.FALSE;
		}
	}


	/**
	 * clean to zero
	 * @param goodsId
	 * @return
	 *
     */
	public Boolean stockClear(String goodsId) {
		try {
			Long ret = this.getRedisShard().del(goodsId);
			if (ret > 0)
				return Boolean.TRUE;
			return Boolean.FALSE;
		}catch (Exception e){
			logger.error("operation fail when clear stock",e);
			return Boolean.FALSE;
		}
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
