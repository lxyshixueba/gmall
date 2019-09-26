package com.atguigu.gmall.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {
	// 获取操作es的客户端类

	@Autowired
	private JestClient jestClient;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testES() throws IOException {
		/*
		1.	定义dsl 语句
		2.	定义执行的动作
		3.	执行动作并获取返回结果
		 */

		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"term\": {\n" +
				"      \"actorList.name\": \"张译\"\n" +
				"    }\n" +
				"  }\n" +
				"}";
		//GET /movie_chn/movie/_search
		Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();
		// 获取数据
		SearchResult searchResult = jestClient.execute(search);

		List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);

		// 循环遍历
		for (SearchResult.Hit<Map, Void> hit : hits) {
			Map map = hit.source;
			System.out.println(map.get("name"));
		}
	}

}
