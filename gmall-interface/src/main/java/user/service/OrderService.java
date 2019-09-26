package user.service;

import com.atguigu.gmall.bean.OrderInfo;

public interface OrderService {
    String  saveOrder(OrderInfo orderInfo);

    //生成 订单流水号
    String getTradeNo(String userId);

    //比较 流水号
    boolean checkTradeCode(String userId,String tradeCodeNo);

    //删除流水号
    void  delTradeCode(String userId);

    /**
     * 获取订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);
}
