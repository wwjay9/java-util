package com.wwj.util.java;

import java.util.Map;

/**
 * @author Wenjie.Wang
 */
public class Demo {

    public static void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            try {
                String s = HttpUtil.post("https://s1.eqxiu.com/eqs/s/scene/count?code=5KuV95yg",
                        Map.of("sceneId", "227374994", "elementId", "6984064198",
                                "pageId", "1819574752", "openid", ""));
                System.out.println(i + "\t" + s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
