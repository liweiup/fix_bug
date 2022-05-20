package com.yonyou.nccloud.bill.service;


import com.alibaba.fastjson.JSONObject;

public interface INccBillService {

    void callback(String id, Integer status, String billType);

    /**
     *20210927 huangdj1 考勤打卡记录导入
     * @param billObj
     * @return
     */
    Object importAttendanceData( JSONObject billObj);

}
