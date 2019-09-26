package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.bean.CartInfo;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface CartInfoMapper extends Mapper<CartInfo> {
    List<CartInfo> selectCartListWithCurPrice( String userId);
}
