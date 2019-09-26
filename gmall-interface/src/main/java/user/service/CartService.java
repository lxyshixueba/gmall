package user.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    void  addToCart(String skuId,String userId,Integer skuNum);

    List<CartInfo> getCartList(String userId);

    /**
     * 合并cookie 和 数据库，redis中的数据
     * @param cartListFromCookie
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId);

    void checkCart(String skuId, String isChecked, String userId);

    List<CartInfo> getCartCheckedList(String userId);


}

