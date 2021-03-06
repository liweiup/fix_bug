package com.yonyou.hrcloud.bill.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yonyou.hrcloud.adder.context.AppContext;
import com.yonyou.hrcloud.adder.exception.AdderBusinessException;
import com.yonyou.hrcloud.adder.exception.AdderRuntimeException;
import com.yonyou.hrcloud.adder.service.IGenericService;
import com.yonyou.hrcloud.attentrpt.service.ITsAttendRptCalService;
import com.yonyou.hrcloud.berry.helper.HRCloudConst;
import com.yonyou.hrcloud.berry.model.MessageResult;
import com.yonyou.hrcloud.businesstrip.model.BusinessTripApply;
import com.yonyou.hrcloud.businesstrip.service.IBusinessTripApplyService;
import com.yonyou.hrcloud.businesstrip.service.IBusinessTripRevokeService;
import com.yonyou.hrcloud.calendar.model.ReplaceCalendar;
import com.yonyou.hrcloud.calendar.service.IReplaceCalendarService;
import com.yonyou.hrcloud.leave.model.LeaveApply;
import com.yonyou.hrcloud.leave.service.ILeaveApplyService;
import com.yonyou.hrcloud.leave.service.ILeaveBlanceRptCalService;
import com.yonyou.hrcloud.leave.service.ILeaveOffService;
import com.yonyou.hrcloud.overtime.model.OvertimeMainBillVO;
import com.yonyou.hrcloud.overtime.service.IOvertimeBillService;
import com.yonyou.hrcloud.sign.model.AttendRecord;
import com.yonyou.hrcloud.sign.model.AttendRecordIOVO;
import com.yonyou.hrcloud.sign.model.FillAttendHead;
import com.yonyou.hrcloud.sign.service.IAttendRecordService;
import com.yonyou.hrcloud.sign.service.IFillAttendHeadService;
import com.yonyou.hrcloud.sign.service.IFillAttendanceService;
import com.yonyou.hrcloud.tcommon.helper.staffAndOrg.SimpleStaffCommonHepler;
import com.yonyou.hrcloud.tcommon.model.MQParamsDTO;
import com.yonyou.hrcloud.tcommon.utils.NCCConstant;
import com.yonyou.nccloud.bill.service.INccBillService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author litfb
 * @version 2.0
 * @since 2019-07-13
 */
@Slf4j
@Service
public class NCCBillServiceImpl implements INccBillService {
    @Autowired @Lazy
    private ILeaveApplyService leaveApplyService;
    @Autowired
    private ILeaveOffService leaveOffService;
    @Autowired
    private IBusinessTripApplyService businessTripApplyService;
    @Autowired
    private IOvertimeBillService overtimeBillService;
    @Autowired
    private IFillAttendanceService fillAttendanceService;
    @Autowired
    private IAttendRecordService attendRecordService;
    @Autowired
    private IReplaceCalendarService replaceCalendarService;
    @Autowired
    private IFillAttendHeadService fillAttendHeadService;
    @Autowired
    private IBusinessTripRevokeService businessTripRevokeService;
    @Autowired
    private ILeaveBlanceRptCalService leaveBlanceRptCalService;
    @Autowired
    private SimpleStaffCommonHepler simpleStaffCommonHepler;
    @Autowired
    private ITsAttendRptCalService tsAttendRptCalService;
    @Autowired
    private IGenericService genericService;

