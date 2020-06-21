/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.BeanMetadataAttribute;
import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.BeanEntry;
import org.springframework.beans.factory.parsing.ConstructorArgumentEntry;
import org.springframework.beans.factory.parsing.ParseState;
import org.springframework.beans.factory.parsing.PropertyEntry;
import org.springframework.beans.factory.parsing.QualifierEntry;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.LookupOverride;
import org.springframework.beans.factory.support.ManagedArray;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedProperties;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.MethodOverrides;
import org.springframework.beans.factory.support.ReplaceOverride;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Stateful delegate class used to parse XML bean definitions.
 * Intended for use by both the main parser and any extension
 * {@link BeanDefinitionParser BeanDefinitionParsers} or
 * {@link BeanDefinitionDecorator BeanDefinitionDecorators}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Fisher
 * @author Gary Russell
 * @see ParserContext
 * @see DefaultBeanDefinitionDocumentReader
 * @since 2.0
 */
public class BeanDefinitionParserDelegate {

	public static final String BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans";

	public static final String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; ";

	/**
	 * Value of a T/F attribute that represents true.
	 * Anything else represents false. Case seNsItive.
	 */
	public static final String TRUE_VALUE = "true";

	public static final String FALSE_VALUE = "false";

	public static final String DEFAULT_VALUE = "default";

	public static final String DESCRIPTION_ELEMENT = "description";

	public static final String AUTOWIRE_NO_VALUE = "no";

	public static final String AUTOWIRE_BY_NAME_VALUE = "byName";

	public static final String AUTOWIRE_BY_TYPE_VALUE = "byType";

	public static final String AUTOWIRE_CONSTRUCTOR_VALUE = "constructor";

	public static final String AUTOWIRE_AUTODETECT_VALUE = "autodetect";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String BEAN_ELEMENT = "bean";

	public static final String META_ELEMENT = "meta";

	public static final String ID_ATTRIBUTE = "id";

	public static final String PARENT_ATTRIBUTE = "parent";

	public static final String CLASS_ATTRIBUTE = "class";

	public static final String ABSTRACT_ATTRIBUTE = "abstract";

	public static final String SCOPE_ATTRIBUTE = "scope";

	private static final String SINGLETON_ATTRIBUTE = "singleton";

	public static final String LAZY_INIT_ATTRIBUTE = "lazy-init";

	public static final String AUTOWIRE_ATTRIBUTE = "autowire";

	public static final String AUTOWIRE_CANDIDATE_ATTRIBUTE = "autowire-candidate";

	public static final String PRIMARY_ATTRIBUTE = "primary";

	public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";

	public static final String INIT_METHOD_ATTRIBUTE = "init-method";

	public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";

	public static final String FACTORY_METHOD_ATTRIBUTE = "factory-method";

	public static final String FACTORY_BEAN_ATTRIBUTE = "factory-bean";

	public static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";

	public static final String INDEX_ATTRIBUTE = "index";

	public static final String TYPE_ATTRIBUTE = "type";

	public static final String VALUE_TYPE_ATTRIBUTE = "value-type";

	public static final String KEY_TYPE_ATTRIBUTE = "key-type";

	public static final String PROPERTY_ELEMENT = "property";

	public static final String REF_ATTRIBUTE = "ref";

	public static final String VALUE_ATTRIBUTE = "value";

	public static final String LOOKUP_METHOD_ELEMENT = "lookup-method";

	public static final String REPLACED_METHOD_ELEMENT = "replaced-method";

	public static final String REPLACER_ATTRIBUTE = "replacer";

	public static final String ARG_TYPE_ELEMENT = "arg-type";

	public static final String ARG_TYPE_MATCH_ATTRIBUTE = "match";

	public static final String REF_ELEMENT = "ref";

	public static final String IDREF_ELEMENT = "idref";

	public static final String BEAN_REF_ATTRIBUTE = "bean";

	public static final String PARENT_REF_ATTRIBUTE = "parent";

	public static final String VALUE_ELEMENT = "value";

	public static final String NULL_ELEMENT = "null";

	public static final String ARRAY_ELEMENT = "array";

	public static final String LIST_ELEMENT = "list";

	public static final String SET_ELEMENT = "set";

	public static final String MAP_ELEMENT = "map";

	public static final String ENTRY_ELEMENT = "entry";

	public static final String KEY_ELEMENT = "key";

	public static final String KEY_ATTRIBUTE = "key";

	public static final String KEY_REF_ATTRIBUTE = "key-ref";

	public static final String VALUE_REF_ATTRIBUTE = "value-ref";

	public static final String PROPS_ELEMENT = "props";

	public static final String PROP_ELEMENT = "prop";

	public static final String MERGE_ATTRIBUTE = "merge";

	public static final String QUALIFIER_ELEMENT = "qualifier";

	public static final String QUALIFIER_ATTRIBUTE_ELEMENT = "attribute";

	public static final String DEFAULT_LAZY_INIT_ATTRIBUTE = "default-lazy-init";

	public static final String DEFAULT_MERGE_ATTRIBUTE = "default-merge";

	public static final String DEFAULT_AUTOWIRE_ATTRIBUTE = "default-autowire";

	public static final String DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE = "default-autowire-candidates";

	public static final String DEFAULT_INIT_METHOD_ATTRIBUTE = "default-init-method";

	public static final String DEFAULT_DESTROY_METHOD_ATTRIBUTE = "default-destroy-method";


	protected final Log logger = LogFactory.getLog(getClass());

	private final XmlReaderContext readerContext;

	private final DocumentDefaultsDefinition defaults = new DocumentDefaultsDefinition();

	private final ParseState parseState = new ParseState();

	/**
	 * Stores all used bean names so we can enforce uniqueness on a per
	 * beans-element basis. Duplicate bean ids/names may not exist within the
	 * same level of beans element nesting, but may be duplicated across levels.
	 */
	private final Set<String> usedNames = new HashSet<>();


	/**
	 * Create a new BeanDefinitionParserDelegate associated with the supplied
	 * {@link XmlReaderContext}.
	 */
	public BeanDefinitionParserDelegate(XmlReaderContext readerContext) {
		Assert.notNull(readerContext, "XmlReaderContext must not be null");
		this.readerContext = readerContext;
	}


