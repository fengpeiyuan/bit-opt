package com.fengpeiyuan.dao.redis.shard.exception;

public class RedisAccessException extends Exception {
	 /**
	 * Constructor for DataAccessException.
	 * @param msg the detail message
	 */
	public RedisAccessException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for DataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using a underlying
	 * data access API such as JDBC)
	 */
	public RedisAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

    public RedisAccessException(Throwable cause) {
		super( cause);
	}
}
