package top.yyiu.aop.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yyiu.aop.dao.UserDao;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 作用于：
 *
 * @author pc
 * @date 2020/4/4
 */
@Service
public class UserServiceImpl{
	@Autowired
	private UserDao userDao;

	public UserServiceImpl() {
		System.out.println("-------------------");
	}

	@Autowired
	private Map<String, UserDao> map;

//	@Lookup
	public void query(String username) {
		System.out.println("query");
		userDao.update("ss");
		userDao.fun();
	}

	@PostConstruct
	public void fun(){

		System.out.println("PostConstruct");
	}

	@Override
	public String toString() {
		return "UserServiceImpl{" +
				"userDao=" + userDao +
				", map=" + map +
				'}';
	}
}