	/**
	 * Get the {@link XmlReaderContext} associated with this helper instance.
	 */
	public final XmlReaderContext getReaderContext() {
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor}
	 * to pull the source metadata from the supplied {@link Element}.
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return this.readerContext.extractSource(ele);
	}

	/**
	 * Report an error with the given message for the given source element.
	 */
	protected void error(String message, Node source) {
		this.readerContext.error(message, source, this.parseState.snapshot());
	}

	/**
	 * Report an error with the given message for the given source element.
	 */
	protected void error(String message, Element source) {
		this.readerContext.error(message, source, this.parseState.snapshot());
	}

	/**
	 * Report an error with the given message for the given source element.
	 */
	protected void error(String message, Element source, Throwable cause) {
		this.readerContext.error(message, source, this.parseState.snapshot(), cause);
	}


	/**
	 * Initialize the default settings assuming a {@code null} parent delegate.
	 */
	public void initDefaults(Element root) {
		initDefaults(root, null);
	}

	/**
	 * Initialize the default lazy-init, autowire, dependency check settings,
	 * init-method, destroy-method and merge settings. Support nested 'beans'
	 * element use cases by falling back to the given parent in case the
	 * defaults are not explicitly set locally.
	 *
	 * @see #populateDefaults(DocumentDefaultsDefinition, DocumentDefaultsDefinition, org.w3c.dom.Element)
	 * @see #getDefaults()
	 */
	public void initDefaults(Element root, @Nullable BeanDefinitionParserDelegate parent) {
		populateDefaults(this.defaults, (parent != null ? parent.defaults : null), root);
		this.readerContext.fireDefaultsRegistered(this.defaults);
	}

	/**
	 * Populate the given DocumentDefaultsDefinition instance with the default lazy-init,
	 * autowire, dependency check settings, init-method, destroy-method and merge settings.
	 * Support nested 'beans' element use cases by falling back to {@code parentDefaults}
	 * in case the defaults are not explicitly set locally.
	 *
	 * @param defaults       the defaults to populate
	 * @param parentDefaults the parent BeanDefinitionParserDelegate (if any) defaults to fall back to
	 * @param root           the root element of the current bean definition document (or nested beans element)
	 */
	protected void populateDefaults(DocumentDefaultsDefinition defaults, @Nullable DocumentDefaultsDefinition parentDefaults, Element root) {
		String lazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
		if (isDefaultValue(lazyInit)) {
			// Potentially inherited from outer <beans> sections, otherwise falling back to false.
			lazyInit = (parentDefaults != null ? parentDefaults.getLazyInit() : FALSE_VALUE);
		}
		defaults.setLazyInit(lazyInit);

		String merge = root.getAttribute(DEFAULT_MERGE_ATTRIBUTE);
		if (isDefaultValue(merge)) {
			// Potentially inherited from outer <beans> sections, otherwise falling back to false.
			merge = (parentDefaults != null ? parentDefaults.getMerge() : FALSE_VALUE);
		}
		defaults.setMerge(merge);

		String autowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE);
		if (isDefaultValue(autowire)) {
			// Potentially inherited from outer <beans> sections, otherwise falling back to 'no'.
			autowire = (parentDefaults != null ? parentDefaults.getAutowire() : AUTOWIRE_NO_VALUE);
		}
		defaults.setAutowire(autowire);

		if (root.hasAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE)) {
			defaults.setAutowireCandidates(root.getAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE));
		} else if (parentDefaults != null) {
			defaults.setAutowireCandidates(parentDefaults.getAutowireCandidates());
		}

		if (root.hasAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE)) {
			defaults.setInitMethod(root.getAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE));
		} else if (parentDefaults != null) {
			defaults.setInitMethod(parentDefaults.getInitMethod());
		}

		if (root.hasAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE)) {
			defaults.setDestroyMethod(root.getAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE));
		} else if (parentDefaults != null) {
			defaults.setDestroyMethod(parentDefaults.getDestroyMethod());
		}

		defaults.setSource(this.readerContext.extractSource(root));
	}

	/**
	 * Return the defaults definition object.
	 */
	public DocumentDefaultsDefinition getDefaults() {
		return this.defaults;
	}

	/**
	 * Return the default settings for bean definitions as indicated within
	 * the attributes of the top-level {@code <beans/>} element.
	 */
	public BeanDefinitionDefaults getBeanDefinitionDefaults() {
		BeanDefinitionDefaults bdd = new BeanDefinitionDefaults();
		bdd.setLazyInit(TRUE_VALUE.equalsIgnoreCase(this.defaults.getLazyInit()));
		bdd.setAutowireMode(getAutowireMode(DEFAULT_VALUE));
		bdd.setInitMethodName(this.defaults.getInitMethod());
		bdd.setDestroyMethodName(this.defaults.getDestroyMethod());
		return bdd;
	}

	/**
	 * Return any patterns provided in the 'default-autowire-candidates'
	 * attribute of the top-level {@code <beans/>} element.
	 */
	@Nullable
	public String[] getAutowireCandidatePatterns() {
		String candidatePattern = this.defaults.getAutowireCandidates();
		return (candidatePattern != null ? StringUtils.commaDelimitedListToStringArray(candidatePattern) : null);
	}


	/**
	 * Parses the supplied {@code <bean>} element. May return {@code null}
	 * if there were errors during parse. Errors are reported to the
	 * {@link org.springframework.beans.factory.parsing.ProblemReporter}.
	 */
	@Nullable
	public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
		//解析元素
		return parseBeanDefinitionElement(ele, null);
	}

	/**
	 * Parses the supplied {@code <bean>} element. May return {@code null}
	 * if there were errors during parse. Errors are reported to the
	 * bean标签的解析，将标签元素转成BeanDefinitionHolder
	 * {@link org.springframework.beans.factory.parsing.ProblemReporter}.
	 */
	@Nullable
	public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
		// 1.解析name和id属性
		// 获取ID元素
		String id = ele.getAttribute(ID_ATTRIBUTE);
		// 获取name元素
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		//别名解析
		// 分割name属性（通过逗号或分号）
		// 例如：<bean name="demoService,demoServiceAlias" class=""/>，分割后aliases为[demoService, demoServiceAlias]
		List<String> aliases = new ArrayList<>();
		if (StringUtils.hasLength(nameAttr)) {
			String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
			aliases.addAll(Arrays.asList(nameArr));
		}
		// beanName默认使用id
		String beanName = id;
		if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
			// 如果id为空，并且aliases不为空，则取aliases的第一个元素作为beanName，其他的仍作为别名
			beanName = aliases.remove(0);
			if (logger.isTraceEnabled()) {
				logger.trace("No XML 'id' specified - using '" + beanName +
						"' as bean name and " + aliases + " as aliases");
			}
		}
		//检查beanName是否重复
		if (containingBean == null) {
			// 检查beanName和aliases是否在同一个 <beans> 下已经存在
			checkNameUniqueness(beanName, aliases, ele);
		}
		// 2.进一步解析bean的其他所有属性并统一封装至GenericBeanDefinition类型实例中
		//解析的核心方法 将element元素解析成beanDefinition 重要程度5
		AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
		if (beanDefinition != null) {
			if (!StringUtils.hasText(beanName)) {
				try {
					// 3.如果bean定义存在，但是beanName为空，则用Spring默认的生成规则为当前bean生成beanName
					if (containingBean != null) {
						beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, this.readerContext.getRegistry(), true);
					} else {
						// Spring提供的生成规则生成beanName，例如：com.joonwhee.open.demo.service.impl.DemoServiceImpl#0
						beanName = this.readerContext.generateBeanName(beanDefinition);
						// Register an alias for the plain bean class name, if still possible,
						// if the generator returned the class name plus a suffix.
						// This is expected for Spring 1.2/2.0 backwards compatibility.
						String beanClassName = beanDefinition.getBeanClassName();
						if (beanClassName != null &&
								beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
								!this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
							// 如果Spring默认的生成规则生成的beanName为:类名加后缀，则将类名注册为别名
							aliases.add(beanClassName);
						}
					}
					if (logger.isTraceEnabled()) {
						logger.trace("Neither XML 'id' nor 'name' specified - " +
								"using generated bean name [" + beanName + "]");
					}
				} catch (Exception ex) {
					error(ex.getMessage(), ele);
					return null;
				}
			}
			String[] aliasesArray = StringUtils.toStringArray(aliases);
			// 4.将bean定义、beanName、bean别名数组封装成BeanDefinitionHolder
			//创建BeanDefinitionHolder
			return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
		}

		return null;
	}

	/**
	 * Validate that the specified bean name and aliases have not been used already
	 * within the current level of beans element nesting.
	 */
	protected void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
		String foundName = null;

		if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
			foundName = beanName;
		}
		if (foundName == null) {
			foundName = CollectionUtils.findFirstMatch(this.usedNames, aliases);
		}
		if (foundName != null) {
			error("Bean name '" + foundName + "' is already used in this <beans> element", beanElement);
		}

		this.usedNames.add(beanName);
		this.usedNames.addAll(aliases);
	}

	/**
	 * Parse the bean definition itself, without regard to name or aliases. May return
	 * {@code null} if problems occurred during the parsing of the bean definition.
	 * 将Element 元素解析成 BeanDefinition
	 */
	@Nullable
	public AbstractBeanDefinition parseBeanDefinitionElement(
			Element ele, String beanName, @Nullable BeanDefinition containingBean) {

		this.parseState.push(new BeanEntry(beanName));

		String className = null;
		// 1.解析class、parent属性
		// 解析class属性
		if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
			className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
		}
		String parent = null;
		// 解析parent属性
		if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
			parent = ele.getAttribute(PARENT_ATTRIBUTE);
		}

		try {
			// 2.创建用于承载属性的AbstractBeanDefinition类型的GenericBeanDefinition
			//创建GenericBeanDefinition对象
			AbstractBeanDefinition bd = createBeanDefinition(className, parent);
			//3. 解析bean标签的属性，并把解析出来的属性设置到BeanDefinition对象中
			parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
			//4. 提取description的描述信息
			bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));
			//解析元数据子节点(基本不用, 不深入介绍)
			parseMetaElements(ele, bd);

			//解析bean中的lookup-method标签  重要程度：2，可看可不看
			parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
			//解析bean中的replaced-method标签  重要程度：2，可看可不看
			parseReplacedMethodSubElements(ele, bd.getMethodOverrides());

			//解析bean中的constructor-arg标签  重要程度：2，可看可不看
			parseConstructorArgElements(ele, bd);
			//解析bean中的property标签  重要程度：2，可看可不看
			parsePropertyElements(ele, bd);
			//解析bean中的Qualifier标签 可以不看，用不到
			parseQualifierElements(ele, bd);
			//设置Resource属性
			bd.setResource(this.readerContext.getResource());
			//设置Source属性
			bd.setSource(extractSource(ele));

			return bd;
		} catch (ClassNotFoundException ex) {
			error("Bean class [" + className + "] not found", ele, ex);
		} catch (NoClassDefFoundError err) {
			error("Class that bean class [" + className + "] depends on not found", ele, err);
		} catch (Throwable ex) {
			error("Unexpected failure during bean definition parsing", ele, ex);
		} finally {
			this.parseState.pop();
		}

		return null;
	}

	/**
	 * Apply the attributes of the given bean element to the given bean * definition.
	 * 解析bean标签的属性，并把解析出来的属性设置到BeanDefinition对象中
	 *
	 * @param ele            bean declaration element
	 * @param beanName       bean name
	 * @param containingBean containing bean definition
	 * @return a bean definition initialized according to the bean element attributes
	 */
	public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName, @Nullable BeanDefinition containingBean, AbstractBeanDefinition bd) {

		// 解析singleton属性
		// 对 singleton="true/false" 进行校验
		//singleton 现已废弃使用 scop进行操作
		if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
			// singleton属性已经不支持, 如果使用了会直接抛出异常, 请使用scope属性代替
			error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
		}
		// 解析scope属性
		else if (ele.hasAttribute(SCOPE_ATTRIBUTE)) {
			bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
		} else if (containingBean != null) {
			// Take default from containing bean in case of an inner bean definition.
			//在内部bean定义的情况下，从包含bean中获取默认值。
			bd.setScope(containingBean.getScope());
		}

		// 是否包含属性 abstract 父子beanBeanDefinition
		/**
		 * 		父类parent会封装成beanDefinition但是不会实例化
		 *     <!--这两个beanDefinition会在实例化过程中合并成一个-->
		 *     <bean id="parent" class="com.xiangxue.jack.bean.Parent" abstract="true">
		 *         <property name="username" value="parent name"/>
		 *     </bean>
		 *     <bean id="son" class="com.xiangxue.jack.bean.Son" parent="parent">
		 *         <!--<property name="username" value="son name"/>-->
		 *         <property name="age" value="18"/>
		 *     </bean>
		 *
		 *     如果自类没有username属性则会集成父类的username属性parent name
		 *
		 */
		if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
			bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
		}

		String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
		if (isDefaultValue(lazyInit)) {
			lazyInit = this.defaults.getLazyInit();
		}
		// 解析lazy-init属性, 默认为false
		bd.setLazyInit(TRUE_VALUE.equals(lazyInit));
		// 解析autowire属性
		String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
		bd.setAutowireMode(getAutowireMode(autowire));
		//depends-on 属性
		//spring允许用户通过depends-on属性指定bean前置依赖的bean,前置依赖的bean会在本bean实例化之前创建好
		// <bean id="manager" class="CacheManager"  depends-on="sysinit"/> 前置依赖
		if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
			String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
			bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
		}
		/**
		 *   <bean class="com.xiangxue.jack.service.AccountServiceImpl" id="accountService"
		 *     autowire-candidate="false" scope="singleton" primary="true"/>
		 *  如果 autowire-candidate 为false 表示该类不参与依赖注入
		 */
		String autowireCandidate = ele.getAttribute(AUTOWIRE_CANDIDATE_ATTRIBUTE);
		if (isDefaultValue(autowireCandidate)) {
			String candidatePattern = this.defaults.getAutowireCandidates();
			if (candidatePattern != null) {
				String[] patterns = StringUtils.commaDelimitedListToStringArray(candidatePattern);
				bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
			}
		} else {
			bd.setAutowireCandidate(TRUE_VALUE.equals(autowireCandidate));
		}
		//primary 如果存在多个只加载primary的
		if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) {
			bd.setPrimary(TRUE_VALUE.equals(ele.getAttribute(PRIMARY_ATTRIBUTE)));
		}
		// 解析init-method属性
		if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
			String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
			bd.setInitMethodName(initMethodName);
		} else if (this.defaults.getInitMethod() != null) {
			bd.setInitMethodName(this.defaults.getInitMethod());
			bd.setEnforceInitMethod(false);
		}
		// 解析destroy-method属性
		if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
			String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
			bd.setDestroyMethodName(destroyMethodName);
		} else if (this.defaults.getDestroyMethod() != null) {
			bd.setDestroyMethodName(this.defaults.getDestroyMethod());
			bd.setEnforceDestroyMethod(false);
		}
		// 解析factory-method属性
		//factory-method 工厂方法，通过factory-method来创建bean的实例
		if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
			bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
		}
		// 解析factory-bean属性
		//factory-bean 工厂bean 通过另一个bean来创建该bean的实例 自己来做实例化不交给spring实例化
		if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
			bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
		}

		return bd;
	}

	/**
	 * Create a bean definition for the given class name and parent name.
	 *
	 * @param className  the name of the bean class
	 * @param parentName the name of the bean's parent bean
	 * @return the newly created bean definition
	 * @throws ClassNotFoundException if bean class resolution was attempted but failed
	 */
	protected AbstractBeanDefinition createBeanDefinition(@Nullable String className, @Nullable String parentName) throws ClassNotFoundException {
		//返回创建的BeanDefinition
		return BeanDefinitionReaderUtils.createBeanDefinition(parentName, className, this.readerContext.getBeanClassLoader());
	}

	/**
	 * Parse the meta elements underneath the given element, if any.
	 */
	public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
		NodeList nl = ele.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, META_ELEMENT)) {
				Element metaElement = (Element) node;
				String key = metaElement.getAttribute(KEY_ATTRIBUTE);
				String value = metaElement.getAttribute(VALUE_ATTRIBUTE);
				BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
				attribute.setSource(extractSource(metaElement));
				attributeAccessor.addMetadataAttribute(attribute);
			}
		}
	}

	/**
	 * Parse the given autowire attribute value into
	 * {@link AbstractBeanDefinition} autowire constants.
	 */
	@SuppressWarnings("deprecation")
	public int getAutowireMode(String attrValue) {
		String attr = attrValue;
		if (isDefaultValue(attr)) {
			attr = this.defaults.getAutowire();
		}
		int autowire = AbstractBeanDefinition.AUTOWIRE_NO;
		if (AUTOWIRE_BY_NAME_VALUE.equals(attr)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_BY_NAME;
		} else if (AUTOWIRE_BY_TYPE_VALUE.equals(attr)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
		} else if (AUTOWIRE_CONSTRUCTOR_VALUE.equals(attr)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;
		} else if (AUTOWIRE_AUTODETECT_VALUE.equals(attr)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_AUTODETECT;
		}
		// Else leave default value.
		return autowire;
	}

	/**
	 * Parse constructor-arg sub-elements of the given bean element.
	 * 解析bean中的constructor-arg标签
	 */
	public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
		// 拿到beanEle节点的所有子节点
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
				// 解析constructor-arg
				parseConstructorArgElement((Element) node, bd);
			}
		}
	}

	/**
	 * Parse property sub-elements of the given bean element.
	 * 解析bean中的property标签
	 */
	public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
		// 拿到beanEle节点的所有子节点
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, PROPERTY_ELEMENT)) {
				// 解析property节点
				parsePropertyElement((Element) node, bd);
			}
		}
	}

	/**
	 * Parse qualifier sub-elements of the given bean element.
	 * 解析bean中的Qualifier标签
	 */
	public void parseQualifierElements(Element beanEle, AbstractBeanDefinition bd) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ELEMENT)) {
				parseQualifierElement((Element) node, bd);
			}
		}
	}

	/**
	 * Parse lookup-override sub-elements of the given bean element.
	 * 解析bean中的lookup-method标签
	 */
	public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, LOOKUP_METHOD_ELEMENT)) {
				Element ele = (Element) node;
				String methodName = ele.getAttribute(NAME_ATTRIBUTE);
				String beanRef = ele.getAttribute(BEAN_ELEMENT);
				//LookupOverride对象主要用来包装lookup属性
				LookupOverride override = new LookupOverride(methodName, beanRef);
				override.setSource(extractSource(ele));
				//加入到overrides的容器
				overrides.addOverride(override);
			}
		}
	}

	/**
	 * Parse replaced-method sub-elements of the given bean element.
	 *
	 * <bean id="replaceClass" class="com.xiangxue.jack.bean.ReplaceClass" lazy-init="false"/>
	 * <bean id="originClass" class="com.xiangxue.jack.bean.OriginClass">
	 * <replaced-method name="method" replacer="replaceClass">
	 * <!--方法可能出现重载的情况，要根据类型和方法名找方法-->
	 * <arg-type match="java.lang.String"/>
	 * </replaced-method>
	 * </bean>
	 * <p>
	 * 用于 不改变代码的情况下 通过spring xml来实现方法的重载
	 */
	public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (isCandidateElement(node) && nodeNameEquals(node, REPLACED_METHOD_ELEMENT)) {
				Element replacedMethodEle = (Element) node;
				String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
				String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
				//一个replaced-method标签封装成一个ReplaceOverride对象，最后加入到BeanDefinition对象中
				ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
				// Look for arg-type match elements.
				List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, ARG_TYPE_ELEMENT);
				for (Element argTypeEle : argTypeEles) {
					String match = argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE);
					//根据方法参数类型来区分同名的不同的方法
					match = (StringUtils.hasText(match) ? match : DomUtils.getTextValue(argTypeEle));
					if (StringUtils.hasText(match)) {
						replaceOverride.addTypeIdentifier(match);
					}
				}
				replaceOverride.setSource(extractSource(replacedMethodEle));
				overrides.addOverride(replaceOverride);
			}
		}
	}

	/**
	 * Parse a constructor-arg element.
	 * 解析构造函数属性
	 */
	public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
		// 1.提取基础属性index、type、name属性值
		// 提取index属性
		String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
		// 提取type属性
		String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
		// 提取name属性
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		if (StringUtils.hasLength(indexAttr)) {
			try {
				int index = Integer.parseInt(indexAttr);
				if (index < 0) {
					error("'index' cannot be lower than 0", ele);
				} else {
					try {
						// 2.index不为空的处理
						this.parseState.push(new ConstructorArgumentEntry(index));
						// 2.1解析ele节点对应的属性值
						Object value = parsePropertyValue(ele, bd, null);
						// 2.2使用ConstructorArgumentValues.ValueHolder类型来封装解析出来的元素
						ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
						// 2.3将type属性封装到ConstructorArgumentValues.ValueHolder
						if (StringUtils.hasLength(typeAttr)) {
							valueHolder.setType(typeAttr);
						}
						// 2.4将name属性封装到ConstructorArgumentValues.ValueHolder
						if (StringUtils.hasLength(nameAttr)) {
							valueHolder.setName(nameAttr);
						}
						valueHolder.setSource(extractSource(ele));
						// 2.5判断index是否重复指定, 如果是则抛出异常
						if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
							error("Ambiguous constructor-arg entries for index " + index, ele);
						} else {
							// 将index和valueHolder以key-value形式添加至当前BeanDefinition的constructorArgumentValues
							// 的indexedArgumentValues属性中，（用于上面判断index是否重复指定）
							//将构造方法以及索引加入ConstructorArgumentValues 的容器
							bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
						}
					} finally {
						this.parseState.pop();
					}
				}
			} catch (NumberFormatException ex) {
				error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
			}
		} else {
			try {
				// 3.index为空的处理
				this.parseState.push(new ConstructorArgumentEntry());
				// 3.1解析ele节点对应的属性值
				Object value = parsePropertyValue(ele, bd, null);
				// 3.2使用ConstructorArgumentValues.ValueHolder类型来封装解析出来的元素
				ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
				// 3.3将type属性封装到ConstructorArgumentValues.ValueHolder
				if (StringUtils.hasLength(typeAttr)) {
					valueHolder.setType(typeAttr);
				}
				// 3.4将name属性封装到ConstructorArgumentValues.ValueHolder
				if (StringUtils.hasLength(nameAttr)) {
					valueHolder.setName(nameAttr);
				}
				valueHolder.setSource(extractSource(ele));
				// 3.5将valueHolder添加至当前BeanDefinition的constructorArgumentValues的genericArgumentValues属性中
				// 与上面的indexedArgumentValues类似，上面有index存为map，这边没index存为list
				bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
			} finally {
				this.parseState.pop();
			}
		}
	}

	/**
	 * Parse a property element.
	 */
	public void parsePropertyElement(Element ele, BeanDefinition bd) {
		// 1.拿到name属性
		String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
		if (!StringUtils.hasLength(propertyName)) {
			// name属性为必要属性，如果没有配置，则抛出异常
			error("Tag 'property' must have a 'name' attribute", ele);
			return;
		}
		this.parseState.push(new PropertyEntry(propertyName));
		try {
			// 2.校验在相同bean节点下，是否存在相同的name属性，如果存在则抛出异常
			if (bd.getPropertyValues().contains(propertyName)) {
				error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
				return;
			}
			// 3.解析属性值
			Object val = parsePropertyValue(ele, bd, propertyName);
			// 4.将解析的属性值和属性name封装成PropertyValue
			PropertyValue pv = new PropertyValue(propertyName, val);
			// 5.解析meta节点（基本不用，不深入解析）
			parseMetaElements(ele, pv);
			pv.setSource(extractSource(ele));
			// 6.将解析出来的PropertyValue，添加到BeanDefinition的propertyValues属性中(上面的重复校验用到)
			bd.getPropertyValues().addPropertyValue(pv);
		} finally {
			this.parseState.pop();
		}
	}

	/**
	 * Parse a qualifier element.
	 */
	public void parseQualifierElement(Element ele, AbstractBeanDefinition bd) {
		String typeName = ele.getAttribute(TYPE_ATTRIBUTE);
		if (!StringUtils.hasLength(typeName)) {
			error("Tag 'qualifier' must have a 'type' attribute", ele);
			return;
		}
		this.parseState.push(new QualifierEntry(typeName));
		try {
			AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(typeName);
			qualifier.setSource(extractSource(ele));
			String value = ele.getAttribute(VALUE_ATTRIBUTE);
			if (StringUtils.hasLength(value)) {
				qualifier.setAttribute(AutowireCandidateQualifier.VALUE_KEY, value);
			}
			NodeList nl = ele.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ATTRIBUTE_ELEMENT)) {
					Element attributeEle = (Element) node;
					String attributeName = attributeEle.getAttribute(KEY_ATTRIBUTE);
					String attributeValue = attributeEle.getAttribute(VALUE_ATTRIBUTE);
					if (StringUtils.hasLength(attributeName) && StringUtils.hasLength(attributeValue)) {
						BeanMetadataAttribute attribute = new BeanMetadataAttribute(attributeName, attributeValue);
						attribute.setSource(extractSource(attributeEle));
						qualifier.addMetadataAttribute(attribute);
					} else {
						error("Qualifier 'attribute' tag must have a 'name' and 'value'", attributeEle);
						return;
					}
				}
			}
			bd.addQualifier(qualifier);
		} finally {
			this.parseState.pop();
		}
	}

	/**
	 * Get the value of a property element. May be a list etc.
	 * Also used for constructor arguments, "propertyName" being null in this case.
	 */
	@Nullable
	public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
		String elementName = (propertyName != null ?
				"<property> element for property '" + propertyName + "'" :
				"<constructor-arg> element");

		// Should only have one child element: ref, value, list, etc.
		// 1.拿到ele节点的子节点，例如list、map
		NodeList nl = ele.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			// 跳过description或者meta节点
			if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) && !nodeNameEquals(node, META_ELEMENT)) {
				// Child element is what we're looking for.
				if (subElement != null) {
					// 只能有1个子节点，否则抛出异常
					error(elementName + " must not contain more than one sub-element", ele);
				} else {
					// 找到子节点，赋值给subElement
					subElement = (Element) node;
				}
			}
		}
		// 2.解析constructor-arg上的ref属性
		boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
		// 3.解析constructor-arg上的value属性
		boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
		// 4.合法性校验。在constructor-arg上：ref属性、value属性、子节点，三者只能有1个，因为这三个都是用来表示该节点的值。
		if ((hasRefAttribute && hasValueAttribute) || ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
			error(elementName + " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
		}
		// 5.ref属性的处理，使用RuntimeBeanReference封装对应的ref值（该ref值指向另一个bean的beanName），
		// RuntimeBeanReference起到占位符的作用，ref指向的beanName将在运行时被解析成真正的bean实例引用
		if (hasRefAttribute) {
			String refName = ele.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(refName)) {
				error(elementName + " contains empty 'ref' attribute", ele);
			}
			RuntimeBeanReference ref = new RuntimeBeanReference(refName);
			ref.setSource(extractSource(ele));
			return ref;
		} else if (hasValueAttribute) {
			// 6.value属性的处理，使用TypedStringValue封装
			TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
			valueHolder.setSource(extractSource(ele));
			return valueHolder;
		} else if (subElement != null) {
			// 7.解析子节点
			return parsePropertySubElement(subElement, bd);
		} else {
			// Neither child element nor "ref" or "value" attribute found.
			// 8.既没有ref属性，也没有value属性，也没有子节点，没法获取ele节点的值，直接抛异常
			error(elementName + " must specify a ref or value", ele);
			return null;
		}
	}

	/**
	 * Parse a value, ref or collection sub-element of a property or
	 * constructor-arg element.
	 *
	 * @param ele subelement of property element; we don't know which yet
	 * @param bd  the current bean definition (if any)
	 */
	@Nullable
	public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd) {
		return parsePropertySubElement(ele, bd, null);
	}

	/**
	 * Parse a value, ref or collection sub-element of a property or
	 * constructor-arg element.
	 *
	 * @param ele              subelement of property element; we don't know which yet
	 * @param bd               the current bean definition (if any)
	 * @param defaultValueType the default type (class name) for any
	 *                         {@code <value>} tag that might be created
	 */
	@Nullable
	public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd, @Nullable String defaultValueType) {
		// 1.校验是否为默认的命名空间，如果不是则走解析自定义节点代码
		if (!isDefaultNamespace(ele)) {
			return parseNestedCustomElement(ele, bd);
		}
		// 2.解析bean节点
		else if (nodeNameEquals(ele, BEAN_ELEMENT)) {
			BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
			if (nestedBd != null) {
				nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
			}
			return nestedBd;
		}
		// 3.解析ref节点
		else if (nodeNameEquals(ele, REF_ELEMENT)) {
			// A generic reference to any name of any bean.
			String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
			boolean toParent = false;
			if (!StringUtils.hasLength(refName)) {
				// A reference to the id of another bean in a parent context.
				refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
				toParent = true;
				if (!StringUtils.hasLength(refName)) {
					error("'bean' or 'parent' is required for <ref> element", ele);
					return null;
				}
			}
			if (!StringUtils.hasText(refName)) {
				error("<ref> element contains empty target attribute", ele);
				return null;
			}
			RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
			ref.setSource(extractSource(ele));
			return ref;
		}
		// 4.解析idref节点
		else if (nodeNameEquals(ele, IDREF_ELEMENT)) {
			return parseIdRefElement(ele);
		} else if (nodeNameEquals(ele, VALUE_ELEMENT)) {
			// 5.解析value节点
			return parseValueElement(ele, defaultValueType);
		} else if (nodeNameEquals(ele, NULL_ELEMENT)) {
			// 6.解析null节点
			// It's a distinguished null value. Let's wrap it in a TypedStringValue
			// object in order to preserve the source location.
			TypedStringValue nullHolder = new TypedStringValue(null);
			nullHolder.setSource(extractSource(ele));
			return nullHolder;
		} else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
			// 7.解析array节点
			return parseArrayElement(ele, bd);
		} else if (nodeNameEquals(ele, LIST_ELEMENT)) {
			// 8.解析list节点
			return parseListElement(ele, bd);
		} else if (nodeNameEquals(ele, SET_ELEMENT)) {
			// 9.解析set节点
			return parseSetElement(ele, bd);
		} else if (nodeNameEquals(ele, MAP_ELEMENT)) {
			// 10.解析map节点
			return parseMapElement(ele, bd);
		} else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
			// 11.解析props节点
			return parsePropsElement(ele);
		} else {
			// 12.未知属性，抛异常
			error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
			return null;
		}
	}

	/**
	 * Return a typed String value Object for the given 'idref' element.
	 */
	@Nullable
	public Object parseIdRefElement(Element ele) {
		// A generic reference to any name of any bean.
		String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
		if (!StringUtils.hasLength(refName)) {
			error("'bean' is required for <idref> element", ele);
			return null;
		}
		if (!StringUtils.hasText(refName)) {
			error("<idref> element contains empty target attribute", ele);
			return null;
		}
		RuntimeBeanNameReference ref = new RuntimeBeanNameReference(refName);
		ref.setSource(extractSource(ele));
		return ref;
	}

	/**
	 * Return a typed String value Object for the given value element.
	 */
	public Object parseValueElement(Element ele, @Nullable String defaultTypeName) {
		// It's a literal value.
		// 拿到ele节点值
		String value = DomUtils.getTextValue(ele);
		// 拿到ele节点的type属性
		String specifiedTypeName = ele.getAttribute(TYPE_ATTRIBUTE);
		String typeName = specifiedTypeName;
		if (!StringUtils.hasText(typeName)) {
			// ele节点没有type属性则则使用入参defaultTypeName
			typeName = defaultTypeName;
		}
		try {
			// 1.使用value和type构建TypedStringValue
			TypedStringValue typedValue = buildTypedStringValue(value, typeName);
			typedValue.setSource(extractSource(ele));
			typedValue.setSpecifiedTypeName(specifiedTypeName);
			return typedValue;
		} catch (ClassNotFoundException ex) {
			error("Type class [" + typeName + "] not found for <value> element", ele, ex);
			return value;
		}
	}

	/**
	 * Build a typed String value Object for the given raw value.
	 *
	 * @see org.springframework.beans.factory.config.TypedStringValue
	 */
	protected TypedStringValue buildTypedStringValue(String value, @Nullable String targetTypeName)
			throws ClassNotFoundException {

		ClassLoader classLoader = this.readerContext.getBeanClassLoader();
		TypedStringValue typedValue;
		// 1.targetTypeName为空，则只使用value来构建TypedStringValue
		if (!StringUtils.hasText(targetTypeName)) {
			typedValue = new TypedStringValue(value);
		} else if (classLoader != null) {
			// 2.targetTypeName不为空，并且classLoader不为null
			// 2.1 利用反射，构建出type的Class，如果type是基本类型，或者 java.lang 包下的常用类，
			// 可以直接从缓存（primitiveTypeNameMap、commonClassCache）中获取
			Class<?> targetType = ClassUtils.forName(targetTypeName, classLoader);
			typedValue = new TypedStringValue(value, targetType);
		} else {
			typedValue = new TypedStringValue(value, targetTypeName);
		}
		return typedValue;
	}

	/**
	 * Parse an array element.
	 */
	public Object parseArrayElement(Element arrayEle, @Nullable BeanDefinition bd) {
		String elementType = arrayEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		NodeList nl = arrayEle.getChildNodes();
		ManagedArray target = new ManagedArray(elementType, nl.getLength());
		target.setSource(extractSource(arrayEle));
		target.setElementTypeName(elementType);
		target.setMergeEnabled(parseMergeAttribute(arrayEle));
		parseCollectionElements(nl, target, bd, elementType);
		return target;
	}

	/**
	 * Parse a list element.
	 */
	public List<Object> parseListElement(Element collectionEle, @Nullable BeanDefinition bd) {
		// 1.拿到collectionEle节点的value-type，顾名思义，该属性就是该list节点下的value的类型
		String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		// 2.拿到collectionEle节点的所有子节点, 一般为<value>
		NodeList nl = collectionEle.getChildNodes();
		// 3.new一个ManagedList，用于存放字节点的值
		ManagedList<Object> target = new ManagedList<>(nl.getLength());
		target.setSource(extractSource(collectionEle));
		target.setElementTypeName(defaultElementType);
		target.setMergeEnabled(parseMergeAttribute(collectionEle));
		// 4.解析子节点集合
		parseCollectionElements(nl, target, bd, defaultElementType);
		return target;
	}

	/**
	 * Parse a set element.
	 */
	public Set<Object> parseSetElement(Element collectionEle, @Nullable BeanDefinition bd) {
		String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		NodeList nl = collectionEle.getChildNodes();
		ManagedSet<Object> target = new ManagedSet<>(nl.getLength());
		target.setSource(extractSource(collectionEle));
		target.setElementTypeName(defaultElementType);
		target.setMergeEnabled(parseMergeAttribute(collectionEle));
		parseCollectionElements(nl, target, bd, defaultElementType);
		return target;
	}

	protected void parseCollectionElements(NodeList elementNodes, Collection<Object> target, @Nullable BeanDefinition bd, String defaultElementType) {
		// 4.1 遍历elementNodes
		for (int i = 0; i < elementNodes.getLength(); i++) {
			Node node = elementNodes.item(i);
			// 4.2 跳过description节点
			if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)) {
				// 4.3 调用parsePropertySubElement方法对节点进行解析, 正常list的子节点为<value>节点会直接走到value节点的解析
				// 如果list的子节点还是list，则相当于递归在走到此方法
				target.add(parsePropertySubElement((Element) node, bd, defaultElementType));
			}
		}
	}

	/**
	 * Parse a map element.
	 */
	public Map<Object, Object> parseMapElement(Element mapEle, @Nullable BeanDefinition bd) {
		String defaultKeyType = mapEle.getAttribute(KEY_TYPE_ATTRIBUTE);
		String defaultValueType = mapEle.getAttribute(VALUE_TYPE_ATTRIBUTE);

		List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
		ManagedMap<Object, Object> map = new ManagedMap<>(entryEles.size());
		map.setSource(extractSource(mapEle));
		map.setKeyTypeName(defaultKeyType);
		map.setValueTypeName(defaultValueType);
		map.setMergeEnabled(parseMergeAttribute(mapEle));

		for (Element entryEle : entryEles) {
			// Should only have one value child element: ref, value, list, etc.
			// Optionally, there might be a key child element.
			NodeList entrySubNodes = entryEle.getChildNodes();
			Element keyEle = null;
			Element valueEle = null;
			for (int j = 0; j < entrySubNodes.getLength(); j++) {
				Node node = entrySubNodes.item(j);
				if (node instanceof Element) {
					Element candidateEle = (Element) node;
					if (nodeNameEquals(candidateEle, KEY_ELEMENT)) {
						if (keyEle != null) {
							error("<entry> element is only allowed to contain one <key> sub-element", entryEle);
						} else {
							keyEle = candidateEle;
						}
					} else {
						// Child element is what we're looking for.
						if (nodeNameEquals(candidateEle, DESCRIPTION_ELEMENT)) {
							// the element is a <description> -> ignore it
						} else if (valueEle != null) {
							error("<entry> element must not contain more than one value sub-element", entryEle);
						} else {
							valueEle = candidateEle;
						}
					}
				}
			}

			// Extract key from attribute or sub-element.
			Object key = null;
			boolean hasKeyAttribute = entryEle.hasAttribute(KEY_ATTRIBUTE);
			boolean hasKeyRefAttribute = entryEle.hasAttribute(KEY_REF_ATTRIBUTE);
			if ((hasKeyAttribute && hasKeyRefAttribute) ||
					(hasKeyAttribute || hasKeyRefAttribute) && keyEle != null) {
				error("<entry> element is only allowed to contain either " +
						"a 'key' attribute OR a 'key-ref' attribute OR a <key> sub-element", entryEle);
			}
			if (hasKeyAttribute) {
				key = buildTypedStringValueForMap(entryEle.getAttribute(KEY_ATTRIBUTE), defaultKeyType, entryEle);
			} else if (hasKeyRefAttribute) {
				String refName = entryEle.getAttribute(KEY_REF_ATTRIBUTE);
				if (!StringUtils.hasText(refName)) {
					error("<entry> element contains empty 'key-ref' attribute", entryEle);
				}
				RuntimeBeanReference ref = new RuntimeBeanReference(refName);
				ref.setSource(extractSource(entryEle));
				key = ref;
			} else if (keyEle != null) {
				key = parseKeyElement(keyEle, bd, defaultKeyType);
			} else {
				error("<entry> element must specify a key", entryEle);
			}

			// Extract value from attribute or sub-element.
			Object value = null;
			boolean hasValueAttribute = entryEle.hasAttribute(VALUE_ATTRIBUTE);
			boolean hasValueRefAttribute = entryEle.hasAttribute(VALUE_REF_ATTRIBUTE);
			boolean hasValueTypeAttribute = entryEle.hasAttribute(VALUE_TYPE_ATTRIBUTE);
			if ((hasValueAttribute && hasValueRefAttribute) ||
					(hasValueAttribute || hasValueRefAttribute) && valueEle != null) {
				error("<entry> element is only allowed to contain either " +
						"'value' attribute OR 'value-ref' attribute OR <value> sub-element", entryEle);
			}
			if ((hasValueTypeAttribute && hasValueRefAttribute) ||
					(hasValueTypeAttribute && !hasValueAttribute) ||
					(hasValueTypeAttribute && valueEle != null)) {
				error("<entry> element is only allowed to contain a 'value-type' " +
						"attribute when it has a 'value' attribute", entryEle);
			}
			if (hasValueAttribute) {
				String valueType = entryEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
				if (!StringUtils.hasText(valueType)) {
					valueType = defaultValueType;
				}
				value = buildTypedStringValueForMap(entryEle.getAttribute(VALUE_ATTRIBUTE), valueType, entryEle);
			} else if (hasValueRefAttribute) {
				String refName = entryEle.getAttribute(VALUE_REF_ATTRIBUTE);
				if (!StringUtils.hasText(refName)) {
					error("<entry> element contains empty 'value-ref' attribute", entryEle);
				}
				RuntimeBeanReference ref = new RuntimeBeanReference(refName);
				ref.setSource(extractSource(entryEle));
				value = ref;
			} else if (valueEle != null) {
				value = parsePropertySubElement(valueEle, bd, defaultValueType);
			} else {
				error("<entry> element must specify a value", entryEle);
			}

			// Add final key and value to the Map.
			map.put(key, value);
		}

		return map;
	}

	/**
	 * Build a typed String value Object for the given raw value.
	 *
	 * @see org.springframework.beans.factory.config.TypedStringValue
	 */
	protected final Object buildTypedStringValueForMap(String value, String defaultTypeName, Element entryEle) {
		try {
			TypedStringValue typedValue = buildTypedStringValue(value, defaultTypeName);
			typedValue.setSource(extractSource(entryEle));
			return typedValue;
		} catch (ClassNotFoundException ex) {
			error("Type class [" + defaultTypeName + "] not found for Map key/value type", entryEle, ex);
			return value;
		}
	}

	/**
	 * Parse a key sub-element of a map element.
	 */
	@Nullable
	protected Object parseKeyElement(Element keyEle, @Nullable BeanDefinition bd, String defaultKeyTypeName) {
		NodeList nl = keyEle.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				// Child element is what we're looking for.
				if (subElement != null) {
					error("<key> element must not contain more than one value sub-element", keyEle);
				} else {
					subElement = (Element) node;
				}
			}
		}
		if (subElement == null) {
			return null;
		}
		return parsePropertySubElement(subElement, bd, defaultKeyTypeName);
	}

	/**
	 * Parse a props element.
	 */
	public Properties parsePropsElement(Element propsEle) {
		ManagedProperties props = new ManagedProperties();
		props.setSource(extractSource(propsEle));
		props.setMergeEnabled(parseMergeAttribute(propsEle));

		List<Element> propEles = DomUtils.getChildElementsByTagName(propsEle, PROP_ELEMENT);
		for (Element propEle : propEles) {
			String key = propEle.getAttribute(KEY_ATTRIBUTE);
			// Trim the text value to avoid unwanted whitespace
			// caused by typical XML formatting.
			String value = DomUtils.getTextValue(propEle).trim();
			TypedStringValue keyHolder = new TypedStringValue(key);
			keyHolder.setSource(extractSource(propEle));
			TypedStringValue valueHolder = new TypedStringValue(value);
			valueHolder.setSource(extractSource(propEle));
			props.put(keyHolder, valueHolder);
		}

		return props;
	}

	/**
	 * Parse the merge attribute of a collection element, if any.
	 */
	public boolean parseMergeAttribute(Element collectionElement) {
		String value = collectionElement.getAttribute(MERGE_ATTRIBUTE);
		if (isDefaultValue(value)) {
			value = this.defaults.getMerge();
		}
		return TRUE_VALUE.equals(value);
	}

	/**
	 * Parse a custom element (outside of the default namespace).
	 *
	 * @param ele the element to parse
	 * @return the resulting bean definition
	 */
	@Nullable
	public BeanDefinition parseCustomElement(Element ele) {
		return parseCustomElement(ele, null);
	}

	/**
	 * Parse a custom element (outside of the default namespace).
	 *	解析自定义标签的主类
	 * @param ele          the element to parse
	 * @param containingBd the containing bean definition (if any)
	 * @return the resulting bean definition
	 */
	@Nullable
	public BeanDefinition parseCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
		// 1.拿到节点ele的命名空间，例如常见的:
		// <context> 节点对应命名空间: http://www.springframework.org/schema/context
		// <aop> 节点对应命名空间: http://www.springframework.org/schema/aop
		String namespaceUri = getNamespaceURI(ele);
		if (namespaceUri == null) {
			return null;
		}
		// 2.拿到命名空间对应的的handler, 例如：http://www.springframework.org/schema/context 对应 ContextNameSpaceHandler
		// 2.1 getNamespaceHandlerResolver: 拿到namespaceHandlerResolver
		// 2.2 resolve: 使用namespaceHandlerResolver解析namespaceUri, 拿到namespaceUri对应的NamespaceHandler
		NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
		if (handler == null) {
			error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
			return null;
		}
		// 3.使用拿到的handler解析节点（ParserContext用于存放解析需要的一些上下文信息）
		return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
	}

	/**
	 * Decorate the given bean definition through a namespace handler, if applicable.
	 *
	 * @param ele         the current element
	 * @param originalDef the current bean definition
	 * @return the decorated bean definition
	 */
	public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder originalDef) {
		return decorateBeanDefinitionIfRequired(ele, originalDef, null);
	}

	/**
	 * Decorate the given bean definition through a namespace handler, if applicable.
	 *
	 * @param ele          the current element
	 * @param originalDef  the current bean definition
	 * @param containingBd the containing bean definition (if any)
	 * @return the decorated bean definition
	 */
	public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder originalDef, @Nullable BeanDefinition containingBd) {

		BeanDefinitionHolder finalDefinition = originalDef;

		// Decorate based on custom attributes first.
		//根据bean标签属性装饰BeanDefinitionHolder，比如<bean class="xx" p:username="yyy"/>
		NamedNodeMap attributes = ele.getAttributes();
		//循环所以的元素
		for (int i = 0; i < attributes.getLength(); i++) {
			//获取属性信息
			Node node = attributes.item(i);
			//如果需要就装饰
			finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
		}

		// Decorate based on custom nested elements.
		//根据bean标签子元素装饰BeanDefinitionHolder
		NodeList children = ele.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
			}
		}
		return finalDefinition;
	}

	/**
	 * Decorate the given bean definition through a namespace handler,
	 * if applicable.
	 *	装饰者设计模式 实现了命名控制属性注入的
	 * @param node         the current child node
	 * @param originalDef  the current bean definition
	 * @param containingBd the containing bean definition (if any)
	 * @return the decorated bean definition
	 */
	public BeanDefinitionHolder decorateIfRequired(Node node, BeanDefinitionHolder originalDef, @Nullable BeanDefinition containingBd) {
		//根据node获取到node的命名空间，形如：http://www.springframework.org/schema/p  p:username="Jack"
		String namespaceUri = getNamespaceURI(node);
		if (namespaceUri != null && !isDefaultNamespace(namespaceUri)) {
			//这里有SPI服务发现的思想，根据配置文件获取namespaceUri对应的处理类
			NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
			if (handler != null) {
				//调用NamespaceHandler处理类的decorate方法，开始具体装饰过程，并返回装饰完的对象
				//org.springframework.beans.factory.xml.SimplePropertyNamespaceHandler
				BeanDefinitionHolder decorated = handler.decorate(node, originalDef, new ParserContext(this.readerContext, this, containingBd));
				if (decorated != null) {
					return decorated;
				}
			} else if (namespaceUri.startsWith("http://www.springframework.org/")) {
				error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", node);
			} else {
				// A custom namespace, not to be handled by Spring - maybe "xml:...".
				if (logger.isDebugEnabled()) {
					logger.debug("No Spring NamespaceHandler found for XML schema namespace [" + namespaceUri + "]");
				}
			}
		}
		return originalDef;
	}

	@Nullable
	private BeanDefinitionHolder parseNestedCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
		BeanDefinition innerDefinition = parseCustomElement(ele, containingBd);
		if (innerDefinition == null) {
			error("Incorrect usage of element '" + ele.getNodeName() + "' in a nested manner. " +
					"This tag cannot be used nested inside <property>.", ele);
			return null;
		}
		String id = ele.getNodeName() + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR +
				ObjectUtils.getIdentityHexString(innerDefinition);
		if (logger.isTraceEnabled()) {
			logger.trace("Using generated bean name [" + id +
					"] for nested custom element '" + ele.getNodeName() + "'");
		}
		return new BeanDefinitionHolder(innerDefinition, id);
	}


	/**
	 * Get the namespace URI for the supplied node.
	 * <p>The default implementation uses {@link Node#getNamespaceURI}.
	 * Subclasses may override the default implementation to provide a
	 * different namespace identification mechanism.
	 *
	 * @param node the node
	 */
	@Nullable
	public String getNamespaceURI(Node node) {
		return node.getNamespaceURI();
	}

	/**
	 * Get the local name for the supplied {@link Node}.
	 * <p>The default implementation calls {@link Node#getLocalName}.
	 * Subclasses may override the default implementation to provide a
	 * different mechanism for getting the local name.
	 *
	 * @param node the {@code Node}
	 */
	public String getLocalName(Node node) {
		return node.getLocalName();
	}

	/**
	 * Determine whether the name of the supplied node is equal to the supplied name.
	 * <p>The default implementation checks the supplied desired name against both
	 * {@link Node#getNodeName()} and {@link Node#getLocalName()}.
	 * <p>Subclasses may override the default implementation to provide a different
	 * mechanism for comparing node names.
	 *
	 * @param node        the node to compare
	 * @param desiredName the name to check for
	 */
	public boolean nodeNameEquals(Node node, String desiredName) {
		return desiredName.equals(node.getNodeName()) || desiredName.equals(getLocalName(node));
	}

	/**
	 * Determine whether the given URI indicates the default namespace.
	 */
	public boolean isDefaultNamespace(@Nullable String namespaceUri) {
		return (!StringUtils.hasLength(namespaceUri) || BEANS_NAMESPACE_URI.equals(namespaceUri));
	}

	/**
	 * Determine whether the given node indicates the default namespace.
	 */
	public boolean isDefaultNamespace(Node node) {
		return isDefaultNamespace(getNamespaceURI(node));
	}

	private boolean isDefaultValue(String value) {
		return (DEFAULT_VALUE.equals(value) || "".equals(value));
	}

	private boolean isCandidateElement(Node node) {
		return (node instanceof Element && (isDefaultNamespace(node) || !isDefaultNamespace(node.getParentNode())));
	}

}
