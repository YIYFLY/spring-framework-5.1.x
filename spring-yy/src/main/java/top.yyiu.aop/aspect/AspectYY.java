package top.yyiu.aop.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 作用于：
 *
 * @author pc
 * @date 2020/4/4
 */
@Component
@Aspect
public class AspectYY {

	@Pointcut("execution(* top.yyiu.aop.dao..*.*(..))")
	public void pointCut(){
	}

	@Before("pointCut()")
	public void before(){
		System.out.println("proxy before");
	}

}
