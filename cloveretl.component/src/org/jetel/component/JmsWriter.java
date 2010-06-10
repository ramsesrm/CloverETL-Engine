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
package org.jetel.component;

import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetel.component.jms.DataRecord2JmsMsg;
import org.jetel.connection.jms.JmsConnection;
import org.jetel.data.DataRecord;
import org.jetel.database.IConnection;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.exception.ConfigurationProblem;
import org.jetel.exception.ConfigurationStatus;
import org.jetel.exception.JetelException;
import org.jetel.exception.XMLConfigurationException;
import org.jetel.graph.InputPort;
import org.jetel.graph.Node;
import org.jetel.graph.Result;
import org.jetel.graph.TransactionMethod;
import org.jetel.graph.TransformationGraph;
import org.jetel.util.compile.ClassLoaderUtils;
import org.jetel.util.compile.DynamicJavaClass;
import org.jetel.util.file.FileUtils;
import org.jetel.util.property.ComponentXMLAttributes;
import org.w3c.dom.Element;



/**
*  <h3>JmsWriter Component</h3>
*
* <table border="1">
*  <th>Component:</th>
* <tr><td><h4><i>Name:</i></h4></td>
* <td>JmsWriter</td></tr>
* <tr><td><h4><i>Category:</i></h4></td>
* <td></td></tr>
* <tr><td><h4><i>Description:</i></h4></td>
* <td>Transforms incoming data records to JMS messages using user-specified transformation class
* (so-called processor). The processor is supposed to implement interface <code>DataRecord2JmsMsg</code>.
* The processor may be specified either by class name or by inline Java code.
* </td></tr>
* <tr><td><h4><i>Inputs:</i></h4></td>
* <td>One input port for incoming data records.</td></tr>
* <tr><td><h4><i>Outputs:</i></h4></td>
* <td></td></tr>
* <tr><td><h4><i>Comment:</i></h4></td>
* <td></td></tr>
* </table>
*  <br>
*  <table border="1">
*  <th>XML attributes:</th>
*  <tr><td><b>type</b></td><td>"JMS_WRITER"</td></tr>
*  <tr><td><b>id</b></td><td>component identification</td>
*  <tr><td><b>connection</b></td><td>JMS connection ID</td>
*  <tr><td><b>processorCode</b></td><td>Inline Java code defining processor class</td>
*  <tr><td><b>processorClass</b></td><td>Name of processor class</td>
*  </tr>
 *  <tr><td><b>processorURL</b></td><td>path to the file with processor code</td></tr>
 *  <tr><td><b>charset</b><i>optional</i></td><td>encoding of extern source</td></tr>
*  </table>
*
* @author Jan Hadrava (jan.hadrava@javlinconsulting.cz), Javlin Consulting (www.javlinconsulting.cz)
* @since 09/15/06  
* @see         org.jetel.data.parser.FixLenDataParser
*/
public class JmsWriter extends Node {
	public final static String COMPONENT_TYPE = "JMS_WRITER";

	static Log logger = LogFactory.getLog(JmsWriter.class);

	private static final String XML_CONNECTION_ATTRIBUTE = "connection";
	private static final String XML_PSORCODE_ATTRIBUTE = "processorCode";
	private static final String XML_PSORCLASS_ATTRIBUTE = "processorClass";
	private static final String XML_PSORURL_ATTRIBUTE = "processorURL";
	private static final String XML_CHARSET_ATTRIBUTE = "charset";

	// component attributes
	private String conId;
	private String psorClass;
	private String psorCode;
	private String psorURL = null;
	private String charset = null;
	private Properties psorProperties;

	private InputPort inPort;
	
	private JmsConnection connection;
	private MessageProducer producer;	
	private DataRecord2JmsMsg psor;

	/** Sole ctor.
	 * @param id Component ID
	 * @param conId JMS connection ID
	 * @param psorClass Processor class
	 * @param psorCode Inline processor definition
	 * @param psorProperties Properties to be passed to data processor.
	 */
	public JmsWriter(String id, String conId, String psorClass, String psorCode,
			String psorURL, Properties psorProperties) {
		super(id);
		this.conId = conId;
		this.psorClass = psorClass;
		this.psorCode = psorCode;
		this.psorURL = psorURL;
		this.psorProperties = psorProperties;
	}

	public JmsWriter(String id, String conId, DataRecord2JmsMsg psor, Properties psorProperties) {
		super(id);
		this.conId = conId;
		this.psor = psor;
		this.psorProperties = psorProperties;
	}

