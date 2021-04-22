package com.github.xiaolyuh.setting;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * 多级缓存配置项
 *
 * @author yuhao.wang
 */
public class LayeringCacheSetting implements Serializable {
    private static final String SPLIT = "-";
    /**
     * 内部缓存名，由[一级缓存有效时间-二级缓存有效时间]组成
     */
    private String internalKey;

    /**
     * 描述，数据监控页面使用
     */
    private String depict;

    /**
     * 是否使用一级缓存
     */
    boolean enableFirstCache = true;

    /**
     * 一级缓存配置
     */
    private FirstCacheSetting firstCacheSetting;

    /**
     * 二级缓存配置
     */
    private SecondaryCacheSetting secondaryCacheSetting;

    public LayeringCacheSetting() {
    }

    public LayeringCacheSetting(FirstCacheSetting firstCacheSetting, SecondaryCacheSetting secondaryCacheSetting,
                                String depict, boolean enableFirstCache) {
        this.firstCacheSetting = firstCacheSetting;
        this.secondaryCacheSetting = secondaryCacheSetting;
        this.depict = depict;
        this.enableFirstCache = enableFirstCache;
        internalKey();
    }

    @JsonIgnore
    private void internalKey() {
        // 一级缓存有效时间-二级缓存有效时间
        StringBuilder sb = new StringBuilder();
        if (firstCacheSetting != null) {
            sb.append(firstCacheSetting.getTimeUnit().toMillis(firstCacheSetting.getExpireTime()));
        }
        if (secondaryCacheSetting != null) {
            sb.append(SPLIT);
            sb.append(secondaryCacheSetting.getTimeUnit().toMillis(secondaryCacheSetting.getExpiration()));
        }
        internalKey = sb.toString();
    }

    public FirstCacheSetting getFirstCacheSetting() {
        return firstCacheSetting;
    }

    public SecondaryCacheSetting getSecondaryCacheSetting() {
        return secondaryCacheSetting;
    }

    public String getInternalKey() {
        return internalKey;
    }

    public void internalKey(String internalKey) {
        this.internalKey = internalKey;
    }

    public boolean isEnableFirstCache() {
        return enableFirstCache;
    }

    public void setEnableFirstCache(boolean enableFirstCache) {
        this.enableFirstCache = enableFirstCache;
    }

    public void setFirstCacheSetting(FirstCacheSetting firstCacheSetting) {
        this.firstCacheSetting = firstCacheSetting;
    }

    public void setSecondaryCacheSetting(SecondaryCacheSetting secondaryCacheSetting) {
        this.secondaryCacheSetting = secondaryCacheSetting;
    }

    public void setInternalKey(String internalKey) {
        this.internalKey = internalKey;
    }

    public String getDepict() {
        return depict;
    }

    public void setDepict(String depict) {
        this.depict = depict;
    }
}
