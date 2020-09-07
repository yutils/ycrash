package com.yujing.ycrash;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * 全局捕获异常当程序发生Uncaught异常的时候，记录错误日志，并且弹出提示，并回调给application
 *
 * @author 余静 QQ3373217 2020年9月7日10:21:01
 */
/* 使用举例

//用法：在Application 的 onCreate() 方法中添加 ，异常崩溃拦截写入日志到本地

YCrash.getInstance().init(this);
YCrash.getInstance().setAppName(getResources().getString(R.string.app_name));
YCrash.getInstance().setCrashInfoListener(appInfo -> Log.e("崩溃拦截", appInfo.崩溃信息));

异常信息存放在：/sdcard/android/data/软件报名/files/crash/软件名_时间.log
注意添加权限：
非必须权限 android.permission.INTERNET
非必须权限 android.permission.ACCESS_COARSE_LOCATION
非必须权限 android.permission.ACCESS_NETWORK_STATE
非必须权限 android.permission.READ_PHONE_STATE

 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
@SuppressLint("MissingPermission")
public class YCrash implements UncaughtExceptionHandler {
    private static final String TAG = "Ycrash";
    /**
     * 单列模式
     */
    @SuppressLint("StaticFieldLeak")
    private static YCrash instance = new YCrash();
    /**
     * Application上下文对象
     */
    private Context mContext;
    /**
     * 用于格式化日期,作为日志文件名的一部分，日期格式用于计算自动清理文件夹时间
     */
    @SuppressLint("SimpleDateFormat")
    private final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * APP名称，存放SD卡文件夹名称
     */
    private String AppName;
    /**
     * 回调监听
     */
    private CrashInfoListener crashInfoListener;
    /**
     * 自动清理文件夹时间（天）
     */
    private int autoClearDay = 30;
    /**
     * 服务器ip
     */
    private String ip = "crash.0-8.top";
    /**
     * 服务器端口
     */
    private String port = "8888";
    /**
     * 提交错误日志的地址
     */
    private static final String submitUrl = "/crash/submit";

    /**
     * 保证只有一个CrashHandler实例
     */
    private YCrash() {
    }

    /**
     * 关闭App时间
     */
    private int closeAppTime = 3000;
    /**
     * 全部信息
     */
    AppInfo appInfo = new AppInfo();

    /**
     * 获取CrashHandler实例 ,单例模式
     *
     * @return YCrash
     */
    public static YCrash getInstance() {
        if (instance == null)
            instance = new YCrash();
        return instance;
    }

    /**
     * 初始化
     *
     * @param context context
     */
    public void init(Context context) {
        mContext = context;
        // 设置该程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
        // 自动清理垃圾文件
        autoClear(autoClearDay);
    }

    public int getCloseAppTime() {
        return closeAppTime;
    }

    /**
     * 设置异常后关闭App时间
     *
     * @param closeAppTime 关闭App时间，毫秒
     */
    public void setCloseAppTime(int closeAppTime) {
        this.closeAppTime = closeAppTime;
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        //        if (handleException(ex)) {
        //            // 用户正常处理错误
        //        }
        exit();
    }

    /**
     * 退出程序
     */
    @SuppressLint("NewApi")
    private void exit() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 适合于安卓5.1以上手机退出
                ActivityManager activityManager = (ActivityManager) mContext.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    List<ActivityManager.AppTask> appTaskList = activityManager.getAppTasks();
                    if (appTaskList != null)
                        for (ActivityManager.AppTask appTask : appTaskList) {
                            appTask.finishAndRemoveTask();
                        }
                }
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } catch (Exception e) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.如果处理了该异常信息; 否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) return false;
        try {
            // 使用Toast来显示异常信息
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast toast = Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出。", Toast.LENGTH_SHORT);
                    toast.show();
                    // show();
                    // Handler handler = new Handler();
                    Looper.loop();
                    // handler.getLooper().quit();//退出Looper()
                }
            }.start();
            // 获取日志文件
            getCrashInfo(ex);
            // 保存日志文件
            saveFile();
            // 上传错误日志,判断网络权限
            int perm = mContext.checkCallingOrSelfPermission(Manifest.permission.INTERNET);
            if (perm == PackageManager.PERMISSION_GRANTED) {
                Map<String, Object> paramsMap = new HashMap<>();
                //如果引用的有Gson包就用Gson序列化
                try {
                    Class cls = Class.forName("com.google.gson.Gson");
                    Object obj = cls.newInstance();
                    //noinspection unchecked
                    Method method = cls.getMethod("toJson", Object.class);
                    String params = (String) method.invoke(obj, appInfo);
                    paramsMap.put("appInfo", params);
                } catch (Exception e) {
                    //如果失败就用Yjson转json
                    paramsMap.put("appInfo", YJson.toJson(appInfo));
                }
                if (ip != null && port != null)
                    post("http://" + ip + ":" + port + submitUrl, paramsMap);
            }
            // 回调
            try {
                if (crashInfoListener != null) {
                    crashInfoListener.info(appInfo);
                } else {
                    Log.e("Ycrash", appInfo.崩溃信息);
                }
            } catch (Exception e) {
                Log.e(TAG, "请检查Application中setCrashInfoListener回调中的错误！", e);
            }

            SystemClock.sleep(closeAppTime);
        } catch (Exception e) {
            Log.e(TAG, "事故处理类发生了错误！", e);
            SystemClock.sleep(closeAppTime);
            return false;
        }
        return true;
    }

    /**
     * 保存错误信息到文件中，返回文件名称, 便于将文件传送到服务器
     */
    private void getCrashInfo(Throwable ex) {
        if (AppName == null) AppName = mContext.getPackageName();
        appInfo.软件名称 = AppName;
        // 获取设备基础信息
        getBaseInfo();
        // 获取设备其他信息
        appInfo.其他信息 = getOtherInfo();
        // 获取异常信息
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.flush();
        printWriter.close();
        appInfo.崩溃信息 = writer.toString();
    }

    //保存到磁盘
    private void saveFile() throws Exception {
        appInfo.日志存放位置 = writeFile(appInfo.toString());
    }

    /**
     * 获取设备信息
     */
    @SuppressLint({"HardwareIds"})
    private void getBaseInfo() {
        appInfo.设备时间 = formatter.format(new Date());
        appInfo.isDebug = "" + isApkDebugable(mContext);
        // 收集设备信息
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                appInfo.包名 = pi.packageName;
                appInfo.版本名 = pi.versionName;
                appInfo.版本号 = "" + pi.versionCode;
                appInfo.第一次安装时间 = formatter.format(pi.firstInstallTime);
                appInfo.最后一次更新时间 = formatter.format(pi.lastUpdateTime);
                appInfo.安装路径 = mContext.getPackageResourcePath();
                appInfo.分辨率 = "" + mContext.getResources().getDisplayMetrics().widthPixels + "*" + mContext.getResources().getDisplayMetrics().heightPixels;
                appInfo.DPI = "" + mContext.getResources().getDisplayMetrics().densityDpi;
            }
        } catch (Exception e) {
            Log.e(TAG, "收集基础信息时出错", e);
        }
        try {
            appInfo.androidId = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            // 判断权限
            int perm = mContext.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE);
            if (perm == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    appInfo.imei = Build.VERSION.SDK_INT >= 26 ? telephonyManager.getImei() : telephonyManager.getDeviceId();
                    //不予以采集
                    //appInfo.手机号 = telephonyManager.getLine1Number();
                    appInfo.网络运营商 = telephonyManager.getNetworkOperatorName();
                    appInfo.网络类型 = "" + telephonyManager.getNetworkType();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "收集设备信息时出错", e);
        }
        try {
            // 判断权限
            int perm = mContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (perm == PackageManager.PERMISSION_GRANTED) {
                LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null) {
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        appInfo.位置 = location.getLongitude() + "," + location.getLatitude();
                        appInfo.精度 = "" + location.getAccuracy();
                        appInfo.高度 = "" + location.getAltitude();
                        appInfo.方向 = "" + location.getBearing();
                        appInfo.速度 = "" + location.getSpeed();
                        appInfo.定位时间 = formatter.format(location.getTime());
                        appInfo.定位类型 = location.getProvider();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "收集位置信息出错", e);
        }
    }

    /**
     * 收集设备参数信息
     */
    private String getOtherInfo() {
        StringBuilder sb = new StringBuilder();
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object obj = field.get(null);
                if (obj instanceof String[])
                    sb.append(field.getName()).append("=").append(Arrays.toString((String[]) obj)).append("\n");
                else
                    sb.append(field.getName()).append("=").append(Objects.requireNonNull(obj).toString()).append("\n");
            } catch (Exception e) {
                Log.e(TAG, "收集崩溃信息时出错", e);
            }
        }
        return sb.toString();
    }

    /**
     * 网络post请求
     *
     * @param url       url地址
     * @param paramsMap 参数
     */
    private void post(final String url, final Map<String, Object> paramsMap) {
        Thread thread = new Thread(() -> {
            StringBuilder params = new StringBuilder();
            if (paramsMap != null) {
                for (Entry<String, Object> element : paramsMap.entrySet()) {
                    if (element.getValue() == null)
                        continue;
                    params.append(element.getKey()).append("=").append(element.getValue()).append("&");
                }
                if (params.length() > 0)
                    params.deleteCharAt(params.length() - 1);
            }
            // 打开和URL之间的连接
            HttpURLConnection httpURLConnection;
            try {
                httpURLConnection = (HttpURLConnection) (new URL(url)).openConnection();
                httpURLConnection.setConnectTimeout(1000 * 3);
                httpURLConnection.setReadTimeout(1000 * 3);// 读取数据超时连续X秒没有读取到数据直接判断超时，不影响正在传输的数据（如：下载）
                // 设置通用的请求属性
                httpURLConnection.setUseCaches(false); // 设置缓存
                httpURLConnection.setRequestProperty("accept", "*/*");
                httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
                httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpURLConnection.setRequestProperty("Charset", "utf-8");
                httpURLConnection.setRequestMethod("POST");
                // 发送POST请求必须设置如下两行
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream output = httpURLConnection.getOutputStream();
                // 字符处理
                OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
                // 获取URLConnection对象对应的输出流
                PrintWriter out = new PrintWriter(writer);
                // 发送请求参数
                out.write(params.toString());
                // flush输出流的缓冲
                out.flush();
                out.close();
                // 根据ResponseCode判断连接是否成功
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "异常提交，失败！code：" + responseCode);
                    return;
                }
                // 定义BufferedReader输入流来读取URL的ResponseData
                InputStream inputStream = httpURLConnection.getInputStream();
                String response = inputStreamToString(inputStream);
                Log.i(TAG, response);
            } catch (java.net.SocketTimeoutException e) {
                Log.e(TAG, "异常提交网络超时");
            } catch (Exception e) {
                Log.e(TAG, "异常提时交发生异常：", e);
            }
        });
        thread.start();
    }

    /**
     * 读取inputStream字符串
     *
     * @param inputStream inputStream
     * @return String
     * @throws IOException IOException
     */
    private String inputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    /**
     * 获取存放log文件夹路径
     *
     * @return String
     */
    private String getGlobalPath() {
//        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "crash" + File.separator + AppName + File.separator;
        return getFilePath(mContext, "crash") + File.separator;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    private static String getFilePath(Context context, String dir) {
        String directoryPath;
        if (MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {//判断外部存储是否可用
            try {
                directoryPath = context.getExternalFilesDir(dir).getAbsolutePath();
            } catch (Exception e) {
                directoryPath = context.getFilesDir() + File.separator + dir;
            }
        } else {//没外部存储就使用内部存储
            directoryPath = context.getFilesDir() + File.separator + dir;
        }
        File file = new File(directoryPath);
        if (!file.exists()) {//判断文件目录是否存在
            file.mkdirs();
        }
        return directoryPath;
    }

    /**
     * String写入文件
     *
     * @param sb String
     * @return 路径
     * @throws Exception Exception
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String writeFile(String sb) throws Exception {
        String filePath = null;
        // 判断是否有SD卡
        String status = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(status)) {
            String path = getGlobalPath();
            String fileName = AppName + "-" + formatter.format(new Date()) + ".log";
            filePath = path + fileName;
            File dir = new File(path);
            if (!dir.exists())
                dir.mkdirs();
            FileOutputStream fos = new FileOutputStream(filePath, true);
            fos.write(sb.getBytes());
            fos.flush();
            fos.close();
        }
        return filePath;
    }

    /**
     * 文件删除，autoClearDay为文件保存天数
     *
     * @param autoClearDay 自动删除天数
     */
    private void autoClear(final int autoClearDay) {
        delete(getGlobalPath(), (file, filename) -> {
            String s = getFileNameWithoutExtension(filename);
            int day = autoClearDay < 0 ? autoClearDay : -1 * autoClearDay;
            // 获得几天之前或者几天之后的日期 day差值：正的往后推，负的往前推
            Calendar mCalendar = Calendar.getInstance();
            mCalendar.add(Calendar.DATE, day);
            String d = formatter.format(mCalendar.getTime());
            String date = AppName + "-" + d;
            return date.compareTo(s) >= 0;
        });
    }

    /**
     * 删除指定目录中特定的文件
     *
     * @param dir    目录
     * @param filter 回调
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void delete(String dir, FilenameFilter filter) {
        if (TextUtils.isEmpty(dir))
            return;
        File file = new File(dir);
        if (!file.exists())
            return;
        if (file.isFile())
            file.delete();
        if (!file.isDirectory())
            return;
        File[] lists;
        if (filter != null)
            lists = file.listFiles(filter);
        else
            lists = file.listFiles();

        if (lists == null)
            return;
        for (File f : lists) {
            if (f.isFile()) {
                f.delete();
            }
        }
    }

    /**
     * 获得不带扩展名的文件名称
     *
     * @param filePath 文件路径
     * @return 文件名称
     */
    private String getFileNameWithoutExtension(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        int indexPoint = filePath.lastIndexOf(".");
        int filePoint = filePath.lastIndexOf(File.separator);
        if (filePoint == -1) {
            return (indexPoint == -1 ? filePath : filePath.substring(0, indexPoint));
        }
        if (indexPoint == -1) {
            return filePath.substring(filePoint + 1);
        }
        return (filePoint < indexPoint ? filePath.substring(filePoint + 1, indexPoint) : filePath.substring(filePoint + 1));
    }

    /**
     * 异常信息回调给Application YuJing  2017年4月28日 下午6:15:39
     */
    @SuppressWarnings("UnnecessaryInterfaceModifier")
    public static interface CrashInfoListener {
        public void info(AppInfo appInfo);
    }

    /**
     * 设置监听
     *
     * @param crashInfoListener 异常信息监听
     */
    public void setCrashInfoListener(CrashInfoListener crashInfoListener) {
        this.crashInfoListener = crashInfoListener;
    }

    /**
     * 判断是不是debug模式
     *
     * @param context context
     * @return 是不是debug模式
     */
    private boolean isApkDebugable(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            Log.e(TAG, "判断是不是debug模式发生异常：", e);
        }
        return false;
    }

    public String getAppName() {
        return AppName;
    }

    public void setAppName(String appName) {
        AppName = appName;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setAutoClearDay(int autoClearDay) {
        this.autoClearDay = autoClearDay;
    }

    @SuppressWarnings("NonAsciiCharacters")
    public static class AppInfo implements Serializable {
        public String isDebug;
        public String 设备时间;
        public String 软件名称;
        public String 包名;
        public String 版本名;
        public String 版本号;
        public String 第一次安装时间;
        public String 最后一次更新时间;
        public String 安装路径;
        public String 分辨率;
        public String DPI;
        public String imei;
        public String androidId;
        //public String 手机号;
        public String 网络运营商;
        public String 网络类型;
        public String 位置;
        public String 精度;
        public String 高度;
        public String 方向;
        public String 速度;
        public String 定位时间;
        public String 定位类型;
        public String 其他信息;
        public String 崩溃信息;
        public String 日志存放位置;

        @Override
        public String toString() {
            return "AppInfo{" +
                    "isDebug='" + isDebug + '\'' +
                    ", 设备时间='" + 设备时间 + '\'' +
                    ", 软件名称='" + 软件名称 + '\'' +
                    ", 包名='" + 包名 + '\'' +
                    ", 版本名='" + 版本名 + '\'' +
                    ", 版本号='" + 版本号 + '\'' +
                    ", 第一次安装时间='" + 第一次安装时间 + '\'' +
                    ", 最后一次更新时间='" + 最后一次更新时间 + '\'' +
                    ", 安装路径='" + 安装路径 + '\'' +
                    ", 分辨率='" + 分辨率 + '\'' +
                    ", DPI='" + DPI + '\'' +
                    ", imei='" + imei + '\'' +
                    ", androidId='" + androidId + '\'' +
                    // ", 手机号='" + 手机号 + '\'' +
                    ", 网络运营商='" + 网络运营商 + '\'' +
                    ", 网络类型='" + 网络类型 + '\'' +
                    ", 位置='" + 位置 + '\'' +
                    ", 精度='" + 精度 + '\'' +
                    ", 高度='" + 高度 + '\'' +
                    ", 方向='" + 方向 + '\'' +
                    ", 速度='" + 速度 + '\'' +
                    ", 定位时间='" + 定位时间 + '\'' +
                    ", 定位类型='" + 定位类型 + '\'' +
                    ", 其他信息='" + 其他信息 + '\'' +
                    ", 崩溃信息='" + 崩溃信息 + '\'' +
                    ", 日志存放位置='" + 日志存放位置 + '\'' +
                    '}';
        }
    }
}
