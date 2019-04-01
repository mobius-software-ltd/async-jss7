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

package org.restcomm.protocols.ss7.tcapAnsi.api.asn;

import java.util.List;

import com.mobius.software.telco.protocols.ss7.asn.ASNClass;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNProperty;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNTag;
import com.mobius.software.telco.protocols.ss7.asn.primitives.ASNInteger;
import com.mobius.software.telco.protocols.ss7.asn.primitives.ASNObjectIdentifier;

/**
*
* @author sergey vetyutnev
*
*/
@ASNTag(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=0x02,constructed=false,lengthIndefinite=false)
public class ConfidentialityImpl {
	
	@ASNProperty(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=0x00,constructed=false,index=-1)
	private ASNInteger integerConfidentialityId;
	@ASNProperty(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=0x01,constructed=false,index=-1)
	private ASNObjectIdentifier objectConfidentialityId;

    public Long getIntegerConfidentialityId() {
    	if(integerConfidentialityId==null)
    		return null;
    	
        return integerConfidentialityId.getValue();
    }

    public void setIntegerConfidentialityId(Long val) {
        integerConfidentialityId = new ASNInteger();
        integerConfidentialityId.setValue(val);
        objectConfidentialityId = null;
    }

    public List<Long> getObjectConfidentialityId() {
    	if(objectConfidentialityId==null)
    		return null;
    	
        return objectConfidentialityId.getValue();
    }

    public void setObjectConfidentialityId(List<Long> val) {
        integerConfidentialityId = null;
        objectConfidentialityId = new ASNObjectIdentifier();
        objectConfidentialityId.setValue(val);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Confidentiality[");
        if (this.integerConfidentialityId != null) {
            sb.append("integerConfidentialityId=");
            sb.append(this.integerConfidentialityId.getValue());
        } else if (this.objectConfidentialityId != null) {
            sb.append("objectConfidentialityId=");
            sb.append(objectConfidentialityId.getValue());
        }
        sb.append("]");
        return sb.toString();
    }

}
