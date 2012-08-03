package org.mobicents.protocols.ss7.map.functional;

/**
 * 
 * @author amit bhayani
 *
 */
public enum EventType {
	// Dialog EventType
	DialogDelimiter, DialogEricssonRequest, DialogRequest, DialogAccept, DialogReject, DialogUserAbort, DialogProviderAbort, DialogClose, DialogNotice, DialogRelease, DialogTimeout,

	//Service EventType
	ErrorComponent, ProviderErrorComponent, RejectComponent, InvokeTimeout, 
	
	// Supplementary EventType
	ProcessUnstructuredSSRequestIndication, ProcessUnstructuredSSResponseIndication, UnstructuredSSRequestIndication, UnstructuredSSResponseIndication, UnstructuredSSNotifyRequestIndication,

	// SMS EventType
	ForwardShortMessageIndication, ForwardShortMessageRespIndication, MoForwardShortMessageIndication, MoForwardShortMessageRespIndication, MtForwardShortMessageIndication, MtForwardShortMessageRespIndication, SendRoutingInfoForSMIndication, SendRoutingInfoForSMRespIndication, ReportSMDeliveryStatusIndication, ReportSMDeliveryStatusRespIndication, InformServiceCentreIndication, AlertServiceCentreIndication, AlertServiceCentreRespIndication,

	// mobility.authentication EventType
	SendAuthenticationInfo_V3, SendAuthenticationInfo_V2, SendAuthenticationInfoResp_V2, SendAuthenticationInfoResp_V3,

	// mobility.locationManagement EventType
	UpdateLocation, UpdateLocationResp,
	
	// mobility.subscriberInformation EventType
	AnyTimeInterrogation, AnyTimeInterrogationResp,

	// mobility.imei EventType
	CheckImei, CheckImeiResp,

	// lsm EventType
	ProvideSubscriberLocation, ProvideSubscriberLocationResp, SubscriberLocationReport, SubscriberLocationReportResp, SendRoutingInfoForLCS, SendRoutingInfoForLCSResp;
}