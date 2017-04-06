package com.lhy.netlibrary.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;

/**
 * Created by lhy on 2017/3/17
 */
public class NetUtil {

    public static boolean checkNet(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected() && networkInfo.isAvailable();
    }

    /**
     * 无网络连接
     */
    public static final int NETWORK_NONE = -1;
    /**
     * WiFi连接
     */
    public static final int NETWORK_WIFI = 1;
    /**
     * 移动数据连接
     */
    public static final int NETWORK_MOBILE = 0;

    /**
     * 网线连接
     */
    public static final int TYPE_ETHERNET = 9;

    /**
     * 获取网络状态
     *
     * @param context 上下文
     * @return 网络状态
     */
    public static int getNetworkState(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo == null) return NETWORK_NONE;

            return networkInfo.getType();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return NETWORK_NONE;
    }

    /**
     * 是否是移动数据连接
     *
     * @param context 上下文
     * @return true为移动数据连接
     */
    public static boolean isMOBILEConnection(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = manager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * 是否是WiFi连接
     *
     * @param context 上下文
     * @return true为WiFi连接
     */
    public static boolean isWIFIConnection(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = manager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * 是否是网线
     *
     * @param context 上下文
     * @return true为WiFi连接
     */
    public static boolean isEthernetConnection(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = manager
                .getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * 获取广播IP
     *
     * @param context 上下文
     * @return 广播IP
     */
    public static String getBroadcastIp(Context context) {
//		WifiManager mWifiManager = (WifiManager) context .getSystemService(Context.WIFI_SERVICE);
//		WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
//		return intToIp(mWifiInfo.getIpAddress(), true);

        String ip = getDefaultIpAddresses(context);
        if (ip == null) return ip;
        assert ip != null;
        String[] split = ip.split("\\.");
        return split[0] + "." + split[1] + "." + split[2] + "." + "255";
    }

    /**
     * 获取WiFi的IP
     *
     * @param context 上下文
     * @return WiFi的IP
     */
    public static String getIp(Context context) {
//		WifiManager mWifiManager = (WifiManager) context .getSystemService(Context.WIFI_SERVICE);
//		WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
//		return intToIp(mWifiInfo.getIpAddress(),false);

        return getDefaultIpAddresses(context);
    }

    /**
     * 将WiFi地址转换成WiFi格式的字符串
     *
     * @param i              WiFi地址
     * @param istBroadcastIp true为广播IP false为WiFi的IP
     * @return 字符串
     */
    private static String intToIp(int i, boolean istBroadcastIp) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + (istBroadcastIp ? "255" : ((i >> 24) & 0xFF));
    }

    /**
     * Returns the default link's IP addresses, if any, taking into account IPv4 and IPv6 style
     * addresses.
     *
     * @param context the application context
     * @return the formatted and newline-separated IP addresses, or null if none.
     */
    public static String getDefaultIpAddresses(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        try {

            Method getActiveLinkProperties = ConnectivityManager.class.getMethod("getActiveLinkProperties");
            LinkProperties prop = (LinkProperties) getActiveLinkProperties.invoke(cm);

            return formatIpAddresses(prop);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) return null;
        try {
            Method getAllAddresses = LinkProperties.class.getMethod("getAllAddresses");

            List<InetAddress> addressList = (List<InetAddress>) getAllAddresses.invoke(prop);
            for (InetAddress mInetAddress : addressList) {
                if (mInetAddress instanceof Inet4Address) {
                    return mInetAddress.getHostAddress();
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}

