package top.yyiu.aop;


import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.yyiu.aop.config.AppConfig;
import top.yyiu.aop.dao.UserDao;
import top.yyiu.aop.service.UserServiceImpl;

/**
 * 作用于：
 *
 * @author pc
 * @date 2020/4/4
 */
public class Test {

	public static void main(String[] args) {
		// 初始化spring的环境
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		UserDao dao = (UserDao) applicationContext.getBean("userDao1");
		dao.update("==");
		dao.update("==");
		System.out.println(applicationContext);
//		ApplicationContext applicationContext1 = applicationContext.getBean(ApplicationContext.class);
//		System.out.println(applicationContext1);
	}

}
