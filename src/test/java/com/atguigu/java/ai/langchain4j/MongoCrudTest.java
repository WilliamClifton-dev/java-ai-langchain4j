package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.bean.ChatMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@SpringBootTest
public class MongoCrudTest {
    @Autowired
    private MongoTemplate mongoTemplate;

//    @Test
//    public void testInsert() {
//        mongoTemplate.insert(new ChatMessages(1L,"聊天记录"));
//    }

    //增
//    @Test
//    public void testInsert2() {
//        ChatMessages chatMessages = new ChatMessages();
//        chatMessages.setContent("聊天记录列表");
//        mongoTemplate.insert(chatMessages);
//    }

    //删
//    @Test
//    public void testDelete() {
//        Criteria criteria = Criteria.where("_id").is("6a18569bc1e7f275dbc67257");
//        Query query = new Query(criteria);
//        mongoTemplate.remove(query, ChatMessages.class);
//    }

    //改或新增
//    @Test
//    public void testUpdata() {
//        Criteria criteria = Criteria.where("_id").is("6a18569bc1e7f275dbc67257");
//        Query query = new Query(criteria);
//        Update update = new Update();
//        update.set("content", "新的聊天记录列表");
//
//        mongoTemplate.upsert(query, update, ChatMessages.class);
//    }

    //查
//    @Test
//    public void findById() {
//        ChatMessages chatMessages = mongoTemplate.findById("6a18569bc1e7f275dbc67257", ChatMessages.class);
//        System.out.println(chatMessages);
//    }
}