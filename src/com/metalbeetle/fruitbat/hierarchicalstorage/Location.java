package com.metalbeetle.fruitbat.hierarchicalstorage;

import com.metalbeetle.fruitbat.io.DataSink;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.List;

public interface Location extends DataSink, DataSrc {
	public String getName();
	public boolean exists() throws FatalStorageException;
	public Location parent() throws FatalStorageException;
	public Location child(String name) throws FatalStorageException;
	public List<Location> children() throws FatalStorageException;
	public KVFile kvFile() throws FatalStorageException;
	public KVFile kvFile(Location cache) throws FatalStorageException;
	public void put(DataSrc data) throws FatalStorageException;
	public void delete() throws FatalStorageException;
}
