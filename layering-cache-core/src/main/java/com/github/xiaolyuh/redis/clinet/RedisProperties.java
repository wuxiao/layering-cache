package com.github.xiaolyuh.redis.clinet;

public class RedisProperties {
    Integer database = 0;
    /**
     * 不为空表示集群版，示例
     * localhost:7379,localhost2:7379
     */
    String cluster = "";
    String host = "localhost";
    Integer port = 6379;
    boolean enableSsl = false;
    String password = null;
    /**
     * 序列化方式:
     * com.github.xiaolyuh.redis.serializer.KryoRedisSerializer
     * com.github.xiaolyuh.redis.serializer.JacksonRedisSerializer
     * com.github.xiaolyuh.redis.serializer.JdkRedisSerializer
     * com.github.xiaolyuh.redis.serializer.ProtostuffRedisSerializer
     */
    String serializer = "com.github.xiaolyuh.redis.serializer.ProtostuffRedisSerializer";

    public Integer getDatabase() {
        return database;
    }

    public void setDatabase(Integer database) {
        this.database = database;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSerializer() {
        return serializer;
    }

    public void setSerializer(String serializer) {
        this.serializer = serializer;
    }

    public boolean isEnableSsl() {
        return enableSsl;
    }

    public void setEnableSsl(boolean enableSsl) {
        this.enableSsl = enableSsl;
    }
}