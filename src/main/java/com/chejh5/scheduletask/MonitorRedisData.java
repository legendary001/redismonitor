package com.chejh5.scheduletask;

import com.alibaba.fastjson.JSONArray;
import com.chejh5.entity.Article;
import com.chejh5.util.DateUtil;
import com.chejh5.util.RedisUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by chenjh5 on 2017/5/3.
 */
@Component
@EnableScheduling
public class MonitorRedisData {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Log log = LogFactory.getLog(MonitorRedisData.class);

    private void writeCountMsgToMysql(String todayDateStr, Map<String, List<Integer>> dataMap) {
        if (todayDateStr == null || dataMap == null) return;
        dataMap.keySet().stream().forEach(key -> {
            log.info(key + "================>" + dataMap.get(key));
            List<Integer> countList = dataMap.get(key);
            jdbcTemplate.update("INSERT INTO redis_data_monitor(tag, time, real_time_count, long_time_count) VALUES(?, ?, ?, ?)",
                    new PreparedStatementSetter() {
                        public void setValues(PreparedStatement ps) throws SQLException {
                            ps.setString(1, key);
                            ps.setString(2, todayDateStr);
                            ps.setInt(3, countList.get(0));
                            ps.setInt(4, countList.get(1));
                        }
                    });
        });
    }

    private void classifyTheData(Map<String, List<Article>> articleMap, Map<String, List<Integer>> dataMap) {
        if (articleMap == null) return;
        articleMap.keySet().stream().forEach(key -> {
            List<Article> list = articleMap.get(key);
            list.stream().forEach(article -> {
                //文章为空 | 时效长效为空 | 时间为空 则跳出本次循环
                if (article == null || article.getWhy() == null || article.getDate() == null)
                    return;
                //时效性数据且为今日
                if (article.getWhy().trim().equals("")) {
                    String[] dateArr = article.getDate().split(" ");
                    if (dateArr.length != 2) return;
                    String dateStr = dateArr[0];
                    long articleTime = 0;
                    long todayTime = 0;
                    try {
                        articleTime = DateUtil.transferStrToDateTime(dateStr);
                        todayTime = DateUtil.getTodayDateTime();
                    } catch (Exception e) {
                        log.error("日期数据格式转换异常");
                        return;
                    }
                    //比较日期
                    if (articleTime == todayTime) {
                        if (dataMap.get(key) == null) {
                            List<Integer> dataList = new ArrayList<>(2);
                            dataList.add(0, 1); dataList.add(0);
                            dataMap.put(key, dataList);
                        } else {
                            List<Integer> dataList = dataMap.get(key);
                            int realTimeDataCount = dataList.get(0);
                            dataList.set(0, ++realTimeDataCount);
                            dataMap.put(key, dataList);
                        }
                    }
                } else if (article.getWhy().trim().equals("longTime")) {
                    if (dataMap.get(key) == null) {
                        List<Integer> dataList = new ArrayList<>(2);
                        dataList.add(0);
                        dataList.add(1, 1);
                        dataMap.put(key, dataList);
                    } else {
                        List<Integer> dataList = dataMap.get(key);
                        int realTimeDataCount = dataList.get(1);
                        dataList.set(1, ++realTimeDataCount);
                        dataMap.put(key, dataList);
                    }
                }
            });
        });
    }

    private void pipLine2ArticleMap(Map<String, List<Article>> articleMap, Map<String, Response<String>> pipeMap) {
        if (articleMap == null || pipeMap == null) return;
        for (Map.Entry<String, Response<String>> entry: pipeMap.entrySet()) {
            Response<String> response =  entry.getValue();
            if (response.get() == null) continue;
            List<Article> articleList = new ArrayList<>(JSONArray.parseArray(response.get(), Article.class));
            articleMap.put(new String(entry.getKey()), articleList);
        }
    }

