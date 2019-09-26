package com.atguigu.gmall.gmallusermanage.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtils;
import com.atguigu.gmall.gmallusermanage.user.mapper.UserAddressMapper;
import com.atguigu.gmall.gmallusermanage.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;
import user.service.UserInfoService;

import java.util.List;

//import com.atguigu.gmall.gmallusermanage.user.mapper.UserInfoMapper;
//import com.atguigu.gmall.gmallusermanage.user.mapper.*;

// com.atguigu.gmall.gmallusermanage.user.mapper.UserInfoMapper;

//import com.atguigu.gmall.gmallusermanage.user.mapper.UserInfoMapper;
//import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;

    //import com.atguigu.gmall.gmallusermanage.user.mapper.*;
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtils redisUtils;



    /**
     * 根据用户id查询用户的个人信息
     * @param userId
     * @return
     */
    @Override
    public List<UserAddress> getUserAddressByUserId(String userId) {
        Example example = new Example(UserAddress.class);
        example.createCriteria().andEqualTo("userId", userId);
        return userAddressMapper.selectByExample(example);
        // select * from userAddress where userId = ?
//        UserAddress userAddress = new UserAddress();
//        userAddress.setUserId(userId);
//        return userAddressMapper.select(userAddress);
    }

    @Override
    public List<UserInfo> findAll() {
        List<UserInfo> userInfos = userInfoMapper.selectAll();
        //Example example = new Example(UserInfo.class);
        //example.createCriteria().
        //userInfoMapper.selectByExample(
        return userInfos;
    }

    @Override
    public UserInfo getUserInfoByName(String name) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("name",name);
        List<UserInfo> userInfos = userInfoMapper.selectByExample(example);

        return userInfos.get(0);
    }

    @Override
    public List<UserInfo> getUserInfoListByName(UserInfo userInfo) {
        List<UserInfo> select = userInfoMapper.select(userInfo);
        return select;
    }


    @Override
    public List<UserInfo> getUserInfoListByNickName(UserInfo userInfo) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andLike("nickName", "%"+userInfo.getNickName()+"%");
        List<UserInfo> userInfos = userInfoMapper.selectByExample(example);
        return userInfos;
    }

    @Override
    public void addUser(UserInfo userInfo) {
        userInfoMapper.insert(userInfo);
    }

    @Override
    public void updUser(UserInfo userInfo) {
        userInfoMapper.updateByPrimaryKeySelective(userInfo);
        //Example example = new Example(UserInfo.class);
        //        example.createCriteria().andEqualTo("name",userInfo.getName());
        //        userInfoMapper.updateByExampleSelective(userInfo,example);
    }

    @Override
    public void delUser(UserInfo userInfo) {
        userInfoMapper.delete(userInfo);
    }

    /**
     * 实现用户的登录功能
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {
        String newPasswd = userInfo.getPasswd();
        String pswd = DigestUtils.md5DigestAsHex(newPasswd.getBytes());
        userInfo.setPasswd(pswd);
        UserInfo info = userInfoMapper.selectOne(userInfo);
        //TODO 现在得到了用户的登录信息，存入redis
        //user:userid:info
        String key = userKey_prefix+info.getId()+userinfoKey_suffix;
        if(info!=null){
            Jedis jedis = redisUtils.getJedis();
            String infoStr = JSON.toJSONString(info);
            jedis.setex(key,userKey_timeOut,infoStr);
            jedis.close();
            return info;
        }
        return null;
    }

    /**
     * 用来进行用户的验证
     * @param userId
     * @return
     */
    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtils.getJedis();
        //TODO 和redis中的用户信息做比对，如果有的话证明用户登录过，刷新用户的登录状态
        String key = userKey_prefix+userId+userinfoKey_suffix;
        String userInfoStrInRedis = jedis.get(key);
        if(userInfoStrInRedis!=null&&userInfoStrInRedis.length()>0) {
            //说明有
            UserInfo userInfoInRedis = JSON.parseObject(userInfoStrInRedis, UserInfo.class);
            //UserInfo userInfo = userInfoMapper.selectByPrimaryKey(userId);
            jedis.expire(key, userKey_timeOut);
            jedis.close();
            return userInfoInRedis;
        }
        return null;
    }
}
