package com.fengpeiyuan.dao.redis.shard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fengpeiyuan.dao.redis.shard.exception.RedisAccessException;
import org.apache.log4j.Logger;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

public class RedisShard {
	private static Logger log = Logger.getLogger(RedisShard.class);
	private ShardedJedisPool wPool;
    private String confStr;
    private Integer timeout=2000;
    private Integer maxActive;
    private Integer maxIdle;
    private Integer maxWait;
    private boolean testOnBorrow;

    public RedisShard() {
	}
	
	private void init(){
		List<JedisShardInfo> wShards = new ArrayList<JedisShardInfo>();
        if(null == this.confStr || this.confStr.isEmpty())
        	throw new ExceptionInInitializerError("confString is empty！");
		List<String> confList = Arrays.asList(this.confStr.split("(?:\\s|,)+"));
		if (null == confList || confList.isEmpty())
			throw new ExceptionInInitializerError("confList is empty！");
		for (String wAddress : confList) {
            if (wAddress != null) {
                String[] wAddressArr = wAddress.split(":");
                if (wAddressArr.length == 1) 
                    throw new ExceptionInInitializerError(wAddressArr + " is not include host:port or host:port:passwd after split \":\"");
                String host = wAddressArr[0];
                int port = Integer.valueOf(wAddressArr[1]);
                JedisShardInfo jedisShardInfo = new JedisShardInfo(host, port, this.timeout);
                log.info("confList:" + jedisShardInfo.toString());

                if (wAddressArr.length == 3 && !wAddressArr[2].isEmpty()) {
                    jedisShardInfo.setPassword(wAddressArr[2]);
                }
                wShards.add(jedisShardInfo);
            }
        }
		
		//config for JedisPoolConfig
		JedisPoolConfig wConfig = new JedisPoolConfig();
		wConfig.setMaxTotal(this.maxActive);
		wConfig.setMaxIdle(this.maxIdle);
		wConfig.setMaxWaitMillis(this.maxWait);
		wConfig.setTestOnBorrow(this.testOnBorrow);

        this.wPool = new ShardedJedisPool(wConfig, wShards);
        log.info("RedisShard init end.");
	}


	/**
	 * set
	 * @param key
	 * @param value
	 * @return
	 * @throws RedisAccessException
     */
	public String set(String key, byte[] value) throws RedisAccessException {
		if(key == null) throw new RedisAccessException("value sent to redis cannot be null");
        boolean flag = true;
        ShardedJedis j = null;
		String result = null;
        try {
			j = wPool.getResource();
			result = j.getShard(key).set(key.getBytes("UTF-8"), value);
        } catch (Exception ex) {
            flag = false;
            wPool.returnBrokenResource(j);
            throw new RedisAccessException(ex+","+j.getShardInfo(key).toString());
        } finally {
            if (flag) {
                wPool.returnResource(j);
            }
        }
        return result;
    }

	/**
	 * setbit
	 * @param key
	 * @param offset
	 * @param value
	 * @return
	 * @throws RedisAccessException
     */
	public Boolean setbit(String key,long offset,Boolean value) throws RedisAccessException {
		Boolean result = null;
		ShardedJedis j = null;
		boolean flag = true;
		try {
			j = wPool.getResource();
			result = j.getShard(key).setbit(key,offset,value);
		} catch (Exception ex) {
			flag = false;
			wPool.returnBrokenResource(j);
			throw new RedisAccessException(ex+","+j.getShardInfo(key).toString());
		} finally {
			if (flag) {
				wPool.returnResource(j);
			}
		}
		return result;
	}


	/**
	 * bitpos
	 * @param key
	 * @param value
	 * @return
	 * @throws RedisAccessException
     */
	public Long bitpos(String key,boolean value) throws RedisAccessException {
		Long result = null;
		ShardedJedis j = null;
		boolean flag = true;
		try {
			j = wPool.getResource();
			result = j.getShard(key).bitpos(key,value);
		} catch (Exception ex) {
			flag = false;
			wPool.returnBrokenResource(j);
			throw new RedisAccessException(ex+","+j.getShardInfo(key).toString());
		} finally {
			if (flag) {
				wPool.returnResource(j);
			}
		}
		return result;
	}


	/**
	 * bitcount
	 * @param key
	 * @return
	 * @throws RedisAccessException
     */
	public Long bitcount(String key) throws RedisAccessException {
		Long result = null;
		ShardedJedis j = null;
		boolean flag = true;
		try {
			j = wPool.getResource();
			result = j.getShard(key).bitcount(key);
		} catch (Exception ex) {
			flag = false;
			wPool.returnBrokenResource(j);
			throw new RedisAccessException(ex+","+j.getShardInfo(key).toString());
		} finally {
			if (flag) {
				wPool.returnResource(j);
			}
		}
		return result;
	}


