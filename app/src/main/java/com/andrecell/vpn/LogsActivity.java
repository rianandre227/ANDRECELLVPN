package com.andrecell.vpn;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class LogsActivity extends Activity {

    TextView tvLogContent, btnBack, btnCopyLog, btnClearLog;
    EditText etSearchLog;
    ScrollView scrollLogs;
    Button btnJumpLatest;

    // Filter TextViews
    TextView filterAll, filterError, filterWarning, filterInfo, filterDebug;

    ArrayList<String> masterLogList; // Menyimpan semua log asli
    String currentFilter = "All";
    String currentSearch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        // Inisialisasi View
        tvLogContent = findViewById(R.id.tvLogContent);
        btnBack = findViewById(R.id.btnBack);
        btnCopyLog = findViewById(R.id.btnCopyLog);
        btnClearLog = findViewById(R.id.btnClearLog);
        etSearchLog = findViewById(R.id.etSearchLog);
        scrollLogs = findViewById(R.id.scrollLogs);
        btnJumpLatest = findViewById(R.id.btnJumpLatest);

        filterAll = findViewById(R.id.filterAll);
        filterError = findViewById(R.id.filterError);
        filterWarning = findViewById(R.id.filterWarning);
        filterInfo = findViewById(R.id.filterInfo);
        filterDebug = findViewById(R.id.filterDebug);

        // 1. BUAT DATA LOG TIRUAN (DUMMY)
        generateDummyLogs();

        // 2. TAMPILKAN LOG
        renderLogs();

        // 3. AKSI TOMBOL KEMBALI
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Kembali ke halaman sebelumnya
            }
        });

        // 4. AKSI TOMBOL COPY
        btnCopyLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("VPN Logs", tvLogContent.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(LogsActivity.this, "Logs disalin ke clipboard!", Toast.LENGTH_SHORT).show();
            }
        });

        // 5. AKSI TOMBOL HAPUS
        btnClearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                masterLogList.clear();
                renderLogs();
                Toast.makeText(LogsActivity.this, "Logs dibersihkan!", Toast.LENGTH_SHORT).show();
            }
        });

        // 6. FITUR PENCARIAN
        etSearchLog.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().toLowerCase();
                renderLogs();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 7. AKSI TOMBOL FILTER
        setupFilterButton(filterAll, "All");
        setupFilterButton(filterError, "Error");
        setupFilterButton(filterWarning, "Warning");
        setupFilterButton(filterInfo, "Info");
        setupFilterButton(filterDebug, "Debug");

        // 8. AKSI JUMP TO LATEST
        btnJumpLatest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollLogs.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollLogs.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    // ==========================================
    // FUNGSI MEMBUAT LOG TIRUAN (SESUAI SCREENSHOT)
    // ==========================================
    private void generateDummyLogs() {
        masterLogList = new ArrayList<>();
        masterLogList.add("2026-10-26T19:23:02.082+0700 [app] LOCK_RELEASE outcome=success");
        masterLogList.add("2026-10-26T19:23:02.111+0700 [app] DESTROY tunnel_running=false");
        masterLogList.add("2026-10-26T19:23:02.115+0700 [app] TEARDOWN_BEGIN had_core=false had_tun=false");
        masterLogList.add("2026-10-26T19:23:02.126+0700 [app] TEARDOWN_END duration_ms=12");
        masterLogList.add("2026-10-26T19:23:02.127+0700 [app] LOCK_RELEASE outcome=success");
        masterLogList.add("2026-10-26T19:23:04.150+0700 [app] SERVICE_COMMAND command=connect redelivered=false");
        masterLogList.add("2026-10-26T19:23:04.177+0700 [app] LOCK_ACQUIRE outcome=success cpu=true wifi=true");
        masterLogList.add("2026-10-26T19:23:04.178+0700 [app] CONNECT_REQUESTED source=service_command profile_available=true");
        masterLogList.add("2026-10-26T19:23:04.179+0700 [app] CONNECT_PREPARING");
        masterLogList.add("2026-10-26T19:23:04.182+0700 [app] PROFILE_META protocol=trojan transport=ws sealed=false mtu=1280 ipv6=false");
        masterLogList.add("2026-10-26T19:23:04.190+0700 [app] HOTSPOT_PROXY_ROUTE setting_enabled=true");
        masterLogList.add("2026-10-26T19:23:04.192+0700 [app] TUN_BEGIN mtu=1280 ipv6=false");
        masterLogList.add("2026-10-26T19:23:04.211+0700 [app] TUN_READY");
        masterLogList.add("2026-10-26T19:23:04.214+0700 [app] UNDERLYING_NETWORK_AVAILABLE transport=cellular capability=internet");
        masterLogList.add("2026-10-26T19:23:04.217+0700 [app] HEALTH_GRACE duration_ms=5000");
        masterLogList.add("2026-10-26T19:23:04.226+0700 [app] CORE_START");
        masterLogList.add("2026-10-26T19:23:04.260+0700 [app] XRAY_RUNTIME level=warning message=\"core: Xray 26.7.11 started\"");
        masterLogList.add("2026-10-26T19:23:04.262+0700 [app] CORE_READY version=26.7.11");
        masterLogList.add("2026-10-26T19:23:04.269+0700 [app] PROTECTOR_SELF_TEST result=success tcp=true udp=true");
        masterLogList.add("2026-10-26T19:23:04.272+0700 [app] STATS_POLLING state=active");
        masterLogList.add("2026-10-26T19:23:04.279+0700 [app] HOTSPOT_PROXY_MONITOR state=started");
        masterLogList.add("2026-10-26T19:23:04.293+0700 [app] CONNECTED");
        masterLogList.add("2026-10-26T19:23:04.301+0700 [app] HOTSPOT_PROXY_LISTENER state=waiting_for_hotspot");
        masterLogList.add("2026-10-26T19:23:04.395+0700 [app] XRAY_RUNTIME level=error message=\"failed to dial to [redacted] :443 > read tcp [redacted] :47302 > [redacted] read: connection reset by peer\"");
        masterLogList.add("2026-10-26T19:23:07.444+0700 [app] XRAY_RUNTIME level=error message=\"failed to dial to [redacted] :443 > read tcp [redacted] :47442 > [redacted] read: connection reset by peer\"");
        masterLogList.add("2026-10-26T19:23:08.190+0700 [app] XRAY_RUNTIME level=error message=\"failed to dial to [redacted] :443 > read tcp [redacted] :47492 > [redacted] read: connection reset by peer\"");
    }

    // ==========================================
    // FUNGSI MENAMPILKAN LOG (DENGAN FILTER & WARNA)
    // ==========================================
    private void renderLogs() {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        for (String log : masterLogList) {
            // 1. Cek Pencarian (Search)
            if (!currentSearch.isEmpty() && !log.toLowerCase().contains(currentSearch)) {
                continue;
            }

            // 2. Cek Filter
            if (currentFilter.equals("Error") && !log.contains("level=error")) continue;
            if (currentFilter.equals("Warning") && !log.contains("level=warning")) continue;
            if (currentFilter.equals("Info") && !log.contains("level=info") && !log.contains("[app]")) continue; // Logika sederhana untuk info

            // 3. Tentukan Warna
            int color = 0xFFA0A0A0; // Warna default (Abu-abu)
            if (log.contains("level=error")) {
                color = 0xFFFF4C4C; // Merah untuk Error
            } else if (log.contains("level=warning")) {
                color = 0xFFFFD700; // Kuning untuk Warning
            } else if (log.contains("CONNECTED") || log.contains("success") || log.contains("started")) {
                color = 0xFF00FFCC; // Hijau Neon untuk Success
            }

            // 4. Tambahkan ke tampilan
            int start = sb.length();
            sb.append(log).append("\n\n");
            sb.setSpan(new ForegroundColorSpan(color), start, sb.length(), 0);
        }

        tvLogContent.setText(sb);

        // Auto scroll ke bawah setiap kali render
        scrollLogs.post(new Runnable() {
            @Override
            public void run() {
                scrollLogs.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    // ==========================================
    // FUNGSI TOMBOL FILTER (MENGUBAH WARNA)
    // ==========================================
    private void setupFilterButton(final TextView button, final String filterName) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = filterName;

                // Reset semua tombol ke desain tidak aktif
                resetFilterButtons();

                // Ubah tombol yang diklik menjadi aktif
                button.setBackgroundResource(R.drawable.bg_filter_active);
                button.setTextColor(0xFF000000); // Teks Hitam

                renderLogs();
            }
        });
    }

    private void resetFilterButtons() {
        int inactiveColor = 0xFFA0A0A0;
        filterAll.setBackgroundResource(R.drawable.bg_filter_inactive);
        filterAll.setTextColor(inactiveColor);
        filterError.setBackgroundResource(R.drawable.bg_filter_inactive);
        filterError.setTextColor(inactiveColor);
        filterWarning.setBackgroundResource(R.drawable.bg_filter_inactive);
        filterWarning.setTextColor(inactiveColor);
        filterInfo.setBackgroundResource(R.drawable.bg_filter_inactive);
        filterInfo.setTextColor(inactiveColor);
        filterDebug.setBackgroundResource(R.drawable.bg_filter_inactive);
        filterDebug.setTextColor(inactiveColor);
    }
}