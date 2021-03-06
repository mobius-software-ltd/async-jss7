package org.restcomm.protocols.ss7.tcap;

import io.netty.buffer.ByteBuf;

import org.restcomm.protocols.ss7.sccp.RemoteSccpStatus;
import org.restcomm.protocols.ss7.sccp.SccpConnection;
import org.restcomm.protocols.ss7.sccp.SccpListener;
import org.restcomm.protocols.ss7.sccp.SignallingPointStatus;
import org.restcomm.protocols.ss7.sccp.message.SccpDataMessage;
import org.restcomm.protocols.ss7.sccp.message.SccpNoticeMessage;
import org.restcomm.protocols.ss7.sccp.parameter.Credit;
import org.restcomm.protocols.ss7.sccp.parameter.ErrorCause;
import org.restcomm.protocols.ss7.sccp.parameter.Importance;
import org.restcomm.protocols.ss7.sccp.parameter.ProtocolClass;
import org.restcomm.protocols.ss7.sccp.parameter.RefusalCause;
import org.restcomm.protocols.ss7.sccp.parameter.ReleaseCause;
import org.restcomm.protocols.ss7.sccp.parameter.ResetCause;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;

public class TestSccpListener implements SccpListener {

	private static final long serialVersionUID = 1L;

	private boolean congestedStatusReceived = false;
	
	@Override
	public void onMessage(SccpDataMessage message) {
		
	}

	@Override
	public void onNotice(SccpNoticeMessage message) {
		
	}

	@Override
	public void onCoordResponse(int ssn, int multiplicityIndicator) {
		
	}

	@Override
	public void onState(int dpc, int ssn, boolean inService,
			int multiplicityIndicator) {
		
	}

	@Override
	public void onPcState(int dpc, SignallingPointStatus status, Integer restrictedImportanceLevel, RemoteSccpStatus remoteSccpStatus) {
		if(status.equals(SignallingPointStatus.CONGESTED))
		    congestedStatusReceived = true;
	}

	public boolean isCongestedStatusReceived() {
		return congestedStatusReceived;
	}

    @Override
    public void onConnectIndication(SccpConnection conn, SccpAddress calledAddress, SccpAddress callingAddress,
            ProtocolClass clazz, Credit credit, ByteBuf data, Importance importance) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onConnectConfirm(SccpConnection conn, ByteBuf data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDisconnectIndication(SccpConnection conn, ReleaseCause reason, ByteBuf data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDisconnectIndication(SccpConnection conn, RefusalCause reason, ByteBuf data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDisconnectIndication(SccpConnection conn, ErrorCause errorCause) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onResetIndication(SccpConnection conn, ResetCause reason) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onResetConfirm(SccpConnection conn) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onData(SccpConnection conn, ByteBuf data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDisconnectConfirm(SccpConnection conn) {
        // TODO Auto-generated method stub
        
    }

}
