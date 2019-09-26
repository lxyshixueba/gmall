package com.atguigu.gmall.gmallitemweb.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import user.service.ManageService;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller//为什么这里只能用controller
public class ItemController {

    @Reference
    private ManageService manageService;



    @RequestMapping("{skuId}.html")
//    @LoginRequire
    public String skuInfoPage(@PathVariable String skuId, HttpServletRequest request){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);

        List<SpuSaleAttr> spuSaleAttrListCheckBySku = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        request.setAttribute("spuSaleAttrListCheckBySku",spuSaleAttrListCheckBySku);
        //#{"136|139|140":"38","135|138|140":"37"}
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
//        [SkuSaleAttrValue(id=null, skuId=37, saleAttrId=null, saleAttrValueId=135, saleAttrName=null, saleAttrValueName=红色),
//        SkuSaleAttrValue(id=null, skuId=38, saleAttrId=null, saleAttrValueId=140, saleAttrName=null, saleAttrValueName=5.8英寸)]
        HashMap<String, String> map = new HashMap<>();
        String base = "";
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            if(skuSaleAttrValueListBySpu.get(i)!=null){
                if(base.length()>0){
                    base+="|";
                }
                String saleAttrValueId = skuSaleAttrValueListBySpu.get(i).getSaleAttrValueId();
                base+=saleAttrValueId;

                //TODO 判断是否到了 循环的最后一个 对象？       37 38
                if(i+1==skuSaleAttrValueListBySpu.size()||!skuSaleAttrValueListBySpu.get(i).getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())){
                                                          //!skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())
                    //这里的情况就是 到了 下一个 或者 到了循环的最后一步
                    map.put(base, skuSaleAttrValueListBySpu.get(i).getSkuId());
                    base = "";
                }
            }
        }
        System.out.println(map.size());
        for (String key  : map.keySet()) {
            System.out.println("key= "+ key + " and value= " + map.get(key));
        }
        String str = JSON.toJSONString(map);
        request.setAttribute("valuesSkuJson",str);
        return "item";
    }



}
