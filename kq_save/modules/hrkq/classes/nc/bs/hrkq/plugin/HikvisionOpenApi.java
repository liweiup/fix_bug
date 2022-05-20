package nc.bs.hrkq.plugin;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hikvision.cms.api.common.util.Digests;
import com.hikvision.cms.api.common.util.HttpClientSSLUtils;

public class HikvisionOpenApi {
    /**
     * APPKEY需要到部署的平台服务器上生成。
     * <p>
     * 调用Openpai的操作码，需要去平台上生成，详见《海康威视iVMS-8700平台SDKV2.*.* HTTP-OpenAPI使用说明书.chm》中[获取AppKey和Secret]章节说明
     * </p>
     * <p>
     * 《海康威视iVMS-8700平台SDKV2.*.* HTTP-OpenAPI使用说明书.chm》 该文档请找技术支持或者交付的同事提供
     * </p>
     */
    private static final String APPKEY = "51b30391";
    /**
     * SECRET需要到部署的平台服务器上生成。
     * <p>
     * 调用Openpai的操作码，需要去平台上生成，详见《海康威视iVMS-8700平台SDKV2.*.* HTTP-OpenAPI使用说明书.chm》中[获取AppKey和Secret]章节说明
     * </p>
     * <p>
     * 《海康威视iVMS-8700平台SDKV2.*.* HTTP-OpenAPI使用说明书.chm》 该文档请找技术支持或者交付的同事提供
     * </p>
     */
    private static final String SECRET = "31b2667143ea4faca34cf8df337a4ef9";
    /**
     * http请求地址
     * <p>openapi的地址,默认情况下openapi的IP端口与基础应用的IP端口是一致的</p>
     * <p>请将地址配置正确.</p>
     * <p>默认情况下是127.0.0.1:80 ，如果地址不通请根据实际情况修改IP端口</p>
     */
    private static final String OPENAPI_IP_PORT_HTTP = "http://10.10.203.150:80";
    /**
     * https请求地址
     * <p>openapi的地址,默认情况下openapi的IP端口与基础应用的IP端口是一致的</p>
     * <p>请将地址配置正确.</p>
     * <p>默认情况下是127.0.0.1:443 ，如果地址不通请根据实际情况修改IP端口</p>
     */
    private static final String OPENAPI_IP_PORT_HTTPS = "https://10.10.203.150:443";
    /**
     * 获取默认用户UUID的接口地址，此地址可以从《海康威视iVMS-8700平台SDKV2.*.* HTTP-OpenAPI使用说明书.chm》中具体的接口说明上获取
     */
    private static final String ITF_ADDRESS_GET_DEFAULT_USER_UUID = "/openapi/service/base/user/getDefaultUserUuid";
    /**
     * 分页获取监控点信息的接口地址，此地址可以从《海康威视iVMS-8700平台SDKV2.*.* HTTP-OpenAPI使用说明书.chm》中具体的接口说明上获取
     */
    private static final String ITF_ADDRESS_GET_CAMERAS = "/openapi/service/vss/res/getCameras";
    /**
     * <p>操作用户UUID，即用户UUID，首次使用操作用户UUID可以通过接口 [获取默认用户UUID]来获取</p>
     * <p>也可以通过接口[分页获取用户]来获取</p>
     */
    private static final String OP_USER_UUID = "cc78be40ec8611e78168af26905e6f0f";
    /**
     * 测试方法
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        /***http方式调用***/
//		System.out.println(testGetDefaultUserUUID());
//		System.out.println(getDoorEventsHistory());
//        System.out.println(getAttResultRecords());

        /***https方式调用***/
//		System.out.println(testGetDefaultUserUUID_Https());
//		System.out.println(testGetCameras_Https());
        String kqHistoryData = getDoorEventsHistory(0,0);
        JSONObject kqObj = JSONObject.parseObject(kqHistoryData).getJSONObject("data");
        int total = kqObj.getIntValue("total"); //总条数
        int pageSize = kqObj.getIntValue("pageSize"); //每页多少条
        int pageNum = (int) Math.ceil(total / pageSize);//总页数
        JSONArray kqList = kqObj.getJSONArray("list");

