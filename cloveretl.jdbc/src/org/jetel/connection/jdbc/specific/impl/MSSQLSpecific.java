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
package org.jetel.connection.jdbc.specific.impl;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.jetel.connection.jdbc.DBConnection;
import org.jetel.connection.jdbc.specific.conn.DefaultConnection;
import org.jetel.connection.jdbc.specific.conn.MSSQLConnection;
import org.jetel.exception.JetelException;
import org.jetel.metadata.DataFieldMetadata;

/**
 * MS SQL 2008 specific behaviour.
 * 
 * This specific works primarily on SQL Server 2008 and above
 * although most of the features work on SQL Server 2005 and older
 * 
 * @modified Pavel Najvar (pavel.najvar@javlin.eu) Mar 2009
 * @author Martin Zatopek (martin.zatopek@javlinconsulting.cz)
 *         (c) Javlin Consulting (www.javlinconsulting.cz)
 *
 * @created Jun 3, 2008
 */
public class MSSQLSpecific extends AbstractJdbcSpecific {

	private static final MSSQLSpecific INSTANCE = new MSSQLSpecific();
	
	protected MSSQLSpecific() {
		super(AutoGeneratedKeysType.SINGLE);
	}

	public static MSSQLSpecific getInstance() {
		return INSTANCE;
	}

	@Override
	protected DefaultConnection prepareSQLConnection(DBConnection dbConnection, OperationType operationType) throws JetelException {
		return new MSSQLConnection(dbConnection, operationType);
	}

    @Override
	public String quoteIdentifier(String identifier) {
        return ('[' + identifier + ']');
    }

	@Override
	public String sqlType2str(int sqlType) {
		switch(sqlType) {
		case Types.TIMESTAMP :
			return "DATETIME";
		case Types.BOOLEAN :
			return "BIT";
		case Types.INTEGER :
			return "INT";
		case Types.NUMERIC :
		case Types.DOUBLE :
			return "FLOAT";
		}
		return super.sqlType2str(sqlType);
	}

	@Override
	public int jetelType2sql(DataFieldMetadata field) {
		switch (field.getType()) {
		case DataFieldMetadata.BOOLEAN_FIELD:
			return Types.BIT;
		case DataFieldMetadata.NUMERIC_FIELD:
			return Types.DOUBLE;
		default:
	return super.jetelType2sql(field);
		}
	}
	
	@Override
	public char sqlType2jetel(int sqlType) {
		switch (sqlType) {
		case Types.BIT:
			return DataFieldMetadata.BOOLEAN_FIELD;
		default:
			return super.sqlType2jetel(sqlType);
		}
	}

	@Override
	public ArrayList<String> getSchemas(java.sql.Connection connection)
			throws SQLException {
	  ArrayList <String> currentCatalog = new ArrayList<String>();
	  currentCatalog.add(connection.getCatalog());
	  return currentCatalog;
	}
	
	@Override
	public String getTablePrefix(String schema, String owner,
			boolean quoteIdentifiers) {
		String tablePrefix;
		String notNullOwner = (owner == null) ? "" : owner;
		if(quoteIdentifiers) {
			tablePrefix = quoteIdentifier(schema)+".";
			//in case when owner is empty or null skip adding
			if(!notNullOwner.isEmpty())
				tablePrefix += quoteIdentifier(notNullOwner);
		} else {
			tablePrefix = schema+"."+notNullOwner;
		}
		return tablePrefix;
	}
	
	@Override
	public int getSqlTypeByTypeName(String sqlTypeName) {
		//text and ntext types are deprecated
		if (sqlTypeName.equals("text") || sqlTypeName.equals("ntext")) {
			//jtds returns Types.CLOB but we want to map them on Types.VARCHAR
			return Types.VARCHAR;
		}
		return Types.CLOB;
	}
}
