# ycrash，安卓崩溃异常捕获类
源码采用java8.0，安卓10.0，API29，androidx。
当然低版本copy代码过去也能用。

##当前最新版：————> [![](https://jitpack.io/v/yutils/ycrash.svg)](https://jitpack.io/#yutils/ycrash)

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

2. 子module添加依赖，当前最新版：————> [![](https://jitpack.io/v/yutils/ycrash.svg)](https://jitpack.io/#yutils/ycrash)

```
dependencies {
    implementation 'com.github.yutils:ycrash:1.0.1'
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
    异常信息存放在：/sdcard/android/data/软件报名/files/crash/软件名_时间.log

## 注意添加权限：
> * 非必须权限  android.permission.INTERNET
> * 非必须权限 android.permission.ACCESS_COARSE_LOCATION
> * 非必须权限 android.permission.ACCESS_NETWORK_STATE
> * 非必须权限 android.permission.READ_PHONE_STATE


感谢关注：[细雨若静](https://weibo.com/32005200)