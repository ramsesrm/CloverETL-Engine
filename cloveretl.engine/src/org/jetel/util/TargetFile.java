package org.jetel.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetel.data.ByteDataField;
import org.jetel.data.DataField;
import org.jetel.data.DataRecord;
import org.jetel.data.Defaults;
import org.jetel.data.StringDataField;
import org.jetel.data.formatter.Formatter;
import org.jetel.data.formatter.provider.FormatterProvider;
import org.jetel.enums.ProcessingType;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.graph.OutputPort;
import org.jetel.graph.dictionary.Dictionary;
import org.jetel.graph.dictionary.IDictionaryType;
import org.jetel.graph.dictionary.WritableChannelDictionaryType;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.util.bytes.RestrictedByteArrayOutputStream;
import org.jetel.util.file.FileUtils;

/**
 * TargetFile is used for basic operation over output files or streams and formatter. It support methods
 * for multifile record.
 * 
 * @author Jan Ausperger (jan.ausperger@javlinconsulting.cz)
 *         (c) Javlin Consulting (www.javlinconsulting.cz)
 */
public class TargetFile {
    private static Log logger = LogFactory.getLog(TargetFile.class);
    
	private static final char NUM_CHAR='#';			// file markter that is replacet file tag.
	private static final String EMPTY_STRING="";
	private static final String PARAM_DELIMITER = ":";
	private static final String PORT_DELIMITER = "\\.";
	private static final String PORT_PROTOCOL = "port:";
	private static final String DICT_PROTOCOL = "dict:";
	private static final String DEFAULT_CHARSET = "UTF-8";
	
	private DecimalFormat format;					// it is used if the file tag is a number

	private String fileURL;							// output file url
	private URL contextURL;							// output context url
    private Iterator<WritableByteChannel> channels; // output channel
	private FormatterProvider formatterProvider;		// creates new formatter
	private DataRecordMetadata metadata;			// metadata
	
    private Iterator<String> fileNames;				// returns filename string
    private Formatter formatter;					// writes records into output
	
    private int records;							// count of record sent to formatter
    private int bytes;								// count of bytes sent to formatter
    private boolean appendData;						// appends data to output
	private boolean useChannel = true;				// if can be used a byteChannel
    
	private Object fileTag;							// string of marks '#' are replaced by this fileTag 
    private String before;							// string of fileURL before last string of marks '#'
    private String after;							// string of fileURL after last string of marks '#'
	private String fileName;

    private WritableByteChannel byteChannel;

	private OutputPort outputPort;
	private DataRecord record;
	private DataField field; 
	private boolean isStringDataField;
	private ProcessingType fieldProcesstingType;
	private CharsetDecoder decoder;
	private ByteBuffer byteBuffer;
	private CharBuffer charBuffer;

	private String charset;

	private Dictionary dictionary;
	private ProcessingType dictProcesstingType;
	private WritableByteChannel dictOutChannel;
	private ArrayList<byte[]> dictOutArray;
	private boolean fieldOrDictOutput;
	private ByteArrayOutputStream bbOutputStream;

	private int compressLevel = -1;

    /**
     * Constructors.
     */
    public TargetFile(String fileURL, URL contextURL, Formatter formatter, DataRecordMetadata metadata) {
    	this.fileURL = fileURL;
    	this.contextURL = contextURL;
    	this.formatter = formatter;
    	this.metadata = metadata;
    }
    
    public TargetFile(Iterator<WritableByteChannel> channels, Formatter formatter, DataRecordMetadata metadata) {
    	this.channels = channels;
    	this.formatter = formatter;
    	this.metadata = metadata;
    }
    
    public TargetFile(String fileURL, URL contextURL, FormatterProvider formatterProvider, DataRecordMetadata metadata) {
    	this.fileURL = fileURL;
    	this.contextURL = contextURL;
    	this.formatterProvider = formatterProvider;
    	this.metadata = metadata;
    }
    
    public TargetFile(Iterator<WritableByteChannel> channels, FormatterProvider formatterProvider, DataRecordMetadata metadata) {
    	this.channels = channels;
    	this.formatterProvider = formatterProvider;
    	this.metadata = metadata;
    }

    /**
     * Prepares file url string, initialize output and formatter.
     * 
     * @throws IOException
     * @throws ComponentNotReadyException
     */
    public void init() throws IOException, ComponentNotReadyException {
    	if (charset == null) charset = DEFAULT_CHARSET;
    	if (fileURL != null && fileURL.startsWith(PORT_PROTOCOL)) {
        	initPortFields();
    	} else if (outputPort != null) {
    		throw new ComponentNotReadyException("File url must contains port or dict protocol.");
    	} else if (fileURL != null && fileURL.startsWith(DICT_PROTOCOL)) {
           	initDictTarget();
    	} else {
        	initUrl();
        	if (fileTag == null) {
        		initFileNames(null);
        	} 
        	else if (fileTag instanceof Number) {
        		initFileNames(format.format((Number)fileTag));
        	} else {
        		initFileNames(fileTag.toString());
        	}
    	}
    	initOutput();
    }
    
