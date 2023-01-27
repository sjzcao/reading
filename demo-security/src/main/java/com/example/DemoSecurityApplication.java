package com.example;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.convert.RedisCustomConversions;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.SpringSessionRedisConnectionFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@SpringBootApplication
public class DemoSecurityApplication {
	private static final Logger logger = LoggerFactory.getLogger(DemoSecurityApplication.class);

	@Value("${demo.session.redis.host}")
	private String redisSessionClusterHost;
	@Value("${demo.session.redis.port}")
	private int redisSessionClusterPort;
	@Value("${demo.enable.redis.listener}")
	private boolean enableRedisListener;

	@Bean
	public static ConfigureRedisAction configureRedisAction() {
		return ConfigureRedisAction.NO_OP;
	}

	@Bean
	@SpringSessionRedisConnectionFactory
	public RedisConnectionFactory springSessionRedisConnectionFactory() {

		RedisClusterClient clusterClient = RedisClusterClient.create(
				RedisURI.Builder.redis(redisSessionClusterHost, redisSessionClusterPort).build());

		// Check for any changes in the cluster topology.
		// Under the hood this calls cluster slots
		clusterClient.refreshPartitions();

		RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
		for (RedisClusterNode node : clusterClient.getPartitions().getPartitions()) {
			clusterConfiguration.addClusterNode(new RedisNode(node.getUri().getHost(), node.getUri().getPort()));
			logger.info("Add node {}:{}", node.getUri().getHost(), node.getUri().getPort());
		}

		return new LettuceConnectionFactory(clusterConfiguration);
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(@Qualifier("springSessionRedisConnectionFactory") RedisConnectionFactory redisConnectionFactory) {
		RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
		redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
		logger.info(">>>>>> Add key space listener.");

		if(enableRedisListener) {
			redisMessageListenerContainer.addMessageListener(new RedisKeyExpiredListener(),
					Arrays.asList(new PatternTopic("__keyevent@*__:expired"),new PatternTopic("__keyevent@*__:del")));
		}
		return redisMessageListenerContainer;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoSecurityApplication.class, args);
	}

}
