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
                //自由态 相当于保存后未提交/撤回/驳回至制单人
            case NCCConstant.NCC_BILL_STATUS_NOPASS:
                //审批不通过
            case NCCConstant.NCC_BILL_STATUS_SUBMIT:
                //提交
                handleCalbackCommit(id, NCCConstant.getNcStatusToDiworkStatus(status), billType);
                break;
            case NCCConstant.NCC_BILL_STATUS_PASS:
                //审批通过
                handleCalbackPass(id, NCCConstant.getNcStatusToDiworkStatus(status), billType);
                break;
            case NCCConstant.NCC_BILL_STATUS_PENDDING:
                //审批进行中
                handleCalbackPending(id, NCCConstant.getNcStatusToDiworkStatus(status), billType);
                break;
            default:
                throw new AdderBusinessException("审批状态错误!", "");
        }
    }

    /**
     * @描述: 提交
     * @作者: sunshh
     * @参数 【id】		:id
     * @参数 【status】		:status
     * @参数 【billType】		:billType
     * @返回值: void
     * @日期: 2019/5/20 19:36
     */
    private void handleCalbackCommit(String id, Integer status, String billType) {
        switch (billType) {
            case NCCConstant.BILL_TYPE_LEAVE:
                //请假
                LeaveApply leaveApply = leaveApplyService.updateStatusByInstanceId(id, status);
                if (leaveApply != null) {
                    // 重新计算休假额度
                    calOneLeaveapply(leaveApply);
                }
                break;
            case NCCConstant.BILL_TYPE_LEAVE_OFF:
                //销假
                leaveOffService.updateStateByInstanceId(id, status);
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP:
                //出差
                businessTripApplyService.updateStatusByInstanceId(id, status);
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP_OFF:
                //销差
                businessTripApplyService.updateOffStatusByInstanceId(id, status);
                break;
            case NCCConstant.BILL_TYPE_OVERTIME:
                //加班
                overtimeBillService.updateOtBillStatusByInstanceId(status, id);
                break;
            case NCCConstant.BILL_TYPE_FILL_ATTEND:
                //补考勤
                fillAttendanceService.updateFillHeadAndBodyByInstanceId(id, status);
                break;
            case NCCConstant.BILL_TYPE_OUTSIDE_ATTEND:
                //外勤
                AttendRecord bill = attendRecordService.loadByInstanceId(id);
                updateAttendRecordStatus(bill, status);
                break;
            case NCCConstant.BILL_TYPE_REPLACE_CALENDAR:
                //调班
                replaceCalendarService.updateStatusByInstanceId(id, status);
                break;
            default:
                throw new AdderBusinessException("业务类型错误!", "");
        }
    }

    /**
     * @描述: 审批中
     * @作者: sunshh
     * @参数 【id】		:id
     * @参数 【status】		:status
     * @参数 【billType】		:billType
     * @返回值: void
     * @日期: 2019/5/20 19:33
     */
    private void handleCalbackPending(String id, Integer status, String billType) {
        switch (billType) {
            case NCCConstant.BILL_TYPE_LEAVE:
                //请假
                LeaveApply apply = getLeaveApplyBill(id);
                if (apply != null && apply.getApprovestatus() != null && apply.getApprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    leaveApplyService.updateStatusByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_LEAVE_OFF:
                // 销假，任务结束,修改状态
                LeaveApply leaveOffApply = getLeaveApplyBill(id);
                if (leaveOffApply != null && leaveOffApply.getOffapprovestatus() != null && leaveOffApply.getOffapprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    leaveOffService.updateStateByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP:
                //出差
                BusinessTripApply businessTripApply = getBusinessTripApplyBill(id);
                if (businessTripApply != null && businessTripApply.getApprovestatus() != null && businessTripApply.getApprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    businessTripApplyService.updateStatusByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP_OFF:
                // 销差通过后，出差表销差状态回写
                BusinessTripApply businessTripOffApply = getBusinessTripApplyBill(id);
                if (businessTripOffApply != null && businessTripOffApply.getRevokeapprovestatus() != null && businessTripOffApply.getRevokeapprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    businessTripApplyService.updateOffStatusByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_OVERTIME:
                //加班
                OvertimeMainBillVO bill = getOvertimeMainBill(id);
                if (null != bill && bill.getBillstatus() != null && bill.getBillstatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    overtimeBillService.updateOtBillStatusByInstanceId(status, id);
                }
                break;
            case NCCConstant.BILL_TYPE_FILL_ATTEND:
                //补考勤
                FillAttendHead fillAttendHead = getFillAttendHeadBill(id);
                if (null != fillAttendHead && fillAttendHead.getApproveStatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    fillAttendanceService.updateFillHeadAndBodyByInstanceId(id, status);
                }
                break;
            case NCCConstant.BILL_TYPE_OUTSIDE_ATTEND:
                //外勤
                AttendRecord outSideAttendRecord = getAttendRecordBill(id);
                if (null != outSideAttendRecord && outSideAttendRecord.getApproveStatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    updateAttendRecordStatus(outSideAttendRecord, status);
                }
                break;
            case NCCConstant.BILL_TYPE_REPLACE_CALENDAR:
                //调班
                ReplaceCalendar replaceCalendar = getReplaceCalendarBill(id);
                if (replaceCalendar != null && replaceCalendar.getApprovestate() != null && replaceCalendar.getApprovestate() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    replaceCalendarService.updateStatusByInstanceId(id, status);
                }
                break;
            default:
                throw new AdderBusinessException("业务类型错误!", "");
        }
    }

    /**
     * @描述: 审批通过
     * @作者: sunshh
     * @参数 【id】		    :id
     * @参数 【status】		:status
     * @参数 【billType】	:billType
     * @返回值: void
     * @日期: 2019/5/20 19:11
     */
    private void handleCalbackPass(String id, Integer status, String billType) {
        switch (billType) {
            case NCCConstant.BILL_TYPE_LEAVE:
                //请假
                LeaveApply apply = leaveApplyService.updateStatusByInstanceId(id, status);
                if (apply == null || apply.getApprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    return;
                }
                // 重新计算休假额度
                calOneLeaveapply(apply);
                log.error("请假发送数据到MQ={}", JSON.toJSONString(apply));
                // 发起计算任务
                sendMQTask(apply.getId(), billType, apply.getStaffid(), apply.getLeavebegintime(),
                        apply.getLeaveendtime());
                break;
            case NCCConstant.BILL_TYPE_LEAVE_OFF:
                // 销假，任务结束,修改状态
                LeaveApply leaveApply = leaveOffService.updateStateByInstanceId(id,
                        HRCloudConst.BILL_STATUS_APPROVED_PASS);
                if (leaveApply == null || leaveApply.getOffapprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    return;
                }
                // 销假，业务逻辑处理
                Map map = leaveOffService.handleApprovedPassCallBack(leaveApply.getId());
                if (MapUtils.isNotEmpty(map)) {
                    LeaveApply leaveOffApply = (LeaveApply) map.get("apply");
                    log.error("销假发送数据到MQ={},map={}", JSON.toJSONString(leaveOffApply), JSON.toJSONString(map));
                    // 重新计算休假额度
                    calOneLeaveapply(leaveOffApply);
                    // 发起计算任务
                    sendMQTask(leaveApply.getId(), billType, leaveOffApply.getStaffid(), (Date) map.get("minDate"),
                            (Date) map.get("maxDate"));
                }
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP:
                //出差
                BusinessTripApply businessTripApply = businessTripApplyService.updateStatusByInstanceId(id,
                        HRCloudConst.BILL_STATUS_APPROVED_PASS);
                if (businessTripApply == null || businessTripApply.getApprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    return;
                }
                log.error("出差发送数据到MQ={}", JSON.toJSONString(businessTripApply));
                // 发起计算任务
                sendMQTask(businessTripApply.getId(), billType, businessTripApply.getStaffid(),
                        businessTripApply.getTripbegintime(), businessTripApply.getTripendtime());
                break;
            case NCCConstant.BILL_TYPE_BUSINESS_TRIP_OFF:
                // 销差通过后，出差表销差状态回写
                businessTripApply = businessTripApplyService.updateOffStatusByInstanceId(id,
                        HRCloudConst.BILL_STATUS_APPROVED_PASS);
                if (businessTripApply == null || businessTripApply.getRevokeapprovestatus() != HRCloudConst.BILL_STATUS_APPROVED_PASS) {
                    return;
                }
                //销差，任务结束
                Map businessOffMap = businessTripRevokeService.handleApprovedPassCallBack(businessTripApply.getId());
                if (MapUtils.isNotEmpty(businessOffMap)) {
                    BusinessTripApply businessTripApply1 = (BusinessTripApply) businessOffMap.get("apply");
                    log.error("销差发送数据到MQ={},map={}", JSON.toJSONString(businessTripApply1),
                            JSON.toJSONString(businessOffMap));
                    // 发起计算任务
                    sendMQTask(businessTripApply.getId(), billType, businessTripApply1.getStaffid(),
                            (Date) businessOffMap.get("minDate"), (Date) businessOffMap.get("maxDate"));
                }
                break;
            case NCCConstant.BILL_TYPE_OVERTIME:
                //加班
                Map overtimeMap = overtimeBillService.updateOtBillStatusByInstanceId(
                        HRCloudConst.BILL_STATUS_APPROVED_PASS, id);
                if (MapUtils.isNotEmpty(overtimeMap)) {
                    log.error("加班发送数据到MQ={}", JSON.toJSONString(overtimeMap));
                    // 发起计算任务
                    sendMQTask(((OvertimeMainBillVO) overtimeMap.get("mainBillVO")).getId(), billType,
                            String.valueOf(overtimeMap.get("staffId")), (Date) overtimeMap.get("minDate"),
                            (Date) overtimeMap.get("maxDate"));
                }
                break;
            case NCCConstant.BILL_TYPE_FILL_ATTEND:
                //补考勤
                FillAttendHead bill = getFillAttendHeadBill(id);
                if (null != bill) {
                    fillAttendanceService.updateFillHeadAndBodyForYunShen(bill.getId(),
                            HRCloudConst.BILL_STATUS_APPROVED_PASS);
                    // 发起计算任务
                    sendMQTask(bill.getId(), billType, bill.getStaffId(), bill.getOriginalFillDate(),
                            bill.getOriginalFillDate());
                }
                break;
            case NCCConstant.BILL_TYPE_OUTSIDE_ATTEND:
                //外勤
                AttendRecord attendRecord = getAttendRecordBill(id);
                if (null != attendRecord) {
                    updateAttendRecordStatus(attendRecord, status);
                    // 发起计算任务
                    sendMQTask(attendRecord.getId(), billType, attendRecord.getStaffId(), attendRecord.getSigndate(),
                            attendRecord.getSigndate());
                }
                break;
            case NCCConstant.BILL_TYPE_REPLACE_CALENDAR:
                //调班
                replaceCalendarService.updateStatusByInstanceId(id, status);
                break;
            default:
                throw new AdderBusinessException("业务类型错误!", "");
        }
    }

    /**
     * 重新计算休假额度
     *
     * @param apply 休假单据
     */
    private void calOneLeaveapply(LeaveApply apply) {
        try {
            String userid = getStaffInfo(apply.getStaffid());
            //1、单据提交 2、主动撤回时 3、审批人拒绝 4、驳回制单人到草稿态 这四种状态需要扣除休假额度，或者返回休假额度
            leaveBlanceRptCalService.calOneLeaveapply(apply.getTenantid(), userid, apply);
        } catch (Exception e) {
            log.error("重新计算休假额度失败", e);
        }
    }

    /**
     * 获取人员信息
     *
     * @param staffId 人员ID
     * @return Staff
     */
    private String getStaffInfo(String staffId) {
        String userid = null;
        try {
            userid= simpleStaffCommonHepler.getUserId(staffId);
        } catch (Exception e) {
            log.error("获取用户信息失败！", e);
        }
        if (StringUtils.isBlank(userid)) {
            throw new AdderBusinessException("获取用户信息失败！", "attend.process.i18n0002");
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
            throw new AdderRuntimeException("BillCallback单据未找到:LeaveApply:" + businessKey);
        }
        return bill;
    }

    private OvertimeMainBillVO getOvertimeMainBill(String businessKey) {
        OvertimeMainBillVO bill = overtimeBillService.loadMainVOByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback单据未找到:OvertimeMainBillVO:" + businessKey);
        }
        return bill;
    }

    private BusinessTripApply getBusinessTripApplyBill(String businessKey) {
        BusinessTripApply bill = businessTripApplyService.loadByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback单据未找到:BusinessTripApply:" + businessKey);
        }
        return bill;
    }

    private ReplaceCalendar getReplaceCalendarBill(String businessKey) {
        ReplaceCalendar bill = replaceCalendarService.loadByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback单据未找到:ReplaceCalendar:" + businessKey);
        }
        return bill;
    }

    private FillAttendHead getFillAttendHeadBill(String businessKey) {
        FillAttendHead bill = fillAttendHeadService.loadByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback单据未找到:FillAttendHead:" + businessKey);
        }
        return bill;
    }

    private AttendRecord getAttendRecordBill(String businessKey) {
        AttendRecord bill = attendRecordService.loadByInstanceId(businessKey);
        if (bill == null) {
            throw new AdderRuntimeException("BillCallback单据未找到:AttendRecord:" + businessKey);
        }
        return bill;
    }


    /**
     * @描述: 发送数据到mq
     * @参数 【id】                :业务id
     * @参数 【billType】          :业务类型
     * @参数 【staffId】           :staffId
     * @参数 【beginDate】         :beginDate
     * @参数 【endDate】           :endDate
     * @日期: 2019/5/24 11:10
     * @作者: sunshh
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
     * 20210911 huangdj1 导入考勤原始打卡记录
     * @return
     * */
    @Override
    public Object importAttendanceData(JSONObject kqObj) {
        MessageResult result = new MessageResult("操作成功！", null, "attend.common.i18n1");
        List<AttendRecordIOVO> recordList = new ArrayList<>();
        JSONObject kqData = kqObj.getJSONObject("data");
        int total = kqData.getIntValue("total"); //总条数
        int pageSize = kqData.getIntValue("pageSize"); //每页多少条
        int pageNum = (int) Math.ceil(total / pageSize);//总页数
        JSONArray kqList = kqData.getJSONArray("list");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");//24小时制
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");//24小时制
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
            //批量插入
            result = attendRecordService.insertAttendRecordIOVO(recordList,1, null,null);
        }
        return result;
    }

}