    public void reset() {
    	if (fileNames != null) {
			((MultiOutFile) fileNames).reset();
		}
		formatter.reset();
		
		// reset CharsetDecoder
    	if (decoder == null) decoder.reset();
	}
    
    /**
     * Output port if data should be write to an output field.
     * 
     * @param outputPort
     */
    public void setOutputPort(OutputPort outputPort) {
    	this.outputPort = outputPort;
    }    

    /**
     * FileURL can contains '#' mark, the string of marks is replaced the fileTag. If a file tag is Number,
     * there is used NumberFormat for definition of minimal lenght of the fileTag. Ie: ## and 5 is "05". 
     * 
     * @param fileTag
     */
    public void setFileTag(Object fileTag) {
    	this.fileTag = fileTag;
    }
    
    public void setFileName(String fileName) {
    	this.fileName = fileName;
    }

    /**
     * Replaces '#' string of marks for value and creates MultiOutFile.
     * 
     * @param value
     * @throws IOException
     * @throws ComponentNotReadyException
     */
    private void initFileNames(String value) throws IOException, ComponentNotReadyException {
    	if (fileURL != null) fileNames = new MultiOutFile(value == null ? fileURL : before + value + after);
    }
    
    /**
     * Divides fileURL to two string. The first one is a string before '#' mark, the second one is
     * after mark. If no mark found, before string is fileURL.
     * Creates decimal format.
     */
    private void initUrl() {
    	if (fileURL == null) return;
    	int idxLast = fileURL.lastIndexOf(NUM_CHAR);
    	if (idxLast == -1) {
    		before = fileURL;
    		after = EMPTY_STRING;
    		format = new DecimalFormat();
    		return;
    	}
    	StringBuilder sb = new StringBuilder();
    	int idxFirst;
		for (idxFirst = idxLast; idxFirst > 0 && fileURL.charAt(idxFirst) == NUM_CHAR; idxFirst--) {
			sb.append(0);
		}
		before = fileURL.substring(0, idxFirst+1);
		after = fileURL.substring(idxLast+1, fileURL.length());
    	format = new DecimalFormat(sb.toString());
    }

	private void initDictTarget() throws ComponentNotReadyException {
		// parse target url
		String[] aDict = fileURL.substring(DICT_PROTOCOL.length()).split(PARAM_DELIMITER);
		if (dictionary == null) throw new RuntimeException("The component doesn't support dictionary writing.");
		Object dictValue = dictionary.getValue(aDict[0]);
		IDictionaryType dictType = dictionary.getType(aDict[0]);
		dictProcesstingType = ProcessingType.fromString(aDict.length > 1 ? aDict[1] : null, ProcessingType.STREAM);
		
		// create target
		switch(dictProcesstingType){
			case STREAM:
				if( dictValue == null){
					// predpoklada se, ze pred spustenim grafu je do dictionary dan OutputStream, kam se vystup nasype
					// zatim neni moznost spustit graf a az pote se zajimat o vystup, aby to bylo mozne, je potreba upravit
					// interface dictionary a pridat tam moznost ziskani dat
					throw new IllegalStateException("Dictionary doesn't contain value for the key '" + aDict[0] + "'.");
				}
				if( !(dictType instanceof WritableChannelDictionaryType)){
					throw new IllegalStateException("Dictionary contains invalid type '"+dictType+"' for the key '" + aDict[0] + "'."); 
				}
				if (dictValue instanceof WritableByteChannel) {
					WritableByteChannel channel = (WritableByteChannel) dictValue;
					dictOutChannel = channel;
				}
				break;
			case DISCRETE:
				dictOutArray = new ArrayList<byte[]>();
				dictionary.setValue(aDict[0], dictOutArray);
				break;
			default:
				throw new ComponentNotReadyException("invalid dictionary processting type " + dictProcesstingType);
		}
	}
    
