/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012.
 * and individual contributors
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
package org.restcomm.protocols.ss7.cap.api.gap;

import org.restcomm.protocols.ss7.cap.api.isup.DigitsImpl;

import com.mobius.software.telco.protocols.ss7.asn.ASNClass;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNProperty;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNTag;
import com.mobius.software.telco.protocols.ss7.asn.primitives.ASNInteger;

/**
 *
 * @author <a href="mailto:bartosz.krok@pro-ids.com"> Bartosz Krok (ProIDS sp. z o.o.)</a>
 */
@ASNTag(asnClass = ASNClass.UNIVERSAL,tag = 16,constructed = true,lengthIndefinite = false)
public class CallingAddressAndServiceImpl {
	@ASNProperty(asnClass = ASNClass.CONTEXT_SPECIFIC,tag = 0,constructed = false,index = -1)
    private DigitsImpl callingAddressValue;
    
    @ASNProperty(asnClass = ASNClass.CONTEXT_SPECIFIC,tag = 1,constructed = false,index = -1)
    private ASNInteger serviceKey;

    public CallingAddressAndServiceImpl() {
    }

    public CallingAddressAndServiceImpl(DigitsImpl callingAddressValue, int serviceKey) {
        this.callingAddressValue = callingAddressValue;
        this.serviceKey = new ASNInteger();
        this.serviceKey.setValue(Long.valueOf(serviceKey));
    }

    public DigitsImpl getCallingAddressValue() {
    	if(callingAddressValue!=null)
    		callingAddressValue.setIsGenericNumber();
    	
        return callingAddressValue;
    }

    public int getServiceKey() {
    	if(serviceKey==null || serviceKey.getValue()==null)
    		return 0;
    	
        return serviceKey.getValue().intValue();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CallingAddressAndService [");

        if (callingAddressValue != null) {
            sb.append("callingAddressValue=");
            sb.append(callingAddressValue.toString());
        }

        sb.append(", serviceKey=");
        sb.append(serviceKey);

        sb.append("]");

        return sb.toString();
    }

}