package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

@Data
public class BaseCatalog1 implements Serializable {
    //
    //@KeySql(useGeneratedKeys = true
    @Id
    @Column
    private String id;
    @Column
    private String name;
}