    private void statisticRedisData(String host, int port, int dbName ,int redisPerDealNumber) throws UnsupportedEncodingException {
        String todayDateStr = DateUtil.getTodayDateStr();
        Jedis jedis = RedisUtil.getJedisClient(host, port);
        jedis.select(dbName);
        Set<String> keySet = jedis.keys("*");
        List<String> keyList = new ArrayList<>(keySet);
        List<List> keySetList = new ArrayList<>();
        Pipeline pipe = jedis.pipelined();
        Map<String, List<Article>> articleMap = new HashMap<>();
        Map<String, List<Integer>> dataMap = new HashMap<>();
        Map<String, Response<String>> pipeMap = new HashMap<>();
        if(keyList.size() > redisPerDealNumber) {
            int setNumber = keyList.size() / redisPerDealNumber;
            int endLength = keyList.size() % redisPerDealNumber;
            List<String> tmpKeyList;
            for (int i = 0; i < setNumber; i++) {
                tmpKeyList = keyList.subList(i * redisPerDealNumber, (i+1) * redisPerDealNumber);
                keySetList.add(tmpKeyList);
            }

            if (endLength != 0) {
                tmpKeyList = keyList.subList(setNumber*redisPerDealNumber, setNumber*redisPerDealNumber + endLength);
                keySetList.add(tmpKeyList);
            }

            for (List<String> list :keySetList) {
                for (String key: list) {
                    pipeMap.put(key, pipe.get(key));
                }
                pipe.sync();
                pipLine2ArticleMap(articleMap, pipeMap);
                pipeMap.clear();
                classifyTheData(articleMap, dataMap);
                articleMap.clear();
                writeCountMsgToMysql(todayDateStr, dataMap);
                dataMap.clear();
            }
            jedis.disconnect();
        } else {
            for (String key: keySet) {
                pipeMap.put(key, pipe.get(key));
            }

            pipe.sync();
            pipLine2ArticleMap(articleMap, pipeMap);
            classifyTheData(articleMap, dataMap);
            writeCountMsgToMysql(todayDateStr, dataMap);
            dataMap.clear();
            jedis.disconnect();
        }
    }

    @Scheduled(fixedRate = 5000)
//    @Scheduled(cron = "0 1 10 * * ?")
    public void monitorTodayRedisMsg() throws Exception {
        statisticRedisData("10.90.4.17", 6379, 12, 30000);
        statisticRedisData("10.90.4.17", 6379, 2, 30000);
        log.info("Monitor end!!!!!");
    }


//    public void monitorTodayRedisMsg() {
//        Jedis jedis = RedisUtil.getJedisClient("10.90.4.17", 6379);
//        jedis.select(2);
//        Set<String> key2Set = jedis.keys("*");
//        Map<String, List<Article>> articleMap = new HashMap<>();
//        Map<String, List<Integer>> dataMap = new HashMap<>();
//        Gson gson = new Gson();
//        key2Set.stream().forEach(key -> {
//            String value = jedis.get(key);
//            List<Article> list = gson.fromJson(value, new TypeToken<ArrayList<Article>>() {
//            }.getType());
//            articleMap.put(key, list);
//        });
//        articleMap.keySet().stream().forEach(key -> {
//            List<Article> list = articleMap.get(key);
//            list.stream().forEach(article -> {
//                //文章为空 | 时效长效为空 | 时间为空 则跳出本次循环
//                if (article == null || article.getWhy() == null || article.getDate() == null)
//                    return;
//                //时效性数据且为今日
//                if (article.getWhy().equals("")) {
//                    String[] dateArr = article.getDate().split(" ");
//                    if (dateArr.length != 2) return;
//                    String dateStr = dateArr[0];
//                    long articleTime = 0;
//                    long todayTime = 0;
//                    try {
//                        articleTime = DateUtil.transferStrToDateTime(dateStr);
//                        todayTime = DateUtil.getTodayDateTime();
//                    } catch (Exception e) {
//                        log.error("日期数据格式转换异常");
//                        return;
//                    }
//                    //比较日期
//                    if (articleTime == todayTime) {
//                        if (dataMap.get(key) == null) {
//                            List<Integer> dataList = new ArrayList<>(3);
//                            dataList.set(0, 1);
//                            dataMap.put(key, dataList);
//                        } else {
//                            List<Integer> dataList = dataMap.get(key);
//                            int realTimeDataCount = dataList.get(0);
//                            dataList.set(0, ++realTimeDataCount);
//                            dataMap.put(key, dataList);
//                        }
//                    }
//                } else if (article.getWhy().equals("longTime")) {
//                    if (dataMap.get(key) == null) {
//                        List<Integer> dataList = new ArrayList<>(3);
//                        dataList.set(1, 1);
//                        dataMap.put(key, dataList);
//                    } else {
//                        List<Integer> dataList = dataMap.get(key);
//                        int realTimeDataCount = dataList.get(0);
//                        dataList.set(1, ++realTimeDataCount);
//                        dataMap.put(key, dataList);
//                    }
//                }
//
//            });
//        });
//
//        dataMap.keySet().stream().forEach(key -> {
//            dataMap.get(key).stream().forEach(count -> System.out.println(count));
//        });
//
//
//    }

}
