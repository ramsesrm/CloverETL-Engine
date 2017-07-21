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
package org.jetel.hadoop.connection;

import java.net.URI;

public class HadoopMapReduceJobDetails {
	private URI jobJarFile;
	// TODO Consider changing type of next 2 attributes to File
	private URI[] inputFiles;
	private URI outputFile;
	private URI workingDirectory;

	private String jobName;
	private String user;

	private Class<?> mapper;
	private Class<?> combiner;
	private Class<?> reducer;
	private Class<?> inputFormat;
	private Class<?> outputFormat;
	private Class<?> outputKey;
	private Class<?> outputValue;

	public HadoopMapReduceJobDetails(URI jobJarFile, URI[] inputFiles, URI outputFile, URI workingDirectory,
			String jobName, String user, Class<?> mapper, Class<?> combiner, Class<?> reducer, Class<?> inputFormat,
			Class<?> outputFormat, Class<?> outputKey, Class<?> outputValue) {
		if (jobJarFile == null) {
			throw new NullPointerException("jobJarFile");
		}
		if (inputFiles == null) {
			throw new NullPointerException("inputFile");
		}
		if (inputFiles.length == 0) {
			throw new IllegalArgumentException("inputFiles must contain at least one input file");
		}
		if (outputFile == null) {
			throw new NullPointerException("outputFile");
		}
		if (mapper == null) {
			throw new NullPointerException("mapper");
		}
		if (outputKey == null) {
			throw new NullPointerException("outputKey");
		}
		if (outputValue == null) {
			throw new NullPointerException("outputValue");
		}

		this.jobJarFile = jobJarFile;
		this.inputFiles = inputFiles;
		this.outputFile = outputFile;
		this.workingDirectory = workingDirectory;
		this.jobName = jobName;
		this.user = user;
		this.mapper = mapper;
		this.combiner = combiner;
		this.reducer = reducer;
		this.inputFormat = inputFormat;
		this.outputFormat = outputFormat;
		this.outputKey = outputKey;
		this.outputValue = outputValue;
	}

	/**
	 * @return the jobJarFile
	 */
	public URI getJobJarFile() {
		return jobJarFile;
	}

	/**
	 * @return the inputFiles
	 */
	public URI[] getInputFiles() {
		return inputFiles;
	}

	public String getUser() {
		return user;
	}

	/**
	 * @return the outputFile
	 */
	public URI getOutputFile() {
		return outputFile;
	}

	/**
	 * @return the workingDirectory
	 */
	public URI getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * @return the jobName
	 */
	public String getJobName() {
		return jobName;
	}

	/**
	 * @return the mapper
	 */
	public Class<?> getMapper() {
		return mapper;
	}

	/**
	 * @return the combiner
	 */
	public Class<?> getCombiner() {
		return combiner;
	}

	/**
	 * @return the reducer
	 */
	public Class<?> getReducer() {
		return reducer;
	}

	/**
	 * @return the inputFormat
	 */
	public Class<?> getInputFormat() {
		return inputFormat;
	}

	/**
	 * @return the outputFormat
	 */
	public Class<?> getOutputFormat() {
		return outputFormat;
	}

	/**
	 * Gets value of outputKey.
	 * 
	 * @return the outputKey
	 */
	public Class<?> getOutputKey() {
		return outputKey;
	}

	/**
	 * Gets value of outputValue.
	 * 
	 * @return the outputValue
	 */
	public Class<?> getOutputValue() {
		return outputValue;
	}
}
