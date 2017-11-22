/*******************************************************************************
* Copyright (c) 2017 Microsoft Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Microsoft Corporation - initial API and implementation
*******************************************************************************/

package com.microsoft.java.debug.core.adapter;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.microsoft.java.debug.core.IDebugSession;
import com.microsoft.java.debug.core.adapter.variables.IVariableFormatter;
import com.microsoft.java.debug.core.adapter.variables.VariableFormatterFactory;
import com.microsoft.java.debug.core.protocol.Events.DebugEvent;
import com.microsoft.java.debug.core.protocol.Messages;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;

public class DebugAdapterContext implements IDebugAdapterContext {
    private static final int MAX_SOURCE_MAPPING_CACHE_ITEMS = 10000;
    private static final int MAX_STACKFRAME_CACHE_ITEMS = 1000;
    private Map<String, String> sourceMappingCache = Collections.synchronizedMap(new LRUCache<>(MAX_SOURCE_MAPPING_CACHE_ITEMS));
    private Map<Object, StackFrame[]> stackFrameCache = Collections.synchronizedMap(new LRUCache<>(MAX_STACKFRAME_CACHE_ITEMS));
    private IProviderContext providerContext;
    private Consumer<Messages.ProtocolMessage> messageConsumer;

    private IDebugSession debugSession;
    private boolean debuggerLinesStartAt1 = true;
    private boolean debuggerPathsAreUri = true;
    private boolean clientLinesStartAt1 = true;
    private boolean clientPathsAreUri = false;
    private boolean isAttached = false;
    private String[] sourcePaths;
    private Charset debuggeeEncoding;
    private transient boolean vmTerminated;
    private boolean isVmStopOnEntry = false;
    private String mainClass;
    private String projectName;
    private Map<Long, Object> stoppedStates = new HashMap<>();

    private IdCollection<String> sourceReferences = new IdCollection<>();
    private RecyclableObjectPool<Long, Object> recyclableIdPool = new RecyclableObjectPool<>();
    private IVariableFormatter variableFormatter = VariableFormatterFactory.createVariableFormatter();

    public DebugAdapterContext(Consumer<Messages.ProtocolMessage> messageConsumer, IProviderContext providerContext) {
        this.providerContext = providerContext;
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void sendEvent(DebugEvent event) {
        messageConsumer.accept(new Messages.Event(event.type, event));
    }

    @Override
    public <T extends IProvider> T getProvider(Class<T> clazz) {
        return providerContext.getProvider(clazz);
    }

    @Override
    public void setDebugSession(IDebugSession session) {
        debugSession = session;
    }

    @Override
    public IDebugSession getDebugSession() {
        return debugSession;
    }

    @Override
    public boolean isDebuggerLinesStartAt1() {
        return debuggerLinesStartAt1;
    }

    @Override
    public void setDebuggerLinesStartAt1(boolean debuggerLinesStartAt1) {
        this.debuggerLinesStartAt1 = debuggerLinesStartAt1;
    }

    @Override
    public boolean isDebuggerPathsAreUri() {
        return debuggerPathsAreUri;
    }

    @Override
    public void setDebuggerPathsAreUri(boolean debuggerPathsAreUri) {
        this.debuggerPathsAreUri = debuggerPathsAreUri;
    }

    @Override
    public boolean isClientLinesStartAt1() {
        return clientLinesStartAt1;
    }

    @Override
    public void setClientLinesStartAt1(boolean clientLinesStartAt1) {
        this.clientLinesStartAt1 = clientLinesStartAt1;
    }

    @Override
    public boolean isClientPathsAreUri() {
        return clientPathsAreUri;
    }

    @Override
    public void setClientPathsAreUri(boolean clientPathsAreUri) {
        this.clientPathsAreUri = clientPathsAreUri;
    }

    @Override
    public boolean isAttached() {
        return isAttached;
    }

    @Override
    public void setAttached(boolean attached) {
        isAttached = attached;
    }

    @Override
    public String[] getSourcePaths() {
        return sourcePaths;
    }

    @Override
    public void setSourcePaths(String[] sourcePaths) {
        this.sourcePaths = sourcePaths;
    }

    @Override
    public String getSourceUri(int sourceReference) {
        return sourceReferences.get(sourceReference);
    }

    @Override
    public int createSourceReference(String uri) {
        return sourceReferences.create(uri);
    }

    @Override
    public RecyclableObjectPool<Long, Object> getRecyclableIdPool() {
        return recyclableIdPool;
    }

    @Override
    public void setRecyclableIdPool(RecyclableObjectPool<Long, Object> idPool) {
        recyclableIdPool = idPool;
    }

    @Override
    public IVariableFormatter getVariableFormatter() {
        return variableFormatter;
    }

    @Override
    public void setVariableFormatter(IVariableFormatter variableFormatter) {
        this.variableFormatter = variableFormatter;
    }

    @Override
    public Map<String, String> getSourceLookupCache() {
        return sourceMappingCache;
    }

    @Override
    public Map<Object, StackFrame[]> getStackFrameCache() {
        return stackFrameCache;
    }

    @Override
    public void setDebuggeeEncoding(Charset encoding) {
        debuggeeEncoding = encoding;
    }

    @Override
    public Charset getDebuggeeEncoding() {
        return debuggeeEncoding;
    }

    @Override
    public void setVmTerminated() {
        vmTerminated = true;
    }

    @Override
    public boolean isVmTerminated() {
        return vmTerminated;
    }

    @Override
    public void setVmStopOnEntry(boolean stopOnEntry) {
        isVmStopOnEntry = stopOnEntry;
    }

    @Override
    public boolean isVmStopOnEntry() {
        return isVmStopOnEntry;
    }

    @Override
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public String getMainClass() {
        return mainClass;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }


    @Override
    public void saveThreadTimestamp(ThreadReference thread) {
        synchronized (stoppedStates) {
            stoppedStates.compute(thread.uniqueID(), (k, v) -> new Object());
        }
    }

    @Override
    public Object getThreadTimestamp(ThreadReference thread) {
        synchronized (stoppedStates) {
            return stoppedStates.get(thread.uniqueID());
        }
    }

    @Override
    public boolean isStaledThreadTimestamp(ThreadReference thread, Object ctx) {
        synchronized (stoppedStates) {
            return ctx != stoppedStates.get(thread.uniqueID());
        }
    }

    @Override
    public void clearThreadTimestamp(ThreadReference thread) {
        synchronized (stoppedStates) {
            stoppedStates.remove(thread.uniqueID());
        }
    }
}
