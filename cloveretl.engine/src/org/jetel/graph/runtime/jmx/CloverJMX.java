/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2002-08  David Pavlis <david_pavlis@hotmail.com>
*    
*    This library is free software; you can redistribute it and/or
*    modify it under the terms of the GNU Lesser General Public
*    License as published by the Free Software Foundation; either
*    version 2.1 of the License, or (at your option) any later version.
*    
*    This library is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU    
*    Lesser General Public License for more details.
*    
*    You should have received a copy of the GNU Lesser General Public
*    License along with this library; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
*/
package org.jetel.graph.runtime.jmx;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import org.jetel.graph.Phase;
import org.jetel.graph.runtime.WatchDog;

/**
 * JMX managed bean implementation.
 *  
 * @author Martin Zatopek (martin.zatopek@javlinconsulting.cz)
 *         (c) Javlin Consulting (www.javlinconsulting.cz)
 *
 * @created Jun 13, 2008
 */
public class CloverJMX extends NotificationBroadcasterSupport implements CloverJMXMBean, Serializable {

	private static final long serialVersionUID = 7993293097835091585L;

	transient static final MemoryMXBean MEMORY_MXBEAN = ManagementFactory.getMemoryMXBean();
    transient static final ThreadMXBean THREAD_MXBEAN = ManagementFactory.getThreadMXBean();
    
    transient private static boolean isThreadCpuTimeSupported = THREAD_MXBEAN.isThreadCpuTimeSupported();

	private final transient WatchDog watchDog;

	private final GraphTrackingDetail graphDetail;

    private boolean canClose = false;

    private int notificationSequence;
    
    /**
	 * Constructor.
	 * @param graph
	 */
	public CloverJMX(WatchDog watchDog) {
		this.watchDog = watchDog;
		this.graphDetail = new GraphTrackingDetail(this, watchDog.getGraph());
	}
	
	/* (non-Javadoc)
	 * @see org.jetel.graph.runtime.jmx.CloverJMXMBean#getCloverVersion()
	 */
	public String getCloverVersion() {
		// TODO Auto-generated method stub
		return "<unknown clover engine version>";
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.runtime.jmx.CloverJMXMBean#getGraphDetail()
	 */
	public GraphTrackingDetail getGraphDetail() {
		return graphDetail;
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.runtime.jmx.CloverJMXMBean#abortGraphExecution()
	 */
	public void abortGraphExecution() {
		watchDog.abort();
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.runtime.jmx.CloverJMXMBean#closeServer()
	 */
	synchronized public void closeServer() {
    	canClose = true;
	}

	public static boolean isThreadCpuTimeSupported() {
		return isThreadCpuTimeSupported;
	}

	WatchDog getWatchDog() {
		return watchDog;
	}

	synchronized public boolean canCloseServer() {
		return canClose;
	}
	
	//******************* EVENTS ********************/
	
	/* (non-Javadoc)
	 * @see org.jetel.graph.runtime.jmx.CloverJMXMBean#graphStarted()
	 */
	synchronized public void graphStarted() {
		getGraphDetail().graphStarted();

        sendNotification(new Notification(GRAPH_STARTED, this/*getGraphDetail()*/, notificationSequence++)); 
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.runtime.jmx.CloverJMXMBean#phaseStarted(org.jetel.graph.Phase)
	 */
	synchronized public void phaseStarted(Phase phase) {
		getGraphDetail().phaseStarted(phase);
		
		sendNotification(new Notification(PHASE_STARTED, this/*getGraphDetail().getRunningPhaseDetail()*/, notificationSequence++)); 
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.runtime.jmx.CloverJMXMBean#gatherTrackingDetails()
	 */
	synchronized public void gatherTrackingDetails() {
		getGraphDetail().gatherTrackingDetails();
		
		sendNotification(new Notification(TRACKING_UPDATED, this/*getGraphDetail().getRunningPhaseDetail()*/, notificationSequence++)); 
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.runtime.jmx.CloverJMXMBean#phaseFinished(org.jetel.graph.Phase)
	 */
	synchronized public void phaseFinished() {
		getGraphDetail().phaseFinished();
		
		sendNotification(new Notification(PHASE_FINISHED, this/*getGraphDetail().getRunningPhaseDetail()*/, notificationSequence++)); 
	}

	/* (non-Javadoc)
	 * @see org.jetel.graph.runtime.jmx.CloverJMXMBean#graphFinished()
	 */
	synchronized public void graphFinished() {
		System.out.println("GRAPH FINISHED SENT");
		getGraphDetail().graphFinished();

		sendNotification(new Notification(GRAPH_FINISHED, this/*getGraphDetail()*/, notificationSequence++)); 
	}

}
