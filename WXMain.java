package com.a85.wechatplugin;

import android.app.Activity;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findField;

/**
 * @author hejiangwei
 * Created at 2019/2/25.
 * @Describe 微信版本为7.0.3  使用请修改手机号／微信号／QQ号
 */
public class WXMain implements IXposedHookLoadPackage {


    public static final String PACKAGE_NAME = "com.tencent.mm";
    public static final String TABLE_MESSAGE = "message";
    public static String TAG_INSERT = "insertWithOnConflict";
    private XC_LoadPackage.LoadPackageParam mlpparam = null;
    private Context mwxContext;
    private boolean isFromAddFriend = false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.contains("com.tencent.mm")) {
//            XposedHelpers.setStaticBooleanField();
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mlpparam = loadPackageParam;
                    final Context context = ((Context) param.args[0]);
                    Log.e("Application", "afterHookedMethod========" + context.getPackageName() + ":" + context.getPackageManager().getPackageInfo(PACKAGE_NAME, 0).versionName);

                    mwxContext = context;
                    hookMultiClound();
                }
            });
        }
    }

    private void hookMultiClound() {
        /**
         * 在微信主界面的时候把isFromAddFriend重置为false，防止查看已加好友信息的时候也会执行click方法
         */
        XposedHelpers.findAndHookMethod("com.tencent.mm.ui.LauncherUI", mlpparam.classLoader,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        isFromAddFriend = false;

                    }
                });
        //全局搜索xposed字段出现的方法，可能是微信的Xposed检测
        XposedHelpers.findAndHookMethod("com.tencent.mm.app.t", mlpparam.classLoader,
                "a",
                StackTraceElement[].class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        super.afterHookedMethod(param);
                        if ((Boolean) param.getResult()) {
                            log("----检测到xposed");
                            param.setResult(false);
                        }
                    }
                });
        final Class FTSAddFriendUIClass;
        final Class SayHiWithSnsPermissionUIClass;
        final Class ContactInfoUIClass;
        final Class GetDataClass;
        final Class ControllerClass;
        try {
            FTSAddFriendUIClass = mlpparam.classLoader.loadClass("com.tencent.mm.plugin.fts.ui.FTSAddFriendUI");
            SayHiWithSnsPermissionUIClass = mlpparam.classLoader.loadClass("com.tencent.mm.plugin.profile.ui.SayHiWithSnsPermissionUI");
            ContactInfoUIClass = mlpparam.classLoader.loadClass("com.tencent.mm.plugin.profile.ui.ContactInfoUI");
            GetDataClass = mlpparam.classLoader.loadClass("com.tencent.mm.pluginsdk.ui.applet.a");
            ControllerClass = mlpparam.classLoader.loadClass("com.tencent.mm.ui.q");

            findAndHookMethod(FTSAddFriendUIClass,
                    "onCreate", Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
//                            oFTSAddFriendUI = param.thisObject;
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    isFromAddFriend = true;
                                    XposedHelpers.setObjectField(param.thisObject, "query", "手机号／微信号／QQ号");
                                    XposedHelpers.callMethod(param.thisObject, "Mf", "手机号／微信号／QQ号");

                                }
                            }, 1000);

                        }
                    });

            /**
             * EF方法的参数
             */
            findAndHookMethod("com.tencent.mm.plugin.profile.a", mlpparam.classLoader,
                    "EF", String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            log("onClick------1" + param.args[0]);
//                            throw new NullPointerException();
                        }
                    });


            /**
             * onItemClick点击
             */

            findAndHookMethod("com.tencent.mm.ui.base.preference.MMPreference$2", mlpparam.classLoader,
                    "onItemClick", AdapterView.class, View.class, int.class, long.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            Object preference = ((AdapterView) param.args[0]).getAdapter().getItem((int) param.args[2]);

                            log("onItemClick------" + preference.getClass().getSimpleName() + "==" +
                                    XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.ui.base.preference.Preference", mlpparam.classLoader), "mKey").get(preference)
                            );
                        }
                    });
            findAndHookMethod(ContactInfoUIClass,
                    "onCreate", Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isFromAddFriend) {
                                        return;
                                    }

                                    try {
                                        Object oHt = XposedHelpers.findField(ContactInfoUIClass, "oHt").get(param.thisObject);

                                        XposedHelpers.callMethod(oHt, "EF", "contact_profile_add_contact");

                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 1000);
                        }
                    });

            /**
             * MMActivity中的mController的抽象父类构建Menu
             */
            findAndHookMethod(ControllerClass,
                    "onCreateOptionsMenu", Menu.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            if (TextUtils.equals(XposedHelpers.findField(ControllerClass, "xyi").get(param.thisObject).getClass().getSimpleName(),
                                    "SayHiWithSnsPermissionUI") && isFromAddFriend) {
                                log("onCreateOptionsMenu--" +
                                        "list(0).xrP=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.ui.q$a", mlpparam.classLoader), "xrP").get(((LinkedList) findField(ControllerClass, "xrG").get(param.thisObject)).get(0)) + "===" +
                                        "list(0).xyM==null=" + (XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.ui.q$a", mlpparam.classLoader), "xyM").get(((LinkedList) findField(ControllerClass, "xrG").get(param.thisObject)).get(0)) == null) + "===" +
                                        "textview=" + ((TextView) findField(ControllerClass, "xrx").get(param.thisObject)).getText() + "==" +
                                        "Button=" + ((Button) findField(ControllerClass, "lvg").get(param.thisObject)).getText() + "=="
                                );
                          final Object menuItem = ((Menu) param.args[0]).getItem(0);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Object c2171a = ((LinkedList) findField(ControllerClass, "xrG").get(param.thisObject)).get(0);
                                            Object menuItemClickListener = findField(XposedHelpers.findClass("com.tencent.mm.ui.q$a", mlpparam.classLoader), "gkj").get(c2171a);
                                            XposedHelpers.callMethod(menuItemClickListener, "onMenuItemClick", menuItem);
                                        } catch (IllegalAccessException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }, 300);
                            }
                        }
                    });
            findAndHookMethod(SayHiWithSnsPermissionUIClass,
                    "onCreate", Bundle.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            log("SayHiWithSnsPermissionUI-bundle=" + logBundle(((Activity) param.thisObject).getIntent().getExtras()));
                        }

                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            if (!isFromAddFriend) {
                                return;
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    try {

                                        EditText editText = (EditText) XposedHelpers.findField(SayHiWithSnsPermissionUIClass, "oLC").get(param.thisObject);
                                        editText.setText("测试用，勿加");
                                        EditText editText1 = (EditText) XposedHelpers.findField(SayHiWithSnsPermissionUIClass, "oLD").get(param.thisObject);
                                        editText1.setText("测试好友");


                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 100);
                        }
                    });

            /**
             * 应该是进行网络请求查看是否需要验证的方法，其中有一个tipDialog
             */
            findAndHookMethod(GetDataClass,
                    "a", String.class, LinkedList.class, boolean.class, String.class,

                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            log(
                                    "m72431a---" + param.args[0] + "--" +
                                            param.args[1] + "----" +
                                            param.args[2] + "----" +
                                            param.args[3] + "----"

                            );

                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                            log("GetDataClass--uzK=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "uzK").get(param.thisObject) +
                                    "--uzL=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "uzL").get(param.thisObject) +
                                    "--oXa=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "oXa").get(param.thisObject) +
                                    "--uzG=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "uzG").get(param.thisObject) +
                                    "--uzI=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "uzI").get(param.thisObject) +
                                    "--mdP=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "mdP").get(param.thisObject) +
                                    "--hPC=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "hPC").get(param.thisObject) +
                                    "--uzJ=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "uzJ").get(param.thisObject) +
                                    "--uzN=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "uzN").get(param.thisObject) +
                                    "--chatroomName=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "chatroomName").get(param.thisObject) +
                                    "--jyu=" + XposedHelpers.findField(XposedHelpers.findClass("com.tencent.mm.pluginsdk.ui.applet.a", mlpparam.classLoader), "jyu").get(param.thisObject)
                            );

                        }
                    });

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void log(Object str) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        XposedBridge.log("com.a85.wechatplugin:[" + df.format(new Date()) + "]:  "
                + str.toString());
    }

   
    public static String logBundle(Bundle bundle) {
        StringBuffer stringBuffer = new StringBuffer();
        for (String key : bundle.keySet()) {
            stringBuffer.append("Key=" + key + ", content=" + bundle.get(key) + "\n");
        }
        return stringBuffer.toString();
    }

}
