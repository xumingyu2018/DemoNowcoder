package com.xmy.demonowcoder;

import com.xmy.demonowcoder.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * @author xumingyu
 * @date 2022/4/20
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = DemoNowcoderApplication.class)
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;//注入HTML模板引擎类

    @Test
    public void testTextMail() {
        mailClient.sendMail("xmy981022@163.com", "Test", "Welcome");
    }

    @Test
    public void testHTMLMail() {
        Context context = new Context();
        context.setVariable("username", "Nevermore");
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail("xmy981022@163.com", "HTML", content);
    }
}
