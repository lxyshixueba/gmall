package com.atguigu.gmall.gmalllistservice.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtils;
import io.searchbox.client.JestClient;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import user.service.ListService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtils redisUtils;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    //保存数据
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {

        Index build = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            DocumentResult result = jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //根据查询参数获取查询结果
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        String query = makeQueryStringForSearch(skuLsParams);
        Search build = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);
        return skuLsResult;
    }

    /**
     * 根据 skuId来修改评分
     * @param skuId
     */
    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtils.getJedis();
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
//        if(hotScore%10==0){
//            updateHotScore(skuId,  Math.round(hotScore));
//        }
    }

    /**
     * 解析查询解果返回需要的对象
     * @param skuLsParams
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        //Math.round()
        SkuLsResult skuLsResult = new SkuLsResult();
        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {

            SkuLsInfo skuLsInfo = hit.source;
            Map<String, List<String>> highlight = hit.highlight;
            if(!CollectionUtils.isEmpty(highlight)){
                List<String> highLightFontList = highlight.get("skuName");
                String s = highLightFontList.get(0);
                skuLsInfo.setSkuName(s);
            }
            skuLsInfoList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        skuLsResult.setTotal(searchResult.getTotal());
        long totalPage= (searchResult.getTotal() + skuLsParams.getPageSize() -1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPage);

//        List<String> attrValueIdList;
        List<String> attrValueIdList = new ArrayList<>();
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby = aggregations.getTermsAggregation("groupby");
        if(groupby!=null){
            for (TermsAggregation.Entry bucket : groupby.getBuckets()) {
                attrValueIdList.add(bucket.getKey());
            }
            skuLsResult.setAttrValueIdList(attrValueIdList);
        }
        return skuLsResult;

    }

    /**
     * 获取查询参数
     * @param skuLsParams
     * @return
     */
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断三级id是否存在于skuLsParams中
        String catalog3Id = skuLsParams.getCatalog3Id();
        if(!StringUtils.isEmpty(catalog3Id)){
            //根据三级分类id去查询
//            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
//            BoolQueryBuilder filter = boolQueryBuilder.filter(termQueryBuilder);
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }

        if(skuLsParams.getKeyword()!=null){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);
            //设置字体高亮显示
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuName");
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.postTags("</span>");
            searchSourceBuilder.highlight(highlightBuilder);
        }

        // 设置属性值
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            for (int i=0;i<skuLsParams.getValueId().length;i++){
                String valueId = skuLsParams.getValueId()[i];
                TermQueryBuilder termsQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termsQueryBuilder);
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);
        //设置分页
        int form = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(form);
        searchSourceBuilder.size(skuLsParams.getPageSize());
        // 设置按照热度
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //设置聚合
        TermsBuilder termsBuilder = AggregationBuilders.terms("groupby").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(termsBuilder);
        String query = searchSourceBuilder.toString();
        System.out.println("query:"+query);
        return query;
    }
}
