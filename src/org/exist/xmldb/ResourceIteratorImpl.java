 /*
 * eXist Open Source Native XML Database
 * Copyright (C) 2001 Wolfgang M. Meier
 * meier@ifs.tu-darmstadt.de
 * http://exist.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.exist.xmldb;

import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import java.util.Vector;
import java.io.IOException;
import org.apache.xmlrpc.*;

public class ResourceIteratorImpl implements ResourceIterator {

	protected XmlRpcClient rpcClient;
	protected RemoteCollection collection;
	protected Vector resources;
	protected int pos = 0, indentXML;
	protected String encoding = "UTF-8";

	public ResourceIteratorImpl(RemoteCollection col, Vector resources, 
								int indentXML, String encoding) {
		this.resources = resources;
		this.collection = col;
		this.indentXML = indentXML;
		this.encoding = encoding;
	}

	public int getLength() {
		return resources.size();
	}

	public boolean hasMoreResources() throws XMLDBException {
		return pos < resources.size();
	}

    public void setNext(int next) {
        pos = next;
    }
    
	public Resource nextResource() throws XMLDBException {
        if(pos >= resources.size())
            return null;
        // node or value?
        if(resources.elementAt(pos) instanceof Vector) {
            // node
            Vector v = (Vector)resources.elementAt(pos++);
            String doc = (String)v.elementAt(0);
            String s_id = (String)v.elementAt(1);
            
            Vector params = new Vector();
            params.addElement(doc);
            params.addElement(s_id);
            params.addElement(new Integer(indentXML));
            params.addElement(encoding);
            try {
                byte[] data = (byte[])collection.getClient().execute("retrieve", params);
                XMLResource res = new RemoteXMLResource(collection, doc, doc + "_" + s_id);
                res.setContent((Object)new String(data, encoding));
                return res;
            } catch(XmlRpcException xre) {
                throw new XMLDBException(ErrorCodes.INVALID_RESOURCE, xre.getMessage(), xre);
            } catch(IOException ioe) {
                throw new XMLDBException(ErrorCodes.VENDOR_ERROR, ioe.getMessage(), ioe);
            }
        } else {
            // value
            XMLResource res = new RemoteXMLResource(collection, null, Integer.toString(pos));
            res.setContent(resources.elementAt(pos++));
            return res;
        }
    }
}
	
