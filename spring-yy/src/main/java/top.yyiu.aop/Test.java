package top.yyiu.aop;


import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
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

		/**AnnotationConfigApplicationContext不支持refresh重复刷新，GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		applicationContext.refresh();
		 */
		/**不会报错
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(AppConfig.class);
		applicationContext.refresh();
		 */
		dao.update("==");
		dao.update("==");
		System.out.println(applicationContext);

//		ApplicationContext applicationContext1 = applicationContext.getBean(ApplicationContext.class);
//		System.out.println(applicationContext1);

	}

	public void test1(){
		// 初始化spring的环境
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		// 不可以注入接口
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition().getBeanDefinition();
		beanDefinition.setBeanClass(UserDao.class);
		applicationContext.registerBeanDefinition("userDao1",beanDefinition);
		applicationContext.refresh();
	}
	public void test2(){
		// 初始化spring的环境
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		applicationContext.register(UserDao.class);
	}

	public void test3(){
		// 可以注入接口
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("userDao",UserDao.class);
		System.out.println(beanFactory.getBean("userDao"));
	}
}


