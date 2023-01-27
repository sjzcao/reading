package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.Nullable;


public class RedisKeyExpiredListener implements MessageListener {
	private static final Logger log = LoggerFactory.getLogger(RedisKeyExpiredListener.class);	
		
	@Override
	public void onMessage(Message message, @Nullable byte[] pattern) {

		log.info("########## onMessage pattern " + new String(pattern) + " | " + message.toString());		
	}
}
