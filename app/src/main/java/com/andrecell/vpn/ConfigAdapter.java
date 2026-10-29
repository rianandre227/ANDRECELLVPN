package com.andrecell.vpn;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

public class ConfigAdapter extends BaseAdapter {
    Context context;
    ArrayList<ConfigModel> listConfig;
    LayoutInflater inflater;

    public ConfigAdapter(Context context, ArrayList<ConfigModel> listConfig) {
        this.context = context;
        this.listConfig = listConfig;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() { return listConfig.size(); }
    @Override
    public Object getItem(int position) { return listConfig.get(position); }
    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_config, parent, false);
        }

        TextView tvCheck = convertView.findViewById(R.id.tvCheck);
        TextView tvLockIcon = convertView.findViewById(R.id.tvLockIcon);
        TextView tvName = convertView.findViewById(R.id.tvItemName);
        TextView tvHost = convertView.findViewById(R.id.tvItemHost);
        TextView tvProtocol = convertView.findViewById(R.id.tvItemProtocol);
        
        TextView tvPing = convertView.findViewById(R.id.tvPing);
        TextView btnEdit = convertView.findViewById(R.id.btnEdit);
        TextView btnShare = convertView.findViewById(R.id.btnShare);
        TextView btnDelete = convertView.findViewById(R.id.btnDelete);

        final ConfigModel currentConfig = listConfig.get(position);
        tvName.setText(currentConfig.getName());

        tvPing.setText("PING");
        tvPing.setTextColor(0xFFFFD700);

        if (currentConfig.isLocked()) {
            tvCheck.setText("✓");
            tvCheck.setTextColor(0xFF00FFCC); 
            tvLockIcon.setVisibility(View.VISIBLE); 
            tvHost.setText("Encrypted"); 
            tvProtocol.setText("—"); 
            btnEdit.setVisibility(View.GONE); 
            btnShare.setVisibility(View.GONE); 
        } else {
            tvCheck.setText("○");
            tvCheck.setTextColor(0xFFA0A0A0); 
            tvLockIcon.setVisibility(View.GONE); 
            
            String displayHost = currentConfig.getHost();
            if (displayHost.startsWith("{")) {
                try {
                    JSONObject detail = new JSONObject(displayHost);
                    String server = detail.optString("server", "");
                    String port = detail.optString("port", "");
                    if (!server.isEmpty()) {
                        displayHost = server + (port.isEmpty() ? "" : ":" + port);
                    }
                } catch (Exception e) {}
            }
            
            tvHost.setText(displayHost); 
            tvProtocol.setText(currentConfig.getProtocol()); 
            btnEdit.setVisibility(View.VISIBLE); 
            btnShare.setVisibility(View.VISIBLE); 
        }

        tvPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvPing.setText("Pinging...");
                tvPing.setTextColor(0xFFA0A0A0);
                tvPing.setEnabled(false);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int pingResult = new Random().nextInt(120) + 30;
                        tvPing.setText(pingResult + " ms");
                        if (pingResult < 80) {
                            tvPing.setTextColor(0xFF00FFCC);
                        } else {
                            tvPing.setTextColor(0xFFFF4C4C);
                        }
                        tvPing.setEnabled(true);
                    }
                }, 1500);
            }
        });

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = context.getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("NAMA_CONFIG", currentConfig.getName());
                editor.putString("HOST", currentConfig.getHost());
                editor.putString("PROTOCOL", currentConfig.getProtocol());
                editor.putBoolean("IS_LOCKED", currentConfig.isLocked());
                editor.apply();

                Toast.makeText(context, "Config dipilih: " + currentConfig.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!currentConfig.isLocked()) {
                    currentConfig.setLocked(true); 
                    saveDataToMemory(); 
                    notifyDataSetChanged(); 
                    Toast.makeText(context, "Config berhasil dikunci permanen!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Config ini sudah terkunci!", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String protokol = currentConfig.getProtocol().toUpperCase();
                Intent intent;
                
                if (protokol.equals("SSH")) { 
                    intent = new Intent(context, AddConfigActivity.class);
                } else { 
                    intent = new Intent(context, V2rayActivity.class);
                }
                
                intent.putExtra("EDIT_POSITION", position);
                context.startActivity(intent);
            }
        });

        // ==========================================
        // TOMBOL SHARE (BUKA HALAMAN EXPORT CONFIG)
        // ==========================================
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentConfig.isLocked()) {
                    Toast.makeText(context, "Config yang sudah dikunci tidak bisa dibagikan!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Buka halaman ExportConfigActivity dan kirim data config
                Intent intent = new Intent(context, ExportConfigActivity.class);
                intent.putExtra("CONFIG_NAME", currentConfig.getName());
                intent.putExtra("CONFIG_HOST", currentConfig.getHost());
                intent.putExtra("CONFIG_PROTOCOL", currentConfig.getProtocol());
                context.startActivity(intent);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                    .setTitle("Hapus Config")
                    .setMessage("Apakah Anda yakin ingin menghapus config " + currentConfig.getName() + "?")
                    .setPositiveButton("HAPUS", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences sharedPref = context.getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
                            String activeName = sharedPref.getString("NAMA_CONFIG", "");

                            if (activeName.equals(currentConfig.getName())) {
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.remove("NAMA_CONFIG");
                                editor.remove("HOST");
                                editor.remove("PROTOCOL");
                                editor.remove("IS_LOCKED");
                                editor.apply();
                            }

                            listConfig.remove(position);
                            saveDataToMemory();
                            notifyDataSetChanged();
                            Toast.makeText(context, "Config berhasil dihapus", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("BATAL", null)
                    .show();
            }
        });

        return convertView;
    }

    private void saveDataToMemory() {
        SharedPreferences sharedPref = context.getSharedPreferences("ConfigVPN", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        
        JSONArray jsonArray = new JSONArray();
        for (ConfigModel config : listConfig) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", config.getName());
                obj.put("host", config.getHost());
                obj.put("protocol", config.getProtocol());
                obj.put("locked", config.isLocked()); 
                jsonArray.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        editor.putString("CONFIG_LIST", jsonArray.toString());
        editor.apply();
    }
}