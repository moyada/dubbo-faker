package io.moyada.sharingan.manager.vo;

import java.io.Serializable;

/**
 * @author xueyikang
 * @create 2018-01-01 11:35
 */
public class SelectVO implements Serializable {

    private static final long serialVersionUID = -7750004293151410471L;

    private String key;

    private String value;

    public SelectVO(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}