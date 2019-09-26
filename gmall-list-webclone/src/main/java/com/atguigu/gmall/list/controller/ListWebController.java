package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import user.service.ListService;
import user.service.ManageService;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@CrossOrigin
@Controller
public class ListWebController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("/list.html")
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){
        skuLsParams.setPageSize(1);

        SkuLsResult skuLsResult  = listService.search(skuLsParams);
        //保存分页参数
        request.setAttribute("totalPages", skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        //声明面包屑集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();
        request.setAttribute("skuLsInfoList", skuLsResult.getSkuLsInfoList());
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);
        //TODO 获取点击之后的valueId，然后和 baseAttrInfoList作比较，如果相等就删除 做的是删除的操作
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo = iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                String baseAttrValueId = baseAttrValue.getId();
                String[] valueIds = skuLsParams.getValueId();
                if(valueIds!=null&&valueIds.length>0){
                    for (String valueId : valueIds) {
                        if(baseAttrValueId.equals(valueId)){

                            String newRequestUrl = makeUrlParam(skuLsParams,valueId);//需要的是当前的 valueId
                            //组成面包屑： 属性名+属性值名
                            String boardName = baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName();
                            BaseAttrValue attrValue = new BaseAttrValue();
                            attrValue.setUrlParam(newRequestUrl);
                            attrValue.setValueName(boardName);
                            baseAttrValueArrayList.add(attrValue);

                            //如果相等就删掉咯
                            iterator.remove();
                        }
                    }
                }

            }

        }
        //TODO 得到了 BaseAttrInfo的集合 设置平台属性值 以及 平台属性到 作用域中
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);


        String urlParam = makeUrlParam(skuLsParams);
        //设置面包屑跳转的请求id
        request.setAttribute("urlParam",urlParam);
        //设置面包屑的关键字
        String keyWord = skuLsParams.getKeyword();
        request.setAttribute("keyWord",keyWord);
        //保存面包屑集合
        request.setAttribute("baseAttrValueArrayList",baseAttrValueArrayList);
        return "list";
    }

    //新增了一个额外的参数，如果我使用的这个参数的话那么我就要在 url中 减去这次参数的拼接
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String UrlParam = "";
        //判断三级分类id
//        if(UrlParam.length()>0){
//            UrlParam+="&";
//        }
        if(skuLsParams.getCatalog3Id()!=null&&skuLsParams.getCatalog3Id().length()>0){
            //http://list.gmall.com/list.html?catalog3Id=61&valueId=83
            UrlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }

        if(skuLsParams.getKeyword()!=null&&skuLsParams.getKeyword().length()>0){
            UrlParam+="keyword="+skuLsParams.getKeyword();
        }

        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            //todo 这里是一个 array数组
            String[] valueIdArray = skuLsParams.getValueId();

            for (String s : valueIdArray) {
                if(excludeValueIds!=null&excludeValueIds.length>0){
                    if(excludeValueIds[0].equals(s)){
                        continue;
                    }
                }
                if(UrlParam.length()>0){
                    //?catalog3Id=61&valueId=83
                    UrlParam+="&valueId=";
                    UrlParam+=s;
                }
            }

            // 循环遍历
//            for (String valueId : skuLsParams.getValueId()) {
//                if (excludeValueIds!=null && excludeValueIds.length>0){
//                    // 获取到用户点击面包屑时的平台属性值Id
//                    String excludeValueId = excludeValueIds[0];
//                    // 如果平台属性值Id 相同则不拼接到参数条件后面
//                    if (excludeValueId.equals(valueId)){
//                        //  break; continue; return;
//                        continue;
//                    }
//                }
//                // href="list.html?catalog3Id=61&valueId=13"
//                // 什么时候拼接&
//                if (UrlParam.length()>0){
//                    // catalog3Id=61&
//                    UrlParam+="&";
//                }
//                UrlParam+="valueId="+valueId;
//            }
        }
        System.out.println(UrlParam);
        return UrlParam;
    }

}
