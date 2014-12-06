/*
 *
 *  * Copyright (c) 2005 - 2014, WSO2 Inc. (http://www.wso2.org)
 *  * All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.core.util.parser.helper;

import org.wso2.siddhi.core.event.state.MetaStateEvent;
import org.wso2.siddhi.core.event.stream.MetaStreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventPool;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.input.QueryStreamReceiver;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.window.WindowProcessor;
import org.wso2.siddhi.core.query.input.stream.SingleStreamRuntime;
import org.wso2.siddhi.core.query.input.stream.StreamRuntime;
import org.wso2.siddhi.core.util.SiddhiConstants;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.util.List;

/**
 * Utility class for queryParser to help with QueryRuntime
 * generation.
 */
public class QueryParserHelper {

    /**
     * Method to clean/refactor MetaStreamEvent and update
     * VariableExpressionExecutors accordingly.
     *
     * @param metaStreamEvent
     * @param variableExpressionExecutorList
     * @param position
     */
    private static void updateVariablePosition(MetaStreamEvent metaStreamEvent, List<VariableExpressionExecutor> variableExpressionExecutorList, int[] position) {
        //position[state event index, event chain index]
        refactorMetaStreamEvent(metaStreamEvent);       //can remove this and call separately so that we can stop calling repeatedly in partition creation. But then we can't enforce refactoring.
        for (VariableExpressionExecutor variableExpressionExecutor : variableExpressionExecutorList) {
            if (metaStreamEvent.getOutputData().contains(variableExpressionExecutor.getAttribute())) {
                if (position[0] != -1 || position[1] != -1) {
                    variableExpressionExecutor.setPosition(new int[]{position[0], position[1], SiddhiConstants.OUTPUT_DATA_INDEX, metaStreamEvent.getOutputData()
                            .indexOf(variableExpressionExecutor.getAttribute())});
                } else {
                    variableExpressionExecutor.setPosition(new int[]{SiddhiConstants.OUTPUT_DATA_INDEX, metaStreamEvent.getOutputData()
                            .indexOf(variableExpressionExecutor.getAttribute())});
                }
            } else if (metaStreamEvent.getOnAfterWindowData().contains(variableExpressionExecutor.getAttribute())) {
                if (position[0] != -1 || position[1] != -1) {
                    variableExpressionExecutor.setPosition(new int[]{position[0], position[1], SiddhiConstants.AFTER_WINDOW_DATA_INDEX, metaStreamEvent
                            .getOnAfterWindowData().indexOf(variableExpressionExecutor.getAttribute())});
                } else {
                    variableExpressionExecutor.setPosition(new int[]{SiddhiConstants.AFTER_WINDOW_DATA_INDEX, metaStreamEvent
                            .getOnAfterWindowData().indexOf(variableExpressionExecutor.getAttribute())});
                }
            } else if (metaStreamEvent.getBeforeWindowData().contains(variableExpressionExecutor.getAttribute())) {
                if (position[0] != -1 || position[1] != -1) {
                    variableExpressionExecutor.setPosition(new int[]{position[0], position[1], SiddhiConstants.BEFORE_WINDOW_DATA_INDEX, metaStreamEvent
                            .getBeforeWindowData().indexOf(variableExpressionExecutor.getAttribute())});
                } else {
                    variableExpressionExecutor.setPosition(new int[]{SiddhiConstants.BEFORE_WINDOW_DATA_INDEX, metaStreamEvent
                            .getBeforeWindowData().indexOf(variableExpressionExecutor.getAttribute())});
                }
            }
        }

    }

    /**
     * Helper method to clean/refactor MetaStreamEvent
     *
     * @param metaStreamEvent
     */
    private static synchronized void refactorMetaStreamEvent(MetaStreamEvent metaStreamEvent) {
        for (Attribute attribute : metaStreamEvent.getOutputData()) {
            if (metaStreamEvent.getBeforeWindowData().contains(attribute)) {
                metaStreamEvent.getBeforeWindowData().remove(attribute);
            }
            if (metaStreamEvent.getOnAfterWindowData().contains(attribute)) {
                metaStreamEvent.getOnAfterWindowData().remove(attribute);
            }
        }
        for (Attribute attribute : metaStreamEvent.getOnAfterWindowData()) {
            if (metaStreamEvent.getBeforeWindowData().contains(attribute)) {
                metaStreamEvent.getBeforeWindowData().remove(attribute);
            }
        }
    }

    /**
     * Method to clean/refactor MetaStateEvent and update
     * VariableExpressionExecutors accordingly.
     *
     * @param metaStateEvent
     * @param executors
     */
    public static void updateVariablePosition(MetaStateEvent metaStateEvent, List<VariableExpressionExecutor> executors) {
        if (metaStateEvent.getEventCount() == 1) {
            updateVariablePosition(metaStateEvent.getMetaEvent(0), executors, new int[]{-1, -1});  //int[] is used to deliver 0,1 indexes of the position array
        } else {

            //TODO handle stateEvent
        }
    }

    public static void initStreamRuntime(StreamRuntime runtime, MetaStateEvent metaStateEvent) {
        int index = 0;
        if (runtime instanceof SingleStreamRuntime) {
            MetaStreamEvent metaStreamEvent = metaStateEvent.getMetaEvent(index);
            StreamEventPool streamEventPool = new StreamEventPool(metaStreamEvent, 5);
            QueryStreamReceiver queryStreamReceiver = ((SingleStreamRuntime) runtime).getQueryStreamReceiver();
            queryStreamReceiver.setMetaStreamEvent(metaStreamEvent);
            queryStreamReceiver.setStreamEventPool(streamEventPool);
            queryStreamReceiver.init();
            Processor processor = ((SingleStreamRuntime) runtime).getProcessorChain();
            while (processor != null) {
                if (processor instanceof WindowProcessor) {
                    ((WindowProcessor) processor).initProcessor(metaStreamEvent, streamEventPool);
                }
                processor = processor.getNextProcessor();
            }
        } else {
            //TODO JoinStreamRuntime/PatternStreamRuntime
        }
    }
}
