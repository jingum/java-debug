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

package com.microsoft.java.debug.core.adapter.handler;

import java.util.Arrays;
import java.util.List;

import com.microsoft.java.debug.core.adapter.AdapterUtils;
import com.microsoft.java.debug.core.adapter.ErrorCode;
import com.microsoft.java.debug.core.adapter.IDebugAdapterContext;
import com.microsoft.java.debug.core.adapter.IDebugRequestHandler;
import com.microsoft.java.debug.core.adapter.IHotCodeReplaceProvider;
import com.microsoft.java.debug.core.protocol.Messages.Response;
import com.microsoft.java.debug.core.protocol.Requests.Arguments;
import com.microsoft.java.debug.core.protocol.Requests.Command;
import com.microsoft.java.debug.core.protocol.Requests.SaveDocumentArguments;
import com.microsoft.java.debug.core.protocol.Responses;

public class SaveDocumentRequestHandler implements IDebugRequestHandler {

    @Override
    public List<Command> getTargetCommands() {
        return Arrays.asList(Command.SAVEDOCUMENT);
    }

    @Override
    public void handle(Command command, Arguments arguments, Response response, IDebugAdapterContext context) {
        String documentUri = ((SaveDocumentArguments) arguments).documentUri;
        if (documentUri == null) {
            AdapterUtils.setErrorResponse(response, ErrorCode.ARGUMENT_MISSING, "SaveDocumentRequest: property 'doucmentUri' is missing, null, or empty");
        } else {
            IHotCodeReplaceProvider hcrProdiver = context.getProvider(IHotCodeReplaceProvider.class);
            hcrProdiver.saveDocument(documentUri);
            response.body = new Responses.ResponseBody();
        }
    }
}
