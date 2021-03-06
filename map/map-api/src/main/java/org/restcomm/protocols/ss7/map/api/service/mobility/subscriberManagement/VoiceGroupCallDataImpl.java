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
package org.restcomm.protocols.ss7.map.api.service.mobility.subscriberManagement;

import org.restcomm.protocols.ss7.map.api.primitives.MAPExtensionContainerImpl;

import com.mobius.software.telco.protocols.ss7.asn.ASNClass;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNProperty;
import com.mobius.software.telco.protocols.ss7.asn.annotations.ASNTag;

/**
 *
 * @author Lasith Waruna Perera
 *
 */
@ASNTag(asnClass=ASNClass.UNIVERSAL,tag=16,constructed=true,lengthIndefinite=false)
public class VoiceGroupCallDataImpl {
	@ASNProperty(asnClass=ASNClass.UNIVERSAL,tag=4,constructed=false,index=0)
	private GroupIdImpl groupId;
    private MAPExtensionContainerImpl extensionContainer;
    private AdditionalSubscriptionsImpl additionalSubscriptions;
    
    @ASNProperty(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=0,constructed=false,index=-1)
    private AdditionalInfoImpl additionalInfo;
    
    @ASNProperty(asnClass=ASNClass.CONTEXT_SPECIFIC,tag=1,constructed=false,index=-1)
    private LongGroupIdImpl longGroupId;

    public VoiceGroupCallDataImpl() {        
    }

    public VoiceGroupCallDataImpl(GroupIdImpl groupId, MAPExtensionContainerImpl extensionContainer,
            AdditionalSubscriptionsImpl additionalSubscriptions, AdditionalInfoImpl additionalInfo, LongGroupIdImpl longGroupId) {
        this.groupId = groupId;
        this.extensionContainer = extensionContainer;
        this.additionalSubscriptions = additionalSubscriptions;
        this.additionalInfo = additionalInfo;
        this.longGroupId = longGroupId;
        if(this.longGroupId!=null)
        	this.groupId=new GroupIdImpl("");
    }

    public GroupIdImpl getGroupId() {
        return this.groupId;
    }

    public MAPExtensionContainerImpl getExtensionContainer() {
        return this.extensionContainer;
    }

    public AdditionalSubscriptionsImpl getAdditionalSubscriptions() {
        return this.additionalSubscriptions;
    }

    public AdditionalInfoImpl getAdditionalInfo() {
        return this.additionalInfo;
    }

    public LongGroupIdImpl getLongGroupId() {
        return this.longGroupId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VoiceGroupCallData [");

        if (this.groupId != null) {
            sb.append("groupId=");
            sb.append(this.groupId.toString());
            sb.append(", ");
        }

        if (this.extensionContainer != null) {
            sb.append("extensionContainer=");
            sb.append(this.extensionContainer.toString());
            sb.append(", ");
        }

        if (this.additionalSubscriptions != null) {
            sb.append("additionalSubscriptions=");
            sb.append(this.additionalSubscriptions.toString());
            sb.append(", ");
        }

        if (this.additionalInfo != null) {
            sb.append("additionalInfo=");
            sb.append(this.additionalInfo.toString());
            sb.append(", ");
        }

        if (this.longGroupId != null) {
            sb.append("longGroupId=");
            sb.append(this.longGroupId.toString());
            sb.append(", ");
        }

        sb.append("]");

        return sb.toString();
    }

}
