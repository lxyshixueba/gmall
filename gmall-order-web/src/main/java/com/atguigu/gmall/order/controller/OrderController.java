package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.LoginRequire;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import user.service.CartService;
import user.service.OrderService;
import user.service.UserInfoService;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    private UserInfoService userInfoService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    //@ResponseBody
//    @RequestMapping("trade")
//    @LoginRequire
//    public String trade(String userId, HttpServletRequest request){
//        //return userInfoService.getUserAddressByUserId(userId);
//        String userId1 = (String) request.getAttribute("userId");
//        List<UserAddress> userAddressList = userInfoService.getUserAddressByUserId(userId1);
//        request.setAttribute("userAddressList",userAddressList);
//        return "trade";
//    }

    @RequestMapping(value = "trade")
    @LoginRequire
    public  String tradeInit(HttpServletRequest request){
        //初始化订单,显示到页面上
        //设置用户的收货地址的信息
        String userId = (String) request.getAttribute("userId");
        List<UserAddress> userAddressList = userInfoService.getUserAddressByUserId(userId);
        request.setAttribute("userAddressList",userAddressList);

        //设置送货清单的信息
        List<CartInfo> cartInfoCheckedList = cartService.getCartCheckedList(userId);
        //TODO 获取到了所有的列表清单，需要我做些什么呢
        ArrayList<OrderDetail> orderDetails = new ArrayList<>();
        for (CartInfo cartInfo : cartInfoCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetails.add(orderDetail);
        }
        request.setAttribute("orderDetailList",orderDetails);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetails);
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());

        // 获取TradeCode号
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeCode",tradeNo);
        return "trade";
    }

    @RequestMapping(value = "submitOrder",method = RequestMethod.POST)
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        // 检查tradeCode
        String userId = (String) request.getAttribute("userId");
        // 初始化参数
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.sumTotalAmount();
        orderInfo.setUserId(userId);
        // 检查tradeCode
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag){
            request.setAttribute("errMsg","该页面已失效，请重新结算!");
            return "tradeFail";
        }
        // 保存
        String orderId = orderService.saveOrder(orderInfo);
        orderService.delTradeCode(userId);
        // 重定向
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

}
