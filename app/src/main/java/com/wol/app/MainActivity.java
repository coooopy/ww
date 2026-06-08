package com.wol.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etMac, etIp, etPort;
    private Button btnWake;
    private TextView tvStatus;
    private SharedPreferences prefs;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("wol_config", Context.MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#F5F7FA"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(32));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Title
        TextView title = new TextView(this);
        title.setText("远程开机");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTextColor(Color.parseColor("#1A1A2E"));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(6));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Wake on LAN · 局域网唤醒");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setTextColor(Color.parseColor("#888888"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, dp(28));
        root.addView(subtitle);

        // MAC 地址卡片
        root.addView(makeLabel("目标主机 MAC 地址"));
        etMac = makeInput("例如：AA:BB:CC:DD:EE:FF", false);
        etMac.setText(prefs.getString("mac", ""));
        root.addView(etMac);

        root.addView(makeLabel("目标主机 IP 地址"));
        etIp = makeInput("例如：192.168.1.100", false);
        etIp.setText(prefs.getString("ip", ""));
        root.addView(etIp);

        root.addView(makeLabel("广播端口（默认 9）"));
        etPort = makeInput("9", false);
        etPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etPort.setText(prefs.getString("port", "9"));
        root.addView(etPort);

        // 状态栏
        tvStatus = new TextView(this);
        tvStatus.setText("");
        tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, dp(16), 0, dp(8));
        tvStatus.setTextColor(Color.parseColor("#555555"));
        root.addView(tvStatus);

        // 开机按钮
        btnWake = new Button(this);
        btnWake.setText("⚡  立即开机");
        btnWake.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        btnWake.setTextColor(Color.WHITE);
        btnWake.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnWake.setBackgroundColor(Color.parseColor("#4361EE"));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        btnParams.topMargin = dp(8);
        btnWake.setLayoutParams(btnParams);
        btnWake.setPadding(0, 0, 0, 0);
        // 圆角效果用 GradientDrawable
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(Color.parseColor("#4361EE"));
        btnBg.setCornerRadius(dp(12));
        btnWake.setBackground(btnBg);
        root.addView(btnWake);

        // 保存按钮
        Button btnSave = new Button(this);
        btnSave.setText("保存配置");
        btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnSave.setTextColor(Color.parseColor("#4361EE"));
        android.graphics.drawable.GradientDrawable saveBg = new android.graphics.drawable.GradientDrawable();
        saveBg.setColor(Color.WHITE);
        saveBg.setCornerRadius(dp(12));
        saveBg.setStroke(dp(1), Color.parseColor("#4361EE"));
        btnSave.setBackground(saveBg);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        saveParams.topMargin = dp(12);
        btnSave.setLayoutParams(saveParams);
        root.addView(btnSave);

        // 提示文字
        TextView tip = new TextView(this);
        tip.setText("ℹ️  目标主机需开启 BIOS 中的 WOL 功能，并在电源管理中允许网卡唤醒");
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tip.setTextColor(Color.parseColor("#AAAAAA"));
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(dp(4), dp(20), dp(4), 0);
        root.addView(tip);

        scroll.addView(root);
        setContentView(scroll);

        // 点击事件
        btnWake.setOnClickListener(v -> sendWOL());
        btnSave.setOnClickListener(v -> {
            saveConfig();
            showStatus("✅ 配置已保存", "#27AE60");
        });
    }

    private void saveConfig() {
        prefs.edit()
                .putString("mac", etMac.getText().toString().trim())
                .putString("ip", etIp.getText().toString().trim())
                .putString("port", etPort.getText().toString().trim())
                .apply();
    }

    private void sendWOL() {
        String mac = etMac.getText().toString().trim();
        String ip = etIp.getText().toString().trim();
        String portStr = etPort.getText().toString().trim();

        if (mac.isEmpty() || ip.isEmpty()) {
            showStatus("⚠️ 请填写 MAC 地址和 IP 地址", "#E74C3C");
            return;
        }

        mac = mac.replace("-", ":").replace(".", ":").toUpperCase();
        if (!mac.matches("([0-9A-F]{2}:){5}[0-9A-F]{2}")) {
            showStatus("❌ MAC 地址格式错误，例如：AA:BB:CC:DD:EE:FF", "#E74C3C");
            return;
        }

        int port = 9;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ignored) {}

        final String finalMac = mac;
        final int finalPort = port;

        btnWake.setEnabled(false);
        showStatus("🚀 正在发送魔术包...", "#4361EE");
        saveConfig();

        executor.execute(() -> {
            boolean success = false;
            String errMsg = "";
            try {
                byte[] magicPacket = buildMagicPacket(finalMac);
                InetAddress broadcastAddr = InetAddress.getByName(ip);
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                DatagramPacket packet = new DatagramPacket(
                        magicPacket, magicPacket.length, broadcastAddr, finalPort);
                socket.send(packet);
                // 额外发送广播地址 255.255.255.255
                DatagramPacket packet2 = new DatagramPacket(
                        magicPacket, magicPacket.length,
                        InetAddress.getByName("255.255.255.255"), finalPort);
                socket.send(packet2);
                socket.close();
                success = true;
            } catch (Exception e) {
                errMsg = e.getMessage();
            }

            final boolean ok = success;
            final String err = errMsg;
            mainHandler.post(() -> {
                btnWake.setEnabled(true);
                if (ok) {
                    showStatus("✅ 魔术包已发送！目标主机正在启动...", "#27AE60");
                } else {
                    showStatus("❌ 发送失败: " + err, "#E74C3C");
                }
            });
        });
    }

    private byte[] buildMagicPacket(String macAddress) {
        String[] macParts = macAddress.split(":");
        byte[] macBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            macBytes[i] = (byte) Integer.parseInt(macParts[i], 16);
        }
        // Magic Packet: 6 bytes 0xFF + 16 repetitions of MAC
        byte[] packet = new byte[6 + 16 * 6];
        for (int i = 0; i < 6; i++) packet[i] = (byte) 0xFF;
        for (int i = 6; i < packet.length; i += 6) {
            System.arraycopy(macBytes, 0, packet, i, 6);
        }
        return packet;
    }

    private void showStatus(String msg, String color) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(Color.parseColor(color));
    }

    private TextView makeLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(Color.parseColor("#555555"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        lp.bottomMargin = dp(4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText makeInput(String hint, boolean multiLine) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(Color.parseColor("#BBBBBB"));
        et.setTextColor(Color.parseColor("#1A1A2E"));
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), Color.parseColor("#DDE1E7"));
        et.setBackground(bg);
        if (!multiLine) et.setSingleLine(true);
        return et;
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
}
