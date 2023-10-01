/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.rat.configuration;

import java.util.Arrays;

public enum Format {
    XML( "xml"),
    TXT ( "txt","text");
    
    
    String[] suffix;
    
    Format(String... suffix) {
        this.suffix = suffix;
    }
    
    public static Format fromName(String name) {
        String[] parts = name.split("\\.");
        String suffix = parts[parts.length-1];
        for (Format f: Format.values()) {
            if (Arrays.stream(f.suffix).anyMatch( suffix::equals )) {
                return f;
            }
        }
        throw new IllegalArgumentException(String.format("No such suffix: %s", suffix));
    }
}
