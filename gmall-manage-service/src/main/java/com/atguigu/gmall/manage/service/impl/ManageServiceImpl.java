package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtils;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import user.service.ManageService;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {

    public static final int SKULOCK_EXPIRE_PX=10000;
    public static final String SKULOCK_SUFFIX=":lock";

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;


    @Autowired
    private  SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//        return baseAttrInfoMapper.select(baseAttrInfo);
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    /**
     * 前台发送json对象，后台接收，存入数据库！！！！！！
     * @param baseAttrInfo
     */
    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //!StringUtils.isEmpty(baseAttrInfo.getId())&&Integer.parseInt(baseAttrInfo.getId()》0
        if(baseAttrInfo.getId()!=null && baseAttrInfo.getId().length()>0){
            //获取的id是空的就 插入，非空就修改
            //baseAttrInfoMapper.insert(baseAttrInfo);
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }else{
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        //获取到了 id，方便后续使用
        String id = baseAttrInfo.getId();
        //直接将数据库中与attrid 相关的 行 都删除掉
        BaseAttrValue baseAttrValue1 = new BaseAttrValue();
        baseAttrValue1.setAttrId(id);
        baseAttrValueMapper.delete(baseAttrValue1);
        //删除成功之后再进行插入操作
        List<BaseAttrValue> baseAttrValueList = baseAttrInfo.getAttrValueList();
        boolean b = CollectionUtils.isEmpty(baseAttrValueList);
        if(!b){
            for (BaseAttrValue baseAttrValue : baseAttrValueList) {
                baseAttrValue.setAttrId(id);
                baseAttrValueMapper.insertSelective(baseAttrValue);
                //todo 两个东西都插入成功了？下一步？
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
//        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
//
//        Example example = new Example(BaseAttrValue.class);
//        Example.Criteria criteria = example.createCriteria();
//        criteria.andEqualTo("attrId", attrId);
//        List<BaseAttrValue> baseAttrValues = baseAttrValueMapper.selectByExample(example);
//        baseAttrInfo.setAttrValueList(baseAttrValues);
        // baseAttrInfo.getId() = attrId  Id 主键
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        // 将平台属性值集合放入平台属性中
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        //baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        List<BaseAttrValue> select = baseAttrValueMapper.select(baseAttrValue);
        baseAttrInfo.setAttrValueList(select);
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    /**
     * 保存 商品属性SPU管理 到数据库
     * @param spuInfo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
//        if(spuInfo.getId()==null||spuInfo.getId().length()==0){
//            spuInfo.setId(null);
//            spuInfoMapper.insertSelective(spuInfo);
//        }else {
//            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
//        }
//
//        //  spuImage 图片列表 先删除，在新增
//        //  delete from spuImage where spuId =?
//        SpuImage spuImage1 = new SpuImage();
//        spuImage1.setSpuId(spuInfo.getId());
//        spuImageMapper.delete(spuImage1);
//
//        //TODO 获取图片列表，插入
//        if(!CollectionUtils.isEmpty(spuInfo.getSpuImageList())){
//            List<SpuImage> spuImageList = spuInfo.getSpuImageList();
//            for (SpuImage spuImage : spuImageList) {
//                spuImage.setId(null);
//                spuImage.setSpuId(spuInfo.getId());
//                spuImageMapper.insertSelective(spuImage);
//            }
//        }
//
//        //TODO 获取销售列表，插入
//        if(!CollectionUtils.isEmpty(spuInfo.getSpuSaleAttrList())){
//            List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
//            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
//                spuSaleAttrMapper.insertSelective(spuSaleAttr);
//                if(spuSaleAttr.getSpuSaleAttrValueList()!=null&&spuSaleAttr.getSpuSaleAttrValueList().size()>0){
//                    List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
//                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
//                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
//                    }
//                }
//            }
//        }

        // 什么情况下是保存，什么情况下是更新 spuInfo
        if (spuInfo.getId()==null || spuInfo.getId().length()==0){
            //保存数据
            spuInfo.setId(null);
            spuInfoMapper.insertSelective(spuInfo);
        }else {
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        }

        //  spuImage 图片列表 先删除，在新增
        //  delete from spuImage where spuId =?
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImage);

        // 保存数据，先获取数据
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList!=null && spuImageList.size()>0){
            // 循环遍历
            for (SpuImage image : spuImageList) {
                image.setId(null);
                image.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(image);
            }
        }
        // 销售属性 删除，插入
        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr);

        // 销售属性值 删除，插入
        SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuInfo.getId());
        spuSaleAttrValueMapper.delete(spuSaleAttrValue);

        // 获取数据
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList!=null && spuSaleAttrList.size()>0) {
            // 循环遍历
            for (SpuSaleAttr saleAttr : spuSaleAttrList) {
                saleAttr.setId(null);
                saleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(saleAttr);

                // 添加销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = saleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    // 循环遍历
                    for (SpuSaleAttrValue saleAttrValue : spuSaleAttrValueList) {
                        saleAttrValue.setId(null);
                        saleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(saleAttrValue);
                    }
                }

            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        //TODO 给了skuInfo对象，插入到数据库中
        skuInfo.setId(null);
        skuInfoMapper.insertSelective(skuInfo);
        String skuInfoId = skuInfo.getId();
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if(!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            SkuSaleAttrValue skuSaleAttrValue1 = new SkuSaleAttrValue();
            skuSaleAttrValue1.setSkuId(skuInfoId);
            skuSaleAttrValueMapper.delete(skuSaleAttrValue1);
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfoId);
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
//        skuSaleAttrValue : 与sku相关的销售属性关联表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(!CollectionUtils.isEmpty(skuImageList)){
            SkuImage skuImage = new SkuImage();
            skuImage.setSkuId(skuInfoId);
            skuImageMapper.delete(skuImage);
            for (SkuImage image : skuImageList) {
                image.setSkuId(skuInfoId);
                skuImageMapper.insertSelective(image);
            }
        }

//        skuAttrValue : 与sku相关的平台属性关联表
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null && skuAttrValueList.size()>0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
    }

    private SkuInfo getSkuInfoRedisson(String skuId) {
        SkuInfo skuInfo =null;
        RLock lock =null;
        Jedis jedis =null;
        try {
            String redisKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            jedis = redisUtils.getJedis();
            //String redisValue = jedis.get(redisKey);
            if(jedis.exists(redisKey)){
                String userJson = jedis.get(redisKey);
                if (!StringUtils.isEmpty(userJson)){
                    skuInfo = JSON.parseObject(userJson, SkuInfo.class);
                    return skuInfo;
                }
            }else{
                Config config = new Config();
                config.useSingleServer()
                        // use "rediss://" for SSL connection
                        .setAddress("redis://192.168.213.221:6379");
                RedissonClient redisson = Redisson.create(config);
                lock = redisson.getLock("cinly");
                lock.lock(10, TimeUnit.SECONDS);

                // 从数据库查询数据
                skuInfo = SkuInfoSetAttr(skuId);
                // 将数据放入缓存
                // jedis.set(userKey,JSON.toJSONString(skuInfo));
                jedis.setex(redisKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis!=null){
                jedis.close();
            }
            if (lock!=null){
                lock.unlock();
            }
        }
        return SkuInfoSetAttr(skuId);
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {
        //TODO 使用redis开发，redis没有数据存入redis中
        //return getSkuInfoRedisson(skuId);


        SkuInfo skuInfo = null;
        //RedisConfig redisConfig = new RedisConfig();
        //RedisUtils redisUtils = redisConfig.getRedisUtils();
        Jedis jedis = null;
        //jedis = redisConfig.getRedisUtils().getJedis();
        //第一次没有命中缓存 / 第一次就命中了缓存
        try {
            jedis = redisUtils.getJedis();
            String redisKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            String redisValue = jedis.get(redisKey);
            //StringUtils.isEmpty(redisValue)
            if(redisValue==null){
                //空的
                String redisLockKey = ManageConst.SKUKEY_PREFIX+skuId+SKULOCK_SUFFIX;
                //自定义分布式锁
                String set = jedis.set(redisLockKey, "Cyndi", "NX", "PX", 10000);
                System.out.println(set);
                if("OK".equals(set)){
                    //走数据库
                    skuInfo = SkuInfoSetAttr(skuId);
                    //去查询一遍数据，并将数据插入数据库
                    String secondTimeResu = JSON.toJSONString(skuInfo);
                    jedis.setex(redisKey, ManageConst.SKUKEY_TIMEOUT,secondTimeResu);
                    //删除锁
                    jedis.del(redisLockKey);
                    return skuInfo;
                }else {
                    //等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //getSkuInfo(skuId);
                    return SkuInfoSetAttr(skuId);
                }
            }else {
                //走缓存
                if(!StringUtils.isEmpty(redisValue)){
                    skuInfo = JSON.parseObject(redisValue, SkuInfo.class);
                    return skuInfo;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis!=null){
                jedis.close();
            }
        }
        return SkuInfoSetAttr(skuId);


//        SkuInfo skuInfo= null;
//        Jedis jedis = null;
//
//        try {
//            // 获取Jedis
//            jedis=redisUtils.getJedis();
//            // 定义key
//            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
//
//            // 获取数据
//            String skuJson = jedis.get(skuKey);
//
//            // 在缓存中没有获取到数据
//            if (skuJson==null){
//                System.out.println("缓存中没有数据：");
//                // 上锁： set k1 v1 px 10000 nx -- OK
//                // 定义锁的key sku:skuId:lock
//                String lockKey=ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
//                String lockKeyResult = jedis.set(lockKey, "Atguigu", "NX", "PX", 10000);
//                if ("OK".equals(lockKeyResult)){
//                    System.out.println("获取分布式锁");
//                    // 走db
//                    skuInfo = SkuInfoSetAttr(skuId);
//                    // 将数据放入缓存
//                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
//                    // 删除锁
//                    jedis.del(lockKey);
//
//                    return skuInfo;
//                }else {
//                    // 等待
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    // 调用方法再查询数据
//                    return  getSkuInfo(skuId);
//                }
//            }else {
//                // 走缓存！
//                if (StringUtils.isNotEmpty(skuJson)){
//                    skuInfo = JSON.parseObject(skuJson,SkuInfo.class);
//                }
//                return skuInfo;
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }finally {
//            if (jedis!=null){
//                jedis.close();
//            }
//        }
//        // 当缓存宕机以后，走数据库！
//        return SkuInfoSetAttr(skuId);

    }

    //假如redis 宕机情况下 走database  getSkuInfoDB
    private SkuInfo SkuInfoSetAttr(String skuId) {
        SkuInfo skuInfo =skuInfoMapper.selectByPrimaryKey(skuId);
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> select = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(select);
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> select1 = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(select1);
        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        //需要做的是 将 这个 String 的 集合数组 转化位一个字符串常量
        String goal = "";
        if(attrValueIdList!=null&&attrValueIdList.size()>0){

            for (Iterator<String> iterator = attrValueIdList.iterator(); iterator.hasNext(); ) {
                String next = iterator.next();
                goal+=next;
                goal+=",";
            }
            goal = goal.substring(0, goal.length()-1);
        }else{
            return null;
        }
        //String join = StringUtils.join(attrValueIdList.toArray(), ",");
        return baseAttrInfoMapper.selectByAttrValueIdList(goal);
    }
}


//第一种情况，没有加上分布式锁
/*
* @Override
    public SkuInfo getSkuInfo(String skuId) {
//        RedisUtils redisUtils = redisConfig.getRedisUtils();
//        Jedis jedis = redisUtils.getJedis();
//        jedis.set("王心凌", "wo爱你呀");
//        jedis.close();
        //TODO 使用redis开发，redis没有数据存入redis中
        SkuInfo skuInfo = null;
        //RedisConfig redisConfig = new RedisConfig();
        RedisUtils redisUtils = redisConfig.getRedisUtils();
        Jedis jedis = null;
        String redisKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX+SKULOCK_SUFFIX;
        try {
            jedis = redisUtils.getJedis();
            //TODO 先去redis中查询 有没有缓存的内容。有就直接用redis中的数据，没有的话就去DB中查找然后返回再存入redis中
            if (jedis.exists(redisKey)) {
                String redisSaveStrValue = jedis.get(redisKey);
                if (StringUtils.isNotEmpty(redisSaveStrValue)) {
                    // 非空走redis
                    skuInfo = JSON.parseObject(redisSaveStrValue, SkuInfo.class);
                    return skuInfo;
                }
            } else {
                //TODO 去数据库中查询
                skuInfo = SkuInfoSetAttr(skuId);
                //TODO 查询的结构存入redis中
                if(skuInfo!=null){
                    String ResultStr = JSON.toJSONString(skuInfo);
                    jedis.setex(redisKey, ManageConst.SKUKEY_TIMEOUT, ResultStr);
                }
                return skuInfo;
            }
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis!=null){
                jedis.close();
            }
        }
        return SkuInfoSetAttr(skuId);
    }
* */
