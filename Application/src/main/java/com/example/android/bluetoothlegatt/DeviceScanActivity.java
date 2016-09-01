/*
 * Copyright (C) 2013 The Android Open Source Project
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



/*
* Algothrim:
* when people click scan
* do {
* 1 asyn:http request
* get SE_ki_(gpk)
* 2 scan m3 for 120s.
* put m3 packege to Map
* 3 calculate m3 for m4
*
* HASH : AESOPT.hashMac
* AES : new  a AESOPT object
* then ogject.setKey to set KEY
* KEY is a 4 byte(length) String
* then encrypt or decrypt
*
* set m4 to global variable
* 4 call startadv to ADV m4
* stradv{
* check setting data callback if null
* if not null
* call multiADV .start
* }
*
*
*
*
* }
*
*
* */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
//import java.util.Map;
import java.nio.channels.SelectionKey;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import android.widget.Toast;
import android.os.ParcelUuid;
import java.util.ArrayList;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertiseCallback;
import android.util.Log;
/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    Context act = this;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
  //  AdvertiseData advData;
 //   Map<String,String> scanAddress = new ArrayMap<String, String>();
    Map<String ,byte[]> scanData =new ArrayMap<String, byte[]>();
    Map<String,AdvertiseCallback> advCb = new ArrayMap<String, AdvertiseCallback>();
    byte[]  mac = new byte[]{ (byte)0xB9,0x27,(byte)0xEB,(byte)0xAB,(byte)0xBA,0x26};
    String readerAddress="B827EBABBA26";
    String resp= null;
    int m3Total=0;
    int stataic_iseq = 0;
    String static_m4 = "9000000";
    int advSeq=0;
    BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
    BluetoothLeAdvertiser advertiser1 = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
    BluetoothLeAdvertiser advertiser2 = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

    AdvertiseSettings settings = new AdvertiseSettings.Builder()
            .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
            .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
            .setConnectable( false )
            .setTimeout(1000)
            .build();
   // ParcelUuid pUuid = new ParcelUuid( UUID.fromString( getString( Constants.Service_UUID ) ) );

    AdvertiseData ADVdata;// =// buildAdvertiseData(0,"abcdefghijk");
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;
    AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
            super.onStartFailure(errorCode);
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
      //  scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {



            mBluetoothAdapter.startLeScan(mLeScanCallback);

                ADVdata = buildAdvertiseData(0,"111222333444555666777");
          //  advertiser.startAdvertising(settings,buildAdvertiseData(0,"111222333444555666777"),advertisingCallback);

                AsynNetUtils.get("http://192.168.0.2:12345/blt.txt", new AsynNetUtils.Callback() {
                    @Override
                    public void onResponse(String response) {
                        if (response != null) {
                            resp = response;
                            Log.d("TEA", response);
                        } else
                            Log.e("tea", "res[ponse = NULL");

                    }
                });


            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    invalidateOptionsMenu();
                    try {
                     m4cal();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, SCAN_PERIOD);
            //advertiser.stopAdvertising(advertisingCallback);
            mScanning = true;
           //testScab mBluetoothAdapter.startLeScan(mLeScanCallback);

        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
