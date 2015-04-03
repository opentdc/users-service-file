/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Arbalo AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.opentdc.users.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.opentdc.service.exception.DuplicateException;
import org.opentdc.service.exception.NotFoundException;
import org.opentdc.users.ServiceProvider;
import org.opentdc.users.UserModel;

public class FileServiceProvider implements ServiceProvider {

	private static Map<String, UserModel> data = new HashMap<String, UserModel>();
	private static final Logger logger = Logger.getLogger(FileServiceProvider.class.getName());

	public FileServiceProvider(
		ServletContext context, 
		String prefix
	) {
	}
	
	private void setNewID(UserModel dataObj) {
		String _id = UUID.randomUUID().toString();
		dataObj.setId(_id);
	}

	private void storeData(UserModel dataObj) {
		data.put(dataObj.getId(), dataObj);
	}

	private UserModel getData(String id) {
		return data.get(id);
	}

	private List<UserModel> getData() {
		return new ArrayList<UserModel>(data.values());
	}

	private int getDataSize() {
		int _retVal = 0;
		if (data != null) {
			_retVal = data.size();
		}
		return _retVal;
	}

	private void removeData(String id) {
		data.remove(id);
	}

	private void clearData() {
		data.clear();
	}
	
	@Override
	public List<UserModel> list(
		String queryType,
		String query,
		long position,
		long size
	) {
		List<UserModel> _list = getData();
		logger.info("list() -> " + getDataSize() + " values");
		return _list;
	}

	@Override
	public UserModel create(
		UserModel user
	) throws DuplicateException {
		if (getData(user.getId()) != null) {
			throw new DuplicateException();
		}
		setNewID(user);
		storeData(user);
		return user;
	}

	@Override
	public UserModel read(
		String id
	) throws NotFoundException {
		UserModel _dataObj = getData(id);
		if (_dataObj == null) {
			throw new NotFoundException();
		}
		// response.setId(id);
		logger.info("read(" + id + "): " + _dataObj);
		return _dataObj;
	}

	@Override
	public UserModel update(
		String id,
		UserModel user
	) throws NotFoundException {
		if (getData(id) == null) {
			throw new NotFoundException();
		} else {
			user.setId(id);
			storeData(user);
			return user;
		}
	}

	@Override
	public void delete(
		String id
	) throws NotFoundException {
		UserModel user = getData(id);
		if (user == null) {
			throw new NotFoundException();
		}
		removeData(id);
		logger.info("delete(" + id + ")");		
	}

	@Override
	public void deleteAll(
	) {
		clearData();
		logger.info("all data deleted");		
	}

	@Override
	public int count(
	) {
		return getDataSize();
	}
	
}
