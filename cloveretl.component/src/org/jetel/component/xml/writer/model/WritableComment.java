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
package org.jetel.component.xml.writer.model;

import java.io.IOException;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jetel.component.xml.writer.XmlFormatter;
import org.jetel.component.xml.writer.model.WritableMapping.MappingWriteState;
import org.jetel.data.DataRecord;

/**
 * class representing xml comment
 * 
 * @author tkramolis (info@cloveretl.com)
 *         (c) Javlin, a.s. (www.cloveretl.com)
 *
 * @created 31 Mar 2011
 */
public class WritableComment implements Writable {

	private WritableValue value;
	
	public WritableComment(WritableValue value) {
		this.value = value;
	}

	@Override
	public void write(XmlFormatter formatter, Map<Integer, DataRecord> availableData) 
	throws XMLStreamException, IOException {
		MappingWriteState state = formatter.getMapping().getState();
		if (state == MappingWriteState.ALL || state == MappingWriteState.HEADER) {
			formatter.getWriter().writeComment(value.getText(availableData));
		}
	}

	@Override
	public boolean isEmpty(Map<Integer, DataRecord> availableData) {
		return false;
	}
	
}
