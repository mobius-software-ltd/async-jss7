/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.protocols.ss7.cap.api.primitives;

import java.util.List;

import com.mobius.software.telco.protocols.ss7.asn.ASNClass;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNProperty;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNTag;
import com.mobius.software.telco.protocols.ss7.asn.primitives.ASNInteger;
import com.mobius.software.telco.protocols.ss7.asn.primitives.ASNObjectIdentifier;
import com.mobius.software.telco.protocols.ss7.asn.primitives.ASNOctetString;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 *
 * @author sergey vetyutnev
 *
 */
@ASNTag(asnClass = ASNClass.UNIVERSAL,tag = 16,constructed = true,lengthIndefinite = false)
public class ExtensionFieldImpl {

	@ASNProperty(asnClass = ASNClass.UNIVERSAL,tag = 2,constructed = false,index = 0)
    private ASNInteger localCode;    

	@ASNProperty(asnClass = ASNClass.UNIVERSAL,tag = 6,constructed = false,index = 0)
	private ASNObjectIdentifier globalCode;
    
	private ASNCriticalityTypeImpl criticalityType;
    
    @ASNProperty(asnClass = ASNClass.CONTEXT_SPECIFIC,tag = 1,constructed = false,index = -1)
    public ASNOctetString data;

    public ExtensionFieldImpl() {
    }

    public ExtensionFieldImpl(Integer localCode, CriticalityType criticalityType, byte[] data) {
    	if(localCode!=null) {
    		this.localCode = new ASNInteger();
    		this.localCode.setValue(localCode.longValue());
    	}
    	
        if(criticalityType!=null && criticalityType!=CriticalityType.typeIgnore) {
        	this.criticalityType = new ASNCriticalityTypeImpl();
        	this.criticalityType.setType(criticalityType);
        }
        
        if(data!=null) {
        	this.data = new ASNOctetString();
        	this.data.setValue(Unpooled.wrappedBuffer(data));
        }
    }

    public ExtensionFieldImpl(List<Long> globalCode, CriticalityType criticalityType, byte[] data) {
    	if(globalCode!=null) {
    		this.globalCode = new ASNObjectIdentifier();
    		this.globalCode.setValue(globalCode);
    	}
    	
        if(criticalityType!=null) {
        	this.criticalityType = new ASNCriticalityTypeImpl();
        	this.criticalityType.setType(criticalityType);
        }
        
        if(data!=null) {
        	this.data = new ASNOctetString();
        	this.data.setValue(Unpooled.wrappedBuffer(data));
        }
    }

    public Integer getLocalCode() {
    	if(localCode==null)
    		return null;
    	
        return localCode.getValue().intValue();
    }

    public List<Long> getGlobalCode() {
    	if(globalCode==null)
    		return null;
    	
        return globalCode.getValue();
    }

    public CriticalityType getCriticalityType() {
    	if(criticalityType==null)
    		return CriticalityType.typeIgnore;
    	
        return criticalityType.getType();
    }

    public byte[] getData() {
    	if(data==null || data.getValue()==null)
    		return null;
    	
    	ByteBuf buffer=data.getValue();
    	byte[] data=new byte[buffer.readableBytes()];    	
    	buffer.readBytes(data);
    	return data;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("ExtensionField [");
        if (this.localCode != null) {
            sb.append("localCode=");
            sb.append(this.localCode);
        }
        if (this.globalCode != null && this.globalCode.getValue()!=null) {
            sb.append("globalCode=[");
            sb.append(printDataArrLong(this.globalCode.getValue()));
            sb.append("]");
        }
        if (this.criticalityType != null) {
            sb.append(", criticalityType=");
            sb.append(criticalityType);
        }
        if (this.data != null && this.data.getValue()!=null) {
            sb.append(", data=[");
            sb.append(data.printDataArr(getData()));
            sb.append("]");
        }
        sb.append("]");

        return sb.toString();
    }

    private String printDataArrLong(List<Long> arr) {
        StringBuilder sb = new StringBuilder();
        for (Long b : arr) {
            sb.append(b);
            sb.append(", ");
        }

        return sb.toString();
    }
}
