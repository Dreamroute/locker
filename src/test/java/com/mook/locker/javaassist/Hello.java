package com.mook.locker.javaassist;

import java.util.Arrays;

import org.apache.ibatis.javassist.ClassPool;
import org.apache.ibatis.javassist.CtClass;
import org.apache.ibatis.javassist.CtMethod;
import org.apache.ibatis.javassist.Modifier;
import org.apache.ibatis.javassist.NotFoundException;
import org.apache.ibatis.javassist.bytecode.CodeAttribute;
import org.apache.ibatis.javassist.bytecode.LocalVariableAttribute;
import org.apache.ibatis.javassist.bytecode.MethodInfo;

public class Hello {

	public static void main(String[] args) throws Exception {
		// 匹配静态方法
		String[] paramNames = getMethodParamNames(Hello.class, "main", String[].class);
		System.out.println(Arrays.toString(paramNames));
		// 匹配实例方法
		paramNames = getMethodParamNames(Hello.class, "foo", String.class);
		System.out.println(Arrays.toString(paramNames));
		// 自由匹配任一个重名方法
		paramNames = getMethodParamNames(Hello.class, "getMethodParamNames");
		System.out.println(Arrays.toString(paramNames));
		// 匹配特定签名的方法
		paramNames = getMethodParamNames(Hello.class, "getMethodParamNames", Class.class, String.class);
		System.out.println(Arrays.toString(paramNames));
	}

	/**
	 * 获取方法参数名称，按给定的参数类型匹配方法
	 * 
	 * @param clazz
	 * @param method
	 * @param paramTypes
	 * @return
	 * @throws NotFoundException
	 *             如果类或者方法不存在
	 * @throws MissingLVException
	 *             如果最终编译的class文件不包含局部变量表信息
	 */
	public static String[] getMethodParamNames(Class clazz, String method, Class... paramTypes)
			throws NotFoundException, MissingLVException {

		ClassPool pool = ClassPool.getDefault();
		CtClass cc = pool.get(clazz.getName());
		
		String[] paramTypeNames = new String[paramTypes.length];
		
		for (int i = 0; i < paramTypes.length; i++)
			paramTypeNames[i] = paramTypes[i].getName();
		
		CtMethod cm = cc.getDeclaredMethod(method, pool.get(paramTypeNames));
		return getMethodParamNames(cm);
	}

	/**
	 * 获取方法参数名称，匹配同名的某一个方法
	 * 
	 * @param clazz
	 * @param method
	 * @return
	 * @throws NotFoundException
	 *             如果类或者方法不存在
	 * @throws MissingLVException
	 *             如果最终编译的class文件不包含局部变量表信息
	 */
	public static String[] getMethodParamNames(Class clazz, String method) throws NotFoundException, MissingLVException {

		ClassPool pool = ClassPool.getDefault();
		CtClass cc = pool.get(clazz.getName());
		CtMethod cm = cc.getDeclaredMethod(method);
		return getMethodParamNames(cm);
	}

	/**
	 * 获取方法参数名称
	 * 
	 * @param cm
	 * @return
	 * @throws NotFoundException
	 * @throws MissingLVException
	 *             如果最终编译的class文件不包含局部变量表信息
	 */
	protected static String[] getMethodParamNames(CtMethod cm) throws NotFoundException, MissingLVException {
		CtClass cc = cm.getDeclaringClass();
		MethodInfo methodInfo = cm.getMethodInfo();
		CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
		if (attr == null)
			throw new MissingLVException(cc.getName());

		String[] paramNames = new String[cm.getParameterTypes().length];
		int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
		for (int i = 0; i < paramNames.length; i++)
			paramNames[i] = attr.variableName(i + pos);
		return paramNames;
	}

	/**
	 * 在class中未找到局部变量表信息<br>
	 * 使用编译器选项 javac -g:{vars}来编译源文件
	 * 
	 * @author Administrator
	 * 
	 */
	public static class MissingLVException extends Exception {
		static String msg = "class:%s 不包含局部变量表信息，请使用编译器选项 javac -g:{vars}来编译源文件。";

		public MissingLVException(String clazzName) {
			super(String.format(msg, clazzName));
		}
	}

	static void foo() {
	}

	void foo(String bar) {
	}
}

