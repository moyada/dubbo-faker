package io.moyada.sharingan.monitor.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author xueyikang
 * @since 1.0
 **/
@ConfigurationProperties(prefix = MonitorConfig.PREFIX + ".async")
public class MonitorConfig {
    public static final String PREFIX = "sharingan.monitor";

    private int intervalTime = 3000;

    private int thresholdSize = 100;

    public int getIntervalTime() {
        return intervalTime;
    }

    public void setIntervalTime(Integer intervalTime) {
        if (null == intervalTime) {
            return;
        }
        if (intervalTime > 10) {
            this.intervalTime = intervalTime;
        }
    }

    public int getThresholdSize() {
        return thresholdSize;
    }

    public void setThresholdSize(Integer thresholdSize) {
        if (null == thresholdSize) {
            return;
        }
        if (thresholdSize > 1) {
            this.thresholdSize = thresholdSize;
        }
    }
}
