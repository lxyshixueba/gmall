package com.atguigu.gmall.gmallmanageweb.controller;

import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuInfo;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import user.service.ManageService;

import java.util.List;

@CrossOrigin
@Controller
public class SpuManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("spuList")
    @ResponseBody
    public List<SpuInfo> spuList(String catalog3Id){
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        List<SpuInfo> spuInfoList = manageService.getSpuInfoList(spuInfo);
        return  spuInfoList;
    }

    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr> getAllBaseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }

    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public String insertValueInDB(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return "ok";
    }
}
