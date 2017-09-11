package vimerzhao.bluetoothcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private final static int COUNT = 8;
    private Button mBtnSend;
    private EditText mEditTextContent;
    private ChatMsgViewAdapter mAdapter;
    private ListView mListView;
    private List<ChatMsgEntity> mDataArrays = new ArrayList<ChatMsgEntity>();
    private ActionBar actionBar;
    /**
     * 连接上的蓝牙设备的名字
     */
    private String mConnectedDeviceName = null;
    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(1, mConnectedDeviceName, "连接到  " + mConnectedDeviceName);

                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus("连接中。。。");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus("无连接");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    send(writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    receive(readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != this) {
                        Toast.makeText(ChatActivity.this, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != this) {
                        Toast.makeText(ChatActivity.this, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };
    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;
    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;
    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;
    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);//
        actionBar = getSupportActionBar();
        actionBar.setTitle("蓝牙聊天");
        setContentView(R.layout.activity_chat);
        initView();
        initData();

        //获得BluetoothAdapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //判断有没有蓝牙设备
        if (mBluetoothAdapter == null) {
            Log.e("错误", "设备没有蓝牙模块");
            finish();
        }

        // 快捷键
        final int[] id = {R.id.number0, R.id.number1, R.id.number2, R.id.number3, R.id.number4, R.id.number5, R.id.number6};
        for (int i = 0; i < id.length; i++) {
            final int finalI = i;
            findViewById(id[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String s = String.valueOf(finalI);
                    byte[] bytes = s.getBytes();
                    if (mChatService != null) {
                        mChatService.write(bytes);
                    }

                }
            });
        }
        findViewById(R.id.number_random).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = String.valueOf(((int) (Math.random() * 100))%id.length);
                byte[] bytes = s.getBytes();
                if (mChatService != null) {
                    mChatService.write(bytes);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        /** 打开蓝牙设备*/
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {

        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /** 发送消息的相关处理*/
                sendMessage();
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, "蓝牙开启失败！",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    /**
     * 连接设备
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    /**
     * 让本设备可见
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        if (null == this) {
            return;
        }
        final ActionBar actionBar = getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    private void setStatus(int ok, CharSequence Title, CharSequence subTitle) {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(Title);
        actionBar.setSubtitle(subTitle);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        if (null == this) {
            return;
        }
        final ActionBar actionBar = getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_connect) {
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, Constants.REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        }

        if (id == R.id.action_show_bluetooth) {
            ensureDiscoverable();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        mListView = (ListView) findViewById(R.id.chat_list_view);
        mBtnSend = (Button) findViewById(R.id.btn_send);
        mEditTextContent = (EditText) findViewById(R.id.et_sendmessage);
    }

    //初始化要显示的数据
    private void initData() {
        mAdapter = new ChatMsgViewAdapter(this, mDataArrays);
        mListView.setAdapter(mAdapter);
    }

    private void sendMessage() {
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "未连接蓝牙设备！", Toast.LENGTH_SHORT).show();
            mEditTextContent.setText("");
            return;
        }
        String contString = mEditTextContent.getText().toString();
        if (contString.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = contString.getBytes();
            mChatService.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mEditTextContent.setText(mOutStringBuffer);
        }
    }


    private void send(String msg) {
        // Check that we're actually connected before trying anything
        /*if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "未连接蓝牙设备！", Toast.LENGTH_SHORT).show();
            mEditTextContent.setText("");
            return;
        }
        String contString = mEditTextContent.getText().toString();
        if (contString.length() > 0)
        mEditTextContent.setText("");
        {*/
        ChatMsgEntity entity = new ChatMsgEntity();
        entity.setDate(getDate());
        entity.setName("我");
        entity.setMsgType(false);
        entity.setText(msg);
        mDataArrays.add(entity);
        mAdapter.notifyDataSetChanged();
        mListView.setSelection(mListView.getCount() - 1);
    }

    private void receive(String msg) {
        ChatMsgEntity entity = new ChatMsgEntity();
        entity.setDate(getDate());
        entity.setName(mConnectedDeviceName);
        entity.setMsgType(true);
        entity.setText(msg);
        mDataArrays.add(entity);
        mAdapter.notifyDataSetChanged();
        mListView.setSelection(mListView.getCount() - 1);
    }

    private String getDate() {
        Calendar c = Calendar.getInstance();
        String year = String.valueOf(c.get(Calendar.YEAR));
        String month = String.valueOf(c.get(Calendar.MONTH));
        String day = String.valueOf(c.get(Calendar.DAY_OF_MONTH) + 1);
        String hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
        String mins = String.valueOf(c.get(Calendar.MINUTE));
        StringBuffer sbBuffer = new StringBuffer();
        sbBuffer.append(year + "-" + month + "-" + day + " " + hour + ":" + mins);
        return sbBuffer.toString();
    }
}