    @Override
        public void callback(String id, Integer status, String billType) {
        log.error("ncc-callback-id=[{}],status=[{}],billType=[{}]",id,status,billType);
        switch (status) {
            case NCCConstant.NCC_BILL_STATUS_FREE:
                //????????? ???????????????????????????/??????/??????????????????
            case NCCConstant.NCC_BILL_STATUS_NOPASS:
                //???????????????
            case NCCConstant.NCC_BILL_STATUS_SUBMIT:
                //??????
                handleCalbackCommit(id, NCCConstant.getNcStatusToDiworkStatus(status), billType);
                break;
            case NCCConstant.NCC_BILL_STATUS_PASS:
                //????????????
                handleCalbackPass(id, NCCConstant.getNcStatusToDiworkStatus(status), billType);
                break;
            case NCCConstant.NCC_BILL_STATUS_PENDDING:
                //???????????????
                handleCalbackPending(id, NCCConstant.getNcStatusToDiworkStatus(status), billType);
                break;
            default:
                throw new AdderBusinessException("??????????????????!", "");
        }
    }

    /**
     * @??????: ??????
     * @??????: sunshh
     * @?????? ???id???		:id
     * @?????? ???status???		:status
     * @?????? ???billType???		:billType
     * @?????????: void
     * @??????: 2019/5/20 19:36
     */
    private void handleCalbackCommit(String id, Integer status, String billType) {
        switch (billType) {
            case NCCConstant.BILL_TYPE_LEAVE:
                //??????
                LeaveApply leaveApply = leaveApplyService.updateStatusByInstanceId(id, status);
                if (leaveApply != null) {
                    // ????????????????????????
                    calOneLeaveapply(leaveApply);
                }
                break;
            case NCCConstant.BILL_TYPE_LEAVE_OFF:
                //??????
                leaveOffService.updateStateByInstanceId(id, status);
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP:
                //??????
                businessTripApplyService.updateStatusByInstanceId(id, status);
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP_OFF:
                //??????
                businessTripApplyService.updateOffStatusByInstanceId(id, status);
                break;
            case NCCConstant.BILL_TYPE_OVERTIME:
                //??????
                overtimeBillService.updateOtBillStatusByInstanceId(status, id);
                break;
            case NCCConstant.BILL_TYPE_FILL_ATTEND:
                //?????????
                fillAttendanceService.updateFillHeadAndBodyByInstanceId(id, status);
                break;
            case NCCConstant.BILL_TYPE_OUTSIDE_ATTEND:
                //??????
                AttendRecord bill = attendRecordService.loadByInstanceId(id);
                updateAttendRecordStatus(bill, status);
                break;
            case NCCConstant.BILL_TYPE_REPLACE_CALENDAR:
                //??????
                replaceCalendarService.updateStatusByInstanceId(id, status);
                break;
            default:
                throw new AdderBusinessException("??????????????????!", "");
        }
    }

