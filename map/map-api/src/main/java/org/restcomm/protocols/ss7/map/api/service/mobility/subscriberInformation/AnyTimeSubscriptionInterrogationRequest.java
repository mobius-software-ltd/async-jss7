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

package org.restcomm.protocols.ss7.map.api.service.mobility.subscriberInformation;

import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressStringImpl;
import org.restcomm.protocols.ss7.map.api.primitives.MAPExtensionContainerImpl;
import org.restcomm.protocols.ss7.map.api.primitives.SubscriberIdentityImpl;
import org.restcomm.protocols.ss7.map.api.service.mobility.MobilityMessage;

/**
 *
<code>
MAP V3:

anyTimeSubscriptionInterrogation OPERATION ::= {
  --Timer m
  ARGUMENT AnyTimeSubscriptionInterrogationArg
  RESULT AnyTimeSubscriptionInterrogationRes
  ERRORS { atsi-NotAllowed | dataMissing | unexpectedDataValue | unknownSubscriber | bearerServiceNotProvisioned |
           teleserviceNotProvisioned | callBarred | illegalSS-Operation | ss-NotAvailable | informationNotAvailable}
  CODE local:62
}

AnyTimeSubscriptionInterrogationArg ::= SEQUENCE {
  subscriberIdentity          [0] SubscriberIdentity,
  requestedSubscriptionInfo   [1] RequestedSubscriptionInfo,
  gsmSCF-Address              [2] ISDN-AddressStringImpl,
  extensionContainer          [3] ExtensionContainer OPTIONAL,
  longFTN-Supported           [4] NULL OPTIONAL,
  ...
}
</code>
 *
 *
 * @author sergey vetyutnev
 *
 */
public interface AnyTimeSubscriptionInterrogationRequest extends MobilityMessage {

    SubscriberIdentityImpl getSubscriberIdentity();

    RequestedSubscriptionInfoImpl getRequestedSubscriptionInfo();

    ISDNAddressStringImpl getGsmScfAddress();

    MAPExtensionContainerImpl getExtensionContainer();

    boolean getLongFTNSupported();

}
