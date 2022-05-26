# ycrash，安卓崩溃异常捕获类

# 项目废弃！！！！   项目废弃！！！！

# 技术太旧 且腾讯Bugly不香吗？

源码采用java8.0，安卓10.0，API29，androidx。
当然低版本copy代码过去也能用。

## 开发环境准备
**推荐使用jetBrains Toolbox 中的android studio，并更新到最新正式版**  

【必须】打开AS的安装目录，在bin目录下找到这两个文件（studio.exe.vmoptions，studio64.exe.vmoptions）  
在其中最后一行添加	-Dfile.encoding=UTF-8   
```bat
安装目录位置
C:\Users\用户名\AppData\Local\JetBrains\Toolbox\apps\AndroidStudio\ch-0\版本\bin
如：
C:\Users\yujing\AppData\Local\JetBrains\Toolbox\apps\AndroidStudio\ch-0\211.7628.21.2111.8139111\bin
```

##当前最新版：————> [![](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9qaXRwYWNrLmlvL3YveXV0aWxzL3ljcmFzaC5zdmc?x-oss-process=image/format,png)](https://jitpack.io/#yutils/ycrash)

- 为了支持个人资料的保护，维护个人隐私权。使用此异常捕获，请在隐私说明中描述出异常崩溃获取手机特征值信息等说明。

**[releases里面有JAR包。点击前往](https://github.com/yutils/ycrash/releases)**

**配套的Server端请看过来：[点击前往](https://github.com/yutils/ycrash-server)**

**配套的WEB前端页面请看过来：[点击前往](https://github.com/yutils/ycrash-web)**

## Gradle 引用

1. 在根build.gradle中添加
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

2. 子module添加依赖，当前最新版：————> [![](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9qaXRwYWNrLmlvL3YveXV0aWxzL3ljcmFzaC5zdmc?x-oss-process=image/format,png)](https://jitpack.io/#yutils/ycrash)

```
dependencies {
    implementation 'com.github.yutils:ycrash:1.0.3'
}
```

##  用法：
  1.在Application 的OnCreate中添加如下代码。

<font color=#0099ff size=4 >java</font>
```java
//必须
YCrash.getInstance().init(this);
//修改名称
YCrash.getInstance().setAppName("AppName");
//日志修改
YCrash.getInstance().setCrashInfoListener(new CrashInfoListener() {
    @Override public void info(AppInfo appInfo) {
        //打印，显示，储存
    }
});
//修改提交日志的服务器和端口，服务器源码见我另外一篇开源代码，设置成null则为不提交服务器
YCrash.getInstance().setIp("IP");
YCrash.getInstance().setPort("端口");
```

<font color=#0099ff size=4 >kotlin</font>
```kotlin
//必须
YCrash.getInstance().init(this)
//修改名称
YCrash.getInstance().appName = "AppName"
//日志修改
YCrash.getInstance().setCrashInfoListener {
    //打印，显示，储存
}
//修改提交日志的服务器和端口，服务器源码见我另外一篇开源代码，设置成null则为不提交服务器
YCrash.getInstance().setIp("IP")
YCrash.getInstance().setPort("端口")
```

## 异常信息存放：
    异常信息存放在：/sdcard/android/data/软件包名/files/crash/软件名_时间.log

## 注意添加权限：
> * 非必须权限 android.permission.INTERNET
> * 非必须权限 android.permission.ACCESS_COARSE_LOCATION
> * 非必须权限 android.permission.ACCESS_NETWORK_STATE
> * 非必须权限 android.permission.READ_PHONE_STATE


Github地址：[https://github.com/yutils/ycrash](https://github.com/yutils/ycrash)

我的CSDN：[https://blog.csdn.net/Yu1441](https://blog.csdn.net/Yu1441)

感谢关注微博：[细雨若静](https://weibo.com/32005200)