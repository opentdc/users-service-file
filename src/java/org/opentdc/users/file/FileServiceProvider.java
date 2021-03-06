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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.opentdc.file.AbstractFileServiceProvider;
import org.opentdc.service.ServiceUtil;
import org.opentdc.service.exception.DuplicateException;
import org.opentdc.service.exception.InternalServerErrorException;
import org.opentdc.service.exception.NotFoundException;
import org.opentdc.service.exception.ValidationException;
import org.opentdc.users.AuthType;
import org.opentdc.users.ServiceProvider;
import org.opentdc.users.UserModel;
import org.opentdc.users.UserQueryHandler;
import org.opentdc.util.PrettyPrinter;

public class FileServiceProvider extends AbstractFileServiceProvider<UserModel> implements ServiceProvider {

	private static Map<String, UserModel> index = null;
	private static final Logger logger = Logger.getLogger(FileServiceProvider.class.getName());

	public FileServiceProvider(
		ServletContext context, 
		String prefix
	) throws IOException {
		super(context, prefix);
		if (index == null) {
			index = new ConcurrentHashMap<String, UserModel>();
			List<UserModel> _users = importJson();
			for (UserModel _user : _users) {
				index.put(_user.getId(), _user);
			}
			logger.info(_users.size() + " Users imported.");
		}
	}
		
	@Override
	public List<UserModel> list(
		String query,
		String queryType,
		long position,
		long size
	) {
		ArrayList<UserModel> _list = new ArrayList<UserModel>(index.values());
		Collections.sort(_list, UserModel.UserComparator);
		UserQueryHandler _queryHandler = new UserQueryHandler(query);

		ArrayList<UserModel> _selection = new ArrayList<UserModel>();
		for (int i = 0; i < _list.size(); i++) {
			if (i >= position && i < (position + size)) {
				if (_queryHandler.evaluate(_list.get(i)) == true) {
					_selection.add(_list.get(i));
				}
			}			
		}
		logger.info("list(<" + query + ">, <" + queryType + 
				">, <" + position + ">, <" + size + ">) -> " + _selection.size() + " users.");
		return _selection;
	}

	@Override
	public UserModel create(
		HttpServletRequest request,
		UserModel user
	) throws DuplicateException, ValidationException {
		logger.info("create(" + PrettyPrinter.prettyPrintAsJSON(user) + ")");
		String _id = user.getId();
		if (_id == null || _id == "") {
			_id = UUID.randomUUID().toString();
		} else {
			if (index.get(_id) != null) {
				// object with same ID exists already
				throw new DuplicateException("user <" + _id + "> exists already.");
			}
			else { 	// a new ID was set on the client; we do not allow this
				throw new ValidationException("user <" + _id + 
					"> contains an ID generated on the client. This is not allowed.");
			}				
		}
		if (user.getLoginId() == null || user.getLoginId().length() == 0) {
			throw new ValidationException("user <" + user.getId() +
					"> must have a valid loginId.");
		}
		if (user.getContactId() == null || user.getContactId().length() == 0) {
			throw new ValidationException("user <" + user.getId() +
					"> must have a valid contactId.");
		}
		// check that the referenced contact exists
		org.opentdc.addressbooks.file.FileServiceProvider.getContactModel(user.getContactId());
		if (user.getAuthType() == null) {
			user.setAuthType(AuthType.getDefault());
		}
		user.setId(_id);
		Date _date = new Date();
		user.setCreatedAt(_date);
		user.setCreatedBy(ServiceUtil.getPrincipal(request));
		user.setModifiedAt(_date);
		user.setModifiedBy(ServiceUtil.getPrincipal(request));
		index.put(_id, user);
		logger.info("create() -> " + PrettyPrinter.prettyPrintAsJSON(user));		
		if (isPersistent) {
			exportJson(index.values());
		}
		return user;
	}

	@Override
	public UserModel read(
			String id) 
		throws NotFoundException {
		UserModel _user = index.get(id);
		if (_user == null) {
			throw new NotFoundException("no user with ID <" + id
					+ "> was found.");
		}
		logger.info("read(" + id + ") -> " + PrettyPrinter.prettyPrintAsJSON(_user));
		return _user;
	}

	@Override
	public UserModel update(
		HttpServletRequest request,
		String id,
		UserModel user
	) throws NotFoundException, ValidationException
	{
		UserModel _userModel = index.get(id);
		if (_userModel == null) {
			throw new NotFoundException("no user with ID <" + id
					+ "> was found.");
		} 
		if (! _userModel.getCreatedAt().equals(user.getCreatedAt())) {
			logger.warning("user <" + id + ">: ignoring createdAt value <" + user.getCreatedAt().toString() +
					"> because it was set on the client.");
		}
		if (! _userModel.getCreatedBy().equalsIgnoreCase(user.getCreatedBy())) {
			logger.warning("user <" + id + ">: ignoring createdBy value <" + user.getCreatedBy() +
					"> because it was set on the client.");		
		}
		if (user.getLoginId() == null || user.getLoginId().length() == 0) {
			throw new ValidationException("updating user <" + id + ">: new data must have a valid loginId.");
		}
		if (user.getContactId() == null || user.getContactId().length() == 0) {
			throw new ValidationException("updating user <" + user.getId() + ">: new data must have a valid contactId.");
		}
		// check that the referenced contact exists
		org.opentdc.addressbooks.file.FileServiceProvider.getContactModel(user.getContactId());
		_userModel.setLoginId(user.getLoginId());
		_userModel.setContactId(user.getContactId());
		_userModel.setHashedPassword(user.getHashedPassword());
		_userModel.setSalt(user.getSalt());
		if (user.getAuthType() == null) {
			_userModel.setAuthType(AuthType.getDefault());
		} else {
			_userModel.setAuthType(user.getAuthType());
		}
		_userModel.setModifiedAt(new Date());
		_userModel.setModifiedBy(ServiceUtil.getPrincipal(request));
		index.put(id, _userModel);
		logger.info("update(" + PrettyPrinter.prettyPrintAsJSON(_userModel) + ")");
		if (isPersistent) {
			exportJson(index.values());
		}
		return _userModel;
	}

	@Override
	public void delete(
			String id) 
		throws NotFoundException, InternalServerErrorException {
		UserModel user = index.get(id);
		if (user == null) {
			throw new NotFoundException("no user with ID <" + id
					+ "> was found.");
		}
		index.remove(id);
		logger.info("delete(" + id + ")");		
		if (isPersistent) {
			exportJson(index.values());
		}
	}
}
