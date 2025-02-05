/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dinky.metadata.config;

import org.dinky.utils.TextUtil;

import lombok.Getter;

@Getter
public enum FileSystemType {
    LOCAL("local"),
    HDFS("hdfs"),
    S3("s3"),
    NONE("none"),
    ;

    private final String type;

    FileSystemType(String type) {
        this.type = type;
    }

    public static FileSystemType fromType(String type) {
        if (TextUtil.isEmpty(type)) {
            return NONE;
        }
        for (FileSystemType value : FileSystemType.values()) {
            if (value.getType().equalsIgnoreCase(type)) {
                return value;
            }
        }
        return null;
    }
}
