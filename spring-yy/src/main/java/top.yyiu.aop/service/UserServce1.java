package top.yyiu.aop.service;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 作用于：
 *
 * @author pc
 * @date 2020/4/5
 */
@Component()
//@Scope("prototype")
public class UserServce1 implements InstantiationAwareBeanPostProcessor {

//	@Autowired

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}
//	private UserServiceImpl userService;
}
