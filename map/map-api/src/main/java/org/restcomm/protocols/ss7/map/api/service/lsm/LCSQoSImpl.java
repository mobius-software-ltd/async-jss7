/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
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

package org.restcomm.protocols.ss7.map.api.service.lsm;

import org.restcomm.protocols.ss7.map.api.primitives.MAPExtensionContainerImpl;
import org.restcomm.protocols.ss7.map.api.service.supplementary.ASNSingleByte;

import com.mobius.software.telco.protocols.ss7.asn.ASNClass;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNProperty;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNTag;
import com.mobius.software.telco.protocols.ss7.asn.primitives.ASNNull;

/**
 *
 *
 * @author amit bhayani
 *
 */
@ASNTag(asnClass=ASNClass.UNIVERSAL,tag=16,constructed=true,lengthIndefinite=false)
public class LCSQoSImpl {
    @ASNProperty(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=0,constructed=false,index=-1)
    private ASNSingleByte horizontalAccuracy;
    
    @ASNProperty(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=1,constructed=false,index=-1)
    private ASNNull verticalCoordinateRequest;
    
    @ASNProperty(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=2,constructed=false,index=-1)
    private ASNSingleByte verticalAccuracy;
    
    @ASNProperty(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=3,constructed=true,index=-1)
    private ResponseTimeImpl responseTime;
    
    @ASNProperty(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=4,constructed=true,index=-1)
    private MAPExtensionContainerImpl extensionContainer;

    /**
     *
     */
    public LCSQoSImpl() {
        super();
    }

    /**
     * @param horizontalAccuracy
     * @param verticalAccuracy
     * @param verticalCoordinateRequest
     * @param responseTime
     * @param extensionContainer
     */
    public LCSQoSImpl(Integer horizontalAccuracy, Integer verticalAccuracy, boolean verticalCoordinateRequest,
            ResponseTimeImpl responseTime, MAPExtensionContainerImpl extensionContainer) {
        super();
        
        if(horizontalAccuracy!=null) {
        	this.horizontalAccuracy = new ASNSingleByte();
        	this.horizontalAccuracy.setValue(horizontalAccuracy);
        }
        
        if(verticalAccuracy!=null) {
        	this.verticalAccuracy = new ASNSingleByte();
        	this.verticalAccuracy.setValue(verticalAccuracy);
        }
        
        if(verticalCoordinateRequest)
        	this.verticalCoordinateRequest = new ASNNull();
        
        this.responseTime = responseTime;
        this.extensionContainer = extensionContainer;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.map.api.service.lsm.LCSQoS#getHorizontalAccuracy ()
     */
    public Integer getHorizontalAccuracy() {
    	if(this.horizontalAccuracy==null)
    		return null;
    	
        return this.horizontalAccuracy.getValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.map.api.service.lsm.LCSQoS# getVerticalCoordinateRequest()
     */
    public boolean getVerticalCoordinateRequest() {
        return this.verticalCoordinateRequest!=null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.map.api.service.lsm.LCSQoS#getVerticalAccuracy ()
     */
    public Integer getVerticalAccuracy() {
    	if(this.verticalAccuracy==null)
    		return null;
    	
        return this.verticalAccuracy.getValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.map.api.service.lsm.LCSQoS#getResponseTime()
     */
    public ResponseTimeImpl getResponseTime() {
        return this.responseTime;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.map.api.service.lsm.LCSQoS#getExtensionContainer ()
     */
    public MAPExtensionContainerImpl getExtensionContainer() {
        return this.extensionContainer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LCSQoS [");

        if (this.horizontalAccuracy != null) {
            sb.append("horizontalAccuracy=");
            sb.append(this.horizontalAccuracy.getValue());
        }
        if (this.verticalAccuracy != null) {
            sb.append(", verticalAccuracy=");
            sb.append(this.verticalAccuracy.getValue());
        }
        if (this.verticalCoordinateRequest!=null) {
            sb.append(", verticalCoordinateRequest");
        }
        if (this.responseTime != null) {
            sb.append(", responseTime=");
            sb.append(this.responseTime.getResponseTimeCategory());
        }        
        if (this.extensionContainer != null) {
            sb.append(", extensionContainer=");
            sb.append(this.extensionContainer.toString());
        }

        sb.append("]");

        return sb.toString();
    }
}
