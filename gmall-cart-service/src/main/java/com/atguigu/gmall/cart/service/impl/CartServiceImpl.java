package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.consts.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import user.service.CartService;
import user.service.ManageService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Reference
    private ManageService manageService;
    /**
     * 实现添加到购物车的功能
     * @param skuId
     * @param userId
     * @param skuNum
     */
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //使用工具：mysql，redis
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        CartInfo cartInfo1 = cartInfoMapper.selectOne(cartInfo);
        Jedis jedis = redisUtils.getJedis();
        //表示曾经添加过购物车且其中有该条记录
        String key = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        if(cartInfo1!=null){
            cartInfo1.setSkuNum(cartInfo1.getSkuNum()+skuNum);
            cartInfo1.setSkuPrice(cartInfo1.getCartPrice());
            cartInfoMapper.updateByPrimaryKeySelective(cartInfo1);
        }else{
            //表示之前没有添加过购物车
            // 如果不存在，保存购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo2 = new CartInfo();
            cartInfo2.setSkuId(skuId);
            cartInfo2.setCartPrice(skuInfo.getPrice());
            cartInfo2.setSkuPrice(skuInfo.getPrice());
            cartInfo2.setSkuName(skuInfo.getSkuName());
            cartInfo2.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo2.setUserId(userId);
            cartInfo2.setSkuNum(skuNum);
            cartInfo1=cartInfo2;
            //存入 db中
            cartInfoMapper.insertSelective(cartInfo2);
        }
        jedis.hset(key, skuId, JSON.toJSONString(cartInfo1));
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userKey);
        jedis.expire(key,ttl.intValue() );
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        /*
        1.  获取redis中的购物车数据
        2.  如果redis 没有，从mysql 获取并放入缓存
         */

        List<CartInfo> cartInfoList = new ArrayList<>();
        Jedis jedis = redisUtils.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List<String> stringList = jedis.hvals(cartKey);

        //得到了 redis中的 购物车信息集合
        if(!CollectionUtils.isEmpty(stringList)){
            for (String s : stringList) {
                CartInfo cartInfo = JSON.parseObject(s, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            jedis.close();
            return cartInfoList;
        }else {
            // 走db -- 放入redis
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    /**
     * 合并cookie 和 数据库，redis中的数据
     * @param cartListFromCookie
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId) {
        List<CartInfo> cartInfoListFromDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        //从 数据库中根据userId 查询
        //在 控制器中已经判断过cookie中取到的集合数据了
//        List<CartInfo> list = new ArrayList<>();
        boolean isMatch = false;
        for (CartInfo cartInfoFromCK : cartListFromCookie) {
            if(!CollectionUtils.isEmpty(cartInfoListFromDB)){
                for (CartInfo cartInfoFromDB : cartInfoListFromDB) {
                    //从DB中去取购物车信息
                    if(cartInfoFromCK.getSkuId().equals(cartInfoFromDB.getSkuId())){//&&"1".equals(cartInfoFromCK.getIsChecked())
                        //设置商品数量
                        cartInfoFromDB.setSkuNum(cartInfoFromDB.getSkuNum()+cartInfoFromCK.getSkuNum());
                        cartInfoFromDB.setSkuPrice(cartInfoFromDB.getSkuPrice());
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoFromDB);
                        isMatch=true;
                        //list.add(cartInfoFromDB);
                    }
                }
            }
        }
        //这种情况下 cookie中的商品 和 db中的商品不一样
        if(!isMatch){
            for (CartInfo cartInfo : cartListFromCookie) {
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
                //list.add(cartInfo);
            }
        }
        List<CartInfo> cartInfoList = loadCartCache(userId); //cartInfoList 刷新数据
//        for (CartInfo cartInfoCK : cartListFromCookie) {
//            for (CartInfo cartInfoDB : cartInfoListFromDB) {
//                if(cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){
//                    cartInfoDB.setIsChecked("1");
//                    checkCart(cartInfoCK.getSkuId(),"1",userId);
//                }
//            }
//        }
        for (CartInfo cartInfo : cartInfoList) {
            for (CartInfo info : cartListFromCookie) {
                if (cartInfo.getSkuId().equals(info.getSkuId())){
                    // 只有被勾选的才会进行更改
                    if (info.getIsChecked().equals("1")){
                        cartInfo.setIsChecked(info.getIsChecked());
                        // 更新redis中的isChecked
                        checkCart(cartInfo.getSkuId(),info.getIsChecked(),userId);
                    }
                }
            }
        }

        return loadCartCache(userId);
    }

    // 获取数据库中的数据并放入缓存
    private List<CartInfo> loadCartCache(String userId) {
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtils.getJedis();
        // 使用实时价格：将skuInfo.price 价格赋值 cartInfo.skuPrice
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList==null || cartInfoList.size()==0){
            return  null;
        }
        //得到了 数据库中的 购物车商品id集合
//        for (CartInfo cartInfo : cartInfoList) {
//            jedis.hset(cartKey, cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
//        }
        HashMap<String, String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));

        }
        jedis.hmset(cartKey, map);
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        //cartInfoList = null;
        List<String> hvals = jedis.hvals(userCheckedKey);
        for (String hval : hvals) {
            CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
            for (int i = 0; i < cartInfoList.size(); i++) {
                if(cartInfoList.get(i).getSkuId().equals(cartInfo.getSkuId())){
                    cartInfoList.remove(i);
                }
            }
            cartInfoList.add(cartInfo);
        }
        jedis.close();
        return cartInfoList;
    }


    /**
     * 用户勾选了并且 需要去更新 redis和数据库的选中状态
     * @param skuId
     * @param isChecked
     * @param userId
     */
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        Jedis jedis = redisUtils.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        String skuInfoInRedis = jedis.hget(cartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(skuInfoInRedis, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        //改变redis中存储该数据的ischecked的状态
        jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfo));
        //为下一步做打算。选中状态为 1 的商品需要放入 一个对象中方便使用
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        if("1".equals(isChecked)){
            jedis.hset(userCheckedKey, skuId, JSON.toJSONString(cartInfo));
        }
        if("0".equals(isChecked)){
            jedis.hdel(userCheckedKey, skuId);
        }
        jedis.close();
    }

    /**
     * 跟据用户id查询用户已经选择的商品
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        ArrayList<CartInfo> cartInfos = new ArrayList<>();
        Jedis jedis = redisUtils.getJedis();
        String checkedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        List<String> hvals = jedis.hvals(checkedKey);
        for (String hval : hvals) {
            CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
            cartInfos.add(cartInfo);
        }
        return cartInfos;
    }
}
