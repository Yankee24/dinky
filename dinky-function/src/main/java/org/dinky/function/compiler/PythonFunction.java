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

package org.dinky.function.compiler;

import org.dinky.assertion.Asserts;
import org.dinky.data.model.SystemConfiguration;
import org.dinky.function.constant.PathConstant;
import org.dinky.function.data.model.UDF;
import org.dinky.function.util.UDFUtil;
import org.dinky.function.util.ZipWriter;

import org.apache.flink.client.python.PythonFunctionFactory;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.python.PythonOptions;
import org.apache.flink.table.catalog.FunctionLanguage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * python 编译
 *
 * @since 0.6.8
 */
@Slf4j
public class PythonFunction implements FunctionCompiler, FunctionPackage {

    /**
     * 函数代码在线动态编译
     *
     * @param udf udf
     * @param conf flink-conf
     * @param taskId 任务id
     * @return 是否成功
     */
    @Override
    public boolean compiler(UDF udf, ReadableConfig conf, Integer taskId) {
        Asserts.checkNull(udf, "flink-config 不能为空");
        // TODO 改为ProcessStep注释

        log.info("正在编译 python 代码 , class: {}", udf.getClassName());
        File pyFile = FileUtil.writeUtf8String(
                udf.getCode(),
                PathConstant.getUdfCompilerPath(
                        FunctionLanguage.PYTHON, UDFUtil.getPyFileName(udf.getClassName()) + ".py"));
        File zipFile = ZipUtil.zip(pyFile);
        FileUtil.del(pyFile);
        try {
            Configuration configuration = new Configuration((Configuration) conf);
            configuration.set(PythonOptions.PYTHON_FILES, zipFile.getAbsolutePath());
            configuration.set(
                    PythonOptions.PYTHON_CLIENT_EXECUTABLE,
                    SystemConfiguration.getInstances().getPythonHome());
            configuration.set(
                    PythonOptions.PYTHON_EXECUTABLE,
                    SystemConfiguration.getInstances().getPythonHome());

            PythonFunctionFactory.getPythonFunction(udf.getClassName(), configuration, null);
            log.info("Python udf compiled successfully; className:{}", udf.getClassName());
        } catch (Exception e) {
            log.error(
                    "Python udf compilation failed; className:{}\n.reason: {}",
                    udf.getClassName(),
                    ExceptionUtil.getRootCauseMessage(e));
            return false;
        }
        FileUtil.del(zipFile);
        return true;
    }

    @Override
    public String[] pack(List<UDF> udfList, Integer taskId) {
        if (CollUtil.isEmpty(udfList)) {
            return new String[0];
        }
        udfList = udfList.stream()
                .filter(udf -> udf.getFunctionLanguage() == FunctionLanguage.PYTHON)
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(udfList)) {
            return new String[0];
        }

        InputStream[] inputStreams = udfList.stream()
                .map(udf -> {
                    File file = FileUtil.writeUtf8String(
                            udf.getCode(),
                            PathConstant.getUdfCompilerPath(
                                    FunctionLanguage.PYTHON, UDFUtil.getPyFileName(udf.getClassName()) + ".py"));
                    return FileUtil.getInputStream(file);
                })
                .toArray(InputStream[]::new);

        String[] paths = udfList.stream()
                .map(x -> StrUtil.split(x.getClassName(), ".").get(0) + ".py")
                .toArray(String[]::new);
        String path = PathConstant.getUdfPackagePath(taskId, PathConstant.UDF_PYTHON_NAME);
        File file = FileUtil.file(path);
        FileUtil.del(file);
        try (ZipWriter zipWriter = new ZipWriter(file, Charset.defaultCharset())) {
            zipWriter.add(paths, inputStreams);
        }
        return new String[] {path};
    }

    @Override
    public String pack(UDF udf, Integer taskId) {
        File udfFile = FileUtil.writeUtf8String(
                udf.getCode(),
                PathConstant.getUdfCompilerPath(
                        FunctionLanguage.PYTHON, UDFUtil.getPyFileName(udf.getClassName()) + ".py"));
        BufferedInputStream inputStream = FileUtil.getInputStream(udfFile);

        String fileName = StrUtil.split(udf.getClassName(), ".").get(0) + ".py";
        String path = PathConstant.getUdfPackagePath(taskId, PathConstant.UDF_PYTHON_NAME);
        File file = FileUtil.file(path);
        FileUtil.del(file);
        try (ZipWriter zipWriter = new ZipWriter(file, Charset.defaultCharset())) {
            zipWriter.add(fileName, inputStream);
        }
        udf.setCompilePackagePath(file.getAbsolutePath());
        return file.getAbsolutePath();
    }
}
