package nc.bs.hrkq.plugin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.alibaba.fastjson.JSONObject;

import lombok.SneakyThrows;

import nc.bs.pub.pa.PreAlertObject;
import nc.bs.pub.taskcenter.BgWorkingContext;
import nc.bs.pub.taskcenter.IBackgroundWorkPlugin;
import nc.vo.pub.BusinessException;
import nccloud.pub.hrkq.util.AttendInvoker;

/**
 * 导入考勤数据
 * author liwei
 */
public class ImportAttendanceDataPlugin implements IBackgroundWorkPlugin {

    @SneakyThrows
    @Override
    public PreAlertObject executeTask(BgWorkingContext arg) throws BusinessException {
        LinkedHashMap<String, Object> param = arg.getKeyMap();
        String startTime = String.valueOf(param != null ? param.get("start_time") : "");
        String endTime = String.valueOf(param != null ? param.get("end_time") : "");
        try {
            String[] dateArr = getDateArr(startTime,endTime);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//24小时制
            long sTimeMill = format.parse(dateArr[0]).getTime();
            long eTimeMill = format.parse(dateArr[1]).getTime();
            //获取考勤数据
            String kqHistoryData = HikvisionOpenApi.getDoorEventsHistory(sTimeMill,eTimeMill);
            AttendInvoker.getInstance().beanInvoke("com.yonyou.nccloud.bill.service.INccBillService","importAttendanceData",new Class<?>[] { JSONObject.class}, new Object[]{JSONObject.parseObject(kqHistoryData)});
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public static String[] getDateArr(String startTime,String endTime) throws ParseException {
        String[] dateArr = {"",""};
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance(); //创建Calendar 的实例
        if (startTime.equals("") && endTime.equals("")) {
            calendar.add(Calendar.DAY_OF_MONTH, -1); //当前时间减去一天，即一天前的时间
            startTime = format.format(calendar.getTime())+" 00:00:00";
            endTime = format.format(calendar.getTime())+" 23:59:59";
        } else if (!startTime.equals("") && !endTime.equals("")) {
            format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            startTime = format.format(format.parse(startTime));
            endTime = format.format(format.parse(startTime));
        } else if (!startTime.equals("")) {
            startTime = format.format(calendar.getTime())+" 00:00:00";
            endTime = format.format(calendar.getTime())+" 23:59:59";
        }
        dateArr[0] = startTime;
        dateArr[1] = endTime;
        return dateArr;
    }

    public static void main(String[] args) throws ParseException {
        String[] dateArr = getDateArr("","");
        System.out.println(Arrays.toString(dateArr));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//24小时制
        long sTimeMill = format.parse(dateArr[0]).getTime();
        long eTimeMill = format.parse(dateArr[1]).getTime();
        System.out.println(sTimeMill);
        System.out.println(eTimeMill);
    }
}
