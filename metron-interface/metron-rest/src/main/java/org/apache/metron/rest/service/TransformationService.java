/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.rest.service;

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.StellarFunctionInfo;
import org.apache.metron.common.dsl.functions.resolver.SingletonFunctionResolver;
import org.apache.metron.common.field.transformation.FieldTransformations;
import org.apache.metron.common.stellar.StellarProcessor;
import org.apache.metron.rest.model.StellarFunctionDescription;
import org.apache.metron.rest.model.TransformationValidation;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransformationService {

    public Map<String, Boolean> validateRules(List<String> rules) {
        Map<String, Boolean> results = new HashMap<>();
        StellarProcessor stellarProcessor = new StellarProcessor();
        for(String rule: rules) {
            try {
                boolean result = stellarProcessor.validate(rule, Context.EMPTY_CONTEXT());
                results.put(rule, result);
            } catch (ParseException e) {
                results.put(rule, false);
            }
        }
        return results;
    }

    public Map<String, Object> validateTransformation(TransformationValidation transformationValidation) {
        JSONObject sampleJson = new JSONObject(transformationValidation.getSampleData());
        transformationValidation.getSensorParserConfig().getFieldTransformations().forEach(fieldTransformer -> {
                    fieldTransformer.transformAndUpdate(sampleJson, transformationValidation.getSensorParserConfig().getParserConfig(), Context.EMPTY_CONTEXT());
                }
        );
        return sampleJson;
    }

    public FieldTransformations[] getTransformations() {
        return FieldTransformations.values();
    }

    public List<StellarFunctionDescription> getStellarFunctions() {
        List<StellarFunctionDescription> stellarFunctionDescriptions = new ArrayList<>();
        Iterable<StellarFunctionInfo> stellarFunctionsInfo = SingletonFunctionResolver.getInstance().getFunctionInfo();
        stellarFunctionsInfo.forEach(stellarFunctionInfo -> {
            stellarFunctionDescriptions.add(new StellarFunctionDescription(
                    stellarFunctionInfo.getName(),
                    stellarFunctionInfo.getDescription(),
                    stellarFunctionInfo.getParams(),
                    stellarFunctionInfo.getReturns()));
        });
        return stellarFunctionDescriptions;
    }

    public List<StellarFunctionDescription> getSimpleStellarFunctions() {
      List<StellarFunctionDescription> stellarFunctionDescriptions = getStellarFunctions();
      return stellarFunctionDescriptions.stream().filter(stellarFunctionDescription ->
              stellarFunctionDescription.getParams().length == 1).sorted((o1, o2) -> o1.getName().compareTo(o2.getName())).collect(Collectors.toList());
    }

}