	private void initPortFields() throws ComponentNotReadyException {
		// prepare output record
		if (outputPort == null) throw new ComponentNotReadyException("Output port is not connected.");
		record = new DataRecord(outputPort.getMetadata());
		record.init();
		
		// parse target url
		String[] aField = fileURL.substring(PORT_PROTOCOL.length()).split(PARAM_DELIMITER);
		if (aField.length < 1) throw new ComponentNotReadyException("The source string '" + fileURL + "' is not valid.");
		String[] aFieldNamePort = aField[0].split(PORT_DELIMITER);
		fieldProcesstingType = ProcessingType.fromString(aField.length > 1 ? aField[1] : null, ProcessingType.DISCRETE);
		if (aFieldNamePort.length < 2) throw new ComponentNotReadyException("The source string '" + fileURL + "' is not valid.");
		String fName = aFieldNamePort[1];

		// check setting
		if (record.hasField(fName)) field = record.getField(fName);
		if (field == null) throw new ComponentNotReadyException("The field not found for the statement: '" + fileURL + "'");
		if (field instanceof StringDataField) isStringDataField = true;
		else if (!(field instanceof ByteDataField))	throw new ComponentNotReadyException("The field '" + field.getMetadata().getName() + "' must be String or (Compressed) Byte data field.");

		decoder = Charset.forName(charset).newDecoder();
		//FIXME
		int x = Defaults.PortReadingWriting.DATA_LENGTH;
		byteBuffer = ByteBuffer.allocateDirect(x);
		charBuffer = CharBuffer.allocate(x);
		charBuffer.flip(); // initially empty 
	}

    /**
     * The method writes footer and header and sets next output to the formatter.
     * 
     * @throws IOException
     */
    public void setNextOutput() throws IOException {
    	if (field == null) {
        	checkOutput();
    	}
    	
        //write footer to the previous destination if it is not first call of this method
        if(byteChannel != null || bbOutputStream != null) {
//        	formatter.writeFooter();	// issue 1503
        	formatter.finish();
        }
        setOutput();

        bytes = records = 0;
        
        formatter.writeHeader();
    }

    public void finish() throws IOException{
    	formatter.finish();
    	formatter.close();
    	write2FieldOrDict();
    }
    
    private void write2FieldOrDict() throws IOException {
    	if (fieldOrDictOutput) {
        	if (bbOutputStream != null) write2OutportOrDictionary(bbOutputStream.toByteArray());
    		try {
    			// there is only one target for port and dictionary protocol
				if (outputPort != null) outputPort.eof();
			} catch (InterruptedException e) {
				throw new IOException(e.getMessage());
			}
    	}
    }
    
    /**
     * Write data to the output port or to the dictionary.
     * @param aData
     * @throws IOException
     */
    private void write2OutportOrDictionary(byte[] aData) throws IOException {
    	if (bbOutputStream != null) {
            if (dictProcesstingType != null) {
            	if (dictOutArray != null) {
            		dictOutArray.add(aData);
            	}
            }
            if (field != null) {
            	// repeat =0 is 1 record, =1 are 2 records, ...
				boolean streamType = fieldProcesstingType == ProcessingType.STREAM;
            	int repeat = 0;
            	if (aData.length > 0) {
            		repeat = aData.length / (streamType ? Defaults.PortReadingWriting.DATA_LENGTH : aData.length);
            	}
            	
            	// write to string field
    			if (isStringDataField) {
    				write2StringField(aData, streamType, repeat);

            	// write to byte field
    			} else {
    				write2ByteField(aData, streamType, repeat);
    			}
            }
    	}
    }
    
    /**
     * Write byte array to string field for n records
     * @param aData
     * @param streamType
     * @param repeat
     * @throws IOException
     */
    private void write2StringField(byte[] aData, boolean streamType, int repeat) throws IOException {
		// string field - stream mode
    	if (streamType) {
    		byteBuffer.clear();
    		byteBuffer.put(aData);
    		byteBuffer.flip();
    		for (int i=repeat; i>=0; i--) {
    			decoder.reset();
    			boolean isEof = i==0;
    			// how to convert unicode from byte array
                CoderResult result = decoder.decode(byteBuffer, charBuffer, isEof);
                if (result.isError()) {
                    throw new IOException(result.toString()+" when converting from "+decoder.charset());
                }
                if (isEof) {
                    result = decoder.flush(charBuffer);
                    if (result.isError()) {
                    	throw new IOException(result.toString()+" when converting from "+decoder.charset());
                    }
                }
                charBuffer.flip();
       			field.setValue(charBuffer.toString());
    		}
    		
    		//broadcast the record to the connected edge
    		writeRecord();
    		
   		// string field - byte mode
    	} else {
        	if (aData.length == 0) return;
   			field.setValue(new String(aData, charset));

   			//broadcast the record to the connected edge
    		writeRecord();
    	}
    }

    /**
     * Write byte array to byte/cbyte field for n records
     * @param aData
     * @param streamType
     * @param repeat
     */
    private void write2ByteField(byte[] aData, boolean streamType, int repeat) {
		if (aData.length == 0)	return;
		
		// byte field - stream mode
    	if (streamType) {
			// is it necessary to copy the byte array?
			byte[] subArray = null;
			int size = Defaults.PortReadingWriting.DATA_LENGTH;
			if (repeat > 0) {
				subArray = new byte[size];
			}
			
			// send all data to records, last record is null
    		for (int i=0; i<=repeat; i++) {
    			if (i == repeat) {
    				size = aData.length % Defaults.PortReadingWriting.DATA_LENGTH;
    				subArray = new byte[size];
    			}
       	    	System.arraycopy(aData, Defaults.PortReadingWriting.DATA_LENGTH*i, subArray, 0, size);
   				field.setValue(subArray);

    			//broadcast the record to the connected edge
    			writeRecord();
    		}
    		
    		//null mark
			field.setNull(true);

		// byte field - discrete mode
		} else {
			field.setValue(aData);
		}
    	
		//broadcast the record to the connected edge
		writeRecord();
    }
    
