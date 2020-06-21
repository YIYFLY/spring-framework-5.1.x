package top.yyiu.aop.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 作用于：
 *
 * @author pc
 * @date 2020/4/4
 */
@Configuration
@ComponentScan("top*")
@EnableAspectJAutoProxy(proxyTargetClass = true,exposeProxy = true)
public class AppConfig {

	public static void main(String[] args) {
		HashMap<String,String> map = new HashMap<>();
//		map.keySet()
		map.values();

		ArrayList<String> arrayList = new ArrayList<>();
		arrayList.add("1");
		arrayList.add("2");
		arrayList.add("3");
		Iterator<String> i = arrayList.iterator();
		int size = arrayList.size();
		for (i.hasNext();;){

		}

	}
}
