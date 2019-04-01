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

package org.restcomm.protocols.ss7.tcapAnsi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.sccp.parameter.SccpAddress;
import org.restcomm.protocols.ss7.tcapAnsi.api.TCAPException;
import org.restcomm.protocols.ss7.tcapAnsi.api.TCAPSendException;
import org.restcomm.protocols.ss7.tcapAnsi.api.TCAPStack;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.ApplicationContext;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.DialogPortion;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.EncodeException;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.ProtocolVersion;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.UserInformation;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.Component;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.ComponentType;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.Invoke;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.PAbortCause;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.Reject;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.RejectProblem;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.TCAbortMessage;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.TCConversationMessage;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.TCQueryMessage;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.TCResponseMessage;
import org.restcomm.protocols.ss7.tcapAnsi.api.asn.comp.TCUniMessage;
import org.restcomm.protocols.ss7.tcapAnsi.api.tc.component.InvokeClass;
import org.restcomm.protocols.ss7.tcapAnsi.api.tc.component.OperationState;
import org.restcomm.protocols.ss7.tcapAnsi.api.tc.dialog.Dialog;
import org.restcomm.protocols.ss7.tcapAnsi.api.tc.dialog.TRPseudoState;
import org.restcomm.protocols.ss7.tcapAnsi.api.tc.dialog.events.TCConversationRequest;
import org.restcomm.protocols.ss7.tcapAnsi.api.tc.dialog.events.TCQueryRequest;
import org.restcomm.protocols.ss7.tcapAnsi.api.tc.dialog.events.TCResponseRequest;
import org.restcomm.protocols.ss7.tcapAnsi.api.tc.dialog.events.TCUniRequest;
import org.restcomm.protocols.ss7.tcapAnsi.api.tc.dialog.events.TCUserAbortRequest;
import org.restcomm.protocols.ss7.tcapAnsi.asn.InvokeImpl;
import org.restcomm.protocols.ss7.tcapAnsi.asn.ReturnResultLastImpl;
import org.restcomm.protocols.ss7.tcapAnsi.asn.ReturnResultNotLastImpl;
import org.restcomm.protocols.ss7.tcapAnsi.asn.TCAbortMessageImpl;
import org.restcomm.protocols.ss7.tcapAnsi.asn.TCConversationMessageImpl;
import org.restcomm.protocols.ss7.tcapAnsi.asn.TCQueryMessageImpl;
import org.restcomm.protocols.ss7.tcapAnsi.asn.TCResponseMessageImpl;
import org.restcomm.protocols.ss7.tcapAnsi.asn.TCUniMessageImpl;
import org.restcomm.protocols.ss7.tcapAnsi.asn.TcapFactory;
import org.restcomm.protocols.ss7.tcapAnsi.asn.Utils;
import org.restcomm.protocols.ss7.tcapAnsi.tc.dialog.events.DialogPrimitiveFactoryImpl;
import org.restcomm.protocols.ss7.tcapAnsi.tc.dialog.events.TCConversationIndicationImpl;
import org.restcomm.protocols.ss7.tcapAnsi.tc.dialog.events.TCPAbortIndicationImpl;
import org.restcomm.protocols.ss7.tcapAnsi.tc.dialog.events.TCQueryIndicationImpl;
import org.restcomm.protocols.ss7.tcapAnsi.tc.dialog.events.TCResponseIndicationImpl;
import org.restcomm.protocols.ss7.tcapAnsi.tc.dialog.events.TCUniIndicationImpl;
import org.restcomm.protocols.ss7.tcapAnsi.tc.dialog.events.TCUserAbortIndicationImpl;

