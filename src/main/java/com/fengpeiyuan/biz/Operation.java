package com.fengpeiyuan.biz;

import com.fengpeiyuan.util.BitUtil;
import com.fengpeiyuan.dao.redis.shard.RedisShard;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
	 * deduct 1
	 * @param goodsId
	 * @return -2: exception
	 * 		   -1:not enough stock
	 * 		  >=0:position
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
			return -2;
		}
		return offset;
	}


	/**
	 *
	 * @param goodsId
	 * @return -2: key not exists
	 * 		   -1: not enough stock
	 *         >=0: position
     */
	private static String shaDeductOneInLua;
	private static String scriptDeductOneInLua = "local isexist = redis.call('exists',KEYS[1]) if 0 == isexist then return -2 end  local offset = redis.call('bitpos',KEYS[1],'1') if offset==-1 then return -1 end  local ret = redis.call('setbit',KEYS[1],offset,'0') if ret == 1 then return offset else return -1 end ";
	/*eval "local isexist = redis.call('exists',KEYS[1]) if 0 == isexist then return -2 end  local offset = redis.call('bitpos',KEYS[1],'1') if offset==-1 then return -1 end  local ret = redis.call('setbit',KEYS[1],offset,'0') if ret == 1 then return offset else return -1 end  " 1 a */

	public Integer stockDeductOneInLua(String goodsId) {
		Integer result = -1;
		try {
			if(null == Operation.shaDeductOneInLua){
				if(this.getRedisShard().scriptExistsSingleShard(goodsId,"e01f375122b933f99b73ba39cf6324a154393de1")){
					Operation.shaDeductOneInLua = "e01f375122b933f99b73ba39cf6324a154393de1";
					result = ((Long)this.getRedisShard().evalshaSingleShard(goodsId,Operation.shaDeductOneInLua,1,goodsId)).intValue();
				}else {
					Operation.shaDeductOneInLua = this.getRedisShard().scriptLoadSingleShard(goodsId, Operation.scriptDeductOneInLua);
					System.out.print(Operation.shaDeductOneInLua);
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
	 *
	 * @param goodsId
	 * @param number
     * @return {-2}: invalid skuid or number(less than 0 or more then 5000)
	 * 		   {}: empty table, not enough stock to deduct(number>remain)
     */
	private static String shaDeductByNumberInLua;
	private static String scriptDeductByNumberInLua = "local tonumber=tonumber   local t_pos={}  local isexist = redis.call('exists',KEYS[1]) if 0 == isexist then t_pos[1]=-2 return t_pos end  local number=tonumber(KEYS[2])  if number<=0 or number>5000 then t_pos[1]=-2 return t_pos end  local remain=redis.call('bitcount',KEYS[1])  if number>remain then t_pos[1]=-1 return t_pos end  local i=1 while i<=number do local offset=redis.call('bitpos',KEYS[1],'1') local ret = redis.call('setbit',KEYS[1],offset,'0') if ret ==1 then t_pos[i]=offset i=i+1 end end return t_pos";
	/*eval "local tonumber=tonumber   local t_pos={}  local isexist = redis.call('exists',KEYS[1]) if 0 == isexist then t_pos[1]=-2 return t_pos end  local number=tonumber(KEYS[2])  if number<=0 or number>5000 then t_pos[1]=-2 return t_pos end  local remain=redis.call('bitcount',KEYS[1])  if number>remain then t_pos[1]=-1 return t_pos end  local i=1 while i<=number do local offset=redis.call('bitpos',KEYS[1],'1') local ret = redis.call('setbit',KEYS[1],offset,'0') if ret ==1 then t_pos[i]=offset i=i+1 end end return t_pos " 2 a 100 */

	public List<Integer> stockDeductByNumberInLua(String goodsId, Integer number) {
		List<Integer> result;
		List<Integer> ret = new ArrayList<Integer>();
		try {
			if(null == Operation.shaDeductByNumberInLua){
				if(this.getRedisShard().scriptExistsSingleShard(goodsId,"fcacd0b4a74789e3084c8d95f85ee428f98fe2df")){
					Operation.shaDeductByNumberInLua = "fcacd0b4a74789e3084c8d95f85ee428f98fe2df";
					result = (List)this.getRedisShard().evalshaSingleShard(goodsId,Operation.shaDeductByNumberInLua,2,goodsId,number.toString());
				}else {
					Operation.shaDeductByNumberInLua = this.getRedisShard().scriptLoadSingleShard(goodsId, Operation.scriptDeductByNumberInLua);
					/*System.out.print(Operation.shaDeductByNumberInLua);*/
					result = (List) this.getRedisShard().evalshaSingleShard(goodsId, Operation.shaDeductByNumberInLua, 2, goodsId,number.toString());
				}
			}else{
				result = (List)this.getRedisShard().evalshaSingleShard(goodsId,Operation.shaDeductByNumberInLua,2,goodsId,number.toString());
			}

			Iterator it=result.iterator();
			while (it.hasNext()){
				ret.add(((Long)it.next()).intValue());
			}

		}catch (Exception t){
			logger.error("error happend when stockDeductByNumberInLua",t);
			return ret;
		}
		return ret;
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
					System.out.print(Operation.shaRefillByNumberInLua);
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
		System.out.print(Integer.MAX_VALUE);
	}


	
	public RedisShard getRedisShard() {
		return redisShard;
	}

	public void setRedisShard(RedisShard redisShard) {
		this.redisShard = redisShard;
	}
	
	
	
	
}
