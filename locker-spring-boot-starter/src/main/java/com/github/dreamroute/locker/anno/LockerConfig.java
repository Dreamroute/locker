package com.github.dreamroute.locker.anno;

import com.github.dreamroute.locker.interceptor.LockerInterceptor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

/**
 * 描述：乐观锁配置类
 *
 * @author w.dehi.2022-02-17
 */
public class LockerConfig implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        MergedAnnotation<EnableLocker> anno = importingClassMetadata.getAnnotations().get(EnableLocker.class);
        String versionColumn = anno.getString("versionColumn");
        boolean failThrowException = anno.getBoolean("failThrowException");
        LockerInterceptor lockerInterceptor = new LockerInterceptor(versionColumn, failThrowException);

        // 注册locker
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) registry;
        beanFactory.registerSingleton("lockerInterceptor", lockerInterceptor);
    }

}