/**
 * @author baranowb
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public class DialogImpl implements Dialog {

	private static final long serialVersionUID = 1L;

    // timeout of remove task after TC_END
    //private static final int _REMOVE_TIMEOUT = 30000;

	private static final Logger logger = Logger.getLogger(DialogImpl.class);

    private Object userObject;

    // values for timer timeouts
    //private long removeTaskTimeout = _REMOVE_TIMEOUT;
    private long idleTaskTimeout;

    // sent/received acn, holds last acn/ui.
    private ApplicationContext lastACN;
    private UserInformation lastUI; // optional

    private Long localTransactionIdObject;
    private long localTransactionId;
    private byte[] remoteTransactionId;
    private Long remoteTransactionIdObject;

    private ProtocolVersion protocolVersion;
    private SccpAddress localAddress;
    private SccpAddress remoteAddress;
    private int localSsn;

    private AtomicReference<Future<?>> idleTimerFuture=new AtomicReference<Future<?>>();
    private AtomicBoolean idleTimerActionTaken = new AtomicBoolean(false);
    private AtomicBoolean idleTimerInvoked = new AtomicBoolean(false);
    private AtomicReference<TRPseudoState> state = new AtomicReference<TRPseudoState>(TRPseudoState.Idle);
    private boolean structured = true;
    // invokde ID space :)
    private static final boolean _INVOKEID_TAKEN = true;
    private static final boolean _INVOKEID_FREE = false;
    private static final int _INVOKE_TABLE_SHIFT = 128;

    private boolean[] invokeIDTable = new boolean[256];
    private int freeCount = invokeIDTable.length;
    private int lastInvokeIdIndex = _INVOKE_TABLE_SHIFT - 1;

    // only originating side keeps FSM, see: Q.771 - 3.1.5
    protected InvokeImpl[] operationsSent = new InvokeImpl[invokeIDTable.length];
    protected InvokeImpl[] operationsSentA = new InvokeImpl[invokeIDTable.length];
    private ConcurrentHashMap<Long,Long> incomingInvokeList = new ConcurrentHashMap<Long,Long>();
    private ScheduledExecutorService executor;

    // scheduled components list
    private List<Component> scheduledComponentList = new ArrayList<Component>();
    private TCAPProviderImpl provider;

    private int seqControl;

    // If the Dialogue Portion is sent in TCBegin message, the first received
    // Continue message should have the Dialogue Portion too
    private boolean dpSentInBegin = false;

    private boolean previewMode = false;
    protected PreviewDialogData prevewDialogData;
    private int networkId;
    private boolean isSwapTcapIdBytes;

    private static int getIndexFromInvokeId(Long l) {
        int tmp = l.intValue();
        return tmp + _INVOKE_TABLE_SHIFT;
    }

    private static Long getInvokeIdFromIndex(int index) {
        int tmp = index - _INVOKE_TABLE_SHIFT;
        return new Long(tmp);
    }

    /**
     * Creating a Dialog for normal mode
     *
     * @param localAddress
     * @param remoteAddress
     * @param origTransactionId
     * @param structured
     * @param executor
     * @param provider
     * @param seqControl
     * @param previewMode
     */
    protected DialogImpl(SccpAddress localAddress, SccpAddress remoteAddress, Long origTransactionId, boolean structured,
            ScheduledExecutorService executor, TCAPProviderImpl provider, int seqControl, boolean previewMode) {
        super();
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        if (origTransactionId != null) {
            this.localTransactionIdObject = origTransactionId;
            this.localTransactionId = origTransactionId;
        }
        this.executor = executor;
        this.provider = provider;
        this.structured = structured;

        this.seqControl = seqControl;
        this.previewMode = previewMode;

        TCAPStack stack = this.provider.getStack();
        this.idleTaskTimeout = stack.getDialogIdleTimeout();
        this.isSwapTcapIdBytes = stack.getSwapTcapIdBytes();

        // start
        startIdleTimer();
    }

    /**
     * Create a Dialog for previewMode
     *
     * @param dialogId
     * @param localAddress
     * @param remoteAddress
     * @param seqControl
     * @param executor
     * @param provider
     * @param pdd
     * @param sideB
     */
    protected DialogImpl(SccpAddress localAddress, SccpAddress remoteAddress, int seqControl, ScheduledExecutorService executor,
            TCAPProviderImpl provider, PreviewDialogData pdd, boolean sideB) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.localTransactionIdObject = pdd.getDialogId();
        this.localTransactionId = pdd.getDialogId();
        this.executor = executor;
        this.provider = provider;
        this.structured = true;

        this.seqControl = seqControl;
        this.previewMode = true;

        TCAPStack stack = this.provider.getStack();
        this.idleTaskTimeout = stack.getDialogIdleTimeout();

        this.prevewDialogData = pdd;
        this.lastACN = pdd.getLastACN();
        if (sideB) {
            if (pdd.getOperationsSentA() != null)
                this.operationsSent = pdd.getOperationsSentA();
            if (pdd.getOperationsSentB() != null)
                this.operationsSentA = pdd.getOperationsSentB();
        } else {
            if (pdd.getOperationsSentA() != null)
                this.operationsSentA = pdd.getOperationsSentA();
            if (pdd.getOperationsSentB() != null)
                this.operationsSent = pdd.getOperationsSentB();
        }

        for (InvokeImpl invoke : this.operationsSent) {
            if (invoke != null) {
                invoke.setDialog(this);
            }
        }
    }

    public void release() {
        if (!this.previewMode) {
            for (int i = 0; i < this.operationsSent.length; i++) {
                InvokeImpl invokeImpl = this.operationsSent[i];
                if (invokeImpl != null) {
                    invokeImpl.setState(OperationState.Idle);
                    // TODO whether to call operationTimedOut or not is still not clear
                    // operationTimedOut(invokeImpl);
                }
            }
        }

        this.setState(TRPseudoState.Expunged);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#getDialogId()
     */
    public Long getLocalDialogId() {

        return localTransactionIdObject;
    }

    /**
     *
     */
    public Long getRemoteDialogId() {
        if (this.remoteTransactionId != null && this.remoteTransactionIdObject == null) {
            this.remoteTransactionIdObject = Utils.decodeTransactionId(this.remoteTransactionId, this.isSwapTcapIdBytes);
        }

        return this.remoteTransactionIdObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#getNewInvokeId()
     */
    public Long getNewInvokeId() throws TCAPException {
    	if (this.freeCount == 0) {
            throw new TCAPException("No free invokeId");
        }

        int tryCnt = 0;
        while (true) {
            if (++this.lastInvokeIdIndex >= this.invokeIDTable.length)
                this.lastInvokeIdIndex = 0;
            if (this.invokeIDTable[this.lastInvokeIdIndex] == _INVOKEID_FREE) {
                freeCount--;
                this.invokeIDTable[this.lastInvokeIdIndex] = _INVOKEID_TAKEN;
                return getInvokeIdFromIndex(this.lastInvokeIdIndex);
            }
            if (++tryCnt >= 256)
                throw new TCAPException("No free invokeId");
        }
    }

    public int getNetworkId() {
        return networkId;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#cancelInvocation (java.lang.Long)
     */
    public boolean cancelInvocation(Long invokeId) throws TCAPException {
        if (this.previewMode)
            return false;

        int index = getIndexFromInvokeId(invokeId);
        if (index < 0 || index >= this.operationsSent.length) {
            throw new TCAPException("Wrong invoke id passed.");
        }

        // lookup through send buffer.
        for (index = 0; index < this.scheduledComponentList.size(); index++) {
            Component cr = this.scheduledComponentList.get(index);
            if ((cr.getType() == ComponentType.InvokeNotLast || cr.getType() == ComponentType.InvokeLast)
                    && cr.getCorrelationId().equals(invokeId)) {
                this.scheduledComponentList.remove(index);
                ((InvokeImpl) cr).stopTimer();
                ((InvokeImpl) cr).setState(OperationState.Idle);
                return true;
            }
        }

        return false;
    }

    private void freeInvokeId(Long l) {
    	int index = getIndexFromInvokeId(l);
        if (this.invokeIDTable[index] == _INVOKEID_TAKEN)
            this.freeCount++;
        this.invokeIDTable[index] = _INVOKEID_FREE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#getRemoteAddress()
     */
    public SccpAddress getRemoteAddress() {

        return this.remoteAddress;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#getLocalAddress()
     */
    public SccpAddress getLocalAddress() {

        return this.localAddress;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#getSsn()
     */
    @Override
    public int getLocalSsn() {
        return localSsn;
    }

    public void setLocalSsn(int newSsn) {
        localSsn = newSsn;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#isEstabilished()
     */
    public boolean isEstabilished() {
        return this.state.get() == TRPseudoState.Active;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#isStructured()
     */
    public boolean isStructured() {

        return this.structured;
    }

    public void keepAlive() {
    	if (this.previewMode)
            return;

        if (this.idleTimerInvoked.get()) {
            this.idleTimerActionTaken.set(true);
        }
    }

    /**
     * @return the acn
     */
    public ApplicationContext getApplicationContextName() {
        return lastACN;
    }

    /**
     * @return the ui
     */
    public UserInformation getUserInformation() {
        return lastUI;
    }

    /**
     * Adding the new incoming invokeId into incomingInvokeList list
     *
     * @param invokeId
     * @return false: failure - this invokeId already present in the list
     */
    private boolean addIncomingInvokeId(Long invokeId) {
    	Long oldValue=this.incomingInvokeList.putIfAbsent(invokeId, invokeId);
        return (oldValue==null);
    }

    private void removeIncomingInvokeId(Long invokeId) {
    	if(invokeId!=null)
    		this.incomingInvokeList.remove(invokeId);        
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#send(org.mobicents
     * .protocols.ss7.tcap.api.tc.dialog.events.TCBeginRequest)
     */
    public void send(TCQueryRequest event) throws TCAPSendException {

        if (this.previewMode)
            return;

        if (this.state.get()!=TRPseudoState.Idle) {
            throw new TCAPSendException("Can not send Begin in this state: " + this.state);
        }

        if (!this.isStructured()) {
            throw new TCAPSendException("Unstructured dialogs do not use Begin");
        }
        
        this.idleTimerActionTaken.set(true);
        restartIdleTimer();
        TCQueryMessageImpl tcbm = (TCQueryMessageImpl) TcapFactory.createTCQueryMessage();

        // build DP
        tcbm.setDialogTermitationPermission(event.getDialogTermitationPermission());
        tcbm.setOriginatingTransactionId(Utils.encodeTransactionId(this.localTransactionId, this.isSwapTcapIdBytes));

        if (event.getApplicationContextName() != null || event.getConfidentiality() != null
                || event.getSecurityContext() != null || event.getUserInformation() != null) {
            this.dpSentInBegin = true;
            this.lastACN = event.getApplicationContextName();
            if (event.getUserInformation() != null) {
                this.lastUI = event.getUserInformation();
            }

            DialogPortion dp = TcapFactory.createDialogPortion();
            dp.setProtocolVersion(TcapFactory.createProtocolVersionFull());
            dp.setApplicationContext(event.getApplicationContextName());
            dp.setUserInformation(event.getUserInformation());
            dp.setSecurityContext(event.getSecurityContext());
            dp.setConfidentiality(event.getConfidentiality());

            tcbm.setDialogPortion(dp);
        }

        // now comps
        if (this.scheduledComponentList.size() > 0) {
            Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
            this.prepareComponents(componentsToSend);
            tcbm.setComponent(componentsToSend);
        }

        AsnOutputStream aos = new AsnOutputStream();
        try {
            tcbm.encode(aos);
            this.setState(TRPseudoState.InitialSent);
            this.provider.send(aos.toByteArray(), event.getReturnMessageOnError(), this.remoteAddress, this.localAddress,
                    this.seqControl, this.getNetworkId(), this.localSsn);
            this.scheduledComponentList.clear();
        } catch (Throwable e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Failed to send message: ", e);
            }
            throw new TCAPSendException("Failed to send TC-Query message: " + e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#send(org.mobicents
     * .protocols.ss7.tcap.api.tc.dialog.events.TCContinueRequest)
     */
    public void send(TCConversationRequest event) throws TCAPSendException {

        if (this.previewMode)
            return;

        if (!this.isStructured()) {
            throw new TCAPSendException("Unstructured dialogs do not use Continue");
        }
        
        if (this.state.get() == TRPseudoState.InitialReceived) {
            this.idleTimerActionTaken.set(true);
            restartIdleTimer();
            TCConversationMessageImpl tcbm = (TCConversationMessageImpl) TcapFactory.createTCConversationMessage();

            tcbm.setDialogTermitationPermission(event.getDialogTermitationPermission());
            tcbm.setOriginatingTransactionId(Utils.encodeTransactionId(this.localTransactionId, this.isSwapTcapIdBytes));
            tcbm.setDestinationTransactionId(this.remoteTransactionId);
            // local address may change, lets check it;
            if (event.getOriginatingAddress() != null && !event.getOriginatingAddress().equals(this.localAddress)) {
                this.localAddress = event.getOriginatingAddress();
            }

            if (event.getApplicationContextName() != null || event.getConfidentiality() != null
                    || event.getSecurityContext() != null || event.getUserInformation() != null) {
                // set dialog portion
                DialogPortion dp = TcapFactory.createDialogPortion();
                dp.setProtocolVersion(TcapFactory.createProtocolVersionFull());
                dp.setApplicationContext(event.getApplicationContextName());
                dp.setUserInformation(event.getUserInformation());
                dp.setSecurityContext(event.getSecurityContext());
                dp.setConfidentiality(event.getConfidentiality());

                tcbm.setDialogPortion(dp);
            }
            if (this.scheduledComponentList.size() > 0) {
                Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
                this.prepareComponents(componentsToSend);
                tcbm.setComponent(componentsToSend);

            }

            AsnOutputStream aos = new AsnOutputStream();
            try {
                tcbm.encode(aos);
                this.provider.send(aos.toByteArray(), event.getReturnMessageOnError(), this.remoteAddress,
                        this.localAddress, this.seqControl, this.getNetworkId(), this.localSsn);
                this.setState(TRPseudoState.Active);
                this.scheduledComponentList.clear();
            } catch (Exception e) {
                // FIXME: remove freshly added invokes to free invoke ID??
                if (logger.isEnabledFor(Level.ERROR)) {
                    logger.error("Failed to send message: ", e);
                }
                throw new TCAPSendException("Failed to send TC-Continue message: " + e.getMessage(), e);
            }

        } else if (state.get() == TRPseudoState.Active) {
            this.idleTimerActionTaken.set(true);
            restartIdleTimer();
            // in this we ignore acn and passed args(except qos)
            TCConversationMessageImpl tcbm = (TCConversationMessageImpl) TcapFactory.createTCConversationMessage();

            tcbm.setDialogTermitationPermission(event.getDialogTermitationPermission());
            tcbm.setOriginatingTransactionId(Utils.encodeTransactionId(this.localTransactionId, this.isSwapTcapIdBytes));
            tcbm.setDestinationTransactionId(this.remoteTransactionId);
            if (this.scheduledComponentList.size() > 0) {
                Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
                this.prepareComponents(componentsToSend);
                tcbm.setComponent(componentsToSend);

            }

            AsnOutputStream aos = new AsnOutputStream();
            try {
                tcbm.encode(aos);
                this.provider.send(aos.toByteArray(), event.getReturnMessageOnError(), this.remoteAddress,
                        this.localAddress, this.seqControl, this.getNetworkId(), this.localSsn);
                this.scheduledComponentList.clear();
            } catch (Exception e) {
                // FIXME: remove freshly added invokes to free invoke ID??
                if (logger.isEnabledFor(Level.ERROR)) {
                    logger.error("Failed to send message: ", e);
                }
                throw new TCAPSendException("Failed to send TC-Continue message: " + e.getMessage(), e);
            }
        } else {
            throw new TCAPSendException("Wrong state: " + this.state);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#send(org.mobicents
     * .protocols.ss7.tcap.api.tc.dialog.events.TCEndRequest)
     */
    public void send(TCResponseRequest event) throws TCAPSendException {

        if (this.previewMode)
            return;

        if (!this.isStructured()) {
            throw new TCAPSendException("Unstructured dialogs do not use Response");
        }

        TCResponseMessageImpl tcbm = null;

        if (state.get() == TRPseudoState.InitialReceived) {
            // TC-END request primitive issued in response to a TC-BEGIN
            // indication primitive
            this.idleTimerActionTaken.set(true);
            stopIdleTimer();
            tcbm = (TCResponseMessageImpl) TcapFactory.createTCResponseMessage();
            tcbm.setDestinationTransactionId(this.remoteTransactionId);
            // local address may change, lets check it;
            if (event.getOriginatingAddress() != null && !event.getOriginatingAddress().equals(this.localAddress)) {
                this.localAddress = event.getOriginatingAddress();
            }

            if (event.getApplicationContextName() != null || event.getConfidentiality() != null
                    || event.getSecurityContext() != null || event.getUserInformation() != null) {
                // set dialog portion
                DialogPortion dp = TcapFactory.createDialogPortion();
                dp.setProtocolVersion(TcapFactory.createProtocolVersionFull());
                dp.setApplicationContext(event.getApplicationContextName());
                dp.setUserInformation(event.getUserInformation());
                dp.setSecurityContext(event.getSecurityContext());
                dp.setConfidentiality(event.getConfidentiality());

                tcbm.setDialogPortion(dp);
            }
            if (this.scheduledComponentList.size() > 0) {
                Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
                this.prepareComponents(componentsToSend);
                tcbm.setComponent(componentsToSend);
            }

        } else if (state.get() == TRPseudoState.Active) {
            restartIdleTimer();
            tcbm = (TCResponseMessageImpl) TcapFactory.createTCResponseMessage();

            tcbm.setDestinationTransactionId(this.remoteTransactionId);
            if (this.scheduledComponentList.size() > 0) {
                Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
                this.prepareComponents(componentsToSend);
                tcbm.setComponent(componentsToSend);
            }

        } else {
            throw new TCAPSendException(String.format("State is not %s or %s: it is %s", TRPseudoState.Active,
                    TRPseudoState.InitialReceived, this.state));
        }

        AsnOutputStream aos = new AsnOutputStream();
        try {
            tcbm.encode(aos);
            this.provider.send(aos.toByteArray(), event.getReturnMessageOnError(), this.remoteAddress, this.localAddress,
                    this.seqControl, this.getNetworkId(), this.localSsn);

            this.scheduledComponentList.clear();
        } catch (Exception e) {
            // FIXME: remove freshly added invokes to free invoke ID??
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Failed to send message: ", e);
            }
            throw new TCAPSendException("Failed to send TC-Response message: " + e.getMessage(), e);
        } finally {
            // FIXME: is this proper place - should we not release in case
            // of error ?
            release();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#sendUni()
     */
    public void send(TCUniRequest event) throws TCAPSendException {

        if (this.previewMode)
            return;

        if (this.isStructured()) {
            throw new TCAPSendException("Structured dialogs do not use Uni");
        }

        TCUniMessageImpl msg = (TCUniMessageImpl) TcapFactory.createTCUniMessage();

        if (event.getApplicationContextName() != null || event.getConfidentiality() != null
                || event.getSecurityContext() != null || event.getUserInformation() != null) {
            // set dialog portion
            DialogPortion dp = TcapFactory.createDialogPortion();
            dp.setProtocolVersion(TcapFactory.createProtocolVersionFull());
            dp.setApplicationContext(event.getApplicationContextName());
            dp.setUserInformation(event.getUserInformation());
            dp.setSecurityContext(event.getSecurityContext());
            dp.setConfidentiality(event.getConfidentiality());

            msg.setDialogPortion(dp);
        }

        if (this.scheduledComponentList.size() > 0) {
            Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
            this.prepareComponents(componentsToSend);
            msg.setComponent(componentsToSend);

        }

        AsnOutputStream aos = new AsnOutputStream();
        try {
            msg.encode(aos);
            this.provider.send(aos.toByteArray(), event.getReturnMessageOnError(), this.remoteAddress, this.localAddress,
                    this.seqControl, this.getNetworkId(), this.localSsn);
            this.scheduledComponentList.clear();
        } catch (Exception e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Failed to send message: ", e);
            }
            throw new TCAPSendException("Failed to send TC-Uni message: " + e.getMessage(), e);
        } finally {
            release();
        }
    }

    public void send(TCUserAbortRequest event) throws TCAPSendException {

        if (this.previewMode)
            return;

        // is abort allowed in "Active" state ?
        if (!isStructured()) {
            throw new TCAPSendException("Unstructured dialog can not be aborted!");
        }

        if (this.state.get() == TRPseudoState.InitialReceived || this.state.get() == TRPseudoState.Active) {

            TCAbortMessageImpl msg = (TCAbortMessageImpl) TcapFactory.createTCAbortMessage();
            msg.setDestinationTransactionId(this.remoteTransactionId);

            if (event.getApplicationContextName() != null || event.getConfidentiality() != null
                    || event.getSecurityContext() != null || event.getUserInformation() != null) {
                // set dialog portion
                DialogPortion dp = TcapFactory.createDialogPortion();
                dp.setProtocolVersion(TcapFactory.createProtocolVersionFull());
                dp.setApplicationContext(event.getApplicationContextName());
                dp.setUserInformation(event.getUserInformation());
                dp.setSecurityContext(event.getSecurityContext());
                dp.setConfidentiality(event.getConfidentiality());

                msg.setDialogPortion(dp);
            }
            msg.setUserAbortInformation(event.getUserAbortInformation());

            if (state.get() == TRPseudoState.InitialReceived) {
                // local address may change, lets check it
                if (event.getOriginatingAddress() != null && !event.getOriginatingAddress().equals(this.localAddress)) {
                    this.localAddress = event.getOriginatingAddress();
                }
            }

            AsnOutputStream aos = new AsnOutputStream();
            try {
                msg.encode(aos);
                this.provider.send(aos.toByteArray(), event.getReturnMessageOnError(), this.remoteAddress,
                        this.localAddress, this.seqControl, this.getNetworkId(), this.localSsn);

                this.scheduledComponentList.clear();
            } catch (Exception e) {
                // FIXME: remove freshly added invokes to free invoke ID??
                if (logger.isEnabledFor(Level.ERROR)) {
                    e.printStackTrace();
                    logger.error("Failed to send message: ", e);
                }
                throw new TCAPSendException("Failed to send TC-U-Abort message: " + e.getMessage(), e);
            } finally {
                release();
            }
        } else if (this.state.get() == TRPseudoState.InitialSent) {
            release();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#sendComponent(org
     * .mobicents.protocols.ss7.tcap.api.tc.component.ComponentRequest)
     */
    public void sendComponent(Component componentRequest) throws TCAPSendException {

        if (this.previewMode)
            return;

        if (componentRequest.getType() == ComponentType.InvokeNotLast
                || componentRequest.getType() == ComponentType.InvokeLast) {
            InvokeImpl invoke = (InvokeImpl) componentRequest;

            // check if its taken!
            int invokeIndex = getIndexFromInvokeId(invoke.getInvokeId());
            if (this.operationsSent[invokeIndex] != null) {
                throw new TCAPSendException("There is already operation with such invoke id!");
            }

            invoke.setState(OperationState.Pending);
            invoke.setDialog(this);

            // if the Invoke timeout value has not be reset by TCAP-User
            // for this invocation we are setting it to the the TCAP stack
            // default value
            if (invoke.getTimeout() == TCAPStackImpl._EMPTY_INVOKE_TIMEOUT)
                invoke.setTimeout(this.provider.getStack().getInvokeTimeout());
        } else {
            if (componentRequest.getType() != ComponentType.ReturnResultNotLast) {
                // we are sending a response and removing invokeId from
                // incomingInvokeList
                this.removeIncomingInvokeId(componentRequest.getCorrelationId());
            }
        }
        this.scheduledComponentList.add(componentRequest);
    }

    public void processInvokeWithoutAnswer(Long invokeId) {
        if (this.previewMode)
            return;

        this.removeIncomingInvokeId(invokeId);
    }

    private void prepareComponents(Component[] res) {

        int index = 0;
        while (this.scheduledComponentList.size() > index) {
            Component cr = this.scheduledComponentList.get(index);
            if (cr.getType() == ComponentType.InvokeNotLast || cr.getType() == ComponentType.InvokeLast) {
                InvokeImpl in = (InvokeImpl) cr;
                // FIXME: check not null?
                this.operationsSent[getIndexFromInvokeId(in.getInvokeId())] = in;
                in.setState(OperationState.Sent);
            }

            res[index++] = cr;
        }
    }

    public int getMaxUserDataLength() {

        return this.provider.getMaxUserDataLength(remoteAddress, localAddress, this.networkId);
    }

    public int getDataLength(TCQueryRequest event) throws TCAPSendException {

        TCQueryMessageImpl tcbm = (TCQueryMessageImpl) TcapFactory.createTCQueryMessage();
        tcbm.setDialogTermitationPermission(event.getDialogTermitationPermission());
        tcbm.setOriginatingTransactionId(Utils.encodeTransactionId(this.localTransactionId, this.isSwapTcapIdBytes));

        if (event.getApplicationContextName() != null || event.getConfidentiality() != null
                || event.getSecurityContext() != null || event.getUserInformation() != null) {
            DialogPortion dp = TcapFactory.createDialogPortion();
            dp.setProtocolVersion(TcapFactory.createProtocolVersionFull());
            dp.setApplicationContext(event.getApplicationContextName());
            dp.setUserInformation(event.getUserInformation());
            dp.setSecurityContext(event.getSecurityContext());
            dp.setConfidentiality(event.getConfidentiality());

            tcbm.setDialogPortion(dp);
        }

        // now comps
        if (this.scheduledComponentList.size() > 0) {
            Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
            for (int index = 0; index < this.scheduledComponentList.size(); index++) {
                componentsToSend[index] = this.scheduledComponentList.get(index);
            }
            tcbm.setComponent(componentsToSend);
        }

        AsnOutputStream aos = new AsnOutputStream();
        try {
            tcbm.encode(aos);
        } catch (EncodeException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Failed to encode message while length testing: ", e);
            }
            throw new TCAPSendException("Error encoding TCBeginRequest", e);
        }
        return aos.size();
    }

    public int getDataLength(TCConversationRequest event) throws TCAPSendException {

        TCConversationMessageImpl tcbm = (TCConversationMessageImpl) TcapFactory.createTCConversationMessage();
        tcbm.setDialogTermitationPermission(event.getDialogTermitationPermission());
        tcbm.setOriginatingTransactionId(Utils.encodeTransactionId(this.localTransactionId, this.isSwapTcapIdBytes));
        tcbm.setDestinationTransactionId(this.remoteTransactionId);

        if (event.getApplicationContextName() != null || event.getConfidentiality() != null
                || event.getSecurityContext() != null || event.getUserInformation() != null) {
            // set dialog portion
            DialogPortion dp = TcapFactory.createDialogPortion();
            dp.setProtocolVersion(TcapFactory.createProtocolVersionFull());
            dp.setApplicationContext(event.getApplicationContextName());
            dp.setUserInformation(event.getUserInformation());
            dp.setSecurityContext(event.getSecurityContext());
            dp.setConfidentiality(event.getConfidentiality());

            tcbm.setDialogPortion(dp);
        }

        if (this.scheduledComponentList.size() > 0) {
            Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
            for (int index = 0; index < this.scheduledComponentList.size(); index++) {
                componentsToSend[index] = this.scheduledComponentList.get(index);
            }
            tcbm.setComponent(componentsToSend);
        }

        AsnOutputStream aos = new AsnOutputStream();
        try {
            tcbm.encode(aos);
        } catch (Exception e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Failed to encode message while length testing: ", e);
            }
            throw new TCAPSendException("Error encoding TCContinueRequest", e);
        }

        return aos.size();
    }

    public int getDataLength(TCResponseRequest event) throws TCAPSendException {

        // TC-END request primitive issued in response to a TC-BEGIN
        // indication primitive
        TCResponseMessageImpl tcbm = (TCResponseMessageImpl) TcapFactory.createTCResponseMessage();
        tcbm.setDestinationTransactionId(this.remoteTransactionId);

        if (this.scheduledComponentList.size() > 0) {
            Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
            for (int index = 0; index < this.scheduledComponentList.size(); index++) {
                componentsToSend[index] = this.scheduledComponentList.get(index);
            }
            tcbm.setComponent(componentsToSend);
        }

        if (state.get() == TRPseudoState.InitialReceived) {
            if (event.getApplicationContextName() != null || event.getConfidentiality() != null
                    || event.getSecurityContext() != null || event.getUserInformation() != null) {
                // set dialog portion
                DialogPortion dp = TcapFactory.createDialogPortion();
                dp.setProtocolVersion(TcapFactory.createProtocolVersionFull());
                dp.setApplicationContext(event.getApplicationContextName());
                dp.setUserInformation(event.getUserInformation());
                dp.setSecurityContext(event.getSecurityContext());
                dp.setConfidentiality(event.getConfidentiality());

                tcbm.setDialogPortion(dp);
            }
        }

        AsnOutputStream aos = new AsnOutputStream();
        try {
            tcbm.encode(aos);
        } catch (Exception e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Failed to encode message while length testing: ", e);
            }
            throw new TCAPSendException("Error encoding TCEndRequest", e);
        }

        return aos.size();
    }

    public int getDataLength(TCUniRequest event) throws TCAPSendException {

        TCUniMessageImpl msg = (TCUniMessageImpl) TcapFactory.createTCUniMessage();

        if (event.getApplicationContextName() != null || event.getConfidentiality() != null
                || event.getSecurityContext() != null || event.getUserInformation() != null) {
            // set dialog portion
            DialogPortion dp = TcapFactory.createDialogPortion();
            dp.setProtocolVersion(TcapFactory.createProtocolVersionFull());
            dp.setApplicationContext(event.getApplicationContextName());
            dp.setUserInformation(event.getUserInformation());
            dp.setSecurityContext(event.getSecurityContext());
            dp.setConfidentiality(event.getConfidentiality());

            msg.setDialogPortion(dp);
        }

        if (this.scheduledComponentList.size() > 0) {
            Component[] componentsToSend = new Component[this.scheduledComponentList.size()];
            for (int index = 0; index < this.scheduledComponentList.size(); index++) {
                componentsToSend[index] = this.scheduledComponentList.get(index);
            }
            msg.setComponent(componentsToSend);

        }

        AsnOutputStream aos = new AsnOutputStream();
        try {
            msg.encode(aos);
        } catch (Exception e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Failed to encode message while length testing: ", e);
            }
            throw new TCAPSendException("Error encoding TCUniRequest", e);
        }

        return aos.size();
    }

    // /////////////////
    // LOCAL METHODS //
    // /////////////////

    @Override
    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    /**
     * @param remoteTransactionId the remoteTransactionId to set
     */
    void setRemoteTransactionId(byte[] remoteTransactionId) {
        this.remoteTransactionId = remoteTransactionId;
    }

    /**
     * @param localAddress the localAddress to set
     */
    public void setLocalAddress(SccpAddress localAddress) {
        this.localAddress = localAddress;
    }

    /**
     * @param remoteAddress the remoteAddress to set
     */
    public void setRemoteAddress(SccpAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    void processUni(TCUniMessage msg, SccpAddress localAddress, SccpAddress remoteAddress) {

        // TCUniIndicationImpl tcUniIndication = null;
    	try {
            this.setRemoteAddress(remoteAddress);
            this.setLocalAddress(localAddress);

            // no dialog portion!
            // convert to indications
            TCUniIndicationImpl tcUniIndication = (TCUniIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider
                    .getDialogPrimitiveFactory()).createUniIndication(this);

            tcUniIndication.setDestinationAddress(localAddress);
            tcUniIndication.setOriginatingAddress(remoteAddress);
            // now comps
            Component[] comps = msg.getComponent();
            tcUniIndication.setComponents(comps);

            if (msg.getDialogPortion() != null) {
                DialogPortion dp = msg.getDialogPortion();
                this.lastACN = dp.getApplicationContext();
                this.lastUI = dp.getUserInformation();
                tcUniIndication.setApplicationContextName(this.lastACN);
                tcUniIndication.setUserInformation(this.lastUI);
                tcUniIndication.setConfidentiality(msg.getDialogPortion().getConfidentiality());
                tcUniIndication.setSecurityContext(msg.getDialogPortion().getSecurityContext());
            }

            // lets deliver to provider, this MUST not throw anything
            this.provider.deliver(this, tcUniIndication);

        } finally {
            this.release();
        }
    }

    protected void processQuery(TCQueryMessage msg, SccpAddress localAddress, SccpAddress remoteAddress,
            boolean dialogTermitationPermission) {

        TCQueryIndicationImpl tcBeginIndication = null;
        if (!this.previewMode) {
            // this is invoked ONLY for server.
            if (state.get() != TRPseudoState.Idle) {
                // should we terminate dialog here?
                if (logger.isEnabledFor(Level.ERROR)) {
                    logger.error("Received Begin primitive, but state is not: " + TRPseudoState.Idle + ". Dialog: " + this);
                }
                this.sendAbnormalDialog();
                return;
            }
            restartIdleTimer();
        }

        // lets setup
        this.setRemoteAddress(remoteAddress);
        this.setLocalAddress(localAddress);
        this.setRemoteTransactionId(msg.getOriginatingTransactionId());
        // convert to indications
        tcBeginIndication = (TCQueryIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider.getDialogPrimitiveFactory())
                .createQueryIndication(this, dialogTermitationPermission);

        tcBeginIndication.setDestinationAddress(localAddress);
        tcBeginIndication.setOriginatingAddress(remoteAddress);

        // if APDU and context data present, lets store it
        DialogPortion dialogPortion = msg.getDialogPortion();

        if (dialogPortion != null) {
            this.lastACN = dialogPortion.getApplicationContext();
            this.lastUI = dialogPortion.getUserInformation();
            tcBeginIndication.setApplicationContextName(this.lastACN);
            tcBeginIndication.setUserInformation(this.lastUI);
            tcBeginIndication.setConfidentiality(msg.getDialogPortion().getConfidentiality());
            tcBeginIndication.setSecurityContext(msg.getDialogPortion().getSecurityContext());
        }

        tcBeginIndication.setComponents(processOperationsState(msg.getComponent()));
        if (!this.previewMode) {
            // change state - before we deliver
            this.setState(TRPseudoState.InitialReceived);
        }
        // lets deliver to provider
        this.provider.deliver(this, tcBeginIndication);
    }

    protected void processConversation(TCConversationMessage msg, SccpAddress localAddress, SccpAddress remoteAddressm,
            boolean dialogTermitationPermission) {

        TCConversationIndicationImpl tcContinueIndication = null;
        if (this.previewMode) {
            tcContinueIndication = (TCConversationIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider
                    .getDialogPrimitiveFactory()).createConversationIndication(this, dialogTermitationPermission);
            this.setRemoteTransactionId(msg.getOriginatingTransactionId());

            // here we will receive DialogResponse APDU - if request was
            // present!
            DialogPortion dialogPortion = msg.getDialogPortion();
            if (dialogPortion != null) {
                this.lastACN = dialogPortion.getApplicationContext();
                this.lastUI = dialogPortion.getUserInformation();
                tcContinueIndication.setApplicationContextName(this.lastACN);
                tcContinueIndication.setUserInformation(this.lastUI);
                tcContinueIndication.setConfidentiality(msg.getDialogPortion().getConfidentiality());
                tcContinueIndication.setSecurityContext(msg.getDialogPortion().getSecurityContext());
            }
            tcContinueIndication.setOriginatingAddress(remoteAddress);
            // now comps
            tcContinueIndication.setComponents(processOperationsState(msg.getComponent()));

            // lets deliver to provider
            this.provider.deliver(this, tcContinueIndication);
        } else {

            if (state.get() == TRPseudoState.InitialSent) {
                restartIdleTimer();
                tcContinueIndication = (TCConversationIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider
                        .getDialogPrimitiveFactory()).createConversationIndication(this, dialogTermitationPermission);
                // in continue remote address MAY change be changed, so lets
                // update!
                this.setRemoteAddress(remoteAddress);
                this.setRemoteTransactionId(msg.getOriginatingTransactionId());
                tcContinueIndication.setOriginatingAddress(remoteAddress);

                // here we will receive DialogResponse APDU - if request was
                // present!
                DialogPortion dialogPortion = msg.getDialogPortion();
                if (dialogPortion != null) {
                    this.lastACN = dialogPortion.getApplicationContext();
                    this.lastUI = dialogPortion.getUserInformation();
                    tcContinueIndication.setApplicationContextName(this.lastACN);
                    tcContinueIndication.setUserInformation(this.lastUI);
                    tcContinueIndication.setConfidentiality(msg.getDialogPortion().getConfidentiality());
                    tcContinueIndication.setSecurityContext(msg.getDialogPortion().getSecurityContext());
                } else if (this.dpSentInBegin) {
                    sendAbnormalDialog();
                    return;

                }
                tcContinueIndication.setOriginatingAddress(remoteAddress);
                // now comps
                tcContinueIndication.setComponents(processOperationsState(msg.getComponent()));
                // change state
                this.setState(TRPseudoState.Active);

                // lets deliver to provider
                this.provider.deliver(this, tcContinueIndication);

            } else if (state.get() == TRPseudoState.Active) {
                restartIdleTimer();
                // XXX: here NO APDU will be present, hence, no ACN/UI change
                tcContinueIndication = (TCConversationIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider
                        .getDialogPrimitiveFactory()).createConversationIndication(this, dialogTermitationPermission);

                tcContinueIndication.setOriginatingAddress(remoteAddress);

                // now comps
                tcContinueIndication.setComponents(processOperationsState(msg.getComponent()));

                // lets deliver to provider
                this.provider.deliver(this, tcContinueIndication);

            } else {
                if (logger.isEnabledFor(Level.ERROR)) {
                    logger.error(
                            "Received Continue primitive, but state is not proper: " + this.state + ", Dialog: " + this);
                }
                this.sendAbnormalDialog();
                return;
            }
        }
    }

    protected void processResponse(TCResponseMessage msg, SccpAddress localAddress, SccpAddress remoteAddress) {
        TCResponseIndicationImpl tcEndIndication = null;
        try {
            if (this.previewMode) {
                tcEndIndication = (TCResponseIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider
                        .getDialogPrimitiveFactory()).createResponseIndication(this);

                DialogPortion dialogPortion = msg.getDialogPortion();
                if (dialogPortion != null) {
                    this.lastACN = dialogPortion.getApplicationContext();
                    this.lastUI = dialogPortion.getUserInformation();
                    tcEndIndication.setApplicationContextName(this.lastACN);
                    tcEndIndication.setUserInformation(this.lastUI);
                    tcEndIndication.setConfidentiality(msg.getDialogPortion().getConfidentiality());
                    tcEndIndication.setSecurityContext(msg.getDialogPortion().getSecurityContext());
                }
                // now comps
                tcEndIndication.setComponents(processOperationsState(msg.getComponent()));

                this.provider.deliver(this, tcEndIndication);
            } else {
                restartIdleTimer();
                tcEndIndication = (TCResponseIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider
                        .getDialogPrimitiveFactory()).createResponseIndication(this);

                if (state.get() == TRPseudoState.InitialSent) {
                    // in end remote address MAY change be changed, so lets
                    // update!
                    this.setRemoteAddress(remoteAddress);
                    tcEndIndication.setOriginatingAddress(remoteAddress);
                }

                DialogPortion dialogPortion = msg.getDialogPortion();
                if (dialogPortion != null) {
                    this.lastACN = dialogPortion.getApplicationContext();
                    this.lastUI = dialogPortion.getUserInformation();
                    tcEndIndication.setApplicationContextName(this.lastACN);
                    tcEndIndication.setUserInformation(this.lastUI);
                    tcEndIndication.setConfidentiality(msg.getDialogPortion().getConfidentiality());
                    tcEndIndication.setSecurityContext(msg.getDialogPortion().getSecurityContext());
                }
                // now comps
                tcEndIndication.setComponents(processOperationsState(msg.getComponent()));

                this.provider.deliver(this, tcEndIndication);
            }
        } finally {
            release();
        }
    }

    protected void processAbort(TCAbortMessage msg, SccpAddress localAddress2, SccpAddress remoteAddress2) {
    	try {

            PAbortCause type = msg.getPAbortCause();
            if (type != null) {

                // its TC-P-Abort
                TCPAbortIndicationImpl tcAbortIndication = (TCPAbortIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider
                        .getDialogPrimitiveFactory()).createPAbortIndication(this);
                tcAbortIndication.setPAbortCause(type);

                this.provider.deliver(this, tcAbortIndication);

            } else {
                // its TC-U-Abort
                TCUserAbortIndicationImpl tcUAbortIndication = (TCUserAbortIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider
                        .getDialogPrimitiveFactory()).createUAbortIndication(this);
                DialogPortion dp = msg.getDialogPortion();
                if (dp != null) {
                    tcUAbortIndication.setApplicationContextName(dp.getApplicationContext());
                    tcUAbortIndication.setUserInformation(dp.getUserInformation());
                    tcUAbortIndication.setSecurityContext(dp.getSecurityContext());
                    tcUAbortIndication.setConfidentiality(dp.getConfidentiality());
                }
                tcUAbortIndication.setUserAbortInformation(msg.getUserAbortInformation());

                if (state.get() == TRPseudoState.InitialSent) {
                    // in userAbort remote address MAY change be changed, so lets
                    // update!
                    this.setRemoteAddress(remoteAddress);
                    tcUAbortIndication.setOriginatingAddress(remoteAddress);
                }

                this.provider.deliver(this, tcUAbortIndication);
            }
        } finally {
            release();
        }
    }

    protected void sendAbnormalDialog() {

        if (this.previewMode)
            return;

        TCPAbortIndicationImpl tcAbortIndication = null;
        try {
            if (this.remoteTransactionId == null) {
                // no remoteTransactionId - we can not send back TC-ABORT
                return;
            }

            // sending to the remote side
            if (this.getProtocolVersion() != null) {
                this.provider.sendProviderAbort(PAbortCause.InconsistentDialoguePortion, remoteTransactionId, remoteAddress,
                        localAddress, seqControl, this.getNetworkId());
            } else {
                this.provider.sendRejectAsProviderAbort(PAbortCause.InconsistentDialoguePortion, remoteTransactionId,
                        remoteAddress, localAddress, seqControl, this.getNetworkId());
            }

            // sending to the local side
            tcAbortIndication = (TCPAbortIndicationImpl) ((DialogPrimitiveFactoryImpl) this.provider
                    .getDialogPrimitiveFactory()).createPAbortIndication(this);
            tcAbortIndication.setDialog(this);
            tcAbortIndication.setPAbortCause(PAbortCause.InconsistentDialoguePortion);

            this.provider.deliver(this, tcAbortIndication);
        } finally {
            this.release();
        }
    }

    protected Component[] processOperationsState(Component[] components) {
        if (components == null) {
            return null;
        }

        List<Component> resultingIndications = new ArrayList<Component>();
        for (Component ci : components) {
            Long invokeId;
            invokeId = ci.getCorrelationId();
            InvokeImpl invoke = null;
            int index = 0;
            if (invokeId != null) {
                index = getIndexFromInvokeId(invokeId);
                invoke = this.operationsSent[index];
            }

            switch (ci.getType()) {

                case InvokeNotLast:
                case InvokeLast:
                    if (invokeId != null && invoke == null) {
                        logger.error(String.format("Rx : %s but no sent Invoke for correlationId exists", ci));
                        this.addReject(resultingIndications, ((InvokeImpl) ci).getInvokeId(),
                                RejectProblem.invokeUnrecognisedCorrelationId);
                    } else {
                        if (invoke != null) {
                            ((InvokeImpl) ci).setCorrelationInvoke(invoke);
                        }

                        if (this.previewMode) {
                            resultingIndications.add(ci);
                            index = getIndexFromInvokeId(((Invoke) ci).getInvokeId());
                            this.operationsSentA[index] = (InvokeImpl) ci;
                            ((InvokeImpl) ci).setDialog(this);
                            ((InvokeImpl) ci).setState(OperationState.Sent);
                        } else {
                            if (!this.addIncomingInvokeId(((InvokeImpl) ci).getInvokeId())) {
                                logger.error(String.format("Rx : %s but there is already Invoke with this invokeId", ci));
                                this.addReject(resultingIndications, ((InvokeImpl) ci).getInvokeId(),
                                        RejectProblem.invokeDuplicateInvocation);
                            } else {
                                resultingIndications.add(ci);
                            }
                        }
                    }
                    break;

                case ReturnResultNotLast:

                    if (invoke == null) {
                        logger.error(String.format("Rx : %s but there is no corresponding Invoke", ci));
                        this.addReject(resultingIndications, ci.getCorrelationId(),
                                RejectProblem.returnResultUnrecognisedCorrelationId);
                    } else if (invoke.getInvokeClass() != InvokeClass.Class1 && invoke.getInvokeClass() != InvokeClass.Class3) {
                        logger.error(String.format("Rx : %s but Invoke class is not 1 or 3", ci));
                        this.addReject(resultingIndications, ci.getCorrelationId(),
                                RejectProblem.returnResultUnexpectedReturnResult);
                    } else {
                        resultingIndications.add(ci);
                        ReturnResultNotLastImpl rri = (ReturnResultNotLastImpl) ci;
                        if (rri.getOperationCode() == null)
                            rri.setOperationCode(invoke.getOperationCode());
                    }
                    break;

                case ReturnResultLast:

                    if (invoke == null) {
                        logger.error(String.format("Rx : %s but there is no corresponding Invoke", ci));
                        this.addReject(resultingIndications, ci.getCorrelationId(),
                                RejectProblem.returnResultUnrecognisedCorrelationId);
                    } else if (invoke.getInvokeClass() != InvokeClass.Class1 && invoke.getInvokeClass() != InvokeClass.Class3) {
                        logger.error(String.format("Rx : %s but Invoke class is not 1 or 3", ci));
                        this.addReject(resultingIndications, ci.getCorrelationId(),
                                RejectProblem.returnResultUnexpectedReturnResult);
                    } else {
                        invoke.onReturnResultLast();
                        if (invoke.isSuccessReported()) {
                            resultingIndications.add(ci);
                        }
                        ReturnResultLastImpl rri = (ReturnResultLastImpl) ci;
                        if (rri.getOperationCode() == null)
                            rri.setOperationCode(invoke.getOperationCode());
                    }
                    break;

                case ReturnError:
                    if (invoke == null) {
                        logger.error(String.format("Rx : %s but there is no corresponding Invoke", ci));
                        this.addReject(resultingIndications, ci.getCorrelationId(),
                                RejectProblem.returnErrorUnrecognisedCorrelationId);
                    } else if (invoke.getInvokeClass() != InvokeClass.Class1 && invoke.getInvokeClass() != InvokeClass.Class2) {
                        logger.error(String.format("Rx : %s but Invoke class is not 1 or 2", ci));
                        this.addReject(resultingIndications, ci.getCorrelationId(), RejectProblem.returnErrorUnexpectedError);
                    } else {
                        invoke.onError();
                        if (invoke.isErrorReported()) {
                            resultingIndications.add(ci);
                        }
                    }
                    break;

                case Reject:
                    Reject rej = (Reject) ci;
                    if (invoke != null) {
                        // If the Reject Problem is the InvokeProblemType we
                        // should move the invoke to the idle state
                        RejectProblem problem = rej.getProblem();
                        if (!rej.isLocalOriginated() && (problem == RejectProblem.invokeDuplicateInvocation
                                || problem == RejectProblem.invokeIncorrectParameter
                                || problem == RejectProblem.invokeUnrecognisedCorrelationId
                                || problem == RejectProblem.invokeUnrecognisedOperation))
                            invoke.onReject();
                    }
                    if (rej.isLocalOriginated() && this.isStructured()) {
                        try {
                            // this is a local originated Reject - we are rejecting
                            // an incoming component
                            // we need to send a Reject also to a peer
                            this.sendComponent(rej);
                        } catch (TCAPSendException e) {
                            logger.error("TCAPSendException when sending Reject component : Dialog: " + this, e);
                        }
                    }
                    resultingIndications.add(ci);
                    break;

                default:
                    resultingIndications.add(ci);
                    break;
            }

        }

        components = new Component[resultingIndications.size()];
        components = resultingIndications.toArray(components);

        return components;

    }

    private void addReject(List<Component> resultingIndications, Long invokeId, RejectProblem p) {
        try {
            Reject rej = TcapFactory.createComponentReject();
            rej.setLocalOriginated(true);
            rej.setCorrelationId(invokeId);
            rej.setProblem(p);

            resultingIndications.add(rej);

            if (this.isStructured())
                this.sendComponent(rej);
        } catch (TCAPSendException e) {
            logger.error(String.format("Error sending Reject component", e));
        }
    }

    protected void setState(TRPseudoState newState) {
    	if(this.state.get()==TRPseudoState.Expunged)
    		return;
    	
    	if(this.state.getAndSet(newState)==TRPseudoState.Expunged)
    		this.state.set(TRPseudoState.Expunged);
    	
        if (newState == TRPseudoState.Expunged) {
            stopIdleTimer();
            provider.release(this);
        }
    }

    private void startIdleTimer() {
        if (!this.structured)
            return;
        if (this.previewMode)
            return;

        IdleTimerTask t = new IdleTimerTask();
        t.d = this;
        FutureTask<Void> dummyTask=new FutureTask<Void>(t,null);
        
        if (!this.idleTimerFuture.compareAndSet(null, dummyTask)) {
            throw new IllegalStateException();
        }

        this.idleTimerFuture.set(this.executor.schedule(t, this.idleTaskTimeout, TimeUnit.MILLISECONDS));
    }

    private void stopIdleTimer() {
        if (!this.structured)
            return;

        Future<?> original=this.idleTimerFuture.getAndSet(null);
        if (original != null) {
        	original.cancel(false);                
        }
    }

    private void restartIdleTimer() {
        stopIdleTimer();
        startIdleTimer();
    }

    private class IdleTimerTask implements Runnable {
        DialogImpl d;

        public void run() {
        	d.idleTimerFuture.set(null);

        	d.idleTimerActionTaken.set(false);
            d.idleTimerInvoked.set(true);
            provider.timeout(d);
            // send abort
            if (d.idleTimerActionTaken.get()) {
                startIdleTimer();
            } else {
            	if (remoteTransactionId != null) {
            		sendAbnormalDialog();
            	}
                else
                    release();
            }
            
            d.idleTimerInvoked.set(false);            
        }

    }

    // ////////////////////
    // IND like methods //
    // ///////////////////
    public void operationEnded(InvokeImpl tcInvokeRequestImpl) {
    	// this op died cause of timeout, TC-L-CANCEL!
        int index = getIndexFromInvokeId(tcInvokeRequestImpl.getInvokeId());
        freeInvokeId(tcInvokeRequestImpl.getInvokeId());
        this.operationsSent[index] = null;
        // lets call listener
        // This is done actually with COmponentIndication ....
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restcomm.protocols.ss7.tcap.api.tc.dialog.Dialog#operationEnded(
     * org.restcomm.protocols.ss7.tcap.tc.component.TCInvokeRequestImpl)
     */
    public void operationTimedOut(InvokeImpl invoke) {
        // this op died cause of timeout, TC-L-CANCEL!
    	int index = getIndexFromInvokeId(invoke.getInvokeId());
        freeInvokeId(invoke.getInvokeId());
        this.operationsSent[index] = null;
        // lets call listener
        this.provider.operationTimedOut(invoke);
    }

    // TC-TIMER-RESET
    public void resetTimer(Long invokeId) throws TCAPException {
    	int index = getIndexFromInvokeId(invokeId);
        InvokeImpl invoke = operationsSent[index];
        if (invoke == null) {
            throw new TCAPException("No operation with this ID");
        }
        invoke.startTimer();
    }

    public TRPseudoState getState() {
        return this.state.get();
    }

    public Object getUserObject() {
        return this.userObject;
    }

    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    public boolean getPreviewMode() {
        return this.previewMode;
    }

    public PreviewDialogData getPrevewDialogData() {
        return this.prevewDialogData;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return super.toString() + ": Local[" + this.getLocalDialogId() + "] Remote[" + this.getRemoteDialogId()
                + "], LocalAddress[" + localAddress + "] RemoteAddress[" + this.remoteAddress + "]";
    }
}
