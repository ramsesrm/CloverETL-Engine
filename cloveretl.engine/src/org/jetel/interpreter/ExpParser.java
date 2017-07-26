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
package org.jetel.interpreter;

import java.util.Map;

import org.jetel.metadata.DataRecordMetadata;


/**
 * @author david
 * @since  22.6.2006
 *
 * This class is only the ancestor of all Parsers in
 * order to share Nodes
 */
public abstract class ExpParser {

    public ExpParser() {
        // TODO Auto-generated constructor stub
    }

    public abstract DataRecordMetadata getInRecordMeta();
 
    public abstract int getInRecordNum(String name);
 
    public abstract int getOutRecordNum(String name);
 
    public abstract DataRecordMetadata getInRecordMeta(int num);
 
    public abstract DataRecordMetadata getOutRecordMeta(int num);
 
    public abstract DataRecordMetadata[] getInRecordMetadata();
    
    public abstract DataRecordMetadata[] getOutRecordMetadata();
 
    
    public abstract Map getFunctions();
}