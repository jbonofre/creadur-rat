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
package org.apache.rat.anttasks;

import org.apache.rat.license.ILicense;
import org.apache.rat.analysis.IHeaderMatcher;

public class License {

    private ILicense.Builder builder = ILicense.builder();

    public ILicense build() {
        return builder.build();
    }

    public void setNotes(String notes) {
        builder.setNotes(notes);
    }

    public void addNotes(String notes) {
        builder.setNotes(notes);
    }
    

    public void setDerivedFrom(String derivedFrom) {
        builder.setDerivedFrom(derivedFrom);
    }

    public void setId(String licenseFamilyCategory) {
        builder.setLicenseFamilyCategory(licenseFamilyCategory);
    }

    public void setName(String licenseFamilyName) {
        builder.setLicenseFamilyName(licenseFamilyName);
    }

    public void add(IHeaderMatcher.Builder builder) {
        this.builder.setMatcher(builder);
    }
    
    public void add(IHeaderMatcher matcher) {
        this.builder.setMatcher(matcher);
    }
}
