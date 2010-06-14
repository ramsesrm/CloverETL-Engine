/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.ctl;

import org.apache.commons.logging.Log;
import org.jetel.component.Transform;
import org.jetel.ctl.ASTnode.CLVFFunctionDeclaration;
import org.jetel.data.DataRecord;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.graph.Node;
import org.jetel.graph.TransformationGraph;

/**
 * Base class of all CTL transform classes.
 *
 * @author Martin Janik, Javlin a.s. &lt;martin.janik@javlin.eu&gt;
 *
 * @version 11th June 2010
 * @created 5th May 2010
 */
public class CTLAbstractTransformAdapter implements Transform {

	/** The name of the CTL init() function. */
	private static final String FUNCTION_INIT_NAME = "init";
	/** The name of the CTL init() function. */
	private static final String FUNCTION_PRE_EXECUTE_NAME = "preExecute";
	/** The name of the CTL init() function. */
	private static final String FUNCTION_POST_EXECUTE_NAME = "postExecute";
	/** The name of the CTL init() function. */
	private static final String FUNCTION_GET_MESSAGE_NAME = "getMessage";

	/** An empty array of arguments used for calls to functions without any arguments. */
    protected static final Object[] NO_ARGUMENTS = new Object[0];
	/** An empty array of data records used for calls to functions that do not access to any data records. */
    protected static final DataRecord[] NO_DATA_RECORDS = new DataRecord[0];

    /** The CTL executor to be used for this transform adapter. */
    protected final TransformLangExecutor executor;
	/** The logger used by this class. */
	protected final Log logger;

    /** The CTL declaration of the optional preExecute() function */
    private CLVFFunctionDeclaration functionPreExecute;
    /** The CTL declaration of the optional postExecute() function */
    private CLVFFunctionDeclaration functionPostExecute;
    /** The CTL declaration of the optional getMessage() function */
    private CLVFFunctionDeclaration functionGetMessage;

    /**
     * Constructs a <code>CTLAbstractTransformAdapter</code> for a given CTL executor and logger.
     *
     * @param executor the CTL executor to be used by this transform adapter, may not be <code>null</code>
     * @param executor the logger to be used by this transform adapter, may not be <code>null</code>
     *
     * @throws NullPointerException if either the executor or the logger is <code>null</code>
     */
    public CTLAbstractTransformAdapter(TransformLangExecutor executor, Log logger) {
		if (executor == null) {
			throw new NullPointerException("executor");
		}

		if (logger == null) {
			throw new NullPointerException("logger");
		}

		this.executor = executor;
		this.logger = logger;
	}

	/**
	 * Calls to this method are ignored, associating a graph is meaningless in case of interpreted CTL.
	 */
    @Override
    public void setNode(Node node) {
		// do nothing
    }

	/**
	 * @return always <code>null</code>, no graph node can be associated with this transform adapter
	 */
    @Override
	public Node getNode() {
		return null;
	}

    /**
     * @return always <code>null</code>, no graph can be associated with this transform adapter
     */
    @Override
    public TransformationGraph getGraph() {
    	return null;
    }

    protected void init() throws ComponentNotReadyException {
		// we will be calling one function at a time so we need global scope active
		executor.keepGlobalScope();
		executor.init();

		try {
			executor.execute();
		} catch (TransformLangExecutorRuntimeException exception) {
			throw new ComponentNotReadyException("Failed to initialize global scope!", exception);
		}

		CLVFFunctionDeclaration functionInit = executor.getFunction(FUNCTION_INIT_NAME);

		if (functionInit != null) {
			try {
				executor.executeFunction(functionInit, NO_ARGUMENTS);
			} catch (TransformLangExecutorRuntimeException exception) {
				throw new ComponentNotReadyException("Execution of " + functionInit.getName()
						+ "() function failed!", exception);
			}
		}

		// initialize optional CTL functions
		functionPreExecute = executor.getFunction(FUNCTION_PRE_EXECUTE_NAME);
		functionPostExecute = executor.getFunction(FUNCTION_POST_EXECUTE_NAME);
		functionGetMessage = executor.getFunction(FUNCTION_GET_MESSAGE_NAME);
    }

    @Override
    public void preExecute() throws ComponentNotReadyException {
    	if (functionPreExecute != null) {
			executor.executeFunction(functionPreExecute, NO_ARGUMENTS);
    	}
    }

    @Override
    public void postExecute() throws ComponentNotReadyException {
    	if (functionPostExecute != null) {
			executor.executeFunction(functionPostExecute, NO_ARGUMENTS);
    	}
    }

    public String getMessage() {
    	if (functionGetMessage == null) {
    		return null;
    	}

		Object result = executor.executeFunction(functionGetMessage, NO_ARGUMENTS, NO_DATA_RECORDS, NO_DATA_RECORDS);

        if (!(result instanceof String)) {
            throw new TransformLangExecutorRuntimeException(functionGetMessage.getName()
            		+ "() function must return a string!");
        }

        return (String) result;
    }

	/**
	 * @deprecated Use {@link #postExecute()} method.
	 */
	@Deprecated
	@Override
	public final void finished() {
		logger.warn("Call to the finished() function ignored!");
	}

	/**
	 * @deprecated Use {@link #preExecute()} method.
	 */
	@Deprecated
	@Override
	public final void reset() {
		logger.warn("Call to the reset() function ignored!");
	}

}