	/**
	 * getbit
	 * @param key
	 * @param offset
	 * @return
	 * @throws RedisAccessException
     */
	public Boolean getbit(String key,long offset) throws RedisAccessException {
		Boolean result = null;
		ShardedJedis j = null;
		boolean flag = true;
		try {
			j = wPool.getResource();
			result = j.getShard(key).getbit(key,offset);
		} catch (Exception ex) {
			flag = false;
			wPool.returnBrokenResource(j);
			throw new RedisAccessException(ex+","+j.getShardInfo(key).toString());
		} finally {
			if (flag) {
				wPool.returnResource(j);
			}
		}
		return result;
	}


	/**
	 * del
	 * @param key
	 * @return
	 * @throws RedisAccessException
     */
	public Long del(String key) throws RedisAccessException {
		if(key == null) throw new RedisAccessException("value sent to redis cannot be null");
		boolean flag = true;
		ShardedJedis j = null;
		Long result = null;
		try {
			j = wPool.getResource();
			result = j.getShard(key).del(key);
		} catch (Exception ex) {
			flag = false;
			wPool.returnBrokenResource(j);
			throw new RedisAccessException(ex+","+j.getShardInfo(key).toString());
		} finally {
			if (flag) {
				wPool.returnResource(j);
			}
		}
		return result;
	}


	/**
	 *
	 * @param keyShard
	 * @param sha1
	 * @return
	 * @throws RedisAccessException
     */
	public Boolean scriptExistsSingleShard(String keyShard, String sha1) throws RedisAccessException {
		if(null == keyShard ||sha1 == null) throw new RedisAccessException("keyShard or sha1 sent to redis cannot be null");
		boolean flag = true;
		ShardedJedis j = null;
		Boolean result = Boolean.FALSE;
		try {
			j = wPool.getResource();
			result = j.getShard(keyShard).scriptExists(sha1);
		} catch (Exception ex) {
			flag = false;
			wPool.returnBrokenResource(j);
			throw new RedisAccessException(ex+","+j.getShardInfo(keyShard).toString());
		} finally {
			if (flag) {
				wPool.returnResource(j);
			}
		}
		return result;
	}


	/**
	 *
	 * @param keyShard
	 * @param script
	 * @return
	 * @throws RedisAccessException
     */
	public String scriptLoadSingleShard(String keyShard, String script) throws RedisAccessException {
		if(null == keyShard ||script == null) throw new RedisAccessException("keyShard or script sent to redis cannot be null");
		boolean flag = true;
		ShardedJedis j = null;
		String result = null;
		try {
			j = wPool.getResource();
			result = j.getShard(keyShard).scriptLoad(script);
		} catch (Exception ex) {
			flag = false;
			wPool.returnBrokenResource(j);
			throw new RedisAccessException(ex+","+j.getShardInfo(keyShard).toString());
		} finally {
			if (flag) {
				wPool.returnResource(j);
			}
		}
		return result;
	}


	/**
	 *
	 * @param keyShard
	 * @param script
	 * @param keyCount
	 * @param params
	 * @return
	 * @throws RedisAccessException
     */
	public Object evalshaSingleShard(String keyShard, String script,int keyCount, String... params) throws RedisAccessException {
		if(null == keyShard ||script == null) throw new RedisAccessException("keyShard or script sent to redis cannot be null");
		boolean flag = true;
		ShardedJedis j = null;
		Object result = null;
		try {
			j = wPool.getResource();
			result = j.getShard(keyShard).evalsha(script,keyCount, params);
		} catch (Exception ex) {
			flag = false;
			wPool.returnBrokenResource(j);
			throw new RedisAccessException(ex+","+j.getShardInfo(keyShard).toString());
		} finally {
			if (flag) {
				wPool.returnResource(j);
			}
		}
		return result;
	}






	public static Logger getLog() {
		return log;
	}

	public static void setLog(Logger log) {
		RedisShard.log = log;
	}

	public ShardedJedisPool getwPool() {
		return wPool;
	}

	public void setwPool(ShardedJedisPool wPool) {
		this.wPool = wPool;
	}

	public String getConfStr() {
		return confStr;
	}

	public void setConfStr(String confStr) {
		this.confStr = confStr;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public Integer getMaxActive() {
		return maxActive;
	}

	public void setMaxActive(Integer maxActive) {
		this.maxActive = maxActive;
	}

	public Integer getMaxIdle() {
		return maxIdle;
	}

	public void setMaxIdle(Integer maxIdle) {
		this.maxIdle = maxIdle;
	}

	public Integer getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(Integer maxWait) {
		this.maxWait = maxWait;
	}

	public boolean isTestOnBorrow() {
		return testOnBorrow;
	}

	public void setTestOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
	}
}