    /**
     * @??????: ?????????
     * @??????: sunshh
     * @?????? ???id???		:id
     * @?????? ???status???		:status
     * @?????? ???billType???		:billType
     * @?????????: void
     * @??????: 2019/5/20 19:33
     */
    private void handleCalbackPending(String id, Integer status, String billType) {
        switch (billType) {
            case NCCConstant.BILL_TYPE_LEAVE:
                //??????
                LeaveApply apply = getLeaveApplyBill(id);
                if (apply != null && apply.getApprovestatus() != null && apply.getApprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    leaveApplyService.updateStatusByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_LEAVE_OFF:
                // ?????????????????????,????????????
                LeaveApply leaveOffApply = getLeaveApplyBill(id);
                if (leaveOffApply != null && leaveOffApply.getOffapprovestatus() != null && leaveOffApply.getOffapprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    leaveOffService.updateStateByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP:
                //??????
                BusinessTripApply businessTripApply = getBusinessTripApplyBill(id);
                if (businessTripApply != null && businessTripApply.getApprovestatus() != null && businessTripApply.getApprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    businessTripApplyService.updateStatusByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP_OFF:
                // ?????????????????????????????????????????????
                BusinessTripApply businessTripOffApply = getBusinessTripApplyBill(id);
                if (businessTripOffApply != null && businessTripOffApply.getRevokeapprovestatus() != null && businessTripOffApply.getRevokeapprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    businessTripApplyService.updateOffStatusByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_OVERTIME:
                //??????
                OvertimeMainBillVO bill = getOvertimeMainBill(id);
                if (null != bill && bill.getBillstatus() != null && bill.getBillstatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    overtimeBillService.updateOtBillStatusByInstanceId(status, id);
                }
                break;
            case NCCConstant.BILL_TYPE_FILL_ATTEND:
                //?????????
                FillAttendHead fillAttendHead = getFillAttendHeadBill(id);
                if (null != fillAttendHead && fillAttendHead.getApproveStatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    fillAttendanceService.updateFillHeadAndBodyByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_OUTSIDE_ATTEND:
                //??????
                AttendRecord outSideAttendRecord = getAttendRecordBill(id);
                if (null != outSideAttendRecord && outSideAttendRecord.getApproveStatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    updateAttendRecordStatus(outSideAttendRecord, status);
                }
                break;
            case NCCConstant.BILL_TYPE_REPLACE_CALENDAR:
                //??????
                ReplaceCalendar replaceCalendar = getReplaceCalendarBill(id);
                if (replaceCalendar != null && replaceCalendar.getApprovestate() != null && replaceCalendar.getApprovestate() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    replaceCalendarService.updateStatusByInstanceId(id, status);
                }
                break;
            default:
                throw new AdderBusinessException("??????????????????!", "");
        }
    }

    /**
     * @??????: ????????????
     * @??????: sunshh
     * @?????? ???id???		    :id
     * @?????? ???status???		:status
     * @?????? ???billType???	:billType
     * @?????????: void
     * @??????: 2019/5/20 19:11
     */
    private void handleCalbackPass(String id, Integer status, String billType) {
        switch (billType) {
            case NCCConstant.BILL_TYPE_LEAVE:
                //??????
                LeaveApply apply = leaveApplyService.updateStatusByInstanceId(id, status);
                if (apply == null || apply.getApprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    return;
                }
                // ????????????????????????
                calOneLeaveapply(apply);
                log.error("?????????????????????MQ={}", JSON.toJSONString(apply));
                // ??????????????????
                sendMQTask(apply.getId(), billType, apply.getStaffid(), apply.getLeavebegintime(),
                        apply.getLeaveendtime());
                break;
            case NCCConstant.BILL_TYPE_LEAVE_OFF:
                // ?????????????????????,????????????
                LeaveApply leaveApply = leaveOffService.updateStateByInstanceId(id,
                        HRCloudConst.BILL_STATUS_APPROVED_PASS);
                if (leaveApply == null || leaveApply.getOffapprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    return;
                }
                // ???????????????????????????
                Map map = leaveOffService.handleApprovedPassCallBack(leaveApply.getId());
                if (MapUtils.isNotEmpty(map)) {
                    LeaveApply leaveOffApply = (LeaveApply) map.get("apply");
                    log.error("?????????????????????MQ={},map={}", JSON.toJSONString(leaveOffApply), JSON.toJSONString(map));
                    // ????????????????????????
                    calOneLeaveapply(leaveOffApply);
                    // ??????????????????
                    sendMQTask(leaveApply.getId(), billType, leaveOffApply.getStaffid(), (Date) map.get("minDate"),
                            (Date) map.get("maxDate"));
                }
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP:
                //??????
                BusinessTripApply businessTripApply = businessTripApplyService.updateStatusByInstanceId(id,
                        HRCloudConst.BILL_STATUS_APPROVED_PASS);
                if (businessTripApply == null || businessTripApply.getApprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    return;
                }
                log.error("?????????????????????MQ={}", JSON.toJSONString(businessTripApply));
                // ??????????????????
                sendMQTask(businessTripApply.getId(), billType, businessTripApply.getStaffid(),
                        businessTripApply.getTripbegintime(), businessTripApply.getTripendtime());
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP_OFF:
                // ?????????????????????????????????????????????
                businessTripApply = businessTripApplyService.updateOffStatusByInstanceId(id,
                        HRCloudConst.BILL_STATUS_APPROVED_PASS);
                if (businessTripApply == null || businessTripApply.getRevokeapprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    return;
                }
                //?????????????????????
                Map businessOffMap = businessTripRevokeService.handleApprovedPassCallBack(businessTripApply.getId());
                if (MapUtils.isNotEmpty(businessOffMap)) {
                    BusinessTripApply businessTripApply1 = (BusinessTripApply) businessOffMap.get("apply");
                    log.error("?????????????????????MQ={},map={}", JSON.toJSONString(businessTripApply1),
                            JSON.toJSONString(businessOffMap));
                    // ??????????????????
                    sendMQTask(businessTripApply.getId(), billType, businessTripApply1.getStaffid(),
                            (Date) businessOffMap.get("minDate"), (Date) businessOffMap.get("maxDate"));
                }
                break;
            case NCCConstant.BILL_TYPE_OVERTIME:
                //??????
                Map overtimeMap = overtimeBillService.updateOtBillStatusByInstanceId(
                        HRCloudConst.BILL_STATUS_APPROVED_PASS, id);
                if (MapUtils.isNotEmpty(overtimeMap)) {
                    log.error("?????????????????????MQ={}", JSON.toJSONString(overtimeMap));
                    // ??????????????????
                    sendMQTask(((OvertimeMainBillVO) overtimeMap.get("mainBillVO")).getId(), billType,
                            String.valueOf(overtimeMap.get("staffId")), (Date) overtimeMap.get("minDate"),
                            (Date) overtimeMap.get("maxDate"));
                }
                break;
            case NCCConstant.BILL_TYPE_FILL_ATTEND:
                //?????????
                FillAttendHead bill = getFillAttendHeadBill(id);
                if (null != bill) {
                    fillAttendanceService.updateFillHeadAndBodyForYunShen(bill.getId(),
                            HRCloudConst.BILL_STATUS_APPROVED_PASS);
                    // ??????????????????
                    sendMQTask(bill.getId(), billType, bill.getStaffId(), bill.getOriginalFillDate(),
                            bill.getOriginalFillDate());
                }
                break;
            case NCCConstant.BILL_TYPE_OUTSIDE_ATTEND:
                //??????
                AttendRecord attendRecord = getAttendRecordBill(id);
                if (null != attendRecord) {
                    updateAttendRecordStatus(attendRecord, status);
                    // ??????????????????
                    sendMQTask(attendRecord.getId(), billType, attendRecord.getStaffId(), attendRecord.getSigndate(),
                            attendRecord.getSigndate());
                }
                break;
            case NCCConstant.BILL_TYPE_REPLACE_CALENDAR:
                //??????
                replaceCalendarService.updateStatusByInstanceId(id, status);
                break;
            default:
                throw new AdderBusinessException("??????????????????!", "");
        }
    }

    /**
     * ????????????????????????
     *
     * @param apply ????????????
     */
    private void calOneLeaveapply(LeaveApply apply) {
        try {
            String userid = getStaffInfo(apply.getStaffid());
            //1??????????????? 2?????????????????? 3?????????????????? 4?????????????????????????????? ??????????????????????????????????????????????????????????????????
            leaveBlanceRptCalService.calOneLeaveapply(apply.getTenantid(), userid, apply);
        } catch (Exception e) {
            log.error("??????????????????????????????", e);
        }
    }

    /**
     * ??????????????????
     *
     * @param staffId ??????ID
     * @return Staff
     */
    private String getStaffInfo(String staffId) {
        String userid = null;
        try {
            userid= simpleStaffCommonHepler.getUserId(staffId);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
        }
        if (StringUtils.isBlank(userid)) {
            throw new AdderBusinessException("???????????????????????????", "attend.process.i18n0002");
        }
        return userid;
    }

    private void updateAttendRecordStatus(AttendRecord bill, Integer approveStatus) {
        bill.setApproveStatus(approveStatus);
        List<String> updateField = new ArrayList<>();
        updateField.add("approveStatus");
        genericService.update(bill, updateField);
    }

    private LeaveApply getLeaveApplyBill(String businessKey) {
        LeaveApply bill = leaveApplyService.loadByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback???????????????:LeaveApply:" + businessKey);
        }
        return bill;
    }

    private OvertimeMainBillVO getOvertimeMainBill(String businessKey) {
        OvertimeMainBillVO bill = overtimeBillService.loadMainVOByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback???????????????:OvertimeMainBillVO:" + businessKey);
        }
        return bill;
    }

    private BusinessTripApply getBusinessTripApplyBill(String businessKey) {
        BusinessTripApply bill = businessTripApplyService.loadByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback???????????????:BusinessTripApply:" + businessKey);
        }
        return bill;
    }

    private ReplaceCalendar getReplaceCalendarBill(String businessKey) {
        ReplaceCalendar bill = replaceCalendarService.loadByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback???????????????:ReplaceCalendar:" + businessKey);
        }
        return bill;
    }

    private FillAttendHead getFillAttendHeadBill(String businessKey) {
        FillAttendHead bill = fillAttendHeadService.loadByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback???????????????:FillAttendHead:" + businessKey);
        }
        return bill;
    }