//scanRecord [10]  is sequence Num
// [4~9] mac
//11~28  is data
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                 //   String hex = "%X";

                    mLeDeviceListAdapter.addDevice(device);

                    mLeDeviceListAdapter.notifyDataSetChanged();
                    Log.d("d", new String(scanRecord));


                 /*   for(int i =0;i<scanRecord.length;i++){//
                        if(i%5 == 0)
                            System.out.print("s"+String.valueOf(0)+"s");
                        System.out.print(" "+(int) scanRecord[i]+" ");

                    }*/
                    int length = scanRecord.length;
                  //  scanData.put()
                    int intArray[]=new int[length];
                    System.out.print("\n");
                    int mSeq= (int)scanRecord[11]-48;
                    int macCheck=100;
                    macCheck = scanRecord[10]-scanRecord[9];
                    macCheck = macCheck +scanRecord[8]-scanRecord[7];
                    macCheck = macCheck +scanRecord[6]-scanRecord[5];
                    if(macCheck==0) {
                        readerAddress = device.getAddress();
                        readerAddress = readerAddress.replaceAll(":","");
                        //System.out.print(readerAddress);
                        scanData.put(String.valueOf(mSeq), scanRecord);
                        byte test[]=scanData.get(String.valueOf(mSeq));
                        // scanData.size();
                        for(int i =0;i<length;i++){//
                            if(i%5 == 0)
                                System.out.print("s"+String.valueOf(mSeq)+"s");
                            System.out.print(" "+(int) test[i]+" ");

                        }
                    }
                    /*for(int i =0;i<length;i++){//
                        if(i%5 == 0)
                            System.out.print("s"+String.valueOf(mSeq)+"s");
                        System.out.print(" "+(int) scanRecord[i]+"");

                    }*/
                    //set total length


                  //  System.out.print(" "+(int) scanRecord[11]+" ");



                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private AdvertiseData buildAdvertiseData(int seqq,String dataa) {
        int seq = seqq;
       //  byte[]  mac = new byte[6];
       // mac = new byte[]{(byte) 0xB8,0x27,(byte)0xEB,(byte)0xAB,(byte)0xBA,0x26};
        //System.out.print(mac);
      //  readerAddress=new String(mac);
        /**
         *
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */ //String mac = "ffffff";
        Log.e("e",dataa);
        //dataa = "12345678901234567890";
        // Toast.makeText(this,dataa,Toast.LENGTH_LONG).show();
        //seq = 1;
       // char f= 'f';
        byte[] sd = new byte[27];//xStringToByteArray("34a3957428ad");
/*data ramge  27   no name no TXPOWER
array 從1開始  跟自在統一
char 從'0'(ASCII 48) 開始
1~6 MAC 7 SEQ
length at sd[8]  len = n-48
   */     for(int i=0;i<=5;i++){
            sd[i] =  hexStringToByteArray(readerAddress)[i]; // mac test
        }
        sd[6] = (byte) seqq;
        char[] data;
        data = dataa.toCharArray();

        for(int i = 7;i<6+data.length;i++){
            sd[i] = dataa.getBytes()[i-7];
        }
       Log.e("build data",new String(sd));
        for(int i =0;i<sd.length;i++)
            System.out.print(" "+sd[i]);






        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(false);
        dataBuilder.setIncludeTxPowerLevel(false);
        dataBuilder.addServiceData(Constants.Service_UUID,sd);
        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());
        AdvertiseData a = dataBuilder.build();


        Log.i("advdata",a.toString());
        return a;
    }
    private void startAdv(String data){
        advSeq = 0;
       final int advlen = data.length();
        byte tm4[] = new byte[advlen+1];
        tm4[0] =(byte) advlen;
        for(int i=1;i<=advlen;i++)
            tm4[i] = data.getBytes()[i-1];


        static_m4 = new String(tm4);

        double dseq = Math.ceil(advlen/21);
       stataic_iseq  = (int) dseq;
        int tt = stataic_iseq;
        Log.e("ddddd",advlen+"  "+stataic_iseq);


        if(settings!= null){
            Log.e("t","s");
            if(ADVdata!= null){
                Log.e("t","data");



                if(advertisingCallback!= null) {
                    Log.e("t", "cb");
                        advertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                    if (advertiser != null) {
                        Log.e("t","adv");
                        //int seq = 9;

                            try{
                             //   ADVdata = buildAdvertiseData(0,"tttttt");
                            //    Log.e("data",ADVdata.toString());
                            //    advertiser.startAdvertising(settings,ADVdata,advertisingCallback);
                                new CountDownTimer(5000*(stataic_iseq+1),5000){
                                    int i =0;

                                    @Override
                                    public void onFinish() {
                                        Log.e("finish","end adv");
                                        multiAdv().start();

                                    }

                                    @Override
                                    public void onTick(long millisUntilFinished) {

                                        String m4d = "";
                                        System.out.print(stataic_iseq+static_m4);
                                        if (stataic_iseq >= i) {
                                                if(21*(i+1)>advlen)
                                                    m4d = static_m4.substring(i*21,advlen);
                                                else
                                                    m4d = static_m4.substring(i*21,(i+1)*21);
                                            advertiser.stopAdvertising(advertisingCallback);
                                            advertiser.startAdvertising(settings, buildAdvertiseData(i, m4d), advertisingCallback);
                                            Log.e("aaaaa",i+" "+m4d+"\n");


                                            i++;
                                        }

                                    }


                                }.start();






                                //seq --;
                              System.out.print("start done");
                                //  mHandler.postDelayed(Runnable stop = new R)
                            }catch (Exception e){


                            }


                        //advertiser.startAdvertising(settings,buildAdvertiseData(0,"ttttt"),advertisingCallback);



                    }
                    else {
                        Log.e("t","advfail");
                    }
                }
            }

        }

    }


    void m4cal() throws Exception {

        // b[12] = length
        // b[29] = data endif


        byte a[] = scanData.get("0");
        int totalLength = (int)a[12];
        if(totalLength<0)
            totalLength+=256;

        if(totalLength>=(scanData.size()-1)*19)
            System.out.print("ffff");
        byte tdata[] = new byte[totalLength+1];
        //totalLength-=48;

        int now = 0;
       double dseq = Math.ceil(totalLength/19);
        int iseq  = (int) dseq;
        for (int i = 0;i<iseq;i++){
            String key =  String.valueOf(i);
            byte btemp[] = scanData.get(key);

            for(int j =0;j<19;j++){
                tdata[now] = btemp[12+j];
                now++;
            }

           //
        }// for iseq set data from map
        byte data[] = new byte[totalLength];
        System.out.println("M3:");
        for(int i = 0;i<totalLength;i++){
            data[i] = tdata[i+1];

            System.out.print(" "+(int) data[i]);
        }

        System.out.print("\n");
        //String gpk = AESEncryptor.decrypt("1234",resp) ;//sd[ko,seki(gpk)]
        Log.e("g",resp);
        AESOPT aes_ki = AESOPT.getInstance();
        aes_ki.setKey("1234");
        String gpk = aes_ki.decrypt(resp);
        String m3 = new String(data);// m3 to string

        Log.e("t","M3:"+m3);
        Log.e("t","gpK:"+gpk);
        String SRN = m3.substring(m3.length()-32,m3.length());
        m3 = m3.substring(0,m3.length()-32);// split srn1
        AESOPT aes_gpk = AESOPT.getInstance();
        aes_gpk.setKey(gpk);
        String plain = aes_gpk.decrypt(m3) ;
        String rv = plain.substring(0,4);
        String ksq = plain.substring(4,8);
        String Hash = plain.substring(8,plain.length());
        String HashCal =oR(rv,ksq);
        //HashCal=aes_ki.encrypt(HashCal);

        Log.e("fff",rv+ " "+ ksq+" "+Hash);
        Log.e("f",HashCal);

        HashCal = AESOPT.hashMac(HashCal,"1234");
        Log.e("f",HashCal);
        String ri="";
        if(Hash.equals(HashCal)){
            ri = "eecs";
            Log.e("ri",ri);
        }
        String SEksq = xOR(ri,"0001");
        SEksq = xOR(SEksq,rv);
        AESOPT Aksq=AESOPT.getInstance();
        Aksq.setKey(ksq);


        SEksq = Aksq.encrypt(SEksq);

        String Hk = oR(rv,SRN);

        Hk = oR(Hk,ri);
        Hk = AESOPT.hashMac(Hk,"1234");
        Log.e("Hk",Hk);
        Log.e("SEKsq",SEksq);
        String m4 = Hk+SEksq+ri;
        Log.e("m4",m4);
        byte bm4[] = m4.getBytes();
        int m4Len = bm4.length;
        startAdv(m4);
     //   Intent ia = getServiceIntent(this);
      //  ia.putExtra("S1",advData);


    //    startAdv(m4);
    }
    String xOR(String a,String b){
        String result = "";
        byte ba[] = a.getBytes();
        byte bb[] = b.getBytes();
        byte bc[];
        byte temp[] = new byte[4];
        if(ba.length>bb.length)
            bc =new byte[ba.length] ;
        else
            bc = new byte[bb.length];
        for(int i = 0;i<4;i++){
            temp[i] = (byte) (ba[i]^bb[i]);

        }
        result = new String(temp);

        return  result;


    }

    String oR(String a,String b){
        String result = "";
        byte ba[] = a.getBytes();
        byte bb[] = b.getBytes();

        byte temp[] = new byte[4];

        for(int i = 0;i<4;i++){
            temp[i] = (byte) (ba[i]|bb[i]);

        }
        result = new String(temp);

        return  result;

    }
    AdvertiseCallback getAdvCb(){
        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
                super.onStartFailure(errorCode);
            }
        };
        return this.advertisingCallback;
    }


    AdvertiseSettings getSettings(){
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( false )
                .setTimeout(2000)
                .build();

        return settings;
    }


    /**
     * Stops BLE Advertising by stopping {@code AdvertiserService}.
     */
    Runnable stop = new Runnable() {
        @Override
        public void run() {
            advertiser.stopAdvertising(advertisingCallback);

        }
    };

    public static byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }
    public static byte[] hexStringToByteArray2(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    CountDownTimer multiAdv(){

        CountDownTimer a =new CountDownTimer(5000*(stataic_iseq+1),5000){
            int i =0;
            int time = 5000*(stataic_iseq+1);
            int advlen = static_m4.length();

            @Override
            public void onFinish() {
                Log.e("finish","end adv");
                advertiser.stopAdvertising(advertisingCallback);
                advertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                advertisingCallback=getAdvCb();
            }

            @Override
            public void onTick(long millisUntilFinished) {

                String m4d = "";
                System.out.print(stataic_iseq+static_m4);
                if (stataic_iseq >= i) {
                    if(21*(i+1)>advlen)
                        m4d = static_m4.substring(i*21,advlen);
                    else
                        m4d = static_m4.substring(i*21,(i+1)*21);
                    advertiser.stopAdvertising(advertisingCallback);
                    advertiser.startAdvertising(settings, buildAdvertiseData(i, m4d), advertisingCallback);
                    Log.e("aaaaa",i+" "+m4d+"\n");


                    i++;
                }
                else
                    this.onFinish();
            }


        }.start();
        return a;
    }
}