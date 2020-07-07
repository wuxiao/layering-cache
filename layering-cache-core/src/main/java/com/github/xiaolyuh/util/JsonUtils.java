/* Created by flym at 11/18/2014 */
package com.github.xiaolyuh.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.TimeZone;

/**
 * json工具，用于格式化对象为json
 *
 * @author flym
 */
public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    private static ObjectMapper uniqueObjectMapper;

    /** 获取默认的序列化器 */
    public static ObjectMapper objectMapper() {
        if(uniqueObjectMapper == null) {
            new DefaultHolder().init();
        }

        return uniqueObjectMapper;
    }

    /** 设置默认的序列化器 */
    @SuppressWarnings("unchecked")
    public static void setDefault(ObjectMapper objectMapper) {
        //注册未注册的模块
        objectMapper.findAndRegisterModules();

        //未知属性忽略
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //允许字段名不带引号 {a:1}
        objectMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        //允许用单引号 {'a':'1'}
        objectMapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        //允许单个元素匹配到数组上
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        //设置默认的时区
        objectMapper.setTimeZone(TimeZone.getDefault());
        //序列化时，忽略空值属性
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //转小数转换为字符串，防止小数科学计数法
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

        JsonUtils.uniqueObjectMapper = objectMapper;
    }

    /** 将对象输出为可反序列化的json字符串，以用于数据存储和传输 */
    public static <T> String toJson(T t) {
        try{
            return objectMapper().writeValueAsString(t);
        } catch(JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** 将对象输出为json字符串，并进行美化显示 */
    public static <T> String toJsonPretty(T t) {
        try{
            return objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(t);
        } catch(JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** 将json字符串转换为指定类型的对象 */
    public static <T> T parse(String json, Class<T> clazz) {
        try{
            return objectMapper().readValue(json, clazz);
        } catch(IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** 将json字符串转换为指定类型的对象，如果不能转换，则返回指定的默认值 */
    public static <T> T parse(String json, Class<T> clazz, T defaultValue) {
        try{
            return objectMapper().readValue(json, clazz);
        } catch(IOException e) {
            logger.debug(e.getMessage(), e);
            return defaultValue;
        }
    }

    private static class DefaultHolder {
        static {
            JsonUtils.setDefault(new ObjectMapper());
        }

        /**
         * 空方法 ,主要目的为使相应的静态初始化完成. 因为是静态块,因此保证仅初始化一次,避免出现多次设置mapper的情况
         * 此方法不是static的原因在于,使其执行相应的构造方法,绕过代码覆盖处理. 表示已经执行过<init>方法
         */
        private void init() {
            //nothing to do
        }
    }
}
