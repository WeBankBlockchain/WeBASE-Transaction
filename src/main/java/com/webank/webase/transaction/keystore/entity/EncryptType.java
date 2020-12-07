/**
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.webank.webase.transaction.keystore.entity;

/**
 * Enumeration of encrypt type.
 */
public enum EncryptType {
    ecdsa(0), guomi(1);

    private int type;

    private EncryptType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public static boolean isInclude(int type) {
        boolean include = false;
        for (EncryptType e : EncryptType.values()) {
            if (e.getType() == type) {
                include = true;
                break;
            }
        }
        return include;
    }
}