    private AttendRecord getAttendRecordBill(String businessKey) {
        AttendRecord bill = attendRecordService.loadByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback???????????????:AttendRecord:" + businessKey);
        }
        return bill;
    }


    /**
     * @??????: ???????????????mq
     * @?????? ???id???                :??????id
     * @?????? ???billType???          :????????????
     * @?????? ???staffId???           :staffId
     * @?????? ???beginDate???         :beginDate
     * @?????? ???endDate???           :endDate
     * @??????: 2019/5/24 11:10
     * @??????: sunshh
     */
    private void sendMQTask(String id, String billType, String staffId, Date beginDate,
            Date endDate) {
        MQParamsDTO mqParamsDTO = new MQParamsDTO(id,
                AppContext.getContext().getScope(),
                AppContext.getContext().getUserId(), 1, 2,
                staffId, beginDate, endDate
        );
        try {
            tsAttendRptCalService.calculateRptForMQ(mqParamsDTO);
        } catch (Exception e) {
            String message = "CategoryID:[" + billType + "], BusinessKey:[" + id + "] " + "sendMQTask faild";
            log.error(message, e);
        }
    }

    /**
     * 20210911 huangdj1 ??????????????????????????????
     * @return
     * */
    @Override
    public Object importAttendanceData(JSONObject kqObj) {
        MessageResult result = new MessageResult("???????????????", null, "attend.common.i18n1");
        List<AttendRecordIOVO> recordList = new ArrayList<>();
        JSONObject kqData = kqObj.getJSONObject("data");
        int total = kqData.getIntValue("total"); //?????????
        int pageSize = kqData.getIntValue("pageSize"); //???????????????
        int pageNum = (int) Math.ceil(total / pageSize);//?????????
        JSONArray kqList = kqData.getJSONArray("list");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");//24?????????
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");//24?????????
        if (kqList.size() > 0) {
            AttendRecordIOVO record = new AttendRecordIOVO();
            for (int i = 0; i < kqList.size(); i++) {
                JSONObject kqInfo = JSONObject.parseObject(kqList.get(i).toString());
                record.setPlaceName(kqInfo.getString("doorName"));
                long eventTime = kqInfo.getLong("eventTime");
                Date date = new Date(eventTime);
                record.setDate(format.format(date));
                record.setTime(hourFormat.format(date));
//                record.setCardCode(kqInfo.getString("deptUuid"));
                record.setCardCode("20110527");
                recordList.add(record);
            }
            //????????????
            result = attendRecordService.insertAttendRecordIOVO(recordList,1, null,null);
        }
        return result;
    }

}
