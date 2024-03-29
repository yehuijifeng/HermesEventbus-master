/**
 * Copyright 2016 Xiaofei
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.library.hermes.sender;

import com.library.hermes.HermesService;
import com.library.hermes.wrapper.MethodWrapper;
import com.library.hermes.wrapper.ObjectWrapper;
import com.library.hermes.wrapper.ParameterWrapper;

import java.lang.reflect.Method;

/**
 * new一个对象的发送方，这里是模拟一个动态代理
 */
public class InstanceCreatingSender extends Sender {

    private Class<?>[] mConstructorParameterTypes;

    public InstanceCreatingSender(Class<? extends HermesService> service, ObjectWrapper object) {
        super(service, object);
    }

    @Override
    protected MethodWrapper getMethodWrapper(Method method, ParameterWrapper[] parameterWrappers) {
        int length = parameterWrappers == null ? 0 : parameterWrappers.length;
        mConstructorParameterTypes = new Class<?>[length];
        for (int i = 0; i < length; ++i) {
            try {
                ParameterWrapper parameterWrapper = parameterWrappers[i];
                mConstructorParameterTypes[i] = parameterWrapper == null ? null : parameterWrapper.getClassType();
            } catch (Exception e) {

            }
        }
        return new MethodWrapper(mConstructorParameterTypes);
    }


}
