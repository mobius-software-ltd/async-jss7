package org.restcomm.protocols.ss7.cap.api.primitives;

import com.mobius.software.telco.protocols.ss7.asn.primitives.ASNEnumerated;

public class ASNTimerIDImpl extends ASNEnumerated {
	public void setType(TimerID t) {
		super.setValue(Long.valueOf(t.getCode()));
	}
	
	public TimerID getType() {
		Long realValue=super.getValue();
		if(realValue==null)
			return null;
		
		return TimerID.getInstance(getValue().intValue());
	}
}
