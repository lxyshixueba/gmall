package user.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {

    void saveSkuInfo(SkuLsInfo skuLsInfo);

    /**
     * 获取查询结果集
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 更改评分
     * @param skuId
     */
    void incrHotScore(String skuId);

}
