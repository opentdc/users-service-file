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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.opentdc.file.AbstractFileServiceProvider;
import org.opentdc.service.exception.DuplicateException;
import org.opentdc.service.exception.InternalServerErrorException;
import org.opentdc.service.exception.NotAllowedException;
import org.opentdc.service.exception.NotFoundException;
import org.opentdc.service.exception.ValidationException;
import org.opentdc.users.ServiceProvider;
import org.opentdc.users.UserModel;
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
			index = new HashMap<String, UserModel>();
			List<UserModel> _users = importJson();
			for (UserModel _user : _users) {
				index.put(_user.getId(), _user);
			}
			logger.info(_users.size() + " Users imported.");
		}
	}
		
	@Override
	public List<UserModel> list(
		String queryType,
		String query,
		long position,
		long size
	) {
		// Collections.sort(index, UserModel.UserComparator);
		logger.info("list() -> " + index.size() + " values");
		return new ArrayList<UserModel>(index.values());
	}

	@Override
	public UserModel create(
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
		user.setId(_id);
		Date _date = new Date();
		user.setCreatedAt(_date);
		user.setCreatedBy("DUMMY_USER");
		user.setModifiedAt(_date);
		user.setModifiedBy("DUMMY_USER");
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
		String id,
		UserModel user
	) throws NotFoundException, NotAllowedException
	{
		UserModel _um = index.get(id);
		if (_um == null) {
			throw new NotFoundException("no user with ID <" + id
					+ "> was found.");
		} 
		if (! _um.getCreatedAt().equals(user.getCreatedAt())) {
			throw new NotAllowedException("user <" + id + ">: it is not allowed to change createdAt on the client.");
		}
		if (! _um.getCreatedBy().equalsIgnoreCase(user.getCreatedBy())) {
			throw new NotAllowedException("user <" + id + ">: it is not allowed to change createdBy on the client.");		
		}
		_um.setLoginId(user.getLoginId());
		_um.setContactId(user.getContactId());
		_um.setHashedPassword(user.getHashedPassword());
		_um.setSalt(user.getSalt());
		_um.setModifiedAt(new Date());
		_um.setModifiedBy("DUMMY_USER");
		index.put(id, _um);
		logger.info("update(" + PrettyPrinter.prettyPrintAsJSON(_um) + ")");
		if (isPersistent) {
			exportJson(index.values());
		}
		return _um;
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
