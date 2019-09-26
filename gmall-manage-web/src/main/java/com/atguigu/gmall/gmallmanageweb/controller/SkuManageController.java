package com.atguigu.gmall.gmallmanageweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuSaleAttr;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import user.service.ListService;
import user.service.ManageService;

import java.util.List;

@CrossOrigin
@Controller
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(String spuId){
        return   manageService.getSpuImageList(spuId);
    }


    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> getspuSaleAttrList(String spuId){
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return  spuSaleAttrList;
    }

    @RequestMapping("saveSkuInfo")
    @ResponseBody
    public String saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return "ok";
    }

    @ResponseBody
    @RequestMapping("onSale")
    public String onSale(@RequestParam String skuId){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        BeanUtils.copyProperties(skuInfo, skuLsInfo);

        //得到了skuId之后干嘛？
        listService.saveSkuInfo(skuLsInfo);
        return "ok";
    }

}
