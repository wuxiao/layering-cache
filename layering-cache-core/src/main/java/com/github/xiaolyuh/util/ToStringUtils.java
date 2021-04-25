package com.github.xiaolyuh.util;


import java.util.Objects;

/**
 * 将一个Object转换成String
 *
 * @author yuhao.wang3
 */
public class ToStringUtils {

    /**
     * 将一个Object转换成String
     *
     * @param object Object
     * @return String
     */
    public static String toString(final Object object) {
        if (Objects.isNull(object)) {
            return null;
        }
        if (object instanceof String) {
            return (String) object;
        }
        String string = JsonUtils.toJson(object);
        return string.replace("\"", "").replace("{", "").replace("}", "");
    }

}
