package com.fengpeiyuan.biz;

import com.fengpeiyuan.util.BitUtil;
import org.apache.log4j.pattern.IntegerPatternConverter;
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


	/**
	 *
	 * @param goodsId
	 * @return
     */
	private static String shaDeductOneInLua;
	private static String scriptDeductOneInLua = "local offset = redis.call('bitpos',KEYS[1],'1') local ret = redis.call('setbit',KEYS[1],offset,'0') if ret == 1 then return offset else return -1 end ";
	/*eval "local offset = redis.call('bitpos',KEYS[1],'1') local ret = redis.call('setbit',KEYS[1],offset,'0') if ret ==1 then return offset else return -1 end " 1 a */

	public Integer stockDeductOneInLua(String goodsId) {
		Integer result = -1;
		try {
			if(null == Operation.shaDeductOneInLua){
				if(this.getRedisShard().scriptExistsSingleShard(goodsId,"36c4584b8f58b58c1abf89a50fcf223dddf34537")){
					Operation.shaDeductOneInLua = "36c4584b8f58b58c1abf89a50fcf223dddf34537";
					result = ((Long)this.getRedisShard().evalshaSingleShard(goodsId,Operation.shaDeductOneInLua,1,goodsId)).intValue();
				}else {
					Operation.shaDeductOneInLua = this.getRedisShard().scriptLoadSingleShard(goodsId, Operation.scriptDeductOneInLua);
					/*System.out.print(Operation.shaDeductOneInLua);*/
					result = ((Long) this.getRedisShard().evalshaSingleShard(goodsId, Operation.shaDeductOneInLua, 1, goodsId)).intValue();
				}
			}else{
				result = ((Long)this.getRedisShard().evalshaSingleShard(goodsId,Operation.shaDeductOneInLua,1,goodsId)).intValue();
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
	 *
	 * @param goodsId
	 * @param number
     * @return	-2(key not exists),
	 * 			-1(too large or too small),
	 * 			other(sum number after refill)
     */
	private static String shaRefillByNumberInLua;
	private static String scriptRefillByNumberInLua = "local tonumber=tonumber local isexist = redis.call('exists',KEYS[1]) if 0 == isexist then return -2 end local lastoffset1 = redis.call('bitpos',KEYS[1],'1',-1) local remain = lastoffset1%8 for j=lastoffset1,lastoffset1+8-1-remain do if 1==redis.call('getbit',KEYS[1],j) then lastoffset1=j end end  if lastoffset1 + KEYS[2] >= 2147483647 or tonumber(KEYS[2]) < 1 then return -1 end  for i = lastoffset1+1, lastoffset1+1+KEYS[2]-1 do redis.call('setbit',KEYS[1],i,'1') end local sum = redis.call('bitcount',KEYS[1]) return sum";
	/*eval "local tonumber=tonumber local isexist = redis.call('exists',KEYS[1]) if 0 == isexist then return -2 end local lastoffset1 = redis.call('bitpos',KEYS[1],'1',-1) local remain = lastoffset1%8 for j=lastoffset1,lastoffset1+8-1-remain do if 1==redis.call('getbit',KEYS[1],j) then lastoffset1=j end end  if lastoffset1 + KEYS[2] >= 2147483647 or tonumber(KEYS[2]) < 1 then return -1 end  for i = lastoffset1+1, lastoffset1+1+KEYS[2]-1 do redis.call('setbit',KEYS[1],i,'1') end local sum = redis.call('bitcount',KEYS[1]) return sum" 2 a 100 */

	public Integer stockRefillByNumber(String goodsId,Integer number){
		Integer result = -1;
		if(number <= 0)
			return  result;
		try {
			if(null == Operation.shaRefillByNumberInLua){
				if(this.getRedisShard().scriptExistsSingleShard(goodsId,"c374b953291d79fb91b3cf96fe9249f6852d4c23")){
					Operation.shaRefillByNumberInLua = "c374b953291d79fb91b3cf96fe9249f6852d4c23";
					result = ((Long)this.getRedisShard().evalshaSingleShard(goodsId,Operation.shaRefillByNumberInLua,2,goodsId,number.toString())).intValue();
				}else {
					Operation.shaRefillByNumberInLua = this.getRedisShard().scriptLoadSingleShard(goodsId, Operation.scriptRefillByNumberInLua);
					/*System.out.print(Operation.shaRefillByNumberInLua);*/
					result = ((Long) this.getRedisShard().evalshaSingleShard(goodsId, Operation.shaRefillByNumberInLua, 2, goodsId, number.toString())).intValue();
				}
			}else{
				result = ((Long)this.getRedisShard().evalshaSingleShard(goodsId,Operation.shaRefillByNumberInLua,2,goodsId,number.toString())).intValue();
			}

		}catch (Exception t){
			logger.error("error happend when refill lua",t);
			return -1;
		}
		return result;
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
//		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"spring-config-redis.xml"});
//        context.start();
//		String goodsId = "a";
//        Operation operation = (Operation)context.getBean("operation");
//
//		operation.stockInit(goodsId,10);
//
//		Integer remain = operation.stockRemain(goodsId);
//        System.out.println("remain:"+remain);
//
//		Integer pos = operation.stockDeductOne(goodsId);
//		System.out.println("decuct one pos:"+pos);
//
//		Integer remain2 = operation.stockRemain(goodsId);
//		System.out.println("remain:"+remain2);
//
//		operation.stockSendbackOne(goodsId,pos);
//
//		Integer remain3 = operation.stockRemain(goodsId);
//		System.out.println("remain:"+remain3);
//
//		operation.stockClear(goodsId);
//
//		Integer remain4 = operation.stockRemain(goodsId);
//		System.out.println("remain:"+remain4);

		System.out.print(Integer.MAX_VALUE);
	}


	
	public RedisShard getRedisShard() {
		return redisShard;
	}

	public void setRedisShard(RedisShard redisShard) {
		this.redisShard = redisShard;
	}
	
	
	
	
}
