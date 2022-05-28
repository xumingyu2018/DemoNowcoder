package com.xmy.demonowcoder;

import java.io.IOException;

/**
 * Java实现wkhtmltopdf工具
 */
public class WkTests {

    public static void main(String[] args) {
        String cmd = "d:/wkhtmltopdf/bin/wkhtmltoimage --quality 75  https://www.nowcoder.com d:/wkhtmltopdf/wk-images/3.png";
        try {
            // 并发执行
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
