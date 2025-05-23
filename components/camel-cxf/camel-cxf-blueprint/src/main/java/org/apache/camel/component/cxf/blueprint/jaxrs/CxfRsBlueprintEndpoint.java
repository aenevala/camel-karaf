/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.cxf.blueprint.jaxrs;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.camel.Component;
import org.apache.camel.blueprint.BlueprintCamelContext;
import org.apache.camel.component.cxf.blueprint.helpers.BlueprintSupport;
import org.apache.camel.component.cxf.blueprint.helpers.RsClientBlueprintBean;
import org.apache.camel.component.cxf.blueprint.helpers.RsServerBlueprintBean;
import org.apache.camel.component.cxf.jaxrs.CxfRsEndpoint;
import org.apache.camel.util.ReflectionHelper;
import org.apache.cxf.jaxrs.AbstractJAXRSFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class CxfRsBlueprintEndpoint extends CxfRsEndpoint {
    private final AbstractJAXRSFactoryBean bean;
    private BlueprintContainer blueprintContainer;
    private BundleContext bundleContext;
    private BlueprintCamelContext blueprintCamelContext;

    public CxfRsBlueprintEndpoint(Component comp, String uri, AbstractJAXRSFactoryBean bean) {
        super(uri, comp);
        this.bean = bean;
        setAddress(bean.getAddress());
        // update the sfb address by resolving the properties
        bean.setAddress(getAddress());
        BlueprintSupport support = (BlueprintSupport)bean;
        setBlueprintContainer(support.getBlueprintContainer());
        setBundleContext(support.getBundleContext());
    }

    @Override
    protected void doInit() throws Exception {
        if (getCamelContext() == null) {
            setCamelContext(blueprintCamelContext);
        }
        super.doInit();
    }

    public BlueprintContainer getBlueprintContainer() {
        return blueprintContainer;
    }

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BlueprintCamelContext getBlueprintCamelContext() {
        return blueprintCamelContext;
    }

    public void setBlueprintCamelContext(BlueprintCamelContext blueprintCamelContext) {
        this.blueprintCamelContext = blueprintCamelContext;
    }
    
    @Override
    protected JAXRSServerFactoryBean newJAXRSServerFactoryBean() {
        checkBeanType(bean, JAXRSServerFactoryBean.class);
        return (RsServerBlueprintBean)bean;
    }
    
    @Override
    protected JAXRSClientFactoryBean newJAXRSClientFactoryBean() {
        checkBeanType(bean, JAXRSClientFactoryBean.class);
        return newInstanceWithCommonProperties();
    }

    private RsClientBlueprintBean newInstanceWithCommonProperties() {
        RsClientBlueprintBean cfb = new RsClientBlueprintBean();

        if (bean instanceof RsClientBlueprintBean) {
            shallowCopyFieldState(bean, cfb);
        }

        return cfb;
    }

    private static void shallowCopyFieldState(final Object src, final Object dest) {
        if (!src.getClass().isAssignableFrom(dest.getClass())) {
            throw new IllegalArgumentException("Destination class [" + dest.getClass().getName() + "] must be same or subclass as source class [" + src.getClass().getName() + "]");
        } else {
            ReflectionHelper.doWithFields(src.getClass(), (field) -> {
                if (isCopyableField(field)) {
                    makeAccessible(field);
                    Object srcValue = field.get(src);
                    field.set(dest, srcValue);
                }
            });
        }
    }

    private static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers()) || Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    private static boolean isCopyableField(Field field) {
        return !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers());
    }

}
