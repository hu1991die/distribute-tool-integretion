package com.feizi.distribute;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App {

    public static void main( String[] args ){
        /*App app = new App();
        BigDecimal amount = new BigDecimal("0");
        app.setAmount(amount);
        System.out.println(amount);*/

        /*App app = new App();
        String str = "a";
        System.out.println("初始化hashCode值：" + str.hashCode());
        System.out.println("初始化值： " + str);
        app.setStr(str);
        System.out.println("调用之后hashCode值：" + str.hashCode());
        System.out.println(str);*/

        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 10000; i++){
            App p = new App();
            if (!list.contains(p.hashCode())){
                list.add(p.hashCode());
            }
        }
        System.out.println(list.size());
    }

    public void setStr(String str){
        System.out.println("修改之前hashCode值：" + str.hashCode());
        str = str.concat("b");
//        str.concat("b");
        System.out.println("修改之后hashCode值：" + str.hashCode());
        System.out.println(str);
    }

    public void setAmount(BigDecimal amount){
        System.out.println(amount.hashCode());
//        amount = amount.add(new BigDecimal("14"));
        amount.add(new BigDecimal("14"));
        System.out.println(amount.hashCode());
    }
}