        System.out.println(kqObj.getIntValue("total"));
        System.out.println(pageNum);
    }

    /**
     * 获取历史打卡信息
     */
    public static String getDoorEventsHistory(long startTime,long endTime) throws Exception{
        String url = OPENAPI_IP_PORT_HTTP + "/openapi/service/acs/event/getDoorEventsHistory";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("appkey", APPKEY);//设置APPKEY
        map.put("time", System.currentTimeMillis());//设置时间参数
        map.put("pageNo", 1);//设置分页参数
        map.put("pageSize", 10);//设置分页参数
        map.put("opUserUuid", OP_USER_UUID);//设置操作用户UUID
        if (startTime != 0 && endTime != 0) {
            map.put("startTime", startTime);
            map.put("endTime", endTime);
        }
        String params = JSON.toJSONString(map);
//        System.out.println(" ====== getCameras请求参数：【" + params + "】");
//        String data = HttpClientSSLUtils.doPost(url + "?token=" + Digests.buildToken(url + "?" + params, null, SECRET), params);
//        System.out.println(" ====== getCameras请求返回结果：【{" + data + "}】");
        String data = "{\"errorCode\":0,\"errorMessage\":\"\",\"data\":{\"total\":520,\"pageNo\":1,\"pageSize\":10,\"list\":[{\"doorName\":\"北门西侧通道_门1\",\"doorUuid\":\"f2eca1b7b2eb42299ecaab56e851d893\",\"eventUuid\":\"C6ACF298-28E4-4068-96DB-EA543C3A52C9\",\"eventType\":196893,\"eventTime\":1651852753000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"643\",\"personId\":820,\"personName\":\"杨栋\",\"deptUuid\":\"788b116640d040ea97cea6526a8f4714\",\"deptName\":\"安全保卫部\",\"picUrl\":\"\",\"videoUrl\":null},{\"doorName\":\"南小楼入口_门1\",\"doorUuid\":\"4475d33b6bb54bde96e24841ec43da75\",\"eventUuid\":\"4920C146-CD6A-495F-A56F-44D1F9D00431\",\"eventType\":196893,\"eventTime\":1651848414000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"678430992\",\"personId\":1188,\"personName\":\"田来选\",\"deptUuid\":\"ca8f26fabc7843cdbbd84fd8a83c6bba\",\"deptName\":\"设备管理部_派遣\",\"picUrl\":\"\",\"videoUrl\":null},{\"doorName\":\"南门东侧_门1\",\"doorUuid\":\"8c939aa5d5e84e9fab2a1ec5e6599ac2\",\"eventUuid\":\"BEE4C466-C0C1-4936-B570-9BF71E244C20\",\"eventType\":196893,\"eventTime\":1651848288000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"678430992\",\"personId\":1188,\"personName\":\"田来选\",\"deptUuid\":\"ca8f26fabc7843cdbbd84fd8a83c6bba\",\"deptName\":\"设备管理部_派遣\",\"picUrl\":\"\",\"videoUrl\":null},{\"doorName\":\"北门东侧入口西_门1\",\"doorUuid\":\"2018b309d9e84387b06d0758a605312d\",\"eventUuid\":\"93D57254-2B23-4DD2-9F8C-6FEA728EBE9B\",\"eventType\":196893,\"eventTime\":1651848052000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"6789367891\",\"personId\":864,\"personName\":\"张景龙\",\"deptUuid\":\"8af376f98e5e4c1280029b3f3fcc26fd\",\"deptName\":\"安全保卫部_派遣\",\"picUrl\":\"\",\"videoUrl\":null},{\"doorName\":\"南门东侧_门1\",\"doorUuid\":\"8c939aa5d5e84e9fab2a1ec5e6599ac2\",\"eventUuid\":\"0708289F-BAC6-41FF-BDA5-2FC911E06E30\",\"eventType\":196893,\"eventTime\":1651846885000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"7643286541\",\"personId\":923,\"personName\":\"杨志军\",\"deptUuid\":\"8af376f98e5e4c1280029b3f3fcc26fd\",\"deptName\":\"安全保卫部_派遣\",\"picUrl\":\"\",\"videoUrl\":null},{\"doorName\":\"北门西侧通道_门1\",\"doorUuid\":\"f2eca1b7b2eb42299ecaab56e851d893\",\"eventUuid\":\"5D64D3E6-EC29-49A4-9B47-87BCEAB7E6EF\",\"eventType\":196893,\"eventTime\":1651846714000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"1757646112\",\"personId\":34,\"personName\":\"王振\",\"deptUuid\":\"788b116640d040ea97cea6526a8f4714\",\"deptName\":\"安全保卫部\",\"picUrl\":\"\",\"videoUrl\":null},{\"doorName\":\"西南门入口_门1\",\"doorUuid\":\"998a8aed8fa040f19e4d2a5bb3c3f544\",\"eventUuid\":\"E22AB2D1-81F9-42D7-B5F3-91041234DB6A\",\"eventType\":196893,\"eventTime\":1651846052000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"2189066611\",\"personId\":1279,\"personName\":\"蔡云鹏\",\"deptUuid\":\"8af376f98e5e4c1280029b3f3fcc26fd\",\"deptName\":\"安全保卫部_派遣\",\"picUrl\":\"\",\"videoUrl\":null},{\"doorName\":\"西南门入口_门1\",\"doorUuid\":\"998a8aed8fa040f19e4d2a5bb3c3f544\",\"eventUuid\":\"BEB341F9-8146-49CA-BCFC-5C15F9CB50A9\",\"eventType\":196893,\"eventTime\":1651846048000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"2189066611\",\"personId\":1279,\"personName\":\"蔡云鹏\",\"deptUuid\":\"8af376f98e5e4c1280029b3f3fcc26fd\",\"deptName\":\"安全保卫部_派遣\",\"picUrl\":\"\",\"videoUrl\":null},{\"doorName\":\"北门西侧通道_门1\",\"doorUuid\":\"f2eca1b7b2eb42299ecaab56e851d893\",\"eventUuid\":\"8BAF6C5E-C50A-41FB-8D2A-6E6989B805D9\",\"eventType\":196893,\"eventTime\":1651845150000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"0209802733\",\"personId\":26,\"personName\":\"常欣\",\"deptUuid\":\"788b116640d040ea97cea6526a8f4714\",\"deptName\":\"安全保卫部\",\"picUrl\":\"\",\"videoUrl\":null},{\"doorName\":\"北门东侧入口西_门1\",\"doorUuid\":\"2018b309d9e84387b06d0758a605312d\",\"eventUuid\":\"2C2B80BD-BC5F-458F-ACC2-4158342E0D80\",\"eventType\":196893,\"eventTime\":1651844948000,\"eventName\":\"人脸认证通过\",\"deviceType\":201943047,\"cardNo\":\"1146\",\"personId\":807,\"personName\":\"金磊\",\"deptUuid\":\"788b116640d040ea97cea6526a8f4714\",\"deptName\":\"安全保卫部\",\"picUrl\":\"\",\"videoUrl\":null}]}}\n";
        return data;
    }

    public static String getAttResultRecords() throws Exception{
        String url = OPENAPI_IP_PORT_HTTP + "/openapi/service/att/record/getAttResultRecords";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("appkey", APPKEY);//设置APPKEY
        map.put("time", System.currentTimeMillis());//设置时间参数
        map.put("pageNo", 1);//设置分页参数
        map.put("pageSize", 10);//设置分页参数
        map.put("opUserUuid", OP_USER_UUID);//设置操作用户UUID
        map.put("shiftType", 0);//班次类型
        map.put("startTime", 1651766400000L);
        map.put("endTime", 1651852800000L);
        String params = JSON.toJSONString(map);
        System.out.println(" ====== getCameras请求参数：【" + params + "】");
        String data = HttpClientSSLUtils.doPost(url + "?token=" + Digests.buildToken(url + "?" + params, null, SECRET), params);
        System.out.println(" ====== getCameras请求返回结果：【{" + data + "}】");
//        {"errorCode":0,"errorMessage":"分页获取考勤结果成功!","data":{"total":2594,"pageNo":1,"pageSize":10,"list":[{"resultId":2408048,"personId":550,"personName":"蔡博洋","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null},{"resultId":2408049,"personId":551,"personName":"傅琳","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null},{"resultId":2408050,"personId":139,"personName":"王月前","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null},{"resultId":2408051,"personId":138,"personName":"佟春燕","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null},{"resultId":2408052,"personId":546,"personName":"吕东","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null},{"resultId":2408053,"personId":140,"personName":"翟胜利","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null},{"resultId":2408054,"personId":143,"personName":"艾晶","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null},{"resultId":2408055,"personId":545,"personName":"乐日乐","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null},{"resultId":2408056,"personId":558,"personName":"果林","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null},{"resultId":2408057,"personId":131,"personName":"黄玉成","departId":57,"departName":"陈列工作部","groupCode":"0018","shiftCode":"0001","onDutyDate":1651766400000,"timePeriodNo":1,"clockOnTime":null,"onStatus":24,"acReaderNameOn":null,"doorNameOn":null,"controllerNameOn":null,"cardNoOn":null,"minuteLate":null,"onTime":1651797000000,"offTime":1651825800000,"clockOffTime":null,"offStatus":24,"acReaderNameOff":null,"doorNameOff":null,"controllerNameOff":null,"cardNoOff":null,"minLeave":0,"minCompLeave":0,"minOvertime":0,"minAttend":0,"timeAll":null,"lessTime":null}]}}
        return data;
    }

    /**
     * HTTP方式
     * 获取默认用户UUID 测试
     * @return
     * @throws Exception
     */
    public static String testGetDefaultUserUUID() throws Exception{
        String url = OPENAPI_IP_PORT_HTTP + ITF_ADDRESS_GET_DEFAULT_USER_UUID;
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("appkey", APPKEY);//设置APPKEY
        map.put("time", System.currentTimeMillis());//设置时间参数
        String params =  JSON.toJSONString(map);
        System.out.println(" ====== testGetDefaultUserUUID 请求参数：【" + params + "】");
        String data = HttpClientSSLUtils.doPost(url + "?token=" + Digests.buildToken(url + "?" + params, null, SECRET), params);
        System.out.println(" ====== testGetDefaultUserUUID 请求返回结果：【{" + data + "}】");

        return data;
    }
}
