package top.yyiu.aop.dao;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import top.yyiu.aop.service.UserServce1;

/**
 * 作用于：
 *
 * @author pc
 * @date 2020/4/4
 */
@Component("userDao1")
public class AUserDaoImpl implements UserDao{

	@Autowired
	ApplicationContext applicationContext;
	@Autowired
	UserServce1 userServce1;

	@Autowired
	public AUserDaoImpl() {
	}

	@Override
	public void update(String sql) {
		System.out.println("target-啊啊啊---"+sql);
//		((AUserDaoImpl) AopContext.currentProxy()).fun();
		System.out.println(applicationContext.getBean("userServce1"));
		System.out.println(applicationContext);
	}

	@Override
	public void fun(){
		System.out.println("fun");
	}
}
