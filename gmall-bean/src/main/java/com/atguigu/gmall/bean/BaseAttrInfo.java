package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
public class BaseAttrInfo implements Serializable {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)//开启主键回显
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    @Transient//表示非数据库字段
    private List<BaseAttrValue> attrValueList;


}