	public void init() throws ComponentNotReadyException {
        if(isInitialized()) return;
		super.init();
		
		if (psor == null && psorClass == null && psorCode == null && psorURL == null) {
			psorClass = "org.jetel.component.jms.DataRecord2JmsMsgProperties";
		}
		IConnection c = getGraph().getConnection(conId);
		if (c == null || !(c instanceof JmsConnection)) {
			throw new ComponentNotReadyException("Specified connection '" + conId + "' doesn't seem to be a JMS connection");
		}

		connection = (JmsConnection)c;
		inPort = getInputPort(0);
		if (psor == null) {
			if (psorClass == null && psorCode == null) {
				psorCode = FileUtils.getStringFromURL(getGraph().getRuntimeContext().getContextURL(), psorURL, charset);
			}
			String[] classPaths = getGraph().getRuntimeContext().getClassPaths();
			psor = psorClass == null ? createProcessorDynamic(psorCode) : createProcessor(psorClass, classPaths);
		}
		psor.setGraph(this.getGraph());
	}
	
	

    @Override
	public void preExecute() throws ComponentNotReadyException {
		super.preExecute();
		psor.preExecute();
		
		if (firstRun()) {//a phase-dependent part of initialization
			psor.init(inPort.getMetadata(), connection.getSession(), psorProperties);
		} else {
			psor.reset();
		}
		try {
			producer = connection.createProducer();
		} catch (Exception e) {
			throw new ComponentNotReadyException("Unable to initialize JMS consumer: " + e.getMessage());
		}
	}

	@Override
	public void postExecute(TransactionMethod transactionMethod) throws ComponentNotReadyException {
		super.postExecute(transactionMethod);
		psor.postExecute(transactionMethod);
		psor.finished();
		
		closeConnection();
	}

	synchronized public void reset() throws ComponentNotReadyException {
    	super.reset();
    }

	@Override
	public synchronized void free() {
		super.free();
        closeConnection();
	}
    
    
	/** Creates processor instance of a class specified by its name.
	 * @param psorClass
	 * @return
	 * @throws ComponentNotReadyException
	 */
	private static DataRecord2JmsMsg createProcessor(String psorClass, String[] classPaths) throws ComponentNotReadyException {
		DataRecord2JmsMsg psor;
        try {
            psor =  (DataRecord2JmsMsg)Class.forName(psorClass, true, JmsWriter.class.getClassLoader()).newInstance();
        }catch (InstantiationException ex){
            throw new ComponentNotReadyException("Can't instantiate msg processor class: "+ex.getMessage());
        }catch (IllegalAccessException ex){
            throw new ComponentNotReadyException("Can't instantiate msg processor class: "+ex.getMessage());
        }catch (ClassNotFoundException ex) {
            if (classPaths == null)
                throw new ComponentNotReadyException( "Can't find specified transformation class: " + psorClass);
            try {
                URLClassLoader classLoader = ClassLoaderUtils.createClassLoader( JmsWriter.class.getClassLoader(), null, classPaths);
                psor =  (DataRecord2JmsMsg)Class.forName(psorClass, true, classLoader).newInstance();
            } catch (ClassNotFoundException ex1) {
                throw new ComponentNotReadyException("Can not find class: "+ ex1);
            } catch (Exception ex3) {
                throw new ComponentNotReadyException(ex3.getMessage());
            }
        }catch (Exception ex) {
            throw new ComponentNotReadyException("Can't create instance of msg processor class: " + psorClass + " "+ ex.getMessage(), ex);
        }
		return psor;
	}

	/**
	 * Creates processor instance of a class specified by its source code.
	 * @param psorCode
	 * @return
	 * @throws ComponentNotReadyException
	 */
	private DataRecord2JmsMsg createProcessorDynamic(String psorCode) throws ComponentNotReadyException {
        Object transObject = DynamicJavaClass.instantiate(psorCode, this.getClass().getClassLoader(),
        		getGraph().getRuntimeContext().getClassPathsUrls());

        if (transObject instanceof DataRecord2JmsMsg) {
			return (DataRecord2JmsMsg) transObject;
        }

        throw new ComponentNotReadyException("Provided msg processor class doesn't implement required interface.");
    }

	@Override
	public Result execute() throws Exception {
		DataRecord nextRecord = new DataRecord(inPort.getMetadata());
		nextRecord.init();
		try {
			while (runIt) {
				// read next
				nextRecord = inPort.readRecord(nextRecord);
				if (nextRecord == null)
					break;
				Message msg = psor.createMsg(nextRecord);
				if (msg == null)
					throw new JetelException(psor.getErrorMsg());

				int deliveryMode = msg.getJMSDeliveryMode();
		        if (deliveryMode != DeliveryMode.PERSISTENT && deliveryMode != DeliveryMode.NON_PERSISTENT)
		        	deliveryMode = Message.DEFAULT_DELIVERY_MODE;
				producer.send(msg, deliveryMode, msg.getJMSPriority(), Message.DEFAULT_TIME_TO_LIVE );
			}
			if (runIt) {
				// send terminating message
				Message termMsg = psor.createLastMsg();
				if (termMsg != null) {
					int deliveryMode = termMsg.getJMSDeliveryMode();
			        if (deliveryMode != DeliveryMode.PERSISTENT && deliveryMode != DeliveryMode.NON_PERSISTENT)
			        	deliveryMode = Message.DEFAULT_DELIVERY_MODE;
					producer.send(termMsg, deliveryMode, termMsg.getJMSPriority(), Message.DEFAULT_TIME_TO_LIVE );
				}
			}
		} catch (Exception e) {
			logger.error("JmxWriter execute", e);
			throw e;
		}
        return runIt ? Result.FINISHED_OK : Result.ABORTED;
	}

