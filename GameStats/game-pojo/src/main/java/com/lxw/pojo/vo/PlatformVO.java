package com.lxw.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 平台拥有情况视图对象
 */
@Data
public class PlatformVO implements Serializable {

    private String name;

    private Boolean owned;
}
