package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import user.service.ManageService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManageService manageService;
    /**
     * 添加购物车信息
     * @param request
     * @param response
     * @param skuId
     * @param userId
     * @param parseInt
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int parseInt) {
        /*
        1.  先获取cookie 中的所有数据
        2.  看当前商品是否在购物车中存在， 比较条件 skuId = skuId
         */
        ArrayList<CartInfo> cartInfosMySelf = new ArrayList<>();
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        boolean isMatch = false;
        if(StringUtils.isNotEmpty(cookieValue)){
            List<CartInfo> cartInfos = JSON.parseArray(cookieValue, CartInfo.class);
            for (CartInfo cartInfo : cartInfos) {
                //通过cookie得到了 购物车的集合。遍历购物车的集合
                //不相等的情况下？
                if(cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setSkuNum(cartInfo.getSkuNum()+parseInt);
                    cartInfo.setSkuPrice(cartInfo.getSkuPrice());
                    cartInfosMySelf.add(cartInfo);
                    isMatch = true;
                }else {
                    cartInfosMySelf.add(cartInfo);
                }
                //判断两样商品不一样的情况
//                if(!cartInfo.getSkuId().equals(skuId)){
//                    cartInfosMySelf.add(cartInfo);
//                    //通过skuId去查找商品并设置其数量
//                    SkuInfo skuInfo = manageService.getSkuInfo(skuId);
//
////                    cartInfosMySelf
//                }
            }
            //然后将 得到的List对象再次存入cookie中
            String cartInfoJsonStr = JSON.toJSONString(cartInfosMySelf);
            CookieUtil.setCookie(request, response, cookieCartName,cartInfoJsonStr,COOKIE_CART_MAXAGE,true);

        }
        if(!isMatch){
            // 用户未登录，实现： 将购物车的信息存入 cookie
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(parseInt);
            cartInfo.setUserId(userId); // null
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuName(skuInfo.getSkuName());

            cartInfosMySelf.add(cartInfo);
            String cartInfoJsonStr = JSON.toJSONString(cartInfosMySelf);
            CookieUtil.setCookie(request, response, cookieCartName, cartInfoJsonStr, COOKIE_CART_MAXAGE, true);
        }
    }

    public List<CartInfo> getCartList(HttpServletRequest request) {
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        List<CartInfo> cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
        return cartInfoList;
    }

    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, cookieCartName);
    }

    /**
     * 用户未登录的状态。需要去改变 cookie中所存储的数据的选中状态
     * @param request
     * @param response
     * @param skuId
     * @param isChecked
     */
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        //从cookie中取数据
        List<CartInfo> cartList = getCartList(request);
        for (CartInfo cartInfo : cartList) {
            if(skuId.equals(cartInfo.getSkuId())){
                cartInfo.setIsChecked(isChecked);
            }
        }
        CookieUtil.setCookie(request, response, cookieCartName, JSON.toJSONString(cartList), COOKIE_CART_MAXAGE, true);
    }
}
