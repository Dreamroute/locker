package com.mook.locker.javaassist;

import org.apache.ibatis.javassist.ClassPool;
import org.apache.ibatis.javassist.CtClass;
import org.apache.ibatis.javassist.CtMethod;
import org.junit.Test;

public class AssistBaseTest {

	@Test
	public void _0Test() throws Exception {
		ClassPool pool = ClassPool.getDefault();
		CtClass cc = pool.get(AAAAAA.class.getName());
		String[] paramName = new String[2];
		paramName[0] = String.class.getName();
		paramName[1] = String.class.getName();
		
		CtMethod cm = cc.getDeclaredMethod("drive", pool.get(paramName));
		
		System.err.println(cm);
		
	}
}

class AAAAAA {
	void drive(String name, String password) {};
}

interface BBBBB {
	void drive(String name, String password);
}