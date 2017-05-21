/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.virb.jroal.virbcontrol;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class NsdHelper {

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;

    public static final String SERVICE_TYPE = "_garmin-virb._tcp.";

    public static final String TAG = "NsdHelper";
    public String mServiceName = "_garmin-";
    public boolean discoveryActive = false;
    public int count = 0;
    public String ip;

    NsdServiceInfo mService;

    public NsdHelper(Context context) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();

        //mNsdManager.init(mContext.getMainLooper(), this);

    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                ++count;
                Log.d(TAG, "Service discovery started " + count);
                discoveryActive = true;
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success " + service);
                if (service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Virb Service found: " + service.getServiceType());
                    mNsdManager.resolveService(service, mResolveListener);
                } else if (service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + service.getServiceName());
                } else if (service.getServiceName().contains(mServiceName)) {
                    Log.d(TAG, "Desired service found " + service.getServiceName());
                    mNsdManager.resolveService(service, mResolveListener);
                }
                String str = "Some other service Name:" + service.getServiceName();
                if (true) str += " Port: " + service.getPort();
                if (service.getServiceType() != null) str += " Type = " + service.getServiceType();
                Log.d(TAG, str);
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);

                Intent i = new Intent();
                i.setAction("co.virb.jroal.virbcontrol.IP");
                i.putExtra("ip", "");
                i.putExtra("camera", "");
                mContext.sendBroadcast(i);
                mService = null;

            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
                discoveryActive = false;

            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                try {
                    mNsdManager.stopServiceDiscovery(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                discoveryActive = false;
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                try {
                    mNsdManager.stopServiceDiscovery(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                discoveryActive = false;
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                ip = serviceInfo.getHost().getHostAddress().toString();
                Log.d(TAG, "IP: " + ip);
                Intent i = new Intent();
                i.setAction("co.virb.jroal.virbcontrol.IP");
                i.putExtra("ip", ip);
                i.putExtra("camera", serviceInfo.getServiceName());
                mContext.sendBroadcast(i);


                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP. " + serviceInfo.getHost().toString());
                    return;
                }
                mService = serviceInfo;
            }
        };
    }


    public void discoverServices() {
        //if(!discoveryActive)
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }

}
