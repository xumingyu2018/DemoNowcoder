package com.xmy.demonowcoder;

import com.xmy.demonowcoder.dao.DiscussPostMapper;
import com.xmy.demonowcoder.dao.elasticsearch.DiscussPostRepository;
import com.xmy.demonowcoder.entities.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Spring整合Elasticsearch demo示例
 *
 * @author xumingyu
 * @date 2022/5/21
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = DemoNowcoderApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    @Test
    public void testInset() {
        // 将数据库的数据插入到Elasticsearch中
        // 根据Post_id从数据库插入一条数据到Elasticsearch
        discussRepository.save(discussPostMapper.selectDiscussPostById(123));
        discussRepository.save(discussPostMapper.selectDiscussPostById(127));
        discussRepository.save(discussPostMapper.selectDiscussPostById(128));
    }

    @Test
    public void testInsetList() {
        // 根据user_id从数据库插入多条同一用户数据到Elasticsearch
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(101, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(107, 0, 100, 0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(103, 0, 100, 0));
        // discussRepository.saveAll(discussPostMapper.selectDiscussPosts(0, 0,  1000));
    }

    @Test
    public void testUpdate() {
        // 修改Elasticsearch中数据
        DiscussPost post = discussPostMapper.selectDiscussPostById(123);
        post.setContent("Elasticsearch测试");
        discussRepository.save(post);
    }

    @Test
    public void testDelete() {
        // discussPostRepository.deleteById(123);
        discussRepository.deleteAll();
    }


    // 使用ElasticsearchRepository方法(对于高亮处理不够好，不完善)
    @Test
    public void testSearchByRepository() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 多字段查询
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                // 降序排序
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                // 分页
                .withPageable(PageRequest.of(0, 10))
                // 高亮查询结果
                .withHighlightFields(
                        // preTags("<em>").postTags("</em>")-->前后各加一个em标签
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
        // discussRepository.search：底层获取得到了高亮显示的值, 但是没有做进一步处理.
        Page<DiscussPost> page = discussRepository.search(searchQuery);
        // 获取搜询数量
        System.out.println(page.getTotalElements());
        // 获取搜寻页数
        System.out.println(page.getTotalPages());
        // 当前第几页
        System.out.println(page.getNumber());
        // 每页几条数据
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }

    // 使用ElasticsearchTemplate方法
    @Test
    public void testSearchByTemplate() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // new SearchResultMapper()匿名类，处理高亮
        Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() <= 0) {
                    return null;
                }

                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {
                    DiscussPost post = new DiscussPost();

                    // elasticsearch中将json格式数据封装为了map,在将map字段存进post中
                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    // createTime字符串是Long类型
                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        // [0]->搜寻结果为多段时，取第一段
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                return new AggregatedPageImpl(list, pageable,
                        hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore());
            }
        });

        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }
}