    /**
     * Writes record to the output port.
     */
    private void writeRecord() {
		//broadcast the record to all connected Edges
		try {
			outputPort.writeRecord(record);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		SynchronizeUtils.cloverYield();
    }
    
    /**
     * Closes underlying formatter.
     */
    public void close() {
        formatter.close();
    }
    
    /**
     * Prepares output file.
     * 
     * @throws IOException 
     * @throws ComponentNotReadyException 
     */
    private void initOutput() throws IOException, ComponentNotReadyException {
    	if (formatter == null) formatter = formatterProvider.getNewFormatter();
    	formatter.init(metadata);
    	setNextOutput();
    }
    
    private void checkOutput() {
    	if (fileNames != null && !fileNames.hasNext()) {
            logger.warn("Unable to open new output file. This may be caused by missing wildcard in filename specification. "
                    + "Size of output file will exceed specified limit.");
            return;
    	}
    	if (channels != null && !channels.hasNext()) {
            logger.warn("Unable to open new output stream. Size of last output stream will exceed specified limit.");
            return;
        }
    }

    /**
     * Prepares next output to the data formatter.
     * 
     * @throws IOException
     */
    private void setOutput() throws IOException {
    	if (fieldOrDictOutput = (field != null || dictProcesstingType != null)) {
    		if (dictOutChannel != null) {
        		setDataTarget(dictOutChannel);
    		}
    		else {
    			if (bbOutputStream != null) {
            		write2OutportOrDictionary(bbOutputStream.toByteArray());
                	bbOutputStream.reset();
            	} else {
                    // stream mode
            		if (fieldProcesstingType == ProcessingType.STREAM) {
                       	bbOutputStream = new ByteArrayOutputStream();
                    
               		// discrete mode
            		} else {
                       	bbOutputStream = new RestrictedByteArrayOutputStream();
                       	if (field != null) 
                       		((RestrictedByteArrayOutputStream)bbOutputStream).setMaxArrayLength(Defaults.DataFormatter.FIELD_BUFFER_LENGTH);
            		}
            	}
        		setDataTarget(Channels.newChannel(bbOutputStream));
    		}
    		
    	} else if (fileNames != null) {
            String fName = fileNames.next();
            if (fileName != null) fName = addUnassignedName(fName);
        	byteChannel = FileUtils.getWritableChannel(contextURL, fName, appendData, compressLevel );
        	if (useChannel) {
        		setDataTarget(byteChannel);
        	} else {
           		setDataTarget(FileUtils.getFileURL(contextURL, fName));
        	}
        	
        } else {
        	byteChannel = channels.next();
        	setDataTarget(byteChannel);
        }
    }
    
    private String addUnassignedName(String fName) throws IOException {
    	int k = fName.lastIndexOf('#');
    	String name; 
    	if (k < 0) {
    		k = 0;
    	}
		name = fName.substring(k);
    	int j = name.lastIndexOf('/');
    	if (j < 0) j = name.lastIndexOf('\\');
    	if (j > 0) k = j;
    	return fName.substring(0, k+1) + fileName;
    }
    
    /**
     * Sets logger.
     * 
     * @param log
     */
    public static void setLogger(Log log) {
		logger = log;
	}

    /**
     * Sets a output to the data formatter.
     * 
     * @param outputDataTarget
     */
    public void setDataTarget(Object outputDataTarget) {
    	formatter.setDataTarget(outputDataTarget);
    }
    
    public Iterator<String> getFileNames() {
    	return fileNames;
    }
    
    public Formatter getFormatter() {
    	return formatter;
    }
    
    public int getRecords() {
    	return records;
    }

    public void setRecords(int records) {
    	this.records = records;
    }
    
    public int getBytes() {
    	return bytes;
    }

    public void setBytes(int bytes) {
    	this.bytes = bytes;
    }

    public void setAppendData(boolean appendData) {
        this.appendData = appendData;
    }
    
	public void setUseChannel(boolean useChannel) {
		this.useChannel = useChannel;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setDictionary(Dictionary dictionary) {
		this.dictionary = dictionary;
	}

	public int getCompressLevel() {
		return compressLevel;
	}

	public void setCompressLevel(int compressLevel) {
		this.compressLevel = compressLevel;
	}

}
