package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import user.service.CartService;
import user.service.ManageService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@CrossOrigin
public class CartController {

    @Reference
    private CartService cartService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @Reference
    private ManageService manageService;
    /**
     * 添加商品到购物车
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        // 获取userId，skuId，skuNum
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        String userId = (String) request.getAttribute("userId");
        //得到了 商品id，用户id，商品数量 做添加购物车的操作
        if (userId!=null){
            // 调用服务层做添加功能
            // 什么时候调用？登录时！
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else {
            // 未登录 cookie ，
            // redis redis.hset(key,field,value) | user:userId:cart ---> user:UUID:cart --> cookie
            // 调用添加方法
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
        }
        //添加到购物车成功了，下一步？
        // 取得sku信息对象
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public  String cartList(HttpServletRequest request,HttpServletResponse response){
        // 判断用户是否登录，登录了从redis中，redis中没有，从数据库中取
        // 没有登录，从cookie中取得
        String userId = (String) request.getAttribute("userId");
        if (userId!=null){
            // 从cookie中查找购物车
            // 登录状态下需要去判断 cookie中有没有数据，如果cookie中有，则需要去合并信息
            List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
            List<CartInfo> cartList = null;
            if (cartListFromCookie!=null && cartListFromCookie.size()>0){
                // 开始合并
                cartList=cartService.mergeToCartList(cartListFromCookie,userId);
                // 删除cookie中的购物车
                cartCookieHandler.deleteCartCookie(request,response);
            }else{
                // 从redis中取得，或者从数据库中
                cartList= cartService.getCartList(userId);
            }
            request.setAttribute("cartList",cartList);
        }else{
            List<CartInfo> cartList = cartCookieHandler.getCartList(request);
            request.setAttribute("cartList",cartList);
        }
        return "cartList";
    }

    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        //对用户的勾选状态进行判断
        String userId = (String) request.getAttribute("userId");
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        //用户已经登录
        request.setAttribute("isChecked", isChecked);
        if (userId!=null){
            cartService.checkCart(skuId,isChecked,userId);
        }else{
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        //用户提交订单的操作
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);
        if (cookieHandlerCartList!=null && cookieHandlerCartList.size()>0){
            cartService.mergeToCartList(cookieHandlerCartList, userId);
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://trade.gmall.com/trade";
    }

}