	/**
	 * Tries to close JMS connection. It keeps silence regardless of operation success/failure.
	 */
	private void closeConnection() {
		try {
			if (producer != null)
				producer.close();
		} catch (JMSException e) {
			// ignore it, the connection is probably already closed
		}
	}
	
	public String getType() {
		return COMPONENT_TYPE;
	}

	public void toXML(Element xmlElement) {
		super.toXML(xmlElement);
	
		xmlElement.setAttribute(XML_ID_ATTRIBUTE, getId());
		if (conId != null) {
			xmlElement.setAttribute(XML_CONNECTION_ATTRIBUTE, conId);
		}
		if (psorCode != null) {
			xmlElement.setAttribute(XML_PSORCODE_ATTRIBUTE, psorCode);
		}
		if (psorClass != null) {
			xmlElement.setAttribute(XML_PSORCLASS_ATTRIBUTE, psorClass);
		}
		if (psorURL != null) {
			xmlElement.setAttribute(XML_PSORURL_ATTRIBUTE, psorURL);
		}
		
		if (charset != null){
			xmlElement.setAttribute(XML_CHARSET_ATTRIBUTE, charset);
		}
		// set processor attributes
		for (Enumeration<Object> names = psorProperties.keys(); names.hasMoreElements();) {
			Object name = names.nextElement();
			Object value = psorProperties.get(name);
			xmlElement.setAttribute((String)name, (String)value);
		}
	}
	
	/** Creates an instance according to XML specification.
	 * @param graph
	 * @param xmlElement
	 * @return
	 * @throws XMLConfigurationException
	 */
	public static Node fromXML(TransformationGraph graph, Element xmlElement) throws XMLConfigurationException {
		JmsWriter jmsReader = null;
		ComponentXMLAttributes xattribs = new ComponentXMLAttributes(xmlElement, graph);
		
		try {
			jmsReader = new JmsWriter(xattribs.getString(XML_ID_ATTRIBUTE),
					xattribs.getString(XML_CONNECTION_ATTRIBUTE, null),
					xattribs.getString(XML_PSORCLASS_ATTRIBUTE, null),
					xattribs.getString(XML_PSORCODE_ATTRIBUTE, null),
					xattribs.getString(XML_PSORURL_ATTRIBUTE, null),
					xattribs.attributes2Properties(new String[]{	// all unknown attributes will be passed to the processor
							XML_ID_ATTRIBUTE, XML_CONNECTION_ATTRIBUTE,
							XML_PSORCLASS_ATTRIBUTE, XML_PSORCODE_ATTRIBUTE
					}));
					if (xattribs.exists(XML_CHARSET_ATTRIBUTE)) {
						jmsReader.setCharset(xattribs.getString(XML_CHARSET_ATTRIBUTE));
					}
		} catch (Exception ex) {
	           throw new XMLConfigurationException(COMPONENT_TYPE + ":" + xattribs.getString(XML_ID_ATTRIBUTE," unknown ID ") + ":" + ex.getMessage(),ex);
		}
		return jmsReader; 
	}

	public ConfigurationStatus checkConfig(ConfigurationStatus status) {
		super.checkConfig(status);
		 
		if(!checkInputPorts(status, 1, 1)
				|| !checkOutputPorts(status, 0, 0)) {
			return status;
		}
		
		if (charset != null && !Charset.isSupported(charset)) {
        	status.add(new ConfigurationProblem(
            		"Charset "+charset+" not supported!", 
            		ConfigurationStatus.Severity.ERROR, this, ConfigurationStatus.Priority.NORMAL));
        }

//        try {
        	
//    		if (psor == null && psorClass == null && psorCode == null) {
//    			throw new ComponentNotReadyException("Message processor not specified");
//    		}
//    		IConnection c = getGraph().getConnection(conId);
//    		if (c == null || !(c instanceof JmsConnection)) {
//    			throw new ComponentNotReadyException("Specified connection '" + conId + "' doesn't seem to be a JMS connection");
//    		}
//
//    		connection = (JmsConnection)c;
//    		inPort = getInputPort(0);
//     		try {
//    			connection.init();
//    			producer = connection.createProducer();
//    		} catch (Exception e) {
//    			throw new ComponentNotReadyException("Unable to initialize JMS consumer: " + e.getMessage());
//    		}
        	
        	
//            init();
//            free();
//        } catch (ComponentNotReadyException e) {
//            ConfigurationProblem problem = new ConfigurationProblem(e.getMessage(), ConfigurationStatus.Severity.ERROR, this, ConfigurationStatus.Priority.NORMAL);
//            if(!StringUtils.isEmpty(e.getAttributeName())) {
//                problem.setAttributeName(e.getAttributeName());
//            }
//            status.add(problem);
//        }
        
        return status;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

}
