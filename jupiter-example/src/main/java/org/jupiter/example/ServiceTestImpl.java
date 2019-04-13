/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jupiter.example;

import java.util.Collections;

import org.jupiter.common.util.Lists;
import org.jupiter.rpc.ServiceProviderImpl;

/**
 * jupiter
 * org.jupiter.example
 *
 * @author jiachun.fjc
 */
@ServiceProviderImpl(version = "1.0.0.daily")
public class ServiceTestImpl extends BaseService implements ServiceTest {

    private String strValue;

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    @SuppressWarnings("NumericOverflow")
    @Override
    public ResultClass sayHello(String... s) {
        ResultClass result = new ResultClass();
        result.lon = Long.MIN_VALUE;
        Integer i = getIntValue();
        result.num = (i == null ? 0 : i);
        result.list = Lists.newArrayList("H", "e", "l", "l", "o");
        for (int j = 0; j < 5000; j++) {
            result.list.add(String.valueOf(Integer.MAX_VALUE - j));
        }
        Collections.addAll(result.list, s);
        return result;
    }
}
