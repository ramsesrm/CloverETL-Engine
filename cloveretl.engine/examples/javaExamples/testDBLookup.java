/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2002  David Pavlis
*
*    This program is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 2 of the License, or
*    (at your option) any later version.
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.jetel.connection.DBConnection;
import org.jetel.data.DataRecord;
import org.jetel.graph.runtime.EngineInitializer;
import org.jetel.lookup.DBLookupTable;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.metadata.DataRecordMetadataXMLReaderWriter;

public class testDBLookup{

	private final static String PARAMETER_FILE = "params.txt"; 
	private final static String PLUGINS_PROPERTY = "plugins";
	private final static String PROPERTIES_FILE_PROPERTY = "propertiesFile";
	private final static String CONNECTION_PROPERTY = "connection";
	private final static String QUERY_PROPERTY = "query";
	private final static String KEY_PROPERTY = "key";
	private final static String METADATA_PROPERTY = "metadataFile"; 
	
	private final static String[] ARGS = {PLUGINS_PROPERTY, PROPERTIES_FILE_PROPERTY, CONNECTION_PROPERTY,
		QUERY_PROPERTY, KEY_PROPERTY, METADATA_PROPERTY
	};
	
	private final static int PLUGINS_PROPERTY_INDEX = 0;
	private final static int PROPERTIES_FILE_PROPERTY_INDEX = 1;
	private final static int CONNECTION_PROPERTY_INDEX = 2;
	private final static int QUERY_PROPERTY_INDEX = 3;
	private final static int KEY_PROPERTY_INDEX = 4;
	private final static int METADATA_PROPERTY_INDEX = 5;

	public static void main(String args[]){
	DBConnection dbCon;
	
	Properties arguments = new Properties();
	if ((new File(PARAMETER_FILE)).exists()) {
		try {
			arguments.load(new FileInputStream(PARAMETER_FILE));
		} catch (FileNotFoundException e) {
			//do nothing: we checked it
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	String[] arg = new String[ARGS.length];
	
	for (int i = 0; i < arg.length; i++){
		if (args.length > i) {
			arg[i] = args[i];
		}else{
			arg[i] = arguments.getProperty(ARGS[i]);
			if (i < 5 && arg[i] == null) {
				System.out.println("Required argument " + ARGS[i] + " not found");
				System.out.println("Usage: testDBLookup <plugin directory> <engine properties file> <driver properties file> <sql query> <key> <db metadata file>");
				System.out.println("Eg: testDBLookup ../plugins postgre.cfg \"select * from employee where employee_id = ?\" 10");
				System.exit(1);
			}
		}
	}
		
	//initialization; must be present
	EngineInitializer.initEngine(arg[0], arg[1], null);

	System.out.println("**************** Input parameters: ****************");
	System.out.println("Plugins directory: "+ arg[PLUGINS_PROPERTY_INDEX]);
	System.out.println("Properties file: "+ arg[PROPERTIES_FILE_PROPERTY_INDEX]);
	System.out.println("Driver propeties: "+arg[CONNECTION_PROPERTY_INDEX]);
	System.out.println("SQL query: "+arg[QUERY_PROPERTY_INDEX]);
	System.out.println("Key: "+arg[KEY_PROPERTY_INDEX]);
	if (arg[METADATA_PROPERTY_INDEX] == null && args.length == 6) {
		arg[METADATA_PROPERTY_INDEX] = args[METADATA_PROPERTY_INDEX];
		System.out.println("Metadata file: " + arg[METADATA_PROPERTY_INDEX]);
	}
	System.out.println("***************************************************");
	
	DataRecordMetadata metadataIn = null;
	DataRecord data;
	
	if (arg[METADATA_PROPERTY_INDEX] != null) {
		DataRecordMetadataXMLReaderWriter metaReader = new DataRecordMetadataXMLReaderWriter();
		try {
			metadataIn = metaReader.read(new FileInputStream(arg[METADATA_PROPERTY_INDEX]));
		} catch (FileNotFoundException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}
	//create connection object. Get driver and connect string from cfg file specified as a first argument
	dbCon=new DBConnection("Conn0",arg[CONNECTION_PROPERTY_INDEX]);
	try{
		dbCon.init();
		
		// create lookup table. Will use previously created connection. The query string
		// is specified as a second parameter
		// query string should contain ? (questionmark) in where clause
		// e.g. select * from customers where customer_id = ? and customer_city= ?
		DBLookupTable lookup=new DBLookupTable("lookup",dbCon.getConnection(dbCon.getId()),metadataIn,arg[QUERY_PROPERTY_INDEX]);
		
		/*
		* in case the DB doesn't support getMetadata, use following constructor:
		* (don't forget to create metadata object first. For example by analyzing DB
		* first and then using DataRecordMetadataXMLReaderWriter
		
		
		
		*/
		
		// we initialize lookup
		lookup.init();
		//try to lookup based on specified parameter
		//following version of get() method is valid for queries with one parameter only
		//in case you have more (as with the example shown above), use array of objects (strings, integers, etc.) and
		//call get(Object[])
		
		
		data=lookup.get(arg[KEY_PROPERTY_INDEX]);
		
		if (data == null) {
			System.out.println("Nothing found for given key");
		}
		
		//in case query returns more than one record, continue displaying it.
		while(data!=null){
			System.out.println(data);
			data=lookup.getNext();
		}
		
	}catch(Exception ex){
		ex.printStackTrace();
	}
	}
} 